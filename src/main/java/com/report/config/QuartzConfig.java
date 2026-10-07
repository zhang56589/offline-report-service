package com.report.config;

import com.report.job.FileCleanJob;
import com.report.job.ReportStatJob;
import com.report.job.TaskTimeoutCheckJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    // ---------------- ReportStatJob：每30秒，调试用 ----------------
    @Bean
    public JobDetail reportStatJobDetail(){
        return JobBuilder.newJob(ReportStatJob.class)
                .withIdentity("reportStatJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reportStatTrigger(){
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("*/30 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(reportStatJobDetail())
                .withIdentity("reportStatTrigger")
                .withSchedule(cron)
                .build();
    }

    // ---------------- FileCleanJob：每30秒，调试用 ----------------
    @Bean
    public JobDetail fileCleanJobDetail(){
        return JobBuilder.newJob(FileCleanJob.class)
                .withIdentity("fileCleanJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger fileCleanTrigger(){
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("*/30 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(fileCleanJobDetail())
                .withIdentity("fileCleanTrigger")
                .withSchedule(cron)
                .build();
    }

    // ---------------- TaskTimeoutCheckJob：每30秒，调试用 ----------------
    @Bean
    public JobDetail taskTimeoutCheckJobDetail(){
        return JobBuilder.newJob(TaskTimeoutCheckJob.class)
                .withIdentity("taskTimeoutCheckJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger taskTimeoutCheckTrigger(){
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("*/30 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(taskTimeoutCheckJobDetail())
                .withIdentity("taskTimeoutCheckTrigger")
                .withSchedule(cron)
                .build();
    }
}
