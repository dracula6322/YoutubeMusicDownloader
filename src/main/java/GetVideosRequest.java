import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class GetVideosRequest {

  private static GetVideosRequest ourInstance = new GetVideosRequest();

  public static GetVideosRequest getInstance() {
    return ourInstance;
  }

  public GetVideosRequest() {
  }

  public String sendPing(String url) {

    String videoId = getIdFromUrl(url);
    StringBuilder result = new StringBuilder();
    String apiUrl = String
        .format("https://www.youtube.com/get_video_info?video_id=%s&el=embedded&ps=default&eurl=&gl=US&hl=en", videoId);

    try {
      URL yahoo = new URL(apiUrl);
      URLConnection yc = yahoo.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(), StandardCharsets.UTF_8));
      String inputLine;

      while ((inputLine = in.readLine()) != null) {
        result.append(inputLine);
      }
      in.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.toString());
    }

    return result.toString();
  }

  private String getIdFromUrl(String url) {

    String id = "";

    int firstPoint = url.indexOf("?");
    String params = url.substring(firstPoint + 1);

    String[] paramsArray = params.split("&");
    for (String s : paramsArray) {
      if (s.startsWith("v=")) {
        id = s.split("=")[1];
      }
    }

    return id;
  }


}
