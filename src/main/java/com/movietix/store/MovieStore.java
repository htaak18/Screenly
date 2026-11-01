package com.movietix.store;

import com.movietix.model.Movie;
import org.w3c.dom.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;

/**
 * Reads and writes movies.xml.
 *
 * XML schema:
 * <pre>
 * &lt;movies&gt;
 *   &lt;movie&gt;
 *     &lt;id&gt;m1&lt;/id&gt;
 *     &lt;title&gt;Inception&lt;/title&gt;
 *     &lt;genre&gt;Sci-Fi&lt;/genre&gt;
 *     &lt;description&gt;...&lt;/description&gt;
 *     &lt;durationMinutes&gt;148&lt;/durationMinutes&gt;
 *     &lt;language&gt;English&lt;/language&gt;
 *     &lt;rating&gt;U/A&lt;/rating&gt;
 *     &lt;posterUrl&gt;/static/img/inception.jpg&lt;/posterUrl&gt;
 *   &lt;/movie&gt;
 * &lt;/movies&gt;
 * </pre>
 */
public class MovieStore {

    private static final Logger LOG = Logger.getLogger(MovieStore.class.getName());

    private final Path          filePath;
    private final ReadWriteLock lock = XmlStore.newFileLock();

    public MovieStore(Path filePath) {
        this.filePath = filePath;
    }

    // -------------------------------------------------------------------------

    public List<Movie> findAll() {
        lock.readLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            return parseMovies(doc);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Movie> findById(String id) {
        return findAll().stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    /** Appends a new movie to movies.xml. */
    public void save(Movie movie) {
        lock.writeLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            Element root = doc.getDocumentElement();

            Element elem = doc.createElement("movie");
            addChild(doc, elem, "id",              movie.getId());
            addChild(doc, elem, "title",           movie.getTitle());
            addChild(doc, elem, "genre",           movie.getGenre());
            addChild(doc, elem, "description",     movie.getDescription());
            addChild(doc, elem, "durationMinutes", String.valueOf(movie.getDurationMinutes()));
            addChild(doc, elem, "language",        movie.getLanguage());
            addChild(doc, elem, "rating",          movie.getRating());
            addChild(doc, elem, "posterUrl",       movie.getPosterUrl());
            root.appendChild(elem);

            XmlStore.writeDocument(doc, filePath);
            LOG.info("Saved movie: " + movie.getTitle());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------

    private List<Movie> parseMovies(Document doc) {
        List<Movie> movies = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("movie");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            int dur = 0;
            try { dur = Integer.parseInt(text(e, "durationMinutes")); } catch (NumberFormatException ex) { /* ignore */ }
            movies.add(new Movie(
                text(e, "id"),
                text(e, "title"),
                text(e, "genre"),
                text(e, "description"),
                dur,
                text(e, "language"),
                text(e, "rating"),
                text(e, "posterUrl")
            ));
        }
        return movies;
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
