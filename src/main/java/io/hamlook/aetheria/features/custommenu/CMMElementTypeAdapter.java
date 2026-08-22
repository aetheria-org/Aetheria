package io.hamlook.aetheria.features.custommenu;

import com.google.gson.*;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.features.custommenu.ui.text.Text;

import java.lang.reflect.Type;

public class CMMElementTypeAdapter implements JsonSerializer<CMMElement>, JsonDeserializer<CMMElement> {

    private static final String TYPE_FIELD = "type";

    @Override
    public JsonElement serialize(CMMElement src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty(TYPE_FIELD, getTypeName(src.getClass()));
        JsonElement element = context.serialize(src, src.getClass());
        obj.add("data", element);
        return obj;
    }

    @Override
    public CMMElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String typeName = obj.get(TYPE_FIELD).getAsString();
        Class<? extends CMMElement> clazz = getClassForType(typeName);
        return context.deserialize(obj.get("data"), clazz);
    }

    private String getTypeName(Class<? extends CMMElement> clazz) {
        if (clazz == CMMButton.class || CMMButton.class.isAssignableFrom(clazz)) return "button";
        if (clazz == Sprite.class || Sprite.class.isAssignableFrom(clazz)) return "sprite";
        if (clazz == Text.class || Text.class.isAssignableFrom(clazz)) return "text";
        return "element";
    }

    private Class<? extends CMMElement> getClassForType(String typeName) {
        switch (typeName) {
            case "button": return CMMButton.class;
            case "sprite": return Sprite.class;
            case "text": return Text.class;
            default: return CMMElement.class;
        }
    }
}