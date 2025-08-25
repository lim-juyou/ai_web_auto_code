package org.lim.aiautocode.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WebScreenshotUtilsTest {


    @Test
    void saveWebPageScreenshot() {
        String testUrl = "http://joyoulim.top/";
        String webPageScreenshot = WebScreenshotUtils.saveWebPageScreenshot(testUrl);
        Assertions.assertNotNull(webPageScreenshot);
    }


}