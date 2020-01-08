import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javafx.util.Pair;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

  public static void main(String[] args) {

    runProgram();

    //parsingDescriptionInfo(testDesc);

  }

  private static void runProgram() {
    String decodeString = GetVideosRequest.getInstance()
        .sendPing("https://www.youtube.com/watch?v=obK-k848Vto");
    // https://www.youtube.com/watch?v=7MSFW8pZ-_4&t=2621s
    try {
      String oldText;
      do {
        oldText = decodeString;
        decodeString = URLDecoder.decode(oldText, StandardCharsets.UTF_8.name());
      } while (!oldText.equals(decodeString));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

//    String[] asd = decodeString.split("&");
//    String playerResponseRecord = "";

//    String playerResonseString = "player_response=";
//    for (String s : asd) {
//      if (s.startsWith(playerResonseString)) {
//        playerResponseRecord = s.substring(playerResonseString.length());
//      }
//    }

    String playerResponseString = findPlayerResponse(decodeString);

    String videoUrl = "";
    JSONObject videoJsonObject = new JSONObject();
    JSONObject jsonObject = new JSONObject(playerResponseString);

    String titleVideo = jsonObject.getJSONObject("microformat").getJSONObject("playerMicroformatRenderer")
        .getJSONObject("title").getString("simpleText");
    System.out.println("titleVideo = " + titleVideo);
    String description = jsonObject.getJSONObject("microformat").getJSONObject("playerMicroformatRenderer")
        .getJSONObject("description").getString("simpleText");
    System.out.println("description = " + description.substring(0, Math.min(description.length(), 10)));
    parsingDescriptionInfo(description);

    JSONObject streamingData = jsonObject.getJSONObject("streamingData");
    JSONArray adaptiveFormats = streamingData.getJSONArray("adaptiveFormats");
    for (int i = 0; i < adaptiveFormats.length(); i++) {
      JSONObject localJsonObject = adaptiveFormats.getJSONObject(i);
      int itag = localJsonObject.getInt("itag");
      if (
          itag == 140
//          || itag == 141
//          || itag == 139
//          || itag == 249
//          || itag == 250
//          itag == 251
      ) {
        videoJsonObject = localJsonObject;
        System.out.println("itag = " + itag);
      }
    }
    long startDownload = System.currentTimeMillis();

    videoUrl = videoJsonObject.getString("url");
    long videoContentLength = videoJsonObject.getLong("contentLength");
    System.out.println("videoUrl = " + videoUrl);
    System.out.println(new Date().toString() + " " + "The start");

    long chuckSize = 10485760;
    ArrayList<Pair<Long, Long>> intervals = new ArrayList<>();
    for (long i = 0; i < videoContentLength / chuckSize; i++) {
      intervals.add(new Pair<>(i * chuckSize, i * chuckSize + chuckSize));
    }
    intervals.add(new Pair<>(chuckSize * intervals.size(), videoContentLength));

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    for (Pair<Long, Long> interval : intervals) {
      ByteArrayOutputStream tmpBuffer = downloadPart(videoUrl, interval.getKey(), interval.getValue());
      try {
        buffer.write(tmpBuffer.toByteArray());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    System.out.println(new Date().toString() + " " + "The end");
    long endDownload = System.currentTimeMillis();
    long diff = endDownload - startDownload;
    System.out.println("buffer.size() = " + buffer.size());
    System.out.println("second = " + diff);
    diff = Math.max(1, diff);
    int speed = (int) (buffer.size() / diff);
    System.out.println("speed = " + speed);

    writeBufferToFile(buffer, titleVideo);

  }

  private static String findPlayerResponse(String data) {

    String playerResponse = "player_response=";
    int firstPoint = data.indexOf(playerResponse);
    int secondPoint = data.indexOf("}&", firstPoint);
    if (secondPoint == -1) {
      secondPoint = data.length();
    } else {
      secondPoint += 1;
    }
    firstPoint += playerResponse.length();

    return data.substring(firstPoint, secondPoint);

  }

  private static ByteArrayOutputStream downloadPart(String videoUrl, long startSize, long endSize) {

    OkHttpClient client = new OkHttpClient.Builder().build();

    Request request = new Request.Builder()
        .url(videoUrl)
        //  .addHeader("Youtubedl-no-compression", "True")
        .addHeader("Range", "bytes=" + startSize + "-" + endSize)
        .addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7")
        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .addHeader("Accept-Encoding", "gzip, deflate")
        .addHeader("Accept-Language", "en-us,en;q=0.5")
        .addHeader("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3755.0 Safari/537.36")
        .build();

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      Response response = client.newCall(request).execute();
      //System.out.println("response.headers() = " + response.headers());
      InputStream inputStream = response.body().byteStream();
      byte[] tmp = new byte[1024 * 8];
      int size;
      while ((size = inputStream.read(tmp)) > 0) {
//            System.out.println("size = " + size);
        buffer.write(tmp, 0, size);
//        System.out.println(
//            "nRead = " + new Date().toString()
//                + " "
//                + "buffer.size() = " + buffer.size()
//                + " "
//                + (new String(Arrays.copyOf(tmp, Math.min(10, size)), StandardCharsets.US_ASCII)));
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("e = " + e);
    }

    return buffer;
  }

  private static void writeBufferToFile(ByteArrayOutputStream buffer, String titleVideo) {

    try (FileOutputStream fos = new FileOutputStream(titleVideo.substring(0, Math.min(10, titleVideo.length())) + ".mp4")) {
      fos.write(buffer.toByteArray());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void parsingDescriptionInfo(String description) {

    String[] lines = description.split("\n");
    ArrayList<Pair<Integer, String>> pairs = new ArrayList<>();

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
        while (Character.isDigit(line.charAt(secondPointNumber)) || line.charAt(secondPointNumber) == ':') {
          secondPointNumber++;
        }
        secondPointNumber++;

        String time = line.substring(firstPointNumber, secondPointNumber);
        time = time.trim();
        firstPoint = secondPointNumber;
        if (time.length() < 4) {
          continue;
        }
//        System.out.println("line = " + line);
        System.out.println(time);

        String clearTime = getAllBadCharacterFromString(time);

        String goodTime = setFullFormatTime(clearTime);

        String goodLine = line.substring(0, firstPointNumber) + line.substring(secondPointNumber);
        goodLine = goodLine.trim();
        System.out.println("goodLine = " + goodLine);

//        System.out.println("goodTime = " + goodTime);

        DateTime dateTime = DateTimeFormat.forPattern("HH:mm:ss").parseDateTime(goodTime);
        int seconds = dateTime.secondOfDay().get();
//        System.out.println("seconds = " + seconds);

        pairs.add(new Pair<>(seconds, goodLine));

      } while (firstPoint != -1);

      pairs.sort(Comparator.comparingInt(Pair::getKey));
    }
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

  public static String testDesc = "Unreal Tournament OST.\n"
      + "Composers: Alexander \"Siren\" Brandon, Michiel \"M.C.A.\" van den Bos, Andrew \"Necros\" Sega, and Dan \"Basehead\" Gardopée, Tero \"Teque\" Kostermaa, Kai-Eerik \"Nitro\" Komppa, Peter \"Skaven\" Hajba.\n"
      + "\n"
      + "Unreal :Tournament Menu [0:00]\n"
      + "Unreal Tournament Title [2:01]\n"
      + "BotMCA #10 [3:27]\n"
      + "Botpack #9 [7:50]\n"
      + "Cannonade [12:42]\n"
      + "Colossus [14:39]\n"
      + "Ending [18:59]\n"
      + "Enigma [20:49]\n"
      + "FireBreath [24:16]\n"
      + "Foregone Destruction [27:33]\n"
      + "Go Down [31:47]\n"
      + "Into The Darkness [34:50]\n"
      + "Lock [37:35]\n"
      + "Mechanism Eight [40:21]\n"
      + "Mission Landing [46:42]\n"
      + "Nether Animal [50:58]\n"
      + "Organic [56:00]\n"
      + "Phantom [59:05]\n"
      + "Razorback (Original Mix) [1:04:59]\n"
      + "Razorback (Unreal Mix) [1:09:02]\n"
      + "Room of Champions [1:13:48]\n"
      + "Run [1:16:05]\n"
      + "Save Me [1:20:37]\n"
      + "Save Me (G-Mix) [1:22:31]\n"
      + "Seeker [1:24:25]\n"
      + "Seeker 2 [1:26:18]\n"
      + "Skyward Fire [1:29:46]\n"
      + "Superfist [1:34:44]\n"
      + "The Course [1:36:44]\n"
      + "Three Wheels Turning [1:41:14]\n"
      + "Underworld 2 [1:43:41]";
}
