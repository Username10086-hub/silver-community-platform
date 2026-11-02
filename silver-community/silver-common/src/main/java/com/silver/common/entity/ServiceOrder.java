package com.silver.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_order")
public class ServiceOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderId;
    private Long publisherId;
    private Integer publisherRole;
    private Integer serviceType;
    private String address;
    private Integer peopleCount;
    private Integer acceptedCount;
    private Long volunteerId;
    private Integer status;
    private Integer auditStatus;
    private LocalDateTime appointmentTime;
    private String remark;
    private LocalDateTime createTime;
    private String contactPhone;
    private String rejectReason;
    private Long reviewerId;
    private Long reviewingAdminId;
    private Long cancelBy;
    private Integer cancelByRole;
    private Integer resubmitCount;
}