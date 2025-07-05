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
            "/api/files/**");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 检查请求路径是否在允许匿名访问的路径列表中
        String requestPath = request.getServletPath();
        logger.debug("Processing request for path: " + requestPath);

        boolean isPermitAllPath = permitAllPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));

        // 如果是允许匿名访问的路径，则跳过JWT验证
        if (isPermitAllPath) {
            logger.debug("Permitting anonymous access to: " + requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");
        logger.debug("Authorization header: " + (authorizationHeader != null ? "present" : "absent"));

        // 如果没有Authorization头，直接放行请求
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.debug("No valid Bearer token found, continuing filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        // 打印完整的Authorization头以进行调试
        logger.debug("Authorization header value: " + authorizationHeader);

        String username = null;
        String jwt = null;

        jwt = authorizationHeader.substring(7);
        logger.debug("JWT token extracted, length: " + jwt.length());

        try {
            username = jwtUtil.extractUsername(jwt);
            logger.debug("Extracted username from JWT: " + username);

            if (username != null) {
                logger.debug("Loading user details for: " + username);
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.debug("User details loaded successfully");

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Authentication successful for user: " + username);
                } else {
                    logger.debug("Token validation failed for user: " + username);
                }
            } else {
                logger.debug("No username could be extracted from token");
            }
        } catch (Exception e) {
            logger.error("JWT token validation error", e);
        }

        filterChain.doFilter(request, response);
    }
}