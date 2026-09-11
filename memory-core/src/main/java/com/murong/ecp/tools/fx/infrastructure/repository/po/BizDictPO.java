package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="biz_dict")
public class BizDictPO {
    private String groupName;      // 分组名
    private String projectName;    // 项目名称
    private String appName;        // 服务器名称
    private String nameCamel;      // 驼峰命名
    private String nameSnake;      // 下划线命名
    private String type;           // Java类型
    private String dbTyp;          // 数据库类型
    private String enumNme;        // 枚举名称
    private String enumRef;        // 枚举路径
    private String baseRef;        // 基础依赖
    private String notNull;        // 非空标志
    private String defaultValue;   // 默认值
    private String commentCn;      // 中文注释
    private String commentEn;      // 英文注释
    private Integer length;        // 长度
    private String status;         // 字段状态
}
