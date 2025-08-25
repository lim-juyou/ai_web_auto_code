package org.lim.aiautocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import org.lim.aiautocode.model.dto.app.AppAddRequest;
import org.lim.aiautocode.model.dto.app.AppQueryRequest;
import org.lim.aiautocode.model.entity.App;
import org.lim.aiautocode.model.entity.User;
import org.lim.aiautocode.model.vo.app.AppVO;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author lim
 */
public interface AppService extends IService<App> {
    Flux<String> chatToGenCode(Long appId, String message, User LoginUser);


    /**
     * 创建应用。
     *
     * @param request 创建应用请求
     * @param userId 用户ID
     * @return 应用ID
     */
    @Transactional(rollbackFor = Exception.class)
    Long createApp(AppAddRequest request, Long userId);

    /**
     * 部署应用。
     *
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署结果
     */
    @Transactional(rollbackFor = Exception.class)
    String deployApp(Long appId, User loginUser);

    /**
     * 获取应用视图（脱敏）对象。
     *
     * @param app 应用
     * @return 应用视图（脱敏）对象
     */
    AppVO getAppVO(App app);
    /**
     * 获取应用视图（脱敏）对象列表。
     *
     * @param appList 应用列表
     * @return 应用视图（脱敏）对象列表
     */

    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取查询条件对象。
     *
     * @param appQueryRequest 应用查询条件对象
     * @return 查询条件对象
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
