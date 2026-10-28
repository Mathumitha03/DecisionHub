package com.decisionhub.entity.authentication;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_emails", indexes = {
        @Index(name = "idx_blocked_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlockedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 255)
    private String reason;

    @Column(name = "blocked_by", length = 100)
    private String blockedBy;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    @PrePersist
    public void prePersist() {
        if (this.blockedAt == null) {
            this.blockedAt = LocalDateTime.now();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }
}
