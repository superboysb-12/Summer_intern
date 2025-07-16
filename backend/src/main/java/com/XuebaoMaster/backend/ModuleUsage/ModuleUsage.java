package com.XuebaoMaster.backend.ModuleUsage;

import com.XuebaoMaster.backend.User.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_usage")
@Data
public class ModuleUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ModuleType moduleType;

    @Column(nullable = false)
    private LocalDateTime accessTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private User.UserRoleType userRole;

    public enum ModuleType {
        COURSE,
        SCHOOL_CLASS,
        TEACHING_PLAN_GENERATOR,
        DEEP_SEEK_CHAT
    }

    // Constructor for easy creation
    public ModuleUsage() {
    }

    public ModuleUsage(User user, ModuleType moduleType) {
        this.user = user;
        this.moduleType = moduleType;
        this.accessTime = LocalDateTime.now();
        this.userRole = user.getUserRole();
    }
}