package com.clothingstore.shop.components;

import com.clothingstore.shop.dto.others.checkout.CheckoutBaseProductVariantModel;
import com.clothingstore.shop.dto.others.checkout.CheckoutBaseStoreOrderModel;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.repository.InventoryRepository;
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

    private final Map<String, TemporaryOrder> tempOrders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public TemporaryOrderStorage(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        // 定時清理超時訂單
        scheduler.scheduleAtFixedRate(this::cleanUpExpiredOrders, 1, 1, TimeUnit.MINUTES);
    }

    public String saveTemporaryOrder(TemporaryOrder order) {
        String orderId = UUID.randomUUID().toString();
        order.setExpirationTime(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)); // 15分鐘超時
        tempOrders.put(orderId, order);
        return orderId;
    }

    public TemporaryOrder getTemporaryOrder(String orderId) {
        return tempOrders.get(orderId);
    }

    public void confirmOrder(String orderId) {
        tempOrders.remove(orderId);
    }

    private void cleanUpExpiredOrders() {
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

