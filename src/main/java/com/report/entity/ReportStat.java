package com.report.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReportStat {
    private Long id;
    private String statDate;
    private Integer totalOrder;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}
