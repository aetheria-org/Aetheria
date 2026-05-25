package io.hamlook.aetheria.features.chatfilters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatFilterManager {

    public static List<ChatFilter> chatFilters = new ArrayList<>();
    public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void initialise(){
        chatFilters.clear();
        chatFilters = loadFromFile();
    }


    public static void saveToFile(){
        File file = new File(ATHRConfig.configDirectory,"chatFilters.json");
        try{
            FileWriter writer = new FileWriter(file);
            writer.write(GSON.toJson(chatFilters));
            writer.close();
        } catch (IOException e) {
            Aetheria.logger.info("[ChatFilter] Error While trying to Save ChatFilters" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<ChatFilter> loadFromFile(){
        List<ChatFilter> filters = new ArrayList<>();
        File file = new File(ATHRConfig.configDirectory,"chatFilters.json");
        TypeToken<List<ChatFilter>> type = new  TypeToken<List<ChatFilter>>(){};
        try{
            if(!file.exists()){
                file.createNewFile();
                return filters;
            }
            FileReader reader = new FileReader(file);
            filters = GSON.fromJson(reader,type.getType());
            if(filters == null){
                Aetheria.logger.info("[ChatFilter] ChatFilters save is corrupted.");
                Date date = new Date();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                file.renameTo(new File(ATHRConfig.configDirectory,"chatFilters" + dateFormat.format(date) + ".corrupted"));
                return new ArrayList<>();
            }
            reader.close();
            return filters;
        } catch (IOException e) {
            Aetheria.logger.info("[ChatFilter] Error While trying to Load ChatFilters" + e.getMessage());
            e.printStackTrace();
        }
        return filters;
    }

    public static IChatComponent applyFilters(IChatComponent message) {
        if (message == null) return null;
        String msg = message.getFormattedText();
        for(ChatFilter filter : chatFilters){
            msg = filter.applyFilter(msg);
            if(msg == null) return null;
        }
        if(msg.equals(message.getFormattedText())) return message;
        return new ChatComponentText(msg.trim());
    }
}
