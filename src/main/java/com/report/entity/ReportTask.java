package com.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report_task")
public class ReportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 业务唯一任务ID，varchar字符串，代码生成UUID
    private String taskId;

    private String reportType;
    private String taskStatus;
    private String filePath;
    private String downloadUrl;
    private String failMsg;
    private Long totalCount;
    private Long finishCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime expireTime;
}
