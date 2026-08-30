package io.hamlook.aetheria.features.custommenu;

import com.google.gson.*;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;
import io.hamlook.aetheria.features.custommenu.ui.buttons.ButtonStyle;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.features.custommenu.ui.text.Text;

import java.lang.reflect.Type;

public class CMMElementTypeAdapter implements JsonSerializer<CMMElement>, JsonDeserializer<CMMElement> {

    private static final String TYPE_FIELD = "type";

    @Override
    public JsonElement serialize(CMMElement src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty(TYPE_FIELD, getTypeName(src));
        JsonElement element = context.serialize(src, src.getClass());
        obj.add("data", element);
        return obj;
    }

    @Override
    public CMMElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String typeName = obj.get(TYPE_FIELD).getAsString();
        Class<? extends CMMElement> clazz = getClassForType(typeName);
        JsonElement data = obj.get("data");
        if (data != null && data.isJsonObject() && data.getAsJsonObject().has("style") && data.getAsJsonObject().get("style").isJsonPrimitive() && data.getAsJsonObject().get("style").getAsJsonPrimitive().isNumber()) {
            int index = data.getAsJsonObject().get("style").getAsInt();
            data.getAsJsonObject().addProperty("style", ButtonStyle.fromIndex(index).name());
        }
        return context.deserialize(data, clazz);
    }

    private String getTypeName(CMMElement element) {
        if (element instanceof io.hamlook.aetheria.features.custommenu.ui.buttons.impl.ActionButton) return "action_button";
        if (element instanceof io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton) return "gui_button";
        if (element instanceof CMMButton) return "button";
        if (element instanceof Sprite) return "image";
        if (element instanceof Text) return "text";
        return "element";
    }

    private Class<? extends CMMElement> getClassForType(String typeName) {
        switch (typeName) {
            case "button": return io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton.class;
            case "gui_button": return io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton.class;
            case "action_button": return io.hamlook.aetheria.features.custommenu.ui.buttons.impl.ActionButton.class;
            case "image":
            case "sprite": return Sprite.class;
            case "text": return Text.class;
            default: return CMMElement.class;
        }
    }
}
