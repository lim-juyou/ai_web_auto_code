package org.lim.aiautocode.service;

import com.mybatisflex.core.service.IService;
import org.lim.aiautocode.model.dto.UserRegisterRequest;
import org.lim.aiautocode.model.entity.User;

/**
 * 用户表 服务层。
 *
 * @author lim
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userRegisterRequest 注册参数
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

}

