package org.lim.aiautocode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum ChatHistoryMessageTypeEnum {

    USER("用户", "user"),
    AI("AI", "ai");
    private static final Map<String, ChatHistoryMessageTypeEnum> TYPE_ENUM_MAP;

    static {
        TYPE_ENUM_MAP = Arrays.stream(values())
                .collect(Collectors.toMap(ChatHistoryMessageTypeEnum::getValue,
                        v -> v));
    }

    private final String text;

    private final String value;

    ChatHistoryMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static ChatHistoryMessageTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
       return TYPE_ENUM_MAP.get(value);
    }
}
