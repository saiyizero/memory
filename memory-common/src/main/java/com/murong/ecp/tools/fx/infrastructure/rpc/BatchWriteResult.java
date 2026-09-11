package com.murong.ecp.tools.fx.infrastructure.rpc;

import lombok.Data;

@Data
public class BatchWriteResult {
    private int successCount;
    private int errorCount;

    public static BatchWriteResult of(int successCount, int errorCount) {
        BatchWriteResult result = new BatchWriteResult();
        result.setSuccessCount(successCount);
        result.setErrorCount(errorCount);
        return result;
    }
}
