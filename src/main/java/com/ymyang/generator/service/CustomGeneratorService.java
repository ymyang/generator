package com.ymyang.generator.service;

import com.ymyang.generator.config.GeneratorConfig;
import com.ymyang.generator.config.Modules;
import com.ymyang.generator.config.View;
import com.ymyang.generator.dao.SysGeneratorDao;
import com.ymyang.generator.utils.GenerationException;
import com.ymyang.generator.utils.GenerationUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成器
 */
@Service
public class CustomGeneratorService {

	@Autowired
	private SysGeneratorDao sysGeneratorDao;

	public List<Map<String, Object>> queryList(Map<String, Object> map) {
		return sysGeneratorDao.queryList(map);
	}

	public int queryTotal(Map<String, Object> map) {
		return sysGeneratorDao.queryTotal(map);
	}

	public Map<String, String> queryTable(String tableName) {
		return sysGeneratorDao.queryTable(tableName);
	}

	public List<Map<String, Object>> queryColumns(String tableName) {
		return sysGeneratorDao.queryColumns(tableName);
	}

	public void generatorCode(GeneratorConfig generatorConfig) {
		List<Modules> modules = generatorConfig.getModules();

		if (modules == null || modules.isEmpty()) {
			return;
		}

		for (Modules module : modules) {
			if (StringUtils.isEmpty(module.getName())) {
				throw new RuntimeException("generator.module.name 不能为空！");
			}

			for (String table : module.getTables()) {
				generatorTableCode(generatorConfig, module.getName(), table);
			}

			for (View view : module.getViews()) {
				generatorViewCode(generatorConfig, module.getName(), view);
			}
		}

	}

	private void generatorTableCode(GeneratorConfig generatorConfig, String moduleName, String tableName) {
		if (StringUtils.isEmpty(tableName)) {
			throw new GenerationException("生成错误：配置表名不能为空！");
		}

		// 查询表信息
		Map<String, String> table = queryTable(tableName);
		if (table == null) {
			throw new GenerationException("未查询到表信息:" + tableName);
		}

		// 查询列信息
		List<Map<String, Object>> columns = queryColumns(tableName);
		if (columns == null || columns.isEmpty()) {
			throw new GenerationException("未查询到字段信息:" + tableName);
		}

		// 重置表名
		table.put("tableName", tableName);

		// 生成代码
		GenerationUtils.generatorCode(table, columns, generatorConfig, moduleName);
	}

	private void generatorViewCode(GeneratorConfig generatorConfig, String moduleName, View view) {
		if (StringUtils.isEmpty(view.getName())) {
			throw new GenerationException("生成错误：配置视图名不能为空！");
		}

		// 查询表信息
		Map<String, String> table = new HashMap<>();
		table.put("tableName", view.getName());
		table.put("tableComment", view.getComment());

		// 查询列信息
		List<Map<String, Object>> columns = queryColumns(view.getName());
		if (columns == null || columns.isEmpty()) {
			throw new GenerationException("未查询到字段信息:" + view.getName());
		}

		// 生成代码
		GenerationUtils.generatorCode(table, columns, generatorConfig, moduleName);
	}

}
