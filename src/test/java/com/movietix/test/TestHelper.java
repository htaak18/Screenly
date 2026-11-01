package com.movietix.test;

import com.movietix.model.Booking;
import com.movietix.model.Show;
import com.movietix.service.SeatService;
import com.movietix.store.BookingStore;
import com.movietix.store.SeatStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared test utilities: creates in-memory SeatService instances
 * with a controllable clock and no-op XML stores (so tests don't
 * touch the real data files).
 */
public class TestHelper {

    public static final String SHOW_ID = "test-show-1";

    /** A Show object used in payment confirmation tests. */
    public static final Show TEST_SHOW = new Show(
        SHOW_ID, "m-test", "Test Movie",
        "2027-12-31T20:00:00", 200.0, "Hall 1"
    );

    // -------------------------------------------------------------------------
    // Adjustable clock
    // -------------------------------------------------------------------------

    /**
     * An {@link AtomicLong} wrapping epoch millis.
     * Tests advance this to simulate time passing (e.g. hold expiry).
     */
    public static class MutableClock extends Clock {
        private final AtomicLong epochMillis;

        public MutableClock(long startMillis) {
            this.epochMillis = new AtomicLong(startMillis);
        }

        /** Advance time by the given number of milliseconds. */
        public void advance(long millis) {
            epochMillis.addAndGet(millis);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(epochMillis.get()); }

        /** Returns current epoch millis (same as Clock.millis()). */
        public long millis() { return epochMillis.get(); }
    }

    // -------------------------------------------------------------------------
    // No-op stores (avoid touching real XML files in unit tests)
    // -------------------------------------------------------------------------

    /** SeatStore that does nothing (tests verify in-memory state only). */
    public static SeatStore noOpSeatStore() throws IOException {
        Path tmp = Files.createTempFile("seats_test_", ".xml");
        Files.writeString(tmp, "<?xml version=\"1.0\"?><seats/>");
        return new SeatStore(tmp);
    }

    /** BookingStore backed by a temp file (for ID generation). */
    public static BookingStore noOpBookingStore() throws IOException {
        Path tmp = Files.createTempFile("bookings_test_", ".xml");
        Files.writeString(tmp, "<?xml version=\"1.0\"?><bookings/>");
        return new BookingStore(tmp);
    }

    /**
     * Creates a fully initialised SeatService with a mutable clock
     * and a single test show's seat grid loaded.
     */
    public static SeatService createService(MutableClock clock) throws IOException {
        SeatService svc = new SeatService(clock, noOpSeatStore(), noOpBookingStore());
        svc.initShow(SHOW_ID);
        return svc;
    }
}
