package com.report.job;

import com.report.entity.ReportTask;
import com.report.mapper.ReportTaskMapper;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class FileCleanJob implements Job {

    @Autowired
    private ReportTaskMapper reportTaskMapper;

    @Value("${report.file.temp-path}")
    private String tempPath;

    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("==== FileCleanJob 定时清理任务执行 ====");
        // 查询已过期的任务
        List<ReportTask> expireList = reportTaskMapper.selectExpireTasks();
        for (ReportTask task : expireList) {
            String filePath = task.getFilePath();
            if(filePath != null){
                File f = new File(filePath);
                if(f.exists()){
                    boolean ok = f.delete();
                    System.out.println("删除文件:" + filePath + " 结果:"+ok);
                }
            }
        }
    }
}
