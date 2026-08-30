package com.group5.lostandfoundjava.repository;

import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Database access for {@link User}.
 *
 * <p>There is no implementation class: Spring Data reads the method names and writes the queries
 * itself. {@code findByEmail} becomes {@code select * from users where email = ?}.
 * {@link JpaRepository} already provides save, findById, findAll, delete and friends.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    /** {@link Optional} rather than a nullable return, so callers cannot forget the "missing" case. */
    Optional<User> findByEmail(String email);

    long countByRole(Role role);

    /** Backs the admin user search: one term matched against both the name and the email. */
    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);
}
