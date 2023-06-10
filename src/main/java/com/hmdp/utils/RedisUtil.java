package com.hmdp.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class RedisUtil {
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Field[] declaredFields = obj.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return map;
    }

}
