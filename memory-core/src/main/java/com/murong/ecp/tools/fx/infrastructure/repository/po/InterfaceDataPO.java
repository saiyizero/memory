package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

/**
 * 交易服务定义持久化对象
 */
@Data
@JTable(name="interface_data")
public class InterfaceDataPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String interfaceName;    // 接口名称
    private String transName;        // 交易名称
    private String className;        // 类名
    private String moduleName;       // 模块名称
    private String transCommentZh;   // 交易中文注释
    private String transCommentEn;   // 交易英文注释
    private String transClass;       // 交易类
    private String simpleName;       // 简单名称
    private String requestJson;      // 请求JSON
    private String responseJson;     // 响应JSON
    private String requestPackage;   // 请求包
    private String responsePackage;  // 响应包
    private String requestClass;     // 请求类
    private String responseClass;    // 响应类
    private String interfaceUrl;     // 接口URL
    private String methodUrl;        // 方法URL
    private String updateBy;         // 更新人
    private String updateTime;       // 更新时间
    private String appName;          // 应用名称
    private String lableName;        // 标签名称
    private String associatEntity;   // 关联实体
    private String associatEnum;     // 关联枚举
    private String reqParentClass;   // 请求父类
    private String rspParentClass;   // 响应父类
} 