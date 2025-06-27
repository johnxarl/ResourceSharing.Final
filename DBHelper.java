package resourcesharing;


	import java.sql.Connection;
	import java.sql.DriverManager;
	import java.sql.SQLException;
	import java.sql.Statement;

	public class DBHelper {
	    private static final String URL = "jdbc:sqlite:resourcesharing.db";

	    static {
	        try (Connection conn = getConnection()) {
	            Statement stmt = conn.createStatement();
	            stmt.execute("""
	                CREATE TABLE IF NOT EXISTS items (
	                    id INTEGER PRIMARY KEY AUTOINCREMENT,
	                    name TEXT NOT NULL,
	                    owner TEXT NOT NULL,
	                    category TEXT,
	                    is_available INTEGER NOT NULL DEFAULT 1
	                );
	            """);
	        } catch (SQLException e) {
	            System.err.println("Error initializing DB: " + e.getMessage());
	        }
	    }

	    public static Connection getConnection() throws SQLException {
	        return DriverManager.getConnection(URL);
	    }
	}

