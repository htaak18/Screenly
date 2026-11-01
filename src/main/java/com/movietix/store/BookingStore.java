package com.movietix.store;

import com.movietix.model.Booking;
import com.movietix.model.Booking.BookingStatus;
import com.movietix.util.IdGenerator;
import org.w3c.dom.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Reads and writes bookings.xml.
 *
 * On startup, initialises IdGenerator with (maxId + 1) so booking IDs
 * remain globally unique even across application restarts.
 *
 * XML schema:
 * <pre>
 * &lt;bookings&gt;
 *   &lt;booking&gt;
 *     &lt;id&gt;BK-00001&lt;/id&gt;
 *     &lt;userId&gt;u2&lt;/userId&gt;
 *     &lt;showId&gt;sh1&lt;/showId&gt;
 *     &lt;movieTitle&gt;Inception&lt;/movieTitle&gt;
 *     &lt;showDateTime&gt;2024-12-25T18:30:00&lt;/showDateTime&gt;
 *     &lt;seatIds&gt;A1,A2,A3&lt;/seatIds&gt;     (comma-separated)
 *     &lt;totalPrice&gt;750.0&lt;/totalPrice&gt;
 *     &lt;status&gt;CONFIRMED&lt;/status&gt;
 *     &lt;bookedAt&gt;2024-12-24T10:15:00&lt;/bookedAt&gt;
 *   &lt;/booking&gt;
 * &lt;/bookings&gt;
 * </pre>
 */
public class BookingStore {

    private static final Logger LOG = Logger.getLogger(BookingStore.class.getName());

    private final Path          filePath;
    private final ReadWriteLock lock = XmlStore.newFileLock();

    public BookingStore(Path filePath) {
        this.filePath = filePath;
        initCounter();
    }

    // -------------------------------------------------------------------------

    public List<Booking> findAll() {
        lock.readLock().lock();
        try {
            return parseBookings(XmlStore.readDocument(filePath));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Booking> findById(String id) {
        return findAll().stream().filter(b -> b.getId().equals(id)).findFirst();
    }

    public List<Booking> findByUserId(String userId) {
        return findAll().stream()
            .filter(b -> b.getUserId().equals(userId))
            .collect(Collectors.toList());
    }

    /** Persists a new booking. */
    public void save(Booking booking) {
        lock.writeLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            Element root = doc.getDocumentElement();
            root.appendChild(toElement(doc, booking));
            XmlStore.writeDocument(doc, filePath);
            LOG.info("Saved booking: " + booking.getId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Updates the status of an existing booking (CONFIRMED → CANCELLED). */
    public void updateStatus(String bookingId, BookingStatus newStatus) {
        lock.writeLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            NodeList nodes = doc.getElementsByTagName("booking");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                if (bookingId.equals(text(e, "id"))) {
                    NodeList statusNodes = e.getElementsByTagName("status");
                    if (statusNodes.getLength() > 0) {
                        statusNodes.item(0).setTextContent(newStatus.name());
                    }
                    break;
                }
            }
            XmlStore.writeDocument(doc, filePath);
            LOG.info("Updated booking " + bookingId + " status → " + newStatus);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Startup counter initialisation
    // -------------------------------------------------------------------------

    /**
     * Parses existing bookings and sets IdGenerator to (max numeric ID + 1).
     * Called from constructor so the counter is ready before any servlet runs.
     */
    private void initCounter() {
        lock.readLock().lock();
        try {
            List<Booking> existing = parseBookings(XmlStore.readDocument(filePath));
            long max = existing.stream()
                .map(Booking::getId)           // "BK-00042"
                .filter(id -> id.startsWith("BK-"))
                .mapToLong(id -> {
                    try { return Long.parseLong(id.substring(3)); }
                    catch (NumberFormatException ex) { return 0; }
                })
                .max()
                .orElse(0L);
            IdGenerator.initBookingCounter(max + 1);
            LOG.info("Booking counter initialised to " + (max + 1));
        } finally {
            lock.readLock().unlock();
        }
    }

    // -------------------------------------------------------------------------
    // XML helpers
    // -------------------------------------------------------------------------

    private List<Booking> parseBookings(Document doc) {
        List<Booking> bookings = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("booking");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e  = (Element) nodes.item(i);
            String seatsCsv = text(e, "seatIds");
            List<String> seats = seatsCsv.isEmpty()
                ? new ArrayList<>()
                : Arrays.asList(seatsCsv.split(","));
            double price = 0;
            try { price = Double.parseDouble(text(e, "totalPrice")); } catch (NumberFormatException ex) { /* ignore */ }
            bookings.add(new Booking(
                text(e, "id"),
                text(e, "userId"),
                text(e, "showId"),
                text(e, "movieTitle"),
                text(e, "showDateTime"),
                seats,
                price,
                BookingStatus.valueOf(text(e, "status")),
                text(e, "bookedAt")
            ));
        }
        return bookings;
    }

    private Element toElement(Document doc, Booking b) {
        Element elem = doc.createElement("booking");
        addChild(doc, elem, "id",           b.getId());
        addChild(doc, elem, "userId",       b.getUserId());
        addChild(doc, elem, "showId",       b.getShowId());
        addChild(doc, elem, "movieTitle",   b.getMovieTitle());
        addChild(doc, elem, "showDateTime", b.getShowDateTime());
        addChild(doc, elem, "seatIds",      String.join(",", b.getSeatIds()));
        addChild(doc, elem, "totalPrice",   String.valueOf(b.getTotalPrice()));
        addChild(doc, elem, "status",       b.getStatus().name());
        addChild(doc, elem, "bookedAt",     b.getBookedAt());
        return elem;
    }

    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent().trim();
    }

    private static void addChild(Document doc, Element parent, String tag, String value) {
        Element child = doc.createElement(tag);
        child.setTextContent(value == null ? "" : value);
        parent.appendChild(child);
    }
}
