package org.lim.aiautocode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import org.lim.aiautocode.model.dto.history.ChatHistoryQueryRequest;
import org.lim.aiautocode.model.entity.ChatHistory;
import org.lim.aiautocode.model.entity.User;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author lim
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史
     *
     * @param appId     应用ID
     * @param message   对话内容
     * @param messageType   对话类型
     * @param userId    用户ID
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用ID删除对话历史
     *
     * @param appId 应用ID
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 根据应用ID分页查询对话历史
     *
     * @param appId 应用ID
     * @param pageSize 每页大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser 登录用户
     * @return 对话历史列表
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest 查询参数
     * @return 查询包装类
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
