package com.group5.lostandfoundjava.entity;

import com.group5.lostandfoundjava.entity.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A registered account.
 *
 * <p>Only the bcrypt hash of the password is stored — the plain password is never written anywhere.
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "rating_avg", nullable = false)
    private double ratingAvg = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    /** Required by JPA. Application code should use the constructor below. */
    protected User() {}

    public User(String name, String email, String phone, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
