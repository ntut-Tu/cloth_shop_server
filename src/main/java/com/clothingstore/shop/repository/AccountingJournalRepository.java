package com.clothingstore.shop.repository;

import com.clothingstore.shop.accounting.AccountingOrderSnapshot;
import com.clothingstore.shop.accounting.AccountingReconciliationCalculator;
import com.clothingstore.shop.dto.response.accounting.AccountingAccountBalanceResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingJournalResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingPostingResponse;
import com.clothingstore.shop.dto.response.accounting.AccountingReconciliationResponse;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Repository
public class AccountingJournalRepository {
    private final DSLContext dsl;

    public AccountingJournalRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public AccountingJournalResponse findJournal(long journalId) {
        Record header = dsl.fetchOne("""
                SELECT journal_id, order_id, entry_type, event_key, occurred_at,
                       reverses_journal_id, actor_user_id, reason
                FROM accounting_journal WHERE journal_id = ?
                """, journalId);
        if (header == null) {
            throw new IllegalArgumentException("Journal not found");
        }
        List<AccountingPostingResponse> postings = dsl.fetch("""
                SELECT posting_id, account_code, debit, credit
                FROM accounting_posting WHERE journal_id = ? ORDER BY posting_id
                """, journalId).map(row -> new AccountingPostingResponse(
                row.get("posting_id", Long.class),
                row.get("account_code", String.class),
                row.get("debit", BigDecimal.class),
                row.get("credit", BigDecimal.class)));
        return new AccountingJournalResponse(
                header.get("journal_id", Long.class),
                header.get("order_id", Integer.class),
                header.get("entry_type", String.class),
                header.get("event_key", String.class),
                header.get("occurred_at", OffsetDateTime.class),
                header.get("reverses_journal_id", Long.class),
                header.get("actor_user_id", Integer.class),
                header.get("reason", String.class),
                postings);
    }

    /** A reversal is an append-only journal whose postings swap the original debit and credit. */
    public long reverse(long originalJournalId, String eventKey, String reason, int actorUserId) {
        return dsl.transactionResult(configuration -> {
            DSLContext tx = DSL.using(configuration);
            Record original = tx.fetchOne("""
                    SELECT journal_id, order_id, entry_type
                    FROM accounting_journal WHERE journal_id = ? FOR SHARE
                    """, originalJournalId);
            if (original == null) {
                throw new IllegalArgumentException("Journal not found");
            }
            if (!"ORDER_PAYMENT".equals(original.get("entry_type", String.class))) {
                throw new IllegalArgumentException("Only payment journals can be reversed");
            }

            Record inserted = tx.fetchOne("""
                    INSERT INTO accounting_journal
                        (order_id, entry_type, event_key, reverses_journal_id, actor_user_id, reason)
                    VALUES (?, 'REVERSAL', ?, ?, ?, ?)
                    ON CONFLICT DO NOTHING RETURNING journal_id
                    """, original.get("order_id", Integer.class), eventKey,
                    originalJournalId, actorUserId, reason);
            if (inserted == null) {
                Record existing = tx.fetchOne("""
                        SELECT journal_id, reverses_journal_id, event_key, actor_user_id, reason
                        FROM accounting_journal
                        WHERE event_key = ? OR reverses_journal_id = ?
                        ORDER BY (event_key = ?) DESC LIMIT 1
                        """, eventKey, originalJournalId, eventKey);
                if (existing != null
                        && Objects.equals(existing.get("reverses_journal_id", Long.class), originalJournalId)
                        && eventKey.equals(existing.get("event_key", String.class))
                        && Objects.equals(existing.get("actor_user_id", Integer.class), actorUserId)
                        && reason.equals(existing.get("reason", String.class))) {
                    return existing.get("journal_id", Long.class);
                }
                throw new IllegalStateException("Reversal idempotency key or target already used");
            }

            long reversalId = inserted.get("journal_id", Long.class);
            int copied = tx.execute("""
                    INSERT INTO accounting_posting (journal_id, account_code, debit, credit)
                    SELECT ?, account_code, credit, debit
                    FROM accounting_posting WHERE journal_id = ?
                    """, reversalId, originalJournalId);
            if (copied < 2) {
                throw new IllegalStateException("Original journal has no balanced postings");
            }
            return reversalId;
        });
    }

    public List<AccountingAccountBalanceResponse> accountBalances(LocalDate throughDate) {
        OffsetDateTime exclusiveEnd = throughDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return dsl.fetch("""
                SELECT a.account_code, a.account_name, a.normal_side,
                       COALESCE(SUM(CASE WHEN j.journal_id IS NULL THEN 0
                           WHEN a.normal_side = 'DEBIT' THEN p.debit - p.credit
                           ELSE p.credit - p.debit END), 0) AS balance
                FROM accounting_account a
                LEFT JOIN accounting_posting p ON p.account_code = a.account_code
                LEFT JOIN accounting_journal j ON j.journal_id = p.journal_id
                  AND j.occurred_at < ?
                GROUP BY a.account_code, a.account_name, a.normal_side
                ORDER BY a.account_code
                """, exclusiveEnd).map(row -> new AccountingAccountBalanceResponse(
                row.get("account_code", String.class),
                row.get("account_name", String.class),
                row.get("normal_side", String.class),
                row.get("balance", BigDecimal.class)));
    }

    /** Reconcile the UTC order-date cohort against its net payment postings. */
    public AccountingReconciliationResponse reconcile(LocalDate orderDate) {
        OffsetDateTime start = orderDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = orderDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<Record> rows = dsl.fetch("""
                SELECT o.order_id, o.pay_status, o.total_amount,
                       payment.journal_id AS payment_journal_id,
                       COALESCE(payment_lines.line_count, 0) AS payment_line_count,
                       COALESCE(payment_lines.debits, 0) AS payment_debits,
                       COALESCE(payment_lines.credits, 0) AS payment_credits,
                       reversal.journal_id AS reversal_journal_id,
                       COALESCE(reversal_lines.line_count, 0) AS reversal_line_count,
                       COALESCE(reversal_lines.debits, 0) AS reversal_debits,
                       COALESCE(reversal_lines.credits, 0) AS reversal_credits
                FROM "order" o
                LEFT JOIN accounting_journal payment
                  ON payment.order_id = o.order_id AND payment.entry_type = 'ORDER_PAYMENT'
                LEFT JOIN LATERAL (
                  SELECT COUNT(*) AS line_count, SUM(debit) AS debits, SUM(credit) AS credits
                  FROM accounting_posting WHERE journal_id = payment.journal_id
                ) payment_lines ON TRUE
                LEFT JOIN accounting_journal reversal
                  ON reversal.reverses_journal_id = payment.journal_id
                LEFT JOIN LATERAL (
                  SELECT COUNT(*) AS line_count, SUM(debit) AS debits, SUM(credit) AS credits
                  FROM accounting_posting WHERE journal_id = reversal.journal_id
                ) reversal_lines ON TRUE
                WHERE o.order_date >= ? AND o.order_date < ?
                ORDER BY o.order_id
                """, start, end);
        List<AccountingOrderSnapshot> snapshots = rows.stream().map(row -> new AccountingOrderSnapshot(
                row.get("order_id", Integer.class),
                row.get("pay_status", String.class),
                row.get("total_amount", Integer.class),
                row.get("payment_journal_id", Long.class),
                row.get("payment_line_count", Long.class),
                row.get("payment_debits", BigDecimal.class),
                row.get("payment_credits", BigDecimal.class),
                row.get("reversal_journal_id", Long.class),
                row.get("reversal_line_count", Long.class),
                row.get("reversal_debits", BigDecimal.class),
                row.get("reversal_credits", BigDecimal.class))).toList();
        return AccountingReconciliationCalculator.calculate(orderDate, snapshots);
    }
}
