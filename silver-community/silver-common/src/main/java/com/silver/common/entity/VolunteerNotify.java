package com.silver.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("volunteer_notify")
public class VolunteerNotify {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long volunteerId;
    private Long orderId;
    private Integer type; // 1被修改 2被取消 3重新审核通过
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;
}