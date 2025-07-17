package com.XuebaoMaster.backend.RAG.Course;

import com.XuebaoMaster.backend.Course.Course;
import com.XuebaoMaster.backend.RAG.RAG;
import jakarta.persistence.*;

@Entity
@Table(name = "course_rag_mappings")
public class CourseRagMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "rag_id", nullable = false)
    private RAG rag;

    public CourseRagMapping() {
    }

    public CourseRagMapping(Course course, RAG rag) {
        this.course = course;
        this.rag = rag;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public RAG getRag() {
        return rag;
    }

    public void setRag(RAG rag) {
        this.rag = rag;
    }
}