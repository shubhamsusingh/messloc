package com.MessLoc;

import com.MessLoc.repository.OwnerProfileRepository;
import com.MessLoc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class MessLocApplicationTests {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OwnerProfileRepository ownerProfileRepository;

    @TestConfiguration
    static class TestMongoConfig {
        @Bean(name = "mongoMappingContext")
        public MongoMappingContext mongoMappingContext() {
            return new MongoMappingContext();
        }
    }

    @Test
    void contextLoads() {
    }

}
