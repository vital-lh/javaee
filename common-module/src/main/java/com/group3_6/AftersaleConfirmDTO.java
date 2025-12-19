package com.group3_6;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AftersaleConfirmDTO {
    /**
     * 审核确认（true:同意，false:拒绝）
     */
    private Boolean confirm;

    /**
     * 审核结论
     */
    private String conclusion;

    /**
     * 服务类型 0上门 1寄件 2线下
     */
    private Integer type;

    /**
     * 构造函数
     */
    public AftersaleConfirmDTO() {
        // 默认值
        this.confirm = false; // 默认审核不通过
        this.type = 0; // 默认上门服务
    }
}
