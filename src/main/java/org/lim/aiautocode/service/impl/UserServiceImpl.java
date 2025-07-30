package org.lim.aiautocode.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.lim.aiautocode.exception.BusinessException;
import org.lim.aiautocode.exception.ErrorCode;
import org.lim.aiautocode.exception.ThrowUtils;
import org.lim.aiautocode.model.dto.UserRegisterRequest;
import org.lim.aiautocode.model.entity.User;
import org.lim.aiautocode.mapper.UserMapper;
import org.lim.aiautocode.model.enums.UserRoleEnum;
import org.lim.aiautocode.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.regex.Pattern;

/**
 * 用户表 服务层实现。
 *
 * @author lim
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

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

            // 3. 加密
            String encryptedPassword = DigestUtils.md5DigestAsHex(userPassword.getBytes());

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
