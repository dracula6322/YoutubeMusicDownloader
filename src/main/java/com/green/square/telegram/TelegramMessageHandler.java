package com.green.square.telegram;

import com.green.square.FileManagerController;
import com.green.square.ZipController;
import com.green.square.model.CommandArgumentsResult;
import com.green.square.model.DownloadState;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramMessageHandler extends TelegramLongPollingBot {

  private long globalLongIncrement = 0;
  private Map<Long, Map<Long, Path>> mapTheRelationshipBetweenUserAndPath = new HashMap<>();

  private FileManagerController fileManagerController;
  private ZipController zipController;

  @Autowired
  public YoutubeDownloaderAndCutter youtubeDownloaderAndCutter;
  public CommandArgumentsResult arguments;
  public Logger logger = LoggerFactory.getLogger(getClass().getName());

  @Autowired
  public TelegramMessageHandler(ProgramArgumentsController programArgumentsController,
      YoutubeDownloaderAndCutter youtubeDownloaderAndCutter, ZipController zipController) {
    this.youtubeDownloaderAndCutter = youtubeDownloaderAndCutter;
    this.arguments = programArgumentsController.getArguments();
    this.zipController = zipController;
  }

  @Override
  public void onUpdateReceived(Update update) {

    boolean result = checkThatTheMessageIsTheCallbackQueryAnswerAndSendFile(update);
    if (result) {
      return;
    }
    logger.info("onUpdateReceived update = " + update);
    result = checkIsYoutubeLinkAndSendMessage(update);
    logger.info("checkIsYoutubeLinkAndSendMessage result = " + result);
    if (result) {
      return;
    }

  }

  private boolean checkThatTheMessageIsTheCallbackQueryAnswerAndSendFile(Update update) {

    CallbackQuery callbackQuery = update.getCallbackQuery();
    if (callbackQuery != null) {
      String data = callbackQuery.getData();
      Long userId = Long.valueOf(update.getCallbackQuery().getFrom().getId());
      Map<Long, Path> userRelationship = mapTheRelationshipBetweenUserAndPath.get(userId);
      Path result = userRelationship.get(Long.parseLong(data));
      logger.info("relationship: userId = " + userId + " " + "data = " + data + " " + "result = " + result);
      sendAudioFile(result, update.getCallbackQuery().getMessage().getChatId());
      return true;
    }
    return false;
  }

  private void sendAudioFile(Path globalResult, Long chatId) {

    SendAudio sendAudio = new SendAudio();
    sendAudio.setChatId(chatId);
    sendAudio.setTitle(globalResult.getFileName().toString());
    sendAudio.setAudio(globalResult.toFile());
    try {
      execute(sendAudio);
    } catch (TelegramApiException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
    }

  }

  private boolean checkIsYoutubeLinkAndSendMessage(Update update) {

    Message message = update.getMessage();
    if (message == null || Strings.isEmpty(message.getText())) {
      return false;
    }

    String youtubeLink = message.getText();

    youtubeDownloaderAndCutter
        .getPairsAndCutTheFileIntoPieces(arguments.getPathToYoutubedl(), youtubeLink, logger, arguments.getFfmpegPath(),
            arguments.getOutputFolderPath())
        .subscribe(new SingleObserver<>() {
          @Override
          public void onSubscribe(@NonNull Disposable d) {
          }

          @Override
          public void onSuccess(@NonNull DownloadState downloadState) {
            logger.info(String.valueOf(downloadState));

            makeInlineKeyboardMarkupWithTrimmedFiles(downloadState, update);

          }

          @Override
          public void onError(@NonNull Throwable e) {
            logger.error(e.getMessage(), e);
          }
        });

    return true;
  }

  private void makeInlineKeyboardMarkupWithTrimmedFiles(DownloadState state, Update update) {

    Long userId = update.getMessage().getChatId();

    Map<Long, Path> integerPathMap = new HashMap<>();
    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
    for (Path trimmedFile : state.getTrimmedFiles()) {

      InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
      inlineKeyboardButton.setText(String.valueOf(trimmedFile.getFileName()));

      integerPathMap.put(globalLongIncrement, trimmedFile);
      inlineKeyboardButton.setCallbackData(String.valueOf(globalLongIncrement));
      globalLongIncrement++;

      List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
      keyboardButtonsRow1.add(inlineKeyboardButton);
      rowList.add(keyboardButtonsRow1);
    }

    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    inlineKeyboardMarkup.setKeyboard(rowList);

    SendMessage request = new SendMessage();
    request.setText("Привер");
    request.setChatId(update.getMessage().getChatId());
    request.setReplyMarkup(inlineKeyboardMarkup);

    mapTheRelationshipBetweenUserAndPath.put(userId, integerPathMap);

    try {
      Message executeResult = execute(request);
      logger.info("executeResult = " + executeResult);
    } catch (TelegramApiException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
    }

  }

  @Override
  public String getBotUsername() {
    return "Hello ";
  }

  @Override
  public String getBotToken() {
    return "";
  }
}
