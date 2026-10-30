package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.ledger.PlatformLedgerResponseDTO;
import com.clothingstore.shop.dto.response.ledger.VendorLedgerResponseDTO;
import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.noCondition;

@Repository
public class LedgerRepository {

    private final DSLContext dsl;
    @Autowired
    public LedgerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<PlatformLedgerResponseDTO> findAllPlatformLedgerEntries(LocalDate fromDate, LocalDate toDate) {
        return dsl.select(
                        field("ledger_entry_id").as("ledgerEntryId"),
                        field("ledger_type").as("ledgerType"),
                        field("transaction_type").as("transactionType"),
                        field("amount").as("amount"),
                        field("total_balance").as("totalBalance"),
                        field("transaction_date").as("transactionDate"),
                        field("notes").as("notes"),
                        field("coupon_id").as("couponId"),
                        field("order_id").as("orderId"))
                .from("platform_ledger_view")
                .where(dateRange(fromDate, toDate))
                .orderBy(field(name("transaction_date")).desc(), field(name("ledger_entry_id")).desc())
                .fetchInto(PlatformLedgerResponseDTO.class);
    }

    public List<VendorLedgerResponseDTO> findAllVendorLedgerEntries(Integer vendorId, LocalDate fromDate, LocalDate toDate) {
        return dsl.select(
                        field("ledger_entry_id").as("ledgerEntryId"),
                        field("ledger_type").as("ledgerType"),
                        field("transaction_type").as("transactionType"),
                        field("amount").as("amount"),
                        field("total_balance").as("totalBalance"),
                        field("transaction_date").as("transactionDate"),
                        field("notes").as("notes"),
                        field("coupon_id").as("couponId"),
                        field("vendor_id").as("vendorId"),
                        field("store_order_id").as("storeOrderId"))
                .from("vendor_ledger_view")
                .where(field("vendor_id").eq(vendorId).and(dateRange(fromDate, toDate)))
                .orderBy(field(name("transaction_date")).desc(), field(name("ledger_entry_id")).desc())
                .fetchInto(VendorLedgerResponseDTO.class);
    }

    private Condition dateRange(LocalDate fromDate, LocalDate toDate) {
        Field<LocalDateTime> transactionDate = field(name("transaction_date"), LocalDateTime.class);
        Condition condition = noCondition();
        if (fromDate != null) {
            condition = condition.and(transactionDate.ge(fromDate.atStartOfDay()));
        }
        if (toDate != null && !toDate.equals(LocalDate.MAX)) {
            condition = condition.and(transactionDate.lt(toDate.plusDays(1).atStartOfDay()));
        }
        return condition;
    }
}
