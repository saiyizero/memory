package ${services[0].properties.interfacePackage}

import com.murong.ecp.bp.common.annotation.ECPClient;
import com.murong.ecp.bp.common.integration.swagger.ECPApiOperation;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import ${service.properties.requestPackage}.${service.properties.requestClass};
import ${service.properties.responsePackage}.${service.properties.responseClass};

/**
 * ${services[0].interfaceName}
 * <p>
 * 功能描述：接口类
 * 该类由代码生成器自动生成，请勿手动修改。
 * 
 * @author ${services[0].properties.authorName}
 * @editTime ${services[0].properties.editTime}
 */

@ECPClient(name = "${services[0].simpleName}")
@RequestMapping("${services[0].properties.interfaceUrl}")
public interface ${services[0].interfaceName} {

    <#list services as service>
    @ECPApiOperation(value = "${service.transCommentZh}", notes = "${service.transCommentEn}")
    @RequestMapping("${service.properties.methodUrl}")
    ${service.properties.responseClass} ${service.transName}(@RequestBody ${service.properties.requestClass} reqBO);
    </#list>
}

<#--
示例数据结构：
{
  "services": [
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
  ]
} 
-->