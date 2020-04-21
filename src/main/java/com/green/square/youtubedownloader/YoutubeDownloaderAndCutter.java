package com.green.square.youtubedownloader;

import com.green.square.CutValue;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.util.TextUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

public class YoutubeDownloaderAndCutter {

  static public final int STATE_START = 1;
  static public final int STATE_GET_ID = 2;
  static public final int STATE_GET_JSON = 3;

  static public final int STATE_END = 10;

  private static YoutubeDownloaderAndCutter ourInstance = new YoutubeDownloaderAndCutter();

  public static YoutubeDownloaderAndCutter getInstance() {
    return ourInstance;
  }

  public YoutubeDownloaderAndCutter() {
  }

  public void downloadAndCutMusic(String pathToYoutubedl, String outFolder, List<String> links, String ffmpegPath,
      Logger logger) {

    ArrayList<String> ids = new ArrayList<>();

    ExecutorService inputThread = Executors.newSingleThreadExecutor();
    ExecutorService errorThread = Executors.newSingleThreadExecutor();

    for (String videoLink : links) {
      String id = getIdFromLink(pathToYoutubedl, videoLink, inputThread, errorThread, logger);
      logger.debug("id = " + id);
      if (TextUtils.isEmpty(id)) {
        throw new NullPointerException();
      }
      ids.add(id);
    }

    for (String id : ids) {
      String jsonData = downloadJsonInMemory(pathToYoutubedl, id, inputThread, errorThread, logger);
      //logger.debug("jsonData = " + jsonData);
      String audioFileName = getAudioFileNameFromJsonData(jsonData);
      logger.debug("audioFileName = " + audioFileName);

      audioFileName = makeGoodString(audioFileName);
      logger.debug("goodAudioName = " + audioFileName);
      if (TextUtils.isEmpty(audioFileName)) {
        throw new NullPointerException();
      }

      File createdFolder = deleteAndCreateFolder(outFolder + File.separator + id, audioFileName, logger);
      String pathToYoutubeFolder = createdFolder.getAbsolutePath() + File.separator;
      logger.debug("pathToYoutubeFolder = " + pathToYoutubeFolder);
      if (!Paths.get(pathToYoutubeFolder).toFile().exists()) {
        throw new NullPointerException();
      }

      File downloadedAudioFile;
      Path path = Paths.get(pathToYoutubeFolder + audioFileName);
      boolean downloadedFileIsExists = Files.exists(path);
      if (downloadedFileIsExists) {
        logger.debug("File exists and don't need download it");
        downloadedAudioFile = path.toFile();
      } else {
        downloadedAudioFile = downloadFile(pathToYoutubedl, id, pathToYoutubeFolder, inputThread,
            errorThread, logger, "original_%(id)s.%(ext)s");
        logger.debug("downloadedAudioFile = " + downloadedAudioFile);
      }

      String duration = getTimeFromJson(jsonData);
      ArrayList<Pair<String, String>> pairs = getPairs(id, jsonData, audioFileName);

//      try {
//        YoutubeAPIController.getInstance().getImages(id);
//      } catch (GeneralSecurityException | IOException e) {
//        e.printStackTrace();
//      }

      for (Pair<String, String> pair : pairs) {
        logger.debug("pair = " + pair);
      }

      if (pairs.size() != 1 || !pairs.get(0).first.equals("00:00:00")) {
        ArrayList<String> cutFiles = cutFileByPairs(ffmpegPath, downloadedAudioFile, pairs, duration, inputThread,
            errorThread, pathToYoutubeFolder, logger);

        checkGoodOrBadResult(cutFiles, pairs, logger);
      } else {
        logger.debug("Pair is empty");
      }
    }

    inputThread.shutdown();
    errorThread.shutdown();
  }

  private void checkGoodOrBadResult(ArrayList<String> cutFiles, ArrayList<Pair<String, String>> pairs, Logger logger) {

    logger.debug("cutFiles = " + cutFiles.toString());

    if (cutFiles.size() == pairs.size()) {
      int goodCount = 0;

      for (int i = 0; i < cutFiles.size(); i++) {
        if (cutFiles.get(i).contains(pairs.get(i).second)) {
          goodCount++;
        } else {
          logger.error(cutFiles.get(i));
          logger.error(pairs.get(i).second);
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

  public static final String pathToYoutubedlOptionsName = "pathToYoutubedl";
  public static final String outputFolderOptionsName = "outputFolder";
  public static final String linkIdOptionsName = "linkId";
  public static final String ffmpegPathOptionsName = "ffmpegPath";

  public CommandArgumentsResult parsingArguments(String[] args, CommandArgumentsResult defaultValue, Logger logger) {

    Options options = new Options();

    Option optionYoutubedlOption = new Option(pathToYoutubedlOptionsName, pathToYoutubedlOptionsName, true,
        "PathToYoutubedl");
    optionYoutubedlOption.setRequired(false);
    options.addOption(optionYoutubedlOption);

    Option outputFolderOption = new Option(outputFolderOptionsName, outputFolderOptionsName, true, "OutputFolder");
    outputFolderOption.setRequired(false);
    options.addOption(outputFolderOption);

    Option linkIdOption = new Option(linkIdOptionsName, linkIdOptionsName, true, "LinkId");
    linkIdOption.setRequired(false);
    options.addOption(linkIdOption);

    Option ffmpegPath = new Option(ffmpegPathOptionsName, ffmpegPathOptionsName, true, "FfmpegPath");
    ffmpegPath.setRequired(false);
    options.addOption(ffmpegPath);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption(pathToYoutubedlOptionsName)) {
        defaultValue.pathToYoutubedl = cmd.getOptionValue(pathToYoutubedlOptionsName);
      }

      if (cmd.hasOption(outputFolderOptionsName)) {
        defaultValue.outputFolderPath = cmd.getOptionValue(outputFolderOptionsName);
      }

      if (cmd.hasOption(linkIdOptionsName)) {
        defaultValue.linkId = cmd.getOptionValue(linkIdOptionsName);
      }

      if (cmd.hasOption(ffmpegPathOptionsName)) {
        defaultValue.ffmpegPath = cmd.getOptionValue(ffmpegPathOptionsName);
      }

    } catch (ParseException e) {
      logger.debug(e.getMessage());
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }

    return defaultValue;
  }

  public static ArrayList<Pair<String, String>> getDescFromYoutubeApi(String videoId) {

    List<String> desc = YoutubeAPIController.getInstance().getComments(videoId);
    ArrayList<Pair<String, String>> result = new ArrayList<>();
    for (String s : desc) {
      ArrayList<Pair<String, String>> parsingDescriptionResult = parsingDescriptionInfo(s);
      if (parsingDescriptionResult.size() > result.size()) {
        result = parsingDescriptionResult;
      }
    }

    return result;
  }

  public static ArrayList<CutValue> getDescFromYoutubeApiVersion2(String videoId, String durationInSeconds) {
    ArrayList<CutValue> result = new ArrayList<>();
    List<String> desc = YoutubeAPIController.getInstance().getComments(videoId);
    for (String s : desc) {
      ArrayList<CutValue> parsingDescriptionResult = parsingDescriptionInfoVersion2(s, durationInSeconds);
      if (parsingDescriptionResult.size() > result.size()) {
        result = parsingDescriptionResult;
      }
    }

    return result;
  }

  public static ArrayList<Pair<String, String>> getPairs(String videoId, String jsonData, String name) {

    ArrayList<Pair<String, String>> result;

    String descriptionFromJson = getDescriptionFromJson(jsonData);
    ArrayList<Pair<String, String>> descPairs = parsingDescriptionInfo(descriptionFromJson);

    JSONArray chapters = getChaptersFromJson(jsonData);
    ArrayList<Pair<String, String>> chaptersPairs = parsingChaptersInfo(chapters);
    if (descPairs.size() > chaptersPairs.size()) {
      result = descPairs;
    } else {
      result = chaptersPairs;
    }

    ArrayList<Pair<String, String>> commentPairs = getDescFromYoutubeApi(videoId);
    if (commentPairs.size() > result.size()) {
      result = commentPairs;
    }

    if (result.size() == 0) {
      result.add(new Pair<>("00:00:00", name));
    }

    return result;
  }

  public static ArrayList<CutValue> getPairsVersion2(String videoId, String jsonData, String name,
      String durationInSecond) {

    ArrayList<CutValue> result = new ArrayList<>();

    String descriptionFromJson = getDescriptionFromJson(jsonData);
    ArrayList<CutValue> cutValuesFromDescription = parsingDescriptionInfoVersion2(descriptionFromJson,
        durationInSecond);
    System.out.println("cutValues = " + cutValuesFromDescription);

    JSONArray chapters = getChaptersFromJson(jsonData);
    ArrayList<CutValue> cutValuesFromChapters = parsingChaptersInfoVersion2(chapters, durationInSecond);
    System.out.println("chaptersPairs = " + cutValuesFromChapters);
    if (cutValuesFromDescription.size() > cutValuesFromChapters.size()) {
      result = cutValuesFromDescription;
    } else {
      result = cutValuesFromChapters;
    }

    ArrayList<CutValue> commentPairs = getDescFromYoutubeApiVersion2(videoId, durationInSecond);
    if (commentPairs.size() > result.size()) {
      result = commentPairs;
    }

    return result;
  }

  public boolean checkAudioFile(File file) {

    return file.exists();
  }

  public static String makeGoodString(String value) {
    return value.replaceAll("[/\\-+^:,]", "").trim();
  }

  public ArrayList<Pair<String, String>> findEqualsName(ArrayList<Pair<String, String>> pairs) {

    ArrayList<Pair<String, String>> result = new ArrayList<>();
    Set<String> set = new HashSet<>();
    for (Pair<String, String> pair : pairs) {
      boolean contain = set.contains(pair.second);
      if (!contain) {
        set.add(pair.second);
        result.add(pair);
      } else {
        for (long i = 1; i < Long.MAX_VALUE; i++) {
          String name = pair.second + " (" + i + ")";
          if (!set.contains(name)) {
            set.add(name);
            Pair<String, String> tmpPair = new Pair<>(pair.first, name);
            result.add(tmpPair);
            break;
          }
        }
      }
    }

    return result;
  }

  public static String getAudioFileNameFromJsonData(String jsonData) {
    String result;
    JSONObject jsonObject = new JSONObject(jsonData);
    result = jsonObject.getString("_filename");
    return result;
  }

  public static String getIdFromLink(String pathToYoutubedl, String link, ExecutorService inputThread,
      ExecutorService errorThread, Logger logger) {

    ArrayList<String> command = new ArrayList<>();
    command.add(pathToYoutubedl);
    command.add("--get-id");
    command.add(link);

    logger.info(command.toString());
    Pair<Integer, ArrayList<List<String>>> result = executeFunctionAndGetStringOutputWithResult(
        command.toArray(new String[0]), "", inputThread, errorThread);
    if (result.first == 0) {
      logger.debug("result = " + result);
    } else {
      logger.error("");
    }

    return result.second.get(0).get(0);
  }

  public static String getTimeFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    int duration = jsonObject.getInt("duration");

    return String.valueOf(duration);
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

  public static Pair<Integer, ArrayList<List<String>>> executeFunctionAndGetStringOutputWithResult(
      String[] stringCommandArray,
      String rootDir, ExecutorService inputThread, ExecutorService errorThread) {

    ArrayList<String> commandArray = new ArrayList<>(Arrays.asList(stringCommandArray));
    int executionCode = -1;
    ArrayList<List<String>> result = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      result.add(Collections.emptyList());
    }
    CountDownLatch countDownLatch = new CountDownLatch(2);

    try {
      Runtime runtime = Runtime.getRuntime();
      Process command;
      if (TextUtils.isEmpty(rootDir)) {
        command = runtime.exec(commandArray.toArray(new String[]{}));
      } else {
        command = runtime.exec(commandArray.toArray(new String[]{}), new String[0], new File(rootDir));
      }
      inputThread.execute(() -> {
        try {
          InputStream inputString = command.getInputStream();
          List<String> resultInputString = getStringsFromInputStream(inputString);
          inputString.close();
          result.set(0, resultInputString);
        } catch (IOException e) {
          e.printStackTrace();
        }
        countDownLatch.countDown();
      });

      errorThread.execute(() -> {
        try {
          InputStream inputString = command.getErrorStream();
          List<String> resultInputString = getStringsFromInputStream(inputString);
          inputString.close();
          result.set(1, resultInputString);
        } catch (IOException e) {
          e.printStackTrace();
        }
        countDownLatch.countDown();
      });
      executionCode = command.waitFor();

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      System.err.println(e);
    }

    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Objects.requireNonNull(result);
    assert result.size() == 2;

    return new Pair<>(executionCode, result);
  }

  private static List<String> getStringsFromInputStream(InputStream inputStream) {

    String line;
    List<String> result = new ArrayList<>();
    try {
      Reader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      BufferedReader stdInput = new BufferedReader(inputStreamReader);
      while ((line = stdInput.readLine()) != null) {
        result.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;

  }

  public static File deleteAndCreateFolder(String pathToFolder, String audioFilePath, Logger logger) {

    File file = new File(pathToFolder);
    if (file.exists()) {
      for (File listFile : file.listFiles()) {
        if (listFile.getName().equals(audioFilePath)) {
          logger.debug("We found file");
          continue;
        }
        listFile.delete();
      }
    }
    file.delete();
    file.mkdir();
    return file;
  }

  public static File downloadFile(String pathToYoutubedl, String id, String saveFolder,
      ExecutorService inputThread, ExecutorService errorThread, Logger logger, String maskDownloadedFile) {

    String rootDirPath;
    if (SystemUtils.IS_OS_LINUX) {
      rootDirPath = "/";
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
    command.add("--no-cache-dir");
    command.add("--rm-cache-dir");
    command.add("--no-continue");
    command.add(id);

    logger.debug("command = " + command);

    //logger.debug("commandPath = " + commandPath);
    Pair<Integer, ArrayList<List<String>>> outputResult = executeFunctionAndGetStringOutputWithResult(
        command.toArray(new String[0]), rootDirPath, inputThread, errorThread);
    if (outputResult.first == 0) {
      logger.debug("result = " + outputResult);
    } else {
      logger.error("");
    }
    logger.debug("outputResult = " + outputResult);
    String[] standardOutputResult = outputResult.second.get(0).toArray(new String[0]);
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

  public static String downloadJsonInMemory(String pathToYoutubedl, String id, ExecutorService inputThread,
      ExecutorService errorThread, Logger logger) {

    ArrayList<String> commandArray = new ArrayList<>();

    commandArray.add(pathToYoutubedl);
    commandArray.add("--skip-download");
    commandArray.add("-f");
    commandArray.add("bestaudio");
    commandArray.add("-o");
//    commandArray.add("\"" + "%(title)s" + "\"");
    commandArray.add("original_%(id)s.%(ext)s");
    commandArray.add("--print-json");
    commandArray.add(id);

    logger.debug("commandPath = " + commandArray.toString());

    Pair<Integer, ArrayList<List<String>>> result = executeFunctionAndGetStringOutputWithResult(
        commandArray.toArray(new String[0]), "",
        inputThread,
        errorThread);
    if (result.first == 0) {
      logger.debug("result = " + result);
    } else {
      logger.error(String.valueOf(result.second));
    }

    List<String> standardOutput = result.second.get(0);
    return standardOutput.get(0);
  }

  private String getTitleVideo(String pathToYoutubedl, String id, ExecutorService inputThread,
      ExecutorService errorThread, Logger logger) {

    ArrayList<String> commandArray = new ArrayList<>();

    commandArray.add(pathToYoutubedl);
    commandArray.add("--skip-download");
    commandArray.add("-f");
    commandArray.add("bestaudio");
    commandArray.add("--get-title");
    commandArray.add(id);

    logger.info("commandPath = " + commandArray.toString());

    Pair<Integer, ArrayList<List<String>>> result = executeFunctionAndGetStringOutputWithResult(
        commandArray.toArray(new String[0]), "",
        inputThread,
        errorThread);
    if (result.first == 0) {
      logger.debug("result = " + result);
    } else {
      logger.error("");
    }

    List<String> standardOutput = result.second.get(0);
    return standardOutput.get(0);
  }


  private ArrayList<String> cutFileByPairs(String ffmpegPath, File audioFile, ArrayList<Pair<String, String>> pairs,
      String duration, ExecutorService inputThread, ExecutorService errorThread, String pathToYoutubeFolder,
      Logger logger) {

    ArrayList<String> result = new ArrayList<>();

    for (int i = 0; i < pairs.size(); i++) {

      String startTime = pairs.get(i).first;
      String endTime;
      if (i != pairs.size() - 1) {
        endTime = pairs.get(i + 1).first;
      } else {
        endTime = String.valueOf(Integer.parseInt(duration));
      }

      String fileName = pairs.get(i).second.trim();
      fileName = makeGoodString(fileName);

      ArrayList<String> commandArray = new ArrayList<>();
      commandArray.add(ffmpegPath);
      // commandArray.add(" -loglevel debug");
      commandArray.add("-y");
      commandArray.add("-i");
      commandArray.add(audioFile.getAbsolutePath());
      commandArray.add("-ss");
      commandArray.add(startTime);
      commandArray.add("-to");
      commandArray.add(endTime);

      String rootDirPath;
      String outputFilePath;
      if (SystemUtils.IS_OS_LINUX) {
        outputFilePath = fileName + ".mp4";
        rootDirPath = pathToYoutubeFolder;
      } else {
        outputFilePath = (pathToYoutubeFolder + fileName) + ".mp4";
        rootDirPath = "";
      }
      commandArray.add(outputFilePath);

      Pair<Integer, ArrayList<List<String>>> executionResult = executeFunctionAndGetStringOutputWithResult(
          commandArray.toArray(new String[0]), rootDirPath, inputThread, errorThread);
      if (executionResult.first == 0) {
        logger.debug("result = " + result);
      } else {
        logger.error(String.valueOf(result));
      }

      logger.debug("executionResult = " + executionResult);
      String[] error = executionResult.second.get(1).toArray(new String[0]);

      String audioOutName = getFileNameFromFfmpegCut(error);
      if (SystemUtils.IS_OS_LINUX) {
        audioOutName = pathToYoutubeFolder + audioOutName;
      }
      logger.debug("audioOutName = " + audioOutName);
      File file = new File(audioOutName);
      if (!file.exists()) {
        logger.error("audioOutName = " + audioOutName);
      } else {
        result.add(audioOutName);
      }

    }

    return result;
  }

  public static ArrayList<String> cutFileByCutValue(String ffmpegPath, File audioFile,
      ArrayList<Pair<String, String>> pairs,
      String duration, ExecutorService inputThread, ExecutorService errorThread, String pathToYoutubeFolder,
      Logger logger) {

    ArrayList<String> result = new ArrayList<>();

    for (int i = 0; i < pairs.size(); i++) {

      String startTime = pairs.get(i).first;
      String endTime;
      if (i != pairs.size() - 1) {
        endTime = pairs.get(i + 1).first;
      } else {
        endTime = String.valueOf(Integer.parseInt(duration));
      }

      String fileName = pairs.get(i).second.trim();
      fileName = makeGoodString(fileName);

      ArrayList<String> commandArray = new ArrayList<>();
      commandArray.add(ffmpegPath);
      // commandArray.add(" -loglevel debug");
      commandArray.add("-y");
      commandArray.add("-i");
      commandArray.add(audioFile.getAbsolutePath());
      commandArray.add("-ss");
      commandArray.add(startTime);
      commandArray.add("-to");
      commandArray.add(endTime);

      String rootDirPath;
      String outputFilePath;
      if (SystemUtils.IS_OS_LINUX) {
        outputFilePath = fileName + ".mp4";
        rootDirPath = pathToYoutubeFolder;
      } else {
        outputFilePath = (pathToYoutubeFolder + fileName) + ".mp4";
        rootDirPath = "";
      }
      commandArray.add(outputFilePath);

      Pair<Integer, ArrayList<List<String>>> executionResult = executeFunctionAndGetStringOutputWithResult(
          commandArray.toArray(new String[0]), rootDirPath, inputThread, errorThread);
      if (executionResult.first == 0) {
        logger.debug("result = " + result);
      } else {
        logger.error(String.valueOf(result));
      }

      logger.debug("executionResult = " + executionResult);
      String[] error = executionResult.second.get(1).toArray(new String[0]);

      String audioOutName = getFileNameFromFfmpegCut(error);
      if (SystemUtils.IS_OS_LINUX) {
        audioOutName = pathToYoutubeFolder + audioOutName;
      }
      logger.debug("audioOutName = " + audioOutName);
      File file = new File(audioOutName);
      if (!file.exists()) {
        logger.error("audioOutName = " + audioOutName);
      } else {
        result.add(audioOutName);
      }

    }

    return result;
  }

  public static ArrayList<String> cutFileByCutValueVersion2(String ffmpegPath, File audioFile,
      ArrayList<CutValue> pairs, ExecutorService inputThread, ExecutorService errorThread, String pathToYoutubeFolder,
      Logger logger) {

    ArrayList<String> result = new ArrayList<>();

    for (int i = 0; i < pairs.size(); i++) {

      String startTime = pairs.get(i).getStartTime();
      String endTime = pairs.get(i).getEndTime();

      String fileName = pairs.get(i).getTitle();
      fileName = makeGoodString(fileName);

      ArrayList<String> commandArray = new ArrayList<>();
      commandArray.add(ffmpegPath);
//      Replace file flag
//      commandArray.add("-y");
      commandArray.add("-i");
      commandArray.add(audioFile.getAbsolutePath());
      commandArray.add("-ss");
      commandArray.add(startTime);
      commandArray.add("-to");
      commandArray.add(endTime);

      String rootDirPath;
      String outputFilePath;
      if (SystemUtils.IS_OS_LINUX) {
        outputFilePath = fileName + ".mp4";
        rootDirPath = pathToYoutubeFolder;
      } else {
        outputFilePath = (pathToYoutubeFolder + fileName) + ".mp4";
        rootDirPath = "";
      }
      commandArray.add(outputFilePath);

      Pair<Integer, ArrayList<List<String>>> executionResult = executeFunctionAndGetStringOutputWithResult(
          commandArray.toArray(new String[0]), rootDirPath, inputThread, errorThread);
      if (executionResult.first == 0) {
        logger.debug("result = " + result);
      } else {
        logger.error(String.valueOf(result));
      }

      logger.debug("executionResult = " + executionResult);
      String[] error = executionResult.second.get(1).toArray(new String[0]);

      String audioOutName = getFileNameFromFfmpegCut(error);
      if (SystemUtils.IS_OS_LINUX) {
        audioOutName = pathToYoutubeFolder + audioOutName;
      }
      logger.debug("audioOutName = " + audioOutName);
      File file = new File(audioOutName);
      if (!file.exists()) {
        logger.error("audioOutName = " + audioOutName);
      } else {
        result.add(audioOutName);
      }

    }

    return result;
  }

  public static String getFileNameFromFfmpegCut(String[] executeResult) {

    String result = "";

    for (String s : executeResult) {
      if (s.startsWith("Output #0")) {
        int firstPoint = s.indexOf(" to ") + " to ".length() + "\'".length();
        return s.substring(firstPoint, s.length() - 2);
      }
    }

    return result;
  }

  public static ArrayList<Pair<String, String>> parsingChaptersInfo(JSONArray chapters) {

    ArrayList<Pair<String, String>> pairs = new ArrayList<>();
    DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");

    for (int i = 0; i < chapters.length(); i++) {
      JSONObject jsonObject = chapters.getJSONObject(i);
      double endTime = jsonObject.getDouble("end_time");
      double startTime = jsonObject.getDouble("start_time");
      int duration = (int) (endTime - startTime);
      String title = jsonObject.getString("title");

      DateTime dt = new DateTime((int) startTime * 1000, DateTimeZone.UTC);
      DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm:ss");
      fmt.withZone(DateTimeZone.UTC);
      String dtStr = fmt.print(dt);

      title = makeGoodString(title);

      pairs.add(new Pair<>(dtStr, title));
    }

    pairs.sort((o1, o2) -> {
      DateTime dateTime = pattern.parseDateTime(o1.first);
      int o1s = dateTime.secondOfDay().get();
      dateTime = pattern.parseDateTime(o2.first);
      int o2s = dateTime.secondOfDay().get();
      return Integer.compare(o1s, o2s);
    });

    return pairs;
  }

  public static ArrayList<CutValue> parsingChaptersInfoVersion2(JSONArray chapters, String durationInSecondString) {

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

      pairs.add(new CutValue(title, dtStr, endTimeString, startTimeInSeconds, endTimeInSeconds));
    }

//    pairs.sort((o1, o2) -> {
//      DateTime dateTime = pattern.parseDateTime(o1.first);
//      int o1s = dateTime.secondOfDay().get();
//      dateTime = pattern.parseDateTime(o2.first);
//      int o2s = dateTime.secondOfDay().get();
//      return Integer.compare(o1s, o2s);
//    });
    int durationInSecond = Integer.parseInt(durationInSecondString);
    DateTime dt = new DateTime(0, DateTimeZone.UTC);
    dt = dt.plusSeconds(durationInSecond);
    String output = DateTimeFormat.forPattern("HH:mm:ss").print(dt);
    pairs.add(new CutValue("full_original", "00:00:00", output, 0, durationInSecond));
    return pairs;
  }

  public static ArrayList<Pair<String, String>> parsingDescriptionInfo(String description) {

    String[] lines = description.split("\n");
    ArrayList<Pair<String, String>> pairs = new ArrayList<>();

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

        pairs.add(new Pair<>(goodTime, goodLine));

      } while (firstPoint != -1);
    }

    pairs.sort((o1, o2) -> {
      DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");
      DateTime dateTime = pattern.parseDateTime(o1.first);
      int o1s = dateTime.secondOfDay().get();
      dateTime = pattern.parseDateTime(o2.first);
      int o2s = dateTime.secondOfDay().get();
      return Integer.compare(o1s, o2s);
    });

    return pairs;
  }

  public static ArrayList<CutValue> parsingDescriptionInfoVersion2(String description, String durationInSecondString) {

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

    int durationInSecond = Integer.parseInt(durationInSecondString);
    Date dateDurationInSecond = new Date(durationInSecond);
    String durationInSecondStringFullFormat = formatter.format(new Date(durationInSecond));

    DateTime dt = new DateTime(0, DateTimeZone.UTC);
    dt = dt.plusSeconds(durationInSecond);
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

  public static String getAllBadCharacterFromString(String substring) {

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

  public static String setFullFormatTime(String time) {
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
