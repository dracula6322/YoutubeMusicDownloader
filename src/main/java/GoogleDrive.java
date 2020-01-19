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
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
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
    initGoogleDrive();
  }

  private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
  private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  private static final String CREDENTIALS_FILE_PATH = ".\\credentials.json";
  private static Drive service;

  public void initGoogleDrive() {
    JSON_FACTORY = JacksonFactory.getDefaultInstance(); // TODO
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
        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, Collections.singleton(DriveScopes.DRIVE_FILE))
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
  // 0AO1Vdj22FnXeUk9PVA
  public String getRootId(){
    try {
      return service.files().get("root").setFields("id").execute().getId();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }



  public List<File> getFolders(String parentId){
    try {
      List<File> result = new ArrayList<File>();
      Files.List request = service.files().list();



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

    } catch (IOException e){
      e.printStackTrace();
    }

    return null;
  }

  public void saveFileInGoogleDrive(String nameFolder, List<String> files) {

    String parentId = createFolder(nameFolder);

    for (String file : files) {
      saveFileInGoogleDrive(file, parentId);
    }
  }
}
