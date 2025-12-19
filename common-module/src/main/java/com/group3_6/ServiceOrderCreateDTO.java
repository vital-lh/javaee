package com.group3_6;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceOrderCreateDTO {
    private Integer type; // 0上门 1寄件 2线下

    private Consignee consignee;

    @Data
    public static class Consignee {
        private String name;
        private String mobile;
        private Integer regionId;
        private String address;

        /**
         * 将收货人信息转换为JSON字符串
         */
        public String toJsonString() {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "{}";
            }
        }

        /**
         * 从JSON字符串解析收货人信息
         */
        public static Consignee fromJsonString(String json) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(json, Consignee.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * 将consignee对象转换为JSON字符串
     */
    public String getConsigneeJson() {
        if (this.consignee != null) {
            return this.consignee.toJsonString();
        }
        return "{}";
    }
}