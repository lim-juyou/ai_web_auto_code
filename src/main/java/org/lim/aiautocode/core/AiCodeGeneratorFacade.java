package org.lim.aiautocode.core;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lim.aiautocode.ai.AiCodeGeneratorService;
import org.lim.aiautocode.ai.model.HtmlCodeResult;
import org.lim.aiautocode.ai.model.MultiFileCodeResult;
import org.lim.aiautocode.core.parse.CodeParserExecutor;
import org.lim.aiautocode.core.saver.CodeFileSaverExecutor;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;
import org.lim.aiautocode.model.enums.CodeGenTypeEnum;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
//    @Resource
//    private AiSummarizerService aiSummarizerService;

    /**
     * 根据用户消息和代码生成类型，生成相应的代码并保存到文件
     *
     * @param userMessage 用户输入的消息，用于指导代码生成
     * @param codeGenType 代码生成类型枚举，指定要生成的代码类型
     * @return 生成并保存后的代码文件对象
     * @throws BusinessException        当生成类型为空时抛出业务异常
     * @throws IllegalArgumentException 当代码生成类型无效时抛出非法参数异常
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        // 参数校验，确保生成类型不为空
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }

        // 根据不同的代码生成类型，调用相应的生成和保存逻辑
        return switch (codeGenType) {
            case HTML -> {
                // 生成HTML代码并保存
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                // 生成多文件代码并保存
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenType.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }
    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 流式返回生成的代码字符串
     */
    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用代码解析执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用代码保持执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    //   <editor-fold desc="之前废弃的流式处理方法">
    /* *//**
     * 生成并保存HTML代码流
     *
     * @param userMessage 用户输入的消息，用于生成HTML代码的提示
     * @return 返回生成的HTML代码流，以Flux<String>形式提供实时响应
     *//*
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> generateHtmlCodeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        StringBuilder codeBuilder = new StringBuilder();

        // 收集流中的代码片段并构建完整代码
        return generateHtmlCodeStream
                .doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    try {
                        String completeHtmlCode = codeBuilder.toString();
                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
//                        // 2. 如果存在描述，就调用AI进行提炼(或者描述大于多少，进行精简)
//                        if (htmlCodeResult.getDescription() != null && !htmlCodeResult.getDescription().trim().isEmpty()) {
//                            String longDescription = htmlCodeResult.getDescription();
//                            String summarizedDescription = aiSummarizerService.summarize(longDescription);
//                            htmlCodeResult.setDescription(summarizedDescription); // 用精简的描述覆盖原来的
//                        }
                        //保存到文件
                        File savedDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                        log.info("保存成功：{}", savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败：{}", e.getMessage());
                    }
                });
    }

    *//**
     * 生成并保存多文件代码流
     *
     * @param userMessage 用户输入的消息，用于生成代码的提示信息
     * @return 返回生成的代码流，类型为Flux<String>
     *//*
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage){
        // 调用AI代码生成服务生成多文件代码流
        Flux<String> generateHtmlCodeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        StringBuilder codeBuilder = new StringBuilder();
        return generateHtmlCodeStream
                .doOnNext(codeBuilder::append)
                // 在流完成时解析并保存生成的代码
                .doOnComplete(() -> {
                    try{
                        String completeMultiFileCode = codeBuilder.toString();
                        // 解析完整的多文件代码
                        MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(completeMultiFileCode);
                        //保存到文件
                        File savedDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                        log.info("保存成功：{}", savedDir.getAbsolutePath());
                    }catch (Exception e){
                        log.error("保存失败：{}", e.getMessage());
                    }
                });
    }
*/
    //</editor-fold>


}
