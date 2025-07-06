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
}
