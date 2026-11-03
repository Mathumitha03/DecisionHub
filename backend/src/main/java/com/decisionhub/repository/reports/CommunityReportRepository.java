package com.decisionhub.repository.reports;

import com.decisionhub.entity.reports.CommunityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {
    List<CommunityReport> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<CommunityReport> findAllByOrderByCreatedAtDesc();
    long countByCommunityId(Long communityId);
}

