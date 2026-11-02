package com.silver.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.silver.common.entity.ServiceOrder;
import com.silver.order.mapper.ServiceOrderMapper;
import com.silver.order.service.ServiceOrderService;
import org.springframework.stereotype.Service;

@Service
public class ServiceOrderServiceImpl extends ServiceImpl<ServiceOrderMapper, ServiceOrder> implements ServiceOrderService {
}