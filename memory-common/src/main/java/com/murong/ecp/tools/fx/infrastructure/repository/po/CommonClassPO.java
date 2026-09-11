package com.murong.ecp.tools.fx.infrastructure.repository.po;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import javafx.beans.property.SimpleBooleanProperty;

import lombok.Data;

@Data
@JTable(name="common_class")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommonClassPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String moduleName;       // 模块名称
    private String className;        // 类名
    private String classType;        // 对象类型
    private String classPath;        // 类路径
    private String classCommentCn;   // 类中文注释
    private String classCommentEn;   // 类英文注释
    private String fieldsJson;       // 字段JSON
    private String indexesJson;      // 索引JSON
    private String completedFlg;     // 完成标志
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
    private String appName;          // 应用名称
    
    // 用于界面选择的属性，仅客户端 UI 使用，不参与 HTTP 序列化
    @JsonIgnore
    private SimpleBooleanProperty selectedProperty;

    @JsonIgnore
    public SimpleBooleanProperty getSelectedProperty() {
        if (selectedProperty == null) {
            selectedProperty = new SimpleBooleanProperty(false);
        }
        return selectedProperty;
    }

    @JsonIgnore
    public void setSelectedProperty(SimpleBooleanProperty selectedProperty) {
        this.selectedProperty = selectedProperty;
    }
}
