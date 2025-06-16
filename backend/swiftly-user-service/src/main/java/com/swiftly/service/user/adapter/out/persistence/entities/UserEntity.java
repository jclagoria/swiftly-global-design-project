package com.swiftly.service.user.adapter.out.persistence.entities;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("users")
public class UserEntity {

    @Id
    private UUID    id;

    @NotNull
    private String  email;

    @NotNull
    @Column("password_hash")
    private String  passwordHash;

    @NotNull
    @Column("first_name")
    private String  firstName;

    @NotNull
    @Column("last_name")
    private String  lastName;

    @Column("created_at")
    private Instant createdAt;

    private Boolean deleted;

    @Column("deleted_at")
    private Instant deletedAt;

}
