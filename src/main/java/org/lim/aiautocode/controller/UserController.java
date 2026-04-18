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
import org.lim.aiautocode.manager.OssManager;
import org.lim.aiautocode.model.dto.user.*;
import org.lim.aiautocode.model.entity.User;
import org.lim.aiautocode.model.vo.user.LoginUserVO;
import org.lim.aiautocode.model.vo.user.UserVO;
import org.lim.aiautocode.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;

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

    @Resource
    private OssManager ossManager;



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

        User loginUser = userService.getLoginUser();

        return ResultUtils.success(userService.getLoginUserVO(loginUser));

    }

// <editor-fold desc="废弃的用户注销" default state="collapsed">
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
// </editor-fold>

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(requiredRoles = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        return ResultUtils.success(userService.add(userAddRequest));
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(requiredRoles = UserConstant.ADMIN_ROLE)
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
    @AuthCheck(requiredRoles = UserConstant.ADMIN_ROLE)
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
   // @AuthCheck(requiredRoles = UserConstant.ADMIN_ROLE)
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
     * 上传用户头像，上传到阿里云OSS
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件为空");
        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        ThrowUtils.throwIf(!fileExtension.matches("\\.(jpg|jpeg|png|gif|webp)"),
                ErrorCode.PARAMS_ERROR, "仅支持 jpg/jpeg/png/gif/webp 格式");
        // 校验文件大小（限制 2MB）
        ThrowUtils.throwIf(file.getSize() > 2 * 1024 * 1024, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
        try {
            String fileName = UUID.randomUUID() + fileExtension;
            File tempFile = File.createTempFile("avatar-", fileName);
            file.transferTo(tempFile);
            String ossKey = "avatars/" + fileName;
            String avatarUrl = ossManager.uploadFile(ossKey, tempFile);
            tempFile.delete();
            ThrowUtils.throwIf(avatarUrl == null || avatarUrl.isBlank(), ErrorCode.OPERATION_ERROR, "头像上传失败");
            // 更新当前登录用户的头像
            User loginUser = userService.getLoginUser();
            User updateUser = new User();
            updateUser.setId(loginUser.getId());
            updateUser.setUserAvatar(avatarUrl);
            userService.updateById(updateUser);
            return ResultUtils.success(avatarUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传处理失败：" + e.getMessage());
        }
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


}
