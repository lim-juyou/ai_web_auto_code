package org.lim.aiautocode.controller;


import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;

import jakarta.annotation.Resource;

import jakarta.servlet.http.HttpServletRequest;

import org.lim.aiautocode.annotation.AuthCheck;
import org.lim.aiautocode.common.BaseResponse;

import org.lim.aiautocode.common.DeleteRequest;
import org.lim.aiautocode.common.ResultUtils;

import org.lim.aiautocode.constant.UserConstant;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;

import org.lim.aiautocode.exception.ThrowUtils;

import org.lim.aiautocode.model.dto.*;

import org.lim.aiautocode.model.enums.UserRoleEnum;
import org.lim.aiautocode.model.vo.LoginUserVO;

import org.lim.aiautocode.model.vo.UserVO;
import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.beans.factory.annotation.Autowired;

import org.lim.aiautocode.model.entity.User;

import org.lim.aiautocode.service.UserService;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.lim.aiautocode.constant.UserConstant.ADMIN_ROLE;


/**
 * 用户表 控制层。
 *
 * @author lim
 */

@RestController

@RequestMapping("/user")

public class UserController {


    @Resource

    private UserService userService;


    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册结果
     */

    @PostMapping("/register")

    public BaseResponse<Long> register(@RequestBody UserRegisterRequest registerRequest) {

        long result = userService.userRegister(registerRequest);

        return ResultUtils.success(result);

    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求
     * @param request          请求
     * @return 登录结果
     */

    @PostMapping("/login")

    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {

        return ResultUtils.success(userService.userLogin(userLoginRequest, request));

    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 登录用户
     */

    @GetMapping("/get/login")

    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);

        return ResultUtils.success(userService.getLoginUserVO(loginUser));

    }


    /**
     * 用户注销
     *
     * @param request 请求
     * @return 注销结果
     */
//改用Spring Security实现
/*    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {

        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        boolean result = userService.userLogout(request);

        return ResultUtils.success(result);

    }*/
    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(requiredRoles  = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        return ResultUtils.success(userService.add(userAddRequest));
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(requiredRoles  = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
// 此接口的定位是公开的，任何人都可以查看用户的VO信息（已脱敏）
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 直接调用Service层的方法，该方法应负责查询并转换为VO
        UserVO userVO = userService.getUserVOById(id);
        ThrowUtils.throwIf(userVO == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userVO);
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(requiredRoles  = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(requiredRoles  = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(requiredRoles = {ADMIN_ROLE})
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }



    /**
  /*   * 保存用户表。
     *
     * @param user 用户表
     * @return {@code true} 保存成功，{@code false} 保存失败
     *//*

    @PostMapping("save")
    @AuthCheck(requiredRoles = {ADMIN_ROLE})
    public boolean save(@RequestBody User user) {

        return userService.save(user);

    }


    *//**
     * 根据主键删除用户表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     *//*

    @DeleteMapping("remove/{id}")
    @AuthCheck(requiredRoles = {ADMIN_ROLE})
    public boolean remove(@PathVariable Long id) {

        return userService.removeById(id);

    }


    *//**
     * 根据主键更新用户表。
     *
     * @param user 用户表
     * @return {@code true} 更新成功，{@code false} 更新失败
     *//*

    *//*@PutMapping("update")
    public boolean update(@RequestBody User user) {

        return userService.updateById(user);

    }*//*


    *//**
     * 查询所有用户表。
     *
     * @return 所有数据
     *//*

    @GetMapping("list")
    @AuthCheck(requiredRoles = {ADMIN_ROLE})
    public List<User> list() {

        return userService.list();

    }


    *//**
     * 根据主键获取用户表。
     *
     * @param id 用户表主键
     * @return 用户表详情
     *//*

    @GetMapping("getInfo/{id}")
    @AuthCheck(requiredRoles = {ADMIN_ROLE})
    public User getInfo(@PathVariable Long id) {

        return userService.getById(id);

    }


    *//**
     * 分页查询用户表。
     *
     * @param page 分页对象
     * @return 分页对象
     *//*

    @GetMapping("page")
    @AuthCheck(requiredRoles = {ADMIN_ROLE})
    public Page<User> page(Page<User> page) {

        return userService.page(page);

    }*/
}