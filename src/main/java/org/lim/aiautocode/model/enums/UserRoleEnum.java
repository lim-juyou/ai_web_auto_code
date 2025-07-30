package org.lim.aiautocode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private static final Map<String, UserRoleEnum> ROLE_ENUM_MAP;
    static {
        ROLE_ENUM_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(
                        UserRoleEnum::getValue,
                        userRoleEnum -> userRoleEnum
                ));
    }

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }


    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        return ROLE_ENUM_MAP.get(value);
    }
}
