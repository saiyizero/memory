package ${properties.responsePackage};

import com.murong.ecp.bp.common.dict.ECPDict;
import com.murong.ecp.bp.common.dict.MrDictType;
import com.murong.ecp.dfp.pub.tool.api.DfpBaseRspBO;
import lombok.Data;
import lombok.ToString;

/**
 * ${properties.responseClass}
 * <p>
 * 功能描述：${properties.transCommentZh} 响应对象
 * 该类由代码生成器自动生成，请勿手动修改。
 * 
 * @author ${properties.authorName}
 * @editTime ${properties.editTime}
 */
@Data
@ToString(callSuper = true)
public class ${properties.responseClass} extends DfpBaseRspBO {
    <#list response as rxField>

    @ECPDict(type = MrDictType.${rxField.mrType}, length = ${rxField.length}, desc = "${rxField.comment}|${rxField.desc}", required = ${rxField.required?string('true','false')}<#if rxField.enums?? && rxField.enums?has_content>, enums = ${rxField.enums}</#if>)
    private ${rxField.type} ${rxField.name};
    </#list>
}

<#--
示例数据结构：
{
  "interfaceName": "SavingQueryService",
  "transName": "savMainAcctQry",
  "className": "SavMainAcctQry",
  "transCommentZh": "主账户信息查询",
  "transCommentEn": "Savings main account information query",
  "transClass": "query",
  "simpleName": "sav",
  "request": [
    {"name": "acNo", "type": "String", "comment": "账号", "length": 32, "desc": "Account Number", "required": false},
    {"name": "usrNo", "type": "Long", "comment": "用户号", "length": 13, "desc": "User number", "required": true},
    {"name": "capTyp", "type": "String", "comment": "资金类型", "length": 1, "desc": "Capital type", "required": false}
  ],
  "response": [
    {"name": "acNo", "type": "String", "comment": "账号", "length": 32, "desc": "Account Number", "required": false},
    {"name": "capProp", "type": "String", "comment": "资金类型", "length": 1, "desc": "Capital type", "required": false}
  ],
  "properties": {
    "authorName": "haoyulin",
    "editTime": "2025-06-11 10:00:00",
    "actionPackage": "com.murong.ecp.dbs.sav.application.action",
    "controllerPackage": "com.murong.ecp.dbs.sav.application.controller",
    "interfacePackage": "com.murong.ecp.dfp.sav.api.service",
    "requestPackage": "com.murong.ecp.dfp.sav.api.model.query.savingaccount",
    "requestClass": "SavMainAccountQueryReqBO",
    "responsePackage": "com.murong.ecp.dfp.sav.api.model.query.savingaccount",
    "responseClass": "SavMainAccountQueryRspBO",
    "interfaceUrl": "/sav/savingQuery",
    "methodUrl": "/querySavingMainAccount"
  }
}
-->