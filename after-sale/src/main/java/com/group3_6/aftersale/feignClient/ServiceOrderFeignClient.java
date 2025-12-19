package com.group3_6.aftersale.feignClient;

import com.group3_6.Result;
import com.group3_6.ServiceOrderCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 服务模块的OpenFeign客户端
 * 用于调用服务模块的创建服务单接口
 */
@FeignClient(
        name = "service",
        url = "${feign.client.service-module.url:http://service:8081}",
        configuration = FeignClientConfig.class
)
public interface ServiceOrderFeignClient {

    /**
     * 创建服务单
     * POST /internal/shops/{shopId}/aftersales/{id}/serviceorders
     */
    @PostMapping("/internal/shops/{shopId}/aftersales/{id}/serviceorders")
    Result createServiceOrder(
            @PathVariable("shopId") Integer shopId,
            @PathVariable("id") Integer aftersaleId,
            @RequestBody ServiceOrderCreateDTO serviceOrderDTO);
}
