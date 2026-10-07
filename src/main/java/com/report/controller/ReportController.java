package com.report.controller;

import com.report.entity.ReportQuery;
import com.report.entity.ReportTask;
import com.report.service.ReportExportService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportExportService reportExportService;

    public ReportController(ReportExportService reportExportService) {
        this.reportExportService = reportExportService;
    }

    @PostMapping("/export/submit")
    public String submit(@RequestBody ReportQuery query) {
        return reportExportService.createTask(query);
    }

    @GetMapping("/export/query")
    public Object query(@RequestParam String taskId) {
        return reportExportService.getTaskInfo(taskId);
    }

    @GetMapping("/download")
    public void download(@RequestParam String taskId, HttpServletResponse response) {
        Object obj = reportExportService.getTaskInfo(taskId);
        if (!(obj instanceof ReportTask)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        ReportTask task = (ReportTask) obj;
        // 业务成功状态为SUC，不再判断"2"
        if (!"SUC".equals(task.getTaskStatus())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        File file = new File(task.getFilePath());
        if (!file.exists() || !file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode(task.getId() + ".xlsx", "UTF-8");
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
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
