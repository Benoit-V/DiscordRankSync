package fr.bebedlastreat.discord.common.sql;

import fr.bebedlastreat.discord.common.interfaces.IDatabaseFetch;

import java.util.UUID;

/**
 * {@link IDatabaseFetch} for every JDBC backend (MySQL/MariaDB and SQLite).
 */
public class SqlFetch implements IDatabaseFetch {

    private final AbstractSqlHandler sql;

    public SqlFetch(AbstractSqlHandler sql) {
        this.sql = sql;
    }

    private String queryString(String qry, String column, Object param) {
        return sql.query(qry, rs -> {
            if (rs.next()) {
                String value = rs.getString(column);
                return value == null ? "" : value;
            }
            return "";
        }, param);
    }

    private int queryCount(String table) {
        return sql.query("SELECT COUNT(*) FROM " + table, rs -> rs.next() ? rs.getInt(1) : 0);
    }

    @Override
    public boolean insert(UUID uuid, String name, String discord) {
        return sql.update("INSERT INTO " + sql.getTable() + " (uuid, name, discord) VALUES (?, ?, ?)", uuid.toString(), name, discord) > 0;
    }

    @Override
    public void update(UUID uuid, String name) {
        sql.update("UPDATE " + sql.getTable() + " SET name=? WHERE uuid=?", name, uuid.toString());
    }

    @Override
    public boolean firstLink(UUID uuid) {
        return sql.query("SELECT uuid FROM " + sql.getAllTimeTable() + " WHERE uuid=?", rs -> !rs.next(), uuid.toString());
    }

    @Override
    public void insertFirstLink(UUID uuid) {
        sql.update("INSERT INTO " + sql.getAllTimeTable() + " (uuid) VALUES (?)", uuid.toString());
    }

    @Override
    public boolean delete(UUID uuid) {
        return sql.update("DELETE FROM " + sql.getTable() + " WHERE uuid=?", uuid.toString()) > 0;
    }

    @Override
    public boolean delete(String discord) {
        return sql.update("DELETE FROM " + sql.getTable() + " WHERE discord=?", discord) > 0;
    }

    @Override
    public boolean exist(String discord) {
        return !uuid(discord).isEmpty();
    }

    @Override
    public boolean exist(UUID uuid) {
        return !discord(uuid).isEmpty();
    }

    @Override
    public String discord(UUID uuid) {
        return queryString("SELECT discord FROM " + sql.getTable() + " WHERE uuid=?", "discord", uuid.toString());
    }

    @Override
    public String discord(String name) {
        return queryString("SELECT discord FROM " + sql.getTable() + " WHERE name=?", "discord", name);
    }

    @Override
    public String uuid(String discord) {
        return queryString("SELECT uuid FROM " + sql.getTable() + " WHERE discord=?", "uuid", discord);
    }

    @Override
    public String name(UUID uuid) {
        return queryString("SELECT name FROM " + sql.getTable() + " WHERE uuid=?", "name", uuid.toString());
    }

    @Override
    public int count() {
        return queryCount(sql.getTable());
    }

    @Override
    public int allTimeCount() {
        return queryCount(sql.getAllTimeTable());
    }

    @Override
    public void insertBoost(UUID uuid, long time) {
        sql.update("INSERT INTO " + sql.getBoostTable() + " (uuid, finish_time) VALUES (?, ?)", uuid.toString(), time);
    }

    @Override
    public void deleteBoost(UUID uuid) {
        sql.update("DELETE FROM " + sql.getBoostTable() + " WHERE uuid=?", uuid.toString());
    }

    @Override
    public boolean canBoost(UUID uuid) {
        return System.currentTimeMillis() > getNextBoost(uuid);
    }

    @Override
    public long getNextBoost(UUID uuid) {
        return sql.query("SELECT finish_time FROM " + sql.getBoostTable() + " WHERE uuid=?",
                rs -> rs.next() ? rs.getLong("finish_time") : 0L, uuid.toString());
    }

    @Override
    public void close() {
        sql.close();
    }
}
