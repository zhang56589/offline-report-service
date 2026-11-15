package com.report.job;

import com.report.service.ReportStatService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportStatJob implements Job {

    @Autowired
    private ReportStatService reportStatService;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("==== ReportStatJob 报表统计定时任务执行 ====");
        reportStatService.calcAndSaveStat();
    }
}
