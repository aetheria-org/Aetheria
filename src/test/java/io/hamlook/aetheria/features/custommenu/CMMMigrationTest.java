package io.hamlook.aetheria.features.custommenu;

import com.google.gson.JsonParseException;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import org.junit.Test;
import static org.junit.Assert.*;

public class CMMMigrationTest {
    @Test public void currentVersionIsSerialized() {
        String json = CMMHelper.GSON.toJson(new CustomMMConfig("test"));
        assertEquals(CustomMMConfig.CURRENT_FORMAT_VERSION, CMMHelper.GSON.fromJson(json, CustomMMConfig.class).formatVersion);
    }
    @Test public void missingVersionUsesCurrentDefault() {
        CustomMMConfig config = CMMHelper.GSON.fromJson("{\"configName\":\"old\",\"elements\":[]}", CustomMMConfig.class);
        assertEquals(CustomMMConfig.CURRENT_FORMAT_VERSION, config.formatVersion);
    }
    @Test(expected = JsonParseException.class) public void malformedElementIsRejected() {
        CMMHelper.GSON.fromJson("{\"configName\":\"bad\",\"elements\":[{\"data\":{}}]}", CustomMMConfig.class);
    }
}
