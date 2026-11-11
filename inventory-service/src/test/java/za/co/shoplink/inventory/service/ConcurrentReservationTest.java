package za.co.shoplink.inventory.service;

import org.junit.jupiter.api.Test;

import za.co.shoplink.inventory.dto.StockReservationRequest;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentReservationTest {

    @Test
    void concurrentBuyersCannotOversell() throws Exception {

        InventoryService inventory = new InventoryService();

        int buyers = 150;

        ExecutorService pool =
                Executors.newFixedThreadPool(24);

        CountDownLatch ready = new CountDownLatch(24);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger success = new AtomicInteger();

        try {

            List<Future<?>> futures =
                    new java.util.ArrayList<>();

            for (int i = 0; i < buyers; i++) {

                final int id = i;

                futures.add(pool.submit(() -> {

                    ready.countDown();
                    start.await();

                    try {

                        inventory.reserve(
                                new StockReservationRequest(
                                        "buyer-" + id,
                                        List.of(
                                                new StockReservationRequest.Item(
                                                        "P1003",
                                                        1
                                                )
                                        )
                                )
                        );

                        success.incrementAndGet();

                    } catch (
                            org.springframework.web.server.ResponseStatusException expected
                    ) {

                        assertEquals(
                                409,
                                expected.getStatusCode().value()
                        );
                    }

                    return null;
                }));
            }

            ready.await();
            start.countDown();

            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }

            assertEquals(25, success.get());
            assertEquals(0, inventory.available("P1003"));

        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentDuplicateReservationOnlyDeductsOnce()
            throws Exception {

        InventoryService inventory = new InventoryService();

        ExecutorService pool =
                Executors.newFixedThreadPool(12);

        try {

            List<Future<?>> futures =
                    new java.util.ArrayList<>();

            for (int i = 0; i < 60; i++) {

                futures.add(pool.submit(() -> {

                    inventory.reserve(
                            new StockReservationRequest(
                                    "same-order",
                                    List.of(
                                            new StockReservationRequest.Item(
                                                    "P1001",
                                                    2
                                            )
                                    )
                            )
                    );

                    return null;
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }

            assertEquals(
                    98,
                    inventory.available("P1001")
            );

        } finally {
            pool.shutdownNow();
        }
    }
}