package com.remotejob.invoiceservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EntityScan(basePackages = {"com.remotejob.invoiceservice.entity"})
@EnableJpaRepositories(basePackages = {"com.remotejob.invoiceservice.repository"})
public class InvoiceServiceAPIApplication {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceServiceAPIApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InvoiceServiceAPIApplication.class);
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            Environment env = event.getEnvironment();

            String datasourceUrl = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String host = env.getProperty("HOST_DB_CONFIG");
            String port = env.getProperty("PORT_DB_CONFIG");
            String database = env.getProperty("DATABASE_NAME_DB_CONFIG");
            String sslMode = env.getProperty("DB_SSL_MODE");
            String[] profiles = env.getActiveProfiles();
            String activeProfile = profiles.length > 0 ? String.join(",", profiles) : "default";

            logger.info("=".repeat(80));
            logger.info("DATABASE CONFIGURATION (BEFORE CONTEXT INITIALIZATION)");
            logger.info("=".repeat(80));
            logger.info("Active Profile: {}", activeProfile);
            logger.info("Database Host: {}", host);
            logger.info("Database Port: {}", port);
            logger.info("Database Name: {}", database);
            logger.info("Database Username: {}", username);
            logger.info("SSL Mode: {}", sslMode != null ? sslMode : "NOT SET (will use default from URL)");
            logger.info("Complete JDBC URL: {}", datasourceUrl);
            logger.info("=".repeat(80));
        });
        app.run(args);
    }

}


