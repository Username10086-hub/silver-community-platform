package com.silver.common.feign;

import com.silver.common.entity.SysUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("service-user")
public interface UserFeignClient {

    @GetMapping("/user/getName/{userId}")
    String getUserNameById(@PathVariable("userId") Long userId);

    @GetMapping("/user/info/{id}")
    SysUser getUserById(@PathVariable("id") Long id);
}
