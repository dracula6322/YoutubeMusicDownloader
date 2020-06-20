package com.green.square;

import com.green.square.telegram.TelegramMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

@SpringBootApplication
public class MainClassTelegramBot {

  Logger logger = LoggerFactory.getLogger(MainClassTelegramBot.class);

  static {
    ApiContextInitializer.init();
  }

  @Autowired
  public MainClassTelegramBot(TelegramMessageHandler telegramMessageHandler) {

    TelegramBotsApi botsApi = new TelegramBotsApi();

    try {
      BotSession botSession = botsApi.registerBot(telegramMessageHandler);
    } catch (TelegramApiException e) {
      logger.error(e.getMessage(), e);
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    ConfigurableApplicationContext applicationContext = SpringApplication.run(MainClassTelegramBot.class, args);
    MainClassTelegramBot bean = applicationContext.getBean(MainClassTelegramBot.class);
  }


}
