package org.openbank.config;

import org.openbank.db.ConnectionPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseConfig {

  private final Environment environment;

  public DatabaseConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean(destroyMethod = "closeAllConnections")
  public ConnectionPool connectionPool() {
    return new ConnectionPool(
        environment.getRequiredProperty("db.url"),
        environment.getRequiredProperty("db.username"),
        environment.getRequiredProperty("db.password"),
        environment.getProperty("db.pool.size", Integer.class, 10)
    );
  }
}
