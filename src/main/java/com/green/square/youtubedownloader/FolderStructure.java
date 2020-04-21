package com.green.square.youtubedownloader;

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
    return "com.green.square.youtubedownloader.FolderStructure{" +
        "isFolder=" + isFolder +
        ", id='" + id + '\'' +
        ", kind='" + kind + '\'' +
        ", name='" + name + '\'' +
        ", md5Checksum='" + md5Checksum + '\'' +
        ", parents='" + parents + '\'' +
        '}';
  }
}
