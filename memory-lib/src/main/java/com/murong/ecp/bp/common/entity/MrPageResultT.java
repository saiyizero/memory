package com.murong.ecp.bp.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yuangou.ecp.biz.transengine.sqlsession.YGPageEntity;

public class MrPageResultT <T>  {
    private int recNum;
    private int totalrows;
    private int pagCnt;
    private int offset;
    private int limit;
    private int total;
    private T data;


    @JsonIgnore
    public void setPageEntity(YGPageEntity pageEntity) {

    }

    @JsonProperty("rec")
    public T getData() {
        return (T) this.data;
    }

    public void setRec(T data) {
        this.data=data;
    }

    public int getRecNum() {
        return this.recNum;
    }

    public int getTotalrows() {
        return this.totalrows;
    }

    public int getPagCnt() {
        return this.pagCnt;
    }

    public int getOffset() {
        return this.offset;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getTotal() {
        return this.total;
    }

    public void setRecNum(final int recNum) {
        this.recNum = recNum;
    }

    public void setTotalrows(final int totalrows) {
        this.totalrows = totalrows;
    }

    public void setPagCnt(final int pagCnt) {
        this.pagCnt = pagCnt;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    public void setTotal(final int total) {
        this.total = total;
    }
}