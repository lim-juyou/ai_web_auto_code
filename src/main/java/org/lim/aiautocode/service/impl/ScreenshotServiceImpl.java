package org.lim.aiautocode.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lim.aiautocode.exception.ErrorCode;
import org.lim.aiautocode.exception.ThrowUtils;
import org.lim.aiautocode.manager.OssManager;
import org.lim.aiautocode.service.ScreenshotService;
import org.lim.aiautocode.utils.WebScreenshotUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {
    @Resource
    private OssManager ossManager;
    /**
     * 生成网页截图并上传到对象存储
     * @param webUrl 网页地址
     * @return OSS URL
     */
    @Override
    public String generateAndUploadScreenshot(String webUrl){
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页地址不能为空");
        log.info("开始生成网页截图");
        // 1.生成本地截图
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.SYSTEM_ERROR, "生成网页截图失败");
        try{
            // 2.上传到OSS
            String ossUrl = uploadScreenshotToOss(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(ossUrl), ErrorCode.SYSTEM_ERROR, "上传截图到OSS失败");
            log.info("上传截图成功，截图地址为：{}", ossUrl);
            return ossUrl;
        }finally {
            // 3.清理本地文件
            cleanLocalScreenshot(localScreenshotPath);
        }
    }


    /**
     * 上传截图到对象存储
     * @param localScreenshotPath 本地截图路径
     * @return OSS URL
     */
    private String uploadScreenshotToOss(String localScreenshotPath) {
        if(StrUtil.isBlank(localScreenshotPath)){
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if(!screenshotFile.exists()){
            log.error("截图文件不存在：{}" ,  localScreenshotPath);
            return null;
        }
        String fileName = RandomUtil.randomString(16) + "_compressed.jpg";
       String ossKey = generateScreenshotKey(fileName);
       return ossManager.uploadFile(ossKey, screenshotFile);

    }

    /**
     * 生成截图的对象存储键
     * 格式：/screenshots/YY/MM/DD/filename.jpg
     * @param fileName 文件名
     * @return 对象存储键
     */

    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 清理本地文件
     * @param localScreenshotPath 本地截图路径
     */
    private void cleanLocalScreenshot(String localScreenshotPath) {
        File screenshotFile = new File(localScreenshotPath);
        if(screenshotFile.exists()){
            File parentFile = screenshotFile.getParentFile();
            FileUtil.del(parentFile);
            log.info("删除本地截图成功：{}", localScreenshotPath);

        }
    }

}
