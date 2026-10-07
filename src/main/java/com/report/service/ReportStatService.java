package com.report.service;

import com.report.entity.ReportStat;
import com.baomidou.mybatisplus.extension.service.IService;
import java.time.LocalDate;
import java.util.List;

public interface ReportStatService extends IService<ReportStat> {

    List<ReportStat> listRangeDate(LocalDate start, LocalDate end);

    // 补上这个方法声明！！
    void calcAndSaveStat();
}
