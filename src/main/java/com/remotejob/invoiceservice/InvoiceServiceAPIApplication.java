package com.remotejob.invoiceservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EntityScan(basePackages = {"com.remotejob.invoiceservice.entity"})
@EnableJpaRepositories(basePackages = {"com.remotejob.invoiceservice.repository"})
public class InvoiceServiceAPIApplication {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceServiceAPIApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(InvoiceServiceAPIApplication.class, args);
    }

    @Bean
    CommandLineRunner logDatabaseConfiguration(Environment env) {
        return args -> {
            String datasourceUrl = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String activeProfile = String.join(",", env.getActiveProfiles());
            if (activeProfile.isEmpty()) {
                activeProfile = "default";
            }

            // Extract individual components for detailed logging
            String host = env.getProperty("HOST_DB_CONFIG");
            String port = env.getProperty("PORT_DB_CONFIG");
            String database = env.getProperty("DATABASE_NAME_DB_CONFIG");
            String sslMode = env.getProperty("DB_SSL_MODE");

            logger.info("=".repeat(80));
            logger.info("DATABASE CONFIGURATION");
            logger.info("=".repeat(80));
            logger.info("Active Profile: {}", activeProfile);
            logger.info("Database Host: {}", host);
            logger.info("Database Port: {}", port);
            logger.info("Database Name: {}", database);
            logger.info("Database Username: {}", username);
            logger.info("SSL Mode: {}", sslMode != null ? sslMode : "NOT SET (will use default from URL)");
            logger.info("Complete JDBC URL: {}", datasourceUrl);
            logger.info("=".repeat(80));
        };
    }

}


