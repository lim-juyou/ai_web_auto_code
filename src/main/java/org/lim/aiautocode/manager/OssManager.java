package org.lim.aiautocode.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.lim.aiautocode.config.OssClientConfig;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 阿里云OSS管理器
 */
@Component
@Slf4j
public class OssManager {

    @Resource
    private OssClientConfig ossClientConfig;

    @Resource
    private OSS ossClient;

    /**
     * 上传对象
     *
     * @param key  唯一键 (即OSS中的完整文件路径)
     * @param file 要上传的文件
     * @return 上传结果
     */
    public PutObjectResult putObject(String key, File file) {
        // 使用ossClient.putObject上传文件
        return ossClient.putObject(ossClientConfig.getBucketName(), key, file);
    }

    /**
     * 上传文件到 OSS 并返回访问 URL
     *
     * @param key  OSS对象键（完整路径）
     * @param file 要上传的文件
     * @return 文件的访问URL，失败返回null
     */
    public String uploadFile(String key, File file) {
        try {
            // 上传文件
            PutObjectResult result = putObject(key, file);
            if (result != null) {
                // 根据是否有自定义域名构建URL
                String url;
//                if (ossClientConfig.getHost() != null && !ossClientConfig.getHost().isEmpty()) {
//                    // 如果使用自定义域名
//                    url = String.format("https://%s/%s", ossClientConfig.getHost(), key);
//                } else {
                    // 如果没有自定义域名，使用默认的Bucket域名
                    url = String.format("https://%s.%s/%s", ossClientConfig.getBucketName(), ossClientConfig.getEndpoint(), key);
//                }
                log.info("文件上传OSS成功: {} -> {}", file.getName(), url);
                return url;
            }
        } catch (Exception e) {
            log.error("文件上传OSS失败: {}", e.getMessage(), e);
        }
        return null;
    }
}