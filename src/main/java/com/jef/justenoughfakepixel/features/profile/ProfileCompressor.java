package com.jef.justenoughfakepixel.features.profile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public class ProfileCompressor {

    public static byte[] compressJSON(String jsonString) throws Exception {
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(jsonString.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        return obj.toByteArray();
    }

}
