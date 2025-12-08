package com.synapsetest.testmanagement.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Logging Interceptor
 * Logs all incoming HTTP requests
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        logger.info("Request: {} {} from {}",
            request.getMethod(),
            request.getRequestURI(),
            request.getRemoteAddr()
        );

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long executeTime = endTime - startTime;

        logger.info("Response: {} {} - Status: {} - Time: {}ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            executeTime
        );

        if (ex != null) {
            logger.error("Request failed with exception", ex);
        }
    }
}
