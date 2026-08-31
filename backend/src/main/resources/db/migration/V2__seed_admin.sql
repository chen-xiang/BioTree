-- 种子管理员：用户名 admin，密码 admin123（仅本地开发，上线务必修改）
INSERT INTO admin_user (username, password_hash, enabled)
VALUES (
    'admin',
    '$2a$10$SiIUtTCZxGOLXsPDu6dh0OIFc9S2bn5xYMoD1pGqbS5uwj4B90htC',
    1
);
