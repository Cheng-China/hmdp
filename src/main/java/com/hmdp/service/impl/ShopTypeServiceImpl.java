package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_LIST;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    /**
     *缓存分类列表
     * 首先查询缓存，没有命中则查询数据库，再加入缓存
     * @return 返回主页列表
     */
    @Override
    public List<ShopType> queryTypeList() {
        //1. 查询redis缓存并转换成对象
        //1.1查询缓存
        String typeListString = stringRedisTemplate.opsForValue().get(CACHE_LIST);
        //将字符串转换成对象
        if (typeListString != null){
            //2. 判断是否为空
            List<ShopType> typeList = JSONUtil.toList(typeListString, ShopType.class);
            //2.1 不等于空直接返回
            return typeList;
        }
        //2.2 查询数据库，返回结果集，并加入 redis 缓存，设置过期时间
        List<ShopType> typeList = query().orderByAsc("sort").list();
        typeListString = JSONUtil.toJsonStr(typeList);
        stringRedisTemplate.opsForValue().set(CACHE_LIST, typeListString, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return typeList;
    }
}
