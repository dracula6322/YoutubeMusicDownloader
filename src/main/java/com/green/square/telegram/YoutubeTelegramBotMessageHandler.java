package com.green.square.telegram;

import com.green.square.FfmpegController.ParsingResult;
import com.green.square.FfmpegParsingState;
import com.green.square.model.CommandArgumentsResult;
import com.green.square.model.DownloadState;
import com.green.square.model.VideoInterval;
import com.green.square.youtubedownloader.ProgramArgumentsController;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter;
import com.green.square.youtubedownloader.YoutubeDownloaderAndCutter.ResultPublisher;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.http.util.TextUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class YoutubeTelegramBotMessageHandler extends TelegramLongPollingBot {

  private final Logger logger = LoggerFactory.getLogger(YoutubeTelegramBotMessageHandler.class);
  public YoutubeDownloaderAndCutter youtubeDownloaderAndCutter;
  public CommandArgumentsResult arguments;
  private final DecimalFormat df = new DecimalFormat("#.00");

  private long globalLongIncrement = 0;

  private final Map<String, YoutubeTelegramBotState> mapYoutubeTelegramBot = new HashMap<>();
  private final Map<String, TelegramUserSessionState> youtubeTelegramBotCurrentState = new HashMap<>();


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

    String userId = update.getMessage().getFrom().getId().toString();
    YoutubeTelegramBotState youtubeTelegramBotState = mapYoutubeTelegramBot.get(userId);

    TelegramUserSessionState userState = youtubeTelegramBotCurrentState
        .getOrDefault(userId, new TelegramUserSessionState());

    if (checkUpdateIsBotCommand(update)) {
      sendFileFromBotCommandMessageWithState(update, youtubeTelegramBotState, userState);
      return;
    }

    if (checkIsYoutubeLinkAndSendMessageWithPairs(update, userState)) {
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

  private boolean sendFileFromBotCommandMessage(Update update, YoutubeTelegramBotState youtubeTelegramBotState) {

    String text = update.getMessage().getText();
    String message = text.substring(1);

    if (TextUtils.isEmpty(message)) {
      return false;
    }
    long dataLong;
    try {
      dataLong = Long.parseLong(message);
    } catch (NumberFormatException e) {
      logger.error(e.getMessage(), e);
      sendMessageWithText("Не правильный формат ответа", youtubeTelegramBotState.userId);
      return false;
    }

    Map<Long, VideoInterval> newVersionUserRelationship = youtubeTelegramBotState.mapTheRelationShipBetweenLinkIdAndCutValue;
    if (newVersionUserRelationship == null) {
      logger.info("Information about video not found in memory");
      sendMessageWithText("Enter the video link again", youtubeTelegramBotState.userId);
      return false;
    }
    long userId = youtubeTelegramBotState.userId;
    VideoInterval result = newVersionUserRelationship.get(dataLong);

    logger.info("relationship: userId = " + userId + " " + "data = " + dataLong + " " + "result = " + result);

    FfmpegParsingState ffmpegParsingState = new FfmpegParsingState();
    Message messageFromRequest = update.getMessage();

    DownloadState state = youtubeTelegramBotState.currentDownloadState;
    SendMessage replyMessage = new SendMessage()
        .setChatId(messageFromRequest.getChatId())
        .setReplyToMessageId(messageFromRequest.getMessageId())
        .setText(messageFromRequest.getText() + " " + result.getTitle());

    Message sendsReplyMessage = null;
    try {
      sendsReplyMessage = execute(replyMessage);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }

    List<VideoInterval> selectedItems = Collections.singletonList(result);
    Message finalSendsReplyMessage = sendsReplyMessage;
    List<File> trimmedFiles = youtubeDownloaderAndCutter
        .downloadAndCutFileByCutValues(logger, selectedItems, arguments.getPathToYoutubedl(),
            state.getAudioFileNameFromJson(), state.getVideoId(), arguments.getOutputFolderPath(),
            arguments.getFfmpegPath(), state.getVideoTitle(), state.getVideoLink(), new ResultPublisher<String>() {
              @Override
              public void publishResult(String value) {

                System.out.println("value = " + value);

                ParsingResult result1 = ffmpegParsingState.processString(value);

                Message messageFromUpdate;
                if (finalSendsReplyMessage != null) {
                  messageFromUpdate = finalSendsReplyMessage;
                } else {
                  messageFromUpdate = finalSendsReplyMessage;
                }

                if (result1.getCurrentDurationInSeconds() == 0 || result1.getDurationInSeconds() == 0) {
                  logger.info("Not done yet");
                  return;
                }

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
                inlineKeyboardButton1.setText("Progress: " + df.format(result1.getPercent() * 100));
                inlineKeyboardButton1.setCallbackData("Hi");
                List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
                keyboardButtonsRow1.add(inlineKeyboardButton1);
                List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
                rowList.add(keyboardButtonsRow1);
                inlineKeyboardMarkup.setKeyboard(rowList);

                EditMessageText message = new EditMessageText()
                    .setText(messageFromUpdate.getText())
                    .setChatId(messageFromUpdate.getChatId())
                    .setMessageId(messageFromUpdate.getMessageId())
                    .setReplyMarkup(inlineKeyboardMarkup);

                executeMessage(message);
              }
            });

    trimmedFiles.remove(0);

    sendAudioFile(trimmedFiles.get(0).toPath(), update.getMessage().getChatId());

    ffmpegParsingState.clean();

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

  private boolean checkIsYoutubeLinkAndSendMessageWithPairs(Update update, TelegramUserSessionState userState) {

    Message message = update.getMessage();
    if (message == null || Strings.isEmpty(message.getText())) {
      return false;
    }

    String youtubeLink = message.getText();
    downloadPairs(youtubeLink, update, userState);

    return true;
  }

  private void downloadPairs(String youtubeLink, Update update, TelegramUserSessionState state) {
    youtubeDownloaderAndCutter.getPairs(arguments.getPathToYoutubedl(), youtubeLink, logger)
        .subscribe(new SingleObserver<DownloadState>() {
          @Override
          public void onSubscribe(@NonNull Disposable d) {
          }

          @Override
          public void onSuccess(@NonNull DownloadState downloadState) {
            logger.info(String.valueOf(downloadState));
            List<String> messageText = getMessageWithTextFromPairs(downloadState, update, state);
            for (String s : messageText) {
              sendMessageWithText(s, update.getMessage().getChatId());
            }
          }

          @Override
          public void onError(@NonNull Throwable e) {
            logger.error(e.getMessage(), e);
          }
        });
  }

  private String writePairsInMemory(DownloadState state, Update update) {

    Map<Long, VideoInterval> integerPathMapNewVersion = new HashMap<>();
    StringBuilder messageText = new StringBuilder(state.getVideoTitle()).append(Strings.LINE_SEPARATOR);

    for (VideoInterval cutValue : state.getCutValues()) {
      globalLongIncrement++;
      integerPathMapNewVersion.put(globalLongIncrement, cutValue);

      messageText.append("/").append(globalLongIncrement).append(" ").append(cutValue.getStartTime()).append(" ")
          .append(cutValue.getEndTime()).append(" / ").append(cutValue.getTitle())
          .append(Strings.LINE_SEPARATOR);
    }
    Long userId = update.getMessage().getChatId();
    YoutubeTelegramBotState youtubeTelegramBotState = new YoutubeTelegramBotState(userId, globalLongIncrement,
        integerPathMapNewVersion, state);

    mapYoutubeTelegramBot.put(String.valueOf(userId), youtubeTelegramBotState);
    return messageText.toString();
  }

  final int messageSize = 4000;

  private List<String> getMessageWithTextFromPairs(DownloadState state, Update update,
      TelegramUserSessionState telegramUserSessionState) {

    List<String> result = new ArrayList<>();
    StringBuilder stringBuilder = new StringBuilder();
    Map<Long, VideoInterval> integerPathMapNewVersion = new HashMap<>();

    for (VideoInterval cutValue : state.getCutValues()) {
      globalLongIncrement++;
      integerPathMapNewVersion.put(globalLongIncrement, cutValue);

      StringBuilder messageText = new StringBuilder(state.getVideoTitle()).append(Strings.LINE_SEPARATOR);
      messageText.append("/").append(globalLongIncrement).append(" ").append(cutValue.getStartTime()).append(" ")
          .append(cutValue.getEndTime()).append(" / ").append(cutValue.getTitle())
          .append(Strings.LINE_SEPARATOR);

      if (stringBuilder.length() + messageText.length() > messageSize) {
        result.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
      } else {
        stringBuilder.append(messageText);
      }
    }
    telegramUserSessionState.setIntervals(integerPathMapNewVersion);
    if (stringBuilder.length() > 0) {
      result.add(stringBuilder.toString());
    }

    Long userId = update.getMessage().getChatId();
    YoutubeTelegramBotState youtubeTelegramBotState = new YoutubeTelegramBotState(userId, globalLongIncrement,
        integerPathMapNewVersion, state);

    mapYoutubeTelegramBot.put(String.valueOf(userId), youtubeTelegramBotState);
    return result;
  }

  private void sendMessageWithText(String messageText, long chatId) {
    SendMessage request = new SendMessage();
    request.setText(messageText);
    request.setChatId(chatId);

    executeMessage(request);
  }

  private boolean executeMessage(SendMessage method) {
    try {
      execute(method);
      return true;
    } catch (TelegramApiException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
      return false;
    }
  }

  private void sendAudioFileMessage(Long chatId, File trimmedFile, String name) {
    SendAudio sendAudio = new SendAudio();
    sendAudio.setChatId(chatId);
    sendAudio.setTitle(name);
    sendAudio.setAudio(trimmedFile);

    executeMessage(sendAudio);
  }

  private boolean executeMessage(SendAudio method) {
    try {
      execute(method);
      return true;
    } catch (TelegramApiException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
      return false;
    }
  }

  private boolean executeMessage(EditMessageText method) {
    try {
      execute(method);
      return true;
    } catch (TelegramApiException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
      return false;
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

  @Data
  @AllArgsConstructor
  @Builder(toBuilder = true)
  static class YoutubeTelegramBotState {

    private long userId;
    private long longIncrement = 0;
    private Map<Long, VideoInterval> mapTheRelationShipBetweenLinkIdAndCutValue;
    private DownloadState currentDownloadState;
  }


  private boolean sendFileFromBotCommandMessageWithState(Update update, YoutubeTelegramBotState youtubeTelegramBotState,
      TelegramUserSessionState userState) {

    String text = update.getMessage().getText();
    String message = text.substring(1);

    if (TextUtils.isEmpty(message)) {
      return false;
    }
    long dataLong;
    try {
      dataLong = Long.parseLong(message);
    } catch (NumberFormatException e) {
      logger.error(e.getMessage(), e);
      sendMessageWithText("Не правильный формат ответа", youtubeTelegramBotState.userId);
      return false;
    }

    Map<Long, VideoInterval> newVersionUserRelationship = youtubeTelegramBotState.mapTheRelationShipBetweenLinkIdAndCutValue;
    if (newVersionUserRelationship == null) {
      logger.info("Information about video not found in memory");
      sendMessageWithText("Enter the video link again", youtubeTelegramBotState.userId);
      return false;
    }
    long userId = youtubeTelegramBotState.userId;
    VideoInterval result = newVersionUserRelationship.get(dataLong);

    logger.info("relationship: userId = " + userId + " " + "data = " + dataLong + " " + "result = " + result);

    FfmpegParsingState ffmpegParsingState = new FfmpegParsingState();
    Message messageFromRequest = update.getMessage();

    DownloadState state = youtubeTelegramBotState.currentDownloadState;
    SendMessage replyMessage = new SendMessage()
        .setChatId(messageFromRequest.getChatId())
        .setReplyToMessageId(messageFromRequest.getMessageId())
        .setText(messageFromRequest.getText() + " " + result.getTitle());

    Message sendsReplyMessage = null;
    try {
      sendsReplyMessage = execute(replyMessage);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }

    List<VideoInterval> selectedItems = Collections.singletonList(result);
    Message finalSendsReplyMessage = sendsReplyMessage;
    List<File> trimmedFiles = youtubeDownloaderAndCutter
        .downloadAndCutFileByCutValues(logger, selectedItems, arguments.getPathToYoutubedl(),
            state.getAudioFileNameFromJson(), state.getVideoId(), arguments.getOutputFolderPath(),
            arguments.getFfmpegPath(), state.getVideoTitle(), state.getVideoLink(), new ResultPublisher<String>() {
              @Override
              public void publishResult(String value) {

                System.out.println("value = " + value);

                ParsingResult result1 = ffmpegParsingState.processString(value);

                Message messageFromUpdate;
                if (finalSendsReplyMessage != null) {
                  messageFromUpdate = finalSendsReplyMessage;
                } else {
                  messageFromUpdate = finalSendsReplyMessage;
                }

                if (result1.getCurrentDurationInSeconds() == 0 || result1.getDurationInSeconds() == 0) {
                  logger.info("Not done yet");
                  return;
                }

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                InlineKeyboardButton inlineKeyboardButton1 = new InlineKeyboardButton();
                inlineKeyboardButton1.setText("Progress: " + df.format(result1.getPercent() * 100));
                inlineKeyboardButton1.setCallbackData("Hi");
                List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
                keyboardButtonsRow1.add(inlineKeyboardButton1);
                List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
                rowList.add(keyboardButtonsRow1);
                inlineKeyboardMarkup.setKeyboard(rowList);

                EditMessageText message = new EditMessageText()
                    .setText(messageFromUpdate.getText())
                    .setChatId(messageFromUpdate.getChatId())
                    .setMessageId(messageFromUpdate.getMessageId())
                    .setReplyMarkup(inlineKeyboardMarkup);

                executeMessage(message);
              }
            });

    trimmedFiles.remove(0);

    sendAudioFile(trimmedFiles.get(0).toPath(), update.getMessage().getChatId());

    ffmpegParsingState.clean();

    return false;
  }
}
