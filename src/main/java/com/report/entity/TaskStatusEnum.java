package com.report.entity;

public enum TaskStatusEnum {
    WAIT("WAIT",0),
    RUNNING("RUNNING",1),
    SUCCESS("SUCCESS",2),
    FAIL("FAIL",3);

    // 不能叫name，和JDK Enum内部私有name冲突！改成 status
    public final String status;
    public final int code;

    TaskStatusEnum(String status, int code) {
        this.status = status;
        this.code = code;
    }
}
