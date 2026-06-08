package org.author.demo.config;

import org.author.demo.db.ConnectionPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {

  @Bean(destroyMethod = "closeAllConnections")
  public ConnectionPool connectionPool() {
    return new ConnectionPool(
        "jdbc:postgresql://localhost:5432/bank?charSet=UTF-8",
        "postgres",
        "postgres",
        10
    );
  }
}