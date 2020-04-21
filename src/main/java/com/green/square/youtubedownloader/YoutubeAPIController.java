package com.green.square.youtubedownloader;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import com.google.api.services.youtube.model.ThumbnailSetResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.json.JSONArray;
import org.json.JSONObject;

public class YoutubeAPIController {

  private static YoutubeAPIController ourInstance = new YoutubeAPIController();

  public static YoutubeAPIController getInstance() {
    return ourInstance;
  }

  public YoutubeAPIController() {
  }

  private static final String DEVELOPER_KEY = "AIzaSyDdsYhIbGJlsHX5xdIElhZtcwq8p-2ghaE";
  private static final String APPLICATION_NAME = "API code samples";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  public List<String> getComments(String idVideo) {

    try {
      String response = getCommentThreads(idVideo);
      return parsingGetCommentThreadsResponse(response);
    } catch (GeneralSecurityException | IOException e) {
      e.printStackTrace();
    }

    return Collections.emptyList();
  }

  private YouTube getService() throws GeneralSecurityException, IOException {
    final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    return new YouTube.Builder(httpTransport, JSON_FACTORY, null)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  private String getCommentThreads(String idVideo) throws GeneralSecurityException, IOException {
    YouTube youtubeService = getService();
    // Define and execute the API request
    YouTube.CommentThreads.List request = youtubeService.commentThreads()
        .list("snippet,replies");
    try {
      CommentThreadListResponse response = request.setKey(DEVELOPER_KEY)
          .setOrder("relevance")
          .setVideoId(idVideo)
          .execute();
      return response.toString();
    } catch (GoogleJsonResponseException e) {
      System.err.println(e.toString());
      return "{}";
    }
  }

  public String getImages(String idVideo) throws GeneralSecurityException, IOException {
    YouTube youtubeService = getService();

    try {

      File mediaFile = new File("YOUR_FILE");
      mediaFile.createNewFile();
      InputStreamContent mediaContent =
          new InputStreamContent("image/png",
              new BufferedInputStream(new FileInputStream(mediaFile)));
      mediaContent.setLength(mediaFile.length());

      // Define and execute the API request
      YouTube.Thumbnails.Set request = youtubeService.thumbnails()
          .set(idVideo, mediaContent);
      ThumbnailSetResponse response = request.execute();
      //response.soutv
      return response.toString();
    } catch (GoogleJsonResponseException e) {
      System.err.println(e.toString());
      return "{}";
    }

//    YouTube.CommentThreads.List request = youtubeService.commentThreads()
//        .list("snippet,replies");
//    try {
//      CommentThreadListResponse response = request.setKey(DEVELOPER_KEY)
//          .setOrder("relevance")
//          .setVideoId(idVideo)
//          .execute();
//      return response.toString();
//    } catch (GoogleJsonResponseException e) {
//      System.err.println(e.toString());
//      return "{}";
//    }
  }

  private List<String> parsingGetCommentThreadsResponse(@Nonnull String response) {

    List<String> results = new ArrayList<>();

    JSONObject jsonResponse = new JSONObject(response);
    if (!jsonResponse.has("items")) {
      return results;
    }

    JSONArray items = jsonResponse.getJSONArray("items");
    for (int i = 0; i < items.length(); i++) {
      JSONObject snippet = items.getJSONObject(i).getJSONObject("snippet");
      JSONObject topLevelComment = snippet.getJSONObject("topLevelComment");
      snippet = topLevelComment.getJSONObject("snippet");
      String textDisplay = snippet.getString("textOriginal");
      results.add(textDisplay);

    }
    return results;

  }

}
