package com.green.square.model;

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
