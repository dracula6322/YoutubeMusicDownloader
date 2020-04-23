package com.green.square;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@AllArgsConstructor
@Data
@Builder(toBuilder = true)
public class CutValue {

  @NonNull
  String title;
  @NonNull
  String startTime;
  @NonNull
  String endTime;
  long startTimeInSecond;
  long endTimeInSecond;

  String hashName = "";

}
