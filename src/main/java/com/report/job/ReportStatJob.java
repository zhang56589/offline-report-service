package com.report.job;

import com.report.service.ReportStatService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportStatJob implements Job {

    @Autowired
    private ReportStatService reportStatService;

    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("==== ReportStatJob 报表统计定时任务执行 ====");
        reportStatService.calcAndSaveStat();
    }
}
