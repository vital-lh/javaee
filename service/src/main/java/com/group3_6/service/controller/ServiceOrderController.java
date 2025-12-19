package com.group3_6.service.controller;

import com.group3_6.Result;
import com.group3_6.ServiceOrderCreateDTO;
import com.group3_6.service.service.ServiceOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/internal/shops/{shopId}/aftersales/{id}")
public class ServiceOrderController {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @PostMapping("/serviceorders")
    public Result createServiceOrder(
            @PathVariable("shopId") Integer shopId,
            @PathVariable("id") Integer aftersaleId,
            @RequestBody ServiceOrderCreateDTO createDTO) {

        log.info("创建服务单 - 店铺ID: {}, 售后单ID: {}, 请求参数: {}", shopId, aftersaleId, createDTO);

        try {
            // 创建服务单
            Integer serviceOrderId = serviceOrderService.createServiceOrder(shopId, aftersaleId, createDTO);

            if (serviceOrderId != null) {
                log.info("服务单创建成功 - ID: {}", serviceOrderId);
                return Result.success(serviceOrderId);
            } else {
                log.error("服务单创建失败 - 售后单ID: {}", aftersaleId);
                return Result.error("服务单创建失败");
            }

        } catch (Exception e) {
            log.error("创建服务单时发生异常 - 店铺ID: {}, 售后单ID: {}", shopId, aftersaleId, e);
            return Result.error("系统错误，创建服务单失败");
        }
    }

    @GetMapping("/serviceorders")
    public Result getServiceOrders(
            @PathVariable("shopId") Integer shopId,
            @PathVariable("id") Integer aftersaleId) {

        log.info("查询服务单 - 店铺ID: {}, 售后单ID: {}", shopId, aftersaleId);

        try {
            // 查询服务单
            return Result.success(serviceOrderService.getServiceOrderByAftersaleId(aftersaleId));
        } catch (Exception e) {
            log.error("查询服务单异常", e);
            return Result.error("查询失败");
        }
    }
}
