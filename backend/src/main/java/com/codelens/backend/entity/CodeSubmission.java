package com.codelens.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "code_submissions")
@Getter
@Setter
@NoArgsConstructor
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}