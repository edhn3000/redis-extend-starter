package com.edhn.cache.redis.test.cases;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.util.Assert;

import com.alicp.jetcache.support.AbstractValueDecoder;
import com.alicp.jetcache.support.AbstractValueEncoder;
import com.alicp.jetcache.support.JavaValueDecoder;
import com.alicp.jetcache.support.JavaValueEncoder;
import com.edhn.cache.redis.configuration.GsonConfiguration;
import com.edhn.cache.redis.serializer.FastjsonValueDecoder;
import com.edhn.cache.redis.serializer.FastjsonValueEncoder;
import com.edhn.cache.redis.serializer.GsonValueDecoder;
import com.edhn.cache.redis.serializer.GsonValueEncoder;
import com.edhn.cache.redis.serializer.IObjectSerializer;
import com.edhn.cache.redis.serializer.JacksonValueDecoder;
import com.edhn.cache.redis.serializer.JacksonValueEncoder;
import com.edhn.cache.redis.serializer.impl.JacksonObjectSerializer;
import com.edhn.cache.redis.test.model.TestBean;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RedisCacheValueParserTest extends AbstractTestCase {
    
    private static int times = 50000;
    private static TestBean bean;
    
    /**
     * for gson
     */
    public static Gson gson;
    
    /**
     * for jackson
     */
    static ObjectMapper objectMapper;
    
    private static byte[] beanJsonBytes;
    private static byte[] beanJavaBytes;
    
    
//    private static Map<String, Class<?>> clazzCache = new ConcurrentHashMap<>();
    
    @BeforeClass
    public static void init() {
        gson = GsonConfiguration.gson;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        bean = new TestBean();
        bean.setId("appid123");
        bean.setName("app名称");
        bean.setCreateTime(new Date());
        bean.setNum(2);
//        GsonValueEncoder gsonValueEncoder = new GsonValueEncoder(false, gson);
        JavaValueEncoder javaValueEncoder = new JavaValueEncoder(false);
        JacksonValueEncoder jacksonValueEncoder = new JacksonValueEncoder(false, objectMapper);
        beanJsonBytes = jacksonValueEncoder.apply(bean);
        beanJavaBytes = javaValueEncoder.apply(bean);
    }
    
    /**
     * 性能记录方法
     * @param <T>
     * @param runable
     * @param testSubject
     * @return
     */
    protected <T> long logPerform(Runnable runable, String testSubject) {
        return super.logPerform(runable, times, testSubject);
    }
    
//    private Class<?> getClass(String className, boolean useCache) {
//        if (useCache) {
//            return clazzCache.computeIfAbsent(className, k->{
//                try {
//                    return Class.forName(k);
//                } catch (ClassNotFoundException e) {
//                    return Object.class;
//                }
//            });
//        } else {
//            try {
//                return Class.forName(className);
//            } catch (ClassNotFoundException e) {
//                return null;
//            }
//        }
//    }
    
    private void testJsonCorrectInner(String testName, AbstractValueEncoder encoder, AbstractValueDecoder decoder) {
        byte[] encodeByjackson = encoder.apply(bean);
        Object beanDecode = decoder.apply(encodeByjackson);
        Assert.isTrue(bean.getClass().equals(beanDecode.getClass()), testName + "序列化 + 反序列化后，对象类型错误！");
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field f: fields) {
            try {
                f.setAccessible(true);
                Object value1 = f.get(bean);
                Object value2 = f.get(beanDecode);
                Assert.isTrue(Objects.equals(value1, value2), 
                    testName + "序列化 + 反序列化后，属性（" + f.getName() + "）的值错误");
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("反射出错！", e);
            }
        }
    }
    
    private void testJsonCorrectInner(String testName, IObjectSerializer serializer) throws Exception {
        String value = serializer.serializeObject(bean);
        TestBean beanDecode = serializer.deserializeObject(value, bean.getClass());
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field f: fields) {
            try {
                f.setAccessible(true);
                Object value1 = f.get(bean);
                Object value2 = f.get(beanDecode);
                Assert.isTrue(Objects.equals(value1, value2), 
                    testName + "序列化 + 反序列化后，属性（" + f.getName() + "）的值错误");
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("反射出错！", e);
            }
        }
        
        // test normal class
        String strValue = serializer.serializeObject("abc");
        String strValueDecode = serializer.deserializeObject(strValue, String.class);
        Assert.isTrue(strValue.equals(strValueDecode), "String 序列化反序列化不相等");
        
        String intValue = serializer.serializeObject(1234567);
        int intValueDecode = serializer.deserializeObject(intValue, int.class);
        Assert.isTrue(Integer.valueOf(intValue).equals(intValueDecode), "Integer 序列化反序列化不相等");
        
        if (serializer instanceof JacksonObjectSerializer) {
            JacksonObjectSerializer jacksonSeria = (JacksonObjectSerializer) serializer;
            strValue = jacksonSeria.serializeObject("abcd");    
            strValueDecode = jacksonSeria.deserializeObject(strValue, new TypeReference<String>() {});
            Assert.isTrue(strValue.equals(strValueDecode), "String TypeReference 序列化反序列化不相等");

            intValue = jacksonSeria.serializeObject(12345678);
            intValueDecode = jacksonSeria.deserializeObject(intValue, new TypeReference<Integer>() {});
            Assert.isTrue(Integer.valueOf(intValue).equals(intValueDecode), "Integer TypeReference 序列化反序列化不相等");
        }
    }
    
    
    @Test
    public void test001_Date() throws Exception {
        String json = "{"
                + "\"createTime\" : \"Apr 23, 2023 23:44:52 AM\","
                + "\"updateTime\" : 1682328649341}";
        TestBean fromJson = gson.fromJson(json, TestBean.class);
        log.info("json parse result:{}", fromJson);
        
    }
    
    @Test
    public void test1_jsonCorrect() throws Exception {
        AbstractValueEncoder encoder = new JacksonValueEncoder(false, objectMapper);
//        log.debug("正确性测试，json内容：{}", new String(encodeByjackson, StandardCharsets.UTF_8));
        AbstractValueDecoder decoder = new JacksonValueDecoder(false, objectMapper);
        testJsonCorrectInner("jackson", encoder, decoder);
        log.info("jackson序列化正确性测试通过！");

        encoder = new GsonValueEncoder(false);
        decoder = new GsonValueDecoder(false);
        testJsonCorrectInner("gson", encoder, decoder);
        log.info("gson序列化正确性测试通过！");

        encoder = new FastjsonValueEncoder(false);
        decoder = new FastjsonValueDecoder(false);
        testJsonCorrectInner("fastjson", encoder, decoder);
        
        IObjectSerializer serializer = new JacksonObjectSerializer();
        testJsonCorrectInner("jacksonSerializer", serializer);
        
        log.info("fastjson序列化正确性测试通过！");
    }
    
    private void testJsonCompatibilityInner(String testName, AbstractValueEncoder encoder, AbstractValueDecoder... decoders) {
        byte[] encodeByjackson = encoder.apply(bean);
        for (AbstractValueDecoder decoder: decoders) {
            Object beanDecode2 = decoder.apply(encodeByjackson);
            Assert.isTrue(bean.getClass().equals(beanDecode2.getClass()), String.format("%s序列化 + %s反序列化后，对象类型错误！", 
                encoder.getClass().getSimpleName(), decoder.getClass().getSimpleName()));

            Field[] fields = bean.getClass().getDeclaredFields();
            for (Field f: fields) {
                try {
                    f.setAccessible(true);
                    Object value1 = f.get(bean);
                    Object value2 = f.get(beanDecode2);
                    Assert.isTrue(Objects.equals(value1, value2), 
                        String.format("%s序列化 + %s反序列化后，属性（" + f.getName() + "）的值错误",
                        encoder.getClass().getSimpleName(), decoder.getClass().getSimpleName()));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    log.error("反射出错！", e);
                }
            }
        }
    }
    
    
    @Test
    public void test2_JsonCompatibility() {
        testJsonCompatibilityInner("jackson(fastjson,gson)", new JacksonValueEncoder(false, objectMapper), 
            new FastjsonValueDecoder(false), new GsonValueDecoder(false));
        testJsonCompatibilityInner("fastjson(jackson,gson)", new FastjsonValueEncoder(false), 
            new JacksonValueDecoder(false, objectMapper), new GsonValueDecoder(false));
        testJsonCompatibilityInner("gson(fastjson,jackson)", new GsonValueEncoder(false), 
            new FastjsonValueDecoder(false), new JacksonValueDecoder(false, objectMapper));
        
        log.info("json序列化兼容性测试通过！");
    }
    
    
    @Test
    public void test9_Perform() {
        /////////// encode 
        Comparator<String> comp = (s1, s2)-> {
            return Integer.parseInt(s1.substring(0, s1.indexOf("_"))) - Integer.parseInt(s2.substring(0, s2.indexOf("_")));};
        SortedSet<String> orders = new TreeSet<>(comp);
        JacksonValueEncoder jacksonValueEncoder = new JacksonValueEncoder(false, objectMapper);
        orders.add(logPerform(()->jacksonValueEncoder.apply(bean), "jackson encode") + "_jackson");
        
        GsonValueEncoder gsonValueEncoder = new GsonValueEncoder(false);
        orders.add(logPerform(()->gsonValueEncoder.apply(bean), "gson encode") + "_gson");
        
        FastjsonValueEncoder fastjsonValueEncoder = new FastjsonValueEncoder(false);
        fastjsonValueEncoder.apply(bean); // fastjson 预热
        orders.add(logPerform(()->fastjsonValueEncoder.apply(bean), "fastjson encode") + "_fastjson");
        
        JavaValueEncoder javaValueEncoder = new JavaValueEncoder(false);
        orders.add(logPerform(()->javaValueEncoder.apply(bean), "java encode") + "_java");
        log.info("序列化性能测试完毕！耗时顺序={}", orders.stream().map(s->s.substring(s.indexOf("_")+1)).collect(Collectors.toList()));
        
        /////////// decode
        orders = new TreeSet<>(comp);
        JacksonValueDecoder jacksonValueDecoder = new JacksonValueDecoder(false, objectMapper);
        orders.add(logPerform(()->jacksonValueDecoder.apply(beanJsonBytes), "jackson decode") + "_jackson");;
        
        GsonValueDecoder gsonValueDecoder = new GsonValueDecoder(false);
        orders.add(logPerform(()->gsonValueDecoder.apply(beanJsonBytes), "gson decode") + "_gson");;

        FastjsonValueDecoder fastjsonValueDecoder = new FastjsonValueDecoder(false);
        fastjsonValueDecoder.apply(beanJsonBytes); // fastjson 预热
        orders.add(logPerform(()->fastjsonValueDecoder.apply(beanJsonBytes), "fastjson decode") + "_fastjson");;
        
        JavaValueDecoder javaValueDecoder = new JavaValueDecoder(false);
        orders.add(logPerform(()->javaValueDecoder.apply(beanJavaBytes), "java decode") + "_java");;
        
        log.info("反序列化性能测试完毕！耗时顺序={}", orders.stream().map(s->s.substring(s.indexOf("_")+1)).collect(Collectors.toList()));

        /////////// test reflect
//        logPerform(() -> getClass("com.edhn.cache.jetcache.JetCacheValueParserTest.TestBean", false), "get class reflect");
//        logPerform(()->getClass("com.edhn.cache.jetcache.JetCacheValueParserTest.TestBean", true), "get class with map");
    }

}
