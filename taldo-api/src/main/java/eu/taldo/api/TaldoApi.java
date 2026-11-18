package eu.taldo.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TaldoApi {

  static void main(String[] args) {
    SpringApplication.run(TaldoApi.class, args);
  }
}
