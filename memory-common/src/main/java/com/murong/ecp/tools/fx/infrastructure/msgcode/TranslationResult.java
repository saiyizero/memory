package com.murong.ecp.tools.fx.infrastructure.msgcode;

import java.util.ArrayList;
import java.util.List;

public class TranslationResult {
    private boolean success;
    private String message;
    private int totalCount;
    private int successCount;
    private int failCount;
    private List<String> failedItems;

    public TranslationResult() {
        this.failedItems = new ArrayList<>();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public List<String> getFailedItems() { return failedItems; }
    public void setFailedItems(List<String> failedItems) { this.failedItems = failedItems; }

    public void incrementSuccessCount() { this.successCount++; }
    public void incrementFailCount() { this.failCount++; }
    public void addFailedItem(String item) { this.failedItems.add(item); }

    public double getSuccessRate() {
        return totalCount > 0 ? (double) successCount / totalCount * 100 : 0.0;
    }

    public void addResult(TranslationResult other) {
        this.totalCount += other.totalCount;
        this.successCount += other.successCount;
        this.failCount += other.failCount;
        this.failedItems.addAll(other.failedItems);
    }
}
