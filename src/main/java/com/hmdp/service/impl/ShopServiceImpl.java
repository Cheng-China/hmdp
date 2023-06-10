package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 *  服务实现类
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
        //首先查询缓存
        String key = LOCK_SHOP_KEY + id;
        String shopString = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopString)){
            //存在，返回结果
            Shop shop = JSONUtil.toBean(shopString, Shop.class);
            return Result.ok(shop);
        }
        //不存在，从数据库查询并加入缓存
        Shop shop = getById(id);
        stringRedisTemplate.opsForValue().set(LOCK_SHOP_KEY+id, JSONUtil.toJsonStr(shop));
        return Result.ok(shop);
    }
}
