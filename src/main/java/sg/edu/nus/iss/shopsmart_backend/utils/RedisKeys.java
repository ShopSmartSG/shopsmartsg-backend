package sg.edu.nus.iss.shopsmart_backend.utils;

public interface RedisKeys {
    String REDIS_HOST_KEY = "redis.host";
    String REDIS_PORT_KEY = "redis.port";
    String REDIS_PASSWORD_KEY = "redis.password";
    String REDIS_DB_NO_KEY = "redis.db";
    String REDIS_DDO_PREFIX = "ss:ddo:";
    String REDIS_ENVIRONMENT_DEVELOPMENT = "ss:environment:development";
    String REDIS_SESSION_PREFIX = "ss:session:";
    String REDIS_FEATURE_FLAGS = "ss:feature:flags";
}
