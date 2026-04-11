package com.dorandoran.user.repository;

import com.dorandoran.user.entity.AdminHomePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminHomePostRepository extends JpaRepository<AdminHomePost, Long> {

    @Query("SELECT p FROM AdminHomePost p WHERE p.deletedAt IS NULL AND p.isMainHome = true ORDER BY p.displayOrder ASC NULLS LAST")
    List<AdminHomePost> findMainHomePosts();

    @Query("SELECT COUNT(p) FROM AdminHomePost p WHERE p.deletedAt IS NULL AND p.isMainHome = true")
    long countMainHome();

    @Query("SELECT p FROM AdminHomePost p WHERE p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<AdminHomePost> findAllActive(Pageable pageable);

    @Query("SELECT p FROM AdminHomePost p WHERE p.deletedAt IS NULL AND p.isMainHome = :isMainHome ORDER BY p.displayOrder ASC NULLS LAST, p.createdAt DESC")
    Page<AdminHomePost> findByIsMainHome(@Param("isMainHome") boolean isMainHome, Pageable pageable);

    Optional<AdminHomePost> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT p FROM AdminHomePost p WHERE p.deletedAt IS NULL AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY p.createdAt DESC")
    Page<AdminHomePost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
