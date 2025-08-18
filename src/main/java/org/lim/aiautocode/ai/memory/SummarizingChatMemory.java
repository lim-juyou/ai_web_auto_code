package org.lim.aiautocode.ai.memory;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.lim.aiautocode.ai.services.AiSummarizerService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Builder
public class SummarizingChatMemory implements ChatMemory {

    private final MessageWindowChatMemory delegate;
    private final AiSummarizerService aiSummarizerService;
    private final int maxMessagesBeforeSummarization; // 触发总结前的最大非系统消息数 (例如20)
    private final int messagesToSummarize; // 需要总结的最旧非系统消息数 (例如15)

    /**
     * 构造函数。
     *
     * @param delegate 实际存储消息的底层 ChatMemory (例如 MessageWindowChatMemory)。
     * 其 maxMessages 应该设置得足够大，以容纳系统消息 + maxMessagesBeforeSummarization 的非系统消息，
     * 例如 `maxMessagesBeforeSummarization + 1`。
     * @param aiSummarizerService 用于总结文本的服务。
     * @param maxMessagesBeforeSummarization 触发总结前的最大非系统消息数。
     * @param messagesToSummarize 达到阈值时需要总结的最旧非系统消息数。
     */
    public SummarizingChatMemory(MessageWindowChatMemory delegate, AiSummarizerService aiSummarizerService,
                                 int maxMessagesBeforeSummarization, int messagesToSummarize) {
        this.delegate = delegate;
        this.aiSummarizerService = aiSummarizerService;
        this.maxMessagesBeforeSummarization = maxMessagesBeforeSummarization;
        this.messagesToSummarize = messagesToSummarize;

        if (messagesToSummarize <= 0 || messagesToSummarize >= maxMessagesBeforeSummarization) {
            throw new IllegalArgumentException("messagesToSummarize must be between 1 and maxMessagesBeforeSummarization - 1");
        }
    }

    @Override
    public Object id() {
        return delegate.id();
    }

    @Override
    public List<ChatMessage> messages() {
        return delegate.messages();
    }

    // 重写 ChatMemory 接口的 add 方法
    @Override
    public void add(ChatMessage message) {
        // 1. 获取当前代理中的所有消息，并加入新消息。
        // 创建一个临时列表，包含 delegate 当前的消息以及新传入的消息。
        List<ChatMessage> currentMessagesWithNew = new ArrayList<>(delegate.messages());
        currentMessagesWithNew.add(message);

        // 2. 分离系统消息和非系统消息。
        SystemMessage systemMessage = null;
        List<ChatMessage> nonSystemMessages = new ArrayList<>();

        for (ChatMessage msg : currentMessagesWithNew) {
            if (msg instanceof SystemMessage) {
                // 如果找到系统消息，保存它
                systemMessage = (SystemMessage) msg;
            } else {
                // 其他所有类型的消息都视为非系统消息
                nonSystemMessages.add(msg);
            }
        }

        List<ChatMessage> finalMessagesList = new ArrayList<>();
        boolean summarizationAttempted = false; // Track if we tried to summarize
        boolean summarizationSuccessful = false; // Track if summarization succeeded

        // 3. 根据非系统消息的数量判断是否需要进行总结。
        // 如果非系统消息的数量达到或超过了预设的总结阈值，则触发总结。
        if (nonSystemMessages.size() >= maxMessagesBeforeSummarization) {
            summarizationAttempted = true; // We are attempting summarization
            log.info("AppId: {} 的非系统聊天记忆达到 {} 条。触发总结。", delegate.id(), nonSystemMessages.size());

            try {
                // 提取需要总结的最旧的非系统消息
                List<ChatMessage> messagesForSummarization = new ArrayList<>();
                for (int i = 0; i < Math.min(messagesToSummarize, nonSystemMessages.size()); i++) {
                    messagesForSummarization.add(nonSystemMessages.get(i));
                }

                // 将消息列表中的文本内容提取并拼接成用于摘要的文本
                // 该函数处理不同类型的聊天消息：
                // - 对于UserMessage，提取其中所有TextContent的文本内容并用空格连接
                // - 对于AiMessage，直接获取其文本内容
                // - 对于其他不支持的消息类型，记录警告日志并跳过
                // 最终将所有非空文本用换行符连接成一个字符串
                String textToSummarize = messagesForSummarization.stream()
                        .map(msg -> {
                            if (msg instanceof UserMessage) {
                                // For UserMessage, iterate its contents and extract text from TextContent
                                return ((UserMessage) msg).contents().stream()
                                        .filter(content -> content instanceof TextContent)
                                        .map(content -> ((TextContent) content).text())
                                        .collect(Collectors.joining(" ")); // Join multiple text parts with a space
                            } else if (msg instanceof AiMessage) {
                                // AiMessage directly has a text method
                                return ((AiMessage) msg).text();
                            }
                            // If other ChatMessage types are encountered (e.g., ToolMessage, ImageMessage without text)
                            log.warn("Attempted to summarize unsupported ChatMessage type: {}. Skipping its text for summarization.", msg.type());
                            return ""; // Return an empty string for unsupported types
                        })
                        .filter(text -> !text.isEmpty()) // Now 'text' is always a String, so isEmpty() is valid
                        .collect(Collectors.joining("\n")); // Join message texts with a newline


                String summarizedText = aiSummarizerService.summarize(textToSummarize);
                SystemMessage summarizedChatMessage = new SystemMessage("Previous conversation summary: " + summarizedText);

                // 4. 构建新的精简后的消息列表：系统消息 (如果存在) + 总结消息 + 剩余的非系统消息
                if (systemMessage != null) {
                    finalMessagesList.add(systemMessage); // 系统消息永远在最前面
                }
                finalMessagesList.add(summarizedChatMessage); // 添加新生成的总结消息

                // 添加未被总结的剩余非系统消息
                for (int i = messagesToSummarize; i < nonSystemMessages.size(); i++) {
                    finalMessagesList.add(nonSystemMessages.get(i));
                }

                log.debug("AppId: {} 聊天记忆在总结后：共 {} 条消息。新总数：{}", delegate.id(), finalMessagesList.size());
                summarizationSuccessful = true; // Summarization succeeded

            } catch (Exception e) {
                // Catch any exception during summarization
                log.error("AppId: {} 总结服务调用失败。将跳过本轮总结，直接进行截断。", delegate.id(), e);
                // Summarization failed, so we'll fall through to the 'else' block
                // which handles non-summarized scenarios.
            }
        }

        // 5. 根据总结是否成功或是否需要总结，来更新 delegate 的记忆。
        if (summarizationAttempted && summarizationSuccessful) {
            // 如果尝试了总结并且成功了，就用总结后的列表更新
            delegate.clear();
            delegate.add(finalMessagesList); // 使用 delegate 的 add(Iterable<ChatMessage> messages) 方法
        } else {
            // 否则 (没触发总结，或者总结失败了)，就直接使用 currentMessagesWithNew
            // MessageWindowChatMemory 会根据自己的 maxMessages 进行截断
            delegate.clear(); // 先清空，再添加，确保最新的消息在最后且截断由 delegate 负责
            delegate.add(currentMessagesWithNew);
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}