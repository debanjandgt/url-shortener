package com.urlshortener.url_shortener.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="urls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Url {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,length=2048)
    private String originalUrl;

    @Column(unique = true,nullable = false)
    private String shortCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Long clickCount = 0L;

    @PrePersist
    public void prePersist()
    {
        this.createdAt = LocalDateTime.now();
        if(this.expiresAt == null)
        {
            //Default expiry - 30 Days from creation
            this.expiresAt = this.createdAt.plusDays(30);
        }
    }


}
