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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReportExportServiceImpl extends ServiceImpl<ReportTaskMapper, ReportTask> implements ReportExportService {

    private final ReportStatService reportStatService;
    private final ReportExportAsyncService reportExportAsyncService;

    @Value("${report.file.expire-day:7}")
    private int expireDay;

    public ReportExportServiceImpl(ReportStatService reportStatService, ReportExportAsyncService reportExportAsyncService) {
        this.reportStatService = reportStatService;
        this.reportExportAsyncService = reportExportAsyncService;
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

        // 交由独立异步 Bean 调用，保证 @Async 代理生效
        reportExportAsyncService.asyncExportTask(reportTask.getId(), query.getStartDate(), query.getEndDate());
        return reportTask.getTaskId();
    }

    @Override
    public ReportTask getTaskInfo(String taskId) {
        ReportTask task = lambdaQuery()
                .eq(ReportTask::getTaskId, taskId)
                .one();
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在: " + taskId);
        }
        return task;
    }
}
