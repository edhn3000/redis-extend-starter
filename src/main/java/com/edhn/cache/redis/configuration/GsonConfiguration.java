package com.edhn.cache.redis.configuration;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.bind.util.ISO8601Utils;

/**
 * GsonConfiguration
 * 
 * @author edhn
 * @version 1.0
 * @date 2021-05-11
 * 
 */
@ConditionalOnClass(name = "com.google.gson.Gson")
public class GsonConfiguration {

    private static final List<DateFormat> dateFormats = new ArrayList<>();
    
    public static Gson gson;
    
    static class GsonDateSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {              
            return new JsonPrimitive(src.getTime());    
        }
    }
    
    static class GsonDateDeserializer implements JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String s = json.getAsString();
            if (s.matches("^\\d+$")) {
                return new Date(Long.valueOf(s));
            }
            for (DateFormat dateFormat : dateFormats) {
                try {
                    return dateFormat.parse(s);
                } catch (ParseException ignored) {
                    //
                }
            }
            try {
                return ISO8601Utils.parse(s, new ParsePosition(0));
            } catch (ParseException e) {
                throw new JsonSyntaxException("Failed parsing '" + s + "' as Date" , e);
            }
        }
    }
    
    static {
        try {
            dateFormats.add(new SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.US));
            dateFormats.add(new SimpleDateFormat("MMM d, yyyy h:mm:ss a"));
            dateFormats.add(new SimpleDateFormat("MMM d, yyyy, h:mm:ss a", Locale.US));
            dateFormats.add(new SimpleDateFormat("MMM d, yyyy, h:mm:ss a"));
            dateFormats.add(new SimpleDateFormat("yyyy-M-d H:mm:ss"));
            
            gson = new GsonBuilder()
            .registerTypeAdapter(java.util.Date.class, new GsonDateSerializer()).setDateFormat(DateFormat.LONG)
            .registerTypeAdapter(java.util.Date.class, new GsonDateDeserializer()).setDateFormat(DateFormat.LONG)
            .create();
        } catch (Exception e) {
            // ignore error when gson package is not imported
        }
    }
    
    
}
