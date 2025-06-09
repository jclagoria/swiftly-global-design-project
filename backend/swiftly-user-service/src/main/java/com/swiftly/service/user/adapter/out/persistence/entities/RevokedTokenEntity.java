package com.swiftly.service.user.adapter.out.persistence.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("revoked_tokens")
public class RevokedTokenEntity {

    @Id
    private String token;

    @Column("expires_at")
    private Instant expiresAt;

}
