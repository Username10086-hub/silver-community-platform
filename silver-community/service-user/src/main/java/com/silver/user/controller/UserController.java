package com.silver.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.silver.common.R;
import com.silver.common.dto.RegisterDTO;
import com.silver.common.entity.ElderGuardian;
import com.silver.common.entity.ElderInfo;
import com.silver.common.entity.SysUser;
import com.silver.user.mapper.ElderGuardianMapper;
import com.silver.user.mapper.ElderInfoMapper;
import com.silver.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private ElderInfoMapper elderInfoMapper;
    @Autowired
    private ElderGuardianMapper elderGuardianMapper;

    // ==================== 基础接口 ====================

    @GetMapping("/hello")
    public String hello() {
        return "Hello! service-user is running.";
    }

    @GetMapping("/getName/{userId}")
    public String getUserNameById(@PathVariable("userId") Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知用户";
    }

    @GetMapping("/info/{id}")
    public SysUser getUserById(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) user.setPassword(null);
        return user;
    }

    @GetMapping("/getInfo/{id}")
    public R<SysUser> getUserInfo(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
            return R.ok(user);
        }
        return R.error("用户不存在");
    }

    // ==================== 登录（支持 ID 或用户名） ====================

    @PostMapping("/login")
    public R<SysUser> login(@RequestParam String account, @RequestParam String password) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("password", password);
        if (account != null && account.matches("\\d+")) {
            wrapper.and(w -> w.eq("id", Long.parseLong(account)).or().eq("username", account));
        } else {
            wrapper.eq("username", account);
        }
        SysUser user = sysUserMapper.selectOne(wrapper);
        if (user == null) return R.error("账号或密码错误");
        if (user.getStatus() != null && user.getStatus() == 0) {
            return R.error("账号已被禁用，请联系管理员");
        }
        user.setPassword(null);
        return R.ok(user);
    }

    // ==================== 注册 ====================

    @PostMapping("/register")
    public R<Long> register(@RequestBody RegisterDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return R.error("用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 6) {
            return R.error("密码长度不能少于6位");
        }
        if (dto.getRole() == null || (dto.getRole() != 1 && dto.getRole() != 2 && dto.getRole() != 4)) {
            return R.error("请选择有效的角色类型");
        }

        SysUser exist = sysUserMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", dto.getUsername())
        );
        if (exist != null) return R.error("用户名已被注册，请换一个");

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(dto.getPassword());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setAge(dto.getAge());
        user.setGender(dto.getGender());
        user.setAddress(dto.getAddress());
        user.setStatus(1);
        sysUserMapper.insert(user);

        if (dto.getRole() == 1) {
            ElderInfo info = new ElderInfo();
            info.setElderId(user.getId());
            info.setLivingStatus(dto.getLivingStatus());
            info.setMobility(dto.getMobility());
            info.setChronicDisease(dto.getChronicDisease());
            info.setDietaryRestriction(dto.getDietaryRestriction());
            info.setEmergencyContact(dto.getEmergencyPhone());
            info.setCreateTime(LocalDateTime.now());
            elderInfoMapper.insert(info);

            if (dto.getCurrentGuardianId() != null) {
                bindElderToGuardian(user.getId(), dto.getCurrentGuardianId(), "子女");
            }

            if (dto.getGuardianBindings() != null) {
                for (RegisterDTO.GuardianBinding gb : dto.getGuardianBindings()) {
                    if (gb.getGuardianId() == null) continue;
                    SysUser guardian = sysUserMapper.selectById(gb.getGuardianId());
                    if (guardian != null && guardian.getRole() == 4
                            && guardian.getRealName().equals(gb.getGuardianName())) {
                        bindElderToGuardian(user.getId(), guardian.getId(), "子女");
                    }
                }
            }
        }
        return R.ok(user.getId());
    }

    private void bindElderToGuardian(Long elderId, Long guardianId, String relation) {
        Long count = elderGuardianMapper.selectCount(
                new QueryWrapper<ElderGuardian>()
                        .eq("elder_id", elderId).eq("guardian_id", guardianId)
        );
        if (count == 0) {
            ElderGuardian eg = new ElderGuardian();
            eg.setElderId(elderId);
            eg.setGuardianId(guardianId);
            eg.setRelation(relation);
            eg.setCreateTime(LocalDateTime.now());
            elderGuardianMapper.insert(eg);
        }
    }

    // ==================== 子女绑定管理 ====================

    /** 子女查询自己绑定的所有老人 */
    @GetMapping("/getBindElderList")
    public R<List<SysUser>> getBindElderList(@RequestParam Long guardianId) {
        List<ElderGuardian> rels = elderGuardianMapper.selectList(
                new QueryWrapper<ElderGuardian>().eq("guardian_id", guardianId)
        );
        if (rels.isEmpty()) return R.ok(new ArrayList<>());
        List<Long> elderIds = rels.stream().map(ElderGuardian::getElderId).collect(Collectors.toList());
        List<SysUser> elders = sysUserMapper.selectBatchIds(elderIds);
        elders.forEach(u -> u.setPassword(null));
        return R.ok(elders);
    }

    /** 兼容旧接口名 */
    @GetMapping("/elders")
    public R<List<SysUser>> getEldersByGuardian(@RequestParam Long guardianId) {
        return getBindElderList(guardianId);
    }

    /** 子女解绑老人 */
    @DeleteMapping("/unbind")
    public R<String> unbind(@RequestParam Long guardianId, @RequestParam Long elderId) {
        elderGuardianMapper.delete(
                new QueryWrapper<ElderGuardian>()
                        .eq("guardian_id", guardianId).eq("elder_id", elderId)
        );
        return R.ok("已解除绑定");
    }

    /** 查询某位老人的所有子女 */
    @GetMapping("/getGuardiansByElder")
    public R<List<SysUser>> getGuardiansByElder(@RequestParam Long elderId) {
        List<ElderGuardian> rels = elderGuardianMapper.selectList(
                new QueryWrapper<ElderGuardian>().eq("elder_id", elderId)
        );
        if (rels.isEmpty()) return R.ok(new ArrayList<>());
        List<Long> guardianIds = rels.stream().map(ElderGuardian::getGuardianId).collect(Collectors.toList());
        List<SysUser> guardians = sysUserMapper.selectBatchIds(guardianIds);
        guardians.forEach(u -> u.setPassword(null));
        return R.ok(guardians);
    }

    /** 查询某位老人的扩展档案 */
    @GetMapping("/elderInfo/{elderId}")
    public R<ElderInfo> getElderInfo(@PathVariable Long elderId) {
        ElderInfo info = elderInfoMapper.selectOne(
                new QueryWrapper<ElderInfo>().eq("elder_id", elderId)
        );
        return R.ok(info);
    }

    /** 老人主动绑定子女 */
    @PostMapping("/bindGuardian")
    public R<String> bindGuardian(@RequestParam Long elderId,
                                  @RequestParam Long guardianId,
                                  @RequestParam String guardianName) {
        SysUser guardian = sysUserMapper.selectById(guardianId);
        if (guardian == null) return R.error("该子女账号不存在");
        if (guardian.getRole() != 4) return R.error("该账号不是子女角色");
        if (!guardian.getRealName().equals(guardianName)) return R.error("姓名与ID不匹配");

        bindElderToGuardian(elderId, guardianId, "子女");
        return R.ok("绑定成功");
    }

    // ==================== 个人信息修改 ====================

    /** 修改个人信息（不允许改密码和角色） */
    @PutMapping("/update")
    public R<String> updateUser(@RequestBody SysUser user) {
        if (user.getId() == null) return R.error("缺少用户ID");
        SysUser exist = sysUserMapper.selectById(user.getId());
        if (exist == null) return R.error("用户不存在");

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setRealName(user.getRealName());
        update.setPhone(user.getPhone());
        update.setAge(user.getAge());
        update.setGender(user.getGender());
        update.setAddress(user.getAddress());
        sysUserMapper.updateById(update);
        return R.ok("修改成功");
    }

    /** 老人更新扩展档案 */
    @PutMapping("/updateElderInfo")
    public R<String> updateElderInfo(@RequestBody ElderInfo info) {
        if (info.getElderId() == null) return R.error("缺少老人ID");
        ElderInfo exist = elderInfoMapper.selectOne(
                new QueryWrapper<ElderInfo>().eq("elder_id", info.getElderId())
        );
        if (exist == null) {
            info.setCreateTime(LocalDateTime.now());
            elderInfoMapper.insert(info);
        } else {
            info.setId(exist.getId());
            elderInfoMapper.updateById(info);
        }
        return R.ok("档案已更新");
    }

    // ==================== 管理员功能 ====================

    /** 用户列表（支持按角色、状态过滤） */
    @GetMapping("/list")
    public R<List<SysUser>> listUsers(@RequestParam(required = false) Integer role,
                                      @RequestParam(required = false) Integer status) {
        QueryWrapper<SysUser> w = new QueryWrapper<>();
        if (role != null) w.eq("role", role);
        if (status != null) w.eq("status", status);
        w.orderByDesc("id");
        List<SysUser> users = sysUserMapper.selectList(w);
        users.forEach(u -> u.setPassword(null));
        return R.ok(users);
    }

    /** 启用/禁用 */
    @PutMapping("/toggle/{id}")
    public R<String> toggleStatus(@PathVariable Long id) {
        SysUser u = sysUserMapper.selectById(id);
        if (u == null) return R.error("用户不存在");
        u.setStatus(u.getStatus() != null && u.getStatus() == 1 ? 0 : 1);
        sysUserMapper.updateById(u);
        return R.ok(u.getStatus() == 1 ? "已启用" : "已禁用");
    }

    /** 用户详情（含工单统计、绑定信息） */
    @GetMapping("/detail/{id}")
    public R<Map<String, Object>> userDetail(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return R.error("用户不存在");
        user.setPassword(null);
        Map<String, Object> m = new HashMap<>();
        m.put("user", user);

        if (user.getRole() == 1) {
            ElderInfo info = elderInfoMapper.selectOne(
                    new QueryWrapper<ElderInfo>().eq("elder_id", id));
            m.put("elderInfo", info);
            List<ElderGuardian> rels = elderGuardianMapper.selectList(
                    new QueryWrapper<ElderGuardian>().eq("elder_id", id));
            List<SysUser> guardians = new ArrayList<>();
            for (ElderGuardian r : rels) {
                SysUser g = sysUserMapper.selectById(r.getGuardianId());
                if (g != null) { g.setPassword(null); guardians.add(g); }
            }
            m.put("guardians", guardians);
        }
        if (user.getRole() == 4) {
            List<ElderGuardian> rels = elderGuardianMapper.selectList(
                    new QueryWrapper<ElderGuardian>().eq("guardian_id", id));
            List<SysUser> elders = new ArrayList<>();
            for (ElderGuardian r : rels) {
                SysUser e = sysUserMapper.selectById(r.getElderId());
                if (e != null) { e.setPassword(null); elders.add(e); }
            }
            m.put("elders", elders);
        }
        return R.ok(m);
    }

    // ==================== 注销 ====================

    /** 注销账号（删除用户 + 关联数据） */
    @DeleteMapping("/delete/{id}")
    public R<String> deleteUser(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) return R.error("用户不存在");

        if (user.getRole() == 1) {
            elderInfoMapper.delete(new QueryWrapper<ElderInfo>().eq("elder_id", id));
            elderGuardianMapper.delete(new QueryWrapper<ElderGuardian>().eq("elder_id", id));
        }
        if (user.getRole() == 4) {
            elderGuardianMapper.delete(new QueryWrapper<ElderGuardian>().eq("guardian_id", id));
        }
        sysUserMapper.deleteById(id);
        return R.ok("账号已注销");
    }

    /** 超级管理员：管理员列表 */
    @GetMapping("/adminList")
    public R<List<SysUser>> adminList(@RequestParam Long adminId) {
        SysUser me = sysUserMapper.selectById(adminId);
        if (me == null || me.getRole() != 5) return R.error("无权限");
        List<SysUser> list = sysUserMapper.selectList(new QueryWrapper<SysUser>()
                .eq("role", 3).ne("id", adminId));
        list.forEach(u -> u.setPassword(null));
        return R.ok(list);
    }

    /** 超级管理员：新增管理员 */
    @PostMapping("/addAdmin")
    public R<Long> addAdmin(@RequestParam Long adminId,
                            @RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String realName,
                            @RequestParam String phone) {
        SysUser me = sysUserMapper.selectById(adminId);
        if (me == null || me.getRole() != 5) return R.error("无权限");
        SysUser exist = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (exist != null) return R.error("用户名已存在");
        SysUser u = new SysUser();
        u.setUsername(username); u.setPassword(password);
        u.setRealName(realName); u.setPhone(phone);
        u.setRole(3); u.setStatus(1);
        sysUserMapper.insert(u);
        return R.ok(u.getId());
    }

    /** 超级管理员：删除管理员 */
    @DeleteMapping("/delAdmin/{id}")
    public R<String> delAdmin(@PathVariable Long id, @RequestParam Long adminId) {
        SysUser me = sysUserMapper.selectById(adminId);
        if (me == null || me.getRole() != 5) return R.error("无权限");
        SysUser target = sysUserMapper.selectById(id);
        if (target == null) return R.error("用户不存在");
        if (target.getRole() == 5) return R.error("不能删除超级管理员");
        sysUserMapper.deleteById(id);
        return R.ok("已删除");
    }

    /** 超级管理员：启用/禁用管理员 */
    @PutMapping("/toggleAdmin/{id}")
    public R<String> toggleAdmin(@PathVariable Long id, @RequestParam Long adminId) {
        SysUser me = sysUserMapper.selectById(adminId);
        if (me == null || me.getRole() != 5) return R.error("无权限");
        SysUser u = sysUserMapper.selectById(id);
        if (u == null) return R.error("用户不存在");
        if (u.getRole() == 5) return R.error("不能操作超级管理员");
        u.setStatus(u.getStatus() != null && u.getStatus() == 1 ? 0 : 1);
        sysUserMapper.updateById(u);
        return R.ok("已切换");
    }
}