package com.murong.ecp.tools.fx.infrastructure.repository.po;

import com.murong.ecp.tools.fx.infrastructure.annotation.JTable;
import lombok.Data;

@Data
@JTable(name="interface_data_his")
public class InterfaceDataHisPO {
    private String groupName;        // 分组名
    private String projectName;      // 项目名称
    private String appName;          // 应用名称
    private String interfaceName;    // 接口名称
    private String transName;        // 交易名称
    private String className;        // 类名
    private String transCommentZh;   // 交易中文注释
    private String transCommentEn;   // 交易英文注释
    private String interfaceUrl;     // 接口URL
    private String methodUrl;        // 方法URL
    private String lableName;        // 标签名称
}