package org.lim.aiautocode.service;

public interface ScreenshotService {
    /**
     *  截取网页截图并上传
     * @param webUrl 网页地址
     * @return OSS URL
     */
    String generateAndUploadScreenshot(String webUrl);
}
