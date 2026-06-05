package io.hamlook.aetheria.repo;

import com.google.gson.reflect.TypeToken;
import io.hamlook.aetheria.repo.data.PlayerSizeData;
import io.hamlook.aetheria.utils.NameUtils;

import java.lang.reflect.Type;
import java.util.List;

public class PlayerSizeRepo {

    private static final Type LIST_TYPE = new TypeToken<List<PlayerSizeData>>() {
    }.getType();

    private PlayerSizeRepo() {
    }

    public static PlayerSizeData getScale(String name) {
        List<PlayerSizeData> list = RepoHandler.get(ATHRRepo.KEY_PLAYERSIZES, LIST_TYPE, null);
        if (list == null || name == null) return null;
        String normalized = NameUtils.normalize(name);
        for (PlayerSizeData d : list) {
            if (d != null && normalized.equals(NameUtils.normalize(d.name))) return d;
        }
        return null;
    }
}
