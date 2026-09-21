package com.suresh.sms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Switches on @CreatedDate / @LastModifiedDate handling for entities that
 * use AuditingEntityListener (currently Student).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
