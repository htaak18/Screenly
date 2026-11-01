package com.movietix.store;

import com.movietix.model.Seat;
import com.movietix.model.Seat.SeatState;
import org.w3c.dom.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;

/**
 * Reads and writes seats.xml – ONLY for BOOKED seats.
 *
 * Why only BOOKED?  HELD seats are transient (they expire in 7 minutes).
 * Persisting them on every hold/release would be very chatty.  Instead:
 *   - On shutdown/restart, we treat every non-BOOKED seat as AVAILABLE.
 *   - SeatService loads booked seats from this file at startup.
 *
 * XML schema:
 * <pre>
 * &lt;seats&gt;
 *   &lt;seat&gt;
 *     &lt;showId&gt;sh1&lt;/showId&gt;
 *     &lt;seatId&gt;A1&lt;/seatId&gt;
 *     &lt;state&gt;BOOKED&lt;/state&gt;
 *     &lt;bookingId&gt;BK-00001&lt;/bookingId&gt;
 *   &lt;/seat&gt;
 * &lt;/seats&gt;
 * </pre>
 */
public class SeatStore {

    private static final Logger LOG = Logger.getLogger(SeatStore.class.getName());

    private final Path          filePath;
    private final ReadWriteLock lock = XmlStore.newFileLock();

    public SeatStore(Path filePath) {
        this.filePath = filePath;
    }

    // -------------------------------------------------------------------------

    /** Returns all BOOKED seats from seats.xml. */
    public List<Seat> findAllBooked() {
        lock.readLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            return parseSeats(doc);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Marks a list of seats as BOOKED in the XML file.
     * Called by SeatService inside the booking confirmation path (write lock held by SeatService).
     * This method acquires the XML file's write lock independently.
     */
    public void markBooked(String showId, List<String> seatIds, String bookingId) {
        lock.writeLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            Element root = doc.getDocumentElement();
            for (String seatId : seatIds) {
                Element elem = doc.createElement("seat");
                addChild(doc, elem, "showId",    showId);
                addChild(doc, elem, "seatId",    seatId);
                addChild(doc, elem, "state",     SeatState.BOOKED.name());
                addChild(doc, elem, "bookingId", bookingId);
                root.appendChild(elem);
            }
            XmlStore.writeDocument(doc, filePath);
            LOG.info("Persisted BOOKED seats for booking " + bookingId + ": " + seatIds);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes booked seat entries for a given booking (cancellation).
     * Seats are removed from the file – they will be AVAILABLE on next load.
     */
    public void removeByBookingId(String bookingId) {
        lock.writeLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            Element root = doc.getDocumentElement();
            NodeList nodes = root.getElementsByTagName("seat");
            // collect nodes to remove (can't remove while iterating NodeList)
            List<Node> toRemove = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                if (bookingId.equals(text(e, "bookingId"))) {
                    toRemove.add(e);
                }
            }
            toRemove.forEach(root::removeChild);
            XmlStore.writeDocument(doc, filePath);
            LOG.info("Removed seat entries for cancelled booking: " + bookingId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------

    private List<Seat> parseSeats(Document doc) {
        List<Seat> seats = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("seat");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            Seat seat = new Seat(text(e, "showId"), text(e, "seatId"));
            seat.setState(SeatState.valueOf(text(e, "state")));
            seat.setBookingId(text(e, "bookingId"));
            seats.add(seat);
        }
        return seats;
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
