package sg.edu.nus.iss.shopsmart_backend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;

import java.util.Map;

@Service
public class RedisManager extends RedisKeys {
    private static final Logger log = LoggerFactory.getLogger(RedisManager.class);
    private final ObjectMapper mapper = Json.mapper();

    private final JedisPool jedisPool;

    @Autowired
    public RedisManager(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void setHashValue(String key, String mapEntry, String value){
        log.debug("Setting hash value for entry {} in redis key: {}", mapEntry, key);
        try(Jedis jedis = jedisPool.getResource()){
            jedis.hset(key, mapEntry, value);
        }
    }

    public void setHashMap(String key, Map<String, String> hashMap){
        log.debug("Setting hash map for redis key: {}", key);
        try(Jedis jedis = jedisPool.getResource()){
            jedis.hset(key, hashMap);
        }
    }

    public Map<String, String> getHashMap(String key){
        log.debug("Fetching hash map for redis key: {}", key);
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hgetAll(key);
        }
    }

    public void set(String key, JsonNode value){
        log.debug("Setting object {} in redis for key: {}", value, key);
        try(Jedis jedis = jedisPool.getResource()){
            jedis.set(key, value.toString());
        }
    }

    public JsonNode getAsJson(String key){
        log.debug("Fetching json data for key: {}", key);
        String value = "";
        try(Jedis jedis = jedisPool.getResource()){
            value = jedis.get(key);
            if(StringUtils.isEmpty(value)){
                return null;
            }
            return mapper.readTree(value);
        } catch (Exception ex){
            log.error("Error while parsing fetched key data {} into json: {}", value, ex.getMessage());
            return null;
        }
    }

    public String getHashValue(String key, String mapKey){
        log.debug("Fetching hash value for entry {} in redis key: {}", mapKey, key);
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hget(key, mapKey);
        }
    }

    public DataDynamicObject getDdoData(String apiKey){
        String key = REDIS_DDO_PREFIX + apiKey;
        JsonNode ddoObt = getAsJson(key);
        if(ddoObt == null){
            return null;
        }
        return mapper.convertValue(ddoObt, DataDynamicObject.class);
    }

    public String getServiceEndpoint(String service){
        log.debug("Fetching service endpoint for service: {}", service);
        return getHashValue(REDIS_ENVIRONMENT_DEVELOPMENT, service);
    }
}
