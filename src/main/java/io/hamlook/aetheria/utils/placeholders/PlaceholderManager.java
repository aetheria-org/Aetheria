package io.hamlook.aetheria.utils.placeholders;

import io.hamlook.aetheria.utils.placeholders.impl.PlayerPlaceholder;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderManager {

    public static List<Placeholder> placeholders = new ArrayList<>();

    public static void initialise(){
        placeholders.add(new PlayerPlaceholder());
    }

    public static String replace(String initial){
        String finalString = initial;
        for(Placeholder placeholder : placeholders){
            finalString = placeholder.replace(finalString);
        }
        return finalString;
    }

}
