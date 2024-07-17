package me.sedattr.deluxeauctions.others;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PlaceholderUtil {
    public Map<String, String> placeholders = new HashMap<>();

    public PlaceholderUtil addPlaceholder(String key, String value) {
        if (key == null)
            return this;
        if (value == null)
            return this;

        this.placeholders.put(key, value);
        return this;
    }
}
