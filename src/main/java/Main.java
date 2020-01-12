import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javafx.util.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {

  public static void main(String[] args) {

    doJob();

  }

  private static void telegramBot(){

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

  private static void doJob(){
    String outFolder = "C:\\youtube\\";
    String pathToYoutubedl = "C:\\Users\\Andrey\\Downloads\\youtube\\youtube-dl.exe ";

    String videoLink = "https://www.youtube.com/watch?v=YfmkxMuOk6c&t=1756s";
//    String videoLink = "https://www.youtube.com/watch?v=y71lli8MS8s";

    String id = getIdFromLink(pathToYoutubedl, videoLink);
    downloadFile(pathToYoutubedl, id, outFolder);

    String jsonData = readJsonFile(outFolder, id);
    String duration = getTimeFromJson(jsonData);
    String description = getDescriptionFromJson(jsonData);

    ArrayList<Pair<String, String>> pairs = parsingDescriptionInfo(description);
    for (Pair<String, String> pair : pairs) {
      System.out.println("pair = " + pair);
    }

    File audioFile = getAudioFile(id, outFolder);
    cutFileByPairs(audioFile, pairs, duration);
  }

  private static File getAudioFile(String id, String outFolder) {

    File description = new File(outFolder + "\\" + id);

    for (File file : description.listFiles()) {
      if (!(file.getName().endsWith(".m4a") || file.getName().endsWith(".mp4") || file.getName().endsWith(".webm"))) {
        continue;
      }

      return file;
    }

    try {
      throw new FileNotFoundException();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
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

  private static String getDescriptionFromJson(String json) {

    JSONObject jsonObject = new JSONObject(json);
    String description = jsonObject.getString("description");

    return description;
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

  private static void downloadFile(String pathToYoutubedl, String id, String saveFolder) {
//>youtube-dl.exe

    File file = new File(saveFolder + "\\" + id);
    if (file.exists()) {
      for (File listFile : file.listFiles()) {
        listFile.delete();
      }
    }
    file.delete();

    String commandPath = pathToYoutubedl
        + " -f 140 -o "
        + " \"\"" + saveFolder + "%(id)s\\\\%(title)s.%(ext)s\"\" "
        + " -q --no-warnings --write-info-json "
        + id;

    try {
      Runtime runtime = Runtime.getRuntime();
      Process command = runtime.exec(commandPath);
      command.waitFor();

      BufferedReader stdInput = new BufferedReader(new
          InputStreamReader(command.getInputStream()));

      String line;
      while ((line = stdInput.readLine()) != null) {
        System.out.println(line);
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
  }

  private static void cutFileByPairs(File audioFile, ArrayList<Pair<String, String>> pairs, String duration) {

    for (int i = 0; i < pairs.size(); i++) {

      String startTime = pairs.get(i).getKey();
      String endTime;
      if (i != pairs.size() - 1) {
        endTime = pairs.get(i + 1).getKey();
      } else {
        endTime = duration;
      }

      String commandPath = "E:\\Programs\\ffmpeg\\bin\\ffmpeg.exe -loglevel quiet -stats"
          + " -y "
          + " -i " + "\"" + audioFile.getAbsolutePath() + "\"" + " "
          + " -ss " + startTime + " "
          + " -to " + endTime + " "
          + " \"" + (audioFile.getParent() + "\\" + pairs.get(i).getValue()
          .substring(0, Math.min(10, pairs.get(i).getValue().length())).trim() + "_" + i + ".mp4") + "\"";

      System.out.println("commandPath = " + commandPath);

      try {
        Runtime runtime = Runtime.getRuntime();
        Process command = runtime.exec(commandPath);
        command.waitFor();

        BufferedReader stdInput = new BufferedReader(new
            InputStreamReader(command.getInputStream()));

        String line;
        while ((line = stdInput.readLine()) != null) {
          System.out.println(line);
        }
        stdInput.close();
        BufferedReader errorInput = new BufferedReader(new
            InputStreamReader(command.getErrorStream()));

        while ((line = errorInput.readLine()) != null) {
          System.err.println(line);
        }

      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        System.out.println(e);
      }

    }

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
        goodLine = goodLine.trim();

        pairs.add(new Pair<>(goodTime, goodLine));

      } while (firstPoint != -1);


    }
    DateTimeFormatter pattern = DateTimeFormat.forPattern("HH:mm:ss");

    pairs.sort(new Comparator<Pair<String, String>>() {
      @Override
      public int compare(Pair<String, String> o1, Pair<String, String> o2) {
        DateTime dateTime = pattern.parseDateTime(o1.getKey());
        int o1s = dateTime.secondOfDay().get();
        dateTime = pattern.parseDateTime(o2.getKey());
        int o2s = dateTime.secondOfDay().get();
        return Integer.compare(o1s, o2s);
      }
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

  private static class DownloadedData {

    String description;
    String pathToFile;

    public DownloadedData(String description, String pathToFile) {
      this.description = description;
      this.pathToFile = pathToFile;
    }
  }
}
