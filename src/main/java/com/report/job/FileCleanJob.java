package com.report.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.report.entity.ReportTask;
import com.report.mapper.ReportTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class FileCleanJob implements Job {

    @Autowired
    private ReportTaskMapper reportTaskMapper;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("==== FileCleanJob 定时清理任务执行 ====");
        // 查询已过期的任务
        List<ReportTask> expireList = reportTaskMapper.selectExpireTasks();
        log.info("过期任务数量：{}", expireList.size());
        for (ReportTask task : expireList) {
            String filePath = task.getFilePath();
            if (filePath == null) {
                continue;
            }
            File f = new File(filePath);
            if (!f.exists()) {
                log.info("文件不存在，跳过清理, taskId={}, path={}", task.getTaskId(), filePath);
                clearFilePath(task);
                continue;
            }
            if (f.delete()) {
                log.info("删除文件成功, taskId={}, path={}", task.getTaskId(), filePath);
                clearFilePath(task);
            } else {
                log.error("删除文件失败, taskId={}, path={}", task.getTaskId(), filePath);
            }
        }
    }

    /**
     * 清空数据库中该任务的 file_path。
     * updateById 默认忽略 null 字段，因此使用 LambdaUpdateWrapper 显式置空。
     */
    private void clearFilePath(ReportTask task) {
        LambdaUpdateWrapper<ReportTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReportTask::getId, task.getId()).set(ReportTask::getFilePath, null);
        int rows = reportTaskMapper.update(null, wrapper);
        if (rows == 0) {
            log.warn("清空 file_path 失败, taskId={}", task.getTaskId());
        }
    }
}
