package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 业务字段选择对话框
 */
public class BizDictSelectionDialog {
    
    private Stage dialogStage;
    private BizDictPO selectedBizDict;
    private ObservableList<BizDictPO> bizDictList = FXCollections.observableArrayList();
    
    @FXML
    private TableView<BizDictPO> bizDictTable;
    @FXML
    private TableColumn<BizDictPO, String> nameCamelColumn;
    @FXML
    private TableColumn<BizDictPO, String> nameSnakeColumn;
    @FXML
    private TableColumn<BizDictPO, String> typeColumn;
    @FXML
    private TableColumn<BizDictPO, String> commentCnColumn;
    @FXML
    private TableColumn<BizDictPO, String> commentEnColumn;
    @FXML
    private TextField searchField;
    @FXML
    private Button selectButton;
    @FXML
    private Button cancelButton;
    
    public BizDictSelectionDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/biz_search_dialog.fxml"));
            loader.setController(this);
            Scene scene = new Scene(loader.load());
            
            dialogStage = new Stage();
            dialogStage.setTitle("选择业务字段");
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(800);
            dialogStage.setMinHeight(600);
            
            // FXML加载完成后初始化组件
            Platform.runLater(() -> {
                setupTable();
                setupEventHandlers();
            });
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void setupTable() {
        // 设置表格列
        nameCamelColumn.setCellValueFactory(new PropertyValueFactory<>("nameCamel"));
        nameCamelColumn.setText("驼峰命名");
        nameCamelColumn.setPrefWidth(150);
        
        nameSnakeColumn.setCellValueFactory(new PropertyValueFactory<>("nameSnake"));
        nameSnakeColumn.setText("下划线命名");
        nameSnakeColumn.setPrefWidth(150);
        
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setText("Java类型");
        typeColumn.setPrefWidth(100);
        
        commentCnColumn.setCellValueFactory(new PropertyValueFactory<>("commentCn"));
        commentCnColumn.setText("中文注释");
        commentCnColumn.setPrefWidth(200);
        
        commentEnColumn.setCellValueFactory(new PropertyValueFactory<>("commentEn"));
        commentEnColumn.setText("英文注释");
        commentEnColumn.setPrefWidth(200);
        
        // 设置表格数据
        bizDictTable.setItems(bizDictList);
        
        // 双击选择
        bizDictTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleSelect();
            }
        });
        
        // 回车键选择
        bizDictTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSelect();
            }
        });
    }
    
    private void setupEventHandlers() {
        // 搜索框事件
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // 如果表格有选中项，则选择；否则执行搜索
                if (bizDictTable.getSelectionModel().getSelectedItem() != null) {
                    handleSelect();
                }
            }
        });
        
        // 搜索框文本变化监听
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterTable(newValue);
        });
        
        // 按钮事件
        selectButton.setOnAction(event -> handleSelect());
        cancelButton.setOnAction(event -> handleCancel());
        
        // ESC键取消
        if (dialogStage != null) {
            dialogStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    handleCancel();
                }
            });
        }
    }
    
    private void filterTable(String searchText) {
        if (StringUtils.isBlank(searchText)) {
            // 显示所有数据
            bizDictTable.setItems(bizDictList);
        } else {
            // 过滤数据
            ObservableList<BizDictPO> filteredList = FXCollections.observableArrayList();
            for (BizDictPO bizDict : bizDictList) {
                if (StringUtils.containsIgnoreCase(bizDict.getNameCamel(), searchText) ||
                    StringUtils.containsIgnoreCase(bizDict.getNameSnake(), searchText) ||
                    StringUtils.containsIgnoreCase(bizDict.getCommentCn(), searchText) ||
                    StringUtils.containsIgnoreCase(bizDict.getCommentEn(), searchText)) {
                    filteredList.add(bizDict);
                }
            }
            bizDictTable.setItems(filteredList);
        }
        
        // 强制刷新表格以确保内容正确显示
        Platform.runLater(() -> {
            bizDictTable.refresh();
            // 滚动到顶部
            bizDictTable.scrollTo(0);
        });
    }
    
    private void handleSelect() {
        BizDictPO selected = bizDictTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedBizDict = selected;
            dialogStage.close();
        }
    }
    
    private void handleCancel() {
        selectedBizDict = null;
        dialogStage.close();
    }
    
    /**
     * 显示对话框并返回选择的业务字段
     * @param bizDictList 业务字段列表
     * @param initialSearchText 搜索框的初始文本
     * @return 选择的业务字段，如果取消则返回null
     */
    public Optional<BizDictPO> showAndWait(List<BizDictPO> bizDictList, String initialSearchText) {
        this.bizDictList.clear();
        this.bizDictList.addAll(bizDictList);
        
        // 设置搜索框的初始文本并触发筛选
        if (searchField != null && StringUtils.isNotBlank(initialSearchText)) {
            // 使用Platform.runLater确保在JavaFX线程中执行
            Platform.runLater(() -> {
                searchField.setText(initialSearchText);
                // 手动触发筛选，确保表格数据被正确过滤
                filterTable(initialSearchText);
            });
        }
        
        // 如果有数据，选中第一行
        if (!bizDictList.isEmpty()) {
            bizDictTable.getSelectionModel().selectFirst();
        }
        
        // 聚焦到搜索框并选中文本
        Platform.runLater(() -> {
            searchField.requestFocus();
            if (StringUtils.isNotBlank(initialSearchText)) {
                searchField.selectAll();
            }
        });
        
        // 显示对话框并等待
        dialogStage.showAndWait();
        return Optional.ofNullable(selectedBizDict);
    }
    
    /**
     * 显示对话框并返回选择的业务字段
     * @param bizDictList 业务字段列表
     * @return 选择的业务字段，如果取消则返回null
     */
    public Optional<BizDictPO> showAndWait(List<BizDictPO> bizDictList) {
        return showAndWait(bizDictList, "");
    }
    
    /**
     * 关闭对话框
     */
    public void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
