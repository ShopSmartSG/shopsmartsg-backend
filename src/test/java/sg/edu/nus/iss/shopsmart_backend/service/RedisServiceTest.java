package sg.edu.nus.iss.shopsmart_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.utils.RedisManager;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RedisServiceTest {
    private final ObjectMapper objectMapper = Json.mapper();

    @Mock
    private RedisManager redisManager;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInsertDdoDataInRedis() {
        DataDynamicObject ddo = new DataDynamicObject();
        ddo.setMethod("POST");
        ddo.setApi("api");
        ddo.setService("service");

        String resp = redisService.insertDdoDataInRedis("apiKey", ddo);
        assertNotNull(resp);
    }

    @Test
    public void testGetDdoDataFromRedis() {
        when(redisManager.getAsJson(anyString())).thenReturn(objectMapper.createObjectNode());
        assertNotNull(redisService.getDdoDataFromRedis("apiKey"));
    }

    @Test
    public void testSetHashValue() {
        doNothing().when(redisManager).setHashValue(anyString(), anyString(), anyString());
        redisService.setHashValue("key", "entry", "value");
        verify(redisManager, times(1)).setHashValue("key", "entry", "value");
    }

    @Test
    public void testGetHashValue() {
        when(redisManager.getHashValue(anyString(), anyString())).thenReturn("value");
        assertEquals("value", redisService.getHashValue("key", "entry"));
    }

    @Test
    public void testGetHashMap() {
        when(redisManager.getHashMap(anyString())).thenReturn(new HashMap<>());
        assertNotNull(redisService.getHashMap("key"));
    }
}