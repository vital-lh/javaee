package com.group3_6.aftersale.controller;

import com.group3_6.aftersale.service.afterSaleService;
import com.group3_6.AftersaleConfirmDTO;
import com.group3_6.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/shops/{shopId}/aftersales")
public class afterSaleController {

    @Autowired
    private afterSaleService aftersaleService;

    /**
     * 审核售后单
     * PUT /shops/{shopId}/aftersales/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public Result confirmAftersale(
            @PathVariable("shopId") Integer shopId,
            @PathVariable("id") Integer id,
            @RequestHeader(value = "authorization", required = false) String authorization,
            @RequestBody AftersaleConfirmDTO confirmDTO) {

        // 记录请求开始
        log.info("==================== 开始审核售后单 ====================");
        log.info("店铺ID: {}, 售后单ID: {}", shopId, id);
        log.info("请求头token: {}", authorization);
        log.info("请求参数: {}", confirmDTO);



        // 验证请求参数
        if (confirmDTO == null) {
            log.error("请求参数为空");
            log.info("==================== 审核结束 ====================");
            return Result.error("请求参数不能为空");
        }

        // 记录confirm值
        log.info("审核确认值(confirm): {}", confirmDTO.getConfirm());

        // 调用Service处理业务逻辑
        Result result = aftersaleService.confirmAftersale(shopId, id, confirmDTO);

        // 记录最终结果
        log.info("审核结果: errno={}, errmsg={}", result.getErrno(), result.getErrmsg());
        log.info("==================== 审核结束 ====================");

        return result;
    }

}
