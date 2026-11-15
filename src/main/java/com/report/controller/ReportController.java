package com.report.controller;

import com.report.entity.ReportQuery;
import com.report.entity.ReportTask;
import com.report.entity.TaskStatusEnum;
import com.report.service.ReportExportService;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

@RestController
@RequestMapping("/report")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportExportService reportExportService;

    public ReportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @PostMapping("/export/submit")
    public String submit(@Valid @RequestBody ReportQuery query) {
        return reportExportService.createTask(query);
    }

    @GetMapping("/export/query")
    public ReportTask query(@RequestParam String taskId) {
        return reportExportService.getTaskInfo(taskId);
    }

    @GetMapping("/download")
    public void download(@RequestParam String taskId, HttpServletResponse response) {
        ReportTask task = reportExportService.getTaskInfo(taskId);
        // 业务成功状态为SUC，不再判断"2"
        if (!TaskStatusEnum.SUC.status.equals(task.getTaskStatus())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String filePath = task.getFilePath();
        if (filePath == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode(task.getTaskId() + ".xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    os.write(buf, 0, len);
                }
                os.flush();
            }
        } catch (IOException e) {
            if (isClientAbort(e)) {
                log.warn("客户端取消下载，taskId={}", taskId);
                return;
            }
            log.error("导出文件下载失败，taskId={}", taskId, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * 判断是否为客户端主动断开导致的 IO 异常（异常可能被包装在 cause 链中）。
     */
    private static boolean isClientAbort(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ClientAbortException
                    || "ClientAbortException".equals(current.getClass().getSimpleName())) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("broken pipe") || lower.contains("connection reset")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
