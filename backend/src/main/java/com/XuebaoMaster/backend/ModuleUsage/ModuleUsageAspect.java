package com.XuebaoMaster.backend.ModuleUsage;

import com.XuebaoMaster.backend.User.CustomUserDetailsService.CustomUserPrincipal;
import com.XuebaoMaster.backend.User.User;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ModuleUsageAspect {

    private static final Logger logger = LoggerFactory.getLogger(ModuleUsageAspect.class);

    private final ModuleUsageService moduleUsageService;

    @Autowired
    public ModuleUsageAspect(ModuleUsageService moduleUsageService) {
        this.moduleUsageService = moduleUsageService;
        logger.info("ModuleUsageAspect 初始化成功");
    }

    @Before("execution(* com.XuebaoMaster.backend.Course.CourseController.*(..))")
    public void trackCourseModuleUsage(JoinPoint joinPoint) {
        logger.info("检测到课程模块操作: {}", joinPoint.getSignature().getName());
        recordModuleUsage(ModuleUsage.ModuleType.COURSE, joinPoint);
    }

    @Before("execution(* com.XuebaoMaster.backend.SchoolClass.SchoolClassController.*(..))")
    public void trackSchoolClassModuleUsage(JoinPoint joinPoint) {
        logger.info("检测到班级模块操作: {}", joinPoint.getSignature().getName());
        recordModuleUsage(ModuleUsage.ModuleType.SCHOOL_CLASS, joinPoint);
    }

    @Before("execution(* com.XuebaoMaster.backend.TeachingPlanGenerator.TeachingPlanGeneratorController.*(..))")
    public void trackTeachingPlanGeneratorModuleUsage(JoinPoint joinPoint) {
        logger.info("检测到教案生成器模块操作: {}", joinPoint.getSignature().getName());
        recordModuleUsage(ModuleUsage.ModuleType.TEACHING_PLAN_GENERATOR, joinPoint);
    }

    @Before("execution(* com.XuebaoMaster.backend.DeepSeekChat.DeepSeekChatController.*(..))")
    public void trackDeepSeekChatModuleUsage(JoinPoint joinPoint) {
        logger.info("检测到智能助手模块操作: {}", joinPoint.getSignature().getName());
        recordModuleUsage(ModuleUsage.ModuleType.DEEP_SEEK_CHAT, joinPoint);
    }

    private void recordModuleUsage(ModuleUsage.ModuleType moduleType, JoinPoint joinPoint) {
        try {
            // Get the current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            logger.info("当前认证信息: {}", authentication);

            if (authentication == null) {
                logger.warn("未找到认证信息");
                return;
            }

            if (!authentication.isAuthenticated()) {
                logger.warn("用户未认证");
                return;
            }

            if ("anonymousUser".equals(authentication.getPrincipal())) {
                logger.warn("匿名用户，不记录操作");
                return;
            }

            Object principal = authentication.getPrincipal();
            logger.info("认证主体类型: {}", principal.getClass().getName());

            if (principal instanceof CustomUserPrincipal) {
                User user = ((CustomUserPrincipal) principal).getUser();
                logger.info("记录用户 [{}] 的 [{}] 模块操作: {}",
                        user.getUsername(),
                        moduleType,
                        joinPoint.getSignature().getName());

                moduleUsageService.recordModuleUsage(user, moduleType);
                logger.info("操作记录保存成功");
            } else {
                logger.warn("无法获取用户信息，认证主体不是预期类型: {}", principal.getClass().getName());
                // 输出认证主体的详细信息，帮助调试
                logger.info("认证主体详情: {}", principal);
            }
        } catch (Exception e) {
            logger.error("记录模块使用情况时出错", e);
        }
    }
}