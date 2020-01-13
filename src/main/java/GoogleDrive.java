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
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleDrive {

  private static GoogleDrive ourInstance = new GoogleDrive();

  public static GoogleDrive getInstance() {
    return ourInstance;
  }

  public GoogleDrive() {
  }

  private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
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
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, DriveScopes.all())
        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  public void saveFileInGoogleDrive(String pathToFile, String parentId) {

    com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
    java.io.File filePath = new java.io.File(pathToFile);
    fileMetadata.setName(filePath.getName());
    fileMetadata.setParents(Collections.singletonList(parentId));
    FileContent mediaContent = new FileContent("audio/mp4", filePath);

    try {
      File file = service.files().create(fileMetadata, mediaContent)
          .setFields("id")
          .execute();
      System.out.println("File ID: " + file.getId());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String createFolder(String name){
    File fileMetadata = new File();
    fileMetadata.setName(name);
    fileMetadata.setParents(Collections.singletonList("1QyDSiM6M5VKB0tDOHaJeM6OoWPzsSngg"));
    fileMetadata.setMimeType("application/vnd.google-apps.folder");

    try {
      File file = service.files().create(fileMetadata)
          .setFields("id")
          .execute();

      return file.getId();
    } catch (IOException e) {
      e.printStackTrace();
    }

    throw new RuntimeException();
  }


  public void saveFileInGoogleDrive(String nameFolder, List<String> files) {

    String parentId = createFolder(nameFolder);

    for (String file : files) {
      saveFileInGoogleDrive(file, parentId);
    }


  }
}
