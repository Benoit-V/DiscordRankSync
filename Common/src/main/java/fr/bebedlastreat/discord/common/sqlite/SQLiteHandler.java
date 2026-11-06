package fr.bebedlastreat.discord.common.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.bebedlastreat.discord.common.sql.AbstractSqlHandler;
import lombok.Getter;

@Getter
public class SQLiteHandler extends AbstractSqlHandler {
    public static final String DEFAULT_DRIVER = "org.sqlite.JDBC";

    @Getter
    private static SQLiteHandler instance;
    private final SQLiteCredentials credentials;

    public SQLiteHandler(SQLiteCredentials credentials, String table) {
        super(table);
        this.credentials = credentials;
        instance = this;
    }

    @Override
    protected HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(credentials.toUrl());
        config.setDriverClassName(credentials.getDriver());

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(2000);
        config.setConnectionTestQuery("SELECT 1;");
        config.setInitializationFailTimeout(0);
        return new HikariDataSource(config);
    }
}
