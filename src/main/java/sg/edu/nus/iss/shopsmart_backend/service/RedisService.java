package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisKeys;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;

import java.util.Map;

@Service
public class RedisService extends RedisKeys {
    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private final ObjectMapper mapper = Json.mapper();

    private final RedisManager redisManager;

    @Autowired
    public RedisService(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public String insertDdoDataInRedis(String key, DataDynamicObject ddo) {
        log.info("Request received to insert ddo data in redis for ddo key : {}, with value : {}", key, ddo);
        String ddoKey = REDIS_DDO_PREFIX.concat(key);
        JsonNode value = mapper.convertValue(ddo, JsonNode.class);
        redisManager.set(key, value);
        log.info("Inserting data in redis as string: {} for key {}", value.toString(), ddoKey);

        ObjectNode res = mapper.createObjectNode();
        try{
            log.info("Parsed response : {}", mapper.readTree(value.toString()));
            res.put("status", "done");
            return res.toString();
        } catch (Exception e){
            log.error("Error while parsing response: {}", e.getMessage());
            res.put("error", e.getMessage());
            return res.toString();
        }
    }

    public JsonNode getDdoDataFromRedis(String ddoKey){
        log.info("Request received to get data from redis for ddo key: {}", ddoKey);
        JsonNode ddoData = redisManager.getAsJson(ddoKey);
        log.info("Response received after fetching data from redis for key: {} is {}", ddoKey, ddoData);
        return ddoData;
    }

    public void setHashValue(String key, String mapEntry, String value){
        log.debug("Setting hash value for entry {} in redis key: {}", mapEntry, key);
        redisManager.setHashValue(key, mapEntry, value);
    }

    public String getHashValue(String key, String mapEntry){
        log.debug("Fetching hash value for entry {} in redis key: {}", mapEntry, key);
        return redisManager.getHashValue(key, mapEntry);
    }

    public JsonNode getHashMap(String key){
        log.debug("Fetching hash map for key: {}", key);
        Map<String, String> resp = redisManager.getHashMap(key);
        log.info("fetched map : {}", resp);
        return mapper.convertValue(resp, JsonNode.class);
    }
}
