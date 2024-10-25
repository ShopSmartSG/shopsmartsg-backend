package sg.edu.nus.iss.shopsmart_backend.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class JsonUtils {
    private final static String SEPARATOR = ".";

    public static String getText(JsonNode payload, String path){
        if (payload == null || payload.isNull()) {
            return null;
        }
        if (!path.contains(SEPARATOR)) {
            return payload.hasNonNull(path) ? payload.get(path).asText() : null;
        }
        String key = path.substring(0, path.indexOf(SEPARATOR));
        String subPath = path.substring(path.indexOf(SEPARATOR) + 1);
        return getText(payload.get(key), subPath);
    }
}
