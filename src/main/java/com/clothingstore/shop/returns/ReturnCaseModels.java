package com.clothingstore.shop.returns;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ReturnCaseModels {
    private ReturnCaseModels() {}

    public record CreateRequest(Integer orderItemId, Integer quantity, String reason) {}
    public record DecisionRequest(Boolean approved, String response) {}
    public record AppealRequest(String reason) {}

    public record Summary(UUID caseId, int orderItemId, int orderId, int quantity,
                          int grossAmount, int approvedAmount, String currency, ReturnCaseState status,
                          OffsetDateTime vendorDeadline, OffsetDateTime appealDeadline) {}

    public record Action(UUID actionId, String actorRole, String actionType, ReturnCaseState fromStatus,
                         ReturnCaseState toStatus, String note, OffsetDateTime createdAt) {}

    public record Settlement(UUID caseId, int orderId, int orderItemId, int quantity,
                             int amount, String currency, String decisionSource, OffsetDateTime createdAt) {}

    public record Detail(Summary summary, String reason, String vendorResponse, String adminResponse,
                         List<Action> actions, Settlement settlement) {}
}
