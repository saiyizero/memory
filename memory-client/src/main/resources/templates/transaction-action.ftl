package ${properties.actionPackage};

import org.springframework.stereotype.Service;
import com.murong.ecp.dfp.pub.tool.transaction.MrTransaction;
import ${properties.requestPackage}.${properties.requestClass}
import ${properties.responsePackage}.${properties.responseClass};
import com.yuangou.ecp.bp.core.common.exception.YGException;
import com.yuangou.ecp.bp.core.common.message.YGBizMessageContext;

/**
 * ${className}
 * <p>
 * 功能描述：${transCommentZh}
 * 该类由代码生成器自动生成，请勿手动修改。
 * 
 * @author ${properties.authorName}
 * @editTime ${properties.editTime}
 */
@Service("${transName}")
public class ${className}Action extends MrTransaction<${properties.requestClass}, ${properties.responseClass}> {

    @Override
    public ${properties.responseClass} process(${properties.requestClass} reqBO, YGBizMessageContext ctx) throws YGException {
    ${properties.responseClass} responseBO = getResponseBO();
        return responseBO;
    }

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