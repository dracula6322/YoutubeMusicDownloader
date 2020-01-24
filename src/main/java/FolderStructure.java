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
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FolderStructure {

  boolean isFolder;
  String id;
  String kind;
  String name;
  String md5Checksum;
  List<String> parents;

  public FolderStructure(boolean isFolder, String id, String kind, String name, String md5Checksum,
      List<String> parents) {
    this.isFolder = isFolder;
    this.id = id;
    this.kind = kind;
    this.name = name;
    this.md5Checksum = md5Checksum;
    this.parents = parents;
  }

  public boolean isFolder() {
    return isFolder;
  }

  public void setFolder(boolean folder) {
    isFolder = folder;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMd5Checksum() {
    return md5Checksum;
  }

  public void setMd5Checksum(String md5Checksum) {
    this.md5Checksum = md5Checksum;
  }

  public List<String> getParents() {
    return parents;
  }

  public void setParents(List<String> path) {
    this.parents = path;
  }

  @Override
  public String toString() {
    return "FolderStructure{" +
        "isFolder=" + isFolder +
        ", id='" + id + '\'' +
        ", kind='" + kind + '\'' +
        ", name='" + name + '\'' +
        ", md5Checksum='" + md5Checksum + '\'' +
        ", parents='" + parents + '\'' +
        '}';
  }
}
