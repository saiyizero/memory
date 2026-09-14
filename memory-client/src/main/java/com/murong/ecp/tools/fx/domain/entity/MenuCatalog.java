package com.murong.ecp.tools.fx.domain.entity;

import java.util.List;

/**
 * 客户端菜单目录，侧边栏与菜单管理共用同一份定义。
 */
public final class MenuCatalog {

    public static final String MENU_MANAGEMENT_KEY = "menuManagement";

    public record Item(String key, String text, String icon, String fxmlPath, boolean adminOnly) {
        public Item(String key, String text, String icon, String fxmlPath) {
            this(key, text, icon, fxmlPath, false);
        }
    }

    public record Group(String groupKey, String groupName, String groupIcon, List<Item> items) {
    }

    private static final List<Group> GROUPS = List.of(
            new Group("common", "工具管理", "remixB/global-line.png", List.of(
                    new Item("logService", "日志服务", "remixB/command-fill.png", "/fxml/pane_server_mng.fxml"),
                    new Item("transactionReview", "检索记录", "remixB/command-fill.png", "/fxml/pane_transaction_review.fxml"),
                    new Item("textEditor", "文本编辑", "remixB/command-fill.png", "/fxml/pane_format.fxml"),
                    new Item("AiPrompt", "AI提示词", "remixB/command-fill.png", "/fxml/pane_ai_prompt.fxml"),
                    new Item("terminal", "终端", "remixB/terminal-box-line.png", "/fxml/pane_terminal.fxml")
            )),
            new Group("collection", "集合管理", "remixB/list-radio.png", List.of(
                    new Item("enum", "枚举维护", "remixB/command-fill.png", "/fxml/pane_enum.fxml"),
                    new Item("baseDict", "基础字典", "remixB/command-fill.png", "/fxml/pane_base_dict.fxml"),
                    new Item("bizDict", "业务字典", "remixB/command-fill.png", "/fxml/pane_bizdict.fxml"),
                    new Item("infoCode", "信息码", "remixB/command-fill.png", "/fxml/pane_infocode.fxml"),
                    new Item("commonObj", "公共对象", "remixB/command-fill.png", "/fxml/pane_commonobj.fxml")
            )),
            new Group("transaction", "交易管理", "remixB/color-filter-ai-line.png", List.of(
                    new Item("transactionApi", "交易接口", "remixB/command-fill.png", "/fxml/pane_transaction.xml"),
                    new Item("transactionTest", "测试记录", "remixB/command-fill.png", "/fxml/pane_restful_record.fxml"),
                    new Item("transactionLable", "交易标签", "remixB/command-fill.png", "/fxml/pane_lable.xml")
            )),
            new Group("table", "库表管理", "remixB/database-2-line.png", List.of(
                    new Item("tableManager", "表结构", "remixB/command-fill.png", "/fxml/pane_table_mng.xml"),
                    new Item("exesql", "SQL执行", "remixB/command-fill.png", "/fxml/pane_exesql.fxml"),
                    new Item("exeRecord", "执行记录", "remixB/command-fill.png", "/fxml/pane_exe_record.fxml"),
                    new Item("datasourceManager", "数据源", "remixB/command-fill.png", "/fxml/pane_db_mangr.fxml"),
                    new Item("tableDiff", "结构对比", "remixB/command-fill.png", "/fxml/pane_table_diff.fxml")
            )),
            new Group("project", "项目管理", "remixB/settings-5-line.png", List.of(
                    new Item("projectStructure", "项目结构", "remixB/command-fill.png", "/fxml/pane_structure.fxml"),
                    new Item("projectGroup", "项目组", "remixB/command-fill.png", "/fxml/pane_project_group.fxml"),
                    new Item("userManagement", "用户管理", "remixB/command-fill.png", "/fxml/pane_user_management.fxml"),
                    new Item("menuManagement", "菜单管理", "remixB/command-fill.png", "/fxml/pane_menu_management.fxml", true),
                    new Item("generateTrans", "交易生成", "remixB/command-fill.png", "/fxml/pane_generate_trans.fxml")
            )),
            new Group("about", "基础配置", "remixB/settings-5-line.png", List.of(
                    new Item("basicConfig", "基础配置", "remixB/command-fill.png", "/fxml/pane_basic_config.fxml"),
                    new Item("knowledge", "知识库", "remixB/command-fill.png", "/fxml/pane_knowledge.fxml")
            ))
    );

    private MenuCatalog() {
    }

    public static List<Group> groups() {
        return GROUPS;
    }

    public static Item findItem(String key) {
        if (key == null) {
            return null;
        }
        for (Group group : GROUPS) {
            for (Item item : group.items()) {
                if (key.equals(item.key())) {
                    return item;
                }
            }
        }
        return null;
    }

    public static Group findGroupByItemKey(String key) {
        if (key == null) {
            return null;
        }
        for (Group group : GROUPS) {
            for (Item item : group.items()) {
                if (key.equals(item.key())) {
                    return group;
                }
            }
        }
        return null;
    }
}
