package com.group3_6.aftersale.service;

import com.group3_6.aftersale.feignClient.ServiceOrderFeignClient;
import com.group3_6.aftersale.repository.AftersaleRepository;
import com.group3_6.AftersaleConfirmDTO;
import com.group3_6.Result;
import com.group3_6.ServiceOrderCreateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
public class afterSaleService {

    @Autowired
    private AftersaleRepository aftersaleRepository;

    @Autowired
    private ServiceOrderFeignClient serviceOrderFeignClient;

    @Transactional
    public Result confirmAftersale(Integer shopId, Integer aftersaleId, AftersaleConfirmDTO confirmDTO) {
        log.info("开始处理售后单审核 - 店铺ID: {}, 售后单ID: {}, 请求参数: {}", shopId, aftersaleId, confirmDTO);

        try {
            // 1. 查询售后单信息
            Map<String, Object> aftersale = aftersaleRepository.findById(aftersaleId);
            if (aftersale == null) {
                log.error("售后单不存在 - ID: {}", aftersaleId);
                return Result.error("售后单不存在");
            }

            // 2. 检查审核状态
            Integer currentStatus = (Integer) aftersale.get("status");
            if (currentStatus != 1) {
                log.warn("售后单状态不是待审核 - ID: {}, 当前状态: {}", aftersaleId, currentStatus);
                return Result.error("售后单状态不是待审核");
            }

            // 3. 根据confirm值处理不同逻辑
            if (Boolean.TRUE.equals(confirmDTO.getConfirm())) {
                // 审核通过
                log.info("审核通过，开始处理售后单 - ID: {}", aftersaleId);

                // 3.1 更新售后单状态为审核通过
                boolean updateSuccess = aftersaleRepository.updateStatus(
                        aftersaleId,
                        2, // 审核通过
                        confirmDTO.getConclusion() != null ? confirmDTO.getConclusion() : "审核通过"
                );

                if (!updateSuccess) {
                    log.error("更新售后单状态失败 - ID: {}", aftersaleId);
                    return Result.error("更新售后单状态失败");
                }

                log.info("售后单状态更新成功，状态: 审核通过 - ID: {}", aftersaleId);

                // 3.2 检查是否为维修类型（维修类型为2）
                Integer aftersaleType = (Integer) aftersale.get("aftersale_type");
                if (aftersaleType != null && aftersaleType == 2) {
                    log.info("检测到维修类型售后单（类型: {}），开始创建服务单 - 售后单ID: {}", aftersaleType, aftersaleId);

                    // 构建创建服务单的请求参数
                    ServiceOrderCreateDTO serviceOrderDTO = new ServiceOrderCreateDTO();
                    serviceOrderDTO.setType(confirmDTO.getType() != null ? confirmDTO.getType() : 0); // 默认上门服务

                    // 从售后单中获取收货人信息
                    ServiceOrderCreateDTO.Consignee consignee = new ServiceOrderCreateDTO.Consignee();
                    consignee.setName(aftersale.get("customer_name").toString());
                    consignee.setMobile(aftersale.get("customer_phone").toString());
                    consignee.setAddress(aftersale.get("address").toString());

                    // 尝试获取regionId，如果不存在则设为0
                    if (aftersale.containsKey("region_id") && aftersale.get("region_id") != null) {
                        consignee.setRegionId((Integer) aftersale.get("region_id"));
                    } else {
                        consignee.setRegionId(0); // 默认值
                    }

                    serviceOrderDTO.setConsignee(consignee);

                    // 记录创建服务单的请求参数
                    log.info("创建服务单请求参数 - shopId: {}, aftersaleId: {}, serviceOrderDTO: {}",
                            shopId, aftersaleId, serviceOrderDTO);

                    try {
                        // 调用服务模块创建服务单
                        Result result = serviceOrderFeignClient.createServiceOrder(shopId, aftersaleId, serviceOrderDTO);

                        if (result.getErrno() == 0) {
                            log.info("服务单创建成功 - 售后单ID: {}, 服务单ID: {}",
                                    aftersaleId, result.getData());
                            return Result.success("审核通过，服务单已创建");
                        } else {
                            log.error("服务单创建失败 - 售后单ID: {}, 错误信息: {}",
                                    aftersaleId, result.getErrmsg());
                            return Result.success("审核通过，但服务单创建失败: " + result.getErrmsg());
                        }
                    } catch (Exception e) {
                        log.error("调用服务模块创建服务单时发生异常 - 售后单ID: {}", aftersaleId, e);
                        return Result.success("审核通过，但服务单创建异常");
                    }
                } else {
                    // 不是维修类型，只更新状态
                    log.info("非维修类型售后单（类型: {}），不创建服务单 - 售后单ID: {}", aftersaleType, aftersaleId);
                    return Result.success("审核通过");
                }

            } else {
                // 审核不通过
                log.info("审核不通过，更新售后单状态 - ID: {}", aftersaleId);

                // 更新售后单状态为审核拒绝
                boolean updateSuccess = aftersaleRepository.updateStatus(
                        aftersaleId,
                        3, // 审核拒绝
                        confirmDTO.getConclusion() != null ? confirmDTO.getConclusion() : "审核不通过"
                );

                if (!updateSuccess) {
                    log.error("更新售后单状态失败 - ID: {}", aftersaleId);
                    return Result.error("更新售后单状态失败");
                }

                log.info("售后单审核不通过，状态更新成功 - ID: {}", aftersaleId);
                return Result.success("审核不通过");
            }

        } catch (Exception e) {
            log.error("审核售后单时发生异常 - 店铺ID: {}, 售后单ID: {}", shopId, aftersaleId, e);
            return Result.error("审核处理异常: " + e.getMessage());
        }
    }
}
