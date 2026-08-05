package com.system.booking.modules.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// enables async event listeners and scheduled sweep jobs
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
