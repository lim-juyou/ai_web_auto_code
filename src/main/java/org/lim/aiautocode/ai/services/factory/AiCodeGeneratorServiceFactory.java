package org.lim.aiautocode.ai.services.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lim.aiautocode.ai.enums.CodeGenTypeEnum;
import org.lim.aiautocode.ai.memory.SummarizingChatMemory;
import org.lim.aiautocode.ai.services.AiCodeGeneratorService;
import org.lim.aiautocode.ai.services.AiSummarizerService;
import org.lim.aiautocode.ai.tools.ToolManager;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;
import org.lim.aiautocode.langgraph4j.node.SpringContextUtil;
import org.lim.aiautocode.service.ChatHistoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private AiSummarizerService aiSummarizerService;
    @Resource
    private ToolManager toolManager;

    /**
     * AiCodeGeneratorService缓存
     * 缓存策略：
     * 最大缓存1000个，超过1000个则移除最旧的缓存
     * 写入后30分钟内没有被访问则移除
     * 访问后10分钟内没有被访问则移除
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMinutes(30))
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .removalListener((key, value, cause) -> {
                        log.debug("AIServices 服务实例被移除，appId:{},原因：{}", key, cause);
                    })
                    .build();

    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId,codeGenType);
      return serviceCache.get(cacheKey,key->createAiCodeGeneratorService(appId,codeGenType) );
    }

    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);

        // 设置触发总结的阈值和需要总结的消息数量
        int chatMemoryMaxNonSystemMessages = 20; // 非系统消息达到20条时，触发总结
        int messagesToSummarizeCount = 15; // 总结前15条非系统消息

        // 原始的 MessageWindowChatMemory
        // 其 maxMessages 应该设置为 chatMemoryMaxNonSystemMessages + 1，
        // 以允许容纳一个系统消息和 chatMemoryMaxNonSystemMessages 条非系统消息。
        // 这样，在 MessageWindowChatMemory 自身截断之前，SummarizingChatMemory 有机会介入处理。
        MessageWindowChatMemory rawChatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(chatMemoryMaxNonSystemMessages + 1) // 设置为21 (20条非系统消息 + 1条系统消息)
                .build();

        // 从数据库加载历史对话到记忆中。
        // 这里加载的消息数量也应考虑到可能存在的系统消息。
        chatHistoryService.loadChatHistoryToMemory(appId, rawChatMemory, chatMemoryMaxNonSystemMessages + 1);

        // 使用 SummarizingChatMemory 包装 rawChatMemory
        SummarizingChatMemory summarizingChatMemory =SummarizingChatMemory.builder()
                .delegate(rawChatMemory) // 原始的 MessageWindowChatMemory
                .aiSummarizerService(aiSummarizerService)
                .maxMessagesBeforeSummarization(chatMemoryMaxNonSystemMessages)
                .messagesToSummarize(messagesToSummarizeCount)
                .build();
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> summarizingChatMemory)
                        .tools(toolManager.getAllTools())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();
            }
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(summarizingChatMemory)
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };


    }
    private String buildCacheKey(Long appId,CodeGenTypeEnum codeGenType){
        return appId+"_"+codeGenType.getValue();
    }


/**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

}