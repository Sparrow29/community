package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class) // 表示该类是controller的全局配置类
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 记录日志
        logger.error("服务器发生异常: " + e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }
        // 判断是否为异步请求
        String xRequestWith = request.getHeader("x-requested-with");
        // 异步请求
        if ("XMLHttpRequest".equals(xRequestWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtils.getJSONString(1, "服务器异常!"));
        } else {
            // 普通请求
            response.sendRedirect(request.getContextPath() + "/error");
        }

    }

}
