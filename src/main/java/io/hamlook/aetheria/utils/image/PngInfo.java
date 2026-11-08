package io.hamlook.aetheria.utils.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Iterator;

public final class PngInfo {

    private static final int SUBSAMPLE = 8;

    private PngInfo() {
    }

    public static int[] dimensions(File file) {
        ImageInputStream in = null;
        ImageReader reader = null;
        try {
            in = open(file);
            if (in == null) return null;
            reader = firstReader(in);
            if (reader == null) return null;
            reader.setInput(in, false, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            return width > 0 && height > 0 ? new int[]{width, height} : null;
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null) reader.dispose();
            closeQuietly(in);
        }
    }

    public static boolean isIntact(File file) {
        ImageInputStream in = null;
        ImageReader reader = null;
        try {
            in = open(file);
            if (in == null) return false;
            reader = firstReader(in);
            if (reader == null) return false;
            reader.setInput(in, false, true);
            ImageReadParam param = reader.getDefaultReadParam();
            if (param == null) param = new ImageReadParam();
            param.setSourceSubsampling(SUBSAMPLE, SUBSAMPLE, 0, 0);
            return reader.read(0, param) != null;
        } catch (Exception e) {
            return false;
        } finally {
            if (reader != null) reader.dispose();
            closeQuietly(in);
        }
    }

    public static boolean matches(File file, int width, int height) {
        int[] size = dimensions(file);
        if (size == null || size[0] != width || size[1] != height) return false;
        return isIntact(file);
    }

    private static ImageInputStream open(File file) throws Exception {
        if (file == null || !file.isFile() || file.length() == 0) return null;
        return ImageIO.createImageInputStream(file);
    }

    private static ImageReader firstReader(ImageInputStream in) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
        return readers.hasNext() ? readers.next() : null;
    }

    private static void closeQuietly(ImageInputStream in) {
        if (in == null) return;
        try {
            in.close();
        } catch (Exception ignored) {
        }
    }
}
