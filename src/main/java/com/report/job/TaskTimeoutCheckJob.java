package com.report.job;

import com.report.entity.ReportTask;
import com.report.entity.TaskStatusEnum;
import com.report.mapper.ReportTaskMapper;
import com.report.util.JsonUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;

@Component
public class TaskTimeoutCheckJob implements Job {

    @Resource
    private ReportTaskMapper reportTaskMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final int TIMEOUT_MINUTE = 30;

    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("==== TaskTimeoutCheckJob 超时检测执行 ====");
        List<ReportTask> timeoutList = reportTaskMapper.selectRunningTimeoutTasks(TIMEOUT_MINUTE);
        System.out.println("本次检测超时任务数量：" + timeoutList.size());

        for (ReportTask task : timeoutList) {
            task.setTaskStatus(TaskStatusEnum.FAIL.name());
            task.setFailMsg("任务执行超时");

            reportTaskMapper.updateById(task);

            //临时使用数据库主键id
            String redisKey = "report:task:" + task.getId();
            String json = JsonUtil.toJson(task);
            if(json != null){
                stringRedisTemplate.opsForValue().set(redisKey, json);
            }
            System.out.println("标记超时任务, id=" + task.getId());
        }
    }
}
