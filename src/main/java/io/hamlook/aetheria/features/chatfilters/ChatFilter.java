package io.hamlook.aetheria.features.chatfilters;

import io.hamlook.aetheria.features.chatfilters.vars.FilterCase;
import io.hamlook.aetheria.features.chatfilters.vars.FilterMode;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.hamlook.aetheria.features.chatfilters.vars.FilterAction;

public class ChatFilter {

    public List<String> filterWords;
    public FilterMode filterType;
    public FilterCase filterCase;
    public boolean replace;
    public FilterAction action;

    public ChatFilter() {}

    public ChatFilter(List<String> filterWords, FilterMode filterType, FilterCase filterCase, FilterAction action) {
        this.filterWords = filterWords;
        this.filterType = filterType;
        this.filterCase = filterCase;
        this.action = action;
        this.replace = (action == FilterAction.REPLACE);
    }

    public String applyFilter(String message) {
        String[] parts = splitPrefixAndBody(message);
        String prefix = parts[0];
        String body = parts[1];
        
        boolean matched = false;

        FilterAction currentAction = action != null ? action : (replace ? FilterAction.REPLACE : FilterAction.CANCEL);

        List<String> sortedWords = new ArrayList<>(filterWords);
        sortedWords.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String word : sortedWords) {
            if (word == null || word.isEmpty()) continue;

            String regexStr = buildRegex(word, filterType);
            int flags = 0;
            if (filterCase == FilterCase.INSENSITIVE) {
                flags |= Pattern.CASE_INSENSITIVE;
            }

            Pattern pattern = Pattern.compile(regexStr, flags);
            Matcher matcher = pattern.matcher(body);

            if (matcher.find()) {
                matched = true;
                if (currentAction == FilterAction.REPLACE) {
                    body = matcher.replaceAll("");
                } else if (currentAction == FilterAction.CENSOR) {
                    StringBuffer sb = new StringBuffer();
                    matcher.reset();
                    while (matcher.find()) {
                        String match = matcher.group();
                        int visibleLength = net.minecraft.util.StringUtils.stripControlCodes(match).length();
                        StringBuilder asterisks = new StringBuilder();
                        for (int k = 0; k < visibleLength; k++) asterisks.append("*");
                        matcher.appendReplacement(sb, asterisks.toString());
                    }
                    matcher.appendTail(sb);
                    body = sb.toString();
                } else {
                    return null;
                }
            }
        }

        if (matched) {
            if (currentAction == FilterAction.REPLACE) {
                body = body.replaceAll(" {2,}", " ");
            }

            String unformattedBody = net.minecraft.util.StringUtils.stripControlCodes(body).trim();
            if (unformattedBody.isEmpty()) {
                return null;
            }

            return prefix + body.trim();
        }
        return message;
    }

    private String[] splitPrefixAndBody(String msg) {
        String unformatted = net.minecraft.util.StringUtils.stripControlCodes(msg);
        
        int colonIdx = unformatted.indexOf(": ");
        if (colonIdx != -1 && colonIdx < 48) {
            return splitAt(msg, colonIdx + 1);
        }
        
        if (unformatted.startsWith("<")) {
            int bracketIdx = unformatted.indexOf("> ");
            if (bracketIdx != -1 && bracketIdx < 32) {
                return splitAt(msg, bracketIdx + 1);
            }
        }
        
        int arrowsIdx = unformatted.indexOf("» ");
        if (arrowsIdx != -1 && arrowsIdx < 48) {
            return splitAt(msg, arrowsIdx + 1);
        }
        
        int arrow2Idx = unformatted.indexOf("-> ");
        if (arrow2Idx != -1 && arrow2Idx < 48) {
            return splitAt(msg, arrow2Idx + 2);
        }

        return new String[]{"", msg};
    }

    private String[] splitAt(String msg, int unformattedSplitIdx) {
        int unformLen = 0;
        int formIdx = 0;
        while (formIdx < msg.length()) {
            if (unformLen == unformattedSplitIdx + 1) { 
                break;
            }
            if (msg.charAt(formIdx) == '§' && formIdx + 1 < msg.length()) {
                formIdx += 2;
            } else {
                unformLen++;
                formIdx++;
            }
        }
        if (formIdx < msg.length()) {
            return new String[]{msg.substring(0, formIdx), msg.substring(formIdx)};
        }
        return new String[]{"", msg};
    }

    private String buildRegex(String word, FilterMode type) {
        StringBuilder regex = new StringBuilder();
        String colorRegex = "(?:§[0-9a-fk-orA-FK-OR])*";

        if (type == FilterMode.STARTS) {
            regex.append("^").append(colorRegex);
        }

        List<String> tokens = new ArrayList<>();
        boolean escape = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (escape) {
                tokens.add(String.valueOf(c));
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '&' && i + 1 < word.length()) {
                char next = word.charAt(i + 1);
                if ("0123456789abcdefklmnorABCDEFKLMNOR".indexOf(next) != -1) {
                    tokens.add("§" + next);
                    i++;
                } else {
                    tokens.add(String.valueOf(c));
                }
            } else {
                tokens.add(String.valueOf(c));
            }
        }

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.startsWith("§") && token.length() == 2) {
                regex.append(token).append(colorRegex);
            } else {
                regex.append(Pattern.quote(token));
                if (i < tokens.size() - 1) {
                    regex.append(colorRegex);
                }
            }
        }

        if (type == FilterMode.ENDS) {
            regex.append(colorRegex).append("$");
        }

        return regex.toString();
    }
}
