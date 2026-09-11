package com.murong.ecp.tools.fx.infrastructure.web;

import com.murong.ecp.tools.fx.infrastructure.rpc.MemoryHttpHeaders;
import com.murong.ecp.tools.fx.infrastructure.rpc.ServiceRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ServiceRequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ServiceRequestContext.Context context = new ServiceRequestContext.Context();
        context.setUserId(request.getHeader(MemoryHttpHeaders.USER_ID));
        context.setUsername(request.getHeader(MemoryHttpHeaders.USERNAME));
        context.setRealName(request.getHeader(MemoryHttpHeaders.REAL_NAME));
        context.setRoles(request.getHeader(MemoryHttpHeaders.ROLES));
        context.setGroupName(request.getHeader(MemoryHttpHeaders.GROUP_NAME));
        context.setProjectName(request.getHeader(MemoryHttpHeaders.PROJECT_NAME));
        context.setAppName(request.getHeader(MemoryHttpHeaders.APP_NAME));
        ServiceRequestContext.set(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            ServiceRequestContext.clear();
        }
    }
}
