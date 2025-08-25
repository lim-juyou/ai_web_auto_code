package org.lim.aiautocode.manager;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
class OssManagerTest {
    @Resource
    private OssManager ossManager;

    @Test
    void putObject() {
    }

    @Test
    void uploadFile() {
        String url = ossManager.uploadFile("test.txt", new File("D:\\dev\\workspace\\Jworkspace\\aiautocode\\ai-auto-code\\temp\\screenshot\\d5ddd034\\6vS9Y.jpg"));
        Assertions.assertNotNull(url);
    }
}