# redis-extend

[TOC]

## 简介
Redis缓存扩展实现包，为了简化缓存API使用，提供一些方便的特性支持，并兼容多种底层的redis驱动。
主要支持特性有：
* 支持扩展jetcache，使其支持fastjson、gson、jackson的序列化
* 提供简单的RedisSimpleApi，不用关心redis连接管理和具体redis实现组件
* 简化redis锁操作，提供两种锁实现，见`IRedisLockService`、`RedisSimpleApi.tryLock`
* 简化基于Redis的分布式任务实现，提供抽象类：`AbstractRedisLockTaskService`
* 支持缓存批量操作，支持(MLL)缓存+db混合批量加载数据
* 支持基于redis排队的异步写库操作
* 支持redis连接健康检查

可支持的底层redis驱动包括：
1. jetcache 2.6.x
2. spring-data-redis
3. jedis-client

## 如何引入
```xml
    <dependency>
        <groupId>com.edhn.cache</groupId>
        <artifactId>redis-extend-starter</artifactId>
        <version>1.2.0</version>
    </dependency>
```
> 从1.0.10开始，组件不强依赖具体的redis实现包，使用jetcache-starter-redis或spring-boot-starter-data-redis需额外主动引入

## 功能说明

### json序列化配置

```yml
jetcache:
  statIntervalMinutes: 60
  areaInCacheName: false
  local:
    default:
      type: caffeine
      keyConvertor: jackson
  remote:
    default:
      type: redis
      keyConvertor: jackson
      valueEncoder: jackson # valueEncoder和valueDecoder都可使用fastjson、jackson、gson
      valueDecoder: jackson
# ...
```
> 注：如使用gson序列化需额外主动引入gson包

### 使用Redis锁
方式一，使用`IRedisLockService`，默认实现类是`JetCacheLockService`，这是基于jetcache提供的锁实现做的封装。
```java
    @Autowired
    private IRedisLockService lockService;

    // 加锁示例代码，可查看接口详细说明
    lockService.retryLockAndSupply("test-lock", 5000, 10000, ()->{
      log.info("get lock");
      return "ok";
    });
```

方式二，使用`RedisSimpleApi`，这是基于redis分布式锁原理加的一个简单实现
```java
    @Autowired
    private RedisSimpleApi redisApi;
    // 加锁示例代码
    try (Closeable lock = redisApi.tryLock("testSimpleLock", 5000, 10000)) {
        if (lock == null) {
            throw new RuntimeException("lock fail!");
        }
        log.info("SimpleLock加锁测试用例成功");
    }
```

### 通过jetcache获得JedisPool
> 前提是使用了redis版的jetcache组件，启用了redis配置

```java
// 声明了一个Cache
@CreateCache(name = "testCache.", cacheType = CacheType.REMOTE, expire = 600)
private Cache<String, String> testCache;

// 通过上面声明的Cache获得JedisPool
JedisCacheUnWrapper cacheUnWrapper = new JedisCacheUnWrapper(testCache);
byte[] doInRedis = cacheUnWrapper.doInRedis(redis->{
  redis.set("abc".getBytes(), "value123".getBytes());
  return redis.get("abc".getBytes());
});
log.info("value:{}", new String(doInRedis));
```

### 简单Redis操作Api(RedisSimpleApi)

#### 批量操作
```java
  // 注入RedisSimpleApi
  @Autowired
  private RedisSimpleApi redisApi;

// ===== 批量操作示例代码=====
Map<String, String> datas = new HashMap<>(); // 组织数据
// 批量写入
int num = redisApi.batchSet(datas);
log.info("batch set datas {}, result {}", datas.size(), num);
// 批量读取
Map<String, String> values = redisApi.batchGet(loadKeys, String.class);
log.info("batch get values:{}", values);

```

#### computeIfAbsent操作
> 参照java中map的computeIfAbsent方法原理，当key不存在时自动触发加载函数，并将加载结果写入缓存，内部使用锁保证线程安全
```java
  // 注入RedisSimpleApi
  @Autowired
  private RedisSimpleApi redisApi;

  redisApi.computeIfAbsent(key, k->{
                return mapper.load(k); // 加载数据，仅当key不存在时执行
            });
```

#### 获取具体jedis连接
```java
  // 注入RedisSimpleApi
  @Autowired
  private RedisSimpleApi redisApi;

  redisApi.unwrap().doInRedis(redis->{
  //  do in redis
  });

  redisApi.unwrap().doInPipeline(pipeline->{
  //  do in pipeline
  });

```

### 多级数据加载层（Multi-Level-LoadLayer）
支持缓存+DB混合批量加载数据，兼顾批量加载与缓存的优势，解决多层加载复杂度，详见`MultiLevelLoadLayer`多级批量数据加载层。

举例使用redis缓存+db组成混合加载层，执行混合加载时，优先从redis批量加载数据，对缓存未命中的部分可自动降到DB层批量加载，从DB加载到的数据可批量写入缓存以加快下次加载速度。

```java
// 获得jedis客户端
JedisCacheUnWrapper cacheUnWrapper = new JedisCacheUnWrapper(testCache);
// 初始化，为一类需要批量加载的数据初始化混合加载逻辑
MultiLevelLoadLayer loadLayer = new MultiLevelLoadLayer<IUser>(
        new RedisMultiLevelBatchLoader<IUser>(new JedisSimpleApiImpl(cacheUnWrapper)) {}, 
        new IMultiLevelBatchLoader<IUser>() {
            @Override
            public Map<String, IUser> batchGetData(Collection<String> ids) {
                return organService.getUsers(ids);
            }
        });
// 执行加载
loadLayer.batchLoadData(userIds);
```
