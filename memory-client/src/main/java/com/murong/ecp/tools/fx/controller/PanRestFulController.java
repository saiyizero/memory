package com.murong.ecp.tools.fx.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.domain.entity.InterFaceEntity;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.domain.service.interfaces.RestFulConnService;
import com.murong.ecp.tools.fx.domain.service.interfaces.InterFaceEntityService;
import com.murong.ecp.tools.fx.domain.service.terminal.SShDomainService;
import com.murong.ecp.tools.fx.infrastructure.converter.DatabaseConvert;
import com.murong.ecp.tools.fx.infrastructure.converter.InterfaceConvert;
import com.murong.ecp.tools.fx.infrastructure.rpc.CommonClassRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.DebugLogRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServerInfoRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ServerInfoPO;
import com.murong.ecp.tools.fx.infrastructure.utils.JsonFormatUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.RxFileUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.LogDialogUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class PanRestFulController {
    @FXML
    private ComboBox<String> ipComboBox;
    @FXML
    private TextField urlField;
    @FXML
    private ComboBox<String> methodComboBox;
    @FXML
    private Button sendButton;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Button detailButton;
    @FXML
    private Button logButton;
    @FXML
    private VBox rightMenuBar;
    @FXML
    private Button caseQueryButton;
    @FXML
    private ListView<String> caseListView;
    @FXML
    private Button importJsonButton;
    @FXML
    private Button exportHttpButton;
    @FXML
    private Label transNameField;
    @FXML
    private Label transCommentZhLabel;
    @FXML
    private TreeTableView<ParamItem> requestTable;
    @FXML
    private TreeTableView<ParamItem> responseTable;
    @FXML
    private TreeTableColumn<ParamItem, String> reqParamNameColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> reqParamTypeColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> reqParamLengthColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> reqParamRemarkColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> reqParamValueColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> respParamNameColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> respParamTypeColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> respParamLengthColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> respParamRemarkColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> respParamValueColumn;
    @FXML
    private TreeTableView<ParamItem> commonParamTable;
    @FXML
    private TreeTableColumn<ParamItem, String> commonParamNameColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> commonParamTypeColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> commonParamLengthColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> commonParamValueColumn;
    @FXML
    private TreeTableColumn<ParamItem, String> commonParamRemarkColumn;

    @Autowired
    private RestFulConnService restFulConnService;
    @Autowired
    private InterFaceEntityService interFaceEntityService;
    @Autowired
    private ServerInfoRpcService serverInfoRpcService;
    @Autowired
    private CommonClassRpcService commonClassRpcService;
    @Autowired
    private GlobalProperties globalProperties;
    @Autowired
    private DebugLogRpcService debugLogRpcService;
    @Autowired
    private SShDomainService sshDomainService;

    // 参数/请求头数据模型
    @Data
    public static class ParamItem {
        private String comment;
        private String name;
        private String value;
        private String type;
        private String length;
        private List<ParamItem> children = new java.util.ArrayList<>();
        public ParamItem(String comment, String name, String value, String type, String length) {
            this.comment = comment;
            this.name = name;
            this.value = value;
            this.type = type;
            this.length = length;
        }
    }
    
    private ObservableList<ParamItem> headerData = FXCollections.observableArrayList();
    private List<DebugLogPO> debugLogCache = new java.util.ArrayList<>();

    @FXML
    private void onImportJson() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("导入JSON");
        dialog.setHeaderText("请输入JSON报文");
        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        TextArea textArea = new TextArea();
        textArea.setPromptText("请输入JSON...");
        textArea.setPrefWidth(500);
        textArea.setPrefHeight(300);
        dialog.getDialogPane().setContent(textArea);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return textArea.getText();
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(jsonStr -> {
            importJsonToRequestTable(jsonStr);
        });
    }

    @FXML
    public void initialize() {
        methodComboBox.setItems(FXCollections.observableArrayList("GET", "POST", "PUT", "DELETE"));
        methodComboBox.setValue("POST");
        // 右侧菜单栏宽度 - 移除最小宽度限制，允许自由调整

        //初始化IP地址下拉框
        initServerIpComboBox();

        // 移除分割面板位置设置，允许用户自由拖拽调整
        // 详情按钮事件
        if (detailButton != null) {
            detailButton.setOnAction(e -> showDebugLogDetailDialog());
        }
        // 日志按钮事件
        if (logButton != null) {
            logButton.setOnAction(e -> onDownloadLogFile());
        }
        // 初始化请求参数/响应参数表格
        if (requestTable != null) {
            reqParamNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getName()));
            reqParamTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getType()));
            reqParamLengthColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getLength()));
            reqParamRemarkColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getComment()));
            // 参数值列自定义cellFactory（始终显示输入框，且实时更新value）
            reqParamValueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getValue()));
            reqParamValueColumn.setCellFactory(col -> new TreeTableCell<ParamItem, String>() {
                private final TextField textField = new TextField();
                private final Button addButton = new Button("+");
                private final Button removeButton = new Button("-");
                {
                    addButton.setFocusTraversable(false);
                    removeButton.setFocusTraversable(false);
                    addButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #000; -fx-font-weight: bold;");
                    removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #000; -fx-font-weight: bold;");
                }
                // 判断当前节点是否在List类型节点下
                private boolean isUnderList(TreeItem<ParamItem> item) {
                    TreeItem<ParamItem> parent = item != null ? item.getParent() : null;
                    while (parent != null && parent.getValue() != null) {
                        if ("List".equalsIgnoreCase(parent.getValue().getType())) {
                            return true;
                        }
                        parent = parent.getParent();
                    }
                    return false;
                }
                // 判断是否为List下Object分组的子节点
                private boolean isListObjectField(TreeItem<ParamItem> item) {
                    if (item == null || item.getParent() == null || item.getParent().getParent() == null) return false;
                    ParamItem parentParam = item.getParent().getValue();
                    ParamItem grandParentParam = item.getParent().getParent().getValue();
                    return parentParam != null && "Object".equalsIgnoreCase(parentParam.getType())
                        && grandParentParam != null && "List".equalsIgnoreCase(grandParentParam.getType());
                }
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    TreeItem<ParamItem> treeItem = getTreeTableRow() != null ? getTreeTableRow().getTreeItem() : null;
                    ParamItem param = treeItem != null ? treeItem.getValue() : null;
                    if (empty || param == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    // 类型为List，显示+号
                    if ("List".equalsIgnoreCase(param.getType())) {
                        addButton.setOnAction(e -> {
                            // 先保存模板（第一个Object或字段组）
                            TreeItem<ParamItem> template = null;
                            if (!treeItem.getChildren().isEmpty()) {
                                if ("Object".equalsIgnoreCase(treeItem.getChildren().get(0).getValue().getType())) {
                                    template = cloneTree(treeItem.getChildren().get(0));
                                } else {
                                    List<TreeItem<ParamItem>> fieldTemplates = new ArrayList<>(treeItem.getChildren());
                                    ParamItem groupParam = new ParamItem("分组", "Object[1]", null, "Object", null);
                                    template = new TreeItem<>(groupParam);
                                    for (TreeItem<ParamItem> fieldTemplate : fieldTemplates) {
                                        template.getChildren().add(cloneTree(fieldTemplate));
                                    }
                                }
                            }
                            // 删除所有参数名不是Object[数字]的节点
                            treeItem.getChildren().removeIf(child -> {
                                String name = child.getValue().getName();
                                return name == null || !name.matches("Object\\[\\d+\\]");
                            });
                            // 增加新的Object分组
                            if (template != null) {
                                template.getValue().setName("Object[" + (treeItem.getChildren().size() + 1) + "]");
                                treeItem.getChildren().add(template);
                                getTreeTableView().refresh();
                            }
                        });
                        setText(null);
                        setGraphic(addButton);
                    }
                    // List下的Object分组节点，只有从第二个才显示-号
                    else if (treeItem.getParent() != null && treeItem.getParent().getValue() != null
                            && "List".equalsIgnoreCase(treeItem.getParent().getValue().getType())
                            && "Object".equalsIgnoreCase(param.getType())) {
                        int idx = treeItem.getParent().getChildren().indexOf(treeItem);
                        if (idx > 0) { // 不是第一个Object分组才显示减号
                            removeButton.setOnAction(e -> {
                                treeItem.getParent().getChildren().remove(treeItem);
                                getTreeTableView().refresh();
                            });
                            setText(null);
                            setGraphic(removeButton);
                        } else {
                            setText(null);
                            setGraphic(null);
                        }
                    }
                    // 普通字段，显示输入框，但如果在List下则不显示输入框，除非是List下Object分组的子节点
                    else if (!"List".equalsIgnoreCase(param.getType()) && !"Object".equalsIgnoreCase(param.getType()) && (!isUnderList(treeItem) || isListObjectField(treeItem))) {
                        if (!textField.textProperty().isBound()) {
                            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                                ParamItem p = getTreeTableRow() != null ? (ParamItem) getTreeTableRow().getItem() : null;
                                if (p != null) {
                                    p.setValue(newVal);
                                }
                            });
                        }
                        textField.setText(item == null ? "" : item);
                        setText(null);
                        setGraphic(textField);
                    } else {
                        setText(null);
                        setGraphic(null);
                    }
                }
            });
            requestTable.getStylesheets().add(getClass().getResource("/css/pane_restful.css").toExternalForm());
            // 斑马纹和选中高亮，与PaneApiController一致
            requestTable.setRowFactory(tv -> new TreeTableRow<ParamItem>() {
                @Override
                protected void updateItem(ParamItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0) {
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
        if (responseTable != null) {
            respParamNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getName()));
            respParamTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getType()));
            respParamLengthColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getLength()));
            respParamRemarkColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getComment()));
            // 参数值列自定义cellFactory
            respParamValueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getValue()));
            respParamValueColumn.setCellFactory(col -> new TreeTableCell<ParamItem, String>() {
                private final TextField textField = new TextField();
                @Override
                public void startEdit() {
                    if (!isEmpty() && isEditableCell()) {
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
                    ParamItem param = getTreeTableRow() != null ? (ParamItem) getTreeTableRow().getItem() : null;
                    if (empty || param == null || isListOrObject(param)) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        if (isEditing()) {
                            textField.setText(item == null ? "" : item);
                            setText(null);
                            setGraphic(textField);
                        } else {
                            setText(item == null ? "" : item);
                            setGraphic(null);
                        }
                    }
                }
                @Override
                public void commitEdit(String newValue) {
                    super.commitEdit(newValue);
                    ParamItem param = getTreeTableRow() != null ? (ParamItem) getTreeTableRow().getItem() : null;
                    if (param != null) {
                        param.setValue(newValue);
                    }
                    setText(newValue);
                    setGraphic(null);
                }
                private boolean isListOrObject(ParamItem param) {
                    String type = param.getType();
                    return "List".equalsIgnoreCase(type) || "object".equalsIgnoreCase(type);
                }
                private boolean isEditableCell() {
                    ParamItem param = getTreeTableRow() != null ? (ParamItem) getTreeTableRow().getItem() : null;
                    return param != null && !isListOrObject(param);
                }
            });
            responseTable.getStylesheets().add(getClass().getResource("/css/pane_restful.css").toExternalForm());
            // 斑马纹和选中高亮，与PaneApiController一致
            responseTable.setRowFactory(tv -> new TreeTableRow<ParamItem>() {
                @Override
                protected void updateItem(ParamItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0) {
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
        // 公共参数表格初始化，表头与请求参数一致
        if (commonParamTable != null) {
            commonParamNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getName()));
            commonParamTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getType()));
            commonParamLengthColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getLength()));
            commonParamRemarkColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getComment()));
            commonParamValueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getValue()));
            // 参数值列自定义cellFactory（与请求参数表单一致，除Object和List外为输入框）
            commonParamValueColumn.setCellFactory(col -> new TreeTableCell<ParamItem, String>() {
                private final TextField textField = new TextField();
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    ParamItem param = getTreeTableRow() != null ? (ParamItem) getTreeTableRow().getItem() : null;
                    if (empty || param == null || isListOrObject(param)) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        textField.setText(item == null ? "" : item);
                        textField.textProperty().addListener((obs, oldVal, newVal) -> {
                            ParamItem p = getTreeTableRow() != null ? (ParamItem) getTreeTableRow().getItem() : null;
                            if (p != null) {
                                p.setValue(newVal);
                            }
                        });
                        setText(null);
                        setGraphic(textField);
                    }
                }
                private boolean isListOrObject(ParamItem param) {
                    String type = param.getType();
                    return "List".equalsIgnoreCase(type) || "object".equalsIgnoreCase(type);
                }
            });
            commonParamTable.setRowFactory(tv -> new TreeTableRow<ParamItem>() {
                @Override
                protected void updateItem(ParamItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0) {
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
            commonParamTable.getStylesheets().add(getClass().getResource("/css/pane_restful.css").toExternalForm());
        }
        // 设置表头为中文
        if (reqParamRemarkColumn != null) reqParamRemarkColumn.setText("中文描述");
        if (respParamRemarkColumn != null) respParamRemarkColumn.setText("中文描述");
        if (mainTabPane != null) {
            Platform.runLater(() -> {
                if (mainTabPane.getTabs().size() > 1) {
                    mainTabPane.getSelectionModel().select(1);
                }
            });
        }
        // 新增：监听caseListView选中事件，自动填充请求参数表单和公共参数表单
        caseListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            int index = newIdx.intValue();
            if (debugLogCache != null && index >= 0 && index < debugLogCache.size()) {
                DebugLogPO log = debugLogCache.get(index);
                String reqJson = log.getReqParam();
                if (reqJson != null && !reqJson.isEmpty()) {
                    // 先设置请求参数选项卡
                    importJsonToRequestTable(reqJson);
                    // 再设置公共参数选项卡
                    importJsonToCommonParamTable(reqJson);
                }
            }
        });
        // 设置导出http按钮样式
        if (exportHttpButton != null) {
            exportHttpButton.setStyle(""); // 恢复为默认样式
        }
        // 设置详情按钮为蓝底白字粗体
        if (detailButton != null) {
            detailButton.setStyle("-fx-background-color: #199cd7; -fx-text-fill: white; -fx-font-weight: bold;");
        }
        // 设置日志按钮为红底白字粗体
        if (logButton != null) {
            logButton.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-font-weight: bold;");
        }
        // 查询按钮点击时重新初始化界面
        if (caseQueryButton != null) {
            caseQueryButton.setOnAction(e -> onQueryDebugLog());
        }
        // 监听ipComboBox值变化，自动查询日志
        if (ipComboBox != null) {
            ipComboBox.valueProperty().addListener((obs, oldVal, newVal) -> onQueryDebugLog());
        }
    }

    private void initServerIpComboBox() {
        String projectName = globalProperties.getProjectName();
        ServerInfoPO query = new ServerInfoPO();
        query.setProjectName(projectName);
        List<ServerInfoPO> serverList = serverInfoRpcService.queryForList(query);
        ObservableList<String> ipList = FXCollections.observableArrayList();
        ipList.add("请选择环境地址");
        ipList.add("http://127.0.0.1:"+globalProperties.getAppPort());
        String selectedIp = null;
        for (ServerInfoPO server : serverList) {
            String port = (server.getAppPort() != null && !server.getAppPort().isEmpty())
                ? server.getAppPort()
                : (server.getPort() != null ? server.getPort() : "");
            String display = "http://" + server.getIp() + (port.isEmpty() ? "" : ":" + port);
            ipList.add(display);
        }
        ipComboBox.setItems(ipList);
        ipComboBox.setValue("请选择环境地址"); // 默认选中
    }

    public void setUrl(String url) {
        if (urlField != null) {
            urlField.setText(url);
        }
    }

    /**
     * 扁平字段转树结构（参考PaneApiController）
     */
    private List<RxField> buildFieldTree(List<RxField> flatRxFields) {
        Map<String, RxField> nodeMap = new java.util.HashMap<>();
        List<RxField> roots = new java.util.ArrayList<>();
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
                    node.setChildren(new java.util.ArrayList<>());
                    nodeMap.put(key, node);
                    if (parent != null) {
                        if (parent.getChildren() == null) parent.setChildren(new java.util.ArrayList<>());
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

    /**
     * 递归构建参数树
     */
    private TreeItem<ParamItem> buildParamTree(List<RxField> rxFields) {
        TreeItem<ParamItem> root = new TreeItem<>(new ParamItem("", "", "", "", ""));
        if (rxFields != null) {
            for (RxField f : rxFields) {
                ParamItem item = new ParamItem(f.getCommentCn(), f.getNameCamel(), "", f.getType(), f.getLength() == null ? null : String.valueOf(f.getLength()));
                // 如果是List或object，递归children
                if (("List".equalsIgnoreCase(f.getType()) || "object".equalsIgnoreCase(f.getType())) && f.getChildren() != null && !f.getChildren().isEmpty()) {
                    TreeItem<ParamItem> node = new TreeItem<>(item);
                    node.getChildren().addAll(buildParamTree(f.getChildren()).getChildren());
                    root.getChildren().add(node);
                } else {
                    root.getChildren().add(new TreeItem<>(item));
                }
            }
        }
        return root;
    }

    /**
     * 设置交易名称和中文描述，并加载参数表格内容
     */
    public void setTransInfo(String transName, String transCommentZh) {
        if (transNameField != null) {
            transNameField.setText(transName);
        }
        if (transCommentZhLabel != null) {
            transCommentZhLabel.setText(transCommentZh);
        }
        // 新增：加载参数表格内容
        loadApiDetail(transName);
    }

    @FXML
    private void onSend() {
        String ipPort = ipComboBox.getValue();
        String urlPath = urlField.getText();
        String baseUrl = (ipPort != null && ipPort.startsWith("http")) ? ipPort : (ipPort != null ? "http://" + ipPort : "");
        String fullUrl = baseUrl + ((urlPath != null && urlPath.startsWith("/")) ? urlPath : (urlPath != null ? "/" + urlPath : ""));

        String reqBodyJson;
        if (requestTable != null && requestTable.getRoot() != null) {
            Object reqObj = treeToJson(requestTable.getRoot());
            reqBodyJson = JSON.toJSONString(reqObj, true);
        }else {
            reqBodyJson = "{}";
        }

        String commonParamJson;
        if (commonParamTable != null && commonParamTable.getRoot() != null) {
            Object commonObj = treeToJson(commonParamTable.getRoot());
            commonParamJson = JSON.toJSONString(commonObj, true);
        }else {
            commonParamJson = "{}";
        }

        final String json = restFulConnService
                .setGdaForReqJson(reqBodyJson, transNameField.getText(),commonParamJson);

        if (ipPort == null || ipPort.isEmpty() || "请选择环境地址".equals(ipPort)) {
            showAlert("请选择服务器地址");
            return;
        }
        if (urlPath == null || urlPath.isEmpty()) {
            showAlert("请输入接口URL");
            return;
        }

        // 创建进度对话框
        Dialog<Void> progressDialog = new Dialog<>();
        progressDialog.setTitle("发送请求中");
        progressDialog.setHeaderText("正在发送请求...");
        progressDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        
        // 创建进度条
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(400);
        progressBar.setProgress(-1); // 不确定进度
        
        // 创建信息标签
        Label infoLabel = new Label();
        infoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // 创建详细URL标签
        Label urlLabel = new Label();
        urlLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        urlLabel.setWrapText(true);
        
        VBox content = new VBox(10, infoLabel, urlLabel, progressBar);
        content.setStyle("-fx-padding: 20;");
        progressDialog.getDialogPane().setContent(content);
        
        // 设置对话框样式
        progressDialog.getDialogPane().setStyle("-fx-background-color: white;");
        
        // 更新信息显示
        final String displayIp;
        if (ipPort != null && ipPort.startsWith("http://")) {
            displayIp = ipPort.substring(7);
        } else if (ipPort != null && ipPort.startsWith("https://")) {
            displayIp = ipPort.substring(8);
        } else {
            displayIp = ipPort;
        }
        infoLabel.setText("目标服务器: " + displayIp);
        urlLabel.setText("请求地址: " + fullUrl);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 更新进度信息
                Platform.runLater(() -> {
                    infoLabel.setText("正在连接服务器: " + displayIp);
                });
                
                System.out.println("url => " + fullUrl);
                URL url = new URL(fullUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("ECP_BUS_KEY", "{\"tenant_id\":\"KEN\", \"ccy\":\"KES\",\"tenant_ccy\":\"KES\", \"timezone\":\"GMT+08:00\", \"name\":\"Tanzania\"}");
                for (ParamItem item : headerData) {
                    if (item.getName() != null && !item.getName().isEmpty()) {
                        conn.setRequestProperty(item.getName(), item.getValue());
                    }
                }
                conn.setDoOutput(true);
                
                // 更新进度信息
                Platform.runLater(() -> {
                    infoLabel.setText("正在发送数据到: " + displayIp);
                });
                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                
                // 更新进度信息
                Platform.runLater(() -> {
                    infoLabel.setText("正在等待响应: " + displayIp);
                });
                
                int responseCode = conn.getResponseCode();
                String resp;
                try (InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream()) {
                    String str_resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    resp=JsonFormatUtil.toHumpJson(str_resp);
                }
                
                if (responseCode == 200) {
                    // 更新进度信息
                    Platform.runLater(() -> {
                        infoLabel.setText("请求成功，正在处理响应数据...");
                    });
                    
                    DebugLogPO log = new DebugLogPO();
                    log.setGroupName("");
                    String ipValue = ipComboBox.getValue();
                    String ip = ipValue;
                    if (ipValue != null && ipValue.startsWith("http")) {
                        ip = ipValue.replaceFirst("https?://", "");
                        int idx = ip.indexOf(":");
                        if (idx > 0) {
                            ip = ip.substring(0, idx);
                        }
                    }
                    log.setIp(ip);
                    log.setProjectName(globalProperties.getProjectName());
                    log.setAppName(globalProperties.getAppName());
                    log.setInterfaceName(urlField.getText());
                    log.setTransName(transNameField != null ? transNameField.getText() : "");
                    log.setMethod("POST");
                    log.setUrl(fullUrl);
                    log.setReqParam(json);
                    log.setRspParam(resp);
                    log.setSequence(java.util.UUID.randomUUID().toString());
                    log.setUpdateBy(globalProperties.getOperator().getUsername());
                    log.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    log.setGroupName(globalProperties.getGroupName());
                    try {
                        JSONObject respJson = JSON.parseObject(resp);
                        JSONObject gda = respJson.getJSONObject("gda");
                        if (gda != null) {
                            String reqBusNo = gda.getString("reqBusNo");
                            if(StringUtils.isNotBlank(reqBusNo)) {
                                log.setJrnNo(reqBusNo);
                            }else {
                                log.setJrnNo(" ");
                            }
                            log.setRequestId(gda.getString("reqId"));
                            log.setMsgCode(gda.getString("msgCd"));
                            log.setMsgInf(gda.getString("msgInf"));
                        }else {
                            log.setJrnNo(" ");
                            log.setRequestId(" ");
                            log.setMsgCode("SCM60001");
                            log.setMsgInf("系统错误");
                        }
                    } catch (Exception ignore) {
                        ignore.printStackTrace();
                    }
                    debugLogRpcService.save(log);
                    
                    // 关闭进度对话框
                    Platform.runLater(() -> {
                        progressDialog.close();
                        onQueryDebugLog();
                    });

                    // 新增：自动填充响应参数表单
                    Platform.runLater(() -> {
                        try {
                            Map<String, Object> respMap = JSON.parseObject(resp, Map.class);
                            if (responseTable != null && responseTable.getRoot() != null) {
                                for (TreeItem<ParamItem> child : responseTable.getRoot().getChildren()) {
                                    String name = child.getValue().getName();
                                    Object value = null;
                                    if (respMap.containsKey(name)) {
                                        value = respMap.get(name);
                                    } else {
                                        for (Object key : respMap.keySet()) {
                                            if (key instanceof String && ((String) key).equalsIgnoreCase(name)) {
                                                value = respMap.get(key);
                                                break;
                                            }
                                        }
                                    }
                                    fillTreeWithJson(child, value);
                                }
                                responseTable.refresh();
                                // 自动切换到"响应参数"选项卡
                                if (mainTabPane != null) {
                                    for (int i = 0; i < mainTabPane.getTabs().size(); i++) {
                                        Tab tab = mainTabPane.getTabs().get(i);
                                        if ("响应参数".equals(tab.getText())) {
                                            mainTabPane.getSelectionModel().select(i);
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showAlert("响应JSON解析失败: " + ex.getMessage());
                        }
                    });
                }else {
                    // 关闭进度对话框并显示错误
                    Platform.runLater(() -> {
                        progressDialog.close();
                        ViewUtils.alertForFail("通信失败:"+responseCode);
                    });
                }
                return null;
            }
        };
        
        // 处理取消按钮
        progressDialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.CANCEL) {
                task.cancel();
            }
            return null;
        });
        
        // 启动任务
        new Thread(task).start();
        
        // 显示进度对话框
        progressDialog.showAndWait();
    }

    /**
     * 递归填充TreeTableView参数树的值，支持object、list等复杂结构
     */
    private void fillTreeWithJson(TreeItem<ParamItem> node, Object jsonValue) {
        ParamItem param = node.getValue();
        if (param == null) return;
        if ("List".equalsIgnoreCase(param.getType())) {
            if (jsonValue instanceof List) {
                List<?> jsonList = (List<?>) jsonValue;
                if (node.getChildren().isEmpty()) return;
                boolean isObjectTemplate = "Object".equalsIgnoreCase(node.getChildren().get(0).getValue().getType());
                if (isObjectTemplate) {
                    // 先保存模板
                    TreeItem<ParamItem> objectTemplate = cloneTree(node.getChildren().get(0));
                    node.getChildren().clear();
                    for (int i = 0; i < jsonList.size(); i++) {
                        Object item = jsonList.get(i);
                        TreeItem<ParamItem> objectNode = cloneTree(objectTemplate);
                        // 给Object节点加上序号
                        objectNode.getValue().setName("Object[" + (i + 1) + "]");
                        fillTreeWithJson(objectNode, item);
                        node.getChildren().add(objectNode);
                    }
                } else {
                    // 字段节点模板，需为每组数据clone一组字段节点，并包裹在Object分组节点下
                    List<TreeItem<ParamItem>> fieldTemplates = new java.util.ArrayList<>(node.getChildren());
                    node.getChildren().clear();
                    for (int i = 0; i < jsonList.size(); i++) {
                        Object item = jsonList.get(i);
                        java.util.Map<?, ?> itemMap = (item instanceof java.util.Map) ? (java.util.Map<?, ?>) item : null;
                        // 新建Object分组节点
                        ParamItem groupParam = new ParamItem("分组", "Object[" + (i + 1) + "]", null, "Object", null);
                        TreeItem<ParamItem> groupNode = new TreeItem<>(groupParam);
                        for (TreeItem<ParamItem> fieldTemplate : fieldTemplates) {
                            TreeItem<ParamItem> fieldNode = cloneTree(fieldTemplate);
                            String fieldName = fieldNode.getValue().getName();
                            Object fieldValue = null;
                            if (itemMap != null) {
                                if (itemMap.containsKey(fieldName)) {
                                    fieldValue = itemMap.get(fieldName);
                                } else {
                                    for (Object key : itemMap.keySet()) {
                                        if (key instanceof String && ((String) key).equalsIgnoreCase(fieldName)) {
                                            fieldValue = itemMap.get(key);
                                            break;
                                        }
                                    }
                                }
                            }
                            // System.out.println("[List字段递归] 第" + i + "组 字段: " + fieldName + ", value: " + fieldValue);
                            fillTreeWithJson(fieldNode, fieldValue);
                            groupNode.getChildren().add(fieldNode);
                        }
                        node.getChildren().add(groupNode);
                    }
                }
            }
            return;
        }
        if ("Object".equalsIgnoreCase(param.getType()) || jsonValue instanceof java.util.Map) {
            if (jsonValue instanceof java.util.Map) {
                java.util.Map<?, ?> jsonMap = (java.util.Map<?, ?>) jsonValue;
                for (TreeItem<ParamItem> child : node.getChildren()) {
                    String childName = child.getValue().getName();
                    Object childValue = null;
                    if (jsonMap.containsKey(childName)) {
                        childValue = jsonMap.get(childName);
                    } else {
                        for (Object key : jsonMap.keySet()) {
                            if (key instanceof String && ((String) key).equalsIgnoreCase(childName)) {
                                childValue = jsonMap.get(key);
                                break;
                            }
                        }
                    }
                    // System.out.println("[填充参数] 当前节点: " + childName + "，可选key: " + jsonMap.keySet() + ", 匹配到: " + (childValue != null));
                    fillTreeWithJson(child, childValue);
                }
            }
            return;
        }
        // 普通字段直接赋值
        if (jsonValue != null) {
            param.setValue(jsonValue.toString());
        }
    }

    // 深度clone参数树，完整复制所有属性
    private TreeItem<ParamItem> cloneTree(TreeItem<ParamItem> src) {
        ParamItem srcParam = src.getValue();
        ParamItem newParam = new ParamItem(
            srcParam.getComment(), // comment
            srcParam.getName(),    // name
            null,                  // value，clone时不带value，递归填充时再赋值
            srcParam.getType(),    // type
            srcParam.getLength()   // length
        );
        TreeItem<ParamItem> newItem = new TreeItem<>(newParam);
        for (TreeItem<ParamItem> child : src.getChildren()) {
            newItem.getChildren().add(cloneTree(child));
        }
        return newItem;
    }

    private void showAlert(String msg) {
        Platform.runLater(() -> {
            ViewUtils.alertForSucess(msg);
        });
    }

    public void onQueryDebugLog() {
        String selectedIp = ipComboBox.getValue();
        String url = urlField.getText();
        List<DebugLogPO> logs;
        if (selectedIp != null && !"请选择环境地址".equals(selectedIp)) {
            String baseUrl = selectedIp.startsWith("http") ? selectedIp : "http://" + selectedIp;
            String fullUrl = baseUrl + (url != null && !url.isEmpty() ? (url.startsWith("/") ? url : "/" + url) : "");
            DebugLogPO query = new DebugLogPO();
            query.setUrl(fullUrl);
            logs = debugLogRpcService.queryForList(query);
        } else {
            DebugLogPO debugLogPO = new DebugLogPO();
            if(StringUtils.isNotBlank(url)) {
                debugLogPO.setInterfaceName(url);
            }
            logs = debugLogRpcService.queryForList(debugLogPO);
        }
        debugLogCache = logs;
        ObservableList<String> items = FXCollections.observableArrayList();
        for (DebugLogPO log : logs) {
            String display = String.format("[%s] - %s   %s", MrDateUtils.toShortTime(log.getUpdateTime()), log.getMsgCode(),log.getMsgInf());
            items.add(display);
        }
        caseListView.setItems(items);
    }

    // 弹出详情窗口
    private void showDebugLogDetailDialog() {
        int index = caseListView.getSelectionModel().getSelectedIndex();
        if (debugLogCache == null || index < 0 || index >= debugLogCache.size()) {
            showAlert("请先选择一条日志记录");
            return;
        }
        DebugLogPO log = debugLogCache.get(index);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("调试日志详情");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        // 顶部信息
        Label title = new Label("交易名: " + log.getTransName());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label jrnNo = new Label("流水号: " + log.getJrnNo());
        jrnNo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(18, title, jrnNo, spacer);
        topRow.setStyle("-fx-padding: 0 0 0 0;");
        Label reqId = new Label("RequestId: " + log.getRequestId());
        reqId.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox topBar = new VBox(topRow, reqId);
        topBar.setStyle("-fx-padding: 12 12 12 12; -fx-background-color: #f5f5f5;");
        // 左右分栏
        SplitPane splitPane = new SplitPane();
        splitPane.setPrefWidth(900);
        splitPane.setPrefHeight(500);
        // 格式化请求和响应参数
        String reqText = log.getReqParam();
        String rspText = log.getRspParam();
        try {
            Object reqObj = JSON.parse(reqText);
            reqText = JSON.toJSONString(reqObj, true);
        } catch (Exception ignore) {}
        try {
            Object rspObj = JSON.parse(rspText);
            rspText = JSON.toJSONString(rspObj, true);
        } catch (Exception ignore) {}
        TextArea reqArea = new TextArea(reqText);
        reqArea.setEditable(false);
        reqArea.setWrapText(true);
        reqArea.setPromptText("请求参数");
        reqArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        VBox.setVgrow(reqArea, Priority.ALWAYS);
        TextArea rspArea = new TextArea(rspText);
        rspArea.setEditable(false);
        rspArea.setWrapText(true);
        rspArea.setPromptText("响应参数");
        rspArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        VBox.setVgrow(rspArea, Priority.ALWAYS);
        VBox left = new VBox(new Label("请求参数"), reqArea);
        VBox right = new VBox(new Label("响应参数"), rspArea);
        left.setSpacing(6); right.setSpacing(6);
        left.setStyle("-fx-padding: 8;"); right.setStyle("-fx-padding: 8;");
        VBox.setVgrow(left, Priority.ALWAYS);
        VBox.setVgrow(right, Priority.ALWAYS);
        splitPane.getItems().addAll(left, right);
        splitPane.setDividerPositions(0.5);
        VBox content = new VBox(topBar, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // 日志按钮下载日志文件
    private void onDownloadLogFile() {
        int index = caseListView.getSelectionModel().getSelectedIndex();
        if (debugLogCache == null || index < 0 || index >= debugLogCache.size()) {
            showAlert("请先选择一条日志记录");
            return;
        }
        DebugLogPO log = debugLogCache.get(index);
        String ip = log.getIp();
        String requestId = log.getRequestId();
        if (StringUtils.isBlank(ip) || StringUtils.isBlank(requestId)) {
            showAlert("日志记录缺少IP或request_id，无法下载日志文件");
            return;
        }
        String fileName = requestId + ".trc";
        if(StringUtils.equals(ip,"127.0.0.1")){
            String path = globalProperties.getPropPath();
            int idx = path.indexOf("/src/main");
            if (idx > 0) {
                path = path.substring(0, idx);
            }
            String filePath =
            globalProperties.getBasePath()+File.separator+path+File.separator+
                    "target/trc/"+ MrDateUtils.getCurrentDay();
            String fullFilePath = filePath + File.separator + fileName;
            File logFile = new File(fullFilePath);
            if (logFile.exists()) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(logFile.toPath()), "UTF-8");
                    LogDialogUtil.showLogDialog(content, "日志文件: " + fileName, 1200, 800);
                } catch (Exception e) {
                    showAlert("读取日志文件失败：" + fullFilePath + "\n错误: " + e.getMessage());
                }
            } else {
                showAlert("日志不存在：" + fullFilePath);
            }
        }else {
            // 选择保存目录
            DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
            chooser.setTitle("选择日志保存目录");
            File dir = chooser.showDialog(logButton.getScene().getWindow());
            if (dir == null) return;
            String localPath = dir.getAbsolutePath() + File.separator + fileName;
            // 检查并下载
            try {
                String downloaded = sshDomainService.ftpGetLogTrc(ip, fileName, localPath);
                File logFile = new File(localPath);
                if (logFile.exists()) {
                    try {
                        String content = new String(java.nio.file.Files.readAllBytes(logFile.toPath()), "UTF-8");
                        LogDialogUtil.showLogDialog(content, "日志文件: " + fileName, 1200, 800);
                    } catch (Exception e) {
                        showAlert("读取日志文件失败：" + localPath + "\n错误: " + e.getMessage());
                    }
                } else {
                    showAlert("日志文件已下载到：" + downloaded + "\n但文件不存在：" + localPath);
                }
            } catch (Exception ex) {
                String remotePath = "[服务器:" + ip + "] [文件:" + fileName + "]";
                showAlert("日志文件下载失败！路径：" + remotePath + "\n错误: " + ex.getMessage());
            }
        }
    }

    // ========== 新增：加载接口详情，设置参数表格内容 ==========
    public void loadApiDetail(String transName) {
        if (StringUtils.isBlank(transName)) return;
        // 查询接口详情
        InterFaceEntity entity = interFaceEntityService.getByTransName(transName,null);
        if (entity == null) return;
        // 请求参数
        java.util.List<RxField> reqTree = buildFieldTree(entity.getRequest());
        TreeItem<ParamItem> reqRoot = buildParamTree(reqTree);
        if (requestTable != null) {
            requestTable.setRoot(reqRoot);
            requestTable.setShowRoot(false);
        }
        // 公共参数
        if(!StringUtils.equals(entity.getReqParentClass(),"AbstractBaseGDA")){
            List<RxField> commonFields = getCommonFields(entity.getReqParentClass());
            List<RxField> commonTree = buildFieldTree(commonFields);
            TreeItem<ParamItem> commonRoot = buildParamTree(commonTree);
            if (commonParamTable != null) {
                commonParamTable.setRoot(commonRoot);
                commonParamTable.setShowRoot(false);
            }
        }

        // 响应参数
        CommonClassPO commonClass = commonClassRpcService.queryByClassName("AbstractBaseGDA");
        List<RxField> rspRxField = InterfaceConvert.jsonToFields(commonClass.getFieldsJson());
        rspRxField.addAll(entity.getResponse());
        List<RxField> respTree = buildFieldTree(rspRxField);
        TreeItem<ParamItem> respRoot = buildParamTree(respTree);
        if (responseTable != null) {
            responseTable.setRoot(respRoot);
            responseTable.setShowRoot(false);
        }
    }

    /**
     * 递归将参数树转为Map/List结构
     */
    private Object treeToJson(TreeItem<ParamItem> node) {
        ParamItem param = node.getValue();
        if ("List".equalsIgnoreCase(param.getType())) {
            List<Object> list = new ArrayList<>();
            for (TreeItem<ParamItem> child : node.getChildren()) {
                // List下每个Object分组节点
                list.add(treeToJson(child));
            }
            return list;
        } else if ("Object".equalsIgnoreCase(param.getType()) || !node.getChildren().isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (TreeItem<ParamItem> child : node.getChildren()) {
                String key = child.getValue().getName();
                Object value = treeToJson(child);
                map.put(key, value);
            }
            return map;
        } else {
            // 普通字段
            return param.getValue();
        }
    }

    /**
     * 供外部调用：将json字符串填充到请求参数表单
     */
    public void importJsonToRequestTable(String jsonStr) {
        try {
            // 先将下划线key转为驼峰
            String humpJson = JsonFormatUtil.toHumpJson(jsonStr);
            Map<String, Object> jsonMap = JSON.parseObject(humpJson, Map.class);
            if (requestTable != null && requestTable.getRoot() != null) {
                for (TreeItem<ParamItem> child : requestTable.getRoot().getChildren()) {
                    String name = child.getValue().getName();
                    Object value = null;
                    if (jsonMap.containsKey(name)) {
                        value = jsonMap.get(name);
                    } else {
                        for (Object key : jsonMap.keySet()) {
                            if (key instanceof String && ((String) key).equalsIgnoreCase(name)) {
                                value = jsonMap.get(key);
                                break;
                            }
                        }
                    }
                    fillTreeWithJson(child, value);
                }
                requestTable.refresh();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("JSON解析失败: " + ex.getMessage());
        }
    }

    /**
     * 供外部调用：将json字符串填充到公共参数表单
     */
    public void importJsonToCommonParamTable(String humpJson) {
        try {
            // 先将下划线key转为驼峰
            Map<String, Object> jsonMap = JSON.parseObject(humpJson, Map.class);
            if (commonParamTable != null && commonParamTable.getRoot() != null) {
                for (TreeItem<ParamItem> child : commonParamTable.getRoot().getChildren()) {
                    String name = child.getValue().getName();
                    Object value = null;
                    if (jsonMap.containsKey(name)) {
                        value = jsonMap.get(name);
                    } else {
                        for (Object key : jsonMap.keySet()) {
                            if (key instanceof String && ((String) key).equalsIgnoreCase(name)) {
                                value = jsonMap.get(key);
                                break;
                            }
                        }
                    }
                    fillTreeWithJson(child, value);
                }
                commonParamTable.refresh();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("JSON解析失败: " + ex.getMessage());
        }
    }

    // 新增：导出http按钮事件
    @FXML
    public void onExportHttp() {
        String method = methodComboBox != null ? methodComboBox.getValue() : "POST";
        // 优先使用选中日志记录的url
        String fullUrl = null;
        int selectedIdx = caseListView != null ? caseListView.getSelectionModel().getSelectedIndex() : -1;
        if (debugLogCache != null && selectedIdx >= 0 && selectedIdx < debugLogCache.size()) {
            DebugLogPO log = debugLogCache.get(selectedIdx);
            fullUrl = log.getUrl();
        }
        if (fullUrl == null || fullUrl.isEmpty()) {
            String ipPort = ipComboBox != null ? ipComboBox.getValue() : "http://localhost:8381";
            String urlPath = urlField != null ? urlField.getText() : "/";
            String baseUrl = (ipPort != null && ipPort.startsWith("http")) ? ipPort : (ipPort != null ? "http://" + ipPort : "");
            fullUrl = baseUrl + ((urlPath != null && urlPath.startsWith("/")) ? urlPath : (urlPath != null ? "/" + urlPath : ""));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(fullUrl).append("\n");
        sb.append("Content-Type: application/json\n");
        sb.append("ECP_BUS_KEY: {\"tenant_id\":\"KEN\", \"ccy\":\"KES\",\"tenant_ccy\":\"KES\", \"timezone\":\"GMT+08:00\", \"name\":\"Tanzania\"}\n");
        if (headerData != null) {
            for (ParamItem item : headerData) {
                if (item.getName() != null && !item.getName().isEmpty()) {
                    sb.append(item.getName()).append(": ").append(item.getValue()).append("\n");
                }
            }
        }
        sb.append("\n");
        String json;
        if (requestTable != null && requestTable.getRoot() != null) {
            Object reqObj = treeToJson(requestTable.getRoot());
            json = JSON.toJSONString(reqObj, true);
        } else {
            json = "{}";
        }
        sb.append(json);
        // 弹出保存文件对话框
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("保存http文件");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("HTTP文件", "*.http"));
        fileChooser.setInitialFileName("request.http");
        java.io.File file = fileChooser.showSaveDialog(exportHttpButton.getScene().getWindow());
        if (file != null) {
            try (java.io.FileWriter writer = new java.io.FileWriter(file, false)) {
                writer.write(sb.toString());
                showAlert("导出成功: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert("导出失败: " + ex.getMessage());
            }
        }
    }

    // 获取公共参数字段（可根据实际情况调整）
    private List<RxField> getCommonFields(String classPath) {
        List<RxField> list = new ArrayList<>();
        CommonClassPO commonClassPO = new CommonClassPO();
        commonClassPO.setClassPath(classPath);
        commonClassPO.setClassType("PCLS");
        CommonClassPO commonClassRspPO = commonClassRpcService.queryOne(commonClassPO);
        return DatabaseConvert.jsonToFields(commonClassRspPO.getFieldsJson());
    }
}
