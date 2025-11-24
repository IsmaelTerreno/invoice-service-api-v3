package com.remotejob.invoiceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EntityScan(basePackages = {"com.remotejob.invoiceservice.entity"})
@EnableJpaRepositories(basePackages = {"com.remotejob.invoiceservice.repository"})
public class InvoiceServiceAPIApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceServiceAPIApplication.class, args);
    }

}


