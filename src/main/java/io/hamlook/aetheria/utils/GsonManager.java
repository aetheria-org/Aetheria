package io.hamlook.aetheria.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonManager {

    public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

}
