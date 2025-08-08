package org.lim.aiautocode.core;

import jakarta.annotation.Resource;
import org.lim.aiautocode.ai.AiCodeGeneratorService;
import org.lim.aiautocode.ai.model.HtmlCodeResult;
import org.lim.aiautocode.ai.model.MultiFileCodeResult;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;
import org.lim.aiautocode.model.enums.CodeGenTypeEnum;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
public class AiCodeGeneratorFacade {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType){
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenType) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
            }
            default -> throw new IllegalArgumentException("Invalid code generation type: " + codeGenType);
        };
    }
}
