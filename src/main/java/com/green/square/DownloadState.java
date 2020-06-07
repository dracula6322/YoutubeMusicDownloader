package com.green.square;

import java.util.ArrayList;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@Data
@Builder(toBuilder = true)
public class DownloadState {

  @Id
  String videoId;
  String videoLink;
  String videoTitle;
  String downloadedAudioFilePath;
  @ToString.Exclude
  String json;
  String audioFileName;
  String createdFolderPath;
  long durationInSeconds;
  ArrayList<CutValue> pairs;

}
