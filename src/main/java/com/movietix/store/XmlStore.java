package com.movietix.store;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Low-level XML file reader/writer used by all *Store classes.
 *
 * -----------------------------------------------------------------------
 * WHY THE ReentrantReadWriteLock?
 * -----------------------------------------------------------------------
 * XML files are plain files on disk.  Without synchronisation:
 *
 *   Thread A (write): reads file → modifies DOM → writes file
 *   Thread B (write): reads file → modifies DOM → writes file   ← RACES A
 *
 * Thread B could overwrite Thread A's changes, or one of them could
 * read a half-written file.  This is a classic read-modify-write race.
 *
 * A ReentrantReadWriteLock allows:
 *   - MULTIPLE concurrent readers (no contention for reads).
 *   - EXCLUSIVE writer access (only one thread writes at a time, and no
 *     readers can read while a write is in progress).
 *
 * Each XML file gets its OWN lock instance so reads/writes on different
 * files don't block each other unnecessarily.
 *
 * -----------------------------------------------------------------------
 * WHY ATOMIC WRITES (write-to-temp then rename)?
 * -----------------------------------------------------------------------
 * Writing directly to the target file is dangerous:
 *
 *   1. Thread writes first half of file → server crashes / power fails
 *   2. File is now half-written and corrupt on disk
 *
 * Instead we write to a temp file (foo.xml.tmp) and then do an atomic
 * rename (Files.move with ATOMIC_MOVE or REPLACE_EXISTING).  The OS
 * guarantees that rename is atomic: readers either see the old complete
 * file or the new complete file, never a partial file.
 *
 * -----------------------------------------------------------------------
 * USAGE PATTERN (for subclasses)
 * -----------------------------------------------------------------------
 *
 *   // Read
 *   lock.readLock().lock();
 *   try {
 *       Document doc = XmlStore.readDocument(path);
 *       ...
 *   } finally { lock.readLock().unlock(); }
 *
 *   // Write
 *   lock.writeLock().lock();
 *   try {
 *       Document doc = XmlStore.readDocument(path);
 *       // mutate doc ...
 *       XmlStore.writeDocument(doc, path);
 *   } finally { lock.writeLock().unlock(); }
 */
public final class XmlStore {

    private static final Logger LOG = Logger.getLogger(XmlStore.class.getName());

    /** Prevent instantiation – all methods are static helpers. */
    private XmlStore() {}

    // -------------------------------------------------------------------------
    // Static helpers used by all *Store classes
    // -------------------------------------------------------------------------

    /**
     * Reads and parses an XML file, returning its DOM {@link Document}.
     * Must be called while holding at least a read lock on the file's lock.
     *
     * @param path  absolute path to the XML file
     * @return parsed Document
     * @throws RuntimeException if the file is missing or malformed
     */
    public static Document readDocument(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external DTD/entity loading (security: XML injection)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException("Failed to read XML: " + path, e);
        }
    }

    /**
     * Writes a DOM {@link Document} to disk atomically:
     *   1. Write to &lt;path&gt;.tmp
     *   2. Rename .tmp → &lt;path&gt; (atomic on most OS/file-systems)
     *
     * Must be called while holding the write lock on the file's lock.
     *
     * @param doc   the DOM document to serialise
     * @param path  target XML file path
     */
    public static void writeDocument(Document doc, Path path) {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            // Step 1: write to temp file
            try (OutputStream out = Files.newOutputStream(tmp)) {
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
            // Step 2: atomically replace original with temp file
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            LOG.fine("Wrote XML file: " + path);
        } catch (IOException | TransformerException e) {
            // Attempt cleanup of temp file on error
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw new RuntimeException("Failed to write XML: " + path, e);
        }
    }

    /**
     * Creates a new empty Document with the given root element name.
     * Useful when a data file doesn't exist yet and needs to be bootstrapped.
     */
    public static Document newDocument(String rootElement) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            doc.appendChild(doc.createElement(rootElement));
            return doc;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to create new XML Document", e);
        }
    }

    /**
     * Creates and returns a new {@link ReadWriteLock} for a file.
     * Each *Store calls this once and keeps the lock as a final field.
     */
    public static ReadWriteLock newFileLock() {
        return new ReentrantReadWriteLock();
    }
}
