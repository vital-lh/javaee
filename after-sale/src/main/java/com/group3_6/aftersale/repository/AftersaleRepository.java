package com.group3_6.aftersale.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Slf4j
@Repository
public class AftersaleRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 根据ID查询售后单
     */
    public Map<String, Object> findById(Integer aftersaleId) {
        String sql = "SELECT * FROM aftersale_order WHERE id = ?";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, aftersaleId);
            log.info("查询到售后单 - ID: {}, 类型: {}, 状态: {}",
                    aftersaleId, result.get("aftersale_type"), result.get("status"));
            return result;
        } catch (Exception e) {
            log.error("查询售后单失败 - ID: {}", aftersaleId, e);
            return null;
        }
    }

    /**
     * 更新售后单状态
     */
    public boolean updateStatus(Integer aftersaleId, Integer status, String conclusion) {
        String sql = "UPDATE aftersale_order SET status = ?, audit_result = ?, audit_time = NOW(), update_time = NOW() WHERE id = ?";
        try {
            int rows = jdbcTemplate.update(sql, status, conclusion, aftersaleId);
            boolean success = rows > 0;

            if (success) {
                log.info("更新售后单状态成功 - ID: {}, 状态: {}, 结论: {}",
                        aftersaleId, status, conclusion);
            } else {
                log.warn("更新售后单状态失败，未找到记录 - ID: {}", aftersaleId);
            }

            return success;
        } catch (Exception e) {
            log.error("更新售后单状态异常 - ID: {}", aftersaleId, e);
            return false;
        }
    }

    /**
     * 获取售后单类型
     */
    public Integer getAftersaleType(Integer aftersaleId) {
        String sql = "SELECT aftersale_type FROM aftersale_order WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, aftersaleId);
        } catch (Exception e) {
            log.error("获取售后单类型失败 - ID: {}", aftersaleId, e);
            return null;
        }
    }
}