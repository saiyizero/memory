package ${packageName};

import com.yuangou.ecp.bp.comp.pubatc.IMessageCode;

public enum ${className} implements IMessageCode {

<#list bizMsgInfoList as msgInfo>
    /**
     * ${msgInfo.msgCd}-${msgInfo.msgDescCn}
     **/
    ${msgInfo.msgKeyJava}("${msgInfo.msgCd}", "${msgInfo.msgDescEscaped}")<#if msgInfo_has_next>,<#else>;</#if>
</#list>

    private String code;
    private String msg;

    ${className}(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getMsgCod() {
        return code;
    }

    @Override
    public String getMsgInf() {
        return msg;
    }
}
