package com.ymyang.generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "generator")
public class GeneratorConfig {

    // 包名
    private String packageName;

    private Boolean overwrite;

    // 控制器所属modules绝对路径
    private String controllerPath;

    // vuejs绝对路径
    private String vuePath;

    // service所属modules绝对路径
    private String servicePath;
    
    // dto,param所属modules绝对路径
    private String modelPath;

    private List<Modules> modules;

    private String tablePrefix;

}
