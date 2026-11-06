package fr.bebedlastreat.discord.common.sql;

import com.zaxxer.hikari.HikariDataSource;
import fr.bebedlastreat.discord.common.DiscordCommon;
import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Shared JDBC logic for every SQL backend (MySQL/MariaDB, SQLite).
 * Every query is a prepared statement: values are always bound as parameters, never concatenated.
 */
@Getter
public abstract class AbstractSqlHandler {

    private volatile HikariDataSource dataSource;
    private final String table;
    private final String allTimeTable;
    private final String boostTable;

    protected AbstractSqlHandler(String table) {
        this.table = table;
        this.allTimeTable = table + "_alltime";
        this.boostTable = table + "_boost";
    }

    protected abstract HikariDataSource createDataSource();

    public void initPool() {
        getDataSource();
    }

    private HikariDataSource getDataSource() {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            synchronized (this) {
                ds = dataSource;
                if (ds == null) {
                    ds = createDataSource();
                    dataSource = ds;
                }
            }
        }
        return ds;
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    /**
     * @return the number of affected rows, or -1 if the query failed
     */
    public int update(String qry, Object... params) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(qry)) {
            bind(s, params);
            return s.executeUpdate();
        } catch (SQLException e) {
            DiscordCommon.getLogger().log(Level.SEVERE, "SQL update failed (" + qry + "): " + e.getMessage());
            return -1;
        }
    }

    public <T> T query(String qry, ResultSetMapper<T> mapper, Object... params) {
        try (Connection c = getConnection();
             PreparedStatement s = c.prepareStatement(qry)) {
            bind(s, params);
            try (ResultSet rs = s.executeQuery()) {
                return mapper.map(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL query failed (" + qry + "): " + e.getMessage(), e);
        }
    }

    public void createDefault() {
        update("CREATE TABLE IF NOT EXISTS " + table + "(" +
                "uuid VARCHAR(64) PRIMARY KEY NOT NULL, " +
                "name VARCHAR(32) NOT NULL," +
                "discord VARCHAR(32) NOT NULL UNIQUE" +
                ")");
        update("CREATE TABLE IF NOT EXISTS " + allTimeTable + "(" +
                "uuid VARCHAR(64) PRIMARY KEY NOT NULL" +
                ")");
        update("CREATE TABLE IF NOT EXISTS " + boostTable + "(" +
                "uuid VARCHAR(64) PRIMARY KEY NOT NULL, " +
                "finish_time BIGINT" +
                ")");
    }

    @FunctionalInterface
    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}
