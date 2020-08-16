package com.green.square.model;

import java.nio.file.Path;
import java.util.List;
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
  String audioFileNameFromJson;
  List<Path> trimmedFiles;
  long durationInSeconds;
  List<VideoInterval> cutValues;

}
