package com.report.controller;

import com.report.entity.ReportStat;
import com.report.entity.ReportTask;
import com.report.mapper.ReportStatMapper;
import com.report.mapper.ReportTaskMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    private final ReportTaskMapper reportTaskMapper;
    private final ReportStatMapper reportStatMapper;

    public TestController(ReportTaskMapper reportTaskMapper, ReportStatMapper reportStatMapper) {
        this.reportTaskMapper = reportTaskMapper;
        this.reportStatMapper = reportStatMapper;
    }

    @GetMapping("/test/mapper")
    public String testMapper(){
        //调用你已经定义好的方法：查询超时运行任务，传参数30分钟
        List<ReportTask> timeoutList = reportTaskMapper.selectRunningTimeoutTasks(30);
        //查询过期任务
        List<ReportTask> expireList = reportTaskMapper.selectExpireTasks();
        //测试stat查询
        List<ReportStat> statList = reportStatMapper.selectByDate("2026-10-04");

        return "超时任务数量:"+timeoutList.size()+" | 过期任务数量:"+expireList.size()+" | stat数量:"+statList.size();
    }
}
