import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {

  public static String outFolder = "C:\\youtube\\";
  public static String pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe ";

  public static void main(String... args) throws IOException, GeneralSecurityException {

    List<String> links = new ArrayList<>();
//    links.add("https://www.youtube.com/watch?v=SMvXVtKjm5s&t=899s");
//    links.add("https://www.youtube.com/watch?v=5LW20jazxkg");
//    links.add("https://www.youtube.com/watch?v=hrlRAjUR0KQ&has_verified=1");
//    links.add("https://www.youtube.com/watch?v=4CyDFA5IO9U");
//    links.add("https://www.youtube.com/watch?v=ipdoxwEHXbM");
//    links.add("https://www.youtube.com/watch?v=xULTMMgwLuo&t=1784s");
    links.add("https://www.youtube.com/watch?v=m81koYhgc5o");
//
//    links.add("https://www.youtube.com/watch?v=N4mPA-tPvtc");

    //links.add("https://www.youtube.com/watch?v=Wv7ThDcf6FE"); RDR ost from comments

//    for (String link : links) {
//      String fileName = getFileName(pathToYoutubedl, link);
//      System.out.println("fileName = " + fileName);
//    }

//    doJob(links);

    getFolders();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  private static void getFolders() {

    GoogleDrive.getInstance().initGoogleDrive();
    List<com.google.api.services.drive.model.File> result = GoogleDrive.getInstance().getFolders("");
    System.out.println("result = " + result);
  }

  private static void uploadFileInGoogleDrive(String title, List<String> files) {

    GoogleDrive.getInstance().initGoogleDrive();
    GoogleDrive.getInstance().saveFileInGoogleDrive(title, files);
  }

  private static void telegramBot() {

    ApiContextInitializer.init();
    TelegramBotsApi telegramBotsApi = new TelegramBotsApi();

    try {
      telegramBotsApi.registerBot(new TelegramLongPollingBot() {
        @Override
        public String getBotToken() {
          return "826400786:AAFQOCssfBejtEhZPHgM89qb7b4bVRQTBiY";
        }

        @Override
        public void onUpdateReceived(Update update) {
          System.out.println("update = " + update);
        }

        @Override
        public String getBotUsername() {
          return "BotUSerNameCurrent";
        }
      });
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }

  }

  private static void doJob(List<String> links) {

    for (String videoLink : links) {

      String id = getIdFromLink(pathToYoutubedl, videoLink);
      String name = getFileName(pathToYoutubedl, id);
      clearFolder(outFolder + "\\" + id);
      downloadJson(pathToYoutubedl, id, outFolder, name);

      File audioFile = downloadFile(pathToYoutubedl, id, outFolder, name);
      try {
        checkAudioFile(audioFile);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        System.err.println(e);
      }

//
      String jsonData = readJsonFile(outFolder, id);
      String duration = getTimeFromJson(jsonData);
      String description = getDescriptionFromJson(jsonData);
//
//      String title = getTitleFromJson(jsonData);
      ArrayList<Pair<String, String>> descPairs = parsingDescriptionInfo(description);
//
      JSONArray chapters = getChaptersFromJson(jsonData);
      ArrayList<Pair<String, String>> chaptersPairs = parsingChaptersInfo(chapters);
//
      MongoDBHelper.getInstance().writeComparePairResult(id, descPairs, chaptersPairs, jsonData);
//
      System.out.println("descPairs = " + descPairs);
//
      System.out.println("\"Hi\" = " + "Hi");
//
      ArrayList<Pair<String, String>> pairs = new ArrayList<>(descPairs);
//
      if (pairs.size() == 0) {
        pairs.add(new Pair<>("00:00:00", name));
      }
      findEqualsName(pairs);

      for (Pair<String, String> pair : pairs) {
        System.out.println("pair = " + pair);
      }

      //    File newAudioFile = getAudioFile(id, outFolder, name);
      ArrayList<String> cutFiles = cutFileByPairs(audioFile, pairs, duration);

      uploadFileInGoogleDrive(name, cutFiles);
    }
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

  private static String getIdFromLink(String pathToYoutubedl, String link) {

    String result = "";

    String commandPath = pathToYoutubedl
        + " --get-id "
        + link;

    try {
      Runtime runtime = Runtime.getRuntime();
      Process command = runtime.exec(commandPath);
      command.waitFor();

      BufferedReader stdInput = new BufferedReader(new
          InputStreamReader(command.getInputStream()));

      String line;
      while ((line = stdInput.readLine()) != null) {
        result = line;
      }
      stdInput.close();
      stdInput = new BufferedReader(new
          InputStreamReader(command.getErrorStream()));

      while ((line = stdInput.readLine()) != null) {
        System.err.println(line);
      }
      stdInput.close();

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      System.out.println(e);
    }

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

  private static String[] executeFunctionAndGetStringOutput(String stringCommand) {

    return executeFunctionAndGetStringOutput(stringCommand, false);
  }

  private static String[] executeFunctionAndGetStringOutput(String stringCommand, boolean isReturnError) {

    ArrayList<String> result = new ArrayList<>();

    try {
      Runtime runtime = Runtime.getRuntime();
      Process command = runtime.exec(stringCommand);
      command.waitFor();

      BufferedReader stdInput = new BufferedReader(new
          InputStreamReader(command.getInputStream()));

      String line;
      while ((line = stdInput.readLine()) != null) {
        System.out.println(line);
        if (!isReturnError) {
          result.add(line);
        }
      }
      stdInput.close();
      stdInput = new BufferedReader(new
          InputStreamReader(command.getErrorStream()));

      while ((line = stdInput.readLine()) != null) {
        System.err.println(line);
        if (isReturnError) {
          result.add(line);
        }
      }
      stdInput.close();

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      System.err.println(e);
    }

    Objects.requireNonNull(result);

    String[] massResult = new String[result.size()];

    return result.toArray(massResult);
  }

  private static String getFileName(String pathToYoutubedl, String id) {

    String commandPath = pathToYoutubedl
        + " -f bestaudio --get-filename "
        + "-o "
        + " %(title)s "
        + id;

    System.out.println("commandPath = " + commandPath);
    String[] outputResult = executeFunctionAndGetStringOutput(commandPath);
    return outputResult[0];
  }

  private static void clearFolder(String pathToFolder) {

    File file = new File(pathToFolder);
    if (file.exists()) {
      for (File listFile : file.listFiles()) {
        listFile.delete();
      }
    }
    file.delete();
    file.mkdir();

  }

  private static File downloadFile(String pathToYoutubedl, String id, String saveFolder, String name) {

    //String pathFile = saveFolder + id + "\\" + name;

    String commandPath = pathToYoutubedl
        + " -f bestaudio -o "
        + " \"" + saveFolder + "%(id)s\\" + name + ".%(ext)s\" "
        //+ " \"" + pathFile + "\" "
        //+ " -q "
        //    + " --no-warnings "

        + " --no-progress "
        + " --write-info-json "
        + id;

    System.out.println("commandPath = " + commandPath);
    String[] outputResult = executeFunctionAndGetStringOutput(commandPath);
    for (String s : outputResult) {
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

  private static void downloadJson(String pathToYoutubedl, String id, String saveFolder, String name) {
//>youtube-dl.exe

    //        + " \"\"" + saveFolder + "%(id)s\\\\%(title)s.%(ext)s\"\" "

    String commandPath = pathToYoutubedl
        + " --skip-download  -f bestaudio -o "
        + " \"" + saveFolder + "%(id)s\\" + name + ".%(ext)s\" "
        + " -q --no-warnings --write-info-json "
        + id;

    System.out.println("commandPath = " + commandPath);
    executeFunctionAndGetStringOutput(commandPath);

  }

  private static ArrayList<String> cutFileByPairs(File audioFile, ArrayList<Pair<String, String>> pairs,
      String duration) {

    ArrayList<String> result = new ArrayList<>();

    int fileCount = pairs.size();

    for (int i = 0; i < fileCount; i++) {

      String startTime = pairs.get(i).getKey();
      String endTime;
      if (i != pairs.size() - 1) {
        endTime = pairs.get(i + 1).getKey();
      } else {
        endTime = duration;
      }

      String fileName = pairs.get(i).getValue().trim();
      fileName = makeGoodString(fileName);

      String audioOutName = (audioFile.getParent() + "\\" + fileName);
      audioOutName += ".mp4";

      String commandPath = "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe -stats"
          + " -y "
          + " -i " + "\"" + audioFile.getAbsolutePath() + "\"" + " "
          + " -ss " + startTime + " "
          + " -to " + endTime + " "
          + " \"" + audioOutName + "\"";

      System.out.println("commandPath = " + commandPath);
      String[] executeResult = executeFunctionAndGetStringOutput(commandPath, true);

      audioOutName = getFileNameFromFfmpegCut(executeResult);

      File file = new File(audioOutName);
      if (!file.exists()) {
        System.out.println("audioOutName = " + audioOutName);
      } else result.add(audioOutName);


    }

    return result;
  }

  private static String getFileNameFromFfmpegCut(String[] executeResult){

    String result = null;

    for (String s : executeResult) {
      if(s.startsWith("Output #0")){
        int firstPoint = s.indexOf(" to ") + " to ".length() + "\'".length();
        return s.substring(firstPoint, s.length() - 2);
      }
    }

    return result;
  }

  private static void convertFromM4atoAac(String pathToFile, String pathToOut, String fileName) {

    try {

      String commandPath = "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe "
          + " -i "
          + pathToFile + " "
          + "-acodec copy "
          + pathToOut;
      Runtime runtime = Runtime.getRuntime();
      Process command = runtime.exec(commandPath);

      BufferedReader stdInput = new BufferedReader(new
          InputStreamReader(command.getInputStream()));

      String line;
      while ((line = stdInput.readLine()) != null) {
        System.out.println(line);
      }

      BufferedReader errorInput = new BufferedReader(new
          InputStreamReader(command.getErrorStream()));

      while ((line = errorInput.readLine()) != null) {
        System.err.println(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e);
    }

  }

  private static ArrayList<Pair<String, String>> parsingChaptersInfo(JSONArray chapters) {

    ArrayList<Pair<String, String>> pairs = new ArrayList<>();
    DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");

    for (int i = 0; i < chapters.length(); i++) {
//{"end_time": 103.0, "start_time": 102.0, "title": "0- / Main Menu"}
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
