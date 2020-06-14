package com.green.square;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class FileManagerController {

  private static FileManagerController ourInstance = new FileManagerController();

  public static FileManagerController getInstance() {
    return ourInstance;
  }

  public FileManagerController() {
  }

  public String getHashNameFile(String fileName) {
    return DigestUtils.md5DigestAsHex((fileName).getBytes(StandardCharsets.UTF_8));
  }


  public List<Path> getFilesByPath(String path) {

    List<Path> result = new ArrayList<>();
    try {
      return Files.list(Paths.get(path)).collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  public JSONObject getFilesByPathInJson(String path) {

    JSONArray foldersArray = new JSONArray();
    getFilesByPath(path).forEach(new Consumer<Path>() {
      @Override
      public void accept(Path path) {
        foldersArray.put(path.getFileName());
      }
    });
    JSONObject result = new JSONObject();
    result.put("folders", foldersArray);
    return result;
  }

  public JSONObject getFilesByPathInJsonWithBase64(String path) {

    List<Path> files = getFilesByPath(path);
    JSONArray filesJsonArrays = new JSONArray();

    for (Path file : files) {
      JSONObject item = new JSONObject();
      item.put("name", file.getFileName());
      item.put("base64", getHashNameFile(file.getFileName().toString()));
      filesJsonArrays.put(item);
    }
    JSONObject result = new JSONObject();
    result.put("files", filesJsonArrays);
    return result;

  }

}
