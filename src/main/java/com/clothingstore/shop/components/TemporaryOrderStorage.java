package com.clothingstore.shop.components;

import com.clothingstore.shop.dto.others.checkout.CheckoutBaseProductVariantModel;
import com.clothingstore.shop.dto.others.checkout.CheckoutBaseStoreOrderModel;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TemporaryOrderStorage {
    private final InventoryRepository inventoryRepository;
    private static final Logger logger = LoggerFactory.getLogger(TemporaryOrderStorage.class);
    private final Map<String, TemporaryOrder> tempOrders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TemporaryOrderStorage(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        // 定時清理超時訂單
        logger.info("Initializing TemporaryOrderStorage");
        scheduler.scheduleAtFixedRate(this::cleanUpExpiredOrders, 1, 1, TimeUnit.MINUTES);
    }

    public String saveTemporaryOrder(TemporaryOrder order) {
        String orderId = UUID.randomUUID().toString();
        order.setExpirationTime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)); // 15分鐘超時
        logger.info("Saved temporary order with ID: {}", orderId);
        tempOrders.put(orderId, order);
        return orderId;
    }

    public TemporaryOrder getTemporaryOrder(String orderId) {
        TemporaryOrder order = tempOrders.get(orderId);
        logger.info("Retrieved temporary order with ID: {}", orderId);
        return order;
    }

    public void confirmOrder(String orderId) {
        tempOrders.remove(orderId);
        logger.info("Confirmed order with ID: {}", orderId);
    }

    private void cleanUpExpiredOrders() {
        logger.info("Running cleanUpExpiredOrders");
        logger.info("Running cleanUpExpiredOrders. Current head of tempOrders: {}", tempOrders.entrySet().stream().findFirst().map(Map.Entry::getKey).orElse("No orders"));
        long now = System.currentTimeMillis();
        tempOrders.entrySet().removeIf(entry -> {
            if (entry.getValue().getExpirationTime() < now) {
                // 回滾預留庫存
                TemporaryOrder tempOrder = entry.getValue();
                for (TemporaryStoreOrder storeOrder : tempOrder.getStoreOrders()) {
                    for (TemporaryProductVariant productVariant : storeOrder.getProductVariants()) {
                        inventoryRepository.rollbackStock(productVariant.getProductVariantId(), productVariant.getQuantity());
                    }
                }
                return true;
            }
            return false;
        });
    }


}

