package com.group3_6.aftersale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AfterSaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterSaleApplication.class, args);
    }

}
