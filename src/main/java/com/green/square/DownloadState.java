package com.green.square;

import java.util.ArrayList;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@Builder(toBuilder = true)
public class DownloadState {

  @Id
  String videoId;
  String videoLink;
  String json;
  String audioFileName;
  String createdFolderPath;
  String duration;
  ArrayList<CutValue> pairs;

}
