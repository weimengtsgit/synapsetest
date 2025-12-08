package com.synapsetest.testmanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * AI驱动测试任务管理系统 - 主应用类
 * Using MyBatis for persistence
 *
 * @author SynapseTest Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableKafka
@MapperScan("com.synapsetest.testmanagement.mapper")
public class TestManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestManagementApplication.class, args);
    }
}
