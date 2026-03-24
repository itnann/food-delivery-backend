package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    private static final String KEY = "SHOP_STATUS";
    private static final String phone = "19949262615";

    @Autowired
    private RedisTemplate redisTemplate;



    /**
     * 获取营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取营业状态")
    public Result getStatus(){
        Integer shop_status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取营业状态，状态为：{}",shop_status == 1 ? "营业中" : "打烊中");
        return Result.success(shop_status);
    }

    /**
     * 获取店铺电话
     * @return
     */
    @GetMapping("/phone")
    @ApiOperation("获取店铺电话")
    public Result<String> getShopPhone(){
        return Result.success(phone);
    }
}
