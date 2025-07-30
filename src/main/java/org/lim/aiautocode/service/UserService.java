package org.lim.aiautocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.lim.aiautocode.model.dto.UserAddRequest;
import org.lim.aiautocode.model.dto.UserLoginRequest;
import org.lim.aiautocode.model.dto.UserQueryRequest;
import org.lim.aiautocode.model.dto.UserRegisterRequest;
import org.lim.aiautocode.model.entity.User;
import org.lim.aiautocode.model.vo.LoginUserVO;
import org.lim.aiautocode.model.vo.UserVO;

import java.util.List;

/**
 * 用户表 服务层。
 *
 * @author lim
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册参数
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录参数
     * @return 脱敏的登录用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return 脱敏的已登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 注销结果
     */
    //改用Spring Security实现
//    boolean userLogout(HttpServletRequest request);
    /**
     * 获取脱敏的用户信息
     *
     * @param user  用户
     * @return 脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);
    /**
     * 获取查询条件
     *
     * @param userQueryRequest 查询参数
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 添加用户
     *
     * @param userAddRequest 添加用户参数
     * @return 新用户 id
     */
    Long add(UserAddRequest userAddRequest);

    /**
     * 更具Id获取用户信息（脱敏）
     * @param id 用户Id
     *  */
    UserVO getUserVOById(long id);
}

