package com.yuangou.ecp.bp.core.common.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

public class YGException extends RuntimeException {
    private static final long serialVersionUID = 843027728253830766L;
    protected Throwable nestedException = null;
    private String code = "211007";
    private String[] msg = null;
    protected Map msgStack = null;
    private boolean isLog = false;
    private boolean smManagerFlag = true;

    protected void clone(YGException e) {
        this.nestedException = e.nestedException;
        this.code = e.code;
        this.msg = e.msg;
        this.msgStack = e.msgStack;
        this.isLog = e.isLog;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg != null && this.msg.length != 0 ? this.msg[0] : "";
    }

    public String[] getMsgs() {
        return this.msg;
    }

    public Throwable getNestedException() {
        return this.nestedException;
    }

    public void printStackTrace() {
        super.printStackTrace();
        if (this.nestedException != null) {
            System.err.println(" Nested Exception : ");
            this.nestedException.printStackTrace();
        }

    }

    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        if (this.nestedException != null) {
            out.println(" Nested Exception: ");
            this.nestedException.printStackTrace(out);
        }

    }

    public void printStackTrace(PrintWriter writer) {
        super.printStackTrace(writer);
        if (this.nestedException != null) {
            writer.println(" Nested Exception: ");
            this.nestedException.printStackTrace(writer);
        }

    }

    public boolean isLog() {
        return this.isLog;
    }

    public void setLog(boolean isLog) {
        this.isLog = isLog;
    }

    public void setMsg(String msg) {
        this.msg = new String[]{msg};
    }
}
