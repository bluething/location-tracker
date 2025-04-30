package io.github.bluething.playground.java.ingestionservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IngestionKafkaProperties.class)
public class IngestionConfig {
}
