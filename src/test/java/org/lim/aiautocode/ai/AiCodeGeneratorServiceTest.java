package org.lim.aiautocode.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lim.aiautocode.ai.AiCodeGeneratorService;
import org.lim.aiautocode.ai.model.HtmlCodeResult;
import org.lim.aiautocode.ai.model.MultiFileCodeResult;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("生成个helloword网页");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个网页的留言板");
        Assertions.assertNotNull(multiFileCode);
    }
}
