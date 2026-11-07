package com.shringaar.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.shringaar.store.config.SchemaUpgrades;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class SchemaUpgradesTest {

    @Test
    void widensTheOldTwoValueAppointmentTypeColumn() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:upgrade;DB_CLOSE_DELAY=-1");
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // the column as v0.1 created it
            st.execute("CREATE TABLE APPOINTMENT (ID BIGINT PRIMARY KEY, LOCATION ENUM('HOME_VISIT','STUDIO'))");
            st.execute("INSERT INTO APPOINTMENT VALUES (1, 'STUDIO')");

            new SchemaUpgrades(ds).run(new DefaultApplicationArguments());
            new SchemaUpgrades(ds).run(new DefaultApplicationArguments()); // safe to repeat

            st.execute("INSERT INTO APPOINTMENT VALUES (2, 'VIDEO_CALL')");
            try (ResultSet rs = st.executeQuery("SELECT LOCATION FROM APPOINTMENT ORDER BY ID")) {
                rs.next(); assertThat(rs.getString(1)).isEqualTo("STUDIO");
                rs.next(); assertThat(rs.getString(1)).isEqualTo("VIDEO_CALL");
            }
        }
    }
}
