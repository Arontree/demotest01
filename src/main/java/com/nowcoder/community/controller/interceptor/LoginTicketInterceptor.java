package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.utils.CookieUtil;
import com.nowcoder.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private UserService userService;

   @Autowired
   private HostHolder hostHolder;

   //在这个拦截器这里会认证并授予权限
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从cookie中获取凭证
        String ticket = cookieUtil.getValue(request,"ticket");

        if(ticket!=null){
            //查询凭证
            LoginTicket loginTicket=userService.findLoginTicket(ticket);
            //检查凭证是否有效
            if(loginTicket!=null && loginTicket.getStatus()==0 && loginTicket.getExpired().after(new Date())){
                //根据凭证查询用户
                User user=userService.findUserById(loginTicket.getUserId());
                //在本次请求中持有用户
               hostHolder.setUsers(user);
               // 构建用户认证的结果，并存入securityContext,以便于security进行授权
                // principal: 主要信息; credentials: 证书(登录密码凭证); authorities: 权限;
                Authentication authentication=new UsernamePasswordAuthenticationToken(user,
                        user.getPassword(),userService.getAuthorities(user.getId()));
                // 它将一个包含认证信息的 SecurityContext 对象存储在当前线程的 ThreadLocal 中，以便在整个应用程序中访问该信息。
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user=hostHolder.getUser();
        if(user!=null && modelAndView !=null){
            modelAndView.addObject("loginUser",user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
hostHolder.clear();
// 清除认证信息
        SecurityContextHolder.clearContext();
    }
}
