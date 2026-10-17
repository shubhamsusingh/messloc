package com.MessLoc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB configuration enabling automatic auditing for @CreatedDate and @LastModifiedDate fields.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
