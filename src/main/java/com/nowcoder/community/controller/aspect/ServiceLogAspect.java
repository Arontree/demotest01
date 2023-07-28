package com.nowcoder.community.controller.aspect;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/*@Component
@Aspect*///当前类为切面类
public class ServiceLogAspect {

    //用于在Java代码中创建一个日志记录器的常见用法。它使用了SLF4J（Simple Logging Facade for Java）日志框架来实现日志记录功能。
    private static final Logger Logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    // 通过表达式的方式定位一个或多个具体的连接点
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){

    }

    @Before("pointcut()")
    //JoinPoint是AspectJ中的一个概念，用于表示在程序执行过程中切面可以插入的点。
    // 它可以是方法的调用、方法的执行、字段的访问、异常的抛出等。
    public void before(JoinPoint joinPoint){
        //目标输出： 用户[1.2.3.4],在[xxx],访问了[com.nowcoder.community.service.xxx()].
        //ServletRequestAttributes是Spring框架中的一个类，用于提供对当前请求的访问和操作。它是ServletRequest和ServletResponse的包装类，提供了访问请求和响应对象的方法。
        //可以使用RequestContextHolder类来获取当前请求的ServletRequestAttributes对象。
        ServletRequestAttributes attributes=(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes==null){
            return;
        }
        HttpServletRequest request=attributes.getRequest();
        String ip=request.getRemoteHost();
        String now=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        //获取目标对象的类名+方法名
        String target=joinPoint.getSignature().getDeclaringTypeName()+"."+joinPoint.getSignature().getName();
        Logger.info(String.format("用户[%s],在[%s],访问了[%s].",ip,now,target));
    }
}
