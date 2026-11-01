package com.movietix.store;

import com.movietix.model.User;
import org.w3c.dom.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;

/**
 * Reads and writes users.xml.
 *
 * XML schema:
 * <pre>
 * &lt;users&gt;
 *   &lt;user&gt;
 *     &lt;id&gt;u1&lt;/id&gt;
 *     &lt;username&gt;alice&lt;/username&gt;
 *     &lt;email&gt;alice@example.com&lt;/email&gt;
 *     &lt;passwordHash&gt;...64 hex chars...&lt;/passwordHash&gt;
 *     &lt;salt&gt;...32 hex chars...&lt;/salt&gt;
 *     &lt;role&gt;USER&lt;/role&gt;
 *   &lt;/user&gt;
 * &lt;/users&gt;
 * </pre>
 *
 * Thread-safety: all public methods acquire the per-file ReadWriteLock
 * (read lock for reads, write lock for mutations) via XmlStore helpers.
 */
public class UserStore {

    private static final Logger LOG = Logger.getLogger(UserStore.class.getName());

    private final Path          filePath;
    /** Per-file lock – prevents concurrent write corruption. See XmlStore. */
    private final ReadWriteLock lock = XmlStore.newFileLock();

    public UserStore(Path filePath) {
        this.filePath = filePath;
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /** Returns all users from the file. */
    public List<User> findAll() {
        lock.readLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            return parseUsers(doc);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Finds a user by username (case-sensitive). */
    public Optional<User> findByUsername(String username) {
        return findAll().stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst();
    }

    /** Finds a user by ID. */
    public Optional<User> findById(String id) {
        return findAll().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst();
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /** Appends a new user to the file. */
    public void save(User user) {
        lock.writeLock().lock();
        try {
            Document doc = XmlStore.readDocument(filePath);
            Element root = doc.getDocumentElement();

            Element elem = doc.createElement("user");
            addChild(doc, elem, "id",           user.getId());
            addChild(doc, elem, "username",     user.getUsername());
            addChild(doc, elem, "email",        user.getEmail());
            addChild(doc, elem, "passwordHash", user.getPasswordHash());
            addChild(doc, elem, "salt",         user.getSalt());
            addChild(doc, elem, "role",         user.getRole());
            root.appendChild(elem);

            XmlStore.writeDocument(doc, filePath);
            LOG.info("Saved new user: " + user.getUsername());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------------------
    // XML parsing helpers
    // -------------------------------------------------------------------------

    private List<User> parseUsers(Document doc) {
        List<User> users = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("user");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            users.add(new User(
                text(e, "id"),
                text(e, "username"),
                text(e, "email"),
                text(e, "passwordHash"),
                text(e, "salt"),
                text(e, "role")
            ));
        }
        return users;
    }

    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        Node node = nl.item(0);
        return node == null ? "" : node.getTextContent().trim();
    }

    private static void addChild(Document doc, Element parent, String tag, String value) {
        Element child = doc.createElement(tag);
        child.setTextContent(value == null ? "" : value);
        parent.appendChild(child);
    }
}
