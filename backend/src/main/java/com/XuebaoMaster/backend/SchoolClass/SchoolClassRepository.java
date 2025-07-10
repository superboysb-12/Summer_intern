package com.XuebaoMaster.backend.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    Optional<SchoolClass> findByClassName(String className);
    boolean existsByClassName(String className);
    List<SchoolClass> findByClassNameContaining(String keyword);
}
