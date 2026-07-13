package MainApp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.swing.JOptionPane;

public class DatabaseManager {

    private static final String URL = buildUrl();

    private static String buildUrl() {
        String appData = System.getenv("LOCALAPPDATA");
        String base = (appData != null) ? appData : System.getProperty("user.home");
        return "jdbc:sqlite:" + base + "/TradingTablePro/trading_data.db";
    }

    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not connect to the database.\nPath: " + URL + "\nReason: " + e.getMessage(), e);
        }
    }

    public static void initializeDatabase() {
        String appData = System.getenv("LOCALAPPDATA");
        String base = (appData != null) ? appData : System.getProperty("user.home");
        String folderPath = base + "/TradingTablePro";

        String tradesSql = """
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

        String journalSql = """
            CREATE TABLE IF NOT EXISTS journal_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entry_date TEXT,
                note TEXT
            );
        """;

        String settingsSql = """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                value TEXT
            );
        """;

        try {
            java.io.File folder = new java.io.File(folderPath);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (!created) {
                    throw new RuntimeException("Could not create app data folder at:\n" + folderPath);
                }
            }

            try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
                stmt.execute(tradesSql);
                stmt.execute(journalSql);
                stmt.execute(settingsSql);

                // Migration: add trade_date if this DB predates it. SQLite has
                // no "ADD COLUMN IF NOT EXISTS", so check pragma first.
                boolean hasDateColumn = false;
                try (ResultSet cols = stmt.executeQuery("PRAGMA table_info(trades)")) {
                    while (cols.next()) {
                        if ("trade_date".equalsIgnoreCase(cols.getString("name"))) {
                            hasDateColumn = true;
                            break;
                        }
                    }
                }
                if (!hasDateColumn) {
                    stmt.execute("ALTER TABLE trades ADD COLUMN trade_date TEXT");
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to initialise the database:\n\n" + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }
    }

    public static void updateTrade(Connection conn, int id, String pair, String pattern, String wave,
                                    String diversion, String sr, String direction,
                                    String entrySignal, String outcome) throws SQLException {
        String sql = """
            UPDATE trades SET pair=?, pattern=?, wave=?, diversion=?, sr=?,
                               direction=?, entrySignal=?, outcome=?
            WHERE id=?
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pair);
            pstmt.setString(2, pattern);
            pstmt.setString(3, wave);
            pstmt.setString(4, diversion);
            pstmt.setString(5, sr);
            pstmt.setString(6, direction);
            pstmt.setString(7, entrySignal);
            pstmt.setString(8, outcome);
            pstmt.setInt(9, id);
            pstmt.executeUpdate();
        }
    }

    public static int insertTrade(Connection conn, String pair, String pattern, String wave,
                                   String diversion, String sr, String direction,
                                   String entrySignal, String outcome) throws SQLException {
        String sql = """
            INSERT INTO trades (pair, pattern, wave, diversion, sr, direction, entrySignal, outcome, trade_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, pair);
            pstmt.setString(2, pattern);
            pstmt.setString(3, wave);
            pstmt.setString(4, diversion);
            pstmt.setString(5, sr);
            pstmt.setString(6, direction);
            pstmt.setString(7, entrySignal);
            pstmt.setString(8, outcome);
            pstmt.setString(9, java.time.LocalDate.now().toString());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Insert succeeded but no generated id was returned.");
    }

    public static void deleteTrades(Connection conn, List<Integer> ids) throws SQLException {
        if (ids.isEmpty()) return;
        String sql = "DELETE FROM trades WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int id : ids) {
                pstmt.setInt(1, id);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public static void insertJournalEntry(Connection conn, String entryDate, String note) throws SQLException {
        String sql = "INSERT INTO journal_entries (entry_date, note) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entryDate);
            pstmt.setString(2, note);
            pstmt.executeUpdate();
        }
    }

    public static void deleteJournalEntry(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM journal_entries WHERE id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public static String getSetting(Connection conn, String key, String defaultValue) throws SQLException {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("value");
            }
        }
        return defaultValue;
    }

    public static void setSetting(Connection conn, String key, String value) throws SQLException {
        String sql = "INSERT INTO app_settings (key, value) VALUES (?, ?) "
                + "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
    }
}
