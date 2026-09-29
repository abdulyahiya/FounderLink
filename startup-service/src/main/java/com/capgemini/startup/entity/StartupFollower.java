package com.capgemini.startup.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "startup_followers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"startupId", "investorId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupFollower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long startupId;

    @Column(nullable = false)
    private Long investorId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime followedAt;
}
