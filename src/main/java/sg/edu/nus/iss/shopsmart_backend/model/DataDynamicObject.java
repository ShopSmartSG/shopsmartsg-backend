package sg.edu.nus.iss.shopsmart_backend.model;

import lombok.Data;

import java.util.Map;

@Data
public class DataDynamicObject {
    private String api;
    private String service;
    private String method;
    private Map<String, String> params;
    private Map<String, String> headers;
    private long connectTimeout = 1000;
    private long readTimeout = 30000;
}
