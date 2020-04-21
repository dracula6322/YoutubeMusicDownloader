package com.green.square;

import lombok.Data;
import lombok.NonNull;

@Data
public class CutValue {

  @NonNull
  String title;
  @NonNull
  String startTime;
  @NonNull
  String endTime;
  @NonNull
  long startTimeInSecond;
  @NonNull
  long endTimeInSecond;

}
