package org.lim.aiautocode.core.parse.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析的辅助工具类
 */
public class ParserUtils {
    // 将所有正则表达式常量放在这里
    public static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    public static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    public static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 提取代码描述。
     */
    public static String extractDescription(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String description = content
                .replaceAll(HTML_CODE_PATTERN.pattern(), "")
                .replaceAll(CSS_CODE_PATTERN.pattern(), "")
                .replaceAll(JS_CODE_PATTERN.pattern(), "");
        return description.trim();
    }

    /**
     * 根据正则模式提取代码
     */
    public static String extractCodeByPattern(String content, Pattern pattern) {
        if (content == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}