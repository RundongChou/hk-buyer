package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class InboundScanRequest {

    @NotNull
    private Long taskId;

    @NotBlank
    @Size(max = 40)
    private String warehouseCode;

    @NotBlank
    @Size(max = 20)
    private String qcDecision;

    @Size(max = 255)
    private String qcNote;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getQcDecision() {
        return qcDecision;
    }

    public void setQcDecision(String qcDecision) {
        this.qcDecision = qcDecision;
    }

    public String getQcNote() {
        return qcNote;
    }

    public void setQcNote(String qcNote) {
        this.qcNote = qcNote;
    }
}
