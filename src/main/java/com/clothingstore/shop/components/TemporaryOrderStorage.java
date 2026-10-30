package com.clothingstore.shop.components;

import com.clothingstore.shop.dto.others.tempOrder.TemporaryOrder;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryProductVariant;
import com.clothingstore.shop.dto.others.tempOrder.TemporaryStoreOrder;
import com.clothingstore.shop.repository.InventoryRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TemporaryOrderStorage {
    private static final Logger logger = LoggerFactory.getLogger(TemporaryOrderStorage.class);
    private static final long QUOTE_LIFETIME_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private final InventoryRepository inventoryRepository;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, StoredOrder> tempOrders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final class StoredOrder {
        private final TemporaryOrder order;
        private volatile boolean committed;

        private StoredOrder(TemporaryOrder order, boolean committed) {
            this.order = order;
            this.committed = committed;
        }
    }

    public TemporaryOrderStorage(InventoryRepository inventoryRepository, PlatformTransactionManager transactionManager) {
        this.inventoryRepository = inventoryRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        scheduler.scheduleAtFixedRate(this::cleanUpExpiredOrders, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stopScheduler() {
        scheduler.shutdownNow();
    }

    public String saveTemporaryOrder(TemporaryOrder order) {
        boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
        order.setExpirationTime(System.currentTimeMillis() + QUOTE_LIFETIME_MILLIS);

        String orderId;
        StoredOrder storedOrder = new StoredOrder(order, !transactionActive);
        do {
            orderId = UUID.randomUUID().toString();
        } while (tempOrders.putIfAbsent(orderId, storedOrder) != null);

        if (transactionActive) {
            String savedOrderId = orderId;
            try {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_COMMITTED) {
                            // A quote cannot be claimed until its reserved inventory is durable.
                            order.setExpirationTime(System.currentTimeMillis() + QUOTE_LIFETIME_MILLIS);
                            storedOrder.committed = true;
                        } else {
                            tempOrders.remove(savedOrderId, storedOrder);
                        }
                    }
                });
            } catch (RuntimeException exception) {
                tempOrders.remove(savedOrderId, storedOrder);
                throw exception;
            }
        }

        logger.info("Saved temporary order with ID: {}", orderId);
        return orderId;
    }

    public TemporaryOrder getTemporaryOrder(String orderId) {
        StoredOrder storedOrder = tempOrders.get(orderId);
        return storedOrder != null && storedOrder.committed ? storedOrder.order : null;
    }

    public boolean claimTemporaryOrder(String orderId, TemporaryOrder expectedOrder) {
        AtomicBoolean claimed = new AtomicBoolean();
        tempOrders.computeIfPresent(orderId, (id, storedOrder) -> {
            if (storedOrder.committed && storedOrder.order == expectedOrder
                    && storedOrder.order.getExpirationTime() > System.currentTimeMillis()) {
                claimed.set(true);
                return null;
            }
            return storedOrder;
        });
        return claimed.get();
    }

    public void restoreTemporaryOrder(String orderId, TemporaryOrder order) {
        StoredOrder existing = tempOrders.putIfAbsent(orderId, new StoredOrder(order, true));
        if (existing != null && existing.order != order) {
            logger.error("Cannot restore temporary order {} because the ID is already occupied", orderId);
        }
    }

    private void cleanUpExpiredOrders() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, StoredOrder> entry : tempOrders.entrySet()) {
            StoredOrder storedOrder = entry.getValue();
            if (!storedOrder.committed || storedOrder.order.getExpirationTime() > now
                    || !tempOrders.remove(entry.getKey(), storedOrder)) {
                continue;
            }

            try {
                transactionTemplate.execute(status -> {
                    for (TemporaryStoreOrder storeOrder : storedOrder.order.getStoreOrders()) {
                        for (TemporaryProductVariant productVariant : storeOrder.getProductVariants()) {
                            inventoryRepository.rollbackStock(productVariant.getProductVariantId(), productVariant.getQuantity());
                        }
                    }
                    return null;
                });
                logger.info("Released expired temporary order {}", entry.getKey());
            } catch (RuntimeException exception) {
                restoreTemporaryOrder(entry.getKey(), storedOrder.order);
                logger.error("Failed to release expired temporary order {}; it will be retried", entry.getKey(), exception);
            }
        }
    }
}
