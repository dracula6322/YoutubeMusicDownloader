package com.green.square.telegram;

import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;

@Component
public class TelegramYoutubeBotController {

  private final Logger logger = LoggerFactory.getLogger(TelegramYoutubeBotController.class);

  @Autowired
  public TelegramYoutubeBotController(ProgramArgumentsController programArgumentsController,
      YoutubeDownloaderAndCutter youtubeDownloaderAndCutter) {

    ApiContextInitializer.init();

    TelegramBotsApi botsApi = new TelegramBotsApi();

    try {
      LongPollingBot telegramMessageHandler = new YoutubeTelegramBotMessageHandler(programArgumentsController,
          youtubeDownloaderAndCutter);
      botsApi.registerBot(telegramMessageHandler);
    } catch (TelegramApiException e) {
      logger.error(e.getMessage(), e);
      e.printStackTrace();
    }
  }
}
