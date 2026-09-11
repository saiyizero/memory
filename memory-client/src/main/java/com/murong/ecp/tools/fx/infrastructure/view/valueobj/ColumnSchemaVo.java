package com.murong.ecp.tools.fx.infrastructure.view.valueobj;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ColumnSchemaVo {

    private final BooleanProperty needSave = new SimpleBooleanProperty();
    private final StringProperty columnName = new SimpleStringProperty();
    private final StringProperty dataType = new SimpleStringProperty();
    private final StringProperty length = new SimpleStringProperty();
    private final BooleanProperty notNull = new SimpleBooleanProperty();
    private final StringProperty defaultValue = new SimpleStringProperty();
    private final StringProperty chineseComment = new SimpleStringProperty();
    private final StringProperty englishComment = new SimpleStringProperty();

    public boolean isNeedSave() {
        return needSave.get();
    }

    public BooleanProperty needSaveProperty() {
        return needSave;
    }

    public void setNeedSave(boolean primaryKey) {
        this.needSave.set(primaryKey);
    }

    public String getColumnName() {
        return columnName.get();
    }

    public StringProperty columnNameProperty() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName.set(columnName);
    }

    public String getDataType() {
        return dataType.get();
    }

    public StringProperty dataTypeProperty() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType.set(dataType);
    }

    public String getLength() {
        return length.get();
    }

    public StringProperty lengthProperty() {
        return length;
    }

    public void setLength(String length) {
        this.length.set(length);
    }

    public boolean isNotNull() {
        return notNull.get();
    }

    public BooleanProperty notNullProperty() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull.set(notNull);
    }

    public String getDefaultValue() {
        return defaultValue.get();
    }

    public StringProperty defaultValueProperty() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue.set(defaultValue);
    }

    public String getChineseComment() {
        return chineseComment.get();
    }

    public StringProperty chineseCommentProperty() {
        return chineseComment;
    }

    public void setChineseComment(String chineseComment) {
        this.chineseComment.set(chineseComment);
    }

    public String getEnglishComment() {
        return englishComment.get();
    }

    public StringProperty englishCommentProperty() {
        return englishComment;
    }

    public void setEnglishComment(String englishComment) {
        this.englishComment.set(englishComment);
    }
} 