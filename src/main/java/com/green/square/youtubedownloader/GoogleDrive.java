package com.green.square.youtubedownloader;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoogleDrive {


  public static class GoogleDriveHolder {

    public static final GoogleDrive HOLDER_INSTANCE = new GoogleDrive();
  }

  public static GoogleDrive getInstance() {
    return GoogleDriveHolder.HOLDER_INSTANCE;
  }

  public GoogleDrive() {
    initGoogleDrive();
  }

  private static final String APPLICATION_NAME = "Youtube music downloader";
  private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  private static final String CREDENTIALS_FILE_PATH = ".\\credentials.json";
  private static Drive service;

  public void initGoogleDrive() {
    try {
      NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
          .setApplicationName(APPLICATION_NAME)
          .build();

    } catch (GeneralSecurityException | IOException e) {
      e.printStackTrace();
    }
  }

  public Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    // Load client secrets.
    InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
    if (in == null) {
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
    }
    InputStreamReader credentialsInputStreamReader = new InputStreamReader(in);
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, credentialsInputStreamReader);

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, DriveScopes.all())
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  public void deleteFileById(String id) {
    // Load client secrets.

    try {
      service.files().delete(id);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static ArrayList<FolderStructure> getFilesById(String id) {

    GoogleDrive.getInstance().initGoogleDrive();

    String folderId = id;
    if (id == null || id.equals("")) {
      folderId = GoogleDrive.getInstance().getRootId();
    }

    System.out.println("folderId = " + folderId);

    List<com.google.api.services.drive.model.File> folderStructure = GoogleDrive.getInstance()
        .getFilesByParentId(folderId);
    System.out.println("result = " + folderStructure);

    ArrayList<FolderStructure> result = new ArrayList<>();

    for (com.google.api.services.drive.model.File file : folderStructure) {

      result.add(new FolderStructure(
          file.getMimeType().endsWith(".folder"),
          file.getId(),
          file.getKind(),
          file.getName(),
          file.getMd5Checksum(),
          Collections.singletonList(folderId)
      ));
    }

    return result;
  }


  public static Pair<Boolean, List<FolderStructure>> getFilesByFolderName(String folderNameToFile, String parentId) {

    ArrayList<FolderStructure> currentFolderStructure = getFilesById(parentId);
    ArrayList<FolderStructure> result = new ArrayList<>();

    for (FolderStructure folderStructure : currentFolderStructure) {
      if (folderStructure.getName().equals(folderNameToFile)) {
        result.add(folderStructure);
      }
    }
    return new Pair<>(!result.isEmpty(), result);
  }


  public void saveFileInGoogleDrive(String pathToFile, String parentId) {
    System.out.printf("We in saveFileInGoogleDrive %s", pathToFile);
    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
    java.io.File filePath = new java.io.File(pathToFile);
    fileMetadata.setName(filePath.getName());
    fileMetadata.setParents(Collections.singletonList(parentId));
    FileContent mediaContent = new FileContent("audio/mp4", filePath);

    try {

      System.out.printf("We start upload file %s", pathToFile);
      InputStream inputStream = service.files().create(fileMetadata, mediaContent)
          .setFields("id")
          .executeAsInputStream();

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(inputStream));

      String line;
      while ((line = stdInput.readLine()) != null) {
        System.out.println(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e);
    }
    fileMetadata.clear();
  }


  public String createFolder(List<String> name, String folderName) {

    String parentId = getRootId();

    if (folderName == null || folderName.isEmpty()) {
      return parentId;
    }
    Pair<Boolean, List<FolderStructure>> files;
    for (int i = 0; i < name.size(); i++) {
      String subFolderName = name.get(i);
      if (subFolderName == null || subFolderName.isEmpty()) {
        continue;
      }
      files = getFilesByFolderName(subFolderName, parentId);

      if (files.first) {
        //createFolderWithParents(subList.get(subList.size() - 1), files.getValue().get(0).parents);
        parentId = files.second.get(0).getId();
      } else {
        files = createFolderWithParents(subFolderName, parentId);
        parentId = files.second.get(0).getId();
      }
    }
    files = getFilesByFolderName(folderName, parentId);
    if (files.first) {
      parentId = files.second.get(0).getId();
    } else {
      parentId = createFolderWithParents(folderName, parentId).second.get(0).getId();
    }
    return parentId;

  }

  public Pair<Boolean, List<FolderStructure>> createFolderWithParents(String name, String parent) {

    File fileMetadata = new File();
    fileMetadata.setName(name);
    if (parent != null) {
      fileMetadata.setParents(Collections.singletonList(parent));
    }
    fileMetadata.setMimeType("application/vnd.google-apps.folder");

    try {
      File file = service.files().create(fileMetadata)
          .setFields("id, mimeType, name, parents")
          .execute();

      FolderStructure folderStructure = new FolderStructure(true,
          file.getId(),
          file.getMimeType(),
          file.getName(),
          file.getMd5Checksum(),
          file.getParents());
      fileMetadata.clear();
      return new Pair<>(true, Arrays.asList(folderStructure));
    } catch (IOException e) {
      e.printStackTrace();
    }

    throw new RuntimeException();
  }

  // 0AO1Vdj22FnXeUk9PVA
  public String getRootId() {
    try {
      Get qwe = service.files()
          .get("root")
          .setFields("id, name");
      File file = qwe.execute();

      return file.getId();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }


  public List<File> getFilesByParentId(String parentId) {
    try {
      List<File> result = new ArrayList<File>();
      Files.List request = service
          .files()
          .list()
          .setQ(String.format("'%s' in parents and trashed=false", parentId));

      do {
        try {
          FileList files = request.execute();
          result.addAll(files.getFiles());
          request.setPageToken(files.getNextPageToken());
        } catch (IOException e) {
          System.out.println("An error occurred: " + e);
          request.setPageToken(null);
        }
      } while (request.getPageToken() != null &&
          request.getPageToken().length() > 0);

      return result;

    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  public void saveFileInGoogleDrive(List<String> pathNamesToFile, String folderName, List<String> files) {

    String parentId = createFolder(pathNamesToFile, folderName);

    for (String file : files) {
      saveFileInGoogleDrive(file, parentId);
    }
  }
}
