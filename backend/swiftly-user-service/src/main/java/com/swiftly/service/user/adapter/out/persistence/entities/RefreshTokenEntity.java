package com.swiftly.service.user.adapter.out.persistence.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Table(name = "refresh_tokens")
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RefreshTokenEntity {

    @Id
    private UUID token;

    @NonNull
    @Column("user_id")
    private UUID userId;

    @NonNull
    @Column("created_at")
    private Instant createdAt;

    @NonNull
    @Column("expires_at")
    private Instant expiresAt;

    @NonNull
    private Boolean revoked;

}
