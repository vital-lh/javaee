-- 创建数据库
CREATE DATABASE IF NOT EXISTS oomall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE oomall;

-- 创建售后单表
CREATE TABLE IF NOT EXISTS aftersale_order (
                                               id INT PRIMARY KEY AUTO_INCREMENT COMMENT '售后单ID',
                                               shop_id INT NOT NULL COMMENT '店铺ID',
                                               order_id INT NOT NULL COMMENT '原始订单ID',
                                               aftersale_type INT NOT NULL DEFAULT 2 COMMENT '售后类型：1退货 2维修 3换货',
                                               product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
                                               customer_name VARCHAR(100) NOT NULL COMMENT '客户姓名',
                                               customer_phone VARCHAR(20) NOT NULL COMMENT '客户电话',
                                               address VARCHAR(500) NOT NULL COMMENT '客户地址',
                                               region_id INT DEFAULT 0 COMMENT '地区ID',
                                               reason VARCHAR(500) COMMENT '售后原因',
                                               status INT NOT NULL DEFAULT 1 COMMENT '状态：1待审核 2审核通过 3审核拒绝 4已完成',
                                               audit_result VARCHAR(500) COMMENT '审核结论',
                                               audit_time DATETIME NULL COMMENT '审核时间',
                                               create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               INDEX idx_shop_id (shop_id),
                                               INDEX idx_status (status),
                                               INDEX idx_order_id (order_id),
                                               INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='售后单表';

-- 创建服务单表
CREATE TABLE IF NOT EXISTS service_order (
                                             id INT PRIMARY KEY AUTO_INCREMENT COMMENT '服务单ID',
                                             shop_id INT NOT NULL COMMENT '店铺ID',
                                             aftersale_id INT NOT NULL COMMENT '售后单ID',
                                             type INT NOT NULL DEFAULT 0 COMMENT '服务类型：0上门 1寄件 2线下',
                                             consignee TEXT NOT NULL COMMENT '收货人信息(JSON格式)',
                                             status INT NOT NULL DEFAULT 1 COMMENT '状态：1待处理 2处理中 3已完成 4已取消',
                                             create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             INDEX idx_shop_id (shop_id),
                                             INDEX idx_aftersale_id (aftersale_id),
                                             INDEX idx_status (status),
                                             INDEX idx_create_time (create_time),
                                             UNIQUE KEY uk_aftersale (aftersale_id) COMMENT '售后单唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务单表';

-- 创建索引优化查询性能
CREATE INDEX idx_aftersale_shop_status ON aftersale_order(shop_id, status);
CREATE INDEX idx_service_shop_status ON service_order(shop_id, status);

-- 创建用户并授权（如果不存在）
CREATE USER IF NOT EXISTS 'oomall_user'@'%' IDENTIFIED BY 'Oomall@User123';
GRANT ALL PRIVILEGES ON oomall.* TO 'oomall_user'@'%';
FLUSH PRIVILEGES;

-- 插入测试数据
INSERT INTO aftersale_order (shop_id, order_id, aftersale_type, product_name, customer_name, customer_phone, address, region_id, reason, status) VALUES
                                                                                                                                                     (100, 10001, 2, 'iPhone 14 Pro', '张三', '13800138000', '北京市朝阳区建国路88号', 110105, '屏幕损坏，需要维修', 1),
                                                                                                                                                     (100, 10002, 1, 'MacBook Pro', '李四', '13900139000', '上海市浦东新区陆家嘴', 310115, '不喜欢颜色，申请退货', 1),
                                                                                                                                                     (101, 10003, 2, '华为Mate 50', '王五', '13600136000', '广州市天河区体育西路', 440106, '电池续航问题，需要维修', 1),
                                                                                                                                                     (101, 10004, 3, 'iPad Air', '赵六', '13700137000', '深圳市南山区科技园', 440305, '屏幕有坏点，申请换货', 1),
                                                                                                                                                     (102, 10005, 2, '戴尔笔记本电脑', '钱七', '13500135000', '杭州市西湖区文三路', 330106, '键盘失灵，需要维修', 1);

-- 显示创建的表结构
SHOW TABLES;
DESC aftersale_order;
DESC service_order;

-- 显示测试数据
SELECT
    id,
    shop_id,
    order_id,
    aftersale_type,
    CASE aftersale_type
        WHEN 1 THEN '退货'
        WHEN 2 THEN '维修'
        WHEN 3 THEN '换货'
        ELSE '其他'
        END as type_name,
    product_name,
    customer_name,
    status,
    create_time
FROM aftersale_order ORDER BY id;