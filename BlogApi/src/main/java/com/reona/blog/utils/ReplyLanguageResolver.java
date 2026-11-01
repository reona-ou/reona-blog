package com.reona.blog.utils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReplyLanguageResolver {

    private static final Pattern ZH_REQUEST = Pattern.compile(
            "(?:用|以|改用|改说|改成|换成|换到|切换成|切换为|切回|回到|恢复成|恢复为)\\s*(英语|英文|日语|日文|中文|汉语|漢語)"
                    + "(?:\\s*(?:来)?\\s*(?:回答|回复|作答|说|写|交流|沟通))?"
                    + "|(?:英语|英文|日语|日文|中文|汉语|漢語)\\s*(?:回答|回复|作答|交流)");

    private static final Pattern EN_REQUEST = Pattern.compile(
            "(?i)\\b(?:answer|reply|respond|speak|write|talk|continue|say\\s+it)\\b[^.?!\\n]{0,30}?\\bin\\s+(english|japanese|chinese)\\b"
                    + "|(?i)\\b(?:in\\s+)?(english|japanese|chinese)\\s+please\\b"
                    + "|(?i)\\b(?:switch|change)\\s+(?:to\\s+)?(english|japanese|chinese)\\b");

    private static final Pattern JA_REQUEST = Pattern.compile(
            "(英語|日本語|中国語|中文)\\s*で\\s*(?:回答|答えて|答える|話して|返信|お願い)"
                    + "|(英語|日本語|中国語|中文)\\s*(?:に|へ)\\s*(?:切り替え|変更)");

    private ReplyLanguageResolver() {
    }

    public static String resolve(String defaultLanguage, List<Map<String, Object>> history) {
        String fallback = normalize(defaultLanguage);
        if (history == null || history.isEmpty()) {
            return fallback;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> message = history.get(i);
            if (message == null || !"user".equals(String.valueOf(message.get("role")))) {
                continue;
            }
            Object content = message.get("content");
            String text = content == null ? "" : String.valueOf(content);
            String requested = detect(text);
            return requested == null ? fallback : requested;
        }
        return fallback;
    }

    private static String detect(String text) {
        Matcher zh = ZH_REQUEST.matcher(text);
        if (zh.find()) {
            return languageOf(zh.group(1));
        }
        Matcher en = EN_REQUEST.matcher(text);
        if (en.find()) {
            String name = en.group(1) != null ? en.group(1)
                    : en.group(2) != null ? en.group(2) : en.group(3);
            return languageOf(name);
        }
        Matcher ja = JA_REQUEST.matcher(text);
        if (ja.find()) {
            return languageOf(ja.group(1) != null ? ja.group(1) : ja.group(2));
        }
        return null;
    }

    private static String languageOf(String name) {
        if (name == null) {
            return null;
        }
        String value = name.toLowerCase();
        if (value.contains("english") || value.contains("英语") || value.contains("英文")
                || value.contains("英語")) {
            return "en";
        }
        if (value.contains("japanese") || value.contains("日语") || value.contains("日文")
                || value.contains("日本語")) {
            return "ja";
        }
        if (value.contains("chinese") || value.contains("中文") || value.contains("汉语")
                || value.contains("漢語") || value.contains("中国語")) {
            return "zh";
        }
        return null;
    }

    private static String normalize(String language) {
        if ("en".equalsIgnoreCase(language)) {
            return "en";
        }
        if ("ja".equalsIgnoreCase(language)) {
            return "ja";
        }
        return "zh";
    }
}
