package com.report.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportData {
    @ExcelProperty("编号")
    private Long id;
    @ExcelProperty("名称")
    private String name;
    @ExcelProperty("金额")
    private BigDecimal amount;
}
