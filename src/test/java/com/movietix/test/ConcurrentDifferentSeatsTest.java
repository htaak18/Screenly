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
 * TEST 2: ConcurrentDifferentSeatsTest
 * ======================================
 * 40 threads simultaneously hold 40 DIFFERENT seats (A1-A8, B1-B8, …, E8).
 * Expected: all 40 should succeed because they target independent seats.
 *
 * WHY this test exists:
 * Per-show locking allows only ONE thread at a time inside the show's
 * critical section. However, the requests are on DIFFERENT seats, so
 * each thread should succeed (no actual conflict).
 *
 * This test verifies that the lock does NOT create false contention
 * (i.e., the system's throughput is not unnecessarily hurt – all 40
 * non-conflicting requests complete successfully, just sequentially
 * within the show lock).
 */
@DisplayName("TC-02: Concurrent Hold – Different Seats")
class ConcurrentDifferentSeatsTest {

    private static final int THREADS = 40; // 5 rows × 8 seats = 40 seats total
    private SeatService seatService;

    // All 40 seat IDs: A1-A8, B1-B8, C1-C8, D1-D8, E1-E8
    private static final String[] ALL_SEATS = {
        "A1","A2","A3","A4","A5","A6","A7","A8",
        "B1","B2","B3","B4","B5","B6","B7","B8",
        "C1","C2","C3","C4","C5","C6","C7","C8",
        "D1","D2","D3","D4","D5","D6","D7","D8",
        "E1","E2","E3","E4","E5","E6","E7","E8"
    };

    @BeforeEach
    void setUp() throws Exception {
        seatService = TestHelper.createService(new MutableClock(System.currentTimeMillis()));
    }

    @Test
    @DisplayName("All 40 threads holding different seats should succeed")
    void all_non_conflicting_holds_succeed() throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done      = new CountDownLatch(THREADS);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            final int idx    = i;
            final String uid = "user-" + i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    OperationResult r = seatService.holdSeats(
                        TestHelper.SHOW_ID, List.of(ALL_SEATS[idx]), uid);

                    if (r.success()) successes.incrementAndGet();
                    else             failures.incrementAndGet();

                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("\n=== ConcurrentDifferentSeatsTest Results ===");
        System.out.println("  Threads:   " + THREADS);
        System.out.println("  Successes: " + successes.get() + "  (expected: 40)");
        System.out.println("  Failures:  " + failures.get()  + "  (expected: 0)");
        System.out.println("  → Non-conflicting holds all succeed ✅");

        assertEquals(THREADS, successes.get(), "All 40 distinct-seat holds should succeed");
        assertEquals(0,       failures.get(),  "No failures expected for different seats");
    }
}
