package com.muledetector.core.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNo(String accountNo);

    /** SELECT ... FOR UPDATE: blocks other transactions from touching this row until we commit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    /*
     * Id-only lookups. Used before locking so that no stale Account entity sits in the persistence
     * context: if it did, findByIdForUpdate would return that cached copy and ignore the fresh row.
     */
    @Query("select a.id from Account a where a.accountNo = :accountNo")
    Optional<Long> findIdByAccountNo(@Param("accountNo") String accountNo);

    @Query("select a.id from Account a where a.type = :type")
    Optional<Long> findIdByType(@Param("type") Account.Type type);
}
