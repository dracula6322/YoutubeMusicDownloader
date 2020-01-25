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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

  public static String outFolder = "C:\\youtubeNew\\";
  public static String pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe ";

  public static void main(String... args) throws IOException, GeneralSecurityException {

    List<String> links = new ArrayList<>();
    links.add("https://www.youtube.com/watch?v=xULTMMgwLuo&t=1784s");
    links.add("https://www.youtube.com/watch?v=Wv7ThDcf6FE"); //RDR ost from comments

    doJob(links);


//    GoogleDrive.getInstance().createFolder(Arrays.asList("Audio"), "Hi");

    //getDescFromYoutubeApi("https://www.youtube.com/watch?v=Wv7ThDcf6FE");

  }

  private static ArrayList<Pair<String, String>> getDescFromYoutubeApi(String link) {

    String idVideo = getIdFromLink(pathToYoutubedl, link);
    List<String> desc = YoutubeController.getInstance().getComments(idVideo);
    ArrayList<Pair<String, String>> result = new ArrayList<>();
    for (String s : desc) {
      ArrayList<Pair<String, String>> parsingDescriptionResult = parsingDescriptionInfo(s);
      System.out.println("s = " + s);
      System.out.println("parsingDescriptionResult.size() = " + parsingDescriptionResult.size());
      if (parsingDescriptionResult.size() > result.size()) {
        result = parsingDescriptionResult;
      }
    }

    return result;
  }

  private static void uploadFileInGoogleDrive(List<String> pathToSave, String title, List<String> files) {

    GoogleDrive.getInstance().saveFileInGoogleDrive(pathToSave, title, files);
  }

  private static ArrayList<Pair<String, String>> getPairs(String videoLink, String jsonData, String name) {

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

    ArrayList<Pair<String, String>> commentPairs = getDescFromYoutubeApi(videoLink);
    if (commentPairs.size() > result.size()) {
      result = commentPairs;
    }


    if (result.size() == 0) {
      result.add(new Pair<>("00:00:00", name));
    }

    return result;
  }

  //MongoDBHelper.getInstance().writeComparePairResult(id, descPairs, chaptersPairs, jsonData);

  private static void doJob(List<String> links) {

    for (String videoLink : links) {

      String id = getIdFromLink(pathToYoutubedl, videoLink);
      String name = getFileName(pathToYoutubedl, id);
      deleteAndCreateFolder(outFolder + "\\" + id);
      downloadJson(pathToYoutubedl, id, outFolder, name);

      File audioFile = downloadFile(pathToYoutubedl, id, outFolder, name);
      try {
        checkAudioFile(audioFile);
//        convertFromM4atoAac(audioFile.getAbsolutePath(), outFolder, "new.aac");
        //     audioFile = new File(audioFile.getAbsolutePath() + ".aac");
        //     checkAudioFile(audioFile);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        System.err.println(e);
      }

//
      String jsonData = readJsonFile(outFolder, id);
      String duration = getTimeFromJson(jsonData);
      ArrayList<Pair<String, String>> pairs = getPairs(videoLink, jsonData, name);


      //    File newAudioFile = getAudioFile(id, outFolder, name);
      ArrayList<String> cutFiles = cutFileByPairs(audioFile, pairs, duration);

      uploadFileInGoogleDrive(Arrays.asList("Audio"), name, cutFiles);
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
      int executionCode = command.waitFor();
      System.out.println("executionCode = " + executionCode);

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

    return executeFunctionAndGetStringOutput(stringCommand, true, false);
  }


  private static String[] executeFunctionAndGetStringOutputSync(String stringCommand, boolean isReturnGood,
      boolean isReturnError, ExecutorService inputThread, ExecutorService errorThread) {

    List<String> result = new CopyOnWriteArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(2);

    try {
      Runtime runtime = Runtime.getRuntime();
      Process command = runtime.exec(stringCommand);

      inputThread.execute(new Runnable() {
        @Override
        public void run() {
          System.out.println("nameThreadWithInput " + stringCommand + " " + Thread.currentThread().getName());
          String line;
          try {
            if (isReturnGood) {
              BufferedReader stdInput = new BufferedReader(new
                  InputStreamReader(command.getInputStream()));
              while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
                result.add(line);
              }
              stdInput.close();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          countDownLatch.countDown();
        }
      });

      errorThread.execute(new Runnable() {
        @Override
        public void run() {
          System.out.println("nameThreadWithError " + stringCommand + " " + Thread.currentThread().getName());
          String line;

          try {
            if (isReturnError) {
              BufferedReader stdInput = new BufferedReader(new
                  InputStreamReader(command.getErrorStream()));
              while ((line = stdInput.readLine()) != null) {
                System.err.println(line);
                result.add(line);
              }
              stdInput.close();
            }
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

    String[] massResult = new String[result.size()];

    return result.toArray(massResult);
  }

  private static String[] executeFunctionAndGetStringOutput(String stringCommand, boolean isReturnGood,
      boolean isReturnError) {

    ArrayList<String> result = new ArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(2);
    try {
      Runtime runtime = Runtime.getRuntime();
      Process command = runtime.exec(stringCommand);

      int executionCode = command.waitFor();
      System.out.println("executionCode = " + executionCode);

      BufferedReader stdInput = new BufferedReader(new
          InputStreamReader(command.getInputStream()));

      String line;

      if (isReturnGood) {
        while ((line = stdInput.readLine()) != null) {
          System.out.println(line);
          result.add(line);
        }
        stdInput.close();
      }

      if (isReturnError) {
        BufferedReader errInput = new BufferedReader(new
            InputStreamReader(command.getErrorStream()));

        while ((line = errInput.readLine()) != null) {
          System.err.println(line);
          result.add(line);
        }
        errInput.close();
      }


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

  private static void deleteAndCreateFolder(String pathToFolder) {

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

    ExecutorService inputThread = Executors.newSingleThreadExecutor();
    ExecutorService errorThread = Executors.newSingleThreadExecutor();

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

      String audioOutName = (audioFile.getParent() + "\\" + fileName);
      audioOutName += ".mp4";

      String commandPath = "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe "

          + " -loglevel debug "
          //   + " -y "
          + " -i " + "\"" + audioFile.getAbsolutePath() + "\"" + " "
          + " -ss " + startTime + " "
          + " -to " + endTime + " "
          + " \"" + audioOutName + "\"";

      System.out.println("commandPath = " + commandPath);

      String[] executeResult = executeFunctionAndGetStringOutputSync(commandPath, true, true, inputThread, errorThread);

      audioOutName = getFileNameFromFfmpegCut(executeResult);

      File file = new File(audioOutName);
      if (!file.exists()) {
        System.out.println("audioOutName = " + audioOutName);
      } else {
        result.add(audioOutName);
      }


    }

    inputThread.shutdown();
    errorThread.shutdown();

    return result;
  }

  private static String getFileNameFromFfmpegCut(String[] executeResult) {

    String result = null;

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
