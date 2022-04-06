/**
 * Copyright 2018 人人开源 http://www.renren.io
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.example.generator.utils;

import org.example.generator.config.GeneratorConfig;
import org.example.generator.entity.ColumnEntity;
import org.example.generator.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileUrlResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Stream;

/**
 * 代码生成器   工具类
 */
public class GenerationUtils {

    static Logger logger = LoggerFactory.getLogger(GenerationUtils.class);

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/QueryParam.java.vm");

        templates.add("template/UpdateParam.java.vm");
        templates.add("template/CreateParam.java.vm");

        templates.add("template/Output.java.vm");
        templates.add("template/Mapper.java.vm");
        templates.add("template/Mapper.xml.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/ServiceImpl.java.vm");
        templates.add("template/Controller.java.vm");

        templates.add("template/FeignClient.java.vm");
        templates.add("template/FeignFallbackFactory.java.vm");

        templates.add("template/Menu.sql.vm");

        templates.add("template/add-or-update.vue.vm");
        templates.add("template/index.vue.vm");
        return templates;
    }


    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> tableInfo,
                                     List<Map<String, Object>> columns,
                                     GeneratorConfig generatorConfig,
                                     String moduleName
    ) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasLocalDateTime = false;
        boolean hasLocalDate = false;
        boolean hasLocalTime = false;

        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(tableInfo.get("tableName"));
        tableEntity.setComments(tableInfo.get("tableComment"));


        //表名转换成Java类名
        String className = tableToJava(tableInfo.get("tableName"), generatorConfig.getTablePrefix(), generatorConfig.getSingularize());
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, Object> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName").toString());
            columnEntity.setDataType(column.get("dataType").toString());
            columnEntity.setComments(column.get("columnComment").toString().replace("\n", ""));
            Object extra = column.get("extra");
            if (extra == null) {
            	extra = column.get("EXTRA");
            }
            columnEntity.setExtra(extra == null ? null : extra.toString());
            columnEntity.setNullable(column.get("nullable").toString());
            columnEntity.setColumnDefault(column.get("columnDefault") == null ? null : column.get("columnDefault").toString());
            if (column.get("characterMaximumLength") != null) {
                columnEntity.setCharacterMaximumLength(Integer.valueOf(column.get("characterMaximumLength").toString()));
            }

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));
            columnEntity.setAttrName(attrName);

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && attrType.equals("BigDecimal")) {
                hasBigDecimal = true;
            }
            if (!hasLocalDateTime && attrType.equals("LocalDateTime")) {
                hasLocalDateTime = true;
            }
            if (!hasLocalDate && attrType.equals("LocalDate")) {
                hasLocalDate = true;
            }
            if (!hasLocalTime && attrType.equals("LocalTime")) {
                hasLocalTime = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey").toString()) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }
            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

//        String mainPath = config.getString("mainPath");

        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("moduleName", moduleName);
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments().replace("\n", " "));
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("hasLocalTime", hasLocalTime);
        map.put("hasLocalDate", hasLocalDate);
        map.put("hasLocalDateTime", hasLocalDateTime);
        map.put("config", generatorConfig);
//        map.put("package", generatorConfig.getPackageName());
//        map.put("controllerPackageName", generatorConfig.getControllerPackageName());
//        map.put("moduleName", moduleName);
        map.put("shiroAnnotation", generatorConfig.getShiroAnnotation());
        map.put("author", generatorConfig.getAuthor());
        map.put("email", generatorConfig.getEmail());
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("date", DateUtils.format(new Date(), DateUtils.DATE_PATTERN));
        map.put("time", DateUtils.format(new Date(), DateUtils.TIME_PATTERN));
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {

                String filePath = getFileName(template, tableEntity.getClassName(), generatorConfig, moduleName);

                if (filePath == null) continue;
                File file = new File(filePath);
                if (!generatorConfig.getOverwrite() && file.exists()) {
                    continue;
                }
                System.err.println(filePath);
                FileOutputStream templateOutput = new FileOutputStream(file);
                IOUtils.write(sw.toString(), templateOutput, "UTF-8");
                IOUtils.closeQuietly(sw);
                templateOutput.close();
            } catch (Exception e) {
                throw new GenerationException("渲染模板失败，表名：" + tableEntity.getTableName() + "，原因：" + e.getMessage());
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix, Boolean singularize) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        InflectorUtil inflectorUtil = InflectorUtil.getInstance();
//        System.err.println("表名转换成Java类名0: " + tableName);
        String javaClassName = tableName;
        if (singularize == null || singularize) {
            javaClassName = inflectorUtil.singularize(tableName);
        }
//        System.err.println("表名转换成Java类名1: " + singularize);
        javaClassName = columnToJava(javaClassName);
//        System.err.println("表名转换成Java类名2: " + javaClassName);
        return javaClassName;
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new GenerationException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, GeneratorConfig config, String moduleName) {

//        String projectPath = System.getProperty("user.dir") + File.separator + "src" + File.separator;

        String javaPath = File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        String resourcesPath = File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;

        if (template.contains("Controller.java.vm") && StringUtils.isNotBlank(config.getControllerPath()) && StringUtils.isNotBlank(config.getControllerPath())) {
            String path = config.getControllerPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "controller"
                    + File.separator + "back" + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + "Back" + className + "Controller.java";
        }

        if (template.contains("QueryParam.java.vm") && StringUtils.isNotBlank(config.getQueryParamPath())) {
            String path = config.getQueryParamPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "param"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "QueryParam.java";
        }

        if (template.contains("UpdateParam.java.vm") && StringUtils.isNotBlank(config.getUpdateParamPath())) {
            String path = config.getUpdateParamPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "param"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "UpdateParam.java";
        }

        if (template.contains("CreateParam.java.vm") && StringUtils.isNotBlank(config.getCreateParamPath())) {
            String path = config.getCreateParamPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "param"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "CreateParam.java";
        }

        if (template.contains("Output.java.vm") && StringUtils.isNotBlank(config.getDtoPath())) {
            String path = config.getDtoPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "dto"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "Dto.java";
        }

        if (template.contains("Entity.java.vm") && StringUtils.isNotBlank(config.getEntityPath())) {
            String path = config.getEntityPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "entity"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "Entity.java";
        }

        if (template.contains("Service.java.vm") && StringUtils.isNotBlank(config.getServicePath())) {
            String path = config.getServicePath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "service"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "Service.java";
        }

        if (template.contains("ServiceImpl.java.vm") && StringUtils.isNotBlank(config.getServicePath())) {
            String path = config.getServicePath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "service"
                    + File.separator + moduleName + File.separator + "impl" + File.separator;
            new File(path).mkdirs();
            return path + className + "ServiceImpl.java";
        }

        if (template.contains("Mapper.java.vm") && StringUtils.isNotBlank(config.getMapperPath())) {
            String path = config.getMapperPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "mapper"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "Mapper.java";
        }
        if (template.contains("Mapper.xml.vm") && StringUtils.isNotBlank(config.getMapperPath())) {
            String path = config.getMapperPath() + resourcesPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "mapper"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "Mapper.xml";
        }

        if (template.contains("FeignClient.java.vm") && StringUtils.isNotBlank(config.getFeignClientPath())) {
            String path = config.getFeignClientPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "feign"
                    + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + "FeignClient.java";
        }
        if (template.contains("FeignFallbackFactory.java.vm") && StringUtils.isNotBlank(config.getFeignClientPath())) {
            String path = config.getFeignClientPath() + javaPath
                    + config.getBasePackageName().replace(".", File.separator) + File.separator + "feign"
                    + File.separator + moduleName + File.separator + "hystrix" + File.separator;
            new File(path).mkdirs();
            return path + className + "FeignFallbackFactory.java";
        }

        if (template.contains("Menu.sql.vm") && StringUtils.isNotBlank(config.getMenuSqlPath())) {
            String path = config.getMapperPath() + resourcesPath + "sql" + File.separator + moduleName + File.separator;
            new File(path).mkdirs();
            return path + className + ".sql";
        }

        return null;
    }

    public static Integer getMigrationVersion(File file) {
        String[] list = file.list();
        Stream<String> stream = Arrays.stream(list);
        Optional<Integer> first = stream.map(item -> Integer.valueOf(item.substring(1, item.indexOf('_'))))
                .sorted((v1, v2) -> v2 - v1).findFirst();
        return first.orElse(0);

    }

    public static FileUrlResource[] getGeneratorConfig(String config, String sourceDirectory) {

        String[] resources;
        if (!config.endsWith(".yml") && !config.endsWith(".yaml")) {
            resources = new String[]{"generator.yml", "generator.yaml", config + ".yml", config + ".yaml"};
        } else {
            resources = new String[]{"generator.yml", "generator.yaml", config};
        }
        String resourceDir = resourceDirectoryFromSourceDirectory(sourceDirectory);

        FileUrlResource[] fileUrlResources = Arrays.stream(resources).distinct()
                .map(resource -> {
                    try {
                        return new FileUrlResource(resourceDir + resource);
                    } catch (MalformedURLException e) {
                        return null;
                    }
                })
                .filter(resource -> {
                    try {
                        return resource != null && resource.getFile().exists();
                    } catch (IOException e) {
                        return false;
                    }
                })
                .toArray(FileUrlResource[]::new);

        if (fileUrlResources.length == 0) {
            printHelp(resourceDir + "generator.yml");
            throw new RuntimeException("配置错误！");
        }
        return fileUrlResources;
    }

    public static String resourceDirectoryFromSourceDirectory(String sourceDirectory) {
        return sourceDirectory.substring(0, sourceDirectory.lastIndexOf("java")) + "resources" + File.separator;
    }

    public static void printHelp(String resource) {
        System.err.println("缺少配置（也可指定自定义配置文件-Dconfig=myConfig.yml），请创建配置文件：" + resource +
                "\n配置范例如下:\n" +
                "spring:\n" +
                "  datasource:\n" +
                "    type: com.alibaba.druid.pool.DruidDataSource\n" +
                "    driverClassName: com.mysql.jdbc.Driver\n" +
                "    url: jdbc:mysql://localhost:3306/db_name?useUnicode=true&characterEncoding=UTF-8&useSSL=false\n" +
                "    username: root\n" +
                "    password: root" +
                "# mysql逆向生成\n" +
                "## mysql逆向生成配置\n" +
                "generator:\n" +
                "\n" +
                "  # 作者，会标注接口文档的负责人\n" +
                "  author:\n" +
                "\n" +
                "  # 邮箱\n" +
                "  email:\n" +
                "\n" +
                "  # 当前应用的微服务应用名，会标注在Feign Client上\n" +
                "  applicationName:\n" +
                "\n" +
                "  # 是否文件覆盖\n" +
                "  overwrite: false\n" +
                "\n" +
                "  # 控制器增加shiro权限注解\n" +
                "  shiroAnnotation: true\n" +
                "\n" +
                "  # 控制器所属modules绝对路径，若path和packageName为空时，则不生成，最终目录：${controllerPath}/src/main/java/${controllerPackageName}/${modulesName}\n" +
                "  controllerPath:\n" +
                "  controllerPackageName:\n" +
                "\n" +
                "  # 控制器中的查询参数对象，若path和packageName为空时，则不生成，最终目录：${queryParamPath}/src/main/java/${queryParamPackageName}/${modulesName}\n" +
                "  queryParamPath:\n" +
                "  queryParamPackageName:\n" +
                "\n" +
                "  # 控制器中的新建参数对象，若path和packageName为空时，则不生成，最终目录：${createParamPath}/src/main/java/${createParamPackageName}/${modulesName}\n" +
                "  createParamPath:\n" +
                "  createParamPackageName:\n" +
                "\n" +
                "  # 控制器中的更新参数对象，若path和packageName为空时，则不生成，最终目录：${updateParamPath}/src/main/java/${updateParamPackageName}/${modulesName}\n" +
                "  updateParamPath:\n" +
                "  updateParamPackageName:\n" +
                "\n" +
                "  # 控制器中的返回值对象，若path和packageName为空时，则不生成，最终目录：${dtoPath}/src/main/java/${dtoPackageName}/${modulesName}\n" +
                "  dtoPath:\n" +
                "  dtoPackageName:\n" +
                "\n" +
                "  # service所属modules绝对路径，若path和packageName为空时，则不生成，最终目录：${servicePath}/src/main/java/${servicePackageName}/${modulesName}\n" +
                "  servicePath:\n" +
                "  servicePackageName:\n" +
                "\n" +
                "  # 实体类，若path和packageName为空时，则不生成，最终目录：${entityPath}/src/main/java/${entityPackageName}/${modulesName}\n" +
                "  entityPath:\n" +
                "  entityPackageName:\n" +
                "\n" +
                "  # mybatis mapper类，若path和packageName为空时，则不生成，最终目录：${mapperPath}/src/main/java/${mapperPackageName}/${modulesName}\n" +
                "  mapperPath:\n" +
                "  mapperPackageName:\n" +
                "\n" +
                "  # feign客户端，若path和packageName为空时，则不生成，最终目录：${feignClientPath}/src/main/java/${feignClientPackageName}/${modulesName}\n" +
                "  feignClientPath:\n" +
                "  feignClientPackageName:\n" +
                "\n" +
                "  # 菜单sql 最终目录：${menuSqlPath}/src/main/resources/sql/${modulesName}\n" +
                "  menuSqlPath:\n" +
                "\n" +
                "  # 是否表名复数转单数，默认true转换\n" +
                "  singularize: true\n" +
                "\n" +
                "  # 表前缀\n" +
                "  tablePrefix: sys_\n" +
                "\n" +
                "  modules:\n" +
                "    # 模块名\n" +
                "    - name: sys\n" +
                "      # 需要生成的表名列表\n" +
                "      tables:\n" +
                "        - sys_user\n" +
                "        - sys_menu"
        );
    }
}
