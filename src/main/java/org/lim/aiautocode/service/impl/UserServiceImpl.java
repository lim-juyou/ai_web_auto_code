package org.lim.aiautocode.service.impl;



import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;

import com.mybatisflex.spring.service.impl.ServiceImpl;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;

import org.lim.aiautocode.common.ResultUtils;
import org.lim.aiautocode.exception.BusinessException;

import org.lim.aiautocode.exception.ErrorCode;

import org.lim.aiautocode.exception.ThrowUtils;

import org.lim.aiautocode.model.dto.UserAddRequest;
import org.lim.aiautocode.model.dto.UserLoginRequest;

import org.lim.aiautocode.model.dto.UserQueryRequest;
import org.lim.aiautocode.model.dto.UserRegisterRequest;

import org.lim.aiautocode.model.entity.User;

import org.lim.aiautocode.mapper.UserMapper;

import org.lim.aiautocode.model.enums.UserRoleEnum;

import org.lim.aiautocode.model.vo.LoginUserVO;

import org.lim.aiautocode.model.vo.UserVO;
import org.lim.aiautocode.service.UserService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


import static org.lim.aiautocode.constant.UserConstant.DEFAULT_PASSWORD;
import static org.lim.aiautocode.constant.UserConstant.USER_LOGIN_STATE;



/**

 * 用户表 服务层实现。

 *

 * @author lim

 */

@RequiredArgsConstructor

@Service

public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final PasswordEncoder passwordEncoder;


// 正则表达式，用于校验账号是否包含特殊字符

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");


    @Override

    @Transactional // 开启事务，保证注册操作的原子性

    public long userRegister(UserRegisterRequest userRegisterRequest) {

// 1. 校验参数

        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        String userAccount = userRegisterRequest.getUserAccount();

        String userPassword = userRegisterRequest.getUserPassword();

        String checkPassword = userRegisterRequest.getCheckPassword();

        validateRegistrationParams(userAccount, userPassword, checkPassword);


// 2. 检查账号是否存在 (使用 Mybatis-Flex 的 QueryWrapper)

// 加锁防止并发注册同一个账号

//String.intern() 的核心作用是：返回该字符串在“字符串常量池 (String Pool)”中的唯一实例的引用。

//

//简单来说，它是一个将字符串对象“规范化”的方法。无论你在程序的哪个地方，对于值相同的字符串，调用 intern() 后得到的将是同一个对象引用。

// 危险！如果两个线程传入的 userAccount 虽然内容相同但不是同一个对象，锁就无效了！

        synchronized (userAccount.intern()) {

            QueryWrapper queryWrapper = QueryWrapper.create()

                    .where(User::getUserAccount).eq(userAccount);


// 使用 ServiceImpl 自带的 exists 方法，比查询整个对象更高效

            if (this.exists(queryWrapper)) {

                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该账号已被注册");

            }


// 3. 【安全】使用 BCrypt 加密

            String encryptedPassword = passwordEncoder.encode(userPassword);


// 4. 保存账号

//创建用户对象

            User newUser = User.builder()

                    .userName("昵称无")

                    .userAccount(userAccount)

                    .userPassword(encryptedPassword)

                    .userRole(UserRoleEnum.USER.getValue())

                    .build();


// 使用 ServiceImpl 自带的 save 方法保存实体

            boolean saveResult = this.save(newUser);

            if (!saveResult) {

// 如果保存失败，抛出系统异常

                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试");

            }


// MyBatis-Flex在保存后会自动将生成的主键ID回填到newUser对象中

            return newUser.getId();

        }

    }


    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {

//1.校验参数

        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        String userAccount = userLoginRequest.getUserAccount();

        String userPassword = userLoginRequest.getUserPassword();

        if (StringUtils.isAnyBlank(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword())) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");

        }

        if (userAccount.length() < 4) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");

        }

//2.检查数据库是否存在

// a.查询用户是否存在

        QueryWrapper queryWrapper = QueryWrapper.create()

                .where(User::getUserAccount).eq(userAccount);

        User user = this.getOne(queryWrapper);

        if (user == null) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");

        }

// b. 验证密码

        String encryptedPasswordFromDB = user.getUserPassword();

// 使用 BCryptPasswordEncoder 的 matches 方法验证用户输入的密码与数据库中存储的加密密码是否匹配

// 参数顺序：原始密码（用户输入的明文密码），加密后的密码（数据库中存储的密码）

        boolean passwordMatch = passwordEncoder.matches(userPassword, encryptedPasswordFromDB);

// 如果密码不匹配，抛出参数错误

        if (!passwordMatch) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");

        }
        // 4. 【核心改造】记录登录态到 Spring Security
        // a. 创建一个 AuthenticationToken，这里我们不关心权限，所以第三个参数给一个空列表
        // UsernamePasswordAuthenticationToken(principal, credentials, authorities)
        // principal: 主要信息，通常是 User 对象或用户名
        // credentials: 凭证，通常是密码（因为已认证，可以设为 null）
        // authorities: 权限列表
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                user, null, Collections.emptyList()
        );

        // b. 创建一个新的 SecurityContext 并设置认证信息
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authenticationToken);

        // c. 将 SecurityContext 存入 Session。
        // Spring Security 会在后续请求中自动从 Session 的这个固定键名下读取 SecurityContext
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

//4.记录登录态

        LoginUserVO loginUserVO = getLoginUserVO(user);

//        request.getSession().setAttribute(USER_LOGIN_STATE, loginUserVO);

//5.返回脱敏用户信息

        return loginUserVO;

    }


    @Override

    public LoginUserVO getLoginUserVO(User user) {

        if (user == null) {

            return null;

        }

//或者使用工具类

// LoginUserVO loginUserVO = BeanUtil.copyProperties(user, LoginUserVO.class);

        return LoginUserVO.builder()

                .id(user.getId())

                .userAccount(user.getUserAccount())

                .userName(user.getUserName())

                .userAvatar(user.getUserAvatar())

                .userProfile(user.getUserProfile())

                .userRole(user.getUserRole())

                .createTime(user.getCreateTime())

                .updateTime(user.getUpdateTime())

                .build();

    }



    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */


    // UserServiceImpl.java

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 从 Spring Security 的上下文中获取认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 从认证信息中获取 principal（我们在登录时存入的 User 对象）
        Object principal = authentication.getPrincipal();
        User currentUser;
        if (principal instanceof User) {
            currentUser = (User) principal;
        } else {
            // 如果 principal 不是预期的类型，说明存在问题
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 你可以根据ID再次查询数据库确保信息最新，这是个好习惯
        return this.getById(currentUser.getId());
    }

//改用Spring Security框架标准登出
   /* @Override
    public boolean userLogout(HttpServletRequest request) {

// 先判断是否已登录

        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);

        if (userObj == null) {

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");

        }

// 移除登录态

        request.getSession().removeAttribute(USER_LOGIN_STATE);

        return true;

    }*/
    /**
     * 添加用户
     *
     * @param userAddRequest
     */
    @Override
    public Long add(UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 默认密码 12345678
        String encryptPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        User user = User.builder()
                .userName(userAddRequest.getUserName())
                .userAccount(userAddRequest.getUserAccount())
                .userAvatar(userAddRequest.getUserAvatar())
                .userPassword(encryptPassword)
                .userProfile(userAddRequest.getUserProfile())
                .userRole(userAddRequest.getUserRole())
                .build();
        boolean result = this.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    /**
     * 获取用户信息
     *
     * @param id
     * @return
     */
    public UserVO getUserVOById(long id) {
        User user = this.getById(id);
        if (user == null) {
            return null;
        }
        // 返回脱敏后的VO对象
        return this.getUserVO(user);
    }


    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }
    /**
     * 获取脱敏的用户列表
     *
     * @param userList
     * @return
     */

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }



    /**
     * 提取出的私有方法，用于参数校验，使主流程更清晰
     */

    private void validateRegistrationParams(String userAccount, String userPassword, String checkPassword) {

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册信息不能为空");

        }

        if (userAccount.length() < 4) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度不能小于4位");

        }

        if (userPassword.length() < 8) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能小于8位");

        }

        if (!ACCOUNT_PATTERN.matcher(userAccount).matches()) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号只能包含字母和数字");

        }

        if (!userPassword.equals(checkPassword)) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");

        }

    }
}