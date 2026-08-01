package io.hamlook.aetheria.repo;

import io.hamlook.aetheria.repo.data.RepoData;

public class CapeAPI {

    public static String getAPIUrl() {
        RepoData url = RepoHandler.get(ATHRRepo.KEY_REPO, RepoData.class, new RepoData());
        return url.capeApi;
    }

    public static String getModSecret(){
        return "a7c0e73c-3b0b-4789-8c80-741dd09ba1bc";
    }

    public static String getAPIUrl(String endpoint) {
        String api = getAPIUrl();
        return api + (api.endsWith("/") || endpoint.startsWith("/") ? "" : "/") + endpoint;
    }

    public static String getWebsocketURL() {
        String capeAPI = getAPIUrl();
        if(capeAPI.startsWith("http")){
            capeAPI = capeAPI.replace("http","ws");
        }
        if(capeAPI.startsWith("https")){
            capeAPI = capeAPI.replace("https","wss");
        }
        if(capeAPI.endsWith("/")){
            capeAPI = capeAPI.substring(0,capeAPI.length()-1);
        }
        return capeAPI;
    }
}
