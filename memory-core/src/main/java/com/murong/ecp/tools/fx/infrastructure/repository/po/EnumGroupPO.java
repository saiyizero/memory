package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Data;

@Data
@JTable(name="enum_dict")
public class EnumGroupPO {
    private String enumNme;          // 枚举名称
    private String enumRef;          // 枚举引用
    private String dbName;           // 数据库名称
    private String appName;          // 应用名称
    private String descCn;           // 中文描述
    private String descEn;           // 英文描述

    // 复选框属性
    private transient SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    public SimpleBooleanProperty getSelectedProperty() {
        return selectedProperty;
    }
    public void setSelectedProperty(SimpleBooleanProperty selectedProperty) {
        this.selectedProperty = selectedProperty;
    }
    public String getDescCn() {
        return descCn;
    }
    public String getDescEn() {
        return descEn;
    }
}
