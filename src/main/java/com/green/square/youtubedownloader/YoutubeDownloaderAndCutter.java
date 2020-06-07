package com.green.square.youtubedownloader;

import com.green.square.CutValue;
import com.green.square.DownloadState;
import com.green.square.DownloadStateRepository;
import com.green.square.ProgramExecutor;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import java.io.File;
import java.lang.invoke.WrongMethodTypeException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.util.TextUtils;
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


  @Autowired
  private DownloadStateRepository downloadStateRepository;

  private ProgramExecutor programExecutor;

  @Autowired
  public YoutubeDownloaderAndCutter(ProgramExecutor programExecutor) {
    this.programExecutor = programExecutor;
  }


  private void checkGoodOrBadResult(ArrayList<String> cutFiles, ArrayList<CutValue> pairs, Logger logger) {

    logger.debug("cutFiles = " + cutFiles.toString());

    if (cutFiles.size() == pairs.size()) {
      int goodCount = 0;

      for (int i = 0; i < cutFiles.size(); i++) {
        if (cutFiles.get(i).contains(pairs.get(i).getTitle())) {
          goodCount++;
        } else {
          logger.error(cutFiles.get(i));
          logger.error(pairs.get(i).getTitle());
        }
      }
      logger.debug("Good cut " + goodCount + "/" + pairs.size());
    } else {
      logger.error("Bad cut");
    }

  }

  private void uploadFileInGoogleDrive(List<String> pathToSave, String title, List<String> files) {
    //GoogleDrive.getInstance().saveFileInGoogleDrive(pathToSave, title, files);
  }


  private ArrayList<CutValue> getDescFromYoutubeApi(String videoId, long durationInSeconds) {
    ArrayList<CutValue> result = new ArrayList<>();

    List<String> desc = YoutubeAPIController.getInstance().getComments(videoId);
    for (String s : desc) {
      ArrayList<CutValue> parsingDescriptionResult = parsingDescriptionInfo(s, durationInSeconds);
      if (parsingDescriptionResult.size() > result.size()) {
        result = parsingDescriptionResult;
      }
    }

    return result;
  }

  private Single<DownloadState> getPairsWithSingle(Single<DownloadState> jsonWithTimeSingle) {

    return jsonWithTimeSingle.map(downloadState -> {
      ArrayList<CutValue> pairs = getPairs(downloadState.getVideoId(), downloadState.getJson(),
          downloadState.getDurationInSeconds());

      return downloadState.toBuilder().pairs(pairs).build();
    });
  }

  private ArrayList<CutValue> getPairs(String videoId, String jsonData, long durationInSecond) {

    ArrayList<CutValue> result;

    String descriptionFromJson = getDescriptionFromJson(jsonData);
    ArrayList<CutValue> cutValuesFromDescription = parsingDescriptionInfo(descriptionFromJson, durationInSecond);
    System.out.println("cutValuesFromDescription.size() = " + cutValuesFromDescription.size());

    JSONArray chapters = getChaptersFromJson(jsonData);
    ArrayList<CutValue> cutValuesFromChapters = parsingChaptersInfo(chapters, durationInSecond);
    System.out.println("cutValuesFromChapters.size() = " + cutValuesFromChapters.size());
    if (cutValuesFromDescription.size() > cutValuesFromChapters.size()) {
      result = cutValuesFromDescription;
    } else {
      result = cutValuesFromChapters;
    }

    ArrayList<CutValue> cutValuesFromComment = getDescFromYoutubeApi(videoId, durationInSecond);
    System.out.println("cutValuesFromComment.size() = " + cutValuesFromComment.size());
    if (cutValuesFromComment.size() > result.size()) {
      result = cutValuesFromComment;
    }

    return result;
  }

  public File downloadVideo(Logger logger, String pathToYoutubedl, String audioFileName, String pathToYoutubeFolder,
      String videoId) {

    Path potentialVideoFilePath = Paths.get(pathToYoutubeFolder + audioFileName);
    logger.info("potentialVideoFilePath = " + potentialVideoFilePath.toString());
    File downloadedAudioFile;
    boolean downloadedFileIsExists = Files.exists(potentialVideoFilePath);
    if (downloadedFileIsExists) {
      logger.debug("File exists and don't need to download it");
      downloadedAudioFile = potentialVideoFilePath.toFile();
    } else {
      logger.debug("File not exists");
      File createdFolder = deleteAndCreateFolder(pathToYoutubeFolder, audioFileName, logger);
      String createdAbsolutePathFolder = createdFolder.getAbsolutePath() + File.separatorChar;
      downloadedAudioFile = downloadFileUsingYoutubedl(pathToYoutubedl, videoId, createdAbsolutePathFolder, logger,
          "original_%(id)s.%(ext)s");
      logger.debug("downloadedAudioFile = " + downloadedAudioFile);
    }

    return downloadedAudioFile;
  }

  public ArrayList<File> cutTheFileIntoPieces(String downloadedAudioPath,
      ArrayList<CutValue> selectedItemsSet, Logger logger, CommandArgumentsResult arguments, String pathToYoutubeFolder,
      long durationInSeconds) {

    ArrayList<File> files = new ArrayList<>();

    if (selectedItemsSet == null || selectedItemsSet.size() == 0) {
      logger.info("Files not selected");
      return files;
    }

    File downloadedAudioFile = Paths.get(downloadedAudioPath).toFile();

    boolean isWeFoundFullInterval = false;

    for (Iterator<CutValue> iterator = selectedItemsSet.listIterator(); iterator.hasNext(); ) {
      CutValue cutValue = iterator.next();
      if (cutValue.getStartTimeInSecond() == 0 && cutValue.getEndTimeInSecond() == durationInSeconds) {
        if (!isWeFoundFullInterval) {
          files.add(downloadedAudioFile);
          isWeFoundFullInterval = true;
        }
        iterator.remove();
      }
    }

    List<CompletableFuture<List<CutFileResultValue>>> callableList = new ArrayList<>();

    ExecutorService threadPool = programExecutor.getBackgroundExecutors();

    for (CutValue cutValue : selectedItemsSet) {

      CompletableFuture<List<CutFileResultValue>> completableFuture = CompletableFuture
          .supplyAsync(new Supplier<List<CutFileResultValue>>() {
            @Override
            public List<CutFileResultValue> get() {
              return cutOneFileByCutValue(arguments.ffmpegPath, downloadedAudioFile.getAbsolutePath(), cutValue,
                  pathToYoutubeFolder, logger);
            }
          }, threadPool);
      callableList.add(completableFuture);
    }
    System.out.println("callableList.size() = " + callableList.size());

    CompletableFuture.allOf(callableList.toArray(new CompletableFuture[callableList.size()]))
        .thenAccept(new java.util.function.Consumer<Void>() {
          @Override
          public void accept(Void aVoid) {
            logger.info("We are end all workers");
            for (CompletableFuture<List<CutFileResultValue>> completableFuture : callableList) {
              try {

                List<CutFileResultValue> cutFileResultValueList = completableFuture.get();
                if (cutFileResultValueList.get(0).cattedFile == null) {
                  logger.info("Bad file " + cutFileResultValueList.get(0).commandArray);
                  return;
                }
                files.add(cutFileResultValueList.get(0).cattedFile);
              } catch (InterruptedException | ExecutionException e) {
                logger.error(e.getMessage(), e);
                e.printStackTrace();
              }
            }
          }
        }).join();

    logger.info("files = " + files);

    return files;
  }

  private @NonNull Single<DownloadState> getVideoIdFromVideoLink(@NonNull String videoLink, Logger logger,
      String pathToYoutubedl) {
    return Single.just(videoLink)
        .map(videoLink1 -> DownloadState.builder().videoLink(videoLink1).build())
        .doOnSuccess(downloadState -> logger.info("videoLink = " + downloadState.getVideoLink()))
        .map(downloadState -> downloadStateRepository.findByVideoLink(downloadState.getVideoLink()))
        .onErrorReturn(throwable -> {
          logger.info("We cannot found record in database : " + throwable.getMessage());
          String videoId = getIdFromLink(pathToYoutubedl, videoLink, logger);
          return DownloadState.builder().videoLink(videoLink).videoId(videoId).build();
        })
        .doOnSuccess(downloadState -> {
          logger.info("We found record by videoLink in a database = " + downloadState);
          if (TextUtils.isEmpty(downloadState.getVideoId())) {
            throw new NullPointerException("idVideo is null or empty = " + downloadState.getVideoId());
          }
          logger.info("idVideo = " + downloadState.getVideoId());
        });
  }

  private @NonNull Single<DownloadState> getJsonFromVideoId(Single<DownloadState> videoIdSingle, Logger logger,
      String pathToYoutubedl) {

    return videoIdSingle
        .map(new Function<DownloadState, DownloadState>() {
          @Override
          public DownloadState apply(DownloadState downloadState) {
            String json = downloadState.getJson();
            if (TextUtils.isEmpty(json)) {
              logger.info("Json from a database is null");
              json = downloadJsonInMemory(pathToYoutubedl, downloadState.getVideoId(), logger);
              logger.info("Json from an internet is " + json.length());
              return downloadState.toBuilder().json(json).build();
            } else {
              logger.info("Json from a database exists");
              return downloadState;
            }
          }
        })
        .doOnError(new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Throwable {
            logger.error("Error in json file");
            logger.error(throwable.getMessage());
          }
        });
  }

  public @NonNull Single<DownloadState> getPairs(String pathToYoutubedl, String videoLink, Logger logger) {

    if (TextUtils.isEmpty(videoLink)) {
      logger.error("videoLink is null");
      return Single.error(new NullPointerException("videoLink is null"));
    }

    Single<DownloadState> videoIdSingle = getVideoIdFromVideoLink(videoLink, logger, pathToYoutubedl);

    Single<DownloadState> jsonSingle = getJsonFromVideoId(videoIdSingle, logger, pathToYoutubedl);

    Single<DownloadState> jsonWithTimeSingle = getTimeFromJsonSingle(jsonSingle);

    Single<DownloadState> videoTitleSingle = getTitleVideoFromJsonDataSingle(jsonWithTimeSingle);

    videoTitleSingle = videoTitleSingle.doOnSuccess(new Consumer<DownloadState>() {
      @Override
      public void accept(DownloadState downloadState) throws Throwable {
        logger.info("videoTitle = " + downloadState.getVideoTitle());
      }
    });

    Single<DownloadState> pairsWithSingle = getPairsWithSingle(videoTitleSingle);
    pairsWithSingle = pairsWithSingle.map(new Function<DownloadState, DownloadState>() {
      @Override
      public DownloadState apply(DownloadState downloadState) throws Throwable {

        for (int i = 0; i < downloadState.getPairs().size(); i++) {
          CutValue cutValue = downloadState.getPairs().get(i);
          if (cutValue.getStartTimeInSecond() > cutValue.getEndTimeInSecond()) {
            cutValue.setEndTimeInSecond(downloadState.getDurationInSeconds());
            cutValue.setEndTime(getTimeStringFromTimeLong(cutValue.getEndTimeInSecond()));
          }
        }
        for (CutValue pair : downloadState.getPairs()) {
          logger.debug("pair = " + pair);
        }
        return downloadState;
      }
    });

    return pairsWithSingle
        .map(downloadState -> {
          String audioFileName = getAudioFileNameFromJsonData(downloadState.getJson());
          logger.debug("badAudioFileName = " + audioFileName);
          String goodString = makeGoodString(audioFileName);
          logger.debug("goodAudioFileName = " + goodString);
          return downloadState.toBuilder().audioFileName(goodString).build();
        })
        .doOnSuccess(downloadState -> {
          if (TextUtils.isEmpty(downloadState.getAudioFileName())) {
            throw new NullPointerException();
          }
          logger.info("audioFileName = " + downloadState.getAudioFileName());

        });

  }

  private @NonNull Single<DownloadState> createFolderSingle(Single<DownloadState> downloadStateSingle,
      String outputFolder, Logger logger) {

    return downloadStateSingle.map(new Function<DownloadState, DownloadState>() {
      @Override
      public DownloadState apply(DownloadState downloadState) throws Throwable {
        String createdFolderPath = createFolder(outputFolder, downloadState.getVideoId(),
            downloadState.getAudioFileName(), logger);
        return downloadState.toBuilder().createdFolderPath(createdFolderPath).build();
      }
    });
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
      logger.error("Folder not found");
      throw new NullPointerException();
    }

    return folder.getAbsolutePath() + File.separator;
  }

  private String makeGoodString(String value) {
    return value.replaceAll("[/\\:*?\"<>|]", "").trim();
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

    return jsonSingle.map(new Function<DownloadState, DownloadState>() {
      @Override
      public DownloadState apply(DownloadState downloadState) throws Throwable {
        String title = getTitleVideoFromJsonData(downloadState.getJson());
        return downloadState.toBuilder().videoTitle(title).build();
      }
    });
  }

  private String getIdFromLink(String pathToYoutubedl, String link, Logger logger) {

    ArrayList<String> command = new ArrayList<>();
    command.add(pathToYoutubedl);
    command.add("--get-id");
    command.add(link);

    logger.info(command.toString());

    logger.info("programExecutor =" + programExecutor);
    logger.info("downloadState =" + downloadStateRepository);

    Pair<Integer, List<List<String>>> result = programExecutor.executeFunctionAndGetStringOutputWithResult(
        command.toArray(new String[0]), "", logger);
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
    String description = jsonObject.getString("description");

    return description;
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
    if (file.exists()) {
      for (File listFile : file.listFiles()) {
        if (listFile.getName().equals(audioFilePath)) {
          logger.debug("We found a file: " + " " + audioFilePath);
          continue;
        }
        listFile.delete();
      }
    }
    boolean deleteResult = file.delete();
    logger.info("deleteResult = " + deleteResult);
    boolean mkdirResult = file.mkdir();
    logger.info("mkdirResult = " + mkdirResult);
    return file;
  }

  private File downloadFileUsingYoutubedl(String pathToYoutubedl, String id, String saveFolder, Logger logger,
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
    command.add(id);

    logger.debug("command = " + command);

    Pair<Integer, List<List<String>>> outputResult = programExecutor.executeFunctionAndGetStringOutputWithResult(
        command.toArray(new String[0]), rootDirPath, logger);
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

    logger.debug("commandPath = " + commandArray.toString());

    Pair<Integer, List<List<String>>> result = programExecutor.executeFunctionAndGetStringOutputWithResult(
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
      CutValue pair, String pathToYoutubeFolder, Logger logger) {

    ArrayList<CutFileResultValue> result = new ArrayList<>();

    String startTime = pair.getStartTime();
    String endTime = pair.getEndTime();

    String fileName = pair.getTitle();
    fileName = makeGoodString(fileName);

    ArrayList<String> commandArray = new ArrayList<>();
    commandArray.add(ffmpegPath);
//      Replace file flag
    commandArray.add("-n");
    commandArray.add("-i");
    commandArray.add(audioFilePath);
    commandArray.add("-ss");
    commandArray.add(startTime);
    commandArray.add("-to");
    commandArray.add(endTime);

    String extension = FilenameUtils.getExtension(audioFilePath);

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
    logger.info("Run " + Arrays.toString(program));
    Pair<Integer, List<List<String>>> executionResult = programExecutor
        .executeFunctionAndGetStringOutputWithResult(program, rootDirPath, logger);
    if (executionResult.getKey() == 0) {
      logger.debug("result = " + result);
    } else {
      logger.error(String.valueOf(result));
    }

    logger.debug("executionResult = " + executionResult);
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

  @Data
  @AllArgsConstructor
  public class CutFileResultValue {

    List<String> commandArray;
    File cattedFile;

  }

  private List<File> cutFileByCutValue(String ffmpegPath, String audioFilePath,
      ArrayList<CutValue> pairs, String pathToYoutubeFolder,
      Logger logger) {

    ArrayList<CutFileResultValue> result = new ArrayList<>();

    for (int i = 0; i < pairs.size(); i++) {
      result = cutOneFileByCutValue(ffmpegPath, audioFilePath, pairs.get(i), pathToYoutubeFolder, logger);
    }
    return Collections.singletonList(result.get(0).cattedFile);
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

  private String getTimeStringFromTimeLong(long longTime) {

    Date date = new Date(longTime * 1000);
    DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return formatter.format(date);

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

      CutValue cutValue = CutValue.builder()
          .title(title)
          .startTime(dtStr)
          .endTime(endTimeString)
          .startTimeInSecond(startTimeInSeconds)
          .endTimeInSecond(endTimeInSeconds)
          .build();

      pairs.add(cutValue);
    }

//    pairs.sort((o1, o2) -> {
//      DateTime dateTime = pattern.parseDateTime(o1.first);
//      int o1s = dateTime.secondOfDay().get();
//      dateTime = pattern.parseDateTime(o2.first);
//      int o2s = dateTime.secondOfDay().get();
//      return Integer.compare(o1s, o2s);
//    });
    DateTime dt = new DateTime(0, DateTimeZone.UTC);
    dt = dt.plusSeconds((int) durationInSecond);
    String output = DateTimeFormat.forPattern("HH:mm:ss").print(dt);

    CutValue cutValue = CutValue.builder()
        .title("full_original")
        .startTime("00:00:00")
        .endTime(output)
        .startTimeInSecond(0)
        .endTimeInSecond(durationInSecond)
        .build();

    pairs.add(cutValue);
    return pairs;
  }

  private ArrayList<CutValue> parsingDescriptionInfo(String description, long durationInSecond) {

    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss");
    formatter.setTimeZone(TimeZone.getTimeZone(String.valueOf(TimeZone.getTimeZone("UTC"))));

    String[] lines = description.split("\n");
    //ArrayList<Pair<String, String>> pairs = new ArrayList<>();
    ArrayList<CutValue> cutValues = new ArrayList<>();

    for (String line : lines) {

      int firstPoint = -1;
      do {
        firstPoint = line.indexOf(":", firstPoint + 1);
        if (firstPoint == -1) {
          break;
        }
        int firstPointNumber = firstPoint;
        while (Character.isDigit(line.charAt(firstPointNumber)) || line.charAt(firstPointNumber) == ':') {
          firstPointNumber--;
          if (firstPointNumber < 0) {
            firstPointNumber = 0;
            break;
          }
        }
        int secondPointNumber = firstPoint + 1;
        if (secondPointNumber < line.length()) {
          while (Character.isDigit(line.charAt(secondPointNumber)) || line.charAt(secondPointNumber) == ':') {
            secondPointNumber++;
            if (secondPointNumber >= line.length()) {
              break;
            }
          }
        }
        secondPointNumber++;
        secondPointNumber = Math.min(secondPointNumber, line.length());
        String time = line.substring(firstPointNumber, secondPointNumber);
        time = time.trim();
        firstPoint = secondPointNumber;
        if (time.length() < 4) {
          continue;
        }
//
        String clearTime = getAllBadCharacterFromString(time);

        String goodTime = setFullFormatTime(clearTime);

        String goodLine = line.substring(0, firstPointNumber) + line.substring(secondPointNumber);

        goodLine = makeGoodString(goodLine);

        long timeInSecond = Long.MIN_VALUE;
        try {
          timeInSecond = formatter.parse(goodTime).getTime();
          timeInSecond /= 1000;
        } catch (java.text.ParseException e) {
          e.printStackTrace();
        }

        cutValues.add(new CutValue(goodLine, goodTime, "", timeInSecond, Long.MIN_VALUE));
        //pairs.add(new Pair<>(goodTime, goodLine));

      } while (firstPoint != -1);
    }

    Date dateDurationInSecond = new Date(durationInSecond);
    String durationInSecondStringFullFormat = formatter.format(new Date(durationInSecond));

    DateTime dt = new DateTime(0, DateTimeZone.UTC);
    dt = dt.plusSeconds((int) durationInSecond);
    String output = DateTimeFormat.forPattern("HH:mm:ss").print(dt);

    for (int i = 0; i < cutValues.size(); i++) {

      String futureStringTime;
      if (i != cutValues.size() - 1) {
        futureStringTime = cutValues.get(i + 1).getStartTime();
      } else {
        futureStringTime = output;
      }
      cutValues.get(i).setEndTime(futureStringTime);

      long timeInSecond = Long.MIN_VALUE;
      try {
        timeInSecond = formatter.parse(cutValues.get(i).getEndTime()).getTime();
        timeInSecond /= 1000;
      } catch (java.text.ParseException e) {
        e.printStackTrace();
      }
      cutValues.get(i).setEndTimeInSecond(timeInSecond);
    }
    cutValues.add(new CutValue("full_original", "00:00:00", output, 0, durationInSecond));

//    pairs.sort((o1, o2) -> {
//      DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");
//      DateTime dateTime = pattern.parseDateTime(o1.first);
//      int o1s = dateTime.secondOfDay().get();
//      dateTime = pattern.parseDateTime(o2.first);
//      int o2s = dateTime.secondOfDay().get();
//      return Integer.compare(o1s, o2s);
//    });

    return cutValues;
  }

  private String getAllBadCharacterFromString(String substring) {

    int firstPoint = 0;
    while (!Character.isDigit(substring.charAt(firstPoint))) {
      firstPoint++;
    }
    int secondPoint = substring.length() - 1;
    while (!Character.isDigit(substring.charAt(secondPoint))) {
      secondPoint--;
    }

    return substring.substring(firstPoint, secondPoint + 1);
  }

  private String setFullFormatTime(String time) {
    String[] splitTime = time.split(":");
    List<String> arraySplitTime = new ArrayList<String>(Arrays.asList(splitTime));
    if (arraySplitTime.size() == 3) {
      String hour = arraySplitTime.get(0);
      if (hour.length() == 1) {
        arraySplitTime.set(0, '0' + arraySplitTime.get(0));
      }
    }
    if (arraySplitTime.size() == 2) {
      arraySplitTime.add(0, "00");
    }
    String minute = arraySplitTime.get(1);
    if (minute.length() == 1) {
      arraySplitTime.set(1, '0' + arraySplitTime.get(1));
    }

    return String.join(":", arraySplitTime);
  }
}
