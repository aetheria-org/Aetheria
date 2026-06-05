package io.hamlook.aetheria.repo.data;

import com.google.gson.annotations.SerializedName;

public class UpdateData {

    @SerializedName("version")
    public String version = "0.0.0";

    @SerializedName("update_msg")
    public String updateMsg = "";

    @SerializedName("update_url")
    public String updateUrl = "";
}
