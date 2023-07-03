package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.Lock;
import io.netty.util.internal.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.Lock.tryLock;
import static com.hmdp.utils.Lock.unlock;
import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
       /* //首先查询缓存
        String key = CACHE_SHOP_KEY + id;
        String shopString = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopString)) {
            //存在，返回结果
            Shop shop = JSONUtil.toBean(shopString, Shop.class);
            return Result.ok(shop);
        }
        //不存在，从数据库查询并加入缓存，并设置缓存时间
        Shop shop = getById(id);
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), 10, TimeUnit.MINUTES);*/

        //利用互斥锁解决缓存击穿问题
         Shop shop = queryWithMutex(id);

        //利用逻辑过期来解决缓存击穿问题
        //Shop shop = logicalExpire(id);

        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        //判断 id 是否为空
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺 id 不能为空");
        }
        //更新数据库
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok("更新成功");
    }

    //存储店铺数据到 redis
    public void saveShop2Redis(Long id, Long expireSeconds){
        //查询店铺数据
        Shop shop = getById(id);
        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    public Shop queryWithMutex(Long id) {
        //从缓存中查询店铺
        Shop shop = null;
        String jsonShop = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //判断是否命中
        if (jsonShop == null || "".equals(jsonShop)) {
            //未命中，尝试获取锁,获取失败等待一段时间继续获取
            try {
                boolean flag = tryLock(stringRedisTemplate, LOCK_SHOP_KEY + id);
                if (!flag) {
                    //获取锁失败
                    Thread.sleep(50);
                    queryWithMutex(id);
                }
                //获取锁成功
                //获取锁成功后还要再次检测缓存是否存在
                String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
                if (shopJson == null || "".equals(shopJson)){
                    //查询店铺，加入缓存
                    shop = getById(id);
                    stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop) ,CACHE_SHOP_TTL, TimeUnit.MINUTES);
                }else {
                    shop = JSONUtil.toBean(jsonShop, Shop.class);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }finally {
                unlock(stringRedisTemplate, LOCK_SHOP_KEY + id);
            }
            return shop;
        }
        //缓存中命中
        shop = JSONUtil.toBean(jsonShop, Shop.class);
        return shop;
    }

    //线程池配置
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 利用逻辑过期来处理缓存击穿问题
     * @param id 店铺id
     * @return shop 放回店铺
     */
    public Shop logicalExpire(Long id){
        String key = CACHE_SHOP_KEY + id;
        //从 redis 中查询店铺缓存
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        //判断缓存是否为空
        if (StrUtil.isBlank(jsonStr)){
            return null;
        }

        //命中，先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(jsonStr, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        //判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //未过期
            return shop;
        }

        //已经过期，重建缓存
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean flag = tryLock(stringRedisTemplate, lockKey);

        //判断是否获取锁成功
        if (flag){
            //获取成功，开启新线程重建缓存
            CACHE_REBUILD_EXECUTOR.submit( ()->{
                try{
                    //重建缓存
                    this.saveShop2Redis(id,20L);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unlock(stringRedisTemplate ,lockKey);
                }
            });
        }

        //返回过期店铺信息
        return shop;
    }
}
