package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.ExeStatusEnum;
import com.murong.ecp.tools.fx.enums.MenuEnum;
import com.murong.ecp.tools.fx.infrastructure.rpc.ExeSqlRecordRpcService;
import com.murong.ecp.tools.fx.infrastructure.rpc.UserProjSettingRpcService;
import com.murong.ecp.tools.fx.domain.service.database.ExecuteSqlService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.ExeSqlRecordPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserProjSettingPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.ExeSqlRecordQuery;
import com.murong.ecp.tools.fx.infrastructure.view.LogDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.view.ReminderDialogUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.view.SqlDialogUtil;
import org.springframework.context.ApplicationContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class PaneExeRecordController {
    @Autowired
    private ExeSqlRecordRpcService exeSqlRecordRpcService;
    @Autowired
    private UserProjSettingRpcService UserProjSettingRpcService;

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button queryButton;
    @FXML private TableView<ExeSqlRecordPO> tableView;
    @FXML private TableColumn<ExeSqlRecordPO, Boolean> checkCol;
    @FXML private TableColumn<ExeSqlRecordPO, String> schemaNmColumn;
    @FXML private TableColumn<ExeSqlRecordPO, String> envNameColumn;
    @FXML private TableColumn<ExeSqlRecordPO, String> exeDateColumn;
    @FXML private TableColumn<ExeSqlRecordPO, String> exeTimeColumn;
    @FXML private TableColumn<ExeSqlRecordPO, String> exeSqlColumn;
    @FXML private TableColumn<ExeSqlRecordPO, String> updateByColumn;
    @FXML private TableColumn<ExeSqlRecordPO, Void> opCol;
    @FXML private TableColumn<ExeSqlRecordPO, String> statusColumn;
    @FXML private ComboBox<String> envComboBox;
    @FXML private ComboBox<String> schemaComboBox;
    @FXML private TextField keywordTextField;

    private ObservableList<ExeSqlRecordPO> dataList = FXCollections.observableArrayList();
    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ExecuteSqlService executeSqlService;

    @FXML
    public void initialize() {
        // 检查项目配置
        if (!ViewUtils.validateProjectConfiguration(globalPropes)) {
            return; // 配置不完整，直接返回，不初始化界面
        }
        
        // 默认查询三天内的记录（含当天）
        startDatePicker.setValue(LocalDate.now().minusDays(2));
        endDatePicker.setValue(LocalDate.now());
        // 初始化环境下拉框
        envComboBox.getItems().addAll("全部","dev", "sit", "uat", "poc");
        envComboBox.getSelectionModel().selectFirst();
        // 初始化schema下拉框
        schemaComboBox.getItems().add("全部");
        List<UserProjSettingPO> projectParams = UserProjSettingRpcService.queryForList(new UserProjSettingPO());
        projectParams.stream().map(UserProjSettingPO::getSchemaNm).distinct().forEach(schema -> {
            if (schema != null && !schema.isEmpty()) schemaComboBox.getItems().add(schema);
        });
        if (!schemaComboBox.getItems().isEmpty()) schemaComboBox.getSelectionModel().selectFirst();
        // 复选框列
        checkCol.setCellFactory(col -> {
            CheckBoxTableCell<ExeSqlRecordPO, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        // 其它字段列
        schemaNmColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSchemaNm()));
        // SCHEMA列字体居中
        schemaNmColumn.setCellFactory(col -> {
            return new TableCell<ExeSqlRecordPO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-alignment: CENTER;");
                }
            };
        });
        envNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEnvName()));
        // 环境列字体居中
        envNameColumn.setCellFactory(col -> {
            return new TableCell<ExeSqlRecordPO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-alignment: CENTER;");
                }
            };
        });
        exeDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getExeDate()));
        // 执行日期列字体居中
        exeDateColumn.setCellFactory(col -> {
            return new TableCell<ExeSqlRecordPO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-alignment: CENTER;");
                }
            };
        });
        exeTimeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getExeTime()));
        // 执行时间列字体居中
        exeTimeColumn.setCellFactory(col -> {
            return new TableCell<ExeSqlRecordPO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-alignment: CENTER;");
                }
            };
        });
        exeSqlColumn.setCellValueFactory(cell -> {
            String sql = cell.getValue().getExeSql();
            // 在表格中显示时，将SQL截断并添加省略号，保持可读性
            if (sql != null && sql.length() > 50) {
                sql = sql.substring(0, 50) + "...";
            }
            return new SimpleStringProperty(sql);
        });
        // 执行SQL列双击事件
        exeSqlColumn.setCellFactory(col -> new TableCell<ExeSqlRecordPO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER_LEFT;");
                
                // 为每个单元格添加双击事件监听器
                if (!empty) {
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            ExeSqlRecordPO po = getTableView().getItems().get(getIndex());
                            if (po != null) {
                                // 显示完整的SQL语句，保持换行格式
                                SqlDialogUtil.showLogDialog(po.getExeSql(), "SQL详情", 900, 600);
                            }
                        }
                    });
                }
            }
        });
        // 执行状态列
        statusColumn.setCellValueFactory(cell -> {
            String status = "";
            try {
                // 优先getStatus方法
                java.lang.reflect.Method m = cell.getValue().getClass().getMethod("getStatus");
                Object v = m.invoke(cell.getValue());
                status = v == null ? "" : v.toString();
            } catch (Exception e) {
                // 若无getStatus则尝试getCompletedFlg
                try {
                    java.lang.reflect.Method m2 = cell.getValue().getClass().getMethod("getCompletedFlg");
                    Object v2 = m2.invoke(cell.getValue());
                    status = v2 == null ? "" : v2.toString();
                } catch (Exception ignore) {}
            }
            
            // 根据StatusEnum进行映射
            if (status != null && !status.isEmpty()) {
                for (ExeStatusEnum exeStatusEnum : ExeStatusEnum.values()) {
                    if (exeStatusEnum.getKey().equals(status)) {
                        status = exeStatusEnum.getValue();
                        break;
                    }
                }
            }
            
            return new SimpleStringProperty(status);
        });
        // 执行状态列字体居中
        statusColumn.setCellFactory(col -> {
            return new TableCell<ExeSqlRecordPO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-alignment: CENTER;");
                }
            };
        });
        updateByColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUpdateBy()));
        // 更新人列字体居中
        updateByColumn.setCellFactory(col -> {
            return new TableCell<ExeSqlRecordPO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-alignment: CENTER;");
                }
            };
        });
        // 操作列
        opCol.setCellFactory(col -> new TableCell<>() {
            private final HBox buttonBox = new HBox(4);
            private final Button executeBtn = new Button();
            private final Button deleteBtn = new Button();
            
            {
                // 设置按钮容器居中
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            }
            
            {
                // 设置执行按钮样式 - 使用与testBtn相同的样式
                javafx.scene.shape.SVGPath executeIcon = new javafx.scene.shape.SVGPath();
                executeIcon.setContent("M8 5v14l11-7z");
                executeIcon.setStyle("-fx-stroke: #27ae60; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                executeIcon.setScaleX(0.85); executeIcon.setScaleY(0.85);
                executeBtn.setGraphic(executeIcon);
                executeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                executeBtn.setTooltip(new Tooltip("执行SQL"));
                executeBtn.setMinWidth(28);
                executeBtn.setPrefWidth(28);
                executeBtn.setMaxWidth(28);
                executeBtn.setMinHeight(28);
                executeBtn.setPrefHeight(28);
                executeBtn.setMaxHeight(28);
                // 添加鼠标悬停效果
                executeBtn.setOnMouseEntered(e -> executeBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                executeBtn.setOnMouseExited(e -> executeBtn.setStyle("-fx-background-color: transparent;"));
                executeBtn.setOnMousePressed(e -> executeBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                executeBtn.setOnMouseReleased(e -> executeBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                
                // 设置删除按钮样式 - 使用与PaneTableController相同的SVG图标样式
                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14zM10 11v6M14 11v6");
                trashIcon.setStyle("-fx-stroke: #222; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                trashIcon.setScaleX(0.7);
                trashIcon.setScaleY(0.7);
                deleteBtn.setGraphic(trashIcon);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                deleteBtn.setTooltip(new Tooltip("删除"));
                deleteBtn.setMinWidth(28);
                deleteBtn.setPrefWidth(28);
                deleteBtn.setMaxWidth(28);
                deleteBtn.setMinHeight(28);
                deleteBtn.setPrefHeight(28);
                deleteBtn.setMaxHeight(28);
                // 添加鼠标悬停效果
                deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent;"));
                deleteBtn.setOnMousePressed(e -> deleteBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                deleteBtn.setOnMouseReleased(e -> deleteBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                
                // 执行按钮事件
                executeBtn.setOnAction(e -> {
                    ExeSqlRecordPO po = getTableView().getItems().get(getIndex());
                    if (po != null) {
                        // 将SQL和环境信息传递给执行SQL界面
                        loadSqlToExecutePanel(po.getExeSql(), po.getEnvName());
                    }
                });
                
                // 删除按钮事件
                deleteBtn.setOnAction(e -> {
                    ExeSqlRecordPO po = getTableView().getItems().get(getIndex());
                    if (po != null) {
                        // 使用ReminderDialogUtil显示确认删除对话框
                        ReminderDialogUtil.showConfirmDeleteDialog(() -> deleteRecord(po));
                    }
                });
                
                buttonBox.getChildren().addAll(executeBtn, deleteBtn);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
                setStyle("-fx-alignment: center;");
            }
        });
        tableView.setItems(dataList);
        // 设置表格行高为默认高度的两倍（约50像素，减少三分之一）
        tableView.setFixedCellSize(50);
        // 查询按钮事件
        queryButton.setOnAction(e -> doQuery());
        // 默认加载全部
        doQuery();
    }

    private void doQuery() {
        String start = startDatePicker.getValue() == null ? null : startDatePicker.getValue().toString();
        String end = endDatePicker.getValue() == null ? null : endDatePicker.getValue().toString();
        String env = envComboBox.getValue();
        String schema = schemaComboBox.getValue();
        String keyword = keywordTextField.getText();
        List<ExeSqlRecordPO> result = queryExeSqlRecords(start, end, env, schema, keyword);
        dataList.setAll(result);
    }

    private List<ExeSqlRecordPO> queryExeSqlRecords(String startDate, String endDate, String env, String schema, String keyword) {
        ExeSqlRecordQuery query = new ExeSqlRecordQuery();
        query.setBeginDt(startDate);
        query.setEndDt(endDate);
        query.setGroupName(globalPropes.getGroupName());
        if(!StringUtils.equals(env, "全部")) {
            query.setEnvName(env);
        }
        if(!StringUtils.equals(schema, "全部")) {
            query.setSchemaNm(schema);
        }
        if (StringUtils.isNotBlank(keyword)) {
            query.setKeyword(keyword); // 你需要在 ExeSqlRecordQuery 和 Dao 层支持关键字查询
        }
        List<ExeSqlRecordPO> exeSqlRecordLst = exeSqlRecordRpcService.queryByDateRange(query);
        exeSqlRecordLst.forEach(exeSqlRecord -> {
            String rawDate = exeSqlRecord.getExeDate();
            if (rawDate != null && rawDate.length() == 8) {
                String formattedDate = rawDate.substring(0,4) + "-" + rawDate.substring(4,6) + "-" + rawDate.substring(6,8);
                exeSqlRecord.setExeDate(formattedDate);
            }
            String rawTime = exeSqlRecord.getExeTime();
            if (rawTime != null && rawTime.length() == 6) {
                String formattedTime = rawTime.substring(0,2) + ":" + rawTime.substring(2,4) + ":" + rawTime.substring(4,6);
                exeSqlRecord.setExeTime(formattedTime);
            }
            String exeSql = exeSqlRecord.getExeSql();
            if (exeSql != null) {
                // 保持SQL语句的换行格式，只替换多余的换行符为单个换行符
                exeSqlRecord.setExeSql(exeSql.replaceAll("[\r\n]+", "\n"));
            }
        });
        return exeSqlRecordLst;
    }
    
    /**
     * 删除执行记录
     * @param po 要删除的记录
     */
    private void deleteRecord(ExeSqlRecordPO po) {
        try {
            // 调用服务删除记录
            CrResult<Object> result = executeSqlService.deleteExeSqlRecord(po);
            if (result.isSucess()) {
                ViewUtils.alertForSucess(result.getMsgInf());
                doQuery();
            } else {
                ViewUtils.alertForFail(result.getMsgInf());
            }
        } catch (Exception e) {
            ViewUtils.alertForFail("删除记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 将SQL和环境信息加载到执行SQL界面
     * @param sql SQL语句
     * @param envName 环境名称
     */
    private void loadSqlToExecutePanel(String sql, String envName) {
        try {
            // 获取主控制器
            MainController mainController = applicationContext.getBean(MainController.class);
            
            // 切换到执行SQL标签页
            mainController.showPage(MenuEnum.EXESQL.getKey(), "/fxml/pane_exesql.fxml");
            
            // 延迟执行，确保界面加载完成后再设置内容
            javafx.application.Platform.runLater(() -> {
                try {
                    // 获取执行SQL控制器
                    PaneExeSqlController exeSqlController = applicationContext.getBean(PaneExeSqlController.class);
                    
                    // 设置SQL内容
                    if (exeSqlController.getSqlTextArea() != null) {
                        exeSqlController.getSqlTextArea().setText(sql);
                    }
                    
                    // 设置环境
                    if (exeSqlController.getEnvComboBox() != null && envName != null) {
                        exeSqlController.getEnvComboBox().setValue(envName);
                    }
                } catch (Exception e) {
                    ViewUtils.alertForFail("设置SQL内容失败: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            ViewUtils.alertForFail("加载SQL到执行界面失败: " + e.getMessage());
        }
    }
} 