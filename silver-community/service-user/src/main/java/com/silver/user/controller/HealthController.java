package com.silver.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.silver.common.R;
import com.silver.common.entity.HealthRecord;
import com.silver.user.mapper.HealthRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/user/health")
public class HealthController {

    @Autowired
    private HealthRecordMapper healthRecordMapper;

    @PostMapping("/add")
    public R<String> addRecord(@RequestParam Long elderId,
                               @RequestParam(required = false) String bloodPressure,
                               @RequestParam(required = false) String bloodSugar,
                               @RequestParam(required = false) Integer heartRate) {
        HealthRecord r = new HealthRecord();
        r.setElderId(elderId);
        r.setBloodPressure(bloodPressure);
        r.setBloodSugar(bloodSugar);
        r.setHeartRate(heartRate);
        r.setRecordTime(LocalDateTime.now());
        healthRecordMapper.insert(r);
        return R.ok("上报成功");
    }

    @GetMapping("/list")
    public R<List<HealthRecord>> list(@RequestParam Long elderId,
                                      @RequestParam(required = false) Integer limit) {
        QueryWrapper<HealthRecord> w = new QueryWrapper<HealthRecord>()
                .eq("elder_id", elderId).orderByDesc("record_time");
        if (limit != null) w.last("LIMIT " + limit);
        return R.ok(healthRecordMapper.selectList(w));
    }
}