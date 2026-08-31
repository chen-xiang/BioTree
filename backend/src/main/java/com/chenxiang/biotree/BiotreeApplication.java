/**
 * BioTree 应用启动入口。
 *
 * Author: chen-xiang
 * Created: 2026-08-31
 */
package com.chenxiang.biotree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BiotreeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiotreeApplication.class, args);
    }
}
