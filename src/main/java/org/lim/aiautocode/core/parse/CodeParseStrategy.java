package org.lim.aiautocode.core.parse;

/**
 * 代码解析策略接口（Strategy 角色）
 * 使用泛型 T 来支持不同的解析结果类型
 * @param <T> 解析结果的类型
 */
public interface CodeParseStrategy<T> {

    /**
     * 解析代码内容
     *
     * @param codeContent AI生成的原始代码内容
     * @return 解析后的结构化结果对象
     */
    T parseCode(String codeContent);
}