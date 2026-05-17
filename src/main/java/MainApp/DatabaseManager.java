package MainApp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import javax.swing.JOptionPane;

public class DatabaseManager {

    private static final String URL = buildUrl();

    // ✅ FIXED: Uses %LOCALAPPDATA% so it works correctly inside
    //    the Windows Store MSIX sandbox. Falls back to user.home
    //    when running outside the Store (e.g. during development).
    private static String buildUrl() {
        String appData = System.getenv("LOCALAPPDATA");
        String base = (appData != null)
                ? appData
                : System.getProperty("user.home");
        return "jdbc:sqlite:" + base + "/TradingTablePro/trading_data.db";
    }

    public static Connection connect() {

        // ✅ FIXED: Was silently catching the exception, printing it
        //    to console, and returning null. Every caller then did
        //    conn.createStatement() on null and exploded with a
        //    cryptic NullPointerException.
        //
        //    Now: throws a RuntimeException so the caller's own
        //    catch block handles it and shows the user a real message.
        //    No null checks needed anywhere in your code.

        try {
            return DriverManager.getConnection(URL);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not connect to the database.\n"
                            + "Path: " + URL + "\n"
                            + "Reason: " + e.getMessage(),
                    e  // ← wraps the original cause so the stack trace is preserved
            );
        }
    }

    public static void initializeDatabase() {

        // ✅ FIXED: Folder creation and table creation are now inside
        //    a single try/catch that shows the user a dialog on failure
        //    instead of silently swallowing the error.
        //
        //    Also uses the same base path as buildUrl() so the folder
        //    and the database file always end up in the same place.

        String appData = System.getenv("LOCALAPPDATA");
        String base = (appData != null)
                ? appData
                : System.getProperty("user.home");

        String folderPath = base + "/TradingTablePro";

        String sql = """
            CREATE TABLE IF NOT EXISTS trades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pair TEXT,
                pattern TEXT,
                wave TEXT,
                diversion TEXT,
                sr TEXT,
                direction TEXT,
                entrySignal TEXT,
                outcome TEXT
            );
        """;

        try {

            java.io.File folder = new java.io.File(folderPath);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (!created) {
                    // mkdirs() returned false without throwing —
                    // surface this immediately rather than letting
                    // the DB connection fail with a confusing error
                    throw new RuntimeException(
                            "Could not create app data folder at:\n" + folderPath
                    );
                }
            }

            try (
                    Connection conn = connect(); // ← throws RuntimeException if it fails
                    Statement stmt = conn.createStatement()
            ) {
                stmt.execute(sql);
            }

        } catch (Exception e) {
            // Show the user exactly what went wrong at startup
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to initialise the database:\n\n" + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
            // Re-throw so main() stops instead of opening a broken window
            throw new RuntimeException(e);
        }
    }
}