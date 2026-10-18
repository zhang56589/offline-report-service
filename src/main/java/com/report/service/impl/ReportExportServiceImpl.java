package com.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ReportQuery;
import com.report.entity.ReportStat;
import com.report.entity.ReportTask;
import com.report.entity.TaskStatusEnum;
import com.report.mapper.ReportTaskMapper;
import com.report.service.ReportExportService;
import com.report.service.ReportStatService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ReportExportServiceImpl extends ServiceImpl<ReportTaskMapper, ReportTask> implements ReportExportService {

    private final ReportStatService reportStatService;
    private final RedissonClient redissonClient;

    /** 注入自身代理，解决 @Async 自调用不生效问题 */
    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ReportExportService self;

    @Value("${report.file.expire-day:7}")
    private int expireDay;

    public ReportExportServiceImpl(ReportStatService reportStatService, RedissonClient redissonClient) {
        this.reportStatService = reportStatService;
        this.redissonClient = redissonClient;
    }

    private static final String EXPORT_LOCK = "report:export:lock:";

    @Override
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

            System.out.println("【导出】start=" + start + " end=" + end);
            List<ReportStat> data = reportStatService.listRangeDate(start, end);
            System.out.println("【导出】查询行数：" + data.size());

            String filePath = System.getProperty("user.dir") + File.separator + "export" + File.separator + taskId + ".xlsx";
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
            e.printStackTrace();
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

    @Override
    public void syncExport(LocalDate start, LocalDate end, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode("统计报表", StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
        List<ReportStat> list = reportStatService.listRangeDate(start, end);
        EasyExcel.write(response.getOutputStream(), ReportStat.class).sheet("统计报表").doWrite(list);
    }

    @Override
    public String createTask(ReportQuery query) {
        ReportTask reportTask = new ReportTask();
        reportTask.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        reportTask.setReportType("STAT");
        reportTask.setTaskStatus(TaskStatusEnum.WAIT.status);
        reportTask.setTotalCount(0L);
        reportTask.setFinishCount(0L);
        reportTask.setCreateTime(LocalDateTime.now());
        reportTask.setUpdateTime(LocalDateTime.now());
        reportTask.setExpireTime(LocalDateTime.now().plusDays(expireDay));
        save(reportTask);

        // 通过自身代理调用，使 @Async 生效
        self.asyncExportTask(reportTask.getId(), query.getStartDate(), query.getEndDate());
        return reportTask.getTaskId();
    }

    @Override
    public Object getTaskInfo(String taskId) {
        return lambdaQuery()
                .eq(ReportTask::getTaskId, taskId)
                .one();
    }
}
