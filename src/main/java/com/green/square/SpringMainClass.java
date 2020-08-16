package com.green.square;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class SpringMainClass {

  public static void main(String[] args) {

    ApiContextInitializer.init();
    SpringApplication.run(SpringMainClass.class, args);
  }
}