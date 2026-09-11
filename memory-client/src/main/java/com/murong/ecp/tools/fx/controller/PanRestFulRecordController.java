package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.infrastructure.rpc.DebugLogRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogGroupPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.DebugLogPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.DebugLogQuery;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import com.murong.ecp.tools.fx.infrastructure.utils.MrSpringContextHolder;
import com.murong.ecp.tools.fx.infrastructure.view.LogDialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class PanRestFulRecordController implements Initializable {
    @Autowired
    private DebugLogRpcService debugLogRpcService;
    @Autowired
    private GlobalProperties globalPropes;
    @Autowired
    private PanRestFulController restfulController;

    // 左侧汇总区域控件
    @FXML private Button clearButton;
    @FXML private TableView<DebugLogGroupPO> summaryTableView;
    @FXML private TableColumn<DebugLogGroupPO, String> interfaceNameColumn;
    @FXML private TableColumn<DebugLogGroupPO, String> transNameColumn;
    @FXML private TableColumn<DebugLogGroupPO, String> transDescColumn;
    @FXML private TableColumn<DebugLogGroupPO, Void> summaryTestCol;

    // 右侧查询区域控件
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> ipComboBox;
    @FXML private TextField keywordTextField;
    @FXML private Button queryButton;
    @FXML private TableView<DebugLogPO> detailTableView;
    @FXML private TableColumn<DebugLogPO, String> detailIpColumn;
    @FXML private TableColumn<DebugLogPO, String> detailUpdateTimeColumn;
    @FXML private TableColumn<DebugLogPO, String> detailMsgCodeColumn;
    @FXML private TableColumn<DebugLogPO, String> detailMsgInfColumn;
    @FXML private TableColumn<DebugLogPO, String> detailRemarkColumn;

    @FXML private Label statusLabel;
    @FXML private Label recordCountLabel;

    private ObservableList<DebugLogGroupPO> summaryList = FXCollections.observableArrayList();
    private ObservableList<DebugLogPO> detailList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeLeftSummaryArea();
        initializeRightQueryArea();
        // 设置默认日期为三天内
        setDefaultDateRange();
        loadSummaryData();
    }

    private void initializeLeftSummaryArea() {
        // 设置左侧汇总表格列
        interfaceNameColumn.setCellValueFactory(new PropertyValueFactory<>("interfaceName"));
        transNameColumn.setCellValueFactory(new PropertyValueFactory<>("transName"));
        transDescColumn.setCellValueFactory(new PropertyValueFactory<>("transNmeDsc"));

        // 启用列排序
        interfaceNameColumn.setSortable(true);
        transNameColumn.setSortable(true);
        transDescColumn.setSortable(true);

        // 测试列：只放测试按钮
        summaryTestCol.setCellFactory(col -> new TableCell<DebugLogGroupPO, Void>() {
            private final Button testBtn = new Button();
            {
                javafx.scene.shape.SVGPath testIcon = new javafx.scene.shape.SVGPath();
                testIcon.setContent("M8 5v14l11-7z");
                testIcon.setStyle("-fx-stroke: #27ae60; -fx-stroke-width: 1.5; -fx-fill: transparent;");
                testIcon.setScaleX(0.85); testIcon.setScaleY(0.85);
                testBtn.setGraphic(testIcon);
                testBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                testBtn.setTooltip(new Tooltip("接口测试"));
                testBtn.setMinWidth(28);
                testBtn.setPrefWidth(28);
                testBtn.setMaxWidth(28);
                testBtn.setMinHeight(28);
                testBtn.setPrefHeight(28);
                testBtn.setMaxHeight(28);
                // 添加鼠标悬停效果 - 参考PaneExeRecordController的执行按钮样式
                testBtn.setOnMouseEntered(e -> testBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                testBtn.setOnMouseExited(e -> testBtn.setStyle("-fx-background-color: transparent;"));
                testBtn.setOnMousePressed(e -> testBtn.setStyle("-fx-background-color: #b0b0b0; -fx-background-radius: 8;"));
                testBtn.setOnMouseReleased(e -> testBtn.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8;"));
                
                testBtn.setOnAction(e -> {
                    DebugLogGroupPO group = getTableView().getItems().get(getIndex());
                    openRestfulTest(group);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(testBtn);
                }
                setStyle("-fx-alignment: center;");
            }
        });

        summaryTableView.setItems(summaryList);
        summaryTableView.setPlaceholder(new Label("暂无汇总数据"));
        
        // 设置表格选择模式
        summaryTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 选择监听器 - 点击左侧汇总项时加载右侧详细数据
        summaryTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadDetailData(newVal);
            }
        });

        // 查询按钮
        clearButton.setOnAction(e -> {
            loadSummaryData();
        });
    }

    private void initializeRightQueryArea() {
        // 设置右侧详细表格列
        detailIpColumn.setCellValueFactory(new PropertyValueFactory<>("ip"));
        detailUpdateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("updateTime"));
        detailMsgCodeColumn.setCellValueFactory(new PropertyValueFactory<>("msgCode"));
        detailMsgInfColumn.setCellValueFactory(new PropertyValueFactory<>("msgInf"));
        detailRemarkColumn.setCellValueFactory(new PropertyValueFactory<>("debugDesc"));

        // 启用列排序
        detailIpColumn.setSortable(true);
        detailUpdateTimeColumn.setSortable(true);
        detailMsgCodeColumn.setSortable(true);
        detailMsgInfColumn.setSortable(true);
        detailRemarkColumn.setSortable(true);





        detailTableView.setItems(detailList);
        detailTableView.setPlaceholder(new Label("请选择左侧汇总项查看详细记录"));
        
        // 添加双击事件监听器
        detailTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                DebugLogPO selectedLog = detailTableView.getSelectionModel().getSelectedItem();
                if (selectedLog != null) {
                    showDetailDialog(selectedLog);
                }
            }
        });
        
        // 添加测试数据以验证测试列是否显示
        System.out.println("当前表格列数: " + detailTableView.getColumns().size());
        for (int i = 0; i < detailTableView.getColumns().size(); i++) {
            System.out.println("列 " + i + ": " + detailTableView.getColumns().get(i).getText());
        }

        // 查询按钮
        queryButton.setOnAction(e -> {
            DebugLogGroupPO selectedGroup = summaryTableView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                loadDetailData(selectedGroup);
            } else {
                new Alert(Alert.AlertType.WARNING, "请先选择左侧汇总项").showAndWait();
            }
        });

        // IP选择变化监听器
        ipComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            DebugLogGroupPO selectedGroup = summaryTableView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                loadDetailData(selectedGroup);
            }
        });

        // 关键字输入监听器
        keywordTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            DebugLogGroupPO selectedGroup = summaryTableView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                loadDetailData(selectedGroup);
            }
        });

        // 日期选择监听器
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            DebugLogGroupPO selectedGroup = summaryTableView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                loadDetailData(selectedGroup);
            }
        });
        
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            DebugLogGroupPO selectedGroup = summaryTableView.getSelectionModel().getSelectedItem();
            if (selectedGroup != null) {
                loadDetailData(selectedGroup);
            }
        });

        // 初始化IP下拉框
        loadIpOptions();
    }

    private void loadIpOptions() {
        // IP选项将在选择左侧汇总项时动态加载
        ObservableList<String> ipOptions = FXCollections.observableArrayList();
        ipComboBox.setItems(ipOptions);
    }

    /**
     * 设置默认日期范围为三天内
     */
    private void setDefaultDateRange() {
        // 设置结束日期为今天
        endDatePicker.setValue(java.time.LocalDate.now());
        // 设置开始日期为三天前
        startDatePicker.setValue(java.time.LocalDate.now().minusDays(3));
    }

    private void loadIpOptionsForGroup(DebugLogGroupPO group) {
        try {
            List<String> ipList = debugLogRpcService.queryIpListByGroup(group);
            ObservableList<String> ipOptions = FXCollections.observableArrayList();
            ipOptions.add("全部"); // 添加"全部"选项
            if (ipList != null) {
                ipOptions.addAll(ipList);
            }
            ipComboBox.setItems(ipOptions);
            ipComboBox.setValue("全部"); // 默认选择"全部"
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "加载IP选项失败: " + e.getMessage()).showAndWait();
        }
    }

    private void loadSummaryData() {
        try {
            DebugLogQuery query = new DebugLogQuery();

            if (startDatePicker.getValue() != null) {
                query.setBeginDt(startDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            if (endDatePicker.getValue() != null) {
                query.setEndDt(endDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            query.setProjectName(globalPropes.getProjectName());
            List<DebugLogGroupPO> groups = debugLogRpcService.qryGroupForList(query);
            summaryList.clear();
            if (groups != null) {
                summaryList.addAll(groups);
            }
            statusLabel.setText("汇总数据加载完成，共 " + summaryList.size() + " 条记录");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "加载汇总数据失败: " + e.getMessage()).showAndWait();
            statusLabel.setText("加载汇总数据失败");
        }
    }

    private void loadDetailData(DebugLogGroupPO group) {
        try {
            System.out.println("开始加载详细数据，组信息: " + group.getGroupName() + ", " + group.getTransName());
            // 首先加载IP选项
            loadIpOptionsForGroup(group);
            
            DebugLogPO query = new DebugLogPO();
            query.setGroupName(group.getGroupName());
            query.setProjectName(group.getProjectName());
            query.setInterfaceName(group.getInterfaceName());
            query.setTransName(group.getTransName());

            // 添加IP过滤
            if (ipComboBox.getValue() != null && !ipComboBox.getValue().isEmpty() && !"全部".equals(ipComboBox.getValue())) {
                query.setIp(ipComboBox.getValue());
            }

            // 添加日期过滤
            if (startDatePicker.getValue() != null) {
                // 日期过滤在查询后进行处理
            }
            if (endDatePicker.getValue() != null) {
                // 日期过滤在查询后进行处理
            }

            // 添加关键字过滤
            if (StringUtils.isNotBlank(keywordTextField.getText())) {
                // 关键字过滤在查询后进行处理
            }

            List<DebugLogPO> logs = debugLogRpcService.queryForList(query);
            System.out.println("查询到原始数据条数: " + (logs != null ? logs.size() : 0));
            detailList.clear();
            if (logs != null) {

                // 日期过滤
                if (startDatePicker.getValue() != null || endDatePicker.getValue() != null) {
                    String startDate = null;
                    String endDate = null;
                    
                    if (startDatePicker.getValue() != null) {
                        startDate = startDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    if (endDatePicker.getValue() != null) {
                        endDate = endDatePicker.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                    
                    final String finalStartDate = startDate;
                    final String finalEndDate = endDate;
                    
                    logs = logs.stream()
                        .filter(log -> {
                            if (log.getUpdateTime() == null) return false;
                            
                            String logDate = log.getUpdateTime().substring(0, Math.min(10, log.getUpdateTime().length()));
                            
                            // 检查开始日期
                            if (finalStartDate != null && logDate.compareTo(finalStartDate) < 0) {
                                return false;
                            }
                            
                            // 检查结束日期
                            if (finalEndDate != null && logDate.compareTo(finalEndDate) > 0) {
                                return false;
                            }
                            
                            return true;
                        })
                        .collect(java.util.stream.Collectors.toList());
                }
                
                // 关键字过滤
                if (StringUtils.isNotBlank(keywordTextField.getText())) {
                    String keyword = keywordTextField.getText().toLowerCase();
                    logs = logs.stream()
                        .filter(log -> 
                            (log.getDebugDesc() != null && log.getDebugDesc().toLowerCase().contains(keyword)) ||
                            (log.getMsgCode() != null && log.getMsgCode().toLowerCase().contains(keyword)) ||
                            (log.getMsgInf() != null && log.getMsgInf().toLowerCase().contains(keyword)) ||
                            (log.getIp() != null && log.getIp().toLowerCase().contains(keyword))
                        )
                        .collect(java.util.stream.Collectors.toList());
                }
                for (DebugLogPO log : logs) {
                    log.setUpdateTime(MrDateUtils.toShortTime(log.getUpdateTime()));
                    String remark="";
                    if (StringUtils.isBlank(log.getDebugDesc())) {
                        remark=log.getJrnNo();
                    }else {
                        remark=log.getDebugDesc();
                    }
                    log.setDebugDesc(remark);
                }


                detailList.addAll(logs);
            }
            
            System.out.println("最终添加到表格的数据条数: " + detailList.size());
            recordCountLabel.setText("记录数: " + detailList.size());
            statusLabel.setText("已加载 " + group.getTransName() + " 的详细记录");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "加载详细数据失败: " + e.getMessage()).showAndWait();
            statusLabel.setText("加载详细数据失败");
        }
    }

    private void showDetailDialog(DebugLogPO log) {
        try {
            // 构建日志内容
            StringBuilder content = new StringBuilder();
            content.append("=== 调试日志详情 ===\n\n");
            
            // 基本信息
            content.append("【基本信息】\n");
            content.append("执行时间: ").append(MrDateUtils.toShortTime(log.getUpdateTime())).append("\n");
            content.append("IP地址: ").append(log.getIp()).append("\n");
            content.append("消息码: ").append(log.getMsgCode()).append("\n");
            content.append("消息信息: ").append(log.getMsgInf()).append("\n");
            content.append("流水号: ").append(log.getJrnNo()).append("\n");
            content.append("请求ID: ").append(log.getRequestId()).append("\n");
            content.append("备注信息: ").append(log.getDebugDesc()).append("\n\n");
            
            // 请求参数
            content.append("【请求参数】\n");
            content.append(log.getReqParam() != null ? log.getReqParam() : "").append("\n\n");
            
            // 响应参数
            content.append("【响应参数】\n");
            content.append(log.getRspParam() != null ? log.getRspParam() : "").append("\n\n");

            // 使用LogDialogUtil显示日志内容
            LogDialogUtil.showLogDialog(content.toString(), "调试日志详情 - " + log.getTransName(), 900, 700);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "显示详情失败: " + e.getMessage()).showAndWait();
        }
    }

    // 跳转到Restful测试界面并传递组信息
    private void openRestfulTest(DebugLogGroupPO group) {
        if (group == null) return;
        
        try {
            // 获取主控制器
            MainController mainController = MrSpringContextHolder.getBean(MainController.class);
            if (mainController != null) {
                // 显示测试页面
                mainController.showPage("测试-" + group.getTransName(), "/fxml/pane_restful.xml");
                
                // 设置测试信息
                if (restfulController != null) {
                    // 这里可以根据组信息构建URL或使用默认URL
                    String url = group.getInterfaceName(); // 使用接口名作为URL的一部分
                    if (url != null && !url.isEmpty()) {
                        restfulController.setUrl(url);
                    }
                    restfulController.setTransInfo(group.getTransName(), group.getTransNmeDsc());
                    restfulController.onQueryDebugLog();
                }
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "打开测试界面失败: " + e.getMessage()).showAndWait();
        }
    }
} 