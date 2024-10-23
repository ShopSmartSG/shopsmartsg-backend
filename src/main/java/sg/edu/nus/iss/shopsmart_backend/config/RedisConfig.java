package sg.edu.nus.iss.shopsmart_backend.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisKeys;

@Configuration
public class RedisConfig extends RedisKeys {
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${"+REDIS_HOST_KEY+"}")
    private String redisHost;

    @Value("${"+REDIS_PORT_KEY+"}")
    private int redisPort;

    @Value("${"+REDIS_PASSWORD_KEY+"}")
    private String redisPassword;

    @Value("${"+REDIS_DB_NO_KEY+"}")
    private int redisDb;

    @Bean
    public JedisPool jedisPool() {
        log.info("Creating JedisPool with host: {}, port: {}, db: {} and password : {}", redisHost, redisPort, redisDb, redisPassword);
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        if (StringUtils.isEmpty(redisPassword)) {
            log.info("did not find redis password, so skipping it");
            return new JedisPool(poolConfig, redisHost, redisPort, 2000,null, redisDb);
        } else {
            return new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword, redisDb);
        }
    }
}
