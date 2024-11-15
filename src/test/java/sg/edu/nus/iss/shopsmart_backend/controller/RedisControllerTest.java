package sg.edu.nus.iss.shopsmart_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import sg.edu.nus.iss.shopsmart_backend.model.DataDynamicObject;
import sg.edu.nus.iss.shopsmart_backend.service.RedisService;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class RedisControllerTest {
    private final ObjectMapper objectMapper = Json.mapper();

    @Mock
    private RedisService redisService;

    @InjectMocks
    private RedisController redisController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testInsertDdoDataInRedis() {
        DataDynamicObject ddo = new DataDynamicObject();
        when(redisService.insertDdoDataInRedis(anyString(), any(DataDynamicObject.class))).thenReturn("Success");
        ResponseEntity<String> response = redisController.insertDdoDataInRedis("test", ddo);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody());
    }

    @Test
    public void testGetDdoDataFromRedis() {
        when(redisService.getDdoDataFromRedis(anyString())).thenReturn(objectMapper.createObjectNode());
        ResponseEntity<JsonNode> response = redisController.getDdoDataFromRedis("test");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.createObjectNode(), response.getBody());
    }

    @Test
    public void testInsertHashEntry() {
        doNothing().when(redisService).setHashValue(anyString(), anyString(), anyString());
        ResponseEntity<String> response = redisController.insertHashEntry("test", new HashMap<>());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Success", response.getBody());
    }

    @Test
    public void testGetHashEntry() {
        when(redisService.getHashValue(anyString(), anyString())).thenReturn("value");
        ResponseEntity<String> response = redisController.getHashEntry("test", "test");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("value", response.getBody());
    }

    @Test
    public void testGetHashEntries() {
        when(redisService.getHashMap(anyString())).thenReturn(objectMapper.createObjectNode());
        ResponseEntity<JsonNode> response = redisController.getHashEntries("test");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(objectMapper.createObjectNode(), response.getBody());
    }
}