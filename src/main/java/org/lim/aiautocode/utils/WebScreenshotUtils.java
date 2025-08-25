package org.lim.aiautocode.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

/**
 * 网页截图工具类，使用 ThreadLocal 确保 WebDriver 的线程安全性
 *
 * @author yupi
 */
@Slf4j
public class WebScreenshotUtils {

    // 使用 ThreadLocal 存储 WebDriver 实例，确保每个线程独立
    private static final ThreadLocal<WebDriver> threadDriver = new ThreadLocal<>();
    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;
    private static final float COMPRESS_QUALITY = 0.3f;
    private static final String IMAGE_SUFFIX = ".png";
    private static final String COMPRESSED_IMAGE_SUFFIX = ".jpg";


    /**
     * 初始化或获取当前线程的 Chrome 浏览器驱动
     *
     * @return 当前线程的 WebDriver 实例
     */
    private static WebDriver getOrInitDriver() {
        WebDriver driver = threadDriver.get();
        if (driver == null) {
            try {
                // 自动管理 ChromeDriver
                WebDriverManager.chromedriver().setup();
                // 配置 Chrome 选项
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless");
                options.addArguments("--disable-gpu");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments(String.format("--window-size=%d,%d", DEFAULT_WIDTH, DEFAULT_HEIGHT));
                options.addArguments("--disable-extensions");
                options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                // 创建驱动
                driver = new ChromeDriver(options);
                driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15));
                threadDriver.set(driver); // 存入 ThreadLocal
            } catch (Exception e) {
                log.error("初始化 Chrome 浏览器失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
            }
        }
        return driver;
    }

    /**
     * 关闭当前线程的 WebDriver 实例并清理 ThreadLocal
     */
    private static void quitDriver() {
        WebDriver driver = threadDriver.get();
        if (driver != null) {
            driver.quit();
            threadDriver.remove(); // 清除线程本地变量，防止内存泄漏
        }
    }

    /**
     * 等待页面加载完成
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 等待 document.readyState 为 complete
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.warn("等待页面加载时出现异常，继续执行截图", e);
        }
    }

    /**
     * 保存图片
     *
     * @param imageBytes 图片字节数组
     * @param filePath   文件保存路径
     */
    private static void saveImage(byte[] imageBytes, String filePath) {
        try {
            FileUtil.writeBytes(imageBytes, filePath);
        } catch (Exception e) {
            log.error("保存图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片
     *
     * @param originalImagePath 原始图片路径
     * @param compressedImagePath 压缩图片路径
     */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        try {
            ImgUtil.compress(new File(originalImagePath), new File(compressedImagePath), COMPRESS_QUALITY);
        } catch (Exception e) {
            log.error("图片压缩失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }

    /**
     * 网页截图并保存
     *
     * @param webUrl 网页地址
     * @return 压缩后的图片文件路径，失败返回 null
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页地址不能为空");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "网页地址不能为空");
        }

        WebDriver driver = getOrInitDriver();
        try {
            // 创建临时目录
            String rootPath = System.getProperty("user.dir") + File.separator + "temp" + File.separator + "screenshot" + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 原始截图文件路径
            String originalImagePath = rootPath + File.separator + RandomUtil.randomString(5) + IMAGE_SUFFIX;

            // 访问网页
            driver.get(webUrl);
            // 等待页面加载完成
            waitForPageLoad(driver);
            // 截图
            byte[] imageBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            // 保存原始图片
            saveImage(imageBytes, originalImagePath);
            log.info("原始图片保存成功：{}", originalImagePath);

            // 压缩图片
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomString(5) + COMPRESSED_IMAGE_SUFFIX;
            compressImage(originalImagePath, compressedImagePath);
            log.info("压缩图片保存成功：{}", compressedImagePath);

            // 删除原始图片，只保留压缩图片
            FileUtil.del(originalImagePath);
            return compressedImagePath;

        } catch (Exception e) {
            log.error("保存网页截图失败", e);
            return null;
        } finally {
            // 确保在任何情况下都关闭 WebDriver，防止资源泄露
            quitDriver();
        }
    }
}