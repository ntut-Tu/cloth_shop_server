package com.clothingstore.shop.repository;

import com.clothingstore.shop.dto.response.ledger.PlatformLedgerResponseDTO;
import com.clothingstore.shop.dto.response.ledger.VendorLedgerResponseDTO;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.jooq.impl.DSL.field;

@Repository
public class LedgerRepository {

    private final DSLContext dsl;
    @Autowired
    public LedgerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<PlatformLedgerResponseDTO> findAllPlatformLedgerEntries() {
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
                .fetchInto(PlatformLedgerResponseDTO.class);
    }

    public List<VendorLedgerResponseDTO> findAllVendorLedgerEntries() {
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
                .fetchInto(VendorLedgerResponseDTO.class);
    }
}
