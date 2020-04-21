package com.green.square;

public class RestFileSystemConfiguration {

  private String outFolder;
  private String pathToYoutubedl;
  private String ffmpegPath;

  public RestFileSystemConfiguration(String outFolder, String pathToYoutubedl, String ffmpegPath) {
    this.outFolder = outFolder;
    this.pathToYoutubedl = pathToYoutubedl;
    this.ffmpegPath = ffmpegPath;
  }

  public String getOutFolder() {
    return outFolder;
  }

  public RestFileSystemConfiguration setOutFolder(String outFolder) {
    this.outFolder = outFolder;
    return this;
  }

  public String getPathToYoutubedl() {
    return pathToYoutubedl;
  }

  public RestFileSystemConfiguration setPathToYoutubedl(String pathToYoutubedl) {
    this.pathToYoutubedl = pathToYoutubedl;
    return this;
  }

  public String getFfmpegPath() {
    return ffmpegPath;
  }

  public RestFileSystemConfiguration setFfmpegPath(String ffmpegPath) {
    this.ffmpegPath = ffmpegPath;
    return this;
  }

  @Override
  public String toString() {
    return "RestFileSystemConfiguration{" +
        "outFolder='" + outFolder + '\'' +
        ", pathToYoutubedl='" + pathToYoutubedl + '\'' +
        ", ffmpegPath='" + ffmpegPath + '\'' +
        '}';
  }
}
