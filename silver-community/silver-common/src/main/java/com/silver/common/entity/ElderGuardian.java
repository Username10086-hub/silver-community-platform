package com.silver.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("elder_guardian")
public class ElderGuardian {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderId;
    private Long guardianId;
    private String relation;
    private LocalDateTime createTime;
}