package com.dorandoran.user.repository;

import com.dorandoran.user.entity.PostCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCacheRepository extends JpaRepository<PostCache, String> {
    List<PostCache> findTop6ByOrderByFetchedAtDesc();
}
