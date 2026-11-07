package com.shringaar.store.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Small one-off fixes for databases created by earlier versions of the site,
 * which Hibernate's automatic schema update can't make by itself.
 * Each step is safe to run on every start.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaUpgrades implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgrades.class);

    private final DataSource dataSource;

    public SchemaUpgrades(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            DatabaseMetaData meta = c.getMetaData();
            String db = meta.getDatabaseProductName().toLowerCase();

            // v0.1 stored the appointment type as a database enum / check constraint
            // allowing only STUDIO and HOME_VISIT. Widen it to plain text so
            // VIDEO_CALL and PHONE_CALL can be saved.
            if (db.contains("h2") && isEnumColumn(meta, "APPOINTMENT", "LOCATION")) {
                st.execute("ALTER TABLE APPOINTMENT ALTER COLUMN LOCATION VARCHAR(20)");
                log.info("Upgraded APPOINTMENT.LOCATION to VARCHAR(20)");
            } else if (db.contains("postgres")) {
                st.execute("ALTER TABLE IF EXISTS appointment DROP CONSTRAINT IF EXISTS appointment_location_check");
            }
        }
    }

    private static boolean isEnumColumn(DatabaseMetaData meta, String table, String column) throws Exception {
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            // H2 reports the full definition, e.g. ENUM('HOME_VISIT', 'STUDIO')
            return rs.next() && rs.getString("TYPE_NAME").toUpperCase().startsWith("ENUM");
        }
    }
}
