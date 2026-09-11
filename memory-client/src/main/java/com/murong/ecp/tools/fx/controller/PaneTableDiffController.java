package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.service.different.TableDiffService;
import com.murong.ecp.tools.fx.enums.DiffTypeEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDiffPO;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.TableDiffDetailDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class PaneTableDiffController implements Initializable {

    @Autowired
    private TableDiffService tableDiffService;
    
    @Autowired
    private GlobalProperties globalProps;

    @FXML private TableView<TableDiffPO> diffTableView;
    @FXML private TableColumn<TableDiffPO, String> tableNameColumn;
    @FXML private TableColumn<TableDiffPO, String> compareEnvColumn;
    @FXML private TableColumn<TableDiffPO, String> diffFieldCountColumn;
    @FXML private TableColumn<TableDiffPO, String> diffTypeColumn;
    @FXML private TableColumn<TableDiffPO, String> localValueColumn;
    @FXML private TableColumn<TableDiffPO, String> remoteValueColumn;

    @FXML private TableColumn<TableDiffPO, Void> actionColumn;
    
    @FXML private TextField tableNameField;
    @FXML private ComboBox<String> envComboBox;
    @FXML private ComboBox<DiffTypeEnum> diffTypeComboBox;

    @FXML private Button queryButton;
    @FXML private Button clearButton;

    private ObservableList<TableDiffPO> diffList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        // 初始化环境下拉框
        envComboBox.getItems().addAll("请选择环境", "dev", "sit", "uat", "poc");
        envComboBox.setValue("请选择环境"); // 设置默认值
        
        // 设置环境下拉框值变化监听器
        envComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            // 无论选择还是清空都触发查询
            loadDiffRecords();
        });
        
        // 初始化差异类型下拉框
        diffTypeComboBox.getItems().addAll(DiffTypeEnum.values());
        diffTypeComboBox.setPromptText("请选择差异类型");
        
        // 设置差异类型下拉框值变化监听器
        diffTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            // 无论选择还是清空都触发查询
            loadDiffRecords();
        });
        
        // 设置差异类型下拉框的显示文本
        diffTypeComboBox.setCellFactory(param -> new ListCell<DiffTypeEnum>() {
            @Override
            protected void updateItem(DiffTypeEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDesc());
                }
            }
        });
        
        diffTypeComboBox.setButtonCell(new ListCell<DiffTypeEnum>() {
            @Override
            protected void updateItem(DiffTypeEnum item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDesc());
                }
            }
        });
        
        // 设置表格数据
        diffTableView.setItems(diffList);
        
        // 设置表格行高
        diffTableView.setFixedCellSize(25); // 设置固定行高为25像素
        
        // 设置列绑定
        tableNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTableNameSnake()));
        compareEnvColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCompareEnv()));
        diffFieldCountColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(getDiffFieldCountText(cellData.getValue().getDiffFieldCount())));
        diffTypeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(getDiffTypeText(cellData.getValue().getDiffType())));
        localValueColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getLocalValue()));
        remoteValueColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getRemoteValue()));
        
        // 设置列文字居中
        compareEnvColumn.setStyle("-fx-alignment: center;");
        diffFieldCountColumn.setStyle("-fx-alignment: center;");
        diffTypeColumn.setStyle("-fx-alignment: center;");
        actionColumn.setStyle("-fx-alignment: center;");
        
        // 设置操作列
        actionColumn.setCellFactory(col -> new TableCell<TableDiffPO, Void>() {
            private final Button detailBtn = new Button("详情");
            private final HBox hbox = new HBox(5, detailBtn);
            {
                // 设置HBox居中对齐
                hbox.setAlignment(javafx.geometry.Pos.CENTER);
                
                // 设置按钮样式 - 蓝色圆角按钮样式
                detailBtn.setStyle(
                    "-fx-background-color: #007bff;" +  // 蓝色背景
                    "-fx-text-fill: white;" +           // 白色文字
                    "-fx-font-size: 10px;" +            // 字体大小
                    "-fx-font-weight: bold;" +          // 粗体
                    "-fx-background-radius: 10px;" +    // 圆角
                    "-fx-border-radius: 10px;" +        // 边框圆角
                    "-fx-padding: 3px 6px;" +           // 内边距
                    "-fx-cursor: hand;" +               // 手型光标
                    "-fx-min-width: 60px;" +            // 最小宽度
                    "-fx-max-width: 80px;"              // 最大宽度
                );
                
                // 添加鼠标悬停效果
                detailBtn.setOnMouseEntered(e -> {
                    detailBtn.setStyle(
                        "-fx-background-color: #0056b3;" +  // 深蓝色
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-padding: 3px 6px;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 60px;" +
                        "-fx-max-width: 80px;"
                    );
                });
                
                detailBtn.setOnMouseExited(e -> {
                    detailBtn.setStyle(
                        "-fx-background-color: #007bff;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-padding: 3px 6px;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 60px;" +
                        "-fx-max-width: 80px;"
                    );
                });
                
                detailBtn.setOnAction(e -> {
                    TableDiffPO diff = getTableView().getItems().get(getIndex());
                    showDiffDetail(diff);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
        
        // 加载初始数据
        loadDiffRecords();
    }
    
    @FXML
    private void queryDiffRecords() {
        loadDiffRecords();
    }
    
    @FXML
    private void clearQuery() {
        tableNameField.clear();
        envComboBox.setValue("");
        diffTypeComboBox.setValue(null);
        loadDiffRecords();
    }
    
    @FXML
    private void refreshData() {
        loadDiffRecords();
    }
    
    @FXML
    private void exportDiff() {
        // TODO: 实现导出功能
        ViewUtils.alertForSucess("导出功能待实现");
    }
    
    /**
     * 加载差异记录
     */
    private void loadDiffRecords() {
        try {
            // 构建查询条件
            TableDiffPO query = new TableDiffPO();
            query.setGroupName(globalProps.getGroupName());
            query.setProjectName(globalProps.getProjectName());
            query.setAppName(globalProps.getAppName());
            
            // 添加查询条件
            if (tableNameField != null && !tableNameField.getText().trim().isEmpty()) {
                query.setTableNameSnake(tableNameField.getText().trim());
            }
            if (envComboBox != null && envComboBox.getValue() != null && !envComboBox.getValue().isEmpty() && !envComboBox.getValue().equals("请选择环境")) {
                query.setCompareEnv(envComboBox.getValue());
            }
            if (diffTypeComboBox != null && diffTypeComboBox.getValue() != null) {
                query.setDiffType(diffTypeComboBox.getValue());
            }
            
            // 查询差异记录
            CrResult<List<TableDiffPO>> result = tableDiffService.queryDiffRecords(query);
            if (result.isSucess()) {
                diffList.setAll(result.getData());
            } else {
                ViewUtils.alertForFail("查询差异记录失败: " + result.getMsgInf());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("查询差异记录异常: " + e.getMessage());
        }
    }


    
    /**
     * 显示差异详情
     */
    private void showDiffDetail(TableDiffPO diff) {
        TableDiffDetailDialog.showDiffDetail(diff);
    }

    
    /**
     * 获取差异类型文本
     */
    private String getDiffTypeText(DiffTypeEnum diffType) {
        if (diffType == null) return "";
        return diffType.getDesc();
    }
    
    /**
     * 获取字段差异数量文本
     */
    private String getDiffFieldCountText(Integer diffFieldCount) {
        if (diffFieldCount == null) return "0";
        return diffFieldCount.toString();
    }
    

} 