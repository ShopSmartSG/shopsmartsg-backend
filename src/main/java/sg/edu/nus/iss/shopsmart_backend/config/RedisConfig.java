package sg.edu.nus.iss.shopsmart_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisKeys;

@Configuration
public class RedisConfig extends RedisKeys {

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
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        if (redisPassword.isEmpty()) {
            return new JedisPool(poolConfig, redisHost, redisPort, 2000, null, redisDb);
        } else {
            return new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword, redisDb);
        }
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost,redisPort);
        redisStandaloneConfiguration.setDatabase(redisDb);
//        redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        return template;
    }
}
