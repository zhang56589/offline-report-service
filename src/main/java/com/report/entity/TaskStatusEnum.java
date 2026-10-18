package com.report.entity;

public enum TaskStatusEnum {
    WAIT("WAIT", 0),
    RUN("RUN", 1),
    SUC("SUC", 2),
    FAIL("FAIL", 3);

    public final String status;
    public final int code;

    TaskStatusEnum(String status, int code) {
        this.status = status;
        this.code = code;
    }
}
