package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 生成验证码
     * @param phone 用户手机号码
     * @return
     */
    @Override
    public Result sendCode(String phone) {
        //校验手机号是否正确
        boolean isInValid = RegexUtils.isPhoneInvalid(phone);
        //错误则返回手机号码不正确
        if (isInValid){
            return Result.fail("手机号码格式不对");
        }
        //随机生成六位验证码，打印到控制台
        String code = RandomUtil.randomString(6);
        log.debug(code);
        redisTemplate.opsForValue().set(phone,code);
        //设置验证码的过期时间为 5 分钟
        redisTemplate.expire(phone,5,TimeUnit.MINUTES);
        return Result.ok();
    }

    /**
     * 实现用户登录的功能校验（用户可能采用验证码登录或密码登录）
     * @param loginForm 登录的用户信息
     * @return Result
     */
    @Override
    public Result login(LoginFormDTO loginForm) {
        //判断用户输入的手机号码是否合法
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //如果不合法，返回手机号格式错误信息
            return Result.fail("手机号格式错误");
        }
        //判断验证码是否正确
        String code = loginForm.getCode();
        String redisCode = redisTemplate.opsForValue().get(phone);
        if (code == null || !code.equals(redisCode)){
            //如果验证码不相等，返回验证码错误信息
            return Result.fail("验证码错误");
        }
        //判断用户是否已经注册，未注册则完成注册
        User user = query().eq("phone", phone).one();
        if (user == null){
            //用户未注册
            user = new User();
            user.setPhone(loginForm.getPhone());
            //随机生成userID
            String randomId = RandomUtil.randomString(10);
            user.setNickName("user_"+randomId);
            //保存到数据库
            save(user);
        }
        //用户信息存入 redis 中
        //随机生成token，作为登陆令牌
        String token = UUID.randomUUID().toString();
        //将 User 对象转换成 HashMap 存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 存储
        String tokenKey = LOGIN_USER_KEY + token;
        redisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 7.4.设置token有效期
        redisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 8.返回token
        return Result.ok(token);




/*        //1.将用户输入的验证码与 session 中存入的验证码进行比较
        String loginCode = loginForm.getCode();
        String sessionCode = (String) session.getAttribute("code");

        if (loginCode == null){
            return Result.fail("请输入验证码");
        }
        //1.1 如果不相等返回验证码输入错误提示
        if (!loginCode.equals(sessionCode)){
            return Result.fail("验证码错误");
        }
        //2.检查用户是否已经注册，如果未注册则创建数据库
        //2.1 从数据库中查询该手机号码
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, loginForm.getPhone());
        User user = getOne(queryWrapper);
        //2.2 如果 user 为空，则创建用户，并存入 session 中
        if (user == null){
            User newUser = new User();
            newUser.setPhone(loginForm.getPhone());
            //随机生成userID
            String randomId = RandomUtil.randomString(10);
            newUser.setNickName("user_"+randomId);
            //保存到数据库
            save(newUser);
            session.setAttribute("user", newUser);
            return Result.ok();
        }
        //2.3 反之直接存入 session
        session.setAttribute("user", user);
        return Result.ok();*/
    }
}
