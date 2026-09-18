package com.murong.ecp.tools.fx.enums;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BaseDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.BizMsgInfoPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.InterfaceDataPO;
import com.murong.ecp.tools.fx.infrastructure.repository.po.TableDataPO;
import org.apache.commons.lang3.StringUtils;

public enum AuditBizTypeEnum {
    ENUM("ENUM", "枚举维护", EnumDictPO.class),
    BASE_DICT("BASE_DICT", "基础字典维护", BaseDictPO.class),
    BIZ_DICT("BIZ_DICT", "业务字典维护", BizDictPO.class),
    INFO_CODE("INFO_CODE", "信息码维护", BizMsgInfoPO.class),
    COMMON_OBJ("COMMON_OBJ", "公共对象维护", CommonClassPO.class),
    INTERFACE("INTERFACE", "交易接口维护", InterfaceDataPO.class),
    TABLE("TABLE", "表结构维护", TableDataPO.class);

    private final String code;
    private final String desc;
    private final Class<?> entityClass;

    AuditBizTypeEnum(String code, String desc, Class<?> entityClass) {
        this.code = code;
        this.desc = desc;
        this.entityClass = entityClass;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String officialTable() {
        JTable table = entityClass.getAnnotation(JTable.class);
        if (table == null || StringUtils.isBlank(table.name())) {
            throw new IllegalStateException("待审核对象缺少 JTable: " + entityClass.getName());
        }
        return table.name();
    }

    public String tmpTable() {
        return officialTable() + "_tmp";
    }

    public static AuditBizTypeEnum fromClass(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        for (AuditBizTypeEnum type : values()) {
            if (type.entityClass.isAssignableFrom(clazz)) {
                return type;
            }
        }
        return null;
    }

    public static AuditBizTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditBizTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static AuditBizTypeEnum fromEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        Class<?> clazz = entity.getClass();
        for (AuditBizTypeEnum type : values()) {
            if (type.entityClass.isAssignableFrom(clazz)) {
                return type;
            }
        }
        return null;
    }

    public static String getDescByCode(String code) {
        AuditBizTypeEnum type = getByCode(code);
        return type == null ? code : type.getDesc();
    }

    public String resolveBizKey(Object entity) {
        if (entity == null) {
            return "";
        }
        return switch (this) {
            case ENUM -> {
                EnumDictPO po = (EnumDictPO) entity;
                yield join(po.getGroupName(), po.getProjectName(), po.getAppName(), po.getEnumNme(), po.getEnumRef(), po.getEnumVal());
            }
            case BASE_DICT -> StringUtils.defaultString(((BaseDictPO) entity).getNameSnake());
            case BIZ_DICT -> {
                BizDictPO po = (BizDictPO) entity;
                yield join(po.getGroupName(), po.getProjectName(), po.getAppName(), po.getNameCamel());
            }
            case INFO_CODE -> {
                BizMsgInfoPO po = (BizMsgInfoPO) entity;
                yield join(po.getGroupName(), po.getProjectName(), po.getAppName(), po.getMsgClass(), po.getMsgRef(), po.getMsgKey());
            }
            case COMMON_OBJ -> {
                CommonClassPO po = (CommonClassPO) entity;
                yield join(po.getGroupName(), po.getAppName(), po.getClassName(), po.getClassType(), po.getClassPath());
            }
            case INTERFACE -> {
                InterfaceDataPO po = (InterfaceDataPO) entity;
                yield join(po.getGroupName(), po.getProjectName(), po.getAppName(), po.getClassName(), po.getTransName());
            }
            case TABLE -> {
                TableDataPO po = (TableDataPO) entity;
                yield join(po.getGroupName(), po.getProjectName(), po.getAppName(), po.getTableNameCamel());
            }
        };
    }

    public String resolveBizName(Object entity) {
        if (entity == null) {
            return "";
        }
        return switch (this) {
            case ENUM -> {
                EnumDictPO po = (EnumDictPO) entity;
                yield firstNonBlank(po.getEnumNme() + "." + StringUtils.defaultString(po.getEnumVal()), po.getDescCn());
            }
            case BASE_DICT -> {
                BaseDictPO po = (BaseDictPO) entity;
                yield firstNonBlank(po.getNameSnake(), po.getCommentCn());
            }
            case BIZ_DICT -> {
                BizDictPO po = (BizDictPO) entity;
                yield firstNonBlank(po.getNameCamel(), po.getNameSnake(), po.getCommentCn());
            }
            case INFO_CODE -> {
                BizMsgInfoPO po = (BizMsgInfoPO) entity;
                yield firstNonBlank(po.getMsgCd(), po.getMsgKey(), po.getMsgDescCn());
            }
            case COMMON_OBJ -> {
                CommonClassPO po = (CommonClassPO) entity;
                yield firstNonBlank(po.getClassName(), po.getClassCommentCn());
            }
            case INTERFACE -> {
                InterfaceDataPO po = (InterfaceDataPO) entity;
                yield firstNonBlank(po.getTransName(), po.getTransCommentZh(), po.getInterfaceName());
            }
            case TABLE -> {
                TableDataPO po = (TableDataPO) entity;
                yield firstNonBlank(po.getTableNameSnake(), po.getTableNameCamel(), po.getTableCommentCn());
            }
        };
    }

    public String resolveGroupName(Object entity) {
        if (entity == null) {
            return null;
        }
        return switch (this) {
            case ENUM -> ((EnumDictPO) entity).getGroupName();
            case BASE_DICT -> null;
            case BIZ_DICT -> ((BizDictPO) entity).getGroupName();
            case INFO_CODE -> ((BizMsgInfoPO) entity).getGroupName();
            case COMMON_OBJ -> ((CommonClassPO) entity).getGroupName();
            case INTERFACE -> ((InterfaceDataPO) entity).getGroupName();
            case TABLE -> ((TableDataPO) entity).getGroupName();
        };
    }

    public String resolveProjectName(Object entity) {
        if (entity == null) {
            return null;
        }
        return switch (this) {
            case ENUM -> ((EnumDictPO) entity).getProjectName();
            case BASE_DICT -> null;
            case BIZ_DICT -> ((BizDictPO) entity).getProjectName();
            case INFO_CODE -> ((BizMsgInfoPO) entity).getProjectName();
            case COMMON_OBJ -> ((CommonClassPO) entity).getProjectName();
            case INTERFACE -> ((InterfaceDataPO) entity).getProjectName();
            case TABLE -> ((TableDataPO) entity).getProjectName();
        };
    }

    public String resolveAppName(Object entity) {
        if (entity == null) {
            return null;
        }
        return switch (this) {
            case ENUM -> ((EnumDictPO) entity).getAppName();
            case BASE_DICT -> null;
            case BIZ_DICT -> ((BizDictPO) entity).getAppName();
            case INFO_CODE -> ((BizMsgInfoPO) entity).getAppName();
            case COMMON_OBJ -> ((CommonClassPO) entity).getAppName();
            case INTERFACE -> ((InterfaceDataPO) entity).getAppName();
            case TABLE -> ((TableDataPO) entity).getAppName();
        };
    }

    private static String join(String... parts) {
        return String.join("|", parts == null ? new String[0] : parts);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }
}
