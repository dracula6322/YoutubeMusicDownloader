package com.green.square.youtubedownloader;

public class CommandArgumentsResult {

  public String pathToYoutubedl;
  public String outputFolderPath;
  public String linkId;
  public String ffmpegPath;

  public String getPathToYoutubedl() {
    return pathToYoutubedl;
  }

  public CommandArgumentsResult setPathToYoutubedl(String pathToYoutubedl) {
    this.pathToYoutubedl = pathToYoutubedl;
    return this;
  }

  public String getOutputFolderPath() {
    return outputFolderPath;
  }

  public CommandArgumentsResult setOutputFolderPath(String outputFolderPath) {
    this.outputFolderPath = outputFolderPath;
    return this;
  }

  public String getLinkId() {
    return linkId;
  }

  public CommandArgumentsResult setLinkId(String linkId) {
    this.linkId = linkId;
    return this;
  }

  public String getFfmpegPath() {
    return ffmpegPath;
  }

  public CommandArgumentsResult setFfmpegPath(String ffmpegPath) {
    this.ffmpegPath = ffmpegPath;
    return this;
  }

  public CommandArgumentsResult(String pathToYoutubedl, String outputFolderPath, String linkId, String ffmpegPath) {
    this.pathToYoutubedl = pathToYoutubedl;
    this.outputFolderPath = outputFolderPath;
    this.linkId = linkId;
    this.ffmpegPath = ffmpegPath;
  }

  @Override
  public String toString() {
    return "CommandArgumentsResult{" +
        "pathToYoutubedl='" + pathToYoutubedl + '\'' +
        ", outputFolderPath='" + outputFolderPath + '\'' +
        ", linkId='" + linkId + '\'' +
        ", ffmpeg='" + ffmpegPath + '\'' +
        '}';
  }
}
