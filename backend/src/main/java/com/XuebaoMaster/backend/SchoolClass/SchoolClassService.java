package com.XuebaoMaster.backend.SchoolClass;

import java.util.List;

public interface SchoolClassService {
    SchoolClass createSchoolClass(SchoolClass schoolClass);

    SchoolClass updateSchoolClass(SchoolClass schoolClass);

    void deleteSchoolClass(Long id);

    SchoolClass getSchoolClassById(Long id);

    SchoolClass getSchoolClassByName(String className);

    List<SchoolClass> getAllSchoolClasses();

    List<SchoolClass> searchSchoolClasses(String keyword);

    boolean checkClassNameExists(String className);

    /**
     * 获取班级中所有学生的ID列表
     * 
     * @param classId 班级ID
     * @return 学生ID列表
     */
    List<Long> getStudentIdsByClassId(Long classId);
}
