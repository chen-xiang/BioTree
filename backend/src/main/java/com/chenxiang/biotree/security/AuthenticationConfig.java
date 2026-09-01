/**
 * 暴露 AuthenticationManager 供登录接口使用。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 * Updated: 2026-09-01 仅 Web 进程暴露 AuthenticationManager
 */
package com.chenxiang.biotree.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
@ConditionalOnWebApplication
public class AuthenticationConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
