package com.murong.ecp.tools.fx.controller;

import com.murong.ecp.tools.fx.domain.entity.GlobalProperties;
import com.murong.ecp.tools.fx.enums.AuditBizTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditOperTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.po.AuditRecordPO;
import com.murong.ecp.tools.fx.infrastructure.repository.query.AuditRecordQuery;
import com.murong.ecp.tools.fx.infrastructure.rpc.AuditRecordRpcService;
import com.murong.ecp.tools.fx.infrastructure.utils.ViewUtils;
import com.murong.ecp.tools.fx.infrastructure.view.AuditDiffDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaneAuditedAuditController {

    @Autowired
    private AuditRecordRpcService auditRecordRpcService;

    @Autowired
    private GlobalProperties globalProps;

    @FXML private ComboBox<AuditBizTypeEnum> bizTypeCombo;
    @FXML private ComboBox<AuditOperTypeEnum> operTypeCombo;
    @FXML private ComboBox<AuditStatusEnum> statusCombo;
    @FXML private TextField submitByField;
    @FXML private TextField keywordField;
    @FXML private Button queryButton;
    @FXML private Button resetButton;
    @FXML private Label totalCountLabel;
    @FXML private TableView<AuditRecordPO> tableView;
    @FXML private TableColumn<AuditRecordPO, String> bizTypeColumn;
    @FXML private TableColumn<AuditRecordPO, String> bizNameColumn;
    @FXML private TableColumn<AuditRecordPO, String> operTypeColumn;
    @FXML private TableColumn<AuditRecordPO, String> auditStatusColumn;
    @FXML private TableColumn<AuditRecordPO, String> submitByColumn;
    @FXML private TableColumn<AuditRecordPO, String> submitTimeColumn;
    @FXML private TableColumn<AuditRecordPO, String> auditByColumn;
    @FXML private TableColumn<AuditRecordPO, String> auditTimeColumn;
    @FXML private TableColumn<AuditRecordPO, String> auditRemarkColumn;
    @FXML private TableColumn<AuditRecordPO, Void> actionColumn;

    private final ObservableList<AuditRecordPO> dataList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!ViewUtils.validateProjectConfiguration(globalProps)) {
            return;
        }
        tableView.setItems(dataList);
        tableView.setFixedCellSize(32);
        tableView.setRowFactory(tv -> {
            javafx.scene.control.TableRow<AuditRecordPO> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openCompare(row.getItem());
                }
            });
            return row;
        });
        initCombos();
        bindColumns();
        initActionColumn();
        queryRecords();
    }

    @FXML
    public void queryRecords() {
        try {
            AuditRecordQuery query = new AuditRecordQuery();
            query.setGroupName(globalProps.getGroupName());
            query.setProjectName(globalProps.getProjectName());
            query.setAuditedOnly(true);
            if (bizTypeCombo.getValue() != null) {
                query.setBizType(bizTypeCombo.getValue().getCode());
            }
            if (operTypeCombo.getValue() != null) {
                query.setOperType(operTypeCombo.getValue().getCode());
            }
            if (statusCombo.getValue() != null) {
                query.setAuditStatus(statusCombo.getValue().getCode());
            }
            query.setSubmitBy(StringUtils.trimToNull(submitByField.getText()));
            query.setKeyword(StringUtils.trimToNull(keywordField.getText()));
            List<AuditRecordPO> list = auditRecordRpcService.queryByCondition(query);
            dataList.setAll(list == null ? List.of() : list);
            totalCountLabel.setText("总数: " + dataList.size());
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("查询已审核记录失败: " + e.getMessage());
        }
    }

    @FXML
    public void resetQuery() {
        bizTypeCombo.setValue(null);
        operTypeCombo.setValue(null);
        statusCombo.setValue(null);
        submitByField.clear();
        keywordField.clear();
        queryRecords();
    }

    private void initCombos() {
        bizTypeCombo.getItems().setAll(AuditBizTypeEnum.values());
        bizTypeCombo.setPromptText("全部");
        renderCombo(bizTypeCombo, AuditBizTypeEnum::getDesc);

        operTypeCombo.getItems().setAll(AuditOperTypeEnum.values());
        operTypeCombo.setPromptText("全部");
        renderCombo(operTypeCombo, AuditOperTypeEnum::getDesc);

        statusCombo.getItems().setAll(AuditStatusEnum.APPROVED, AuditStatusEnum.REJECTED);
        statusCombo.setPromptText("全部");
        renderCombo(statusCombo, AuditStatusEnum::getDesc);
    }

    private <T> void renderCombo(ComboBox<T> combo, java.util.function.Function<T, String> labelFn) {
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? combo.getPromptText() : labelFn.apply(item));
            }
        });
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : labelFn.apply(item));
            }
        });
    }

    private void bindColumns() {
        bizTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(AuditBizTypeEnum.getDescByCode(data.getValue().getBizType())));
        bizNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(StringUtils.defaultIfBlank(data.getValue().getBizName(), data.getValue().getBizKey())));
        operTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(AuditOperTypeEnum.getDescByCode(data.getValue().getOperType())));
        auditStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(AuditStatusEnum.getDescByCode(data.getValue().getAuditStatus())));
        submitByColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubmitBy()));
        submitTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubmitTime()));
        auditByColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuditBy()));
        auditTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuditTime()));
        auditRemarkColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuditRemark()));
        center(bizTypeColumn);
        center(operTypeColumn);
        center(auditStatusColumn);
        center(submitByColumn);
        center(submitTimeColumn);
        center(auditByColumn);
        center(auditTimeColumn);
    }

    private void center(TableColumn<AuditRecordPO, String> column) {
        column.setStyle("-fx-alignment: CENTER;");
    }

    private void initActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button compareBtn = styledButton("对比", "#007bff");
            private final HBox box = new HBox(6, compareBtn);

            {
                box.setAlignment(Pos.CENTER);
                compareBtn.setOnAction(e -> openCompare(row()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                setGraphic(box);
            }

            private AuditRecordPO row() {
                return getTableView().getItems().get(getIndex());
            }
        });
    }

    private Button styledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-background-radius: 10px;" +
                "-fx-padding: 3px 8px;" +
                "-fx-cursor: hand;"
        );
        return button;
    }

    private void openCompare(AuditRecordPO row) {
        if (row == null) {
            return;
        }
        try {
            AuditRecordPO full = auditRecordRpcService.queryById(row.getId());
            if (full == null) {
                ViewUtils.alertForFail("未找到已审核记录");
                return;
            }
            AuditDiffDialog.show(full, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            ViewUtils.alertForFail("打开对比失败: " + e.getMessage());
        }
    }
}
