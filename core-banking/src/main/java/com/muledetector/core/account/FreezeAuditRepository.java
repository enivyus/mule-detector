package com.muledetector.core.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FreezeAuditRepository extends JpaRepository<FreezeAudit, Long> {

    List<FreezeAudit> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);
}
