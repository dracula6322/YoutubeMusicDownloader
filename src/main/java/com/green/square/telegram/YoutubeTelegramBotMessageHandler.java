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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class YoutubeTelegramBotMessageHandler extends TelegramLongPollingBot {

  private long globalLongIncrement = 0;
  private Map<Long, Map<Long, CutValue>> mapTheRelationshipBetweenUserAndPathNewVersion = new HashMap<>();
  private Map<Long, DownloadState> mapTheRelationshipBetweenUserAndPathNewVersionWithState = new HashMap<>();
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

    logger.info("onUpdateReceived thread = " + Thread.currentThread().toString());
    logger.info("onUpdateReceived update = " + update);

    if (checkUpdateIsBotCommand(update)) {
      sendFileFromBotCommandMessage(update);
      return;
    }

    if (checkIsYoutubeLinkAndSendMessageWithPairs(update)) {
      return;
    }

  }

  private boolean checkUpdateIsBotCommand(Update update) {

    Message message = update.getMessage();
    if (message == null) {
      return false;
    }
    return message.isCommand();

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
    if (userRelationship == null) {
      logger.info("Information about video not found in memory");
      sendMessageWithText("Enter the video link again", String.valueOf(userId));
      return false;
    }

    CutValue result = userRelationship.get(dataLong);
    DownloadState state = mapTheRelationshipBetweenUserAndPathNewVersionWithState.get(userId);

    logger.info("relationship: userId = " + userId + " " + "data = " + dataLong + " " + "result = " + result);

    List<CutValue> selectedItems = Collections.singletonList(result);
    List<File> trimmedFiles = youtubeDownloaderAndCutter
        .downloadAndCutFileByCutValues(logger, selectedItems, arguments.getPathToYoutubedl(),
            state.getAudioFileNameFromJson(), state.getVideoId(), arguments.getOutputFolderPath(),
            arguments.getFfmpegPath(), state.getVideoTitle(), state.getVideoLink());

    trimmedFiles.remove(0);

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
                sendAudioFileMessage(chatId, trimmedFile, trimmedFile.getName());
              });
    } else {
      sendAudioFileMessage(chatId, file, globalResult.getFileName().toString());
    }


  }

  private void sendAudioFileMessage(Long chatId, File trimmedFile, String name) {
    SendAudio sendAudio = new SendAudio();
    sendAudio.setChatId(chatId);
    sendAudio.setTitle(name);
    sendAudio.setAudio(trimmedFile);
    try {
      execute(sendAudio);
    } catch (TelegramApiException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
    }
  }

  private boolean checkIsYoutubeLinkAndSendMessageWithPairs(Update update) {

    Message message = update.getMessage();
    if (message == null || Strings.isEmpty(message.getText())) {
      return false;
    }

    String youtubeLink = message.getText();
    downloadPairs(youtubeLink, update);

    return true;
  }

  private void downloadPairs(String youtubeLink, Update update) {
    youtubeDownloaderAndCutter.getPairs(arguments.getPathToYoutubedl(), youtubeLink, logger)
        .subscribe(new SingleObserver<>() {
          @Override
          public void onSubscribe(@NonNull Disposable d) {
          }

          @Override
          public void onSuccess(@NonNull DownloadState downloadState) {
            logger.info(String.valueOf(downloadState));
            String messageText = writePairsInMemory(downloadState, update);
            sendMessageWithText(messageText, String.valueOf(update.getMessage().getChatId()));
          }

          @Override
          public void onError(@NonNull Throwable e) {
            logger.error(e.getMessage(), e);
          }
        });
  }

  private String writePairsInMemory(DownloadState state, Update update) {

    Map<Long, CutValue> integerPathMapNewVersion = new HashMap<>();
    StringBuilder messageText = new StringBuilder(state.getVideoTitle()).append(Strings.LINE_SEPARATOR);

    for (CutValue cutValue : state.getCutValues()) {
      globalLongIncrement++;
      integerPathMapNewVersion.put(globalLongIncrement, cutValue);

      messageText.append("/").append(globalLongIncrement).append(" ").append(cutValue.getTitle())
          .append(Strings.LINE_SEPARATOR);
    }
    Long userId = update.getMessage().getChatId();
    mapTheRelationshipBetweenUserAndPathNewVersionWithState.put(userId, state);
    mapTheRelationshipBetweenUserAndPathNewVersion.put(userId, integerPathMapNewVersion);
    return messageText.toString();
  }


  private void sendMessageWithText(String messageText, String chatId) {
    SendMessage request = new SendMessage();
    request.setText(messageText);
    request.setChatId(chatId);

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
