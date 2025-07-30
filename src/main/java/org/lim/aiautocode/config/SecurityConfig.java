package org.lim.aiautocode.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.lim.aiautocode.common.BaseResponse;
import org.lim.aiautocode.common.ResultUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // 启用Web安全配置
public class SecurityConfig {

    /**
     * 配置密码编码器 Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置安全过滤器链 Bean
     * @param http HttpSecurity 对象，用于构建安全配置
     * @return SecurityFilterChain
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/user/login",
                                "/user/register",
                                "/doc.html",
                                "/webjars/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .csrf(csrf -> csrf.disable())

                // V 新增登出配置 V
                .logout(logout -> logout
                        .logoutUrl("/user/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // 1. 设置响应头
                            // 响应状态码设置为 200 OK
                            response.setStatus(HttpServletResponse.SC_OK);
                            // 响应内容类型设置为 JSON
                            response.setContentType("application/json;charset=UTF-8");

                            // 2. 创建一个表示登出成功的 BaseResponse 对象
                            // 这里我们使用你项目中的 ResultUtils 工具类来创建标准成功响应
                            // data 部分可以为 true 或 null，这里用 true 表示操作成功
                            BaseResponse<Boolean> successResponse = ResultUtils.success(true);

                            // 3. 将 BaseResponse 对象序列化为 JSON 字符串
                            // 需要一个 ObjectMapper 实例来完成这个工作
                            ObjectMapper objectMapper = new ObjectMapper();
                            String jsonResponse = objectMapper.writeValueAsString(successResponse);

                            // 4. 将 JSON 字符串写入响应体
                            response.getWriter().write(jsonResponse);
                        })
                );

        return http.build();
    }
}