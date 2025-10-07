package com.dorandoran.chat.entity.billing;

import com.dorandoran.chat.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "monthly_user_costs", schema = "billing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(MonthlyUserCost.MonthlyUserCostId.class)
public class MonthlyUserCost {

    @Id
    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "input_tokens", nullable = false)
    private Long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Long outputTokens;

    @Column(name = "cost_in", nullable = false)
    private Double costIn;

    @Column(name = "cost_out", nullable = false)
    private Double costOut;

    @Column(name = "total_cost", nullable = false)
    private Double totalCost;

    @Column(name = "last_aggregated_at", nullable = false)
    private OffsetDateTime lastAggregatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyUserCostId implements Serializable {
        private LocalDate billingMonth;
        private User user;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MonthlyUserCostId that = (MonthlyUserCostId) o;
            return Objects.equals(billingMonth, that.billingMonth) &&
                   Objects.equals(user, that.user);
        }

        @Override
        public int hashCode() {
            return Objects.hash(billingMonth, user);
        }
    }
}