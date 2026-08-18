package com.yigit.requestms.user.repository;

import com.yigit.requestms.admin.dto.AdminUserDto;
import com.yigit.requestms.user.entity.UserEntity;
import com.yigit.requestms.user.enums.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    // The code identifies the account on its own, which is what lets someone
    // set up an account without first proving who they are: holding the code is
    // the proof, and it works once.
    Optional<UserEntity> findBySetupToken(String setupToken);

    // Both filters are optional and expressed the same way, so one query serves
    // every combination the screen offers rather than four near-identical ones.
    //
    // The search matches name or email because an administrator looking for
    // someone has one or the other, not reliably both.
    //
    // Awaiting setup is derived from the token rather than stored twice: an
    // account has credentials or it has a code, never both.
    @Query("""
            SELECT new com.yigit.requestms.admin.dto.AdminUserDto(
                u.id, u.nameSurname, u.email, u.role, u.active, u.locked,
                CASE WHEN u.setupToken IS NOT NULL THEN TRUE ELSE FALSE END,
                u.createdAt)
            FROM UserEntity u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:search IS NULL
                   OR LOWER(u.nameSurname) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    List<AdminUserDto> findForAdmin(@Param("role") Role role,
                                    @Param("search") String search,
                                    Pageable pageable);

    @Query("""
            SELECT COUNT(u)
            FROM UserEntity u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:search IS NULL
                   OR LOWER(u.nameSurname) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    long countForAdmin(@Param("role") Role role, @Param("search") String search);

    long countByRoleAndActiveTrue(Role role);

    // Only active developers can take work, so the assignment lists ask for
    // exactly those rather than filtering a wider result afterwards.
    List<UserEntity> findByRoleAndActiveTrueOrderByNameSurname(Role role);
}