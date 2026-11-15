package com.report.service;

import com.report.entity.ReportQuery;
import com.report.entity.ReportTask;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

public interface ReportExportService {

    void syncExport(LocalDate start, LocalDate end, HttpServletResponse response) throws IOException;

    //新增，给Controller调用
    String createTask(ReportQuery query);
    ReportTask getTaskInfo(String taskId);
}
