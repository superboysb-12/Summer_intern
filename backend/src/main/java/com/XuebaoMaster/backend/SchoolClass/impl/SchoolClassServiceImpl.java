package com.XuebaoMaster.backend.SchoolClass.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.XuebaoMaster.backend.SchoolClass.SchoolClass;
import com.XuebaoMaster.backend.SchoolClass.SchoolClassRepository;
import com.XuebaoMaster.backend.SchoolClass.SchoolClassService;
import com.XuebaoMaster.backend.User.User;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class SchoolClassServiceImpl implements SchoolClassService {
    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Override
    public SchoolClass createSchoolClass(SchoolClass schoolClass) {
        return schoolClassRepository.save(schoolClass);
    }

    @Override
    public SchoolClass updateSchoolClass(SchoolClass schoolClass) {
        SchoolClass existingClass = schoolClassRepository.findById(schoolClass.getId())
                .orElseThrow(() -> new RuntimeException("School class not found"));
        existingClass.setClassName(schoolClass.getClassName());
        return schoolClassRepository.save(existingClass);
    }

    @Override
    public void deleteSchoolClass(Long id) {
        schoolClassRepository.deleteById(id);
    }

    @Override
    public SchoolClass getSchoolClassById(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("School class not found"));
    }

    @Override
    public SchoolClass getSchoolClassByName(String className) {
        return schoolClassRepository.findByClassName(className)
                .orElseThrow(() -> new RuntimeException("School class not found"));
    }

    @Override
    public List<SchoolClass> getAllSchoolClasses() {
        return schoolClassRepository.findAll();
    }

    @Override
    public List<SchoolClass> searchSchoolClasses(String keyword) {
        return schoolClassRepository.findByClassNameContaining(keyword);
    }

    @Override
    public boolean checkClassNameExists(String className) {
        return schoolClassRepository.existsByClassName(className);
    }

    @Override
    public List<Long> getStudentIdsByClassId(Long classId) {
        SchoolClass schoolClass = getSchoolClassById(classId);

        // 获取班级中的所有学生列表并提取学生ID
        List<User> students = schoolClass.getStudents();
        return students.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }
}
