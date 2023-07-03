package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
public class ShopControllerTest {
    @Autowired
    ShopServiceImpl shopService;

    @Test
    public void saveShop2Redis(){
        shopService.saveShop2Redis(1L, 20L);
    }
}
