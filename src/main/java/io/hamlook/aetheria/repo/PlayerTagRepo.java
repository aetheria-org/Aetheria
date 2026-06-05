package io.hamlook.aetheria.repo;

import io.hamlook.aetheria.repo.data.PlayerTagData;
import io.hamlook.aetheria.utils.NameUtils;

public class PlayerTagRepo {

    private PlayerTagRepo() {
    }

    public static PlayerTagData.Entry getTag(String ign) {
        PlayerTagData data = RepoHandler.get(ATHRRepo.KEY_TAGS, PlayerTagData.class, null);
        if (data == null || data.tags == null || ign == null) return null;
        String normalized = NameUtils.normalize(ign);
        for (PlayerTagData.Entry entry : data.tags) {
            if (entry != null && normalized.equals(NameUtils.normalize(entry.name))) {
                return entry;
            }
        }
        return null;
    }
}
