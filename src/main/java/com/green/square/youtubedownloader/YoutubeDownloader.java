package com.green.square.youtubedownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.util.Pair;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.util.TextUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class YoutubeDownloader {


  public static void main(String... args) {

    getMusic(args);
  }

  public static void getMusic(String[] args) {

    String outFolder = "C:\\youtubeNew\\";
    String pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe";
    String linkId = "https://www.youtube.com/watch?v=xULTMMgwLuo&t=1784s";

    CommandArgumentsResult defaultArguments = new CommandArgumentsResult(pathToYoutubedl, outFolder, linkId);
    CommandArgumentsResult arguments = parsingArguments(args, defaultArguments);

    System.out.println("arguments = " + arguments);

    outFolder = arguments.outputFolderPath;
    pathToYoutubedl = arguments.pathToYoutubedl;
    linkId = arguments.linkId;

    List<String> links = new ArrayList<>();
    links.add(linkId);

    ArrayList<String> ids = new ArrayList<>();

    ExecutorService inputThread = Executors.newSingleThreadExecutor();
    ExecutorService errorThread = Executors.newSingleThreadExecutor();

    for (String videoLink : links) {
      String id = getIdFromLink(pathToYoutubedl, videoLink, inputThread, errorThread);
      System.out.println("id = " + id);
      if (TextUtils.isEmpty(id)) {
        throw new NullPointerException();
      }
      ids.add(id);
    }

    for (String id : ids) {

      String name = getFileName(pathToYoutubedl, id, inputThread, errorThread);
      System.out.println("name = " + name);
      if (TextUtils.isEmpty(name)) {
        throw new NullPointerException();
      }

      String jsonData = downloadJsonInMemory(pathToYoutubedl, id, name, inputThread, errorThread);

      String audioFileName = getAudioFileNameFromJsonData(jsonData);
      System.out.println("audioFileName = " + audioFileName);
      if (TextUtils.isEmpty(audioFileName)) {
        throw new NullPointerException();
      }

      File createdFolder = deleteAndCreateFolder(outFolder + File.separator + id, audioFileName);
      System.out.println("createdFolder = " + createdFolder);
      if (!createdFolder.exists()) {
        throw new NullPointerException();
      }

      File downloadedAudioFile;
      Path path = Paths.get(createdFolder + File.separator + audioFileName);
      if (Files.exists(path)) {
        System.out.println("File exists and don't need download it");
        downloadedAudioFile = path.toFile();
      } else {
        downloadedAudioFile = downloadFile(pathToYoutubedl, id, outFolder, name, inputThread, errorThread);
        System.out.println("downloadedAudioFile = " + downloadedAudioFile);
      }

      try {
        checkAudioFile(downloadedAudioFile);
////        convertFromM4atoAac(audioFile.getAbsolutePath(), outFolder, "new.aac");
//        //     audioFile = new File(audioFile.getAbsolutePath() + ".aac");
//        //     checkAudioFile(audioFile);
//        //
        //String jsonData = readJsonFile(outFolder, id);
        String duration = getTimeFromJson(jsonData);
        ArrayList<Pair<String, String>> pairs = getPairs(id, jsonData, name, inputThread, errorThread,
            pathToYoutubedl);
        for (Pair<String, String> pair : pairs) {
          System.out.println("pair = " + pair);
        }

        File newAudioFile = getAudioFile(id, outFolder, name);
        ArrayList<String> cutFiles = cutFileByPairs(downloadedAudioFile, pairs, duration, inputThread, errorThread);

        //uploadFileInGoogleDrive(Arrays.asList("Audio"), name, cutFiles);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        System.err.println(e);
      }

    }

    inputThread.shutdown();
    errorThread.shutdown();
  }

  private static void getFileNameFromJsonPath(String jsonPath) {

    // public static String outFolder = "C:\\youtubeNew\\";
    //  public static String pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe";

    File file = new File(jsonPath);
    String fileName = file.getName();


  }

  private static CommandArgumentsResult parsingArguments(String[] args, CommandArgumentsResult defaultValue) {

    Options options = new Options();

    Option optionYoutubedlOption = new Option("a", "pathToYoutubedl", true, "PathToYoutubedl");
    optionYoutubedlOption.setRequired(false);
    options.addOption(optionYoutubedlOption);

    Option outputFolderOption = new Option("b", "outputFolder", true, "OutputFolder");
    outputFolderOption.setRequired(false);
    options.addOption(outputFolderOption);

    Option linkIdOption = new Option("linkId", "linkId", true, "LinkId");
    linkIdOption.setRequired(true);
    options.addOption(linkIdOption);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);

      if (cmd.hasOption("a")) {
        defaultValue.pathToYoutubedl = cmd.getOptionValue("a");
      }

      if (cmd.hasOption("b")) {
        defaultValue.outputFolderPath = cmd.getOptionValue("b");
      }

      if (cmd.hasOption("linkId")) {
        defaultValue.linkId = cmd.getOptionValue("linkId");
      }

    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("utility-name", options);
      // System.exit(1);
    }

    return defaultValue;
  }

  public static class CommandArgumentsResult {

    private String pathToYoutubedl;
    private String outputFolderPath;
    private String linkId;

    public CommandArgumentsResult(String pathToYoutubedl, String outputFolderPath, String linkId) {
      this.pathToYoutubedl = pathToYoutubedl;
      this.outputFolderPath = outputFolderPath;
      this.linkId = linkId;
    }

    @Override
    public String toString() {
      return "CommandArgumentsResult{" +
          "pathToYoutubedl='" + pathToYoutubedl + '\'' +
          ", outputFolderPath='" + outputFolderPath + '\'' +
          ", shortId='" + linkId + '\'' +
          '}';
    }
  }

  private static ArrayList<Pair<String, String>> getDescFromYoutubeApi(String videoId, String pathToYoutubedl,
      ExecutorService inputThread, ExecutorService errorThread) {

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

  private static void uploadFileInGoogleDrive(List<String> pathToSave, String title, List<String> files) {
    GoogleDrive.getInstance().saveFileInGoogleDrive(pathToSave, title, files);
  }

  private static ArrayList<Pair<String, String>> getPairs(String videoId, String jsonData, String name,
      ExecutorService inputThread, ExecutorService errorThread, String pathToYoutubedl) {

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

    ArrayList<Pair<String, String>> commentPairs = getDescFromYoutubeApi(videoId, pathToYoutubedl, inputThread,
        errorThread);
    if (commentPairs.size() > result.size()) {
      result = commentPairs;
    }

    if (result.size() == 0) {
      result.add(new Pair<>("00:00:00", name));
    }

    return result;
  }

  public static void checkAudioFile(File file) throws FileNotFoundException {

    if (file == null) {
      throw new FileNotFoundException();
    }
    if (!file.exists()) {
      throw new FileNotFoundException();
    }
//    if (!file.getName().contains(name)) {
//      throw new IllegalArgumentException();
//    }
  }

  public static String makeGoodString(String value) {
    return value.replaceAll("[/\\-+.^:,]", "").trim();
  }

  public static ArrayList<Pair<String, String>> findEqualsName(ArrayList<Pair<String, String>> pairs) {

    ArrayList<Pair<String, String>> result = new ArrayList<>();
    Set<String> set = new HashSet<>();
    for (Pair<String, String> pair : pairs) {
      boolean contain = set.contains(pair.getValue());
      if (!contain) {
        set.add(pair.getValue());
        result.add(pair);
      } else {
        for (long i = 1; i < Long.MAX_VALUE; i++) {
          String name = pair.getValue() + " (" + i + ")";
          if (!set.contains(name)) {
            set.add(name);
            Pair<String, String> tmpPair = new Pair<>(pair.getKey(), name);
            result.add(tmpPair);
            break;
          }
        }
      }
    }

    return result;
  }

  private static String getAudioFilePathFromJsonFile(String pathToJsonFile) {
    String result = "";

    try {
      Path path = Paths.get(pathToJsonFile);
      String audioFilePath = String.join("", Files.readAllLines(path));
      JSONObject jsonObject = new JSONObject(audioFilePath);
      result = jsonObject.getString("_filename");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private static String getAudioFileNameFromJsonData(String jsonData) {
    String result;
    JSONObject jsonObject = new JSONObject(jsonData);
    result = jsonObject.getString("_filename");
    return result;
  }

  private static File getAudioFile(String id, String outFolder, String name) {

    File description = new File(outFolder + "\\" + id);

    for (File file : description.listFiles()) {
      String fileName = file.getName();

      boolean isM4a = fileName.endsWith(".m4a");
      boolean isMp4 = fileName.endsWith(".mp4");
      boolean isWebm = fileName.endsWith(".webm");
      boolean isStartWith = fileName.startsWith(name);

      if ((isM4a || isMp4 || isWebm)) {
        if (isStartWith) {
          return file;
        }
      }


    }

    System.err.printf("File %s not found", name);
    return null;
  }

  private static String getIdFromLink(String pathToYoutubedl, String link, ExecutorService inputThread,
      ExecutorService errorThread) {

    String commandPath = pathToYoutubedl
        + " --get-id "
        + link;
    String result = executeFunctionAndGetStringOutput(commandPath, inputThread, errorThread)[0];

    return result;
  }


  private static String getTimeFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    int duration = jsonObject.getInt("duration");

    return String.valueOf(duration);
  }

  private static String getTitleFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    String title = jsonObject.getString("title");
    title = makeGoodString(title);
    return title;
  }

  private static String getDescriptionFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    String description = jsonObject.getString("description");

    return description;
  }

  private static JSONArray getChaptersFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    JSONArray chapters = new JSONArray();
    if (!jsonObject.isNull("chapters")) {
      chapters = jsonObject.getJSONArray("chapters");
    }

    return chapters;
  }

  private static String readJsonFile(String outFolder, String id) {

    File description = new File(outFolder + "\\" + id);
    if (!description.isDirectory()) {
      throw new RuntimeException();
    }

    for (File file : description.listFiles()) {
      if (!file.getName().endsWith(".json")) {
        continue;
      }

      try {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
          result.append(line);
        }
        return result.toString();


      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException();
      }
    }

    return "";
  }

  private static String[] executeFunctionAndGetStringOutput(String stringCommand, ExecutorService inputThread,
      ExecutorService errorThread) {
    return executeFunctionAndGetStringOutputSync(stringCommand, inputThread, errorThread).get(0).toArray(new String[0]);
  }

  private static ArrayList<List<String>> executeFunctionAndGetStringOutputSync(String stringCommand,
      ExecutorService inputThread, ExecutorService errorThread) {

    ArrayList<List<String>> result = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      result.add(Collections.emptyList());
    }
    CountDownLatch countDownLatch = new CountDownLatch(2);

    try {
      Runtime runtime = Runtime.getRuntime();
      Process command = runtime.exec(stringCommand);

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

      errorThread.execute(new Runnable() {
        @Override
        public void run() {
          try {
            InputStream inputString = command.getErrorStream();
            List<String> resultInputString = getStringsFromInputStream(inputString);
            inputString.close();
            result.set(1, resultInputString);
          } catch (IOException e) {
            e.printStackTrace();
          }
          countDownLatch.countDown();
        }
      });
      int executionCode = command.waitFor();
      System.out.println("executionCode = " + executionCode);

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

    return result;
  }

  private static List<String> getStringsFromInputStream(InputStream inputStream) {

    String line;
    List<String> result = new ArrayList<>();
    try {
      Reader inputStreamReader = new InputStreamReader(inputStream);
      BufferedReader stdInput = new BufferedReader(inputStreamReader);
      while ((line = stdInput.readLine()) != null) {
        result.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;

  }

  private static String getFileName(String pathToYoutubedl, String id, ExecutorService inputThread,
      ExecutorService errorThread) {

    String commandPath = pathToYoutubedl
        + " -f bestaudio --get-filename "
        + "-o "
        + " %(title)s "
        + id;

    String[] outputResult = executeFunctionAndGetStringOutput(commandPath, inputThread, errorThread);
    return outputResult[0];
  }

  private static File deleteAndCreateFolder(String pathToFolder, String audioFilePath) {

    File file = new File(pathToFolder);
    if (file.exists()) {
      for (File listFile : file.listFiles()) {
        if (listFile.getName().equals(audioFilePath)) {
          System.out.println("We found file");
          continue;
        }
        listFile.delete();
      }
    }
    file.delete();
    file.mkdir();
    return file;
  }

  private static File downloadFile(String pathToYoutubedl, String id, String saveFolder, String name,
      ExecutorService inputThread, ExecutorService errorThread) {

    //String pathFile = saveFolder + id + "\\" + name;

    String commandPath = pathToYoutubedl
        + " -f bestaudio -o "
        + " \"" + saveFolder + "%(id)s\\" + name + ".%(ext)s\" "
        //+ " \"" + pathFile + "\" "
        //+ " -q "
        //    + " --no-warnings "
        + " --no-progress "
        //+ " --write-info-json "
        + id;

    System.out.println("commandPath = " + commandPath);
    ArrayList<List<String>> outputResult = executeFunctionAndGetStringOutputSync(commandPath, inputThread, errorThread);
    System.out.println("outputResult = " + outputResult);
    String[] standardOutputResult = outputResult.get(0).toArray(new String[0]);
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
    System.out.println("outputResult = " + outputResult);

    return null;
  }

  private static String downloadJsonInFile(String pathToYoutubedl, String id, String saveFolder, String name,
      ExecutorService inputThread, ExecutorService errorThread) {

    String commandPath = pathToYoutubedl
        + " --skip-download  -f bestaudio "
        + "  -o \"" + saveFolder + "%(id)s\\" + name + ".%(ext)s\" "
        + "  --write-info-json "
        + id;

    System.out.println("commandPath = " + commandPath);
    ArrayList<List<String>> result = executeFunctionAndGetStringOutputSync(commandPath, inputThread, errorThread);

    System.out.println("result = " + result);

    String errorJsonWriteString = "ERROR: Cannot write metadata to JSON file ";
    List<String> errorOutput = result.get(1);
    if (!errorOutput.isEmpty()) {
      for (String s : errorOutput) {
        if (s.startsWith(errorJsonWriteString)) {
          return s.substring(errorJsonWriteString.length());
        }
      }
    }

    String standardWritingVideoDescriptionMetadata = "[info] Writing video description metadata as JSON to: ";

    List<String> standardOutput = result.get(0);
    if (!standardOutput.isEmpty()) {
      for (String s : standardOutput) {
        if (s.startsWith(standardWritingVideoDescriptionMetadata)) {
          return s.substring(standardWritingVideoDescriptionMetadata.length());
        }
      }
    }
    return "";
  }

  private static String downloadJsonInMemory(String pathToYoutubedl, String id, String name,
      ExecutorService inputThread, ExecutorService errorThread) {

    String commandPath = pathToYoutubedl
        + " --skip-download  -f bestaudio "
        + "  -o \"" + name + ".%(ext)s\" "
        + "  --print-json "
        + id;

    System.out.println("commandPath = " + commandPath);
    ArrayList<List<String>> result = executeFunctionAndGetStringOutputSync(commandPath, inputThread, errorThread);
    System.out.println("result = " + result);

    List<String> standardOutput = result.get(0);
    return standardOutput.get(0);
  }


  private static ArrayList<String> cutFileByPairs(File audioFile, ArrayList<Pair<String, String>> pairs,
      String duration, ExecutorService inputThread, ExecutorService errorThread) {

    ArrayList<String> result = new ArrayList<>();

    for (int i = 0; i < pairs.size(); i++) {

      String startTime = pairs.get(i).getKey();
      String endTime;
      if (i != pairs.size() - 1) {
        endTime = pairs.get(i + 1).getKey();
      } else {
        endTime = String.valueOf(Integer.parseInt(duration));
      }

      String fileName = pairs.get(i).getValue().trim();
      fileName = makeGoodString(fileName);

      String audioOutName = (audioFile.getParent() + File.separator + fileName) + ".mp4";

      String commandPath = "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe "
          + " -loglevel debug "
          //   + " -y "
          + " -i " + "\"" + audioFile.getAbsolutePath() + "\"" + " "
          + " -ss " + startTime + " "
          + " -to " + endTime + " "
          + " \"" + audioOutName + "\"";

      System.out.println("commandPath = " + commandPath);

      ArrayList<List<String>> executeResult = executeFunctionAndGetStringOutputSync(commandPath, inputThread,
          errorThread);

      String[] error = executeResult.get(1).toArray(new String[0]);

      audioOutName = getFileNameFromFfmpegCut(error);
      System.out.println("audioOutName = " + audioOutName);
      File file = new File(audioOutName);
      if (!file.exists()) {
        System.out.println("audioOutName = " + audioOutName);
      } else {
        result.add(audioOutName);
      }


    }

    return result;
  }

  private static String getFileNameFromFfmpegCut(String[] executeResult) {

    String result = "";

    for (String s : executeResult) {
      if (s.startsWith("Output #0")) {
        int firstPoint = s.indexOf(" to ") + " to ".length() + "\'".length();
        return s.substring(firstPoint, s.length() - 2);
      }
    }

    return result;
  }

  private static ArrayList<Pair<String, String>> parsingChaptersInfo(JSONArray chapters) {

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
      DateTime dateTime = pattern.parseDateTime(o1.getKey());
      int o1s = dateTime.secondOfDay().get();
      dateTime = pattern.parseDateTime(o2.getKey());
      int o2s = dateTime.secondOfDay().get();
      return Integer.compare(o1s, o2s);
    });

    return pairs;
  }

  private static ArrayList<Pair<String, String>> parsingDescriptionInfo(String description) {

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
    DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");

    pairs.sort((o1, o2) -> {
      DateTime dateTime = pattern.parseDateTime(o1.getKey());
      int o1s = dateTime.secondOfDay().get();
      dateTime = pattern.parseDateTime(o2.getKey());
      int o2s = dateTime.secondOfDay().get();
      return Integer.compare(o1s, o2s);
    });

    return pairs;
  }

  private static String getAllBadCharacterFromString(String substring) {

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
