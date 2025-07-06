package com.XuebaoMaster.backend.SchoolClass;
import com.XuebaoMaster.backend.User.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
@Entity
@Table(name = "school_classes")
@Data
public class SchoolClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String className;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @JsonIgnore
    @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.EAGER)
    private List<User> students = new ArrayList<>();
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

