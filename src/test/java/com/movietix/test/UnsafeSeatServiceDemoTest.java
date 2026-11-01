package com.movietix.test;

import com.movietix.model.Seat;
import com.movietix.model.Seat.SeatState;
import com.movietix.service.SeatService;
import com.movietix.service.SeatService.OperationResult;
import com.movietix.store.BookingStore;
import com.movietix.store.SeatStore;
import com.movietix.test.TestHelper.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TEST 7: UnsafeSeatServiceDemoTest
 * ===================================
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  ⚠️  VIVA DEMO ONLY – This class is NOT part of the real application.  ║
 * ║  It deliberately omits locking to demonstrate the race condition that   ║
 * ║  SeatService's ReentrantLock prevents.                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * UnsafeSeatService is a stripped-down, intentionally BROKEN version:
 *   - No locks (uses a plain HashMap, no synchronisation)
 *   - Same logic as SeatService, but without locking
 *
 * Test: 50 threads race to hold seat E1.
 * SAFE version (SeatService)  → exactly 1 succeeds.
 * UNSAFE version              → usually MORE THAN 1 succeeds (race condition).
 *
 * Run this during your viva to show the before-and-after effect of locking.
 * The output clearly shows more than 1 "success" in the unsafe case.
 *
 * NOTE: The unsafe test is non-deterministic – on some runs all races might
 * resolve in the same order. The point is to show the possibility of
 * multiple successes, not guarantee it.
 */
@DisplayName("TC-07: DEMO – Unsafe (No-Lock) vs Safe (Locked) Service")
class UnsafeSeatServiceDemoTest {

    private static final int THREADS = 50;

    // =========================================================================
    // UnsafeSeatService – deliberately broken, NO synchronisation
    // =========================================================================

    /**
     * A simplified, UNSAFE seat holder with no locking whatsoever.
     * Plain HashMap accessed by 50 concurrent threads → race condition.
     */
    static class UnsafeSeatService {

        // Plain HashMap – NOT thread safe! Concurrent access causes races.
        private final Map<String, String> heldBy = new HashMap<>(); // seatId → userId

        /**
         * Tries to hold a seat WITHOUT any synchronisation.
         * RACE: Thread A reads heldBy.get("E1") → null (available)
         *        Thread B reads heldBy.get("E1") → null (available)  ← both think available!
         *        Thread A writes heldBy.put("E1", "userA")
         *        Thread B writes heldBy.put("E1", "userB")  ← OVERWRITES A's hold!
         *        Both return success → DOUBLE HOLD ← the bug.
         */
        public boolean hold(String seatId, String userId) {
            // ⚠️ No lock here – this is the bug
            if (heldBy.containsKey(seatId)) {
                return false; // already held
            }
            // Simulate some processing time to increase race window
            try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            heldBy.put(seatId, userId);
            return true;
        }
    }

    // =========================================================================
    // DEMO TEST: Unsafe version (expected: > 1 success most of the time)
    // =========================================================================

    @Test
    @DisplayName("DEMO: Unsafe service allows multiple concurrent holds (race condition)")
    void unsafe_service_demonstrates_race_condition() throws InterruptedException {
        UnsafeSeatService unsafeService = new UnsafeSeatService();

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done      = new CountDownLatch(THREADS);
        AtomicInteger  successes = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final String uid = "user-" + i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    if (unsafeService.hold("E1", uid)) successes.incrementAndGet();
                } catch (Exception ignored) {
                } finally { done.countDown(); }
            });
        }

        startGate.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║   VIVA DEMO: UNSAFE Service (No Locking)            ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf("║   Threads: %-3d  Successes: %-3d  (should be 1)      ║%n",
            THREADS, successes.get());
        if (successes.get() > 1) {
            System.out.println("║   ⚠️  RACE CONDITION DETECTED – multiple holds!     ║");
        } else {
            System.out.println("║   Race didn't trigger this run (non-deterministic)  ║");
        }
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // We do NOT assert an exact count here because the race is non-deterministic.
        // The print output is the demo output for the viva.
        assertTrue(successes.get() >= 1, "At least one thread should have succeeded");
    }

    // =========================================================================
    // CONTROL TEST: Safe version (expected: exactly 1 success, guaranteed)
    // =========================================================================

    @Test
    @DisplayName("CONTROL: Safe service (locked) – exactly 1 success guaranteed")
    void safe_service_prevents_race_condition() throws Exception {
        MutableClock clock   = new MutableClock(System.currentTimeMillis());
        SeatService  service = TestHelper.createService(clock);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done      = new CountDownLatch(THREADS);
        AtomicInteger  successes = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final String uid = "user-" + i;
            pool.submit(() -> {
                try {
                    startGate.await();
                    OperationResult r = service.holdSeats(
                        TestHelper.SHOW_ID, List.of("E1"), uid);
                    if (r.success()) successes.incrementAndGet();
                } catch (Exception ignored) {
                } finally { done.countDown(); }
            });
        }

        startGate.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║   CONTROL: SAFE Service (Per-Show ReentrantLock)    ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf("║   Threads: %-3d  Successes: %-3d  (must be 1)        ║%n",
            THREADS, successes.get());
        System.out.println("║   ✅ Race condition ELIMINATED by locking!           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        assertEquals(1, successes.get(),
            "SAFE service: exactly 1 thread should succeed (lock enforces this)");
    }
}
