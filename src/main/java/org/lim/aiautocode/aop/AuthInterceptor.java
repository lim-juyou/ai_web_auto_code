package org.lim.aiautocode.aop;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.lim.aiautocode.annotation.AuthCheck;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;
import org.lim.aiautocode.model.entity.User;
import org.lim.aiautocode.model.enums.UserRoleEnum;
import org.lim.aiautocode.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1. 从注解中获取所需的角色列表
        String[] requiredRoles = authCheck.requiredRoles();

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 2. 获取当前登录用户，并进行健壮性检查
//        User loginUser = userService.getLoginUser(request);
        User loginUser = userService.getLoginUser();
        if (loginUser == null) {

            // 抛出明确的“未登录”异常，而不是让程序崩溃
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 3. 如果注解上没有定义任何角色，说明只需登录即可访问，直接放行
        if (ObjectUtils.isEmpty(requiredRoles)) {
            return joinPoint.proceed();
        }

        // 4. 获取当前用户的角色
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            // 如果用户有账号但没有分配角色
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 5. 判断用户角色是否在要求的角色列表之内 (核心校验逻辑)
        List<String> requiredRoleList = Arrays.asList(requiredRoles);
        if (!requiredRoleList.contains(userRoleEnum.getValue())) {
            // 如果用户角色不在所需角色列表中，拒绝访问
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 6. 通过权限校验，放行
        return joinPoint.proceed();
    }
}