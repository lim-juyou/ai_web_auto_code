package org.lim.aiautocode.ai.services;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiSummarizerServiceConfig {

    @Resource
    private ChatModel chatModel;

    /**
     * Ai总结服务Bean
     * @return AI总结服务
     */
    @Bean
    public AiSummarizerService aiSummarizerService() {
        return AiServices.builder(AiSummarizerService.class)
                .chatModel(chatModel)
                .build();
    }
}