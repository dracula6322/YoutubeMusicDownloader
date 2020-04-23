package com.green.square.youtubedownloader;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class CommandArgumentsResult {

  public String pathToYoutubedl;
  public String outputFolderPath;
  public String linkId;
  public String ffmpegPath;
}
