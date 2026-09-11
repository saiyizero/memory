package com.murong.ecp.tools.fx.infrastructure.msgcode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;


public class CrResult<T>{
    @JsonProperty
    private SuccessFailureEnum successFailure;
    private String msgInf;
    private T data;
    private String dropsql;

    public boolean isSucess() {
        return successFailure != null && successFailure.equals(SuccessFailureEnum.SUCCESS);
    }

    public CrResult() {
    }

    public CrResult(SuccessFailureEnum successFailure) {
        this.successFailure = successFailure;
    }

    public static CrResult setSuccessFailure(SuccessFailureEnum sucess) {
       return new CrResult(sucess);
    }

    public SuccessFailureEnum getSuccessFailure() {
        return successFailure;
    }

    public String getMsgInf() {
        return msgInf;
    }

    public void setMsgInf(String msgInf) {
        this.msgInf = msgInf;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getDropsql() {
        return dropsql;
    }

    public void setDropsql(String dropsql) {
        this.dropsql = dropsql;
    }
}
