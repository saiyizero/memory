package com.murong.ecp.tools.fx.infrastructure.view;

import com.murong.ecp.tools.fx.enums.AuditBizTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditOperTypeEnum;
import com.murong.ecp.tools.fx.enums.AuditStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.repository.po.AuditRecordPO;
import com.murong.ecp.tools.fx.infrastructure.utils.AuditJsonDiffUtil;
import com.murong.ecp.tools.fx.infrastructure.utils.AuditJsonDiffUtil.ChangeType;
import com.murong.ecp.tools.fx.infrastructure.utils.AuditJsonDiffUtil.DiffRow;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.BiConsumer;

public final class AuditDiffDialog {

    private AuditDiffDialog() {
    }

    public static void show(AuditRecordPO record, BiConsumer<AuditRecordPO, String> onApprove, BiConsumer<AuditRecordPO, String> onReject) {
        if (record == null) {
            return;
        }
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("待审核数据对比");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(true);
        Window owner = currentWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));
        root.setPrefSize(1080, 640);
        dialog.getDialogPane().getStyleClass().add("audit-diff-dialog");
        var css = AuditDiffDialog.class.getResource("/css/audit-diff-dialog.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }

        Label title = new Label(buildTitle(record));
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        Label meta = new Label(buildMeta(record));
        meta.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        meta.setWrapText(true);

        CheckBox onlyDiffBox = new CheckBox("仅显示差异");
        onlyDiffBox.setSelected(true);

        List<DiffRow> allRows = AuditJsonDiffUtil.diff(record.getOldData(), record.getNewData());
        ObservableList<DiffRow> source = FXCollections.observableArrayList(allRows);
        FilteredList<DiffRow> filtered = new FilteredList<>(source, row -> row.getChangeType() != ChangeType.UNCHANGED);
        onlyDiffBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                filtered.setPredicate(row -> !Boolean.TRUE.equals(newVal) || row.getChangeType() != ChangeType.UNCHANGED));

        Label summary = new Label(buildSummary(allRows));
        summary.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        HBox toolbar = new HBox(16, onlyDiffBox, summary);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TableView<DiffRow> tableView = createDiffTable(filtered);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        root.getChildren().addAll(title, meta, toolbar, tableView);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        boolean pending = AuditStatusEnum.PENDING.getCode().equals(record.getAuditStatus());
        if (pending) {
            ButtonType approveType = new ButtonType("通过", ButtonBar.ButtonData.YES);
            ButtonType rejectType = new ButtonType("驳回", ButtonBar.ButtonData.NO);
            dialog.getDialogPane().getButtonTypes().add(0, approveType);
            dialog.getDialogPane().getButtonTypes().add(1, rejectType);
            Button approveBtn = (Button) dialog.getDialogPane().lookupButton(approveType);
            Button rejectBtn = (Button) dialog.getDialogPane().lookupButton(rejectType);
            approveBtn.setStyle("-fx-background-color: #52c41a; -fx-text-fill: white;");
            rejectBtn.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white;");
            approveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                if (onApprove != null) {
                    onApprove.accept(record, "");
                }
                dialog.close();
            });
            rejectBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                String remark = askRejectRemark();
                if (remark == null) {
                    return;
                }
                if (onReject != null) {
                    onReject.accept(record, remark);
                }
                dialog.close();
            });
        }

        dialog.setOnShown(e -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setMinWidth(900);
            stage.setMinHeight(520);
            tableView.refresh();
        });
        dialog.showAndWait();
    }

    private static TableView<DiffRow> createDiffTable(FilteredList<DiffRow> rows) {
        TableView<DiffRow> tableView = new TableView<>(rows);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("没有可对比的字段"));
        tableView.setFixedCellSize(-1);

        TableColumn<DiffRow, String> typeCol = new TableColumn<>("差异");
        typeCol.setPrefWidth(88);
        typeCol.setMinWidth(80);
        typeCol.setMaxWidth(96);
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getChangeTypeDesc()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                DiffRow row = rowOf(this);
                if (empty || row == null) {
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                badge.setText(item);
                badge.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: "
                        + badgeColor(row.getChangeType()) + "; -fx-background-radius: 11; -fx-padding: 3 10 3 10;");
                setGraphic(badge);
                setStyle("-fx-alignment: CENTER; -fx-background-color: " + rowTint(row.getChangeType()) + ";");
            }
        });

        TableColumn<DiffRow, String> fieldCol = new TableColumn<>("字段");
        fieldCol.setPrefWidth(180);
        fieldCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFieldLabel()));
        fieldCol.setCellFactory(col -> valueCell(fieldCol, ValueSide.FIELD));

        TableColumn<DiffRow, String> oldCol = new TableColumn<>("原数据");
        oldCol.setPrefWidth(380);
        oldCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOldValue()));
        oldCol.setCellFactory(col -> valueCell(oldCol, ValueSide.OLD));

        TableColumn<DiffRow, String> newCol = new TableColumn<>("新数据");
        newCol.setPrefWidth(380);
        newCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNewValue()));
        newCol.setCellFactory(col -> valueCell(newCol, ValueSide.NEW));

        tableView.getColumns().add(typeCol);
        tableView.getColumns().add(fieldCol);
        tableView.getColumns().add(oldCol);
        tableView.getColumns().add(newCol);
        tableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected double computePrefHeight(double width) {
                DiffRow item = getItem();
                if (isEmpty() || item == null) {
                    return 36;
                }
                double fieldH = measureTextHeight(item.getFieldLabel(), fieldCol.getWidth());
                double oldH = measureTextHeight(item.getOldValue(), oldCol.getWidth());
                double newH = measureTextHeight(item.getNewValue(), newCol.getWidth());
                return Math.max(36, Math.max(fieldH, Math.max(oldH, newH)) + 20);
            }
        });
        return tableView;
    }

    private enum ValueSide {
        FIELD, OLD, NEW
    }

    private static TableCell<DiffRow, String> valueCell(TableColumn<DiffRow, String> column, ValueSide side) {
        return new TableCell<>() {
            private final Text text = new Text();
            private final VBox box = new VBox(text);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(8, 10, 8, 10));
                box.setFillWidth(true);
                text.setTextOrigin(VPos.TOP);
                text.wrappingWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                    double width = column.getWidth() - 28;
                    return Math.max(80, width);
                }, column.widthProperty()));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                DiffRow row = rowOf(this);
                if (empty || row == null) {
                    setGraphic(null);
                    setStyle("");
                    setPrefHeight(USE_COMPUTED_SIZE);
                    return;
                }
                boolean blank = StringUtils.isBlank(item);
                text.setText(blank ? "（空）" : item);
                text.setFill(Color.web(valueTextColor(row.getChangeType(), side, blank)));
                text.setFont(Font.font("System", side == ValueSide.FIELD ? FontWeight.BOLD : FontWeight.NORMAL, 13));
                box.setStyle("-fx-background-color: " + valueBackground(row.getChangeType(), side, blank)
                        + "; -fx-background-radius: 4;");
                setGraphic(box);
                setStyle("-fx-background-color: " + rowTint(row.getChangeType()) + "; -fx-padding: 2 4 2 4;");
                setPrefHeight(USE_COMPUTED_SIZE);
            }
        };
    }

    private static DiffRow rowOf(TableCell<DiffRow, ?> cell) {
        TableRow<DiffRow> tableRow = cell.getTableRow();
        if (tableRow != null && tableRow.getItem() != null) {
            return tableRow.getItem();
        }
        TableView<DiffRow> tableView = cell.getTableView();
        int index = cell.getIndex();
        if (tableView != null && index >= 0 && index < tableView.getItems().size()) {
            return tableView.getItems().get(index);
        }
        return null;
    }

    private static double measureTextHeight(String value, double columnWidth) {
        Text probe = new Text(StringUtils.isBlank(value) ? "（空）" : value);
        probe.setFont(Font.font("System", 13));
        probe.setWrappingWidth(Math.max(80, columnWidth - 28));
        return probe.getLayoutBounds().getHeight();
    }

    private static String badgeColor(ChangeType type) {
        return switch (type) {
            case ADDED -> "#389e0d";
            case DELETED -> "#cf1322";
            case MODIFIED -> "#d46b08";
            case UNCHANGED -> "#8c8c8c";
        };
    }

    private static String rowTint(ChangeType type) {
        return switch (type) {
            case ADDED -> "#f6ffed";
            case DELETED -> "#fff1f0";
            case MODIFIED -> "#fff7e6";
            case UNCHANGED -> "transparent";
        };
    }

    private static String valueBackground(ChangeType type, ValueSide side, boolean blank) {
        if (side == ValueSide.FIELD) {
            return "#ffffff";
        }
        if (blank || type == ChangeType.UNCHANGED) {
            return "transparent";
        }
        if (side == ValueSide.OLD && (type == ChangeType.MODIFIED || type == ChangeType.DELETED)) {
            return "#ffccc7";
        }
        if (side == ValueSide.NEW && (type == ChangeType.MODIFIED || type == ChangeType.ADDED)) {
            return "#b7eb8f";
        }
        return "transparent";
    }

    private static String valueTextColor(ChangeType type, ValueSide side, boolean blank) {
        if (side == ValueSide.FIELD) {
            return "#1f1f1f";
        }
        if (blank) {
            return "#8c8c8c";
        }
        if (type == ChangeType.UNCHANGED) {
            return "#262626";
        }
        if (side == ValueSide.OLD && (type == ChangeType.MODIFIED || type == ChangeType.DELETED)) {
            return "#820014";
        }
        if (side == ValueSide.NEW && (type == ChangeType.MODIFIED || type == ChangeType.ADDED)) {
            return "#135200";
        }
        return "#8c8c8c";
    }

    private static String buildTitle(AuditRecordPO record) {
        return AuditBizTypeEnum.getDescByCode(record.getBizType())
                + "  ·  "
                + StringUtils.defaultIfBlank(record.getBizName(), record.getBizKey())
                + "  ·  "
                + AuditOperTypeEnum.getDescByCode(record.getOperType());
    }

    private static String buildMeta(AuditRecordPO record) {
        return "提交人: " + StringUtils.defaultString(record.getSubmitBy())
                + "    提交时间: " + StringUtils.defaultString(record.getSubmitTime())
                + "    状态: " + AuditStatusEnum.getDescByCode(record.getAuditStatus())
                + (StringUtils.isBlank(record.getAuditBy()) ? "" : "    审核人: " + record.getAuditBy())
                + (StringUtils.isBlank(record.getAuditRemark()) ? "" : "    审核意见: " + record.getAuditRemark());
    }

    private static String buildSummary(List<DiffRow> rows) {
        long added = rows.stream().filter(r -> r.getChangeType() == ChangeType.ADDED).count();
        long deleted = rows.stream().filter(r -> r.getChangeType() == ChangeType.DELETED).count();
        long modified = rows.stream().filter(r -> r.getChangeType() == ChangeType.MODIFIED).count();
        return "新增 " + added + " 项，删除 " + deleted + " 项，修改 " + modified + " 项";
    }

    private static String askRejectRemark() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("驳回原因");
        dialog.setHeaderText("请填写驳回意见");
        dialog.initModality(Modality.APPLICATION_MODAL);
        TextArea area = new TextArea();
        area.setPrefRowCount(4);
        area.setWrapText(true);
        area.setPromptText("例如：字段注释不完整，请补充后重新提交");
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(type -> type == ButtonType.OK ? StringUtils.trimToEmpty(area.getText()) : null);
        return dialog.showAndWait().orElse(null);
    }

    private static Window currentWindow() {
        for (Window window : Stage.getWindows()) {
            if (window.isShowing()) {
                return window;
            }
        }
        return null;
    }
}
