package com.wei.orchestrator.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;

@Configuration
public class DistributedLockConfiguration {

    @Value("${scheduler.lock.ttl:60000}")
    private int lockTtl;

    @Bean
    public DefaultLockRepository lockRepository(DataSource dataSource) {
        DefaultLockRepository repository = new DefaultLockRepository(dataSource);
        repository.setPrefix("INT_");
        repository.setTimeToLive(lockTtl);
        return repository;
    }

    @Bean
    public LockRegistry lockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }
}
