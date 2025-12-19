package com.group3_6.service.service;

import com.group3_6.ServiceOrder;
import com.group3_6.ServiceOrderCreateDTO;
import com.group3_6.service.repository.ServiceOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ServiceOrderService {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Transactional
    public Integer createServiceOrder(Integer shopId, Integer aftersaleId, ServiceOrderCreateDTO createDTO) {
        log.info("开始创建服务单 - 店铺ID: {}, 售后单ID: {}", shopId, aftersaleId);

        try {
            // 1. 检查是否已存在相同售后单的服务单
            if (serviceOrderRepository.existsByAftersaleId(aftersaleId)) {
                log.warn("已存在该售后单对应的服务单 - 售后单ID: {}", aftersaleId);
                throw new RuntimeException("已存在该售后单对应的服务单");
            }

            // 2. 创建服务单POJO
            ServiceOrder serviceOrder = new ServiceOrder();
            serviceOrder.setShopId(shopId);
            serviceOrder.setAftersaleId(aftersaleId);
            serviceOrder.setType(createDTO.getType());

            // 将收货人对象转换为JSON字符串
            String consigneeJson = createDTO.getConsigneeJson();
            serviceOrder.setConsignee(consigneeJson);

            // 3. 初始化时间
            serviceOrder.onCreate();

            // 4. 保存到数据库
            Integer serviceOrderId = serviceOrderRepository.save(serviceOrder);

            if (serviceOrderId != null) {
                log.info("服务单创建成功 - 店铺ID: {}, 售后单ID: {}, 服务单ID: {}",
                        shopId, aftersaleId, serviceOrderId);
                return serviceOrderId;
            } else {
                log.error("服务单保存到数据库失败 - 售后单ID: {}", aftersaleId);
                throw new RuntimeException("服务单保存失败");
            }

        } catch (Exception e) {
            log.error("创建服务单异常 - 店铺ID: {}, 售后单ID: {}", shopId, aftersaleId, e);
            throw new RuntimeException("创建服务单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据售后单ID获取服务单
     */
    public ServiceOrder getServiceOrderByAftersaleId(Integer aftersaleId) {
        try {
            // 这里简单实现，实际应该添加查询方法到Repository
            // 由于需求中只有一个服务单对应一个售后单，我们可以先查询列表
            var orders = serviceOrderRepository.findAll();
            return orders.stream()
                    .filter(order -> order.getAftersaleId().equals(aftersaleId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("查询服务单异常 - 售后单ID: {}", aftersaleId, e);
            return null;
        }
    }
}
