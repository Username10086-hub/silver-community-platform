-- ==========================================================
-- 银发无忧社区公益养老平台 完整数据库初始化脚本
-- 数据库: MariaDB 10.11 / MySQL 8.0+
-- 字符集: utf8mb4
-- ==========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------
-- 1. 用户表 (sys_user)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '登录密码',
  `role` tinyint(4) NOT NULL COMMENT '角色：1老人 2志愿者 3普通管理员 4子女 5超级管理员',
  `age` int(11) DEFAULT NULL COMMENT '年龄',
  `gender` tinyint(4) DEFAULT NULL COMMENT '性别：1男 2女',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `address` varchar(200) DEFAULT NULL COMMENT '家庭地址',
  `status` tinyint(4) DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------------------------------------
-- 2. 老人扩展档案表 (elder_info)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `elder_info`;
CREATE TABLE `elder_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `elder_id` bigint(20) NOT NULL COMMENT '关联sys_user.id',
  `living_status` tinyint(4) DEFAULT NULL COMMENT '居住状态：1独居 2与子女同住 3夫妻同住',
  `mobility` tinyint(4) DEFAULT NULL COMMENT '行动能力：1自理 2半自理 3失能',
  `chronic_disease` varchar(500) DEFAULT NULL COMMENT '慢性病史',
  `dietary_restriction` varchar(500) DEFAULT NULL COMMENT '饮食禁忌',
  `emergency_contact` varchar(20) DEFAULT NULL COMMENT '紧急联系人电话',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_elder_id` (`elder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人扩展档案';

-- ----------------------------------------------------------
-- 3. 老人-子女绑定关系表 (elder_guardian)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `elder_guardian`;
CREATE TABLE `elder_guardian` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `elder_id` bigint(20) NOT NULL COMMENT '老人ID',
  `guardian_id` bigint(20) NOT NULL COMMENT '子女ID',
  `relation` varchar(20) DEFAULT NULL COMMENT '关系：儿子/女儿',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_elder_guardian` (`elder_id`, `guardian_id`),
  KEY `idx_guardian` (`guardian_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人-监护人关系表';

-- ----------------------------------------------------------
-- 4. 服务工单表 (service_order)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `service_order`;
CREATE TABLE `service_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `elder_id` bigint(20) NOT NULL COMMENT '工单关联的老人ID',
  `publisher_id` bigint(20) DEFAULT NULL COMMENT '实际发布者ID（可能是子女）',
  `publisher_role` tinyint(4) DEFAULT NULL COMMENT '发布者角色：1老人 4子女',
  `service_type` tinyint(4) NOT NULL COMMENT '服务类型：1陪护 2助老帮扶 3社区帮扶',
  `address` varchar(200) DEFAULT NULL COMMENT '服务地址',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `people_count` int(11) DEFAULT 1 COMMENT '需要志愿者人数',
  `accepted_count` int(11) DEFAULT 0 COMMENT '已接单人数',
  `status` tinyint(4) DEFAULT 0 COMMENT '状态：0待接单 1进行中 2已完成 3已取消 4待验收 5已撤销',
  `audit_status` tinyint(4) DEFAULT 0 COMMENT '审核状态：0待审核 1已通过 2已拒绝 3再审核',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝理由',
  `reviewer_id` bigint(20) DEFAULT NULL COMMENT '审核管理员ID',
  `reviewing_admin_id` bigint(20) DEFAULT NULL COMMENT '正在审核中的管理员ID',
  `cancel_by` bigint(20) DEFAULT NULL COMMENT '取消者ID',
  `cancel_by_role` tinyint(4) DEFAULT NULL COMMENT '取消者角色',
  `resubmit_count` int(11) DEFAULT 0 COMMENT '重新提交次数',
  `appointment_time` datetime DEFAULT NULL COMMENT '预约服务时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '需求描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务工单表';

-- ----------------------------------------------------------
-- 5. 工单-志愿者关系表 (order_volunteer) 支持多人接单
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `order_volunteer`;
CREATE TABLE `order_volunteer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(20) NOT NULL,
  `volunteer_id` bigint(20) NOT NULL,
  `status` tinyint(4) DEFAULT 1 COMMENT '1进行中 2已完成',
  `confirm_status` tinyint(4) DEFAULT 0 COMMENT '0正常 1修改待确认 2已确认继续 3已放弃',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `complete_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_vol` (`order_id`, `volunteer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单-志愿者关系表';

-- ----------------------------------------------------------
-- 6. 志愿者通知表 (volunteer_notify)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `volunteer_notify`;
CREATE TABLE `volunteer_notify` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `volunteer_id` bigint(20) NOT NULL,
  `order_id` bigint(20) NOT NULL,
  `type` tinyint(4) NOT NULL COMMENT '1工单被修改 2工单被取消 3重新审核通过',
  `content` varchar(300) DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者通知表';

-- ----------------------------------------------------------
-- 7. 健康记录表 (health_record)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `health_record`;
CREATE TABLE `health_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `elder_id` bigint(20) NOT NULL,
  `blood_pressure` varchar(20) DEFAULT NULL,
  `blood_sugar` varchar(20) DEFAULT NULL,
  `heart_rate` int(11) DEFAULT NULL,
  `record_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康记录表';

-- ----------------------------------------------------------
-- 8. 紧急求助告警表 (alert_message)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `alert_message`;
CREATE TABLE `alert_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `elder_id` bigint(20) NOT NULL,
  `guardian_id` bigint(20) NOT NULL,
  `content` varchar(200) DEFAULT '一键求助！请及时联系！',
  `is_read` tinyint(1) DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求助消息表';

-- ----------------------------------------------------------
-- 9. 新闻公告表 (news)
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `news`;
CREATE TABLE `news` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `type` tinyint(4) DEFAULT 1 COMMENT '1科普 2通知 3公益',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻公告表';


-- ==========================================================
-- 初始测试数据插入
-- ==========================================================

-- 1. 插入超级管理员 (admin / 123456)
INSERT INTO `sys_user` (`username`, `password`, `role`, `real_name`, `phone`, `status`) 
VALUES ('admin', '123456', 5, '超级管理员', '13800000000', 1);

-- 2. 插入普通管理员 (admin2 / 123456)
INSERT INTO `sys_user` (`username`, `password`, `role`, `real_name`, `phone`, `status`) 
VALUES ('admin2', '123456', 3, '普通管理员', '13800000002', 1);

-- 3. 插入子女测试账号 (zhangzi / 123456)
INSERT INTO `sys_user` (`id`, `username`, `password`, `role`, `real_name`, `phone`, `status`) 
VALUES (3, 'zhangzi', '123456', 4, '张小明', '13900004444', 1);

-- 4. 插入老人测试账号 (zhanglao / 123456)
INSERT INTO `sys_user` (`id`, `username`, `password`, `role`, `real_name`, `phone`, `address`, `status`) 
VALUES (4, 'zhanglao', '123456', 1, '张大爷', '13800001111', '阳光社区1栋101', 1);

-- 5. 插入志愿者测试账号 (wangzhi / 123456)
INSERT INTO `sys_user` (`id`, `username`, `password`, `role`, `real_name`, `phone`, `status`) 
VALUES (5, 'wangzhi', '123456', 2, '王志强', '13800003333', 1);

-- 6. 为张大爷建立老人档案
INSERT INTO `elder_info` (`elder_id`, `living_status`, `mobility`, `chronic_disease`, `dietary_restriction`, `emergency_contact`) 
VALUES (4, 1, 2, '高血压', '忌辛辣', '13900004444');

-- 7. 绑定张小明(子女)和张大爷(老人)的关系
INSERT INTO `elder_guardian` (`elder_id`, `guardian_id`, `relation`) 
VALUES (4, 3, '儿子');

-- 8. 插入新闻测试数据 (科普、通知、公益各一条)
INSERT INTO `news` (`title`, `content`, `type`) VALUES 
('如何预防老年人跌倒', '跌倒是老年人常见的意外伤害。建议家里地面保持干燥，浴室加装扶手。', 1),
('社区免费体检通知', '本月15号上午8点，社区服务中心为60岁以上老人提供免费体检。', 2),
('重阳节敬老活动', '10月4日重阳节，社区将举办敬老爱老活动，欢迎志愿者报名参与。', 3);

SET FOREIGN_KEY_CHECKS = 1;