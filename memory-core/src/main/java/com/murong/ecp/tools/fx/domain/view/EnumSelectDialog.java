package com.murong.ecp.tools.fx.domain.view;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.EnumDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumGroupPO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 枚举选择对话框
 * 用于选择枚举类型，双击左侧记录时自动选择并关闭对话框
 * 采用左右侧布局：左侧显示枚举类型列表，右侧显示枚举项详情（只读）
 * 支持拖拽调整左右宽度
 */
@Component
public class EnumSelectDialog {
    
    private Stage dialogStage;
    private BizDictPO bizDictPO;
    private boolean isConfirmed = false;
    private EnumGroupPO selectedEnumType;
    @Autowired
    private GlobalProperties globalPropes;
    
    // FXML注入的组件
    @FXML private TableView<EnumGroupPO> enumTypeTableView;
    @FXML private TableColumn<EnumDictPO, String> enumNmeColumn;
    @FXML private TableColumn<EnumDictPO, String> dbNameColumn;
    @FXML private TableColumn<EnumDictPO, String> appNameColumn;
    @FXML private TextField filterField;
    @FXML private Button clearButton;
    @FXML private Button refreshButton;
    
    // 右侧组件（只读显示）
    @FXML private TextField enumNmeField;
    @FXML private TextField dbNameField;
    @FXML private TableView<EnumDictPO> enumItemTable;
    @FXML private TableColumn<EnumDictPO, String> enumCdColumn;
    @FXML private TableColumn<EnumDictPO, String> enumValColumn;
    @FXML private TableColumn<EnumDictPO, String> descCnItemColumn;
    @FXML private TableColumn<EnumDictPO, String> descEnItemColumn;
    @FXML private Button closeButton;
    
    // 拖拽调整宽度相关组件
    @FXML private Separator resizeSeparator;
    @FXML private HBox mainContentHBox;
    @FXML private VBox leftVBox;
    @FXML private VBox rightVBox;
    
    // 数据列表
    private ObservableList<EnumGroupPO> enumTypeList = FXCollections.observableArrayList();
    private ObservableList<EnumDictPO> enumItems = FXCollections.observableArrayList();
    
    @Autowired
    private EnumDictDao enumDictDao;
    
    // 拖拽相关变量
    private boolean isDragging = false;
    private double startX;
    private double startLeftWidth;
    private double startRightWidth;
    
    public EnumSelectDialog() {
        // 构造函数中不创建JavaFX组件，改为懒加载
    }
    
    /**
     * 懒加载初始化JavaFX组件
     */
    private void initializeDialog() {
        if (dialogStage != null) {
            return; // 已经初始化过了
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/enum_edit_dialog.fxml"));
            loader.setController(this);
            Scene scene = new Scene(loader.load());
            
            dialogStage = new Stage();
            dialogStage.setTitle("枚举选择对话框");
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.setResizable(true);
            dialogStage.setMinWidth(350);
            dialogStage.setMinHeight(400);
            dialogStage.setHeight(400);
            
            // FXML加载完成后初始化组件
            Platform.runLater(this::setupEventHandlers);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void setupEventHandlers() {
        setupLeftSide();
        setupRightSide();
        setupResizeHandlers();
    }
    
    private void setupResizeHandlers() {
        // 设置拖拽调整宽度的功能
        if (resizeSeparator != null) {
            resizeSeparator.setOnMousePressed(this::handleResizeMousePressed);
            resizeSeparator.setOnMouseDragged(this::handleResizeMouseDragged);
            resizeSeparator.setOnMouseReleased(this::handleResizeMouseReleased);
            
            // 设置鼠标样式，与新的界面样式保持一致
            resizeSeparator.setOnMouseEntered(e -> 
                resizeSeparator.setStyle("-fx-background-color: #bbb; -fx-pref-width: 6px; -fx-cursor: h-resize; -fx-background-radius: 3px;"));
            resizeSeparator.setOnMouseExited(e -> 
                resizeSeparator.setStyle("-fx-background-color: #ddd; -fx-pref-width: 6px; -fx-cursor: h-resize; -fx-background-radius: 3px;"));
        }
    }
    
    private void handleResizeMousePressed(MouseEvent event) {
        isDragging = true;
        startX = event.getSceneX();
        
        // 获取当前左右两侧的宽度
        if (leftVBox != null && rightVBox != null) {
            startLeftWidth = leftVBox.getWidth();
            startRightWidth = rightVBox.getWidth();
        }
        
        event.consume();
    }
    
    private void handleResizeMouseDragged(MouseEvent event) {
        if (!isDragging) return;
        
        double deltaX = event.getSceneX() - startX;
        
        if (leftVBox != null && rightVBox != null) {
            double newLeftWidth = startLeftWidth + deltaX;
            double newRightWidth = startRightWidth - deltaX;
            
            // 设置最小宽度限制，适应新的界面样式（缩小20%）
            double minLeftWidth = 256;
            double minRightWidth = 256;
            
            if (newLeftWidth >= minLeftWidth && newRightWidth >= minRightWidth) {
                leftVBox.setPrefWidth(newLeftWidth);
                rightVBox.setPrefWidth(newRightWidth);
            }
        }
        
        event.consume();
    }
    
    private void handleResizeMouseReleased(MouseEvent event) {
        isDragging = false;
        event.consume();
    }
    
    private void setupLeftSide() {
        // 设置左侧枚举类型表格
        if (enumTypeTableView != null) {

            appNameColumn.setCellValueFactory(new PropertyValueFactory<>("appName"));
            appNameColumn.setText("归属");
            appNameColumn.setPrefWidth(60);
            appNameColumn.setStyle("-fx-alignment: center;");

            enumNmeColumn.setCellValueFactory(new PropertyValueFactory<>("enumNme"));
            enumNmeColumn.setText("枚举名称");
            enumNmeColumn.setPrefWidth(120);
            
            dbNameColumn.setCellValueFactory(new PropertyValueFactory<>("dbName"));
            dbNameColumn.setText("数据库名称");
            dbNameColumn.setPrefWidth(120);
            

            
            // 设置表格数据
            enumTypeTableView.setItems(enumTypeList);
            
            // 双击选中行时，自动选择并关闭对话框
            enumTypeTableView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    EnumGroupPO selected = enumTypeTableView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        selectedEnumType = selected;
                        isConfirmed = true;
                        if (dialogStage != null) {
                            dialogStage.close();
                        }
                    }
                }
            });
            
            // 单击选中行时，右侧显示对应枚举项
            enumTypeTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadEnumItems(newVal.getEnumNme());
                    if (enumNmeField != null) enumNmeField.setText(newVal.getEnumNme());
                    if (dbNameField != null) dbNameField.setText(newVal.getDbName());
                }
            });
        }
        
        // 过滤功能
        if (filterField != null) {
            filterField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterEnumTypes(newVal);
            });
        }
        
        // 清空按钮
        if (clearButton != null) {
            clearButton.setOnAction(e -> {
                if (filterField != null) filterField.clear();
                if (enumTypeTableView != null) enumTypeTableView.getSelectionModel().clearSelection();
                clearRightSide();
            });
        }
        
        // 刷新按钮
        if (refreshButton != null) {
            refreshButton.setOnAction(e -> loadEnumTypeList());
        }
    }
    
    private void setupRightSide() {
        // 右侧表格设置（只读）
        if (enumItemTable != null) {
            enumItemTable.setEditable(false);
            enumItemTable.setItems(enumItems);
            
            // 枚举代码列
            enumCdColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEnumCd()));
            enumCdColumn.setText("枚举代码");
            enumCdColumn.setPrefWidth(100);
            
            // 枚举值列
            enumValColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEnumVal()));
            enumValColumn.setText("枚举值");
            enumValColumn.setPrefWidth(100);
            enumValColumn.setStyle("-fx-alignment: center;");
            
            // 中文描述列
            descCnItemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescCn()));
            descCnItemColumn.setText("中文描述");
            descCnItemColumn.setPrefWidth(150);
            
            // 英文描述列
            descEnItemColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescEn()));
            descEnItemColumn.setText("英文描述");
            descEnItemColumn.setPrefWidth(150);
        }
        
        // 设置右侧输入框为只读
        if (enumNmeField != null) {
            enumNmeField.setEditable(false);
        }
        if (dbNameField != null) {
            dbNameField.setEditable(false);
        }
        
        // 设置关闭按钮
        if (closeButton != null) {
            closeButton.setOnAction(e -> {
                isConfirmed = false;
                if (dialogStage != null) {
                    dialogStage.close();
                }
            });
        }
    }
    
    private void filterEnumTypes(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            if (enumTypeTableView != null) {
                enumTypeTableView.setItems(enumTypeList);
            }
        } else {
            ObservableList<EnumGroupPO> filteredList = FXCollections.observableArrayList();
            for (EnumGroupPO po : enumTypeList) {
                if (po.getEnumNme() != null && po.getEnumNme().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredList.add(po);
                }
            }
            if (enumTypeTableView != null) {
                enumTypeTableView.setItems(filteredList);
            }
        }
    }
    
    private void clearRightSide() {
        if (enumNmeField != null) enumNmeField.clear();
        if (dbNameField != null) dbNameField.clear();
        if (enumItems != null) {
            enumItems.clear();
        }
    }
    
    private void loadEnumTypeList() {
        try {
            // 从数据库加载枚举类型列表
            if (enumTypeList != null && enumDictDao != null) {
                enumTypeList.clear();

                EnumDictPO enumDictReqPO = new EnumDictPO();
                enumDictReqPO.setGroupName(globalPropes.getGroupName());
                List<EnumGroupPO> allEnumTypes =enumDictDao.groupByEnumNme(enumDictReqPO,globalPropes.getAppName());
                enumTypeList.addAll(allEnumTypes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadEnumItems(String enumNme) {
        try {
            if (enumDictDao != null) {
                EnumDictPO query = new EnumDictPO();
                query.setEnumNme(enumNme);
                List<EnumDictPO> items = enumDictDao.queryForList(query);
                if (enumItems != null) {
                    enumItems.setAll(items);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 显示对话框并等待用户选择
     * @param bizDictPO 要编辑的业务字典对象
     * @return 包含枚举名称和引用的结果对象，如果取消则返回空
     */
    public Optional<EnumEditResult> showAndWait(BizDictPO bizDictPO) {
        this.bizDictPO = bizDictPO;
        
        // 确保在JavaFX应用线程中初始化对话框
        if (Platform.isFxApplicationThread()) {
            initializeDialog();
        } else {
            Platform.runLater(this::initializeDialog);
            // 等待初始化完成
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // 清空输入框
        clearRightSide();
        
        // 加载枚举类型列表
        loadEnumTypeList();
        
        // 显示对话框
        if (dialogStage != null) {
            dialogStage.showAndWait();
        }
        
        if (isConfirmed && selectedEnumType != null) {
            String enumName = selectedEnumType.getEnumNme();
            String enumRef = selectedEnumType.getEnumRef(); // 根据枚举名称生成引用路径
            
            EnumEditResult result = new EnumEditResult();
            result.setEnumName(enumName);
            result.setEnumRef(enumRef);
            result.setBizDictPO(bizDictPO);
            
            return Optional.of(result);
        }
        
        return Optional.empty();
    }
    
    /**
     * 关闭对话框
     */
    public void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    /**
     * 枚举编辑结果
     */
    public static class EnumEditResult {
        private String enumName;
        private String enumRef;
        private BizDictPO bizDictPO;
        
        public String getEnumName() {
            return enumName;
        }
        
        public void setEnumName(String enumName) {
            this.enumName = enumName;
        }
        
        public String getEnumRef() {
            return enumRef;
        }
        
        public void setEnumRef(String enumRef) {
            this.enumRef = enumRef;
        }
        
        public BizDictPO getBizDictPO() {
            return bizDictPO;
        }
        
        public void setBizDictPO(BizDictPO bizDictPO) {
            this.bizDictPO = bizDictPO;
        }
    }
}
