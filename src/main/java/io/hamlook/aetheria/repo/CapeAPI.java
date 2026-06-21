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
        RepoData url = RepoHandler.get(ATHRRepo.KEY_REPO, RepoData.class, new RepoData());
        return url.capeApi + (url.capeApi.endsWith("/") || endpoint.startsWith("/") ? "" : "/") + endpoint;
    }

}
