package com.decisionhub.repository.reports;

import com.decisionhub.entity.reports.ReportExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportExportRepository extends JpaRepository<ReportExport, Long> {
    List<ReportExport> findByRequestedByIdOrderByGeneratedAtDesc(Long userId);
}
