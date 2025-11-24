package com.remotejob.planservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EntityScan(basePackages = {"com.remotejob.planservice.entity"})
@EnableJpaRepositories(basePackages = {"com.remotejob.planservice.repository"})
public class PlanServiceAPIApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanServiceAPIApplication.class, args);
    }

}


