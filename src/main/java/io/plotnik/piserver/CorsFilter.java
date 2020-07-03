package io.plotnik.piserver;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, PUT, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers",
                "x-auth-token, x-requested-with, authorization, content-type");
        if (!request.getMethod().equals("OPTIONS")) {
            memoryInfo(request);
            chain.doFilter(req, res);
        }
    }

    private void memoryInfo(HttpServletRequest request) {
        long heapSize = mb(Runtime.getRuntime().totalMemory());
        long heapMaxSize = mb(Runtime.getRuntime().maxMemory());
        long heapFreeSize = mb(Runtime.getRuntime().freeMemory());
        String uri = request.getRequestURI();
        System.out.println(
                "[memoryInfo] uri=" + uri + ", free=" + heapFreeSize + "mb, total=" + heapSize + "mb, max=" + heapMaxSize + "mb");
    }

    private long mb(long x) {
        return x/(1024*1024);
    }

}