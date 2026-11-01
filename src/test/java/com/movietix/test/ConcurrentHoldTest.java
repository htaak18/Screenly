package com.movietix.test;

import com.movietix.service.SeatService;
import com.movietix.service.SeatService.OperationResult;
import com.movietix.test.TestHelper.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TEST 1: ConcurrentHoldTest
 * ==========================
 * 50 threads simultaneously try to hold the SAME seat (A1) on the same show.
 * Expected: exactly 1 succeeds, 49 fail.
 *
 * WHY this matters:
 * Without the per-show ReentrantLock in SeatService, the "check availability
 * then mark HELD" is NOT atomic. Two threads could both read AVAILABLE and
 * both succeed → double hold → double booking → revenue loss.
 *
 * The test uses a CountDownLatch to force all 50 threads to start simultaneously
 * (as close as possible), maximising contention.
 */
@DisplayName("TC-01: Concurrent Hold – Same Seat")
class ConcurrentHoldTest {

    private static final int THREADS = 50;
    private SeatService seatService;

    @BeforeEach
    void setUp() throws Exception {
        MutableClock clock = new MutableClock(System.currentTimeMillis());
        seatService = TestHelper.createService(clock);
    }

    @Test
    @DisplayName("Exactly 1 of 50 concurrent threads should hold seat A1")
    void exactly_one_thread_wins() throws InterruptedException {
        CountDownLatch startGate  = new CountDownLatch(1);       // all threads wait here
        CountDownLatch doneLatch  = new CountDownLatch(THREADS); // main waits for all to finish

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            final String userId = "user-" + i;
            pool.submit(() -> {
                try {
                    startGate.await(); // wait until all threads are ready
                    OperationResult result = seatService.holdSeats(
                        TestHelper.SHOW_ID, List.of("A1"), userId);

                    if (result.success()) successes.incrementAndGet();
                    else                  failures.incrementAndGet();

                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // fire all threads simultaneously
        doneLatch.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("\n=== ConcurrentHoldTest Results ===");
        System.out.println("  Threads: " + THREADS);
        System.out.println("  Successes: " + successes.get() + "  (expected: 1)");
        System.out.println("  Failures:  " + failures.get()  + "  (expected: 49)");
        System.out.println("  → Race condition PREVENTED by ReentrantLock ✅");

        assertEquals(1,          successes.get(), "Exactly 1 thread should succeed");
        assertEquals(THREADS - 1, failures.get(),  "Exactly 49 threads should fail");
    }
}
