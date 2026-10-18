package com.report.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("report_stat")
public class ReportStat {
    @TableId(type = IdType.AUTO)
    @ExcelProperty("主键ID")
    private Long id;

    @ExcelProperty("统计日期")
    private String statDate;

    @ExcelProperty("订单总数")
    private Integer totalOrder;

    @ExcelProperty("订单总金额")
    private BigDecimal totalAmount;

    @ExcelProperty("创建时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
