package com.report.job;

import com.report.entity.ReportTask;
import com.report.entity.TaskStatusEnum;
import com.report.mapper.ReportTaskMapper;
import com.report.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TaskTimeoutCheckJob implements Job {

    @Resource
    private ReportTaskMapper reportTaskMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final int TIMEOUT_MINUTE = 30;

    @Override
    public void execute(JobExecutionContext context) {
        log.info("==== TaskTimeoutCheckJob 超时检测执行 ====");
        List<ReportTask> timeoutList = reportTaskMapper.selectRunningTimeoutTasks(TIMEOUT_MINUTE);
        log.info("本次检测超时任务数量：{}", timeoutList.size());

        for (ReportTask task : timeoutList) {
            task.setTaskStatus(TaskStatusEnum.FAIL.status);
            task.setFailMsg("任务执行超时");

            reportTaskMapper.updateById(task);

            // 使用业务 taskId（UUID）作为缓存 key，避免暴露自增主键
            String redisKey = "report:task:" + task.getTaskId();
            String json = JsonUtil.toJson(task);
            if(json != null){
                stringRedisTemplate.opsForValue().set(redisKey, json, 1, TimeUnit.DAYS);
            }
            log.info("标记超时任务, taskId={}", task.getTaskId());
        }
    }
}
