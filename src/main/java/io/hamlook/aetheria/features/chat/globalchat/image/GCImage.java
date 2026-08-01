package io.hamlook.aetheria.features.chat.globalchat.image;

import dev.matrixlab.webp4j.WebPCodec;
import dev.matrixlab.webp4j.model.AnimatedWebPData;
import dev.matrixlab.webp4j.model.AnimatedWebPFrame;
import io.hamlook.aetheria.Aetheria;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCImage {

    /**
     * Static images are decoded at their true/full resolution so the chat can
     * always draw a quality 1:1 downscale (bilinear). Cap is only a safety net
     * against absurd dimensions / out-of-memory.
     */
    public static final int MAX_STATIC_DIMENSION = 4096;

    /** Animated images are capped much lower so frame memory stays sane. */
    public static final int MAX_ANIMATED_DIMENSION = 1024;

    public List<BufferedImage> images;
    public List<ResourceLocation> frames = new ArrayList<>();
    public int frameDelay;
    public String id;
    public int curFrame = 0;
    public long lastUpdate = 0;
    public boolean circularMask = false;
    public String url = "";

    /** Natural pixel dimensions of the decoded media, once known (0 until loaded). */
    public int width = 0;
    public int height = 0;

    public boolean isLoaded = false;
    public boolean loadFailed = false;

    private static final Pattern META_TAG_PATTERN = Pattern.compile(
            "<meta[^>]*(?:property|name)=[\"'](?:og:image|twitter:image)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_ATTR_PATTERN = Pattern.compile(
            "content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public GCImage(List<BufferedImage> images, int frameDelay) {
        this.images = images;
        this.frameDelay = frameDelay;
        id = UUID.randomUUID().toString();
    }

    public void updateFrame() {
        if (!isLoaded || frames.size() <= 1 || frameDelay <= 0) return;
        if (System.currentTimeMillis() - lastUpdate < frameDelay) return;

        curFrame++;
        if (curFrame >= frames.size()) {
            curFrame = 0;
        }
        lastUpdate = System.currentTimeMillis();
    }

    public ResourceLocation getTextureToRender(boolean isHovered) {
        if (!isLoaded || frames.isEmpty()) return null;
        if (frames.size() == 1) return frames.get(0);
        if (ImageManager.reduceAnimations && !isHovered) return frames.get(0);

        return frames.get(curFrame);
    }

    public static String buildAvatarUrl(String userId, String hash) {
        boolean animated = hash.startsWith("a_");
        String base = "https://cdn.discordapp.com/avatars/" + userId + "/" + hash + ".webp?size=240";
        return animated ? (base + "&animated=true") : base;
    }

    /** True if the URL (ignoring any query string) ends in a common raster/animated image extension. */
    public static boolean looksLikeImageUrl(String url) {
        if (url == null) return false;
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        String lower = path.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp");
    }

    public static String createGCImage(String url) {
        return createGCImage(url, false, false);
    }

    public static String createGCImage(String url, boolean circularMask) {
        return createGCImage(url, circularMask, false);
    }

    public static String createGCImage(String url, boolean circularMask, boolean expectedAnimated) {
        GCImage gcImage = new GCImage(new ArrayList<>(), -1);
        gcImage.url = url;
        gcImage.circularMask = circularMask;
        ImageManager.images.put(gcImage.id, gcImage);

        new Thread(() -> {
            try {
                byte[] rawBytes = downloadBytes(url);

                if (isWebP(rawBytes)) {
                    decodeWebP(gcImage, rawBytes, url);
                } else {
                    decodeViaImageIO(gcImage, rawBytes, url);
                }

                if (expectedAnimated && gcImage.images.size() <= 1) {
                    Aetheria.logger.warning("[GCImage] Expected animated content but only decoded "
                            + gcImage.images.size() + " frame(s) for: " + url);
                }

                finalizeLoad(gcImage);

            } catch (Exception e) {
                Aetheria.logger.warning("[GCImage] Failed to load image from " + url + ": " + e);
                if (url.toLowerCase().endsWith(".gif")) {
                    Aetheria.logger.warning("[GCImage] Trying to fetch animated webp instead.");
                    String webpUrl = url.substring(0, url.length() - 4) + ".webp?animated=true";
                    try {
                        byte[] retryBytes = downloadBytes(webpUrl);
                        if (isWebP(retryBytes)) {
                            decodeWebP(gcImage, retryBytes, webpUrl);
                        } else {
                            decodeViaImageIO(gcImage, retryBytes, webpUrl);
                        }
                        finalizeLoad(gcImage);
                        return;
                    } catch (Exception retryEx) {
                        Aetheria.logger.warning("[GCImage] Retry as webp also failed for " + url + ": " + retryEx);
                    }
                }
                gcImage.loadFailed = true;
                e.printStackTrace();
            }
        }, "GCImage-Downloader-" + gcImage.id).start();
        return gcImage.id;
    }

    /**
     * Resolves a webpage URL (e.g. a tenor.com/view/... link) to its embedded
     * media via OpenGraph/Twitter-card meta tags, then loads that media the
     * same way createGCImage() would. Intended for a whitelist of known
     * link-preview sites - not for arbitrary URLs.
     */
    public static String createGCImageFromPage(String pageUrl) {
        GCImage gcImage = new GCImage(new ArrayList<>(), -1);
        gcImage.url = pageUrl;
        ImageManager.images.put(gcImage.id, gcImage);

        new Thread(() -> {
            try {
                String mediaUrl = resolveOgImageUrl(pageUrl);
                if (mediaUrl == null) {
                    Aetheria.logger.warning("[GCImage] Could not resolve an image from page: " + pageUrl);
                    gcImage.loadFailed = true;
                    return;
                }

                byte[] rawBytes = downloadBytes(mediaUrl);
                if (isWebP(rawBytes)) {
                    decodeWebP(gcImage, rawBytes, mediaUrl);
                } else {
                    decodeViaImageIO(gcImage, rawBytes, mediaUrl);
                }

                finalizeLoad(gcImage);

            } catch (Exception e) {
                Aetheria.logger.warning("[GCImage] Failed to load embed from " + pageUrl + ": " + e);
                gcImage.loadFailed = true;
                e.printStackTrace();
            }
        }, "GCImage-PageResolver-" + gcImage.id).start();
        return gcImage.id;
    }

    private static String resolveOgImageUrl(String pageUrl) throws Exception {
        URL u = new URL(pageUrl);
        HttpURLConnection connection = (HttpURLConnection) u.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        StringBuilder html = new StringBuilder();
        try (InputStream is = new BufferedInputStream(connection.getInputStream())) {
            byte[] buffer = new byte[8192];
            int read;
            int totalRead = 0;
            // og:image/twitter:image live in <head>; no need to pull the whole page.
            while ((read = is.read(buffer)) != -1 && totalRead < 200_000) {
                html.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                totalRead += read;
            }
        }

        String pageHtml = html.toString();
        Matcher tagMatcher = META_TAG_PATTERN.matcher(pageHtml);
        while (tagMatcher.find()) {
            Matcher contentMatcher = CONTENT_ATTR_PATTERN.matcher(tagMatcher.group());
            if (contentMatcher.find()) {
                String candidate = contentMatcher.group(1).replace("&amp;", "&");
                if (looksLikeImageUrl(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static void finalizeLoad(GCImage gcImage) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (gcImage.images.isEmpty()) {
                gcImage.loadFailed = true;
                return;
            }
            for (int i = 0; i < gcImage.images.size(); i++) {
                BufferedImage bimg = gcImage.images.get(i);
                if (gcImage.circularMask) {
                    bimg = applyCircularMask(bimg);
                }
                DynamicTexture dynamicTexture = new DynamicTexture(bimg);
                ResourceLocation resLoc = Minecraft.getMinecraft().getTextureManager()
                        .getDynamicTextureLocation("gcimage_" + gcImage.id + "_" + i, dynamicTexture);
                Minecraft.getMinecraft().getTextureManager().bindTexture(resLoc);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                gcImage.frames.add(resLoc);
            }
            gcImage.images.clear();
            gcImage.isLoaded = true;
        });
    }

    private static BufferedImage applyCircularMask(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage masked = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = masked.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, w, h));
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return masked;
    }

    private static byte[] downloadBytes(String url) throws Exception {
        URL imageUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try (InputStream is = new BufferedInputStream(connection.getInputStream());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private static boolean isWebP(byte[] data) {
        if (data.length < 12) return false;
        return data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P';
    }

    private static void decodeWebP(GCImage gcImage, byte[] rawBytes, String url) {
        if (!WebPCodec.isAvailable()) {
            Aetheria.logger.warning("[GCImage] webp4j native support unavailable on this platform for: " + url);
            return;
        }

        try {
            AnimatedWebPData animated = WebPCodec.decodeAnimatedWebP(rawBytes);
            if (animated.getFrameCount() > 1) {
                int cw = animated.getCanvasWidth();
                int ch = animated.getCanvasHeight();
                float capScale = 1f;
                if (Math.max(cw, ch) > MAX_ANIMATED_DIMENSION) {
                    capScale = MAX_ANIMATED_DIMENSION / (float) Math.max(cw, ch);
                    cw = Math.round(cw * capScale);
                    ch = Math.round(ch * capScale);
                }
                gcImage.width = cw;
                gcImage.height = ch;

                List<AnimatedWebPFrame> webpFrames = animated.getFrames();
                int[] delays = animated.getDelays();

                int totalDelay = 0;
                int validFrames = 0;
                for (int i = 0; i < webpFrames.size(); i++) {
                    BufferedImage rawFrame = webpFrames.get(i).getImage();
                    gcImage.images.add(capScale < 1f ? scaleDown(rawFrame, MAX_ANIMATED_DIMENSION) : rawFrame);
                    if (delays != null && i < delays.length && delays[i] > 0) {
                        totalDelay += delays[i];
                        validFrames++;
                    }
                }
                gcImage.frameDelay = validFrames > 0 ? (totalDelay / validFrames) : 100;
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            BufferedImage image = WebPCodec.decodeImage(rawBytes);
            if (Math.max(image.getWidth(), image.getHeight()) > MAX_STATIC_DIMENSION) {
                image = scaleDown(image, MAX_STATIC_DIMENSION);
            }
            gcImage.images.add(image);
            gcImage.frameDelay = -1;
            gcImage.width = image.getWidth();
            gcImage.height = image.getHeight();
        } catch (Exception staticEx) {
            Aetheria.logger.warning("[GCImage] webp4j failed to decode static webp for " + url + ": " + staticEx);
        }
    }

    private static void decodeViaImageIO(GCImage gcImage, byte[] rawBytes, String url) throws Exception {
        List<ImageReader> candidates = new ArrayList<>();
        try (ImageInputStream probeStream = ImageIO.createImageInputStream(new ByteArrayInputStream(rawBytes))) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(probeStream);
            while (it.hasNext()) candidates.add(it.next());
        }

        if (candidates.isEmpty()) {
            Aetheria.logger.warning("[GCImage] No ImageReader found for: " + url);
            return;
        }

        // Multiple readers can claim the same PNG stream (e.g. the JDK's built-in
        // single-frame reader alongside an APNG-aware plugin). The built-in one
        // doesn't error on an animated PNG - it just silently reports 1 frame.
        // Probe every candidate and keep whichever reports the most frames.
        ImageReader chosenReader = null;
        int chosenFrameCount = 0;

        for (ImageReader candidate : candidates) {
            try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(rawBytes))) {
                candidate.setInput(iis);
                int n = candidate.getNumImages(true);
                if (n > chosenFrameCount) {
                    if (chosenReader != null) chosenReader.dispose();
                    chosenReader = candidate;
                    chosenFrameCount = n;
                } else {
                    candidate.dispose();
                }
            } catch (Exception probeEx) {
                candidate.dispose();
            }
        }

        if (chosenReader == null) {
            Aetheria.logger.warning("[GCImage] No usable ImageReader for: " + url);
            return;
        }

        ImageReader reader = chosenReader;
        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(rawBytes));
        reader.setInput(iis);

        try {
            int numFrames = reader.getNumImages(true);

            if (numFrames > 1) {
                int canvasWidth = reader.getWidth(0);
                int canvasHeight = reader.getHeight(0);
                float capScale = 1f;
                if (Math.max(canvasWidth, canvasHeight) > MAX_ANIMATED_DIMENSION) {
                    capScale = MAX_ANIMATED_DIMENSION / (float) Math.max(canvasWidth, canvasHeight);
                    canvasWidth = Math.round(canvasWidth * capScale);
                    canvasHeight = Math.round(canvasHeight * capScale);
                }
                gcImage.width = canvasWidth;
                gcImage.height = canvasHeight;

                BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = canvas.createGraphics();
                if (capScale < 1f) {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                }

                int totalDelay = 0;
                int validFrames = 0;

                for (int i = 0; i < numFrames; i++) {
                    BufferedImage frame;
                    try {
                        frame = reader.read(i);
                    } catch (Exception frameEx) {
                        frameEx.printStackTrace();
                        continue;
                    }

                    if (capScale < 1f) {
                        g.drawImage(frame, 0, 0, canvasWidth, canvasHeight, null);
                    } else {
                        g.drawImage(frame, 0, 0, null);
                    }
                    BufferedImage snapshot = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                    snapshot.getGraphics().drawImage(canvas, 0, 0, null);
                    gcImage.images.add(snapshot);

                    try {
                        IIOMetadata metadata = reader.getImageMetadata(i);
                        int delayMs = extractDelayMs(metadata);
                        if (delayMs > 0) {
                            totalDelay += delayMs;
                            validFrames++;
                        }
                    } catch (Exception ignored) {}
                }
                g.dispose();
                gcImage.frameDelay = validFrames > 0 ? (totalDelay / validFrames) : 100;

                if (gcImage.images.isEmpty()) {
                    Aetheria.logger.warning("[GCImage] All frames failed to decode for: " + url);
                }
            } else {
                BufferedImage image = reader.read(0);
                if (image != null) {
                    if (Math.max(image.getWidth(), image.getHeight()) > MAX_STATIC_DIMENSION) {
                        image = scaleDown(image, MAX_STATIC_DIMENSION);
                    }
                    gcImage.images.add(image);
                    gcImage.frameDelay = -1;
                    gcImage.width = image.getWidth();
                    gcImage.height = image.getHeight();
                }
            }
        } finally {
            reader.dispose();
            iis.close();
        }
    }

    private static int extractDelayMs(IIOMetadata metadata) {
        try {
            String nativeFormat = metadata.getNativeMetadataFormatName();
            if (nativeFormat == null) return -1;

            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);

            IIOMetadataNode graphicsControl = getNode(root, "GraphicControlExtension");
            if (graphicsControl != null) {
                String delayTime = graphicsControl.getAttribute("delayTime");
                if (!delayTime.isEmpty()) {
                    return Integer.parseInt(delayTime) * 10;
                }
            }

            IIOMetadataNode fctl = getNode(root, "fcTL");
            if (fctl != null) {
                String delayNum = fctl.getAttribute("delay_num");
                String delayDen = fctl.getAttribute("delay_den");
                if (!delayNum.isEmpty() && !delayDen.isEmpty()) {
                    int num = Integer.parseInt(delayNum);
                    int den = Integer.parseInt(delayDen);
                    if (den == 0) den = 100;
                    return (int) ((num / (float) den) * 1000);
                }
            }
        } catch (Exception ignored) {}

        return -1;
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        return null;
    }

    private static BufferedImage scaleDown(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxDim && h <= maxDim) return src;
        float s = maxDim / (float) Math.max(w, h);
        int nw = Math.round(w * s);
        int nh = Math.round(h * s);
        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return scaled;
    }
}