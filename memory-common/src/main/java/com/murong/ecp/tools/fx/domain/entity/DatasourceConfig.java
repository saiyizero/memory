package com.murong.ecp.tools.fx.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Data;

@Data
public class DatasourceConfig {
    private String projectName;
    private String driverName;
    private String username;
    private String password;
    private String url;
    private String appName;
    private String schema;
    private String envName;
    @JsonIgnore
    private transient BooleanProperty selected = new SimpleBooleanProperty(false);
    @JsonIgnore
    private transient BooleanProperty mainFlag = new SimpleBooleanProperty(false);
    @JsonIgnore
    private transient boolean mainFlagListenerAdded = false;

    public DatasourceConfig() {}
    public DatasourceConfig(String driverName, String username, String password, String url) {
        this.driverName = driverName;
        this.username = username;
        this.password = password;
        this.url = url;
    }
    public DatasourceConfig(String projectName,String driverName, String username, String password, String url) {
        this.projectName = projectName;
        this.driverName = driverName;
        this.username = username;
        this.password = password;
        this.url = url;
    }

    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    @JsonIgnore
    public BooleanProperty selectedProperty() { return selected; }
    public boolean isMainFlag() { return mainFlag.get(); }
    public void setMainFlag(boolean mainFlag) { this.mainFlag.set(mainFlag); }
    @JsonIgnore
    public BooleanProperty mainFlagProperty() { return mainFlag; }

    @JsonIgnore
    public boolean isMainFlagListenerAdded() { return mainFlagListenerAdded; }
    public void setMainFlagListenerAdded(boolean mainFlagListenerAdded) { this.mainFlagListenerAdded = mainFlagListenerAdded; }
} 