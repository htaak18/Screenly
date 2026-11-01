package com.movietix.store;

import com.movietix.model.Show;
import org.w3c.dom.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;

/**
 * Reads and writes shows.xml.
 *
 * XML schema:
 * <pre>
 * &lt;shows&gt;
 *   &lt;show&gt;
 *     &lt;id&gt;sh1&lt;/id&gt;
 *     &lt;movieId&gt;m1&lt;/movieId&gt;
 *     &lt;movieTitle&gt;Inception&lt;/movieTitle&gt;
 *     &lt;showDateTime&gt;2024-12-25T18:30:00&lt;/showDateTime&gt;
 *     &lt;pricePerSeat&gt;250.0&lt;/pricePerSeat&gt;
 *     &lt;hallName&gt;Hall 1&lt;/hallName&gt;
 *   &lt;/show&gt;
 * &lt;/shows&gt;
 * </pre>
 */
public class ShowStore {

    private static final Logger LOG = Logger.getLogger(ShowStore.class.getName());

    private final Path          filePath;
    private final ReadWriteLock lock = XmlStore.newFileLock();

    public ShowStore(Path filePath) {
        this.filePath = filePath;
    }

    // -------------------------------------------------------------------------

    public List<Show> findAll() {
        lock.readLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            return parseShows(doc);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Show> findByMovieId(String movieId) {
        return findAll().stream()
            .filter(s -> s.getMovieId().equals(movieId))
            .toList();
    }

    public Optional<Show> findById(String id) {
        return findAll().stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    /** Appends a new show to shows.xml. */
    public void save(Show show) {
        lock.writeLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            Element root = doc.getDocumentElement();

            Element elem = doc.createElement("show");
            addChild(doc, elem, "id",           show.getId());
            addChild(doc, elem, "movieId",      show.getMovieId());
            addChild(doc, elem, "movieTitle",   show.getMovieTitle());
            addChild(doc, elem, "showDateTime", show.getShowDateTime());
            addChild(doc, elem, "pricePerSeat", String.valueOf(show.getPricePerSeat()));
            addChild(doc, elem, "hallName",     show.getHallName());
            root.appendChild(elem);

            XmlStore.writeDocument(doc, filePath);
            LOG.info("Saved show: " + show.getId() + " for movie " + show.getMovieTitle());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------

    private List<Show> parseShows(Document doc) {
        List<Show> shows = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("show");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            double price = 0;
            try { price = Double.parseDouble(text(e, "pricePerSeat")); } catch (NumberFormatException ex) { /* ignore */ }
            shows.add(new Show(
                text(e, "id"),
                text(e, "movieId"),
                text(e, "movieTitle"),
                text(e, "showDateTime"),
                price,
                text(e, "hallName")
            ));
        }
        return shows;
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
