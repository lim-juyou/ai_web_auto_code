package org.lim.aiautocode.core.parse;

import org.lim.aiautocode.ai.model.HtmlCodeResult;
import org.lim.aiautocode.core.parse.utils.ParserUtils;


/**
 * HTML代码解析器
 */
public class HtmlCodeParser implements CodeParseStrategy<HtmlCodeResult> {
//    将正则和匹配工具统一抽象到ParserUtils中

//    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = ParserUtils.extractCodeByPattern(codeContent, ParserUtils.HTML_CODE_PATTERN);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果没有找到代码块，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        // 解析并设置描述信息
        String description = ParserUtils.extractDescription(codeContent);
        result.setDescription(description);

        return result;
    }


    //   <editor-fold desc="之前废弃的提取HTML代码内容方法">
    /**
     * 提取HTML代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
   /*
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }*/
    //<editor-fold>
}
