package com.group3_6.service.repository;

import com.group3_6.ServiceOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Repository
public class ServiceOrderRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 保存服务单并返回生成的ID
     */
    public Integer save(ServiceOrder serviceOrder) {
        try {
            String sql = "INSERT INTO service_order (shop_id, aftersale_id, type, consignee, status, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int rows = jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, serviceOrder.getShopId());
                ps.setInt(2, serviceOrder.getAftersaleId());
                ps.setInt(3, serviceOrder.getType());
                ps.setString(4, serviceOrder.getConsignee());
                ps.setInt(5, serviceOrder.getStatus());
                ps.setTimestamp(6, new java.sql.Timestamp(serviceOrder.getCreateTime().getTime()));
                ps.setTimestamp(7, new java.sql.Timestamp(serviceOrder.getUpdateTime().getTime()));
                return ps;
            }, keyHolder);

            if (rows > 0) {
                Number key = keyHolder.getKey();
                if (key != null) {
                    serviceOrder.setId(key.intValue());
                    log.info("服务单保存成功 - ID: {}, 售后单ID: {}", key, serviceOrder.getAftersaleId());
                    return key.intValue();
                }
            }

            log.error("服务单保存失败 - 售后单ID: {}", serviceOrder.getAftersaleId());
            return null;

        } catch (Exception e) {
            log.error("保存服务单到数据库异常", e);
            return null;
        }
    }

    /**
     * 根据售后单ID查询是否存在服务单
     */
    public boolean existsByAftersaleId(Integer aftersaleId) {
        String sql = "SELECT COUNT(*) FROM service_order WHERE aftersale_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, aftersaleId);
        return count != null && count > 0;
    }

    /**
     * 根据ID查询服务单
     */
    public ServiceOrder findById(Integer id) {
        String sql = "SELECT * FROM service_order WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql,
                    new BeanPropertyRowMapper<>(ServiceOrder.class), id);
        } catch (Exception e) {
            log.error("查询服务单失败 - ID: {}", id, e);
            return null;
        }
    }

    /**
     * 根据店铺ID查询服务单列表
     */
    public List<ServiceOrder> findByShopId(Integer shopId) {
        String sql = "SELECT * FROM service_order WHERE shop_id = ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql,
                new BeanPropertyRowMapper<>(ServiceOrder.class), shopId);
    }

    /**
     * 更新服务单状态
     */
    public boolean updateStatus(Integer id, Integer status) {
        String sql = "UPDATE service_order SET status = ?, update_time = NOW() WHERE id = ?";
        int rows = jdbcTemplate.update(sql, status, id);
        return rows > 0;
    }

    /**
     * 获取所有服务单（用于测试）
     */
    public List<ServiceOrder> findAll() {
        String sql = "SELECT * FROM service_order ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(ServiceOrder.class));
    }
}