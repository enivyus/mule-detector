package com.muledetector.core.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId, Pageable pageable);

    /** Balance recomputed from the ledger: credits minus debits. Used to reconcile the cached balance. */
    @Query("""
           select coalesce(sum(case when e.direction = com.muledetector.core.ledger.LedgerEntry.Direction.CREDIT
                                    then e.amountSatang else -e.amountSatang end), 0L)
           from LedgerEntry e where e.accountId = :accountId
           """)
    long computeBalance(@Param("accountId") Long accountId);

    /** For a healthy double-entry ledger this is always 0. */
    @Query("""
           select coalesce(sum(case when e.direction = com.muledetector.core.ledger.LedgerEntry.Direction.CREDIT
                                    then e.amountSatang else -e.amountSatang end), 0L)
           from LedgerEntry e
           """)
    long globalImbalance();
}
