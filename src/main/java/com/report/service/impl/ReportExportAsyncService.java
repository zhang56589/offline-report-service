package com.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ReportStat;
import com.report.entity.ReportTask;
import com.report.entity.TaskStatusEnum;
import com.report.mapper.ReportTaskMapper;
import com.report.service.ReportStatService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReportExportAsyncService extends ServiceImpl<ReportTaskMapper, ReportTask> {

    private static final String EXPORT_LOCK = "report:export:lock:";

    private final ReportStatService reportStatService;
    private final RedissonClient redissonClient;

    public ReportExportAsyncService(ReportStatService reportStatService, RedissonClient redissonClient) {
        this.reportStatService = reportStatService;
        this.redissonClient = redissonClient;
    }

    @Async("reportExportPool")
    public void asyncExportTask(Long taskId, LocalDate start, LocalDate end) {
        RLock lock = redissonClient.getLock(EXPORT_LOCK + taskId);
        try {
            if (!lock.tryLock(3, 60, TimeUnit.SECONDS)) {
                return;
            }
            ReportTask task = getById(taskId);
            if (task == null) {
                return;
            }
            // 统一状态为 RUN（执行中）
            task.setTaskStatus(TaskStatusEnum.RUN.status);
            updateById(task);

            log.info("【导出】start={} end={}", start, end);
            List<ReportStat> data = reportStatService.listRangeDate(start, end);
            log.info("【导出】查询行数：{}", data.size());

            String filePath = System.getProperty("user.dir") + File.separator + "export" + File.separator + task.getTaskId() + ".xlsx";
            File dir = new File(System.getProperty("user.dir") + File.separator + "export");
            dir.mkdirs();

            EasyExcel.write(filePath, ReportStat.class).sheet("统计报表").doWrite(data);

            task.setTaskStatus(TaskStatusEnum.SUC.status);
            task.setFilePath(filePath);
            task.setTotalCount((long) data.size());
            task.setFinishCount((long) data.size());
            task.setUpdateTime(LocalDateTime.now());
            updateById(task);

        } catch (Exception e) {
            log.error("任务导出失败, taskId: {}", taskId, e);
            ReportTask task = getById(taskId);
            if (task != null) {
                task.setTaskStatus(TaskStatusEnum.FAIL.status);
                task.setFailMsg(e.getMessage());
                task.setUpdateTime(LocalDateTime.now());
                updateById(task);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
