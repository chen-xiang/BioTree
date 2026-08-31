/**
 * Spring Security 配置：公开只读，管理端需 Session 登录；写操作启用 Cookie CSRF。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-08-31 调整规则顺序，确保 /api/admin/** 需登录
 * Updated: 2026-08-31 放行本地配图静态路径 /files/**
 * Updated: 2026-08-31 Session 固定攻击防护与登出清理
 * Updated: 2026-08-31 管理端写操作启用 Cookie CSRF
 */
package com.chenxiang.biotree.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chenxiang.biotree.api.common.ApiResponse;
import com.chenxiang.biotree.api.common.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // 兼容 SPA：允许从 Cookie 头读取 token，不强制请求体参数
        requestHandler.setCsrfRequestAttributeName(null);

        http.csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers("/api/admin/auth/login", "/api/health", "/actuator/health"))
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.migrateSession()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/health").permitAll()
                        .requestMatchers("/files/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(ErrorCode.UNAUTHORIZED));
                }))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/admin/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("BIOTREESESSION", "XSRF-TOKEN")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getOutputStream(), ApiResponse.ok());
                        }));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
