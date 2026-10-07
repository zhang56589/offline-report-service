package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ReportStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReportStatMapper extends BaseMapper<ReportStat> {

    void insertStat(ReportStat reportStat);

    List<ReportStat> selectByDate(@Param("statDate") String statDate);

    // 改为 LocalDate，和Service接口对齐
    List<ReportStat> selectByDateRange(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}
