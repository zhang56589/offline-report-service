package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ReportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportTaskMapper extends BaseMapper<ReportTask> {

    ReportTask selectByTaskId(@Param("taskId") String taskId);

    List<ReportTask> selectRunningTimeoutTasks(@Param("timeoutMinute") int timeoutMinute);

    List<ReportTask> selectExpireTasks();

    /** XML 中定义的自定义插入（与 BaseMapper.insert 区分） */
    int insertTask(ReportTask reportTask);

    /** XML 中定义的自定义更新（与 BaseMapper.updateById 区分） */
    int updateTask(ReportTask reportTask);
}
