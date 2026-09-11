package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Data;

@Data
@JTable(name="enum_dict")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnumGroupPO {
    private String enumNme;          // 枚举名称
    private String enumRef;          // 枚举引用
    private String dbName;           // 数据库名称
    private String appName;          // 应用名称
    private String descCn;           // 中文描述
    private String descEn;           // 英文描述

    // 复选框属性，仅客户端 UI 使用，不参与 HTTP 序列化
    @JsonIgnore
    private transient SimpleBooleanProperty selectedProperty = new SimpleBooleanProperty(false);

    @JsonIgnore
    public SimpleBooleanProperty getSelectedProperty() {
        return selectedProperty;
    }

    @JsonIgnore
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
