package com.dorandoran.infra.persistence.repository;

import com.dorandoran.infra.persistence.entity.UserRole;
import com.dorandoran.infra.persistence.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByIdUserId(UUID userId);
}


