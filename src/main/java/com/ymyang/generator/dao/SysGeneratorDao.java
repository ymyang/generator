package com.ymyang.generator.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


@Mapper
public interface SysGeneratorDao {

    List<Map<String, Object>> queryList(Map<String, Object> map);

    int queryTotal(Map<String, Object> map);

    Map<String, String> queryTable(String tableName);

    List<Map<String, Object>> queryColumns(String tableName);

    int createTmpTable(@Param("tableName") String tableName, @Param("columns") List<String> columns);

    void dropTmpTable(@Param("tableName") String tableName);
}
