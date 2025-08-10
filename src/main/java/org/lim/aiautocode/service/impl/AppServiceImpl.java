package org.lim.aiautocode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.lim.aiautocode.core.AiCodeGeneratorFacade;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;
import org.lim.aiautocode.exception.ThrowUtils;
import org.lim.aiautocode.mapper.AppMapper;
import org.lim.aiautocode.model.dto.app.AppAddRequest;
import org.lim.aiautocode.model.dto.app.AppQueryRequest;
import org.lim.aiautocode.model.entity.App;
import org.lim.aiautocode.model.entity.User;
import org.lim.aiautocode.model.enums.CodeGenTypeEnum;
import org.lim.aiautocode.model.vo.app.AppVO;
import org.lim.aiautocode.model.vo.user.UserVO;
import org.lim.aiautocode.service.AppService;
import org.lim.aiautocode.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author lim
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {
    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    /**
     * 用户与应用对话生成代码
     *
     * @param appId     应用ID
     * @param message   用户输入的消息
     * @param LoginUser 当前登录用户
     * @return 生成的代码流
     */
    public Flux<String> chatToGenCode(Long appId, String message, User LoginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");

        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 验证用户是否有权限访问应用，仅创建者可以生成代码
        ThrowUtils.throwIf(!app.getUserId().equals(LoginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限操作");

        // 4. 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "代码生成类型错误");

        // 5. 调用AI代码生成服务，生成并保存代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, app.getId());
    }

    /**
     * 创建应用
     *
     * @param request 创建应用的请求参数
     * @return 新建的应用ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createApp(AppAddRequest request, Long userId) {
        // 参数业务校验
        String initPrompt = request.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");

        // 构造 App
        App app = new App();
        BeanUtil.copyProperties(request, app);
        app.setUserId(userId);
        app.setAppName(StrUtil.sub(initPrompt, 0, 12));
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());

        // 保存
        boolean saved = this.save(app);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR);

        return app.getId();
    }

    /**
     * 获取 AppVO
     *
     * @param app 应用
     * @return AppVO
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }
    /**
     * 获取应用视图（脱敏）对象列表。
     *
     * @param appList 应用列表
     * @return 应用视图（脱敏）对象列表
     */

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 构建查询请求参数
     * @param appQueryRequest 应用查询条件对象
     * @return 查询条件对象
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


}
