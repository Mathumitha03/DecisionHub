package com.decisionhub.repository.decision;

import com.decisionhub.entity.decision.Decision;
import com.decisionhub.enums.decision.DecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import com.decisionhub.enums.decision.DecisionVisibility;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long> {

    long countByStatus(DecisionStatus status);

    long countByCommunityId(Long communityId);

    long countByCommunityIdAndStatus(
            Long communityId,
            DecisionStatus status
    );

    long countByCommunityIdAndLockedTrue(Long communityId);

    long countByCommunityIdAndLockedFalse(Long communityId);

    long countByCommunityIdAndPinnedTrue(Long communityId);

    long countByVisibility(DecisionVisibility visibility);

    @Query("""
SELECT d.creator.id,
       d.creator.username,
       COUNT(d)
FROM Decision d
WHERE d.community.id = :communityId
GROUP BY d.creator.id, d.creator.username
ORDER BY COUNT(d) DESC
""")
    List<Object[]> getTopDecisionCreators(
            @Param("communityId") Long communityId
    );

    @Query("""
SELECT d.title
FROM Decision d
LEFT JOIN Comment c ON c.decision.id = d.id
GROUP BY d.id, d.title
ORDER BY COUNT(c) DESC
LIMIT 1
""")
    String findMostActiveDecision();

    @Query("SELECT d.createdAt FROM Decision d WHERE d.creator.id = :userId AND d.createdAt >= :cutoffTime ORDER BY d.createdAt ASC")
    List<LocalDateTime> findDecisionCreationTimesSince(
            @Param("userId") Long userId,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );

    @Query("SELECT COUNT(d) FROM Decision d WHERE d.creator.id = :userId AND d.createdAt >= :cutoffTime")
    long countByCreatorIdAndCreatedAtGreaterThanEqual(
            @Param("userId") Long userId,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );
}
