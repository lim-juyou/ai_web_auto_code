package org.lim.aiautocode.core.parse;

import org.lim.aiautocode.ai.model.MultiFileCodeResult;
import org.lim.aiautocode.core.parse.utils.ParserUtils;

import java.util.function.Consumer;

/**
 * 
 */
public class MultiFileCodeParser implements CodeParseStrategy<MultiFileCodeResult> {
    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        // 提取各类代码
        String htmlCode = ParserUtils.extractCodeByPattern(codeContent, ParserUtils.HTML_CODE_PATTERN);
        String cssCode = ParserUtils.extractCodeByPattern(codeContent, ParserUtils.CSS_CODE_PATTERN);
        String jsCode = ParserUtils.extractCodeByPattern(codeContent, ParserUtils.JS_CODE_PATTERN);
        //   <editor-fold desc="废弃的设置各类代码">
           /* // 设置HTML代码
            if (htmlCode != null && !htmlCode.trim().isEmpty()) {
                result.setHtmlCode(htmlCode.trim());
            }
            // 设置CSS代码
            if (cssCode != null && !cssCode.trim().isEmpty()) {
                result.setCssCode(cssCode.trim());
            }
            // 设置JS代码
            if (jsCode != null && !jsCode.trim().isEmpty()) {
                result.setJsCode(jsCode.trim());
            }*/
        // </editor-fold>
        // 设置各类代码
        setCodeIfValid(result::setHtmlCode, htmlCode);
        setCodeIfValid(result::setCssCode, cssCode);
        setCodeIfValid(result::setJsCode, jsCode);


        // 解析并设置描述信息
        String description = ParserUtils.extractDescription(codeContent);
        result.setDescription(description);

        return result;
    }

    /**
     * 如果代码内容有效，则设置代码
     *
     * @param setter      代码设置方法
     * @param codeContent 代码内容
     */
    private void setCodeIfValid(Consumer<String> setter, String codeContent) {
        if (codeContent != null && !codeContent.trim().isEmpty()) {
            setter.accept(codeContent.trim());
        }
    }

}
