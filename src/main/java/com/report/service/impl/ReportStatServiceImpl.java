package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ReportStat;
import com.report.mapper.ReportStatMapper;
import com.report.service.ReportStatService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportStatServiceImpl extends ServiceImpl<ReportStatMapper, ReportStat> implements ReportStatService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<ReportStat> listRangeDate(LocalDate start, LocalDate end) {
        // statDate 是 String 类型，参数是 LocalDate，需要格式化后比较
        String startStr = start.format(DATE_FMT);
        String endStr = end.format(DATE_FMT);
        LambdaQueryWrapper<ReportStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ReportStat::getStatDate, startStr, endStr);
        wrapper.orderByAsc(ReportStat::getStatDate);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public void calcAndSaveStat() {
        // 统计今日数据并保存（示例逻辑：无外部数据源时生成模拟数据）
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FMT);

        // 检查是否已存在今日记录
        LambdaQueryWrapper<ReportStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportStat::getStatDate, todayStr);
        Long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            System.out.println("【ReportStatJob】今日(" + todayStr + ")统计数据已存在，跳过");
            return;
        }

        // 生成模拟统计数据（实际业务中应从订单表等聚合）
        ReportStat stat = new ReportStat();
        stat.setStatDate(todayStr);
        stat.setTotalOrder(0);
        stat.setTotalAmount(BigDecimal.ZERO);
        stat.setCreateTime(LocalDateTime.now());
        baseMapper.insertStat(stat);
        System.out.println("【ReportStatJob】已保存 " + todayStr + " 统计数据");
    }
}
