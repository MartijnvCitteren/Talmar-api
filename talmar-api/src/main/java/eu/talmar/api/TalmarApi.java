package eu.talmar.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TalmarApi {

  static void main(String[] args) {
    SpringApplication.run(TalmarApi.class, args);
  }
}
