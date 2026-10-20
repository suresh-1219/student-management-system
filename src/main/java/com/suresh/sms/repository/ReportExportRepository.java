package com.suresh.sms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suresh.sms.entity.ReportExport;

public interface ReportExportRepository extends JpaRepository<ReportExport, Long> {
}
