package com.capgemini.messaging.repository;

import com.capgemini.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE (c.participant1Id = :u1 AND c.participant2Id = :u2) OR (c.participant1Id = :u2 AND c.participant2Id = :u1)")
    Optional<Conversation> findByParticipants(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT c FROM Conversation c WHERE c.participant1Id = :userId OR c.participant2Id = :userId")
    List<Conversation> findAllByUserId(@Param("userId") Long userId);
}
