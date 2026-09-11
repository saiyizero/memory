package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

/**
 * 表结构定义持久化对象
 */
@Data
@JTable(name="table_data")
public class TableDataPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String appName;          // 服务器名称
    private String moduleName;       // 模块名称
    private String tableNameCamel;   // 表驼峰名称
    private String tableNameSnake;   // 表下划线名称
    private String tableCommentCn;   // 表中文注释
    private String tableCommentEn;   // 表英文注释
    private String fieldsJson;       // 字段列表
    private String primaryKeyJson;   // 主键
    private String indexesJson;      // 索引
    private String lableName;        // 标签
    private String associatEnum;     // 关联枚举
    private String parentClass;      // 公共字段
    private String generCdFlg;       // 自动生成代码
    private String createTabFlg;     // 自动执行建表语句
    private String defOrderBy;       // 查询排序
    private String status;           // 表状态
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
}