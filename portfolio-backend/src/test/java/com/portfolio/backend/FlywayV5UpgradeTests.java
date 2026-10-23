package com.portfolio.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

class FlywayV5UpgradeTests {
 @Test void existingV4DataSurvivesV5AndRepeatedStartupDoesNotDuplicateBaseline(){
  String url=System.getProperty("v5.test.url","jdbc:h2:mem:v5upgrade;MODE=MySQL;DB_CLOSE_DELAY=-1");
  if(url.startsWith("jdbc:mysql:"))assertThat(url).matches("jdbc:mysql://[^/]+/portfolio_v5_test(?:\\?.*)?");
  else assertThat(url).startsWith("jdbc:h2:mem:");
  var data=new DriverManagerDataSource(url,System.getProperty("v5.test.username","sa"),System.getProperty("v5.test.password",""));
  Flyway.configure().dataSource(data).target("4").cleanDisabled(true).load().migrate();
  var jdbc=new JdbcTemplate(data);
  jdbc.update("INSERT INTO analytics_event(scope,occurred_at,visit_date) VALUES ('visit',CURRENT_TIMESTAMP,CURRENT_DATE),('visit',CURRENT_TIMESTAMP,CURRENT_DATE),('click:project-one',CURRENT_TIMESTAMP,CURRENT_DATE)");
  var original=jdbc.queryForList("SELECT id,scope,occurred_at,visit_date FROM analytics_event ORDER BY id");
  var flyway=Flyway.configure().dataSource(data).cleanDisabled(true).load();
  assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);flyway.validate();
  assertThat(jdbc.queryForList("SELECT id,scope,occurred_at,visit_date FROM analytics_event WHERE scope <> 'visit-session' ORDER BY id")).isEqualTo(original);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM analytics_event WHERE scope='visit-session'",Long.class)).isEqualTo(2);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM analytics_event WHERE scope='visit'",Long.class)).isEqualTo(2);
  assertThat(flyway.migrate().migrationsExecuted).isZero();
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM analytics_event",Long.class)).isEqualTo(5);
 }
}
