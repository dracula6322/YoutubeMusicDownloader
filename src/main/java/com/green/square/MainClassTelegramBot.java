package com.green.square;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class MainClassTelegramBot {

  public static void main(String[] args) {

    ApiContextInitializer.init();

    SpringApplication.run(MainClassTelegramBot.class, args);

  }
}
