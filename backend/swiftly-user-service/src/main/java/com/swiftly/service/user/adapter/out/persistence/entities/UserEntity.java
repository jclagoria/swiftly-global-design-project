package com.swiftly.service.user.adapter.out.persistence.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity {

    @Id
    private UUID    id;
    private String  email;

    @Column("password_hash")
    private String  passwordHash;

    @Column("first_name")
    private String  firstName;

    @Column("last_name")
    private String  lastName;

    @Column("created_at")
    private Instant createdAt;

}
