package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="table_record")
public class TableRecordPO {
    private String id;                // 主键ID
    private String groupName;         // 分组名
    private String projectName;       // 项目名称
    private String appName;           // 应用名称
    private String moduleName;        // 模块名称
    private String tableNameCamel;    // 表驼峰名称
    private String tableNameSnake;    // 表下划线名称
    private String tableCommentCn;    // 表中文注释
    private String tableCommentEn;    // 表英文注释
    private String fieldsJson;        // 字段JSON
    private String primaryKeyJson;    // 主键JSON
    private String indexesJson;       // 索引JSON
    private String lableName;         // 标签名称
    private String associatEnum;      // 关联枚举
    private String parentClass;       // 父类
    private String generCdFlg;        // 生成代码标志
    private String createTabFlg;      // 创建表标志
    private String defOrderBy;        // 默认排序
    private String execsqlJson;       // 执行SQL JSON
    private String status;            // 状态
    private String updateBy;          // 更新人
    private String updateTime;        // 更新时间
}