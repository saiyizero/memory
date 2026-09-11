package com.murong.ecp.tools.fx.infrastructure.msgcode;

import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import lombok.Data;


public class CrResult<T>{
    private SuccessFailureEnum successFailure;
    private String msgInf;
    private T data;
    private String dropsql;

    public boolean isSucess() {
        return successFailure.equals(SuccessFailureEnum.SUCCESS);
    }

    public CrResult(SuccessFailureEnum successFailure) {
        this.successFailure = successFailure;
    }

    public static CrResult setSuccessFailure(SuccessFailureEnum sucess) {
       return new CrResult(sucess);
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
