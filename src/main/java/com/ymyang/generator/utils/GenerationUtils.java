package com.ymyang.generator.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import com.ymyang.generator.config.GeneratorConfig;
import com.ymyang.generator.entity.ColumnEntity;
import com.ymyang.generator.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * 代码生成器   工具类
 */
public class GenerationUtils {

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("template/Entity.java.vm");
        templates.add("template/QueryParam.java.vm");
        templates.add("template/CreateParam.java.vm");
        templates.add("template/UpdateParam.java.vm");
        templates.add("template/Output.java.vm");
        templates.add("template/Mapper.java.vm");
        templates.add("template/Mapper.xml.vm");
        templates.add("template/Service.java.vm");
        templates.add("template/ServiceImpl.java.vm");
        templates.add("template/Controller.java.vm");
        templates.add("template/menu.sql.vm");
        templates.add("template/api.sql.vm");
        templates.add("template/add-or-update.vue.vm");
        templates.add("template/index.vue.vm");
        return templates;
    }


    /**
     * 生成代码
     */
    @SuppressWarnings("deprecation")
	public static void generatorCode(Map<String, String> tableInfo,
                                     List<Map<String, Object>> columns,
                                     GeneratorConfig generatorConfig,
                                     String moduleName
    ) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasDate = false;
//        boolean hasLocalDateTime = false;
//        boolean hasLocalDate = false;
//        boolean hasLocalTime = false;

        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(tableInfo.get("tableName"));
        tableEntity.setComments(tableInfo.get("tableComment"));


        //表名转换成Java类名
        String className = tableToJava(tableInfo.get("tableName"), generatorConfig.getTablePrefix());
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, Object> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName").toString());
            columnEntity.setDataType(column.get("dataType").toString());
            columnEntity.setComments(column.get("columnComment").toString());
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
            if (!hasDate && attrType.equals("Date")) {
            	hasDate = true;
            }
//            if (!hasLocalDateTime && attrType.equals("LocalDateTime")) {
//                hasLocalDateTime = true;
//            }
//            if (!hasLocalDate && attrType.equals("LocalDate")) {
//                hasLocalDate = true;
//            }
//            if (!hasLocalTime && attrType.equals("LocalTime")) {
//                hasLocalTime = true;
//            }
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
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("hasDate", hasDate);
//        map.put("hasLocalTime", hasLocalTime);
//        map.put("hasLocalDate", hasLocalDate);
//        map.put("hasLocalDateTime", hasLocalDateTime);
//        map.put("mainPath", mainPath);
        map.put("package", generatorConfig.getPackageName());
//        map.put("controllerPackageName", generatorConfig.getControllerPackageName());
//        map.put("moduleName", moduleName);
        map.put("shiroAnnotation", config.getString("shiroAnnotation"));
        map.put("cacheAnnotation", config.getString("cacheAnnotation"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
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
            } catch (Exception ex) {
            	ex.printStackTrace();
                throw new GenerationException("渲染模板失败，表名：" + tableEntity.getTableName() + "，原因：" + ex.getMessage());
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
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        InflectorUtil inflectorUtil = InflectorUtil.getInstance();
        return columnToJava(inflectorUtil.singularize(tableName));
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
    public static String getFileName(String template, String className, GeneratorConfig generatorConfig, String moduleName) {

//        String projectPath = System.getProperty("user.dir") + File.separator + "src" + File.separator;

        String javaPath = File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        String resourcesPath = File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;

        String packagePath = "";
        if (StringUtils.isNotBlank(generatorConfig.getPackageName())) {
            packagePath += generatorConfig.getPackageName().replace(".", File.separator) + File.separator;
        }

        String entityPath = generatorConfig.getServicePath() + javaPath + packagePath;
        String mapperPath = generatorConfig.getServicePath() + javaPath + packagePath;
        String mapperXmlPath = generatorConfig.getServicePath() + resourcesPath + packagePath;


        String serviceAbsPath = generatorConfig.getServicePath() + javaPath + packagePath;
        String serviceImplPath = generatorConfig.getServicePath() + javaPath + packagePath;

        String queryParamPath = generatorConfig.getModelPath() + javaPath + packagePath;
        String createParamPath = generatorConfig.getModelPath() + javaPath + packagePath;
        String updateParamPath = generatorConfig.getModelPath() + javaPath + packagePath;
        String outputPath = generatorConfig.getModelPath() + javaPath + packagePath;
        String controllerAbsPath = generatorConfig.getControllerPath() + javaPath + packagePath;
        
        if (StringUtils.isNotBlank(moduleName)) {
        	entityPath += moduleName + File.separator;
            mapperPath += moduleName + File.separator;
            mapperXmlPath += moduleName + File.separator;
            
            serviceAbsPath += moduleName + File.separator;
            serviceImplPath += moduleName + File.separator;
            
            queryParamPath += moduleName + File.separator;
            createParamPath += moduleName + File.separator;
            updateParamPath += moduleName + File.separator;
            outputPath += moduleName + File.separator;
            controllerAbsPath += moduleName + File.separator;
        }
        entityPath += "entity" + File.separator;
        mapperPath += "mapper" + File.separator;
        mapperXmlPath += "mapper" + File.separator;
        
        serviceAbsPath += "service" + File.separator;
        serviceImplPath += "service" + File.separator + "impl" + File.separator;
        
        queryParamPath += "param" + File.separator;
        createParamPath += "param" + File.separator;
        updateParamPath += "param" + File.separator;
        outputPath += "dto" + File.separator;
        controllerAbsPath += "controller" + File.separator;

        if (template.contains("Mapper.java.vm")) {
            new File(mapperPath).mkdirs();
            return mapperPath + className + "Mapper.java";
        }

        if (template.contains("Entity.java.vm")) {
            new File(entityPath).mkdirs();
            return entityPath + className + "Entity.java";
        }

        if (template.contains("Mapper.xml.vm")) {
            new File(mapperXmlPath).mkdirs();
            return mapperXmlPath + className + "Mapper.xml";
        }

        if (template.contains("Service.java.vm")) {
            new File(serviceAbsPath).mkdirs();
            return serviceAbsPath + className + "Service.java";
        }

        if (template.contains("ServiceImpl.java.vm")) {
            new File(serviceImplPath).mkdirs();
            return serviceImplPath + className + "ServiceImpl.java";
        }

        if (template.contains("Controller.java.vm")) {
            new File(controllerAbsPath).mkdirs();
            return controllerAbsPath + className + "Controller.java";
        }

        if (template.contains("QueryParam.java.vm")) {
            new File(queryParamPath).mkdirs();
            return queryParamPath + className + "QueryParam.java";
        }

        if (template.contains("CreateParam.java.vm")) {
            new File(createParamPath).mkdirs();
            return createParamPath + className + "CreateParam.java";
        }
        if (template.contains("UpdateParam.java.vm")) {
            new File(updateParamPath).mkdirs();
            return updateParamPath + className + "UpdateParam.java";
        }
        if (template.contains("Output.java.vm")) {
            new File(outputPath).mkdirs();
            return outputPath + className + "Dto.java";
        }

        String sqlPath = generatorConfig.getControllerPath() + resourcesPath + "sql" + File.separator;
        if (template.contains("menu.sql.vm")) {
            new File(sqlPath).mkdirs();
            return sqlPath + className + "Menu.sql";
        }
        if (template.contains("api.sql.vm")) {
            new File(sqlPath).mkdirs();
            return sqlPath + className + "Api.sql";
        }

//        String vuePath = generatorConfig.getVuePath() + "src" + File.separator
//                + "views" + File.separator
//                + "modules" + File.separator
//                + moduleName + File.separator;
//        if (template.contains("add-or-update.vue.vm")) {
//            new File(vuePath).mkdirs();
//            return vuePath + className.toLowerCase() + "-add-or-update.vue";
//        }
//        if (template.contains("index.vue.vm")) {
//            new File(vuePath).mkdirs();
//            return vuePath + className.toLowerCase() + ".vue";
//        }

        return null;
    }

    public static Integer getMigrationVersion(File file) {
        String[] list = file.list();
        Stream<String> stream = Arrays.stream(list);
        Optional<Integer> first = stream.map(item -> Integer.valueOf(item.substring(1, item.indexOf('_'))))
                .sorted((v1, v2) -> v2 - v1).findFirst();
        return first.orElse(0);

    }
}
