package com.XuebaoMaster.backend.security;

import com.XuebaoMaster.backend.User.CustomUserDetailsService;
import com.XuebaoMaster.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> permitAllPaths = Arrays.asList(
            "/users/register",
            "/users/login",
            "/api/auth/**",
            "/api/files/**");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = request.getServletPath();
        logger.debug("处理请求路径: " + requestPath);
        boolean isPermitAllPath = permitAllPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
        if (isPermitAllPath) {
            logger.debug("允许匿名访问: " + requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        final String authorizationHeader = request.getHeader("Authorization");
        logger.debug("Authorization 头部: " + (authorizationHeader != null ? "存在" : "不存在"));
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.debug("没有找到有效的 Bearer token，继续过滤链");
            filterChain.doFilter(request, response);
            return;
        }
        logger.debug("Authorization 头部值: " + authorizationHeader);
        String username = null;
        String jwt = null;
        jwt = authorizationHeader.substring(7);
        logger.debug("提取的 JWT token，长度: " + jwt.length());
        try {
            username = jwtUtil.extractUsername(jwt);
            logger.debug("从 JWT 提取的用户名: " + username);
            if (username != null) {
                logger.debug("加载用户 " + username + " 的详细信息");
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.debug("用户详细信息加载成功，类型: " + userDetails.getClass().getName());

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    logger.debug("创建认证令牌，主体类型: " + userDetails.getClass().getName());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("用户 " + username + " 认证成功");

                    // 打印Security上下文信息
                    logger.debug("当前认证信息: {}", SecurityContextHolder.getContext().getAuthentication());
                    logger.debug("认证主体: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
                } else {
                    logger.warn("用户 " + username + " 的令牌验证失败");
                }
            } else {
                logger.warn("无法从令牌提取用户名");
            }
        } catch (Exception e) {
            logger.error("JWT 令牌验证错误", e);
        }
        filterChain.doFilter(request, response);
    }
}
