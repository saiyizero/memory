package com.murong.ecp.bp.common.util;


public class BaseDO {
    private String creTs;
    private String creOpr;
    private String updTs;
    private String updOpr;
    private String nodId;
    private String tmSmp;

    public String getCreTs() {
        return creTs;
    }

    public void setCreTs(String creTs) {
        this.creTs = creTs;
    }

    public String getCreOpr() {
        return creOpr;
    }

    public void setCreOpr(String creOpr) {
        this.creOpr = creOpr;
    }

    public String getUpdTs() {
        return updTs;
    }

    public void setUpdTs(String updTs) {
        this.updTs = updTs;
    }

    public String getUpdOpr() {
        return updOpr;
    }

    public void setUpdOpr(String updOpr) {
        this.updOpr = updOpr;
    }

    public String getNodId() {
        return nodId;
    }

    public void setNodId(String nodId) {
        this.nodId = nodId;
    }

    public String getTmSmp() {
        return tmSmp;
    }

    public void setTmSmp(String tmSmp) {
        this.tmSmp = tmSmp;
    }
}
