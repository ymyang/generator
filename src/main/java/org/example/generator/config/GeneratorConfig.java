package org.example.generator.config;

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

    // 作者
    private String author;

    // Email
    private String email;

    private String applicationName;

    // 是否文件覆盖
    private Boolean overwrite = false;

    // 控制器增加shiro权限注解
    private Boolean shiroAnnotation = true;

    // 程序包路径
    private String basePackageName;

    // 控制器所属modules绝对路径
    private String controllerPath;

    // service所属modules绝对路径
    private String servicePath;

    // 实体类
    private String entityPath;

    // mybatis mapper类
    private String mapperPath;

    // 控制器中的查询参数对象
    private String queryParamPath;

    // 控制器中的创建参数对象
    private String createParamPath;

    // 控制器中的编辑参数对象
    private String updateParamPath;

    // 控制器中的返回值对象
    private String dtoPath;

    // feign客户端
    private String feignClientPath;

    // 菜单sql
    private String menuSqlPath;

    // 表前缀
    private String tablePrefix;

    // 是否表名复数转单数
    private Boolean singularize;

    private List<Modules> modules;

}
