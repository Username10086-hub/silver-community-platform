package com.silver.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alert_message")
public class AlertMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderId;
    private Long guardianId;   // 接收求助的子女ID
    private String content;
    private Integer isRead;    // 0未读 1已读
    private LocalDateTime createTime;
}