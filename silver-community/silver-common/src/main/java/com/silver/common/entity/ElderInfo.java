package com.silver.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("elder_info")
public class ElderInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderId;
    private Integer livingStatus;
    private Integer mobility;
    private String chronicDisease;
    private String dietaryRestriction;
    private String emergencyContact;
    private LocalDateTime createTime;
}