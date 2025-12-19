package com.group3_6.aftersale.feignClient;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignClientConfig {
    
    /**
     * 配置Feign的日志级别
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // 记录所有请求和响应信息
    }
    
    /**
     * 配置超时时间
     */
    @Bean
    public Request.Options options() {
        return new Request.Options(
            10, TimeUnit.SECONDS,   // 连接超时时间
            30, TimeUnit.SECONDS,   // 读取超时时间
            true                    // 跟随重定向
        );
    }
    
    /**
     * 配置重试机制
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            100,          // 重试间隔
            TimeUnit.SECONDS.toMillis(1), // 最大重试间隔
            3             // 最大重试次数
        );
    }
}