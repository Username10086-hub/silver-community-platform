package com.silver.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.silver.common.R;
import com.silver.common.entity.*;
import com.silver.common.feign.UserFeignClient;
import com.silver.order.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired private ServiceOrderMapper orderMapper;
    @Autowired private OrderVolunteerMapper ovMapper;
    @Autowired private NewsMapper newsMapper;
    @Autowired private AlertMessageMapper alertMapper;
    @Autowired private VolunteerNotifyMapper vnMapper;
    @Autowired private UserFeignClient userFeignClient;

    @PostMapping("/publish")
    public R<Long> publish(@RequestParam Long elderId,
                           @RequestParam(required = false) Long publisherId,
                           @RequestParam(required = false) Integer publisherRole,
                           @RequestParam Integer serviceType,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) String contactPhone,
                           @RequestParam(defaultValue = "1") Integer peopleCount,
                           @RequestParam(required = false) String appointmentTime,
                           @RequestParam(required = false) String remark) {
        ServiceOrder o = new ServiceOrder();
        o.setElderId(elderId);
        o.setPublisherId(publisherId != null ? publisherId : elderId);
        o.setPublisherRole(publisherRole != null ? publisherRole : 1);
        o.setServiceType(serviceType);
        o.setAddress(address);
        o.setContactPhone(contactPhone);
        o.setPeopleCount(peopleCount);
        o.setAcceptedCount(0);
        o.setStatus(0);
        o.setAuditStatus(0);
        o.setResubmitCount(0);
        if (appointmentTime != null && !appointmentTime.isEmpty()) {
            String timeStr = appointmentTime.replace(" ", "T");
            if (timeStr.length() == 16) timeStr += ":00";
            LocalDateTime t = LocalDateTime.parse(timeStr);
            if (!t.isAfter(LocalDateTime.now().plusDays(1))) {
                return R.error("预约时间需晚于当前时间1天以上");
            }
            o.setAppointmentTime(t);
        }
        o.setRemark(remark);
        o.setCreateTime(LocalDateTime.now());
        orderMapper.insert(o);
        return R.ok(o.getId());
    }

    @GetMapping("/home/pending")
    public R<List<Map<String, Object>>> homePending() {
        List<ServiceOrder> list = orderMapper.selectList(
                new QueryWrapper<ServiceOrder>()
                        .eq("audit_status", 1)
                        .eq("status", 0)
                        .apply("accepted_count < people_count")
                        .orderByDesc("create_time"));
        return R.ok(enrichOrders(list));
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) Integer auditStatus,
                                             @RequestParam(required = false) Integer serviceType,
                                             @RequestParam(required = false) Long elderId,
                                             @RequestParam(required = false) Long publisherId,
                                             @RequestParam(required = false) Long volunteerId) {
        QueryWrapper<ServiceOrder> w = new QueryWrapper<>();
        if (status != null) w.eq("status", status);
        if (auditStatus != null) w.eq("audit_status", auditStatus);
        if (serviceType != null) w.eq("service_type", serviceType);
        if (elderId != null) w.eq("elder_id", elderId);
        if (publisherId != null) w.eq("publisher_id", publisherId);
        w.orderByDesc("create_time");
        List<ServiceOrder> all = orderMapper.selectList(w);
        if (volunteerId != null) {
            List<OrderVolunteer> ovs = ovMapper.selectList(
                    new QueryWrapper<OrderVolunteer>().eq("volunteer_id", volunteerId));
            Set<Long> orderIds = ovs.stream().map(OrderVolunteer::getOrderId).collect(Collectors.toSet());
            all = all.stream().filter(o -> orderIds.contains(o.getId())).collect(Collectors.toList());
        }
        return R.ok(enrichOrders(all));
    }

    @PutMapping("/audit/{id}")
    public R<String> audit(@PathVariable Long id,
                           @RequestParam Integer auditStatus,
                           @RequestParam(required = false) String rejectReason,
                           @RequestParam Long adminId) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        if (o.getAuditStatus() != 0 && o.getAuditStatus() != 3) return R.error("工单已被处理");

        SysUser admin = userFeignClient.getUserById(adminId);
        boolean isSuper = admin != null && admin.getRole() == 5;
        if (!isSuper) {
            if (o.getReviewingAdminId() == null || !o.getReviewingAdminId().equals(adminId)) {
                return R.error("请先打开工单锁定后再审核");
            }
        }

        o.setAuditStatus(auditStatus);
        o.setReviewerId(adminId);
        o.setReviewingAdminId(null);
        if (auditStatus == 2) {
            o.setRejectReason(rejectReason);
        }
        orderMapper.updateById(o);

        // 审核通过后处理志愿者
        if (auditStatus == 1) {
            List<OrderVolunteer> ovs = ovMapper.selectList(
                    new QueryWrapper<OrderVolunteer>().eq("order_id", id));
            List<OrderVolunteer> valid = ovs.stream()
                    .filter(v -> v.getConfirmStatus() == null || v.getConfirmStatus() == 1 || v.getConfirmStatus() == 2)
                    .collect(Collectors.toList());
            int count = valid.size();
            o.setAcceptedCount(count);
            if (count >= o.getPeopleCount()) o.setStatus(1);
            else o.setStatus(0);
            orderMapper.updateById(o);
            for (OrderVolunteer v : valid) {
                notifyVolunteer(v.getVolunteerId(), id, 3, "工单已通过审核，请确认继续");
            }
        }
        return R.ok(auditStatus == 1 ? "已通过" : "已拒绝");
    }

    @PutMapping("/lock/{id}")
    public R<String> lockAudit(@PathVariable Long id, @RequestParam Long adminId) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        if (o.getAuditStatus() != 0 && o.getAuditStatus() != 3) return R.error("工单已被处理");
        SysUser admin = userFeignClient.getUserById(adminId);
        if (admin == null) return R.error("管理员不存在");
        if (admin.getRole() == 5) return R.ok("超管无需锁定");
        if (o.getReviewingAdminId() != null && !o.getReviewingAdminId().equals(adminId)) {
            return R.error("该工单正在被其他管理员审核");
        }
        o.setReviewingAdminId(adminId);
        orderMapper.updateById(o);
        return R.ok("已锁定");
    }

    @PutMapping("/unlock/{id}")
    public R<String> unlockAudit(@PathVariable Long id, @RequestParam Long adminId) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        if (o.getReviewingAdminId() != null && o.getReviewingAdminId().equals(adminId)) {
            o.setReviewingAdminId(null);
            orderMapper.updateById(o);
        }
        return R.ok("已释放");
    }

    @PutMapping("/assign/{id}")
    public R<String> assignAudit(@PathVariable Long id,
                                 @RequestParam Long adminId,
                                 @RequestParam Long targetAdminId) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        SysUser admin = userFeignClient.getUserById(adminId);
        if (admin == null || admin.getRole() != 5) return R.error("只有超级管理员可以分派");
        o.setReviewingAdminId(targetAdminId);
        orderMapper.updateById(o);
        return R.ok("已分派");
    }

    @PutMapping("/revoke/{id}")
    public R<String> revoke(@PathVariable Long id, @RequestParam Long adminId) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        SysUser admin = userFeignClient.getUserById(adminId);
        if (admin == null || admin.getRole() != 5) return R.error("只有超级管理员可以撤销");
        if (o.getStatus() == 2 || o.getStatus() == 5) return R.error("工单已结束");
        o.setStatus(5);
        o.setCancelBy(adminId);
        o.setCancelByRole(5);
        orderMapper.updateById(o);
        List<OrderVolunteer> ovs = ovMapper.selectList(
                new QueryWrapper<OrderVolunteer>().eq("order_id", id));
        for (OrderVolunteer ov : ovs) {
            notifyVolunteer(ov.getVolunteerId(), id, 2, "该工单已被管理员撤销");
        }
        return R.ok("已撤销");
    }

    // 修改：只要改了就要重新审核
    @PutMapping("/modify/{id}")
    public R<String> modify(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        if (o.getStatus() == 2 || o.getStatus() == 3 || o.getStatus() == 5) return R.error("工单已结束");

        String newApptTime = (String) body.get("appointmentTime");
        if (newApptTime != null && !newApptTime.isEmpty()) {
            String timeStr = newApptTime.replace(" ", "T");
            if (timeStr.length() == 16) timeStr += ":00";
            LocalDateTime newTime = LocalDateTime.parse(timeStr);
            if (!newTime.isAfter(LocalDateTime.now().plusDays(1))) {
                return R.error("预约时间需晚于当前时间1天以上");
            }
            o.setAppointmentTime(newTime);
        }
        if (body.get("serviceType") != null) o.setServiceType(((Number) body.get("serviceType")).intValue());
        if (body.get("address") != null) o.setAddress((String) body.get("address"));
        if (body.get("contactPhone") != null) o.setContactPhone((String) body.get("contactPhone"));
        if (body.get("peopleCount") != null) o.setPeopleCount(((Number) body.get("peopleCount")).intValue());
        if (body.get("remark") != null) o.setRemark((String) body.get("remark"));

        List<OrderVolunteer> vols = ovMapper.selectList(
                new QueryWrapper<OrderVolunteer>().eq("order_id", id));
        if (!vols.isEmpty()) {
            for (OrderVolunteer v : vols) {
                v.setConfirmStatus(1);
                ovMapper.updateById(v);
                notifyVolunteer(v.getVolunteerId(), id, 1, "您接的工单已被修改，请重新确认");
            }
        }

        o.setAuditStatus(0);
        o.setReviewingAdminId(null);
        o.setRejectReason(null);
        o.setResubmitCount((o.getResubmitCount() == null ? 0 : o.getResubmitCount()) + 1);
        orderMapper.updateById(o);
        return R.ok("已修改，等待重新审核");
    }

    @PutMapping("/resubmit/{id}")
    public R<String> resubmit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        if (o.getAuditStatus() != 2) return R.error("只有已拒绝的工单可重新提交");

        if (body.get("serviceType") != null) o.setServiceType(((Number) body.get("serviceType")).intValue());
        if (body.get("address") != null) o.setAddress((String) body.get("address"));
        if (body.get("contactPhone") != null) o.setContactPhone((String) body.get("contactPhone"));
        if (body.get("peopleCount") != null) o.setPeopleCount(((Number) body.get("peopleCount")).intValue());
        if (body.get("remark") != null) o.setRemark((String) body.get("remark"));

        String newApptTime = (String) body.get("appointmentTime");
        if (newApptTime != null && !newApptTime.isEmpty()) {
            String timeStr = newApptTime.replace(" ", "T");
            if (timeStr.length() == 16) timeStr += ":00";
            LocalDateTime t = LocalDateTime.parse(timeStr);
            if (!t.isAfter(LocalDateTime.now().plusDays(1))) {
                return R.error("预约时间需晚于当前时间1天以上");
            }
            o.setAppointmentTime(t);
        }
        o.setAuditStatus(3);
        o.setRejectReason(null);
        o.setReviewingAdminId(null);
        orderMapper.updateById(o);
        return R.ok("已重新提交审核");
    }

    @PutMapping("/cancel/{id}")
    public R<String> cancel(@PathVariable Long id,
                            @RequestParam Long userId,
                            @RequestParam Integer userRole) {
        ServiceOrder o = orderMapper.selectById(id);
        if (o == null) return R.error("工单不存在");
        if (o.getStatus() == 2 || o.getStatus() == 3 || o.getStatus() == 5) return R.error("工单已结束");

        if (o.getAuditStatus() == 0 || o.getAuditStatus() == 3) {
            o.setStatus(3);
            o.setCancelBy(userId);
            o.setCancelByRole(userRole);
            orderMapper.updateById(o);
            return R.ok("已取消");
        }

        List<OrderVolunteer> vols = ovMapper.selectList(
                new QueryWrapper<OrderVolunteer>().eq("order_id", id));
        if (!vols.isEmpty()) {
            for (OrderVolunteer v : vols) {
                notifyVolunteer(v.getVolunteerId(), id, 2, "您接的工单已被发布者取消");
            }
            ovMapper.delete(new QueryWrapper<OrderVolunteer>().eq("order_id", id));
            o.setAcceptedCount(0);
        }
        o.setStatus(3);
        o.setCancelBy(userId);
        o.setCancelByRole(userRole);
        orderMapper.updateById(o);
        return R.ok("已取消");
    }

    @PutMapping("/accept/{orderId}")
    public R<String> accept(@PathVariable Long orderId, @RequestParam Long volunteerId) {
        ServiceOrder o = orderMapper.selectById(orderId);
        if (o == null) return R.error("工单不存在");
        if (o.getAuditStatus() != 1) return R.error("工单未通过审核");
        if (o.getAcceptedCount() >= o.getPeopleCount()) return R.error("已接满");
        Long exist = ovMapper.selectCount(new QueryWrapper<OrderVolunteer>()
                .eq("order_id", orderId).eq("volunteer_id", volunteerId));
        if (exist > 0) return R.error("您已接过该工单");
        OrderVolunteer ov = new OrderVolunteer();
        ov.setOrderId(orderId);
        ov.setVolunteerId(volunteerId);
        ov.setStatus(1);
        ov.setConfirmStatus(0);
        ov.setCreateTime(LocalDateTime.now());
        ovMapper.insert(ov);
        o.setAcceptedCount(o.getAcceptedCount() + 1);
        if (o.getAcceptedCount() >= o.getPeopleCount()) o.setStatus(1);
        orderMapper.updateById(o);
        return R.ok("接单成功");
    }

    @DeleteMapping("/volunteer-cancel/{orderId}")
    public R<String> volunteerCancel(@PathVariable Long orderId, @RequestParam Long volunteerId) {
        ServiceOrder o = orderMapper.selectById(orderId);
        if (o == null) return R.error("工单不存在");
        if (o.getStatus() == 2 || o.getStatus() == 3) return R.error("工单已结束");
        OrderVolunteer ov = ovMapper.selectOne(new QueryWrapper<OrderVolunteer>()
                .eq("order_id", orderId).eq("volunteer_id", volunteerId));
        if (ov == null) return R.error("您未接该工单");
        ovMapper.deleteById(ov.getId());
        o.setAcceptedCount(Math.max(0, o.getAcceptedCount() - 1));
        if (o.getAcceptedCount() < o.getPeopleCount() && o.getStatus() == 1) {
            o.setStatus(0);
        }
        orderMapper.updateById(o);
        return R.ok("已取消接单");
    }

    @PutMapping("/volunteer-giveup/{orderId}")
    public R<String> volunteerGiveup(@PathVariable Long orderId, @RequestParam Long volunteerId) {
        OrderVolunteer ov = ovMapper.selectOne(new QueryWrapper<OrderVolunteer>()
                .eq("order_id", orderId).eq("volunteer_id", volunteerId));
        if (ov == null) return R.error("未接单");
        ovMapper.deleteById(ov.getId());
        ServiceOrder o = orderMapper.selectById(orderId);
        o.setAcceptedCount(Math.max(0, o.getAcceptedCount() - 1));
        if (o.getAcceptedCount() < o.getPeopleCount() && o.getStatus() == 1) {
            o.setStatus(0);
        }
        orderMapper.updateById(o);
        return R.ok("已放弃");
    }

    @PutMapping("/volunteer-confirm/{orderId}")
    public R<String> volunteerConfirm(@PathVariable Long orderId, @RequestParam Long volunteerId) {
        OrderVolunteer ov = ovMapper.selectOne(new QueryWrapper<OrderVolunteer>()
                .eq("order_id", orderId).eq("volunteer_id", volunteerId));
        if (ov == null) return R.error("未接单");
        ov.setConfirmStatus(2);
        ovMapper.updateById(ov);
        return R.ok("已确认继续");
    }

    @PutMapping("/complete/{orderId}")
    public R<String> complete(@PathVariable Long orderId, @RequestParam Long volunteerId) {
        OrderVolunteer ov = ovMapper.selectOne(new QueryWrapper<OrderVolunteer>()
                .eq("order_id", orderId).eq("volunteer_id", volunteerId));
        if (ov == null) return R.error("未接单");
        ov.setStatus(2);
        ov.setCompleteTime(LocalDateTime.now());
        ovMapper.updateById(ov);
        Long total = ovMapper.selectCount(new QueryWrapper<OrderVolunteer>().eq("order_id", orderId));
        Long done = ovMapper.selectCount(new QueryWrapper<OrderVolunteer>()
                .eq("order_id", orderId).eq("status", 2));
        if (total.equals(done)) {
            ServiceOrder o = orderMapper.selectById(orderId);
            o.setStatus(4);
            orderMapper.updateById(o);
        }
        return R.ok("已提交，等待确认");
    }

    @PutMapping("/confirm/{orderId}")
    public R<String> confirm(@PathVariable Long orderId) {
        ServiceOrder o = orderMapper.selectById(orderId);
        if (o == null) return R.error("工单不存在");
        if (o.getStatus() != 4) return R.error("当前状态不可确认");
        o.setStatus(2);
        orderMapper.updateById(o);
        return R.ok("已确认完成");
    }

    // 志愿者待确认修改数量
    @GetMapping("/volunteer-modify-count")
    public R<Integer> volunteerModifyCount(@RequestParam Long volunteerId) {
        List<OrderVolunteer> list = ovMapper.selectList(new QueryWrapper<OrderVolunteer>()
                .eq("volunteer_id", volunteerId).eq("confirm_status", 1));
        return R.ok(list.size());
    }

    @GetMapping("/volunteer-notify/unread")
    public R<List<Map<String, Object>>> volunteerNotifyUnread(@RequestParam Long volunteerId) {
        List<VolunteerNotify> list = vnMapper.selectList(new QueryWrapper<VolunteerNotify>()
                .eq("volunteer_id", volunteerId).eq("is_read", 0).orderByDesc("create_time"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (VolunteerNotify vn : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("notify", vn);
            ServiceOrder o = orderMapper.selectById(vn.getOrderId());
            if (o != null) {
                m.put("serviceType", o.getServiceType());
                m.put("address", o.getAddress());
            }
            result.add(m);
        }
        return R.ok(result);
    }

    @PutMapping("/volunteer-notify/read/{id}")
    public R<String> volunteerNotifyRead(@PathVariable Long id) {
        VolunteerNotify vn = vnMapper.selectById(id);
        if (vn != null) { vn.setIsRead(1); vnMapper.updateById(vn); }
        return R.ok("已读");
    }

    private void notifyVolunteer(Long volunteerId, Long orderId, Integer type, String content) {
        VolunteerNotify vn = new VolunteerNotify();
        vn.setVolunteerId(volunteerId);
        vn.setOrderId(orderId);
        vn.setType(type);
        vn.setContent(content);
        vn.setIsRead(0);
        vn.setCreateTime(LocalDateTime.now());
        vnMapper.insert(vn);
    }

    @GetMapping("/detail/{orderId}")
    public R<Map<String, Object>> detail(@PathVariable Long orderId) {
        ServiceOrder o = orderMapper.selectById(orderId);
        if (o == null) return R.error("工单不存在");
        Map<String, Object> map = new HashMap<>();
        map.put("order", o);
        try { map.put("elderName", userFeignClient.getUserNameById(o.getElderId())); }
        catch (Exception e) { map.put("elderName", "未知"); }
        List<OrderVolunteer> ovs = ovMapper.selectList(new QueryWrapper<OrderVolunteer>().eq("order_id", orderId));
        List<Map<String, Object>> vols = new ArrayList<>();
        for (OrderVolunteer ov : ovs) {
            Map<String, Object> v = new HashMap<>();
            v.put("volunteerId", ov.getVolunteerId());
            v.put("status", ov.getStatus());
            v.put("confirmStatus", ov.getConfirmStatus());
            try {
                SysUser u = userFeignClient.getUserById(ov.getVolunteerId());
                v.put("realName", u != null ? u.getRealName() : "未知");
                v.put("phone", u != null ? u.getPhone() : "");
            } catch (Exception e) {
                v.put("realName", "未知"); v.put("phone", "");
            }
            vols.add(v);
        }
        map.put("volunteers", vols);
        return R.ok(map);
    }

    @GetMapping("/news/list")
    public R<List<News>> newsList(@RequestParam(required = false) Integer type,
                                  @RequestParam(required = false) Integer limit) {
        QueryWrapper<News> w = new QueryWrapper<>();
        if (type != null) w.eq("type", type);
        w.orderByDesc("create_time");
        if (limit != null) w.last("LIMIT " + limit);
        return R.ok(newsMapper.selectList(w));
    }

    @GetMapping("/news/home")
    public R<List<News>> newsHome() {
        List<News> result = new ArrayList<>();
        for (int t = 1; t <= 3; t++) {
            List<News> list = newsMapper.selectList(new QueryWrapper<News>()
                    .eq("type", t).orderByDesc("create_time").last("LIMIT 1"));
            result.addAll(list);
        }
        return R.ok(result);
    }

    @GetMapping("/news/detail/{id}")
    public R<News> newsDetail(@PathVariable Long id) { return R.ok(newsMapper.selectById(id)); }

    @PostMapping("/news/add")
    public R<String> newsAdd(@RequestBody News n) {
        n.setCreateTime(LocalDateTime.now());
        newsMapper.insert(n);
        return R.ok("添加成功");
    }

    @PutMapping("/news/update")
    public R<String> newsUpdate(@RequestBody News n) {
        newsMapper.updateById(n);
        return R.ok("更新成功");
    }

    @DeleteMapping("/news/delete/{id}")
    public R<String> newsDelete(@PathVariable Long id) {
        newsMapper.deleteById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/alert/send")
    public R<String> alertSend(@RequestParam Long elderId, @RequestParam Long guardianId) {
        AlertMessage m = new AlertMessage();
        m.setElderId(elderId);
        m.setGuardianId(guardianId);
        m.setContent("一键求助！请及时联系！");
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        alertMapper.insert(m);
        return R.ok("已发送");
    }

    @GetMapping("/alert/unread")
    public R<List<AlertMessage>> alertUnread(@RequestParam Long guardianId) {
        return R.ok(alertMapper.selectList(new QueryWrapper<AlertMessage>()
                .eq("guardian_id", guardianId).eq("is_read", 0).orderByDesc("create_time")));
    }

    @PutMapping("/alert/read/{id}")
    public R<String> alertRead(@PathVariable Long id) {
        AlertMessage m = alertMapper.selectById(id);
        if (m != null) { m.setIsRead(1); alertMapper.updateById(m); }
        return R.ok("已读");
    }

    @GetMapping("/alert/all")
    public R<List<Map<String, Object>>> allAlerts() {
        List<AlertMessage> list = alertMapper.selectList(
                new QueryWrapper<AlertMessage>().orderByDesc("create_time"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AlertMessage m : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("alert", m);
            try {
                SysUser elder = userFeignClient.getUserById(m.getElderId());
                item.put("elderName", elder != null ? elder.getRealName() : "未知");
                item.put("elderPhone", elder != null ? elder.getPhone() : "");
            } catch (Exception e) { item.put("elderName", "未知"); }
            try {
                SysUser g = userFeignClient.getUserById(m.getGuardianId());
                item.put("guardianName", g != null ? g.getRealName() : "未知");
                item.put("guardianPhone", g != null ? g.getPhone() : "");
            } catch (Exception e) { item.put("guardianName", "未知"); }
            result.add(item);
        }
        return R.ok(result);
    }

    @GetMapping("/stats/all")
    public R<Map<String, Object>> allStats() {
        Map<String, Object> m = new HashMap<>();
        List<ServiceOrder> all = orderMapper.selectList(null);
        m.put("total", all.size());
        m.put("audit", all.stream().filter(o -> o.getAuditStatus() == 0 && o.getReviewingAdminId() == null).count());
        m.put("reviewing", all.stream().filter(o -> o.getAuditStatus() == 0 && o.getReviewingAdminId() != null).count());
        m.put("pending", all.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 0).count());
        m.put("ongoing", all.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 1).count());
        m.put("verify", all.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 4).count());
        m.put("done", all.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 2).count());
        m.put("cancel", all.stream().filter(o -> o.getStatus() == 3).count());
        m.put("reject", all.stream().filter(o -> o.getAuditStatus() == 2).count());
        m.put("revoke", all.stream().filter(o -> o.getStatus() == 5).count());
        return R.ok(m);
    }

    @GetMapping("/stats/user/{userId}")
    public R<Map<String, Object>> userStats(@PathVariable Long userId) {
        Map<String, Object> m = new HashMap<>();
        SysUser user = null;
        try { user = userFeignClient.getUserById(userId); } catch (Exception e) {}
        Integer role = user != null ? user.getRole() : null;
        if (role != null && role == 2) {
            List<OrderVolunteer> ovs = ovMapper.selectList(
                    new QueryWrapper<OrderVolunteer>().eq("volunteer_id", userId));
            List<Long> orderIds = ovs.stream().map(OrderVolunteer::getOrderId).collect(Collectors.toList());
            if (orderIds.isEmpty()) {
                m.put("total", 0); m.put("waitTeam", 0); m.put("ongoing", 0); m.put("verify", 0); m.put("done", 0); m.put("cancel", 0);
                return R.ok(m);
            }
            List<ServiceOrder> orders = orderMapper.selectBatchIds(orderIds);
            m.put("total", orders.size());
            m.put("waitTeam", orders.stream().filter(o -> o.getStatus() == 0).count());
            m.put("ongoing", orders.stream().filter(o -> o.getStatus() == 1).count());
            m.put("verify", orders.stream().filter(o -> o.getStatus() == 4).count());
            m.put("done", orders.stream().filter(o -> o.getStatus() == 2).count());
            m.put("cancel", orders.stream().filter(o -> o.getStatus() == 3).count());
        } else {
            List<ServiceOrder> orders = orderMapper.selectList(
                    new QueryWrapper<ServiceOrder>()
                            .and(w -> w.eq("publisher_id", userId).or().eq("elder_id", userId)));
            m.put("total", orders.size());
            m.put("audit", orders.stream().filter(o -> o.getAuditStatus() == 0).count());
            m.put("pending", orders.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 0).count());
            m.put("ongoing", orders.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 1).count());
            m.put("verify", orders.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 4).count());
            m.put("done", orders.stream().filter(o -> o.getAuditStatus() == 1 && o.getStatus() == 2).count());
            m.put("cancel", orders.stream().filter(o -> o.getStatus() == 3).count());
            m.put("reject", orders.stream().filter(o -> o.getAuditStatus() == 2).count());
        }
        return R.ok(m);
    }

    @GetMapping("/list/byUser")
    public R<List<Map<String, Object>>> listByUser(@RequestParam Long userId,
                                                   @RequestParam(required = false) Integer role,
                                                   @RequestParam(required = false) String category) {
        QueryWrapper<ServiceOrder> w = new QueryWrapper<>();
        if (role != null && role == 2) {
            List<OrderVolunteer> ovs = ovMapper.selectList(
                    new QueryWrapper<OrderVolunteer>().eq("volunteer_id", userId));
            List<Long> orderIds = ovs.stream().map(OrderVolunteer::getOrderId).collect(Collectors.toList());
            if (orderIds.isEmpty()) return R.ok(new ArrayList<>());
            w.in("id", orderIds);
            if ("ongoing".equals(category)) w.eq("status", 1);
            else if ("verify".equals(category)) w.eq("status", 4);
            else if ("done".equals(category)) w.eq("status", 2);
            else if ("cancel".equals(category)) w.eq("status", 3);
        } else {
            w.and(w2 -> w2.eq("publisher_id", userId).or().eq("elder_id", userId));
            if ("audit".equals(category)) w.eq("audit_status", 0);
            else if ("pending".equals(category)) { w.eq("audit_status", 1); w.eq("status", 0); }
            else if ("ongoing".equals(category)) { w.eq("audit_status", 1); w.eq("status", 1); }
            else if ("verify".equals(category)) { w.eq("audit_status", 1); w.eq("status", 4); }
            else if ("done".equals(category)) { w.eq("audit_status", 1); w.eq("status", 2); }
            else if ("cancel".equals(category)) w.eq("status", 3);
            else if ("reject".equals(category)) w.eq("audit_status", 2);
        }
        w.orderByDesc("create_time");
        return R.ok(enrichOrders(orderMapper.selectList(w)));
    }

    private List<Map<String, Object>> enrichOrders(List<ServiceOrder> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServiceOrder o : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("order", o);
            try { m.put("elderName", userFeignClient.getUserNameById(o.getElderId())); }
            catch (Exception e) { m.put("elderName", "未知"); }
            result.add(m);
        }
        return result;
    }
}