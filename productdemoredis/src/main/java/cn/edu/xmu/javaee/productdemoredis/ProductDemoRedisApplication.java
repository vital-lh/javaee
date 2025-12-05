package cn.edu.xmu.javaee.productdemoredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "cn.edu.xmu.javaee.productdemoredis.mapper")
@EntityScan(basePackages = "cn.edu.xmu.javaee.productdemoredis.mapper.po")
public class ProductDemoRedisApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductDemoRedisApplication.class, args);
    }
}