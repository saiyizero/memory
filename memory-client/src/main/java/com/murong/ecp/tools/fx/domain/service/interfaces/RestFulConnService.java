package com.murong.ecp.tools.fx.domain.service.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murong.ecp.tools.fx.domain.entity.RxField;
import com.murong.ecp.tools.fx.infrastructure.converter.DatabaseConvert;
import com.murong.ecp.tools.fx.infrastructure.rpc.CommonClassRpcService;
import com.murong.ecp.tools.fx.infrastructure.repository.po.CommonClassPO;
import com.murong.ecp.tools.fx.infrastructure.utils.MrDateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;

@Service
public class RestFulConnService {
    @Autowired
    CommonClassRpcService commonClassRpcService;
    /*
    * 给请json报文里面设置gda
    * */
    public String setGdaForReqJson(String reqJson, String txCd, String commonParamJson) {
        CommonClassPO commonClassPO = commonClassRpcService.queryByClassName("AbstractBaseGDA");
        List<RxField> rxFields = DatabaseConvert.jsonToFields(commonClassPO.getFieldsJson());

        try {
            ObjectMapper mapper = new ObjectMapper();
            LinkedHashMap<String, Object> reqMap = null;
            if(StringUtils.isBlank(reqJson)||StringUtils.equals(reqJson, "\"\"")) {
                reqMap = new LinkedHashMap();
            }else {
                reqMap = mapper.readValue(reqJson, LinkedHashMap.class);
            }
            // 合并公共参数
            if (commonParamJson != null && !commonParamJson.isEmpty()) {
                LinkedHashMap<String, Object> commonMap = mapper.readValue(commonParamJson, LinkedHashMap.class);
                reqMap.putAll(commonMap);
            }
            // 用fields生成gda节点，部分字段有特殊赋值
            LinkedHashMap<String, Object> gdaMap = new LinkedHashMap<>();
            String curDate = MrDateUtils.getCurrentDate();
            String curTime = MrDateUtils.getCurrentShortTime(); // HHmmss
            // 生成4位无序数字
            int randomNum = (int)(Math.random() * 10000);
            String jrnNo = "TST" + curDate + curTime + String.format("%04d", randomNum);
            for (RxField rxField : rxFields) {
                if ("gda".equals(rxField.getNameCamel())) continue;
                if (rxField.getNameCamel() != null && rxField.getNameCamel().startsWith("gda.")) {
                    String key = rxField.getNameCamel().substring("gda.".length());
                    Object value = "";
                    switch (key) {
                        case "busCnl":
                        case "sysCnl":
                            value = "TST";
                            break;
                        case "txCd":
                            value = txCd;
                            break;
                        case "txDt":
                            value = curDate;
                            break;
                        case "txTm":
                            value = curTime;
                            break;
                        case "reqJrnNo":
                        case "reqBusNo":
                            value = jrnNo;
                            break;
                        default:
                            value = "";
                    }
                    gdaMap.put(key, value);
                }
            }

            LinkedHashMap<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("gda", gdaMap);
            resultMap.putAll(reqMap);
            gdaMap.forEach( (key, value) -> {
                if(resultMap.containsKey(key)){
                    resultMap.put(key, value);
                }
            });

            //循环所有字段，如果字段值为空字符串“”或者null,则移除该字段节点
            resultMap.entrySet().removeIf(e -> e.getValue() == null || (e.getValue() instanceof String && ((String)e.getValue()).trim().isEmpty()));

            return mapper.writeValueAsString(resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            return reqJson;
        }
    }
}
