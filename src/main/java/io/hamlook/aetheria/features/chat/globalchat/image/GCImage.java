package io.hamlook.aetheria.features.chat.globalchat.image;

import dev.matrixlab.webp4j.WebPCodec;
import dev.matrixlab.webp4j.model.AnimatedWebPData;
import dev.matrixlab.webp4j.model.AnimatedWebPFrame;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.utils.ThreadUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
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


    public List<BufferedImage> images;
    public List<ResourceLocation> frames = new ArrayList<>();
    /** Per-frame display delay in ms, one entry per frame (same order as {@link #frames}). */
    public List<Integer> delays;
    public int frameDelay;
    public String id;
    public transient volatile int curFrame = 0;
    public transient volatile long lastUpdate = 0;
    public boolean circularMask = false;
    public String url = "";

    /** Natural pixel dimensions of the decoded media, once known (0 until loaded). */
    public int width = 0;
    public int height = 0;

    public transient volatile boolean isLoaded = false;
    public transient volatile boolean loadFailed = false;

    public static final int[] QUALITIES = {240,360,480,720,1080,1440,3,840};
    private static final Pattern META_TAG_PATTERN = Pattern.compile(
            "<meta[^>]*(?:property|name)=[\"'](?:og:image|twitter:image)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_ATTR_PATTERN = Pattern.compile(
            "content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public GCImage(List<BufferedImage> images, int frameDelay) {
        this.images = images;
        this.frameDelay = frameDelay;
        this.delays = new ArrayList<>();
        id = UUID.randomUUID().toString();
    }

    public void updateFrame() {
        if (!isLoaded || frames.size() <= 1) return;
        int delay = currentDelay();
        if (delay <= 0) return;
        if (System.currentTimeMillis() - lastUpdate < delay) return;

        curFrame++;
        if (curFrame >= frames.size()) {
            curFrame = 0;
        }
        lastUpdate = System.currentTimeMillis();
    }

    private int currentDelay() {
        if (!delays.isEmpty()) {
            int idx = Math.min(curFrame, delays.size() - 1);
            int d = delays.get(idx);
            if (d > 0) return d;
        }
        return frameDelay > 0 ? frameDelay : 100;
    }

    public ResourceLocation getTextureToRender(boolean animated) {
        if (!isLoaded || frames.isEmpty()) return null;
        if (frames.size() == 1) return frames.get(0);
        if (ATHRConfig.feature != null && ATHRConfig.feature.network.globalChatConfig.reducedAnimations && !animated) return frames.get(0);

        updateFrame();
        return frames.get(curFrame);
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

        ThreadUtils.run("GCImage-Downloader-" + gcImage.id, () -> {
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
                boolean notFound = e instanceof DownloadException && ((DownloadException) e).code == 404;
                if (!notFound && url.toLowerCase().endsWith(".gif")) {
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
            }
        });
        return gcImage.id;
    }

    /** Loads a user-selected local image into the same dynamic texture pipeline as remote images. */
    public static String createGCImageFromFile(String filePath) {
        GCImage gcImage = new GCImage(new ArrayList<>(), -1);
        gcImage.url = filePath == null ? "" : filePath;
        ImageManager.images.put(gcImage.id, gcImage);
        ThreadUtils.run("GCImage-FileLoader-" + gcImage.id, () -> {
            try {
                BufferedImage image = ImageIO.read(new File(filePath));
                if (image == null) throw new IOException("Unsupported image format");
                gcImage.images.add(image);
                gcImage.width = image.getWidth();
                gcImage.height = image.getHeight();
                finalizeLoad(gcImage);
            } catch (Exception e) {
                gcImage.loadFailed = true;
                Aetheria.logger.warning("[GCImage] Failed to load local image: " + e.getMessage());
            }
        });
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

        ThreadUtils.run("GCImage-PageResolver-" + gcImage.id, () -> {
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
            }
        });
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
        MinecraftCompat.getMinecraft().addScheduledTask(() -> {
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
                ResourceLocation resLoc = MinecraftCompat.getMinecraft().getTextureManager()
                        .getDynamicTextureLocation("gcimage_" + gcImage.id + "_" + i, dynamicTexture);
                MinecraftCompat.getMinecraft().getTextureManager().bindTexture(resLoc);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                gcImage.frames.add(resLoc);
            }
            gcImage.images.clear();
            gcImage.curFrame = 0;
            gcImage.lastUpdate = System.currentTimeMillis();
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

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            String hint = code == 404 && url.contains("discordapp.com")
                    ? " (expired or deleted Discord attachment)" : "";
            throw new DownloadException(code, "HTTP " + code + hint + ": " + url);
        }

        int expectedLength = connection.getContentLength();

        try (InputStream is = new BufferedInputStream(connection.getInputStream());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            byte[] data = baos.toByteArray();
            // Servers can close the stream early (proxies, timeouts, flaky CDNs),
            // silently truncating the media. Truncated GIFs lose their trailing
            // frames, so treat short reads as failures instead of decoding them.
            if (expectedLength > 0 && data.length < expectedLength) {
                throw new IOException("Incomplete download: received " + data.length
                        + " of " + expectedLength + " bytes from " + url);
            }
            return data;
        }
    }

    /** Carries the HTTP status so callers can react (e.g. skip retries on a definitive 404). */
    private static class DownloadException extends IOException {
        final int code;

        DownloadException(int code, String message) {
            super(message);
            this.code = code;
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
        // A retry after a partial decode can land here with garbage in the lists.
        gcImage.images.clear();
        gcImage.delays.clear();
        int MAX_STATIC_DIMENSION = QUALITIES[ATHRConfig.feature.network.globalChatConfig.maxImageGifQuality];
        /* Animated images are capped much lower so frame memory stays sane. */
        int MAX_ANIMATED_DIMENSION = MAX_STATIC_DIMENSION/4;
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

                for (int i = 0; i < webpFrames.size(); i++) {
                    BufferedImage rawFrame = webpFrames.get(i).getImage();
                    gcImage.images.add(capScale < 1f ? scaleDown(rawFrame, MAX_ANIMATED_DIMENSION) : rawFrame);
                    int d = delays != null && i < delays.length ? delays[i] : 0;
                    gcImage.delays.add(d > 0 ? d : 100);
                }
                // Per-frame delays drive the animation; this is only a fallback.
                gcImage.frameDelay = 100;
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
        // A retry after a partial decode can land here with garbage in the lists.
        gcImage.images.clear();
        gcImage.delays.clear();
        int MAX_STATIC_DIMENSION = QUALITIES[ATHRConfig.feature.network.globalChatConfig.maxImageGifQuality];
        /* Animated images are capped much lower so frame memory stays sane. */
        int MAX_ANIMATED_DIMENSION = MAX_STATIC_DIMENSION/4;

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

                // Composite like a normal GIF decoder: frames are raw strips, so
                // every frame is drawn onto a persistent canvas the size of the
                // logical screen, honouring the frame's offset and the previous
                // frame's disposal method, and a snapshot of the canvas becomes
                // the displayed frame.
                boolean gifFormat = "gif".equalsIgnoreCase(reader.getFormatName());
                BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = canvas.createGraphics();
                if (capScale < 1f) {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                }

                int prevDisposal = 0;
                int prevX = 0, prevY = 0, prevW = canvasWidth, prevH = canvasHeight;
                int decodeFailures = 0;

                for (int i = 0; i < numFrames; i++) {
                    BufferedImage frame;
                    try {
                        frame = reader.read(i);
                    } catch (Exception frameEx) {
                        decodeFailures++;
                        continue;
                    }
                    if (frame == null) {
                        decodeFailures++;
                        continue;
                    }

                    int fx = 0, fy = 0, fw = frame.getWidth(), fh = frame.getHeight();
                    int disposal = 0;
                    if (gifFormat) {
                        try {
                            IIOMetadata metadata = reader.getImageMetadata(i);
                            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());
                            fx = intAttr(root, "imageLeftPosition", 0);
                            fy = intAttr(root, "imageTopPosition", 0);
                            fw = intAttr(root, "imageWidth", fw);
                            fh = intAttr(root, "imageHeight", fh);
                            disposal = disposalOf(getNode(root, "GraphicControlExtension"));
                        } catch (Exception ignored) {}
                    }

                    if (prevDisposal == 2) {
                        g.clearRect(prevX, prevY, prevW, prevH);
                    }

                    g.drawImage(frame,
                            Math.round(fx * capScale), Math.round(fy * capScale),
                            Math.round(fw * capScale), Math.round(fh * capScale),
                            null);

                    BufferedImage snapshot = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                    snapshot.getGraphics().drawImage(canvas, 0, 0, null);
                    gcImage.images.add(snapshot);

                    try {
                        IIOMetadata metadata = reader.getImageMetadata(i);
                        int delayMs = extractDelayMs(metadata);
                        gcImage.delays.add(delayMs > 0 ? delayMs : 100);
                    } catch (Exception ignored) {
                        gcImage.delays.add(100);
                    }

                    prevDisposal = disposal;
                    prevX = Math.round(fx * capScale);
                    prevY = Math.round(fy * capScale);
                    prevW = Math.round(fw * capScale);
                    prevH = Math.round(fh * capScale);
                }
                g.dispose();
                // Per-frame delays drive the animation; this is only a fallback.
                gcImage.frameDelay = 100;

                if (decodeFailures > 0) {
                    Aetheria.logger.warning("[GCImage] " + decodeFailures + "/" + numFrames
                            + " frames failed to decode for: " + url);
                }
                if (decodeFailures * 2 >= numFrames || gcImage.images.isEmpty()) {
                    Aetheria.logger.warning("[GCImage] Too many frames failed to decode for: " + url);
                    gcImage.images.clear();
                    gcImage.delays.clear();
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

    private static int disposalOf(IIOMetadataNode graphicControl) {
        if (graphicControl == null) return 0;
        String dm = graphicControl.getAttribute("disposalMethod");
        if (dm.isEmpty()) return 0;
        if ("restoreToBackground".equalsIgnoreCase(dm)) return 2;
        if ("restoreToPrevious".equalsIgnoreCase(dm)) return 3;
        return 0;
    }

    private static int intAttr(IIOMetadataNode root, String attr, int def) {
        IIOMetadataNode node = getNode(root, "ImageDescriptor");
        if (node == null) return def;
        String v = node.getAttribute(attr);
        if (v.isEmpty()) return def;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
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
