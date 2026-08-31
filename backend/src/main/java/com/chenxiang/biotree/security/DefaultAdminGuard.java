/**
 * 生产环境拒绝默认管理员口令启动。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree.security;

import com.chenxiang.biotree.domain.user.AdminUser;
import com.chenxiang.biotree.infrastructure.persistence.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DefaultAdminGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminGuard.class);

    private final boolean denyDefaultAdmin;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminGuard(
            @Value("${app.security.deny-default-admin:false}") boolean denyDefaultAdmin,
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder) {
        this.denyDefaultAdmin = denyDefaultAdmin;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!denyDefaultAdmin) {
            return;
        }
        AdminUser admin = adminUserRepository.findByUsername("admin").orElse(null);
        if (admin != null && passwordEncoder.matches("admin123", admin.getPasswordHash())) {
            log.error("Default admin password detected; refusing to start in hardened mode");
            throw new IllegalStateException(
                    "Default admin/admin123 is not allowed when app.security.deny-default-admin=true");
        }
    }
}
