package com.movietix.listener;

import com.movietix.model.Show;
import com.movietix.service.AuthService;
import com.movietix.service.SeatService;
import com.movietix.store.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * ServletContextListener – application lifecycle management.
 *
 * Runs on startup (contextInitialized) and shutdown (contextDestroyed).
 *
 * Responsibilities:
 *  1. Locate / copy seed data XML files to the data directory.
 *  2. Instantiate all service/store singletons and publish them to
 *     ServletContext attributes (accessible from all servlets).
 *  3. Start the ScheduledExecutorService that purges expired holds every 10s.
 *  4. On shutdown: gracefully shut down the scheduler.
 *
 * WHY A LISTENER, not static initialisers?
 *   - We need the ServletContext to locate the data directory.
 *   - Static initialisers run in a classloader context where we can't easily
 *     access Servlet APIs.
 *   - Listeners give us a clean start/stop lifecycle.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    /** Key used to store/retrieve SeatService from ServletContext. */
    public static final String SEAT_SERVICE    = "seatService";
    public static final String AUTH_SERVICE    = "authService";
    public static final String MOVIE_STORE     = "movieStore";
    public static final String SHOW_STORE      = "showStore";
    public static final String BOOKING_STORE   = "bookingStore";

    /** The background scheduler for expiring holds. */
    private ScheduledExecutorService scheduler;

    // -------------------------------------------------------------------------
    // Startup
    // -------------------------------------------------------------------------

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("=== MovieTix Application Starting ===");
        ServletContext ctx = sce.getServletContext();

        // ---- Step 1: Resolve data directory ----
        // Priority: system property > WEB-INF/data (fallback for embedded Tomcat)
        Path dataDir = resolveDataDirectory(ctx);
        LOG.info("Data directory: " + dataDir);

        // ---- Step 2: Copy seed files if they don't exist ----
        copySeedIfMissing(ctx, dataDir, "users.xml");
        copySeedIfMissing(ctx, dataDir, "movies.xml");
        copySeedIfMissing(ctx, dataDir, "shows.xml");
        copySeedIfMissing(ctx, dataDir, "seats.xml");
        copySeedIfMissing(ctx, dataDir, "bookings.xml");

        // ---- Step 3: Create stores ----
        UserStore    userStore    = new UserStore   (dataDir.resolve("users.xml"));
        MovieStore   movieStore   = new MovieStore  (dataDir.resolve("movies.xml"));
        ShowStore    showStore    = new ShowStore   (dataDir.resolve("shows.xml"));
        SeatStore    seatStore    = new SeatStore   (dataDir.resolve("seats.xml"));
        BookingStore bookingStore = new BookingStore(dataDir.resolve("bookings.xml"));

        // ---- Step 4: Create services ----
        Clock       clock       = Clock.systemUTC();
        SeatService seatService = new SeatService(clock, seatStore, bookingStore);
        AuthService authService = new AuthService(userStore);

        // ---- Step 5: Initialise seat grids from shows ----
        List<Show> shows = showStore.findAll();
        for (Show show : shows) {
            seatService.initShow(show.getId());
        }
        seatService.loadBookedSeats();
        LOG.info("Initialised seat grids for " + shows.size() + " shows.");

        // ---- Step 6: Publish to ServletContext ----
        ctx.setAttribute(SEAT_SERVICE,  seatService);
        ctx.setAttribute(AUTH_SERVICE,  authService);
        ctx.setAttribute(MOVIE_STORE,   movieStore);
        ctx.setAttribute(SHOW_STORE,    showStore);
        ctx.setAttribute(BOOKING_STORE, bookingStore);

        // ---- Step 7: Start scheduled hold expiry task ----
        //
        // WHY ScheduledExecutorService?
        //   - We need a background task to proactively release expired holds
        //     so the seat grid refreshes for other users (complementing lazy expiry).
        //   - ScheduledExecutorService is the standard Java concurrency tool for
        //     periodic tasks (superior to java.util.Timer which swallows exceptions).
        //   - We use a daemon thread so the JVM can exit even if this thread is
        //     running (it will be stopped in contextDestroyed anyway).
        //
        // WHY a single thread?
        //   - The task itself acquires per-show locks.  Multiple threads would
        //     just queue up on the same locks – parallelism gains nothing.
        //
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hold-expiry-daemon");
            t.setDaemon(true);  // daemon: JVM exits even if this thread is alive
            return t;
        });

        final SeatService finalSeatService = seatService;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LOG.fine("[hold-expiry-daemon] Checking for expired holds...");
                finalSeatService.purgeAllExpiredHolds();
            } catch (Exception e) {
                // Catch ALL exceptions: if this task throws, the scheduler silently
                // stops scheduling it (ScheduledExecutorService quirk).
                LOG.warning("Error in hold expiry task: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);  // start after 10s, repeat every 10s

        LOG.info("=== MovieTix Application Started. Hold expiry scheduler running. ===");
    }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("=== MovieTix Application Shutting Down ===");

        // Gracefully stop the scheduler: no new tasks, wait up to 5s for current task
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    LOG.warning("Forced scheduler shutdown after timeout.");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        LOG.info("=== MovieTix Application Stopped. ===");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Path resolveDataDirectory(ServletContext ctx) {
        // 1. System property (set by Cargo: -Dmovietix.data.dir=...)
        String sysProp = System.getProperty("movietix.data.dir");
        if (sysProp != null) {
            Path p = Paths.get(sysProp);
            if (Files.isDirectory(p)) return p;
        }
        // 2. Real path of WEB-INF/data (works when deployed as WAR)
        String realPath = ctx.getRealPath("/WEB-INF/data");
        if (realPath != null) {
            Path p = Paths.get(realPath);
            try {
                Files.createDirectories(p);
                return p;
            } catch (Exception e) {
                LOG.warning("Could not create WEB-INF/data: " + e.getMessage());
            }
        }
        // 3. Fallback: current working directory / data
        return Paths.get("data");
    }

    /**
     * Copies a seed XML file from the classpath (resources/data/) to the data
     * directory if it doesn't already exist.  This ensures the app has data to
     * work with on first run without manual steps.
     */
    private void copySeedIfMissing(ServletContext ctx, Path dataDir, String filename) {
        Path target = dataDir.resolve(filename);
        if (Files.exists(target)) return;

        // Try classpath resource first (from src/main/resources/data/)
        String resourcePath = "/WEB-INF/classes/data/" + filename;
        try (InputStream in = ctx.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.createDirectories(dataDir);
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Copied seed file: " + filename + " → " + target);
                return;
            }
        } catch (Exception e) {
            LOG.warning("Error copying seed file " + filename + ": " + e.getMessage());
        }

        // Fallback: create empty XML root
        try {
            Files.createDirectories(dataDir);
            String rootTag = filename.replace(".xml", "");
            Files.writeString(target, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<" + rootTag + "/>\n");
            LOG.info("Created empty seed file: " + target);
        } catch (Exception e) {
            LOG.severe("Cannot create seed file " + filename + ": " + e.getMessage());
        }
    }
}
