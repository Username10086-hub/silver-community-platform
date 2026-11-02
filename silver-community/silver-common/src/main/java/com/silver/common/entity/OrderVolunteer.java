package com.silver.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_volunteer")
public class OrderVolunteer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long volunteerId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
    private Integer confirmStatus;
}