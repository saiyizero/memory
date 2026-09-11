package com.murong.ecp.tools.fx.infrastructure.view.valueobj;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class IndexSchemaVo {
    private final BooleanProperty needSave = new SimpleBooleanProperty();
    private final StringProperty indexName = new SimpleStringProperty();
    private final StringProperty columns = new SimpleStringProperty();
    private final StringProperty indexType = new SimpleStringProperty();

    public boolean isNeedSave() { return needSave.get(); }

    public void setNeedSave(boolean needSave) { this.needSave.set(needSave); }

    public BooleanProperty needSaveProperty() { return needSave; }

    public String getIndexName() {
        return indexName.get();
    }

    public StringProperty indexNameProperty() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName.set(indexName);
    }

    public String getColumns() {
        return columns.get();
    }

    public StringProperty columnsProperty() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns.set(columns);
    }

    public String getIndexType() {
        return indexType.get();
    }

    public StringProperty indexTypeProperty() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType.set(indexType);
    }

}