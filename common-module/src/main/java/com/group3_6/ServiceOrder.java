package com.group3_6;

import lombok.Data;
import java.util.Date;

@Data
public class ServiceOrder {
    private Integer id;
    private Integer shopId;
    private Integer aftersaleId;
    private Integer type; // 0上门 1寄件 2线下
    private String consignee; // JSON格式的收货人信息
    private Integer status; // 1待处理 2处理中 3已完成 4已取消
    private Date createTime;
    private Date updateTime;

    /**
     * 创建时的初始化方法
     */
    public void onCreate() {
        this.createTime = new Date();
        this.updateTime = new Date();
        if (this.status == null) {
            this.status = 1; // 默认待处理
        }
    }

    /**
     * 更新时的处理方法
     */
    public void onUpdate() {
        this.updateTime = new Date();
    }

    /**
     * 获取收货人JSON
     */
    public String getConsigneeJson() {
        return this.consignee;
    }
}