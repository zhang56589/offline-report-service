package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ReportStat;
import com.report.mapper.ReportStatMapper;
import com.report.service.ReportStatService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportStatServiceImpl extends ServiceImpl<ReportStatMapper, ReportStat> implements ReportStatService {

    @Override
    public List<ReportStat> listRangeDate(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<ReportStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ReportStat::getStatDate, start, end);
        return baseMapper.selectList(wrapper);
    }

    //必须实现接口新增的抽象方法 calcAndSaveStat
    @Override
    public void calcAndSaveStat() {
        //业务逻辑，自己填充
    }
}
