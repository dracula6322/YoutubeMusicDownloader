package com.green.square.telegram;

import com.green.square.model.CommandArgumentsResult;
import com.green.square.model.CutValue;
import com.green.square.model.DownloadState;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.util.TextUtils;
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
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class YoutubeTelegramBotMessageHandler extends TelegramLongPollingBot {

  private long globalLongIncrement = 0;
  private Map<Long, Map<Long, CutValue>> mapTheRelationshipBetweenUserAndPathNewVersion = new HashMap<>();
  Map<Long, DownloadState> mapTheRelationshipBetweenUserAndPathNewVersionWithState = new HashMap<>();

  public YoutubeDownloaderAndCutter youtubeDownloaderAndCutter;
  public CommandArgumentsResult arguments;

  private final Logger logger = LoggerFactory.getLogger(YoutubeTelegramBotMessageHandler.class);

  @Autowired
  public YoutubeTelegramBotMessageHandler(ProgramArgumentsController programArgumentsController,
      YoutubeDownloaderAndCutter youtubeDownloaderAndCutter) {
    this.youtubeDownloaderAndCutter = youtubeDownloaderAndCutter;
    this.arguments = programArgumentsController.getArguments();
  }

  @Override
  public void onUpdateReceived(Update update) {

    boolean result = checkUpdateIsBotCommand(update);
    if (result) {
      sendFileFromBotCommandMessage(update);
      return;
    }

    result = checkThatTheMessageIsTheCallbackQueryAnswerAndSendFileNewVersion(update);
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

  private boolean checkUpdateIsBotCommand(Update update) {

    Message message = update.getMessage();
    if (message == null) {
      return false;
    }
    List<MessageEntity> entities = message.getEntities();
    if (entities == null || entities.size() == 0) {
      return false;
    }
    String type = entities.get(0).getType();
    if (TextUtils.isEmpty(type)) {
      return false;
    }
    return type.equals("bot_command");

  }

  private boolean checkThatTheMessageIsTheCallbackQueryAnswerAndSendFileNewVersion(Update update) {

    CallbackQuery callbackQuery = update.getCallbackQuery();
    if (callbackQuery != null) {
      String data = callbackQuery.getData();
      long dataLong = Long.parseLong(data);
      Long userId = Long.valueOf(update.getCallbackQuery().getFrom().getId());
      Map<Long, CutValue> userRelationship = mapTheRelationshipBetweenUserAndPathNewVersion.get(userId);
      CutValue result = userRelationship.get(dataLong);
      DownloadState state = mapTheRelationshipBetweenUserAndPathNewVersionWithState.get(userId);

      logger.info("relationship: userId = " + userId + " " + "data = " + data + " " + "result = " + result);

      List<CutValue> selectedItems = Collections.singletonList(result);
      List<File> trimmedFiles = youtubeDownloaderAndCutter
          .downloadAndCutFileByCutValues(logger, selectedItems, arguments.getPathToYoutubedl(),
              state.getAudioFileNameFromJson(), state.getVideoId(), arguments.getOutputFolderPath(),
              arguments.getFfmpegPath(), result.getTitle(), state.getVideoLink());

      File downloadedFile = trimmedFiles.remove(0);

      sendAudioFile(trimmedFiles.get(0).toPath(), update.getCallbackQuery().getMessage().getChatId());
      return true;
    }
    return false;
  }

  private boolean sendFileFromBotCommandMessage(Update update) {

    String text = update.getMessage().getText();
    String message = text.substring(1);

    if (TextUtils.isEmpty(message)) {
      return false;
    }

    long dataLong = Long.parseLong(message);
    Long userId = Long.valueOf(update.getMessage().getFrom().getId());
    Map<Long, CutValue> userRelationship = mapTheRelationshipBetweenUserAndPathNewVersion.get(userId);
    CutValue result = userRelationship.get(dataLong);
    DownloadState state = mapTheRelationshipBetweenUserAndPathNewVersionWithState.get(userId);

    logger.info("relationship: userId = " + userId + " " + "data = " + dataLong + " " + "result = " + result);

    List<CutValue> selectedItems = Collections.singletonList(result);
    List<File> trimmedFiles = youtubeDownloaderAndCutter
        .downloadAndCutFileByCutValues(logger, selectedItems, arguments.getPathToYoutubedl(),
            state.getAudioFileNameFromJson(), state.getVideoId(), arguments.getOutputFolderPath(),
            arguments.getFfmpegPath(), result.getTitle(), state.getVideoLink());

    File downloadedFile = trimmedFiles.remove(0);

    sendAudioFile(trimmedFiles.get(0).toPath(), update.getMessage().getChatId());

    return false;
  }

  private void sendAudioFile(Path globalResult, Long chatId) {

    File file = globalResult.toFile();
    long maxFileSize = 45 * 1000 * 1000;

    if (file.length() > maxFileSize) {

      youtubeDownloaderAndCutter
          .trimFileBySize(file, arguments.getFfmpegPath(), logger, file.getParent() + File.separatorChar, maxFileSize,
              value -> {
                File trimmedFile = value.getChoppedFile();
                SendAudio sendAudio = new SendAudio();
                sendAudio.setChatId(chatId);
                sendAudio.setTitle(trimmedFile.getName());
                sendAudio.setAudio(trimmedFile);
                try {
                  execute(sendAudio);
                } catch (TelegramApiException e) {
                  e.printStackTrace();
                  logger.error(e.getMessage(), e);
                }
              });
    } else {
      SendAudio sendAudio = new SendAudio();
      sendAudio.setChatId(chatId);
      sendAudio.setTitle(globalResult.getFileName().toString());
      sendAudio.setAudio(file);
      try {
        execute(sendAudio);
      } catch (TelegramApiException e) {
        e.printStackTrace();
        logger.error(e.getMessage(), e);
      }
    }


  }

  private boolean checkIsYoutubeLinkAndSendMessage(Update update) {

    Message message = update.getMessage();
    if (message == null || Strings.isEmpty(message.getText())) {
      return false;
    }

    String youtubeLink = message.getText();

    youtubeDownloaderAndCutter.getPairs(arguments.getPathToYoutubedl(), youtubeLink, logger)
        .subscribe(new SingleObserver<>() {
          @Override
          public void onSubscribe(@NonNull Disposable d) {
          }

          @Override
          public void onSuccess(@NonNull DownloadState downloadState) {
            logger.info(String.valueOf(downloadState));
            makeInlineKeyboardMarkupWithTrimmedFilesInMessage(downloadState, update);
          }

          @Override
          public void onError(@NonNull Throwable e) {
            logger.error(e.getMessage(), e);
          }
        });

    return true;
  }

  private void makeInlineKeyboardMarkupWithTrimmedFiles(DownloadState state, Update update) {

    Map<Long, CutValue> integerPathMapNewVersion = new HashMap<>();
    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

    for (int i = 0; i < state.getCutValues().size(); i++) {
      CutValue cutValue = state.getCutValues().get(i);

      InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
      inlineKeyboardButton.setText(String.valueOf(cutValue.getTitle()));

      integerPathMapNewVersion.put(globalLongIncrement, cutValue);
      inlineKeyboardButton.setCallbackData(String.valueOf(globalLongIncrement));
      globalLongIncrement++;

      List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
      keyboardButtonsRow1.add(inlineKeyboardButton);
      rowList.add(keyboardButtonsRow1);
    }

    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    inlineKeyboardMarkup.setKeyboard(rowList);

    SendMessage request = new SendMessage();
    request.setText(state.getVideoTitle());
    request.setChatId(update.getMessage().getChatId());
    request.setReplyMarkup(inlineKeyboardMarkup);

    Long userId = update.getMessage().getChatId();

    mapTheRelationshipBetweenUserAndPathNewVersionWithState.put(userId, state);
    mapTheRelationshipBetweenUserAndPathNewVersion.put(userId, integerPathMapNewVersion);

    try {
      Message executeResult = execute(request);
      logger.info("executeResult = " + executeResult);
    } catch (TelegramApiException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
    }
  }

  private void makeInlineKeyboardMarkupWithTrimmedFilesInMessage(DownloadState state, Update update) {

    Map<Long, CutValue> integerPathMapNewVersion = new HashMap<>();
    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

    StringBuilder messageText = new StringBuilder(state.getVideoTitle()).append(Strings.LINE_SEPARATOR);

    for (int i = 0; i < state.getCutValues().size(); i++) {
      globalLongIncrement++;
      CutValue cutValue = state.getCutValues().get(i);

      InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
      inlineKeyboardButton.setText(String.valueOf(cutValue.getTitle()));

      integerPathMapNewVersion.put(globalLongIncrement, cutValue);
      inlineKeyboardButton.setCallbackData(String.valueOf(globalLongIncrement));

      List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
      keyboardButtonsRow1.add(inlineKeyboardButton);
      rowList.add(keyboardButtonsRow1);

      messageText.append("/").append(globalLongIncrement).append(" ").append(cutValue.getTitle())
          .append(Strings.LINE_SEPARATOR);
    }

    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    inlineKeyboardMarkup.setKeyboard(rowList);

    SendMessage request = new SendMessage();
    request.setText(messageText.toString());
    request.setChatId(update.getMessage().getChatId());

    Long userId = update.getMessage().getChatId();

    mapTheRelationshipBetweenUserAndPathNewVersionWithState.put(userId, state);
    mapTheRelationshipBetweenUserAndPathNewVersion.put(userId, integerPathMapNewVersion);

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
    return "YoutubeDownloaderBot";
  }

  @Override
  public String getBotToken() {
    return TelegramBotToken.token;
  }
}
