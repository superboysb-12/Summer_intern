package com.XuebaoMaster.backend.SchoolClass;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/classes")
public class SchoolClassController {
    @Autowired
    private SchoolClassService schoolClassService;

    /**
     * 创建班级
     * 
     * @param schoolClass 班级信息
     * @return 创建的班级
     */
    @PostMapping
    public ResponseEntity<SchoolClass> createSchoolClass(@RequestBody SchoolClass schoolClass) {
        return ResponseEntity.ok(schoolClassService.createSchoolClass(schoolClass));
    }

    /**
     * 获取所有班级
     * 
     * @return 班级列表
     */
    @GetMapping
    public ResponseEntity<List<SchoolClass>> getAllSchoolClasses() {
        return ResponseEntity.ok(schoolClassService.getAllSchoolClasses());
    }

    /**
     * 根据ID获取班级
     * 
     * @param id 班级ID
     * @return 班级信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<SchoolClass> getSchoolClassById(@PathVariable Long id) {
        return ResponseEntity.ok(schoolClassService.getSchoolClassById(id));
    }

    /**
     * 更新班级信息
     * 
     * @param id          班级ID
     * @param schoolClass 班级信息
     * @return 更新后的班级
     */
    @PutMapping("/{id}")
    public ResponseEntity<SchoolClass> updateSchoolClass(@PathVariable Long id, @RequestBody SchoolClass schoolClass) {
        schoolClass.setId(id);
        return ResponseEntity.ok(schoolClassService.updateSchoolClass(schoolClass));
    }

    /**
     * 删除班级
     * 
     * @param id 班级ID
     * @return 无内容响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchoolClass(@PathVariable Long id) {
        schoolClassService.deleteSchoolClass(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 搜索班级
     * 
     * @param keyword 关键词
     * @return 班级列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<SchoolClass>> searchSchoolClasses(@RequestParam String keyword) {
        return ResponseEntity.ok(schoolClassService.searchSchoolClasses(keyword));
    }

    /**
     * 检查班级名是否存在
     * 
     * @param className 班级名
     * @return 是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkClassNameExists(@RequestParam String className) {
        return ResponseEntity.ok(schoolClassService.checkClassNameExists(className));
    }
}