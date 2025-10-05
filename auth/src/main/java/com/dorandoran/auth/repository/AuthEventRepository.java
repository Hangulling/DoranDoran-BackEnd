package com.dorandoran.auth.repository;

import com.dorandoran.auth.entity.AuthEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthEventRepository extends JpaRepository<AuthEvent, Long> {
}


