package com.movietix.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe booking ID generator.
 *
 * Uses {@link AtomicLong} so incrementing the counter is a single atomic
 * compare-and-swap instruction – no lock required.
 *
 * WHY: Without this, two threads could read the same counter value and produce
 * duplicate booking IDs.  AtomicLong.incrementAndGet() is guaranteed unique
 * under any level of concurrency in a single JVM.
 *
 * The counter starts at 1 on each application start.  For persistence across
 * restarts, the BookingStore initialises the counter to (max existing ID + 1)
 * when the application starts.
 */
public final class IdGenerator {

    // The counter for booking IDs (thread-safe by AtomicLong)
    private static final AtomicLong BOOKING_COUNTER = new AtomicLong(1);

    private IdGenerator() {}

    /**
     * Sets the next value of the booking counter.
     * Called once at startup by BookingStore after reading existing bookings.
     */
    public static void initBookingCounter(long nextValue) {
        BOOKING_COUNTER.set(nextValue);
    }

    /**
     * Returns a unique, formatted booking ID like "BK-00001".
     * Thread-safe: AtomicLong.getAndIncrement() is an atomic operation.
     */
    public static String nextBookingId() {
        long id = BOOKING_COUNTER.getAndIncrement();
        return String.format("BK-%05d", id);
    }
}
