package com.green.square.youtubedownloader;

import com.green.square.DownloadStateRepository;
import com.green.square.ProgramExecutor;
import com.green.square.model.CutValue;
import com.green.square.model.DownloadState;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import java.io.File;
import java.lang.invoke.WrongMethodTypeException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.util.TextUtils;
import org.apache.logging.log4j.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class YoutubeDownloaderAndCutter {

  private static final int MAX_TITLE_LENGTH = 40;

  private DownloadStateRepository downloadStateRepository;
  public static final String DEFAULT_AUDIO_FILE_NAME = "full_original";

  private ProgramExecutor programExecutor;

  @Autowired
  public YoutubeDownloaderAndCutter(ProgramExecutor programExecutor, DownloadStateRepository downloadStateRepository) {
    this.downloadStateRepository = downloadStateRepository;
    this.programExecutor = programExecutor;
  }


  private ArrayList<CutValue> getDescFromYoutubeApi(String videoId, long durationInSeconds) {
    ArrayList<CutValue> result = new ArrayList<>();

    List<String> desc = YoutubeAPIController.getInstance().getComments(videoId);
    for (String s : desc) {

      ArrayList<CutValue> parsingDescriptionResult = parsingDescriptionInfoNewVersion(s, durationInSeconds);

      if (parsingDescriptionResult.size() > result.size()) {
        result = parsingDescriptionResult;
      }
    }

    return result;
  }

  private Single<DownloadState> getPairsWithSingle(Single<DownloadState> jsonWithTimeSingle, Logger logger) {

    return jsonWithTimeSingle.map(downloadState -> {
      List<CutValue> pairs = getPairs(downloadState.getVideoId(), downloadState.getJson(),
          downloadState.getDurationInSeconds(), logger);
      return downloadState.toBuilder().cutValues(pairs).build();
    });
  }

  private List<CutValue> getPairs(String videoId, String jsonData, long durationInSecond, Logger logger) {

    List<CutValue> result;

    String descriptionFromJson = getDescriptionFromJson(jsonData);
    ArrayList<CutValue> cutValuesFromDescription = parsingDescriptionInfoNewVersion(descriptionFromJson,
        durationInSecond);
    logger.info("cutValuesFromDescription.size() = " + cutValuesFromDescription.size());

    JSONArray chapters = getChaptersFromJson(jsonData);
    ArrayList<CutValue> cutValuesFromChapters = parsingChaptersInfo(chapters, durationInSecond);
    logger.info("cutValuesFromChapters.size() = " + cutValuesFromChapters.size());
    if (cutValuesFromDescription.size() > cutValuesFromChapters.size()) {
      result = cutValuesFromDescription;
    } else {
      result = cutValuesFromChapters;
    }

    ArrayList<CutValue> cutValuesFromComment = getDescFromYoutubeApi(videoId, durationInSecond);
    logger.info("cutValuesFromComment.size() = " + cutValuesFromComment.size());
    if (cutValuesFromComment.size() > result.size()) {
      result = cutValuesFromComment;
    }

    result = removeDuplicates(result);

    result.add(getFullLengthCutValue(durationInSecond));
    return result;
  }

  private CutValue getFullLengthCutValue(long durationInSeconds) {

    String deadline = getHouseMinuteSecondStringFromLong(durationInSeconds);
    return new CutValue(DEFAULT_AUDIO_FILE_NAME, "00:00:00", deadline, 0, durationInSeconds);
  }

  private List<CutValue> removeDuplicates(List<CutValue> cutValues) {

    List<CutValue> result = new ArrayList<>();
    Set<String> firstPart = new HashSet<>();
    Set<String> secondPart = new HashSet<>();

    for (CutValue cutValue : cutValues) {

      boolean isFirstPartGood;
      boolean isSecondPartGood;

      isFirstPartGood = firstPart.add(cutValue.getStartTime());
      isSecondPartGood = secondPart.add(cutValue.getEndTime());

      if (isFirstPartGood && isSecondPartGood) {
        result.add(cutValue);
      }
    }

    return result;
  }


  public DownloadFileResultValue downloadVideo(Logger logger, String pathToYoutubedl, String audioFileName,
      String videoId, String outputFolderPath, String fullLengthVideoName) {

    String pathToCreatedFolder = createFolder(outputFolderPath, videoId, audioFileName, logger);
    logger.info("createdFolder = " + pathToCreatedFolder);
    Path potentialVideoFilePath = Paths.get(pathToCreatedFolder + audioFileName);
    logger.info("potentialVideoFilePath = " + potentialVideoFilePath.toString());
    File downloadedAudioFile;
    boolean downloadedFileIsExists = Files.exists(potentialVideoFilePath);
    if (downloadedFileIsExists) {
      logger.debug("File exists and don't need to download it");
      downloadedAudioFile = potentialVideoFilePath.toFile();
    } else {
      logger.debug("File not exists");
      File createdFolder = deleteAndCreateFolder(pathToCreatedFolder, audioFileName, logger);
      String createdAbsolutePathFolder = createdFolder.getAbsolutePath() + File.separatorChar;
      downloadedAudioFile = downloadFileUsingYoutubedl(pathToYoutubedl, fullLengthVideoName, createdAbsolutePathFolder,
          logger,
          "original_%(id)s.%(ext)s");
      logger.debug("downloadedAudioFile = " + downloadedAudioFile);
    }

    return new DownloadFileResultValue(downloadedAudioFile, pathToCreatedFolder);
  }

  public List<File> cutTheFileIntoPieces(File downloadedAudioFile, List<CutValue> selectedItemsSet, Logger logger,
      String pathToYoutubeFolder, String extension, String ffmpegPath, String videoTitle) {

    ArrayList<File> files = new ArrayList<>();

    if (selectedItemsSet == null || selectedItemsSet.size() == 0) {
      logger.info("Files not selected");
      return files;
    }

    List<CompletableFuture<List<CutFileResultValue>>> callableList = new ArrayList<>();

    ExecutorService threadPool = programExecutor.getBackgroundExecutors();

    if (selectedItemsSet.size() == 1 && !Strings.isEmpty(videoTitle) && videoTitle.length() < MAX_TITLE_LENGTH) {
      selectedItemsSet.get(0).setTitle(videoTitle);
    }

    for (CutValue cutValue : selectedItemsSet) {

      CompletableFuture<List<CutFileResultValue>> completableFuture = CompletableFuture
          .supplyAsync(() -> cutOneFileByCutValue(ffmpegPath, downloadedAudioFile.getAbsolutePath(), cutValue,
              pathToYoutubeFolder, logger, extension), threadPool);
      callableList.add(completableFuture);
    }
    logger.info("callableList.size() = " + callableList.size());

    CompletableFuture.allOf(callableList.toArray(new CompletableFuture[callableList.size()]))
        .thenAccept(aVoid -> {
          logger.info("We are end all workers");
          for (CompletableFuture<List<CutFileResultValue>> completableFuture : callableList) {
            try {

              List<CutFileResultValue> cutFileResultValueList = completableFuture.get();
              if (cutFileResultValueList.get(0).choppedFile == null) {
                logger.info("Bad file " + cutFileResultValueList.get(0).commandArray);
                return;
              }
              files.add(cutFileResultValueList.get(0).choppedFile);
            } catch (InterruptedException | ExecutionException e) {
              logger.error(e.getMessage(), e);
              e.printStackTrace();
            }
          }
        }).join();

    logger.info("files = " + files);

    return files;
  }

  private DownloadState getDownloadStateFromDatabase(DownloadStateRepository downloadStateRepository,
      String fullVideoLink) {

    if (downloadStateRepository == null) {
      return DownloadState.builder().build();
    }

    return downloadStateRepository.findByVideoLink(fullVideoLink);
  }

  private DownloadState saveDownloadStateInDatabase(DownloadStateRepository downloadStateRepository,
      DownloadState downloadState) {

    if (downloadStateRepository == null) {
      return downloadState;
    }

    return downloadStateRepository.save(downloadState);
  }

  private @NonNull Single<DownloadState> getVideoIdFromVideoLink(@NonNull String videoLink, Logger logger,
      String pathToYoutubedl) {
    return Single.just(videoLink)
        .map(videoLink1 -> DownloadState.builder().videoLink(videoLink1).build())
        .doOnSuccess(downloadState -> logger.info("videoLink = " + downloadState.getVideoLink()))
        .map(downloadState -> getDownloadStateFromDatabase(downloadStateRepository, downloadState.getVideoLink()))
        .doOnSuccess(downloadState -> logger.info("We found record by videoLink in a database = " + downloadState))
        .onErrorReturn(throwable -> {
          logger.info("We cannot found record in database : " + throwable.getMessage());
          return DownloadState.builder().videoLink(videoLink).build();
        })
        .map(downloadState -> {

          if (TextUtils.isEmpty(downloadState.getVideoId())) {
            String videoId = getIdFromLink(pathToYoutubedl, videoLink, logger);
            downloadState = downloadState.toBuilder().videoId(videoId).build();
          }
          logger.info("idVideo = " + downloadState.getVideoId());
          if (Strings.isEmpty(downloadState.getVideoId()) || Strings.isBlank(downloadState.getVideoId())) {
            throw new NullPointerException("Wrong json request, videoId = " + downloadState.getVideoId());
          }
          return downloadState;
        });
  }

  private @NonNull Single<DownloadState> getJsonFromVideoId(Single<DownloadState> videoIdSingle, Logger logger,
      String pathToYoutubedl) {

    return videoIdSingle
        .map(state -> {
          String json = state.getJson();
          if (TextUtils.isEmpty(json)) {
            logger.info("Json from a database is null and we are downloading from internet");
            json = downloadJsonInMemory(pathToYoutubedl, state.getVideoLink(), logger);
            logger.info("Json from an internet length is " + json.length());
            return state.toBuilder().json(json).build();
          } else {
            logger.info("Json from a database exists");
            return state;
          }
        })
        .doOnError(throwable -> {
          logger.error("Error in json file");
          logger.error(throwable.getMessage());
        });
  }

  public @NonNull Single<DownloadState> getPairs(String pathToYoutubedl, String videoLink, Logger logger) {

    if (Strings.isEmpty(videoLink)) {
      logger.error("videoLink is null");
      return Single.error(new NullPointerException("videoLink is null"));
    }

    Single<DownloadState> videoIdSingle = getVideoIdFromVideoLink(videoLink, logger, pathToYoutubedl);

    Single<DownloadState> jsonSingle = getJsonFromVideoId(videoIdSingle, logger, pathToYoutubedl);

    Single<DownloadState> jsonWithTimeSingle = getTimeFromJsonSingle(jsonSingle);

    Single<DownloadState> videoTitleSingle = getTitleVideoFromJsonDataSingle(jsonWithTimeSingle);

    videoTitleSingle = videoTitleSingle.doOnSuccess(
        downloadState -> logger.info("videoTitle = " + downloadState.getVideoTitle()));

    Single<DownloadState> pairsWithSingle = getPairsWithSingle(videoTitleSingle, logger);
    pairsWithSingle = pairsWithSingle.map(downloadState -> {

      for (int i = 0; i < downloadState.getCutValues().size(); i++) {
        CutValue cutValue = downloadState.getCutValues().get(i);
        if (cutValue.getStartTimeInSecond() > cutValue.getEndTimeInSecond()) {
          cutValue.setEndTimeInSecond(downloadState.getDurationInSeconds());
          cutValue.setEndTime(getTimeStringFromTimeLong(cutValue.getEndTimeInSecond()));
        }
        cutValue.setTitle(removeInNameEmptyBrackets(cutValue.getTitle()));
      }
      for (CutValue pair : downloadState.getCutValues()) {
        logger.debug("pair = " + pair);
      }
      return downloadState;
    });

    return pairsWithSingle
        .map(downloadState -> {
          String audioFileName = getAudioFileNameFromJsonData(downloadState.getJson());
          logger.debug("badAudioFileName = " + audioFileName);
          String goodString = makeGoodString(audioFileName);
          logger.debug("goodAudioFileName = " + goodString);
          return downloadState.toBuilder().audioFileNameFromJson(goodString).build();
        })
        .doOnSuccess(downloadState -> {
          if (TextUtils.isEmpty(downloadState.getAudioFileNameFromJson())) {
            throw new NullPointerException();
          }
          logger.info("audioFileName = " + downloadState.getAudioFileNameFromJson());

        });

  }

  private String removeInNameEmptyBrackets(String text) {
    return text.replaceAll("\\[" + "]", "");
  }

  public String createFolder(String outputFolder, String videoId, String audioFileName, Logger logger) {
    File outputFolderPath = new File(outputFolder);
    if (!outputFolderPath.isDirectory()) {
      throw new WrongMethodTypeException("outputFolder must by folder");
    }

    String pathToFolder = outputFolder + videoId + File.separatorChar;
    logger.info("PathToFolder = " + pathToFolder);
    File folder = deleteAndCreateFolder(pathToFolder, audioFileName, logger);
    Objects.requireNonNull(folder, "Folder not created");
    if (!folder.exists() && !folder.isDirectory()) {
      logger.error("Folder not found with path " + folder.getAbsolutePath());
      throw new NullPointerException();
    }

    return folder.getAbsolutePath() + File.separator;
  }

  private String makeGoodString(String value) {
    return value.replaceAll("[/\\:*?\"<>|]", " ").trim();
  }

  public String getAudioFileNameFromJsonData(String jsonData) {
    String result;
    JSONObject jsonObject = new JSONObject(jsonData);
    result = jsonObject.getString("_filename");
    return result;
  }

  public String getTitleVideoFromJsonData(String jsonData) {
    String result;
    JSONObject jsonObject = new JSONObject(jsonData);
    result = jsonObject.getString("title");
    return result;
  }

  public @NonNull Single<DownloadState> getTitleVideoFromJsonDataSingle(Single<DownloadState> jsonSingle) {

    return jsonSingle.map(downloadState -> {
      String title = getTitleVideoFromJsonData(downloadState.getJson());
      title = makeGoodString(title);
      return downloadState.toBuilder().videoTitle(title).build();
    });
  }

  private String getIdFromLink(String pathToYoutubedl, String link, Logger logger) {

//    ArrayList<String> command = new ArrayList<>();
//    command.add(pathToYoutubedl);
//    command.add("--get-id");
//    command.add(link);

    String[] commandArray = new String[]{pathToYoutubedl, "--get-id", link};

    //logger.info(command.toString());
    //logger.info("programExecutor =" + programExecutor);

    Pair<Integer, List<List<String>>> result = programExecutor.executeCommand(commandArray, "", logger);
    String videoId = "";
    if (result.getKey() == 0) {
      logger.debug("result = " + result);
      videoId = result.getValue().get(0).get(0);
    } else {
      logger.error("error = " + result);
    }

    return videoId;
  }

  public String getTimeFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    int duration = jsonObject.getInt("duration");
    return String.valueOf(duration);
  }

  public @NonNull Single<DownloadState> getTimeFromJsonSingle(Single<DownloadState> jsonSingle) {

    return jsonSingle.map(downloadState -> {
      String duration = getTimeFromJson(downloadState.getJson());
      return downloadState.toBuilder().durationInSeconds(Long.parseLong(duration)).build();
    });
  }

  public static String getDescriptionFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    return jsonObject.getString("description");
  }

  public static JSONArray getChaptersFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    JSONArray chapters = new JSONArray();
    if (!jsonObject.isNull("chapters")) {
      chapters = jsonObject.getJSONArray("chapters");
    }

    return chapters;
  }

  private File deleteAndCreateFolder(String pathToFolder, String audioFilePath, Logger logger) {

    File file = new File(pathToFolder);
//    if (file.exists()) {
//      for (File listFile : file.listFiles()) {
//        if (listFile.getName().equals(audioFilePath)) {
//          logger.debug("We found a file: " + " " + audioFilePath);
//          continue;
//        }
//        //listFile.delete();
//      }
//    }
    boolean deleteResult = file.delete();
    logger.info("deleteResult = " + deleteResult);
    boolean mkdirsResult = file.mkdirs();
    logger.info("mkdirResult = " + mkdirsResult);
    return file;
  }

  private File downloadFileUsingYoutubedl(String pathToYoutubedl, String videoLink, String saveFolder, Logger logger,
      String maskDownloadedFile) {

    String rootDirPath;
    if (SystemUtils.IS_OS_LINUX) {
      rootDirPath = String.valueOf(File.separatorChar);
    } else {
      rootDirPath = "";
    }

    ArrayList<String> command = new ArrayList<>();
    command.add(pathToYoutubedl);
    command.add("-f");
    command.add("bestaudio");
    command.add("-o");
    command.add(saveFolder + maskDownloadedFile);
    command.add("--no-progress");
    command.add("-v");
    command.add("--no-continue");
    command.add(videoLink);

    Pair<Integer, List<List<String>>> outputResult = programExecutor
        .executeCommand(command.toArray(new String[0]), rootDirPath, logger);
    if (outputResult.getKey() == 0) {
      logger.debug("result = " + outputResult);
    } else {
      logger.error("");
    }
    logger.debug("outputResult = " + outputResult);
    String[] standardOutputResult = outputResult.getRight().get(0).toArray(new String[0]);
    for (String s : standardOutputResult) {
      final String downloadString = "[download]";
      if (s.startsWith(downloadString)) {
        int firstPoint = s.indexOf(downloadString) + downloadString.length();
        String downloadSubString = s.substring(firstPoint).trim();
        final String destinationString = "Destination:";
        if (downloadSubString.startsWith(destinationString)) {
          String pathToFile = downloadSubString.substring(destinationString.length()).trim();
          return new File(pathToFile);
        }
      }
    }
    return null;
  }

  @NonNull
  private String downloadJsonInMemory(String pathToYoutubedl, String id, Logger logger) {

    ArrayList<String> commandArray = new ArrayList<>();

    commandArray.add(pathToYoutubedl);
    commandArray.add("--skip-download");
    commandArray.add("-f");
    commandArray.add("bestaudio");
    commandArray.add("-o");
    commandArray.add("original_%(id)s.%(ext)s");
    commandArray.add("--print-json");
    commandArray.add(id);

    Pair<Integer, List<List<String>>> result = programExecutor.executeCommand(
        commandArray.toArray(new String[0]), "", logger);
    if (result.getKey() == 0) {
      logger.debug("result = " + result.getValue().size());
    } else {
      logger.error(String.valueOf(result.getValue()));
    }

    List<String> standardOutput = result.getValue().get(0);
    return standardOutput.get(0);
  }

  private ArrayList<CutFileResultValue> cutOneFileByCutValue(String ffmpegPath, String audioFilePath,
      CutValue pair, String pathToYoutubeFolder, Logger logger, String extension) {

    ArrayList<CutFileResultValue> result = new ArrayList<>();

    String startTime = pair.getStartTime();
    String endTime = pair.getEndTime();

    String fileName = pair.getTitle();
    fileName = makeGoodString(fileName);

    ArrayList<String> commandArray = new ArrayList<>();
    commandArray.add(ffmpegPath);
    // Replace file flag
    commandArray.add("-n");
    commandArray.add("-i");
    commandArray.add(audioFilePath);
    commandArray.add("-ss");
    commandArray.add(startTime);
    commandArray.add("-to");
    commandArray.add(endTime);

    String rootDirPath;
    String outputFilePath;
    if (SystemUtils.IS_OS_LINUX) {
      outputFilePath = fileName + "." + extension;
      rootDirPath = pathToYoutubeFolder;
    } else {
      outputFilePath = (pathToYoutubeFolder + fileName) + "." + extension;
      rootDirPath = "";
    }
    commandArray.add(outputFilePath);
    String[] program = commandArray.toArray(new String[0]);
    Pair<Integer, List<List<String>>> executionResult = programExecutor
        .executeCommand(program, rootDirPath, logger);
    if (executionResult.getKey() == 0) {
      logger.debug("result = " + result);
    } else {
      logger.error(String.valueOf(result));
    }

    if (executionResult.getLeft() != 0) {
      logger.debug("executionResult = " + executionResult);
    }
    String[] error = executionResult.getValue().get(1).toArray(new String[0]);

    String audioOutName = getFileNameFromFfmpegCut(error);
    if (SystemUtils.IS_OS_LINUX) {
      audioOutName = pathToYoutubeFolder + audioOutName;
    }
    if (audioOutName.isEmpty()) {
      logger.info("audioOutName.isEmpty()");
      audioOutName = outputFilePath;
    }
    logger.debug("audioOutName = " + audioOutName);
    File file = new File(audioOutName);
    if (!file.exists()) {
      logger.error("audioOutName = " + audioOutName);
      result.add(new CutFileResultValue(commandArray, null));
    } else {
      result.add(new CutFileResultValue(commandArray, file));
    }
    return result;
  }

  public ArrayList<CutFileResultValue> trimFileBySize(File file1, String ffmpegPath, Logger logger,
      String pathToYoutubeFolder, long maxFileSize) {

    return this.trimFileBySize(file1, ffmpegPath, logger, pathToYoutubeFolder, maxFileSize,
        value -> {
        });
  }

  public interface ResultPublisher<T> {

    void publishResult(T value);

  }

  public ArrayList<CutFileResultValue> trimFileBySize(File file1, String ffmpegPath, Logger logger,
      String pathToYoutubeFolder, long maxFileSize, ResultPublisher<CutFileResultValue> resultPublisher) {

    String audioFilePath = file1.getAbsolutePath();
    String extension = "mp3";
    String oldTime = "";
    String newTime = "00:00:00";
    ArrayList<CutFileResultValue> result = new ArrayList<>();
    int count = 1;

    String fileNameWithoutExtension = FilenameUtils.removeExtension(file1.getName());
    fileNameWithoutExtension = makeGoodString(fileNameWithoutExtension);
    while (!newTime.equals(oldTime)) {

      oldTime = newTime;
      ArrayList<String> commandArray = new ArrayList<>();
      commandArray.add(ffmpegPath);
//      Replace file flag -y always replace, -n never
      commandArray.add("-y");
      commandArray.add("-i");
      commandArray.add(audioFilePath);
      commandArray.add("-ss");
      commandArray.add(newTime);
      commandArray.add("-fs");
      commandArray.add(String.valueOf(maxFileSize));

      String rootDirPath;
      String outputFilePath;
      String fileName = fileNameWithoutExtension + "_" + count + "." + extension;
      count++;
      if (SystemUtils.IS_OS_LINUX) {
        outputFilePath = fileName;
        rootDirPath = pathToYoutubeFolder;
      } else {
        outputFilePath = pathToYoutubeFolder + fileName;
        rootDirPath = "";
      }
      commandArray.add(outputFilePath);
      String[] program = commandArray.toArray(new String[0]);
      Pair<Integer, List<List<String>>> executionResult = programExecutor.executeCommand(program, rootDirPath, logger);
      if (executionResult.getKey() == 0) {
        logger.debug("result = " + result);
      } else {
        logger.error(String.valueOf(result));
      }

      logger.debug("executionResult = " + executionResult);
      String[] error = executionResult.getValue().get(1).toArray(new String[0]);
      String trimmedFileName = getFileNameFromFfmpegCut(error);
      logger.info("trimmedFileName = " + trimmedFileName);

      String audioLength = getAudioFileLengthFromFfmpegCut(error);
      logger.debug("audioLength = " + audioLength);

      File file = new File(trimmedFileName);
      if (audioLength.equals("00:00:00") || audioLength.equals("00:00:00.00")) {
        logger.info("file.delete() = " + file.delete());
      }

      if (file.exists()) {

        CutFileResultValue cutFileResultValue = new CutFileResultValue(commandArray, file);
        result.add(cutFileResultValue);
        resultPublisher.publishResult(cutFileResultValue);
      }

      newTime = sumTwoTimeFromFfmpeg(oldTime, audioLength, logger);
      logger.debug("newTime = " + newTime);


    }

    return result;

  }

  public @NonNull Single<DownloadState> getPairsAndCutTheAllByThisPairsFileIntoPieces(String pathToYoutubedl,
      String videoId, Logger logger, String ffmpegPath, String outputFolderPath) {

    Single<DownloadState> jsonStateSingle = getPairs(pathToYoutubedl, videoId, logger);

    jsonStateSingle = jsonStateSingle.doOnSuccess(state -> saveDownloadStateInDatabase(downloadStateRepository, state));
    jsonStateSingle = jsonStateSingle.doOnError(throwable -> logger.error(throwable.getMessage(), throwable));

    return jsonStateSingle

        .map(state -> {
          List<File> files = downloadAndCutFileByCutValues(logger, state.getCutValues(),
              pathToYoutubedl, state.getAudioFileNameFromJson(), state.getVideoId(), outputFolderPath,
              ffmpegPath, state.getVideoTitle(), state.getVideoLink());
          files.remove(0);
          logger.info("downloadAndCutVideo is over");
          logger.info(files.toString());

          List<Path> paths = new ArrayList<>();
          for (File file : files) {
            paths.add(Paths.get(file.getPath()));
          }

          return state.toBuilder().trimmedFiles(paths).build();
        });
  }

  public List<File> downloadAndCutFileByCutValues(Logger logger, List<CutValue> selectedItems,
      String pathToYoutubedl, String audioFileNameFromJson, String videoId, String outputFolderPath, String ffmpegPath,
      String videoTitle, String fullLengthVideoName) {
    DownloadFileResultValue downloadedVideoFileResult = downloadVideo(logger, pathToYoutubedl, audioFileNameFromJson,
        videoId, outputFolderPath, fullLengthVideoName);

    List<File> result = new ArrayList<>();
    result.add(downloadedVideoFileResult.downloadedFile);
    result.addAll(cutTheFileIntoPieces(downloadedVideoFileResult.downloadedFile, selectedItems, logger,
        downloadedVideoFileResult.createdFolderPath, "mp3", ffmpegPath, videoTitle));
    return result;
  }

  @Data
  @AllArgsConstructor
  private class DownloadFileResultValue {

    File downloadedFile;
    String createdFolderPath;
  }

  @Data
  @AllArgsConstructor
  public class CutFileResultValue {

    List<String> commandArray;
    File choppedFile;
  }

  private String getFileNameFromFfmpegCut(String[] executeResult) {

    String result = "";
    for (String s : executeResult) {
      if (s.startsWith("Output #0")) {
        int firstPoint = s.indexOf(" to ") + " to ".length() + "\'".length();
        return s.substring(firstPoint, s.length() - 2);
      }
    }

    return result;
  }

  private String getAudioFileLengthFromFfmpegCut(String[] executeResult) {

    String result = "";
    for (int i = executeResult.length - 1; i >= 0; i--) {
      String string = executeResult[i];
      String searchString = "time=";
      int firstIndex = string.indexOf(searchString);
      if (firstIndex >= 0) {
        int secondIndex = string.indexOf(" ", firstIndex);
        if (secondIndex > 0) {
          return string.substring(firstIndex + searchString.length(), secondIndex);
        }
      }
    }

    return result;
  }

  private String getTimeStringFromTimeLong(long longTime) {

    Date date = new Date(longTime * 1000);
    DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return formatter.format(date);
  }

  private String sumTwoTimeFromFfmpeg(String time1, String time2, Logger logger) {
    try {
      DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date date = formatter.parse(time1);
      DateTime dateTime1 = new DateTime(date);
      date = formatter.parse(time2);
      DateTime dateTime2 = new DateTime(date);

      dateTime2 = dateTime2.plus(dateTime1.getMillis());
      return formatter.format(dateTime2.toDate());

    } catch (ParseException e) {
      e.printStackTrace();
      logger.error(e.getMessage(), e);
    }
    return time1;
  }

  private ArrayList<CutValue> parsingChaptersInfo(JSONArray chapters, long durationInSecond) {

    ArrayList<CutValue> pairs = new ArrayList<>();

    DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm:ss");

    for (int i = 0; i < chapters.length(); i++) {
      JSONObject jsonObject = chapters.getJSONObject(i);
      long endTimeInSeconds = jsonObject.getInt("end_time");
      long startTimeInSeconds = jsonObject.getInt("start_time");
      int duration = (int) (endTimeInSeconds - startTimeInSeconds);
      String title = jsonObject.getString("title");

      DateTime dt = new DateTime(startTimeInSeconds * 1000, DateTimeZone.UTC);

      String dtStr = fmt.print(dt);

      dt = new DateTime(endTimeInSeconds * 1000, DateTimeZone.UTC);
      String endTimeString = fmt.print(dt);

      title = makeGoodString(title);

      CutValue cutValue = CutValue.builder().title(title).startTime(dtStr).endTime(endTimeString)
          .startTimeInSecond(startTimeInSeconds).endTimeInSecond(endTimeInSeconds).build();

      pairs.add(cutValue);
    }

    String deadline = getHouseMinuteSecondStringFromLong(durationInSecond);
    completeCutValuesInformation(pairs, deadline, durationInSecond);

    return pairs;
  }

  private ArrayList<CutValue> parsingDescriptionInfoNewVersion(String description, long durationInSecond) {

    SimpleDateFormat minuteAndSeconds = new SimpleDateFormat("mm:ss");
    SimpleDateFormat hourAndMinuteAndSeconds = new SimpleDateFormat("hh:mm:ss");
    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
    formatter.setTimeZone(TimeZone.getTimeZone(String.valueOf(TimeZone.getTimeZone("UTC"))));

    String[] lines = description.split("\n");

    ArrayList<CutValue> cutValues = new ArrayList<>();

    for (String line : lines) {

      String[] split = line.split(" ");
      for (int i = 0; i < split.length; i++) {
        split[i] = split[i].strip();
      }

      for (int i1 = 0; i1 < split.length; i1++) {

        String goodTime = "";
        boolean isOnlyTwoParts = false;
        String firstPart = split[i1];
        try {
          Date date = minuteAndSeconds.parse(firstPart);
          goodTime = "00:" + firstPart;
          isOnlyTwoParts = true;
        } catch (ParseException ignored) {
        }

        if (!isOnlyTwoParts) {
          try {
            Date date = hourAndMinuteAndSeconds.parse(firstPart);
            goodTime = firstPart;
            isOnlyTwoParts = false;
          } catch (ParseException e) {
            continue;
          }
        }

        StringBuilder durationDescriptionBuilder = new StringBuilder();
        String[] nameRange = Arrays.copyOfRange(split, i1 + 1, split.length);
        for (String s : nameRange) {
          durationDescriptionBuilder.append(s).append(" ");
        }
        String durationDescription = durationDescriptionBuilder.toString();
        durationDescription = makeGoodString(durationDescription);

        long timeInSecond = Long.MIN_VALUE;
        try {
          timeInSecond = formatter.parse(goodTime).getTime();
          timeInSecond /= 1000;
        } catch (java.text.ParseException e) {
          e.printStackTrace();
        }

        cutValues.add(new CutValue(durationDescription, goodTime, "", timeInSecond, Long.MIN_VALUE));
      }
    }

    String deadline = getHouseMinuteSecondStringFromLong(durationInSecond);
    completeCutValuesInformation(cutValues, deadline, durationInSecond);

    return cutValues;
  }

  private String getHouseMinuteSecondStringFromLong(long durationInSecond) {
    DateTime dt = new DateTime(0, DateTimeZone.UTC);
    dt = dt.plusSeconds((int) durationInSecond);
    return DateTimeFormat.forPattern("HH:mm:ss").print(dt);
  }

  private void completeCutValuesInformation(List<CutValue> cutValues,
      String deadline, long deadlineInSeconds) {

    for (int i = 0; i < cutValues.size(); i++) {

      String futureStringTime;
      if (i != cutValues.size() - 1) {
        futureStringTime = cutValues.get(i + 1).getStartTime();
      } else {
        futureStringTime = deadline;
      }
      cutValues.get(i).setEndTime(futureStringTime);
    }
    for (int i = 0; i < cutValues.size(); i++) {

      long futureLongTime;
      if (i != cutValues.size() - 1) {
        futureLongTime = cutValues.get(i + 1).getStartTimeInSecond();
      } else {
        futureLongTime = deadlineInSeconds;
      }
      cutValues.get(i).setEndTimeInSecond(futureLongTime);
    }
  }
}
