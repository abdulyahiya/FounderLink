package com.capgemini.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.capgemini.user.entity.UserProfile;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByUserId(Long userId);

    boolean existsByEmail(String email);

    @Query("SELECT p FROM UserProfile p WHERE LOWER(p.skills) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<UserProfile> findBySkillsContaining(@Param("keyword") String keyword);

    List<UserProfile> findByUserIdIn(List<Long> userIds);

    @Query("SELECT p FROM UserProfile p WHERE p.userId IN :ids AND LOWER(p.skills) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<UserProfile> findByUserIdInAndSkillsContaining(@Param("ids") List<Long> ids, @Param("skill") String skill);
}
