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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/user/login",
                                "/user/register",
                                "/user/get/login",
                                // 首页公开数据接口放行
                                "/app/good/list/page/vo",
                                "/app/get/vo",
                                "/doc.html",
                                "/webjars/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                // 预览静态资源放行
                                "/static/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .csrf(csrf -> csrf.disable())

                // 新增：启用跨域配置
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 开发场景：允许 iframe 加载（用于右侧预览 iframe）。如需更安全可改回 sameOrigin。
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))

                .logout(logout -> logout
                        .logoutUrl("/user/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json;charset=UTF-8");
                            BaseResponse<Boolean> successResponse = ResultUtils.success(true);
                            ObjectMapper objectMapper = new ObjectMapper();
                            String jsonResponse = objectMapper.writeValueAsString(successResponse);
                            response.getWriter().write(jsonResponse);
                        })
                );

        return http.build();
    }

    /**
     * 跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许携带凭证
        configuration.setAllowCredentials(true);

        // 允许的源（开发环境可以用 * ，生产环境建议指定具体域名）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许的请求方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("*"));

        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}