package org.lim.aiautocode.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisChatMemoryStoreConfig {
    private String host;
    private int port;
    private long ttl;
    private String password;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
       return RedisChatMemoryStore.builder()
               .host(host)
               .port(port)
               .ttl(ttl)
               .password(password)
               .build();
    }
}
