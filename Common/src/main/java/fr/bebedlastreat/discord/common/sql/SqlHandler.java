package fr.bebedlastreat.discord.common.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

@Getter
public class SqlHandler extends AbstractSqlHandler {
    public static final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";

    @Getter
    private static SqlHandler instance;
    private final SqlCredentials credentials;
    private final int maxPoolSize;
    private final int minIdle;
    private final long maxLifetime;
    private final long keepaliveTime;
    private final long connectionTimeout;

    public SqlHandler(SqlCredentials credentials, int maxPoolSize, int minIdle, long maxLifetime, long keepaliveTime, long connectionTimeout, String table) {
        super(table);
        this.credentials = credentials;
        instance = this;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.maxLifetime = maxLifetime;
        this.keepaliveTime = keepaliveTime;
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    protected HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(credentials.getDriver());
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setMaxLifetime(maxLifetime);
        config.setKeepaliveTime(keepaliveTime);
        config.setConnectionTimeout(connectionTimeout);
        config.setInitializationFailTimeout(-1);
        config.setJdbcUrl(credentials.toUrl());
        config.setUsername(credentials.getUser());
        config.setPassword(credentials.getPass());
        return new HikariDataSource(config);
    }
}
