package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
 /*       //获取 session
        HttpSession session = request.getSession();
        //检查 session 中的用户是否存在
        User user = (User) session.getAttribute("user");
        if (user == null){
            //user 不存在，拦截，返回状态码 401
            response.setStatus(401);
            return false;
        }
        //存在，保存用户信息到 ThreadLocal
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        UserHolder.saveUser(userDTO);
        //放行*/

        //判断是否需要拦截（根据 ThredLoacl 中是否有用户）
        if(UserHolder.getUser() == null){
            //没有用户则拦截，设置状态码为 401
            response.setStatus(401);
            return false;
        }
        //放行
        return true;
    }
}
