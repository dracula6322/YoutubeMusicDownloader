package com.green.square.youtubedownloader;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.CommentThreadListResponse;
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

  public List<String> getComments(String idVideo){

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
    CommentThreadListResponse response = request.setKey(DEVELOPER_KEY)
        .setOrder("relevance")
        .setVideoId(idVideo)
        .execute();
    return response.toString();
  }

  private List<String> parsingGetCommentThreadsResponse(@Nonnull String response) {

    List<String> results = new ArrayList<>();

    JSONObject jsonResponse = new JSONObject(response);
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
