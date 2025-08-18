package org.lim.aiautocode.ai.services;

import dev.langchain4j.service.SystemMessage;

/**
 * 描述信息总结AIService,可以选一个轻量级的AI
 */
public interface AiSummarizerService {

    @SystemMessage(fromResource = "prompt/summarizer-system-prompt.txt")
    String summarize(String textToSummarize);
}