package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="base_dict")
public class BaseDictPO {
    private String nameSnake;        // 下划线命名
    private String type;             // Java类型
    private Integer length;          // 长度
    private String dbTyp;            // 数据库类型
    private String defaultValue;     // 默认值
    private String commentCn;        // 中文注释
    private String commentEn;        // 英文注释
    private String status;           // 字段状态
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
}
