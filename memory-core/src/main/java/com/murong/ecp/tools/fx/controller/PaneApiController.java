package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.domain.service.interfaces.JavaCodeService;
import com.murong.ecp.tools.fx.domain.service.interfaces.InterFaceEntityService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.BizDictDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.InterfaceDataDao;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserProjSettingDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import com.murong.ecp.tools.fx.infrastructure.view.valueobj.BizFieldVo;
import com.murong.ecp.tools.fx.infrastructure.view.ToggleSwitch;
import com.murong.ecp.tools.fx.infrastructure.view.BizDictEditingCell;
import com.murong.ecp.tools.fx.infrastructure.view.BizDictSelectionDialog;
import com.murong.ecp.tools.fx.infrastructure.utils.PinyinFilterUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Modality;
import javafx.util.converter.DefaultStringConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PaneApiController implements Initializable {

    @FXML private TextField apiNameField;
    @FXML private TextField apiPathField;
    @FXML private ComboBox<String> methodComboBox;
    @FXML private TextField apiDescField;
    @FXML private TabPane paramTabPane;
    @FXML private TreeTableView<BizFieldVo> requestTable;
    @FXML private TreeTableView<BizFieldVo> responseTable;
    @FXML private TreeTableColumn<BizFieldVo, String> reqParamNameColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> reqParamTypeColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> reqParamLengthColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> reqParamRemarkColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> reqParamEnglishRemarkColumn;
    @FXML private TreeTableColumn<BizFieldVo, Boolean> reqParamValueColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> reqParamEnumColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> respParamNameColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> respParamTypeColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> respParamLengthColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> respParamRemarkColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> respParamEnglishRemarkColumn;
    @FXML private TreeTableColumn<BizFieldVo, Boolean> respParamValueColumn;
    @FXML private TreeTableColumn<BizFieldVo, String> respParamEnumColumn;
    @FXML private TextField paramSearchField;
    @FXML private Button searchButton;
    @FXML private Button cloneButton;

    @Autowired
    GlobalProperties globalProps;
    @Autowired
    private JavaCodeService javaCodeService;
    @Autowired
    private InterFaceEntityService interFaceEntityService;
    @Autowired
    private BizDictDao bizDictDao;
    @Autowired
    private InterfaceDataDao interfaceDataDao;
    @Autowired
    private UserProjSettingDao projectSettingDao;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        methodComboBox.setItems(FXCollections.observableArrayList("GET", "POST", "PUT", "DELETE"));
        
        // 添加搜索框回车键监听
        paramSearchField.setOnAction(event -> searchParameter());
        
        // loadApiMenu(); // 右侧菜单栏已删除，不再需要加载API菜单
        
        // TreeTableColumn初始化
        reqParamNameColumn.setCellValueFactory(param -> param.getValue().getValue().fieldIdProperty());
        reqParamTypeColumn.setCellValueFactory(param -> param.getValue().getValue().typeProperty());
        reqParamLengthColumn.setCellValueFactory(param -> param.getValue().getValue().lengthProperty());
        reqParamRemarkColumn.setCellValueFactory(param -> param.getValue().getValue().remarkProperty());
        reqParamEnglishRemarkColumn.setCellValueFactory(param -> param.getValue().getValue().englishRemarkProperty());
        reqParamValueColumn.setCellValueFactory(param -> param.getValue().getValue().requiredProperty());
        reqParamValueColumn.setCellFactory(createToggleSwitchTreeTableCellFactory());
        reqParamEnumColumn.setCellValueFactory(param -> param.getValue().getValue().enumValuesProperty());
        respParamNameColumn.setCellValueFactory(param -> param.getValue().getValue().fieldIdProperty());
        respParamTypeColumn.setCellValueFactory(param -> param.getValue().getValue().typeProperty());
        respParamLengthColumn.setCellValueFactory(param -> param.getValue().getValue().lengthProperty());
        respParamRemarkColumn.setCellValueFactory(param -> param.getValue().getValue().remarkProperty());
        respParamEnglishRemarkColumn.setCellValueFactory(param -> param.getValue().getValue().englishRemarkProperty());
        respParamValueColumn.setCellValueFactory(param -> param.getValue().getValue().requiredProperty());
        respParamValueColumn.setCellFactory(createToggleSwitchTreeTableCellFactory());
        respParamEnumColumn.setCellValueFactory(param -> param.getValue().getValue().enumValuesProperty());
        // 设置表头为中文
        reqParamRemarkColumn.setText("中文描述");
        reqParamEnglishRemarkColumn.setText("英文描述");
        respParamRemarkColumn.setText("中文描述");
        respParamEnglishRemarkColumn.setText("英文描述");
        // 设置可编辑cellFactory
        setupEditableTreeTable();
        // 动态添加自定义表格线样式
        String css = getClass().getResource("/css/pane_interface.css").toExternalForm();
        requestTable.getStylesheets().add(css);
        responseTable.getStylesheets().add(css);
        
        // 初始化表格时添加空白行
        TreeItem<BizFieldVo> reqRoot = new TreeItem<>(new BizFieldVo());
        addEditableEmptyRow(reqRoot);
        requestTable.setRoot(reqRoot);
        requestTable.setShowRoot(false);
        
        TreeItem<BizFieldVo> respRoot = new TreeItem<>(new BizFieldVo());
        addEditableEmptyRow(respRoot);
        responseTable.setRoot(respRoot);
        responseTable.setShowRoot(false);

        // 初始化时不设置行工厂，让拖拽排序功能来设置
    }

    public void loadApiDetail(String transName) {
        // 查询接口详情
        InterFaceEntity entity = interFaceEntityService.getByTransName(transName, null);
        if (entity == null) return;
        apiNameField.setText(entity.getTransName());
        if (entity.getProperties() != null) {
            String interfaceUrl = entity.getProperties().getInterfaceUrl();
            String methodUrl = entity.getProperties().getMethodUrl();
            String fullPath = interfaceUrl != null ? interfaceUrl : "";
            if (methodUrl != null && !methodUrl.isEmpty()) {
                if (!fullPath.endsWith("/") && !methodUrl.startsWith("/")) {
                    fullPath += "/";
                }
                fullPath += methodUrl;
            }
            apiPathField.setText(fullPath);
        } else {
            apiPathField.setText("");
        }
        apiDescField.setText(entity.getTransCommentZh());
        methodComboBox.getSelectionModel().select(entity.getProperties() != null ? entity.getProperties().getMethodUrl() : "");
        // 请求参数
        List<RxField> reqTree = buildFieldTree(entity.getRequest());
        TreeItem<BizFieldVo> reqRoot = buildTree(reqTree);
        requestTable.setRoot(reqRoot);
        requestTable.setShowRoot(false);
        // 响应参数
        List<RxField> respTree = buildFieldTree(entity.getResponse());
        TreeItem<BizFieldVo> respRoot = buildTree(respTree);
        responseTable.setRoot(respRoot);
        responseTable.setShowRoot(false);
    }

    @FXML
    private void cloneApi() {
        showCloneDialog();
    }

    @FXML
    private void saveApi() {
        String apiName = apiNameField.getText();
        if (StringUtils.isBlank(apiName)) {
            System.out.println("接口名称不能为空");
            return;
        }
        InterfaceDataPO api = new InterfaceDataPO();
        api.setInterfaceName(apiName);
        api.setInterfaceUrl(apiPathField.getText());
        api.setTransCommentZh(apiDescField.getText());
        api.setMethodUrl(methodComboBox.getValue());
        // TODO: 保存参数列表
        // TODO: 保存到数据库或服务
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成功");
        alert.setHeaderText(null);
        alert.setContentText("保存成功！");
        alert.showAndWait();
    }

    @FXML
    private void searchParameter() {
        String searchText = paramSearchField.getText();
        if (StringUtils.isBlank(searchText)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText(null);
            alert.setContentText("请输入搜索内容！");
            alert.showAndWait();
            return;
        }

        TreeItem<BizFieldVo> foundItem = null;
        TreeTableView<BizFieldVo> targetTable = null;

        // 先搜索请求参数，再搜索响应参数
        foundItem = searchInTreeTable(requestTable, searchText);
        targetTable = requestTable;
        if (foundItem == null) {
            foundItem = searchInTreeTable(responseTable, searchText);
            targetTable = responseTable;
        }

        if (foundItem != null && targetTable != null) {
            // 选中并滚动到找到的项目
            targetTable.getSelectionModel().select(foundItem);
            targetTable.scrollTo(targetTable.getRow(foundItem));
            
            // 高亮显示找到的项目
            highlightFoundItem(targetTable, foundItem);
            
            // 切换到对应的标签页
            if (targetTable == requestTable) {
                paramTabPane.getSelectionModel().select(0);
            } else {
                paramTabPane.getSelectionModel().select(1);
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("搜索结果");
            alert.setHeaderText(null);
            alert.setContentText("未找到匹配的参数！");
            alert.showAndWait();
        }
    }

    /**
     * 在树形表格中搜索参数
     */
    private TreeItem<BizFieldVo> searchInTreeTable(TreeTableView<BizFieldVo> table, String searchText) {
        TreeItem<BizFieldVo> root = table.getRoot();
        if (root == null) return null;
        
        return searchInTreeItem(root, searchText.toLowerCase());
    }

    /**
     * 递归搜索树形项目
     */
    private TreeItem<BizFieldVo> searchInTreeItem(TreeItem<BizFieldVo> item, String searchText) {
        if (item == null || item.getValue() == null) return null;
        
        BizFieldVo vo = item.getValue();
        // 检查参数名、中文描述、英文描述是否包含搜索文本
        if ((vo.getFieldId() != null && vo.getFieldId().toLowerCase().contains(searchText)) ||
            (vo.getRemark() != null && vo.getRemark().toLowerCase().contains(searchText)) ||
            (vo.getEnglishRemark() != null && vo.getEnglishRemark().toLowerCase().contains(searchText))) {
            return item;
        }
        
        // 递归搜索子项目
        for (TreeItem<BizFieldVo> child : item.getChildren()) {
            TreeItem<BizFieldVo> result = searchInTreeItem(child, searchText);
            if (result != null) {
                return result;
            }
        }
        
        return null;
    }

    /**
     * 高亮显示找到的项目
     */
    private void highlightFoundItem(TreeTableView<BizFieldVo> table, TreeItem<BizFieldVo> foundItem) {
        // 清除之前的高亮
        clearHighlight(table);
        
        // 设置高亮样式，同时保持拖拽功能
        table.setRowFactory(tv -> new TreeTableRow<BizFieldVo>() {
            {
                // 设置拖拽检测
                setOnDragDetected(event -> {
                    if (getItem() != null) {
                        // 开始拖拽
                        Dragboard db = startDragAndDrop(TransferMode.MOVE);
                        
                        // 创建拖拽内容
                        ClipboardContent content = new ClipboardContent();
                        content.putString(getItem().getFieldId());
                        
                        // 设置拖拽内容
                        db.setContent(content);
                        
                        // 设置拖拽视图
                        SnapshotParameters sp = new SnapshotParameters();
                        sp.setFill(Color.TRANSPARENT);
                        WritableImage snapshot = snapshot(sp, null);
                        db.setDragView(snapshot);
                        
                        event.consume();
                    }
                });
                
                // 设置拖拽进入
                setOnDragOver(event -> {
                    if (event.getGestureSource() != this && 
                        event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });
                
                // 设置拖拽进入行
                setOnDragEntered(event -> {
                    if (event.getGestureSource() != this && 
                        event.getDragboard().hasString()) {
                        setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 2;");
                    }
                });
                
                // 设置拖拽离开行
                setOnDragExited(event -> {
                    updateRowStyle();
                });
                
                // 设置拖拽放下
                setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    boolean success = false;
                    
                    if (db.hasString()) {
                        // 获取拖拽源和目标
                        TreeItem<BizFieldVo> sourceItem = findTreeItemByFieldId(table, db.getString());
                        TreeItem<BizFieldVo> targetItem = getTreeItem();
                        
                        if (sourceItem != null && targetItem != null && sourceItem != targetItem) {
                            // 执行拖拽排序
                            success = performDragAndDropSort(sourceItem, targetItem);
                        }
                    }
                    
                    event.setDropCompleted(success);
                    event.consume();
                });
            }
            
            @Override
            protected void updateItem(BizFieldVo item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    updateRowStyle();
                }
            }
            
            /**
             * 更新行的样式（斑马纹、选中状态和高亮）
             */
            private void updateRowStyle() {
                if (getIndex() < 0) {
                    setStyle("");
                    getChildren().forEach(cell -> cell.setStyle(""));
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #199cd7; -fx-text-fill: white;");
                    getChildren().forEach(cell -> cell.setStyle("-fx-background-color: #199cd7; -fx-text-fill: white;"));
                } else if (getTreeItem() == foundItem) {
                    // 高亮找到的项目
                    setStyle("-fx-background-color: #ffeb3b; -fx-text-fill: black;");
                    getChildren().forEach(cell -> cell.setStyle("-fx-background-color: #ffeb3b; -fx-text-fill: black;"));
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #f7f7f7;");
                    getChildren().forEach(cell -> cell.setStyle("-fx-background-color: #f7f7f7;"));
                } else {
                    setStyle("-fx-background-color: white;");
                    getChildren().forEach(cell -> cell.setStyle("-fx-background-color: white;"));
                }
            }
        });
    }

    /**
     * 清除高亮
     */
    private void clearHighlight(TreeTableView<BizFieldVo> table) {
        // 重新启用拖拽排序功能
        if (table == requestTable || table == responseTable) {
            enableTreeTableDragAndDrop(table);
        }
    }



    @FXML
    private void backToTransaction() {
        // 获取MainController实例，切换transactionApi标签页内容回交易列表
        MainController mainController = MrSpringContextHolder.getBean(MainController.class);
        Tab transactionTab = mainController.getTabByKey("transactionApi");
        if (transactionTab != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pane_transaction.xml"));
                loader.setControllerFactory(clazz -> MrSpringContextHolder.getBean(clazz));
                Node transactionPage = loader.load();
                transactionTab.setContent(transactionPage);
                mainController.getPageContainer().getSelectionModel().select(transactionTab);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupEditableTreeTable() {
        DefaultStringConverter converter = new DefaultStringConverter();
        
        // 请求参数名列使用条件单元格工厂，根据类型决定是否弹出业务字段选择弹出框
        reqParamNameColumn.setCellFactory(col -> new ConditionalEditingTreeCell(converter, bizDictDao, bizDict -> {
            // 当用户选择业务字段时，自动填充相关字段
            TreeItem<BizFieldVo> currentItem = reqParamNameColumn.getTreeTableView().getSelectionModel().getSelectedItem();
            if (currentItem != null && currentItem.getValue() != null) {
                BizFieldVo vo = currentItem.getValue();
                // 接口参数使用驼峰命名（Java风格）
                vo.setFieldId(bizDict.getNameCamel());
                // 接口参数使用Java类型，而不是数据库类型
                vo.setType(convertDbTypeToJavaType(bizDict.getDbTyp()));
                vo.setLength(String.valueOf(bizDict.getLength()));
                vo.setRemark(bizDict.getCommentCn());
                vo.setEnglishRemark(bizDict.getCommentEn());
                vo.setRequired("Y".equals(bizDict.getNotNull()));
                vo.setEnumValues("");
                
                // 检查是否需要自动新增一行
                Platform.runLater(() -> {
                    TreeItem<BizFieldVo> parent = currentItem.getParent();
                    if (parent != null) {
                        int lastIndex = parent.getChildren().size() - 1;
                        if (lastIndex >= 0 && parent.getChildren().get(lastIndex) == currentItem) {
                            // 如果当前行是最后一行，且字段名不为空，则添加新行
                            if (StringUtils.isNotBlank(bizDict.getNameCamel()) && 
                                StringUtils.isNotBlank(vo.getFieldId())) {
                                addEditableEmptyRow(parent);
                            }
                        }
                    }
                });
            }
        }));
        
        reqParamTypeColumn.setCellFactory(col -> new TypeEditingTreeCell());
        reqParamLengthColumn.setCellFactory(col -> new EditingTreeCell(converter, "length"));
        reqParamRemarkColumn.setCellFactory(col -> new EditingTreeCell(converter, "remark"));
        reqParamEnglishRemarkColumn.setCellFactory(col -> new EditingTreeCell(converter, "englishRemark"));
        reqParamEnumColumn.setCellFactory(col -> new EditingTreeCell(converter, "enumValues"));
        
        // 响应参数名列使用TreeBizDictEditingCell，弹出业务字段选择弹出框
        respParamNameColumn.setCellFactory(col -> new ConditionalEditingTreeCell(converter, bizDictDao, bizDict -> {
            // 当用户选择业务字段时，自动填充相关字段
            TreeItem<BizFieldVo> currentItem = respParamNameColumn.getTreeTableView().getSelectionModel().getSelectedItem();
            if (currentItem != null && currentItem.getValue() != null) {
                BizFieldVo vo = currentItem.getValue();
                // 接口参数使用驼峰命名（Java风格）
                vo.setFieldId(bizDict.getNameCamel());
                // 接口参数使用Java类型，而不是数据库类型
                vo.setType(convertDbTypeToJavaType(bizDict.getDbTyp()));
                vo.setLength(String.valueOf(bizDict.getLength()));
                vo.setRemark(bizDict.getCommentCn());
                vo.setEnglishRemark(bizDict.getCommentEn());
                vo.setRequired("Y".equals(bizDict.getNotNull()));
                vo.setEnumValues("");
                
                // 检查是否需要自动新增一行
                Platform.runLater(() -> {
                    TreeItem<BizFieldVo> parent = currentItem.getParent();
                    if (parent != null) {
                        int lastIndex = parent.getChildren().size() - 1;
                        if (lastIndex >= 0 && parent.getChildren().get(lastIndex) == currentItem) {
                            // 如果当前行是最后一行，且字段名不为空，则添加新行
                            if (StringUtils.isNotBlank(bizDict.getNameCamel()) && 
                                StringUtils.isNotBlank(vo.getFieldId())) {
                                addEditableEmptyRow(parent);
                            }
                        }
                    }
                });
            }
        }));
        
        respParamTypeColumn.setCellFactory(col -> new TypeEditingTreeCell());
        respParamLengthColumn.setCellFactory(col -> new EditingTreeCell(converter, "length"));
        respParamRemarkColumn.setCellFactory(col -> new EditingTreeCell(converter, "remark"));
        respParamEnglishRemarkColumn.setCellFactory(col -> new EditingTreeCell(converter, "englishRemark"));
        respParamEnumColumn.setCellFactory(col -> new EditingTreeCell(converter, "enumValues"));
        
        // 启用拖拽排序功能
        enableDragAndDropSorting();
    }

    /**
     * 启用拖拽排序功能
     */
    private void enableDragAndDropSorting() {
        // 为请求参数表格启用拖拽排序
        enableTreeTableDragAndDrop(requestTable);
        
        // 为响应参数表格启用拖拽排序
        enableTreeTableDragAndDrop(responseTable);
    }
    
    /**
     * 为TreeTableView启用拖拽排序功能
     */
    private void enableTreeTableDragAndDrop(TreeTableView<BizFieldVo> treeTable) {
        // 设置行工厂，添加拖拽功能和斑马纹样式
        treeTable.setRowFactory(tv -> new TreeTableRow<BizFieldVo>() {
            {
                // 设置拖拽检测
                setOnDragDetected(event -> {
                    if (getItem() != null) {
                        // 开始拖拽
                        Dragboard db = startDragAndDrop(TransferMode.MOVE);
                        
                        // 创建拖拽内容
                        ClipboardContent content = new ClipboardContent();
                        content.putString(getItem().getFieldId());
                        
                        // 设置拖拽内容
                        db.setContent(content);
                        
                        // 设置拖拽视图
                        SnapshotParameters sp = new SnapshotParameters();
                        sp.setFill(Color.TRANSPARENT);
                        WritableImage snapshot = snapshot(sp, null);
                        db.setDragView(snapshot);
                        
                        event.consume();
                    }
                });
                
                // 设置拖拽进入
                setOnDragOver(event -> {
                    if (event.getGestureSource() != this && 
                        event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                    event.consume();
                });
                
                // 设置拖拽进入行
                setOnDragEntered(event -> {
                    if (event.getGestureSource() != this && 
                        event.getDragboard().hasString()) {
                        setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 2;");
                    }
                });
                
                // 设置拖拽离开行
                setOnDragExited(event -> {
                    updateRowStyle();
                });
                
                // 设置拖拽放下
                setOnDragDropped(event -> {
                    Dragboard db = event.getDragboard();
                    boolean success = false;
                    
                    if (db.hasString()) {
                        // 获取拖拽源和目标
                        TreeItem<BizFieldVo> sourceItem = findTreeItemByFieldId(treeTable, db.getString());
                        TreeItem<BizFieldVo> targetItem = getTreeItem();
                        
                        if (sourceItem != null && targetItem != null && sourceItem != targetItem) {
                            // 执行拖拽排序
                            success = performDragAndDropSort(sourceItem, targetItem);
                        }
                    }
                    
                    event.setDropCompleted(success);
                    event.consume();
                });
            }
            
            @Override
            protected void updateItem(BizFieldVo item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    updateRowStyle();
                }
            }
            
            /**
             * 更新行的样式（斑马纹和选中状态）
             */
            private void updateRowStyle() {
                if (getIndex() < 0) {
                    setStyle("");
                    getChildren().forEach(cell -> cell.setStyle(""));
                } else if (isSelected()) {
                    setStyle("-fx-background-color: #199cd7; -fx-text-fill: white;");
                    getChildren().forEach(cell -> cell.setStyle("-fx-background-color: #199cd7; -fx-text-fill: white;"));
                } else if (getIndex() % 2 == 0) {
                    setStyle("-fx-background-color: #f7f7f7;");
                    getChildren().forEach(cell -> cell.setStyle("-fx-background-color: #f7f7f7;"));
                } else {
                    setStyle("-fx-background-color: white;");
                    getChildren().forEach(cell -> cell.setStyle("-fx-background-color: white;"));
                }
            }
        });
    }
    
    /**
     * 根据字段ID查找TreeItem
     */
    private TreeItem<BizFieldVo> findTreeItemByFieldId(TreeTableView<BizFieldVo> treeTable, String fieldId) {
        return findTreeItemByFieldIdRecursive(treeTable.getRoot(), fieldId);
    }
    
    /**
     * 递归查找TreeItem
     */
    private TreeItem<BizFieldVo> findTreeItemByFieldIdRecursive(TreeItem<BizFieldVo> root, String fieldId) {
        if (root == null || root.getValue() == null) {
            return null;
        }
        
        // 检查当前节点
        if (fieldId.equals(root.getValue().getFieldId())) {
            return root;
        }
        
        // 递归检查子节点
        for (TreeItem<BizFieldVo> child : root.getChildren()) {
            TreeItem<BizFieldVo> result = findTreeItemByFieldIdRecursive(child, fieldId);
            if (result != null) {
                return result;
            }
        }
        
        return null;
    }
    
    /**
     * 执行拖拽排序操作
     */
    private boolean performDragAndDropSort(TreeItem<BizFieldVo> sourceItem, TreeItem<BizFieldVo> targetItem) {
        try {
            // 获取源项和目标项的父项
            TreeItem<BizFieldVo> sourceParent = sourceItem.getParent();
            TreeItem<BizFieldVo> targetParent = targetItem.getParent();
            
            if (sourceParent == null || targetParent == null) {
                return false;
            }
            
            // 如果源项和目标项在同一个父项下，执行同级排序
            if (sourceParent == targetParent) {
                return performSiblingSort(sourceItem, targetItem, sourceParent);
            } else {
                // 如果源项和目标项在不同的父项下，执行跨级移动
                return performCrossLevelMove(sourceItem, targetItem, sourceParent, targetParent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 执行同级排序
     */
    private boolean performSiblingSort(TreeItem<BizFieldVo> sourceItem, TreeItem<BizFieldVo> targetItem, 
                                     TreeItem<BizFieldVo> parent) {
        try {
            int sourceIndex = parent.getChildren().indexOf(sourceItem);
            int targetIndex = parent.getChildren().indexOf(targetItem);
            
            if (sourceIndex == -1 || targetIndex == -1) {
                return false;
            }
            
            // 从原位置移除
            parent.getChildren().remove(sourceIndex);
            
            // 插入到目标位置
            parent.getChildren().add(targetIndex, sourceItem);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 执行跨级移动
     */
    private boolean performCrossLevelMove(TreeItem<BizFieldVo> sourceItem, TreeItem<BizFieldVo> targetItem,
                                        TreeItem<BizFieldVo> sourceParent, TreeItem<BizFieldVo> targetParent) {
        try {
            // 从源父项移除
            sourceParent.getChildren().remove(sourceItem);
            
            // 添加到目标父项
            int targetIndex = targetParent.getChildren().indexOf(targetItem);
            if (targetIndex >= 0) {
                targetParent.getChildren().add(targetIndex, sourceItem);
            } else {
                targetParent.getChildren().add(sourceItem);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建ToggleSwitch树表单元格工厂
     */
    private javafx.util.Callback<TreeTableColumn<BizFieldVo, Boolean>, TreeTableCell<BizFieldVo, Boolean>> createToggleSwitchTreeTableCellFactory() {
        return column -> new TreeTableCell<BizFieldVo, Boolean>() {
            private final ToggleSwitch toggleSwitch = new ToggleSwitch();
            
            {
                // 监听ToggleSwitch状态变化
                toggleSwitch.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    TreeTableRow<BizFieldVo> row = getTreeTableRow();
                    if (row != null && row.getItem() != null) {
                        row.getItem().setRequired(newVal);
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TreeTableRow<BizFieldVo> row = getTreeTableRow();
                    if (row != null && row.getItem() != null) {
                        // 临时禁用监听器，避免初始化时触发
                        boolean wasSelected = toggleSwitch.isSelected();
                        boolean shouldBeSelected = row.getItem().isRequired();
                        
                        // 只有当值真的不同时才设置，避免触发监听器
                        if (wasSelected != shouldBeSelected) {
                            toggleSwitch.setSelected(shouldBeSelected);
                        }
                        setGraphic(toggleSwitch);
                        setAlignment(javafx.geometry.Pos.CENTER);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        };
    }

    public static class EditingTreeCell extends TreeTableCell<BizFieldVo, String> {
        private final TextField textField = new TextField();
        private final DefaultStringConverter converter;
        private final String property;
        public EditingTreeCell(DefaultStringConverter converter, String property) {
            this.converter = converter;
            this.property = property;
            
            // 为TextField添加拼音过滤功能
            PinyinFilterUtil.addPinyinFilter(textField);
            
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && isEditing()) {
                    commitEdit(textField.getText());
                }
            });
        }
        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                textField.setText(getItem());
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        }
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            // 如果是空行或行对象为null，直接清空
            if (empty || getTreeTableRow() == null || getTreeTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(item == null ? "" : item);
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(item == null ? "" : item);
                setGraphic(null);
            }
        }
        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            BizFieldVo vo = getTreeTableRow().getItem();
            if (vo != null) {
                switch (property) {
                    case "fieldId": vo.setFieldId(newValue); break;
                    case "type": vo.setType(newValue); break;
                    case "length": vo.setLength(newValue); break;
                    case "remark": vo.setRemark(newValue); break;
                    case "englishRemark": vo.setEnglishRemark(newValue); break;
                    case "enumValues": vo.setEnumValues(newValue); break;
                }
                
                // 如果是空白行且用户输入了字段名，触发空白行处理逻辑
                if (property.equals("fieldId") && vo.isEmptyRow() && StringUtils.isNotBlank(newValue)) {
                    // 验证字段名是否真的有值（不是空字符串或null）
                    if (StringUtils.isNotBlank(vo.getFieldId())) {
                        handleEmptyRowFilled(vo);
                    }
                }
            }
            setText(newValue);
            setGraphic(null);
        }
        
        /**
         * 处理空白行被填充后的逻辑
         */
        private void handleEmptyRowFilled(BizFieldVo vo) {
            TreeItem<BizFieldVo> currentItem = getTreeTableRow().getTreeItem();
            if (currentItem != null) {
                TreeItem<BizFieldVo> parent = currentItem.getParent();
                if (parent != null) {
                    // 获取当前行的索引
                    int currentIndex = parent.getChildren().indexOf(currentItem);
                    
                    // 在当前位置后插入新的空白行
                    BizFieldVo newEmptyVo = new BizFieldVo();
                    newEmptyVo.setFieldId("");
                    newEmptyVo.setType("请选择");
                    newEmptyVo.setLength("");
                    newEmptyVo.setRemark("");
                    newEmptyVo.setEnglishRemark("");
                    newEmptyVo.setRequired(false);
                    newEmptyVo.setEnumValues("");
                    newEmptyVo.setIsEmptyRow(true);
                    
                    TreeItem<BizFieldVo> newEmptyRow = new TreeItem<>(newEmptyVo);
                    parent.getChildren().add(currentIndex + 1, newEmptyRow);
                    
                    // 为新空白行设置基本的监听器（简化版本，避免静态上下文问题）
                    setupBasicEmptyRowListener(newEmptyRow, parent);
                    
                    // 将当前行标记为非空白行
                    vo.setIsEmptyRow(false);
                }
            }
        }
        
        /**
         * 为空白行设置基本的监听器（简化版本）
         */
        private void setupBasicEmptyRowListener(TreeItem<BizFieldVo> emptyRow, TreeItem<BizFieldVo> parent) {
            // 监听空白行的fieldId变化
            emptyRow.getValue().fieldIdProperty().addListener((obs, oldVal, newVal) -> {
                if (StringUtils.isNotBlank(newVal) && StringUtils.isBlank(oldVal)) {
                    // 验证字段名是否真的有值（不是空字符串或null）
                    if (StringUtils.isNotBlank(emptyRow.getValue().getFieldId())) {
                        // 用户输入了字段名，创建新的空白行
                        BizFieldVo newEmptyVo = new BizFieldVo();
                        newEmptyVo.setFieldId("");
                        newEmptyVo.setType("请选择");
                        newEmptyVo.setLength("");
                        newEmptyVo.setRemark("");
                        newEmptyVo.setEnglishRemark("");
                        newEmptyVo.setRequired(false);
                        newEmptyVo.setEnumValues("");
                        newEmptyVo.setIsEmptyRow(true);
                        
                        TreeItem<BizFieldVo> newEmptyRow = new TreeItem<>(newEmptyVo);
                        parent.getChildren().add(parent.getChildren().indexOf(emptyRow) + 1, newEmptyRow);
                        
                        // 设置新空白行的监听器
                        setupBasicEmptyRowListener(newEmptyRow, parent);
                        
                        // 将当前行标记为非空白行
                        emptyRow.getValue().setIsEmptyRow(false);
                    }
                }
            });
            
            // 监听类型变化，如果变为object或list，创建子节点结构
            emptyRow.getValue().typeProperty().addListener((obs, oldVal, newVal) -> {
                if (StringUtils.isNotBlank(newVal) && !newVal.equals(oldVal)) {
                    if ("object".equalsIgnoreCase(newVal) || "list".equalsIgnoreCase(newVal)) {
                        // 如果还没有子节点，创建子节点结构
                        if (emptyRow.getChildren().isEmpty()) {
                            TreeItem<BizFieldVo> childRoot = new TreeItem<>(new BizFieldVo());
                            setupBasicEmptyRowListener(childRoot, childRoot); // 递归设置
                            emptyRow.getChildren().add(childRoot);
                        }
                    }
                }
            });
        }
    }

    /**
     * 类型列专用的编辑单元格，使用下拉框选择类型
     */
    public static class TypeEditingTreeCell extends TreeTableCell<BizFieldVo, String> {
        private final ComboBox<String> typeComboBox = new ComboBox<>();
        
        public TypeEditingTreeCell() {
            // 设置下拉框的选项
            typeComboBox.setItems(FXCollections.observableArrayList(
                "String", "Long", "YGAmt", "Integer", "BigDecimal", "Boolean", "List", "Object"
            ));
            
            // 设置提示文本
            typeComboBox.setPromptText("请选择");
            
            // 设置可编辑，允许用户输入自定义类型
            typeComboBox.setEditable(true);
            
            // 监听选择变化
            typeComboBox.setOnAction(e -> commitEdit(typeComboBox.getValue()));
            
            // 监听焦点变化
            typeComboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && isEditing()) {
                    commitEdit(typeComboBox.getValue());
                }
            });
            
            // 监听输入变化，处理自定义类型
            typeComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
                if (StringUtils.isNotBlank(newVal)) {
                    // 检查是否在预定义类型中
                    boolean found = typeComboBox.getItems().stream()
                        .anyMatch(item -> item.equalsIgnoreCase(newVal));
                    
                    if (!found) {
                        // 如果不在预定义类型中，设置为"请选择"
                        typeComboBox.setValue("请选择");
                    }
                }
            });
        }
        
        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                String currentValue = getItem();
                if (StringUtils.isNotBlank(currentValue) && !"请选择".equals(currentValue)) {
                    typeComboBox.setValue(currentValue);
                } else {
                    typeComboBox.setValue("请选择");
                }
                setText(null);
                setGraphic(typeComboBox);
                typeComboBox.requestFocus();
            }
        }
        
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }
        
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTreeTableRow() == null || getTreeTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            
            if (isEditing()) {
                setText(null);
                setGraphic(typeComboBox);
            } else {
                String displayText = item;
                if (StringUtils.isBlank(displayText) || "请选择".equals(displayText)) {
                    displayText = "请选择";
                }
                setText(displayText);
                setGraphic(null);
            }
        }
        
        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            BizFieldVo vo = getTreeTableRow().getItem();
            if (vo != null) {
                vo.setType(newValue);
                
                // 如果是空白行且用户选择了类型，触发空白行处理逻辑
                if (vo.isEmptyRow() && StringUtils.isNotBlank(newValue) && !"请选择".equals(newValue)) {
                    handleTypeSelected(vo);
                }
            }
            setText(newValue);
            setGraphic(null);
        }
        
        /**
         * 处理类型被选择后的逻辑
         */
        private void handleTypeSelected(BizFieldVo vo) {
            TreeItem<BizFieldVo> currentItem = getTreeTableRow().getTreeItem();
            if (currentItem != null) {
                TreeItem<BizFieldVo> parent = currentItem.getParent();
                if (parent != null) {
                    // 获取当前行的索引
                    int currentIndex = parent.getChildren().indexOf(currentItem);
                    
                    // 在当前位置后插入新的空白行
                    BizFieldVo newEmptyVo = new BizFieldVo();
                    newEmptyVo.setFieldId("");
                    newEmptyVo.setType("请选择");
                    newEmptyVo.setLength("");
                    newEmptyVo.setRemark("");
                    newEmptyVo.setEnglishRemark("");
                    newEmptyVo.setRequired(false);
                    newEmptyVo.setEnumValues("");
                    newEmptyVo.setIsEmptyRow(true);
                    
                    TreeItem<BizFieldVo> newEmptyRow = new TreeItem<>(newEmptyVo);
                    parent.getChildren().add(currentIndex + 1, newEmptyRow);
                    
                    // 为新空白行设置基本的监听器
                    setupBasicEmptyRowListener(newEmptyRow, parent);
                    
                    // 将当前行标记为非空白行
                    vo.setIsEmptyRow(false);
                }
            }
        }
        
        /**
         * 为空白行设置基本的监听器（简化版本）
         */
        private void setupBasicEmptyRowListener(TreeItem<BizFieldVo> emptyRow, TreeItem<BizFieldVo> parent) {
            // 监听空白行的fieldId变化
            emptyRow.getValue().fieldIdProperty().addListener((obs, oldVal, newVal) -> {
                if (StringUtils.isNotBlank(newVal) && StringUtils.isBlank(oldVal)) {
                    // 用户输入了字段名，创建新的空白行
                    BizFieldVo newEmptyVo = new BizFieldVo();
                    newEmptyVo.setFieldId("");
                    newEmptyVo.setType("请选择");
                    newEmptyVo.setLength("");
                    newEmptyVo.setRemark("");
                    newEmptyVo.setEnglishRemark("");
                    newEmptyVo.setRequired(false);
                    newEmptyVo.setEnumValues("");
                    newEmptyVo.setIsEmptyRow(true);
                    
                    TreeItem<BizFieldVo> newEmptyRow = new TreeItem<>(newEmptyVo);
                    parent.getChildren().add(parent.getChildren().indexOf(emptyRow) + 1, newEmptyRow);
                    
                    // 设置新空白行的监听器
                    setupBasicEmptyRowListener(newEmptyRow, parent);
                    
                    // 将当前行标记为非空白行
                    emptyRow.getValue().setIsEmptyRow(false);
                }
            });
            
            // 监听类型变化，如果变为object或list，创建子节点结构
            emptyRow.getValue().typeProperty().addListener((obs, oldVal, newVal) -> {
                if (StringUtils.isNotBlank(newVal) && !newVal.equals(oldVal)) {
                    if ("object".equalsIgnoreCase(newVal) || "list".equalsIgnoreCase(newVal)) {
                        // 如果还没有子节点，创建子节点结构
                        if (emptyRow.getChildren().isEmpty()) {
                            TreeItem<BizFieldVo> childRoot = new TreeItem<>(new BizFieldVo());
                            setupBasicEmptyRowListener(childRoot, childRoot); // 递归设置
                            emptyRow.getChildren().add(childRoot);
                        }
                    }
                }
            });
        }
    }

    /**
     * 专门用于TreeTableView的业务字段编辑单元格，弹出业务字段选择弹出框
     */
    public static class TreeBizDictEditingCell extends TreeTableCell<BizFieldVo, String> {
        private final TextField textField = new TextField();
        private final DefaultStringConverter converter;
        private final BizDictDao bizDictDao;
        private final OnBizDictSelectedListener listener;
        
        public interface OnBizDictSelectedListener {
            void onBizDictSelected(BizDictPO bizDict);
        }
        
        public TreeBizDictEditingCell(DefaultStringConverter converter, BizDictDao bizDictDao, OnBizDictSelectedListener listener) {
            this.converter = converter;
            this.bizDictDao = bizDictDao;
            this.listener = listener;
            
            // 为TextField添加拼音过滤功能
            PinyinFilterUtil.addPinyinFilter(textField);
            
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && isEditing()) {
                    commitEdit(textField.getText());
                }
            });
        }
        
        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                textField.setText(getItem());
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
                
                // 双击进入编辑模式后，延迟一下再弹出业务字段选择对话框
                Platform.runLater(() -> {
                    showBizDictSelectionDialog();
                });
            }
        }
        
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }
        
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTreeTableRow() == null || getTreeTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(item == null ? "" : item);
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(item == null ? "" : item);
                setGraphic(null);
            }
        }
        
        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            BizFieldVo vo = getTreeTableRow().getItem();
            if (vo != null) {
                vo.setFieldId(newValue);
            }
            setText(newValue);
            setGraphic(null);
        }
        
        /**
         * 显示业务字段选择对话框
         */
        private void showBizDictSelectionDialog() {
            // 获取当前输入的文本作为搜索条件
            String searchText = textField.getText();
            
            // 调用BizDictDao搜索业务字段，不通过SQL筛选，直接查询所有数据
            List<BizDictPO> bizDictList = bizDictDao.searchByName(null);
            
            if (bizDictList.isEmpty()) {
                // 如果没有找到匹配的业务字段，显示提示
                Platform.runLater(() -> {
                    ViewUtils.alertForFail("未找到匹配的业务字段！");
                });
                return;
            }
            
            // 显示业务字段选择对话框，传递搜索条件
            BizDictSelectionDialog dialog = new BizDictSelectionDialog();
            Optional<BizDictPO> selectedBizDict = dialog.showAndWait(bizDictList, searchText);
            
            selectedBizDict.ifPresent(bizDict -> {
                // 用户选择了业务字段，更新单元格内容
                if (listener != null) {
                    listener.onBizDictSelected(bizDict);
                }
                
                // 更新文本字段，接口参数优先使用驼峰命名
                String displayText = StringUtils.isNotBlank(bizDict.getNameCamel()) ? 
                    bizDict.getNameCamel() : bizDict.getNameSnake();
                textField.setText(displayText);
                
                // 提交编辑
                commitEdit(displayText);
            });
        }
    }

    /**
     * 条件编辑单元格，根据类型决定是否弹出业务字段选择弹出框
     */
    public static class ConditionalEditingTreeCell extends TreeTableCell<BizFieldVo, String> {
        private final TextField textField = new TextField();
        private final DefaultStringConverter converter;
        private final BizDictDao bizDictDao;
        private final OnBizDictSelectedListener listener;
        
        public interface OnBizDictSelectedListener {
            void onBizDictSelected(BizDictPO bizDict);
        }
        
        public ConditionalEditingTreeCell(DefaultStringConverter converter, BizDictDao bizDictDao, OnBizDictSelectedListener listener) {
            this.converter = converter;
            this.bizDictDao = bizDictDao;
            this.listener = listener;
            
            // 为TextField添加拼音过滤功能
            PinyinFilterUtil.addPinyinFilter(textField);
            
            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && isEditing()) {
                    commitEdit(textField.getText());
                }
            });
        }
        
        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                textField.setText(getItem());
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
                
                // 检查当前行的类型，决定是否弹出业务字段选择弹出框
                TreeTableRow<BizFieldVo> row = getTreeTableRow();
                if (row != null && row.getItem() != null) {
                    BizFieldVo vo = row.getItem();
                    String type = vo.getType();
                    
                    // 如果类型是object或list，则使用自定义编辑，不弹出业务字段选择弹出框
                    if ("object".equalsIgnoreCase(type) || "list".equalsIgnoreCase(type)) {
                        // 使用自定义编辑，不弹出弹出框
                        return;
                    } else {
                        // 其他类型弹出业务字段选择弹出框
                        Platform.runLater(() -> {
                            showBizDictSelectionDialog();
                        });
                    }
                }
            }
        }
        
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }
        
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTreeTableRow() == null || getTreeTableRow().getItem() == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(item == null ? "" : item);
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(item == null ? "" : item);
                setGraphic(null);
            }
        }
        
        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            BizFieldVo vo = getTreeTableRow().getItem();
            if (vo != null) {
                vo.setFieldId(newValue);
            }
            setText(newValue);
            setGraphic(null);
        }
        
        /**
         * 显示业务字段选择对话框
         */
        private void showBizDictSelectionDialog() {
            // 获取当前输入的文本作为搜索条件
            String searchText = textField.getText();
            
            // 调用BizDictDao搜索业务字段，不通过SQL筛选，直接查询所有数据
            List<BizDictPO> bizDictList = bizDictDao.searchByName(null);
            
            if (bizDictList.isEmpty()) {
                // 如果没有找到匹配的业务字段，显示提示
                Platform.runLater(() -> {
                    ViewUtils.alertForFail("未找到匹配的业务字段！");
                });
                return;
            }
            
            // 显示业务字段选择对话框，传递搜索条件
            BizDictSelectionDialog dialog = new BizDictSelectionDialog();
            Optional<BizDictPO> selectedBizDict = dialog.showAndWait(bizDictList, searchText);
            
            selectedBizDict.ifPresent(bizDict -> {
                // 用户选择了业务字段，更新单元格内容
                if (listener != null) {
                    listener.onBizDictSelected(bizDict);
                }
                
                // 更新文本字段，接口参数优先使用驼峰命名
                String displayText = StringUtils.isNotBlank(bizDict.getNameCamel()) ? 
                    bizDict.getNameCamel() : bizDict.getNameSnake();
                textField.setText(displayText);
                
                // 提交编辑
                commitEdit(displayText);
            });
        }
    }

    /**
     * 为节点添加可编辑的空白行
     */
    private void addEditableEmptyRow(TreeItem<BizFieldVo> parent) {
        // 创建空白行
        BizFieldVo emptyVo = new BizFieldVo();
        emptyVo.setFieldId("");
        emptyVo.setType("请选择");
        emptyVo.setLength("");
        emptyVo.setRemark("");
        emptyVo.setEnglishRemark("");
        emptyVo.setRequired(false);
        emptyVo.setEnumValues("");
        emptyVo.setIsEmptyRow(true); // 标记为空白行
        
        TreeItem<BizFieldVo> emptyRow = new TreeItem<>(emptyVo);
        parent.getChildren().add(emptyRow);
        
        // 为空白行添加编辑完成监听器
        setupEmptyRowListener(emptyRow, parent);
    }

    /**
     * 设置空白行的编辑监听器
     */
    private void setupEmptyRowListener(TreeItem<BizFieldVo> emptyRow, TreeItem<BizFieldVo> parent) {
        // 监听空白行的fieldId变化
        emptyRow.getValue().fieldIdProperty().addListener((obs, oldVal, newVal) -> {
            if (StringUtils.isNotBlank(newVal) && StringUtils.isBlank(oldVal)) {
                // 验证字段名是否真的有值（不是空字符串或null）
                if (StringUtils.isNotBlank(emptyRow.getValue().getFieldId())) {
                    // 用户输入了字段名，创建新的空白行
                    addEditableEmptyRow(parent);
                    
                    // 将当前行标记为非空白行
                    emptyRow.getValue().setIsEmptyRow(false);
                }
            }
        });
        
        // 监听类型变化，如果变为object或list，创建子节点结构
        emptyRow.getValue().typeProperty().addListener((obs, oldVal, newVal) -> {
            if (StringUtils.isNotBlank(newVal) && !newVal.equals(oldVal)) {
                if ("object".equalsIgnoreCase(newVal) || "list".equalsIgnoreCase(newVal)) {
                    // 如果还没有子节点，创建子节点结构
                    if (emptyRow.getChildren().isEmpty()) {
                        TreeItem<BizFieldVo> childRoot = new TreeItem<>(new BizFieldVo());
                        addEditableEmptyRow(childRoot);
                        emptyRow.getChildren().add(childRoot);
                    }
                }
            }
        });
    }

    /**
     * 将数据库类型转换为Java类型
     */
    private String convertDbTypeToJavaType(String dbType) {
        if (StringUtils.isBlank(dbType)) {
            return "String";
        }
        
        String upperDbType = dbType.toUpperCase();
        
        // 数值类型
        if (upperDbType.contains("INT") || upperDbType.contains("BIGINT")) {
            return "Long";
        } else if (upperDbType.contains("DECIMAL") || upperDbType.contains("NUMERIC")) {
            return "BigDecimal";
        } else if (upperDbType.contains("FLOAT") || upperDbType.contains("DOUBLE")) {
            return "Double";
        } else if (upperDbType.contains("TINYINT")) {
            return "Integer";
        }
        // 字符串类型
        else if (upperDbType.contains("VARCHAR") || upperDbType.contains("CHAR") || 
                 upperDbType.contains("TEXT") || upperDbType.contains("LONGTEXT")) {
            return "String";
        }
        // 日期时间类型
        else if (upperDbType.contains("DATE") || upperDbType.contains("TIME") || 
                 upperDbType.contains("TIMESTAMP") || upperDbType.contains("DATETIME")) {
            return "Date";
        }
        // 布尔类型
        else if (upperDbType.contains("BOOLEAN") || upperDbType.contains("BOOL") || 
                 upperDbType.contains("BIT")) {
            return "Boolean";
        }
        // 二进制类型
        else if (upperDbType.contains("BLOB") || upperDbType.contains("BINARY")) {
            return "byte[]";
        }
        // 特殊类型
        else if (upperDbType.contains("JSON")) {
            return "Object";
        }
        // 默认返回String
        else {
            return "String";
        }
    }

    // 扁平字段转树结构
    private List<RxField> buildFieldTree(List<RxField> flatRxFields) {
        Map<String, RxField> nodeMap = new HashMap<>();
        List<RxField> roots = new ArrayList<>();
        for (RxField f : flatRxFields) {
            String[] parts = f.getNameCamel().split("\\.");
            StringBuilder path = new StringBuilder();
            RxField parent = null;
            for (int i = 0; i < parts.length; i++) {
                if (path.length() > 0) path.append(".");
                path.append(parts[i]);
                String key = path.toString();
                RxField node = nodeMap.get(key);
                if (node == null) {
                    node = new RxField();
                    node.setNameCamel(parts[i]);
                    node.setType(i == parts.length - 1 ? f.getType() : "object");
                    node.setLength(f.getLength());
                    node.setCommentCn(f.getCommentCn());
                    node.setCommentEn(f.getCommentEn());
                    node.setChildren(new ArrayList<>());
                    nodeMap.put(key, node);
                    if (parent != null) {
                        if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                        parent.getChildren().add(node);
                    } else if (i == 0) {
                        roots.add(node);
                    }
                }
                parent = node;
            }
            // 最后一个节点才赋值type/length/comment等
            if (parent != null && parts.length > 0) {
                parent.setType(f.getType());
                parent.setLength(f.getLength());
                parent.setCommentCn(f.getCommentCn());
                parent.setCommentEn(f.getCommentEn());
            }
        }
        return roots;
    }

    // 递归构建TreeItem
    private TreeItem<BizFieldVo> buildTree(List<RxField> rxFields) {
        TreeItem<BizFieldVo> root = new TreeItem<>(new BizFieldVo());
        if (rxFields != null) {
            for (RxField f : rxFields) {
                BizFieldVo vo = new BizFieldVo();
                vo.setFieldId(f.getNameCamel());
                vo.setType(f.getType());
                vo.setLength(f.getLength() == null ? "" : String.valueOf(f.getLength()));
                vo.setRemark(f.getCommentCn());
                vo.setEnglishRemark(f.getCommentEn());
                TreeItem<BizFieldVo> item = new TreeItem<>(vo);
                // 如果是List或object，递归children
                if (("List".equalsIgnoreCase(f.getType()) || "object".equalsIgnoreCase(f.getType())) && f.getChildren() != null && !f.getChildren().isEmpty()) {
                    item.getChildren().addAll(buildTree(f.getChildren()).getChildren());
                } else if ("List".equalsIgnoreCase(f.getType()) || "object".equalsIgnoreCase(f.getType())) {
                    // 如果是object或list类型但没有子节点，也要添加空白行
                    addEditableEmptyRow(item);
                }
                root.getChildren().add(item);
            }
        }
        
        // 为每个节点添加一个可编辑的空白行
        addEditableEmptyRow(root);
        
        return root;
    }

    /**
     * 设置选中的API名称（从交易列表跳转时调用）
     */
    public void setSelectedApi(String transName) {
        if (StringUtils.isNotBlank(transName)) {
            // 调用loadApiDetail来加载和显示完整的API数据
            loadApiDetail(transName);
        } else {
            // 新增接口时，清空所有字段
            clearApiFields();
        }
    }

    /**
     * 清空API字段，用于新增接口
     */
    private void clearApiFields() {
        apiNameField.clear();
        apiPathField.clear();
        apiDescField.clear();
        methodComboBox.getSelectionModel().clearSelection();
        
        // 清空请求参数表格
        TreeItem<BizFieldVo> reqRoot = new TreeItem<>(new BizFieldVo());
        addEditableEmptyRow(reqRoot);
        requestTable.setRoot(reqRoot);
        requestTable.setShowRoot(false);
        
        // 清空响应参数表格
        TreeItem<BizFieldVo> respRoot = new TreeItem<>(new BizFieldVo());
        addEditableEmptyRow(respRoot);
        responseTable.setRoot(respRoot);
        responseTable.setShowRoot(false);
    }

    /**
     * 显示克隆API的查询弹出框
     */
    private void showCloneDialog() {
        try {
            // 创建查询弹出框
            Dialog<InterfaceDataPO> dialog = new Dialog<>();
            dialog.setTitle("克隆API");
            dialog.setHeaderText("请选择要克隆的API接口");
            dialog.initModality(Modality.APPLICATION_MODAL);
            
            // 设置弹出框大小
            dialog.setResizable(true);
            dialog.setWidth(1000);
            dialog.setHeight(600);
            
            // 创建查询条件区域
            VBox dialogContent = new VBox(10);
            dialogContent.setPadding(new Insets(10));
            
            // 查询条件
            HBox searchBox = new HBox(10);
            TextField searchField = new TextField();
            searchField.setPromptText("输入接口名称、交易名称或描述进行搜索");
            searchField.setPrefWidth(250);
            
            // 项目名称下拉框
            ComboBox<String> projectComboBox = new ComboBox<>();
            projectComboBox.setPromptText("选择项目名称");
            projectComboBox.setPrefWidth(120);
            
            // 加载项目名称下拉框数据
            loadProjectNames(projectComboBox);
            
            Button searchBtn = new Button("查询");
            searchBox.getChildren().addAll(new Label("搜索:"), searchField, new Label("项目:"), projectComboBox, searchBtn);
            
            // 创建表格
            TableView<InterfaceDataPO> table = new TableView<>();
            table.setPrefHeight(400);
            
            // 创建表格列
            TableColumn<InterfaceDataPO, String> transNameCol = new TableColumn<>("交易名称");
            transNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTransName()));
            transNameCol.setPrefWidth(120);
            
            TableColumn<InterfaceDataPO, String> interfaceNameCol = new TableColumn<>("接口名称");
            interfaceNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getInterfaceName()));
            interfaceNameCol.setPrefWidth(120);
            
            TableColumn<InterfaceDataPO, String> urlCol = new TableColumn<>("接口路径");
            urlCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMethodUrl()));
            urlCol.setPrefWidth(150);
            
            TableColumn<InterfaceDataPO, String> transCommentZhCol = new TableColumn<>("中文描述");
            transCommentZhCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTransCommentZh()));
            transCommentZhCol.setPrefWidth(150);
            
            TableColumn<InterfaceDataPO, String> transCommentEnCol = new TableColumn<>("英文描述");
            transCommentEnCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTransCommentEn()));
            transCommentEnCol.setPrefWidth(150);
            
            table.getColumns().addAll(transNameCol, interfaceNameCol, urlCol, transCommentZhCol, transCommentEnCol);
            
            // 添加双击选择功能
            table.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    InterfaceDataPO selected = table.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        dialog.setResult(selected);
                        dialog.close();
                    }
                }
            });
            
            // 查询按钮事件
            searchBtn.setOnAction(event -> {
                String searchText = searchField.getText();
                String selectedProject = projectComboBox.getValue();
                performCloneSearch(table, searchText, selectedProject);
            });
            
            // 回车键触发查询
            searchField.setOnAction(event -> {
                String searchText = searchField.getText();
                String selectedProject = projectComboBox.getValue();
                performCloneSearch(table, searchText, selectedProject);
            });
            
            // 项目名称变化时自动查询
            projectComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                String searchText = searchField.getText();
                performCloneSearch(table, searchText, newVal);
            });
            
            // 初始加载数据
            performCloneSearch(table, "", "全部");
            
            // 组装弹出框内容
            dialogContent.getChildren().addAll(searchBox, table);
            dialog.getDialogPane().setContent(dialogContent);
            
            // 添加按钮
            ButtonType selectButtonType = new ButtonType("选择", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, cancelButtonType);
            
            // 设置结果转换器
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == selectButtonType) {
                    return table.getSelectionModel().getSelectedItem();
                }
                return null;
            });
            
            // 显示弹出框并处理结果
            Optional<InterfaceDataPO> result = dialog.showAndWait();
            result.ifPresent(selectedApi -> {
                // 克隆选中的API，传递当前选中的项目名称
                String selectedProject = projectComboBox.getValue();
                cloneSelectedApi(selectedApi, selectedProject);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("显示克隆弹出框失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行克隆搜索
     */
    private void performCloneSearch(TableView<InterfaceDataPO> table, String searchText, String selectedProject) {
        try {
            InterfaceDataPO reqPO = new InterfaceDataPO();
            reqPO.setGroupName(globalProps.getGroupName());
            
            // 如果选择了特定项目，设置项目名称条件
            if (StringUtils.isNotBlank(selectedProject) && !"全部".equals(selectedProject)) {
                reqPO.setProjectName(selectedProject);
            }
            
            List<InterfaceDataPO> list;
            if (StringUtils.isNotBlank(searchText)) {
                list = interfaceDataDao.queryForSearch(reqPO.getAppName(), searchText);
            } else {
                list = interfaceDataDao.queryForList(reqPO);
            }
            
            // 更新表格数据
            ObservableList<InterfaceDataPO> observableList = FXCollections.observableArrayList(list);
            table.setItems(observableList);
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("查询数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载项目名称下拉框数据
     */
    private void loadProjectNames(ComboBox<String> projectComboBox) {
        try {
            // 获取当前项目群名称
            String currentGroupName = globalProps.getGroupName();
            if (StringUtils.isBlank(currentGroupName)) {
                projectComboBox.getItems().add("请选择项目群");
                return;
            }
            
            // 查询当前项目群下的所有项目
            UserProjSettingPO queryPO = new UserProjSettingPO();
            queryPO.setGroupName(currentGroupName);
            queryPO.setShowFlag("Y"); // 只显示启用的项目
            
            List<UserProjSettingPO> projectList = projectSettingDao.queryForList(queryPO);
            
            // 清空下拉框并添加项目名称
            projectComboBox.getItems().clear();
            projectComboBox.getItems().add("全部"); // 添加"全部"选项
            
            if (projectList != null && !projectList.isEmpty()) {
                for (UserProjSettingPO project : projectList) {
                    if (StringUtils.isNotBlank(project.getProjectName())) {
                        projectComboBox.getItems().add(project.getProjectName());
                    }
                }
            }
            
            // 获取横幅中当前选中的项目名称，并设置为下拉框的初始选中值
            String currentProjectName = globalProps.getProjectName();
            if (StringUtils.isNotBlank(currentProjectName) && projectComboBox.getItems().contains(currentProjectName)) {
                projectComboBox.setValue(currentProjectName);
            } else {
                // 如果没有找到当前项目或当前项目为空，则选中"全部"
                projectComboBox.getSelectionModel().selectFirst();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            projectComboBox.getItems().clear();
            projectComboBox.getItems().add("加载失败");
        }
    }

    /**
     * 克隆选中的API
     */
    private void cloneSelectedApi(InterfaceDataPO selectedApi, String selectedProject) {
        try {
            if (selectedApi == null) {
                ViewUtils.alertForFail("请选择要克隆的API");
                return;
            }
            
            // 获取完整的API详情，使用选中的项目名称进行查询
            InterfaceDataPO queryPO = new InterfaceDataPO();
            queryPO.setTransName(selectedApi.getTransName());
            queryPO.setInterfaceName(selectedApi.getInterfaceName());
            queryPO.setProjectName(selectedProject);
            queryPO.setGroupName(globalProps.getGroupName());
            
            InterfaceDataPO resultPO = interfaceDataDao.queryOne(queryPO);
            if (resultPO == null) {
                ViewUtils.alertForFail("未找到选中的API详情数据");
                return;
            }
            
            // 使用选中的项目名称查询InterFaceEntity
            InterFaceEntity entity = null;
            try {
                // 临时修改全局属性中的项目名称，以便getByTransName能使用正确的项目名称
                String originalProjectName = globalProps.getProjectName();
                globalProps.setProjectName(selectedProject);
                
                entity = interFaceEntityService.getByTransName(selectedApi.getTransName(), selectedApi.getInterfaceName());
                
                // 恢复原始项目名称
                globalProps.setProjectName(originalProjectName);
            } catch (Exception e) {
                ViewUtils.alertForFail("获取API详情失败: " + e.getMessage());
                return;
            }
            
            if (entity == null) {
                ViewUtils.alertForFail("未找到选中的API详情数据");
                return;
            }
            
            // 克隆API数据
            String clonedName = selectedApi.getTransName() + "_clone";
            apiNameField.setText(clonedName);
            
            if (entity.getProperties() != null) {
                String interfaceUrl = entity.getProperties().getInterfaceUrl();
                String methodUrl = entity.getProperties().getMethodUrl();
                String fullPath = interfaceUrl != null ? interfaceUrl : "";
                if (methodUrl != null && !methodUrl.isEmpty()) {
                    if (!fullPath.endsWith("/") && !methodUrl.startsWith("/")) {
                        fullPath += "/";
                    }
                    fullPath += methodUrl;
                }
                apiPathField.setText(fullPath);
            }
            
            apiDescField.setText(entity.getTransCommentZh() + " (克隆)");
            methodComboBox.getSelectionModel().select(entity.getProperties() != null ? entity.getProperties().getMethodUrl() : "");
            
            // 克隆请求参数
            List<RxField> reqTree = buildFieldTree(entity.getRequest());
            TreeItem<BizFieldVo> reqRoot = buildTree(reqTree);
            requestTable.setRoot(reqRoot);
            requestTable.setShowRoot(false);
            
            // 克隆响应参数
            List<RxField> respTree = buildFieldTree(entity.getResponse());
            TreeItem<BizFieldVo> respRoot = buildTree(respTree);
            responseTable.setRoot(respRoot);
            responseTable.setShowRoot(false);
            
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("克隆API失败: " + e.getMessage());
        }
    }
} 