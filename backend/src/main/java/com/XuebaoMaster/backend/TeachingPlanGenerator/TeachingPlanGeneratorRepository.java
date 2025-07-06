package com.XuebaoMaster.backend.TeachingPlanGenerator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeachingPlanGeneratorRepository extends JpaRepository<TeachingPlanGenerator, Long> {
    // 可以根据需要添加自定义查询方法
}