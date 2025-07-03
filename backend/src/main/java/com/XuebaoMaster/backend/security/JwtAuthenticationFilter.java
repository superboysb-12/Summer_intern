package com.XuebaoMaster.backend.security;

import com.XuebaoMaster.backend.User.CustomUserDetailsService;
import com.XuebaoMaster.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 定义允许匿名访问的路径
    private final List<String> permitAllPaths = Arrays.asList(
        "/users/register", 
        "/users/login",
        "/api/files/**",
        "/lesson-nodes/generate-rag",
        "/lesson-nodes/generate-rag-with-path",
        "/lesson-nodes/query-rag",
        "/lesson-nodes/*/query-rag",
        "/lesson-nodes/chat"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 检查请求路径是否在允许匿名访问的路径列表中
        String requestPath = request.getServletPath();
        boolean isPermitAllPath = permitAllPaths.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
        
        // 如果是允许匿名访问的路径，则跳过JWT验证
        if (isPermitAllPath) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("JWT token validation error", e);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}