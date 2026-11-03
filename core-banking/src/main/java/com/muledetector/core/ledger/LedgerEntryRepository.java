package com.muledetector.core.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

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
