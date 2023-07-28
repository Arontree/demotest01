package com.nowcoder.community.controller.advice;

import com.nowcoder.community.utils.CommunityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class)//只扫描controller组件，annotation注释
public class ExceptionAdvice {
    private  static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler(Exception.class)
    //Exception e表示捕获的异常对象，
    // HttpServletResponse response表示响应对象，
    // HttpServletRequest request表示请求对象。
    //当一个方法声明使用了 throws IOException，意味着该方法可能会抛出 IOException 异常。
    // IOException 是 Java 中的一个受检异常，表示输入输出操作可能会发生错误。
    //当一个方法可能会发生输入输出错误时，使用 throws IOException 可以将异常的处理责任交给调用该方法的代码。这样的设计可以使代码更加模块化和可维护。
    public void hadndleException(Exception e, HttpServletResponse response, HttpServletRequest request) throws IOException{
        logger.error("服务器发生异常: " + e.getMessage());//将字符串 "服务器发生异常: " 和异常对象 e 的错误消息连接起来，然后将整个字符串作为错误消息记录到日志中。
        for (StackTraceElement element:e.getStackTrace()){
            logger.error(element.toString());
        }
//"x-requested-with"是一个常见的HTTP请求头字段，通常用于标识请求的类型或来源。
// 在前端开发中，它常用于判断请求是普通的页面请求还是Ajax请求。
        String xRequestedWith=request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)){
            //如果是AJAX请求
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer=response.getWriter();
            writer.write(CommunityUtils.getJSONString(1, "服务器异常!"));
        }else {
            //如果是普通请求
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
