package com.group5.lostandfoundjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.group5.lostandfoundjava.common.exception.BadRequestException;
import com.group5.lostandfoundjava.common.exception.NotFoundException;
import com.group5.lostandfoundjava.dto.user.UserResponse;
import com.group5.lostandfoundjava.entity.User;
import com.group5.lostandfoundjava.entity.enums.Role;
import com.group5.lostandfoundjava.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The two guards that keep the admin area reachable. */
class AdminUserServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminUserServiceImpl service = new AdminUserServiceImpl(userRepository);

    @Test
    @DisplayName("updateRole promotes a regular user to admin")
    void updateRolePromotesRegularUser() {
        User target = user(Role.USER);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        UserResponse response = service.updateRole(UUID.randomUUID(), target.getId(), Role.ADMIN);

        assertEquals(Role.ADMIN, response.role());
        assertEquals(Role.ADMIN, target.getRole());
    }

    @Test
    @DisplayName("updateRole rejects an admin changing their own role")
    void updateRoleRejectsSelfChange() {
        User admin = user(Role.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(
                BadRequestException.class, () -> service.updateRole(admin.getId(), admin.getId(), Role.USER));
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    @DisplayName("updateRole refuses to demote the last admin")
    void updateRoleRefusesToDemoteLastAdmin() {
        User lastAdmin = user(Role.ADMIN);
        when(userRepository.findById(lastAdmin.getId())).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(
                BadRequestException.class,
                () -> service.updateRole(UUID.randomUUID(), lastAdmin.getId(), Role.USER));
        assertEquals(Role.ADMIN, lastAdmin.getRole());
    }

    @Test
    @DisplayName("updateRole demotes an admin while others remain")
    void updateRoleDemotesAdminWhenOthersRemain() {
        User admin = user(Role.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(userRepository.save(admin)).thenReturn(admin);

        UserResponse response = service.updateRole(UUID.randomUUID(), admin.getId(), Role.USER);

        assertEquals(Role.USER, response.role());
    }

    @Test
    @DisplayName("updateRole to the current role is a no-op, even for the acting admin")
    void updateRoleToSameRoleIsNoOp() {
        User admin = user(Role.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        UserResponse response = service.updateRole(admin.getId(), admin.getId(), Role.ADMIN);

        assertEquals(Role.ADMIN, response.role());
    }

    @Test
    @DisplayName("get throws NotFoundException for an unknown user")
    void getThrowsForUnknownUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(id));
    }

    private User user(Role role) {
        return new User("Jane", "jane@example.com", null, "hashed", role);
    }
}
