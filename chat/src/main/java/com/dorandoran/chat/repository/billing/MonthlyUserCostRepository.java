package com.dorandoran.chat.repository.billing;

import com.dorandoran.chat.entity.billing.MonthlyUserCost;
import com.dorandoran.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyUserCostRepository extends JpaRepository<MonthlyUserCost, MonthlyUserCost.MonthlyUserCostId> {
    Optional<MonthlyUserCost> findByUserAndBillingMonth(User user, LocalDate billingMonth);
    List<MonthlyUserCost> findByUserAndBillingMonthBetween(User user, LocalDate fromInclusive, LocalDate toInclusive);
}