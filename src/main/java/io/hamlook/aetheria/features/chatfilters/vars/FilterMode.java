package io.hamlook.aetheria.features.chatfilters.vars;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum FilterMode {

    STARTS("Starts With"),
    ENDS("Ends With"),
    CONTAINS("Contains");

    public final String name;
}
