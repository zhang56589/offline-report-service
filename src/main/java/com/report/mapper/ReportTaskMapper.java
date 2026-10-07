package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ReportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ReportTaskMapper extends BaseMapper<ReportTask> {
    // 只保留方法签名，删掉所有@Select注解！SQL写在XML
    ReportTask selectByTaskId(@Param("taskId") String taskId);

    List<ReportTask> selectRunningTimeoutTasks(@Param("timeoutMinute") int timeoutMinute);

    List<ReportTask> selectExpireTasks();
}
