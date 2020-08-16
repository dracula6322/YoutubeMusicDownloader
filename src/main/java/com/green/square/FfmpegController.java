package com.green.square;

import lombok.Builder;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Component;

@Component
public class FfmpegController {

  @Data
  @Builder(toBuilder = true)
  public static class ParsingResult {

    String duration;
    long durationInSeconds;
    String currentDuration;
    long currentDurationInSeconds;
    double percent;
  }

  protected boolean isStringWithProgress(String progressString) {

    int sizeIndex = progressString.indexOf("size");
    int timeIndex = progressString.indexOf("time");
    int bitrateIndex = progressString.indexOf("bitrate");
    int speedIndex = progressString.indexOf("speed");
    return speedIndex > bitrateIndex && bitrateIndex > timeIndex && timeIndex > sizeIndex && sizeIndex >= 0;
  }

  protected long parsingDurationToSeconds(String resultDuration) {

    org.joda.time.format.DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm:ss.SS");
    fmt = fmt.withZoneUTC();
    DateTime dateTime = fmt.parseDateTime(resultDuration);
    return dateTime.getMillis();
  }

  protected String tryGetDurationFromString(String currentString) {

    String[] split = currentString.split(",");
    if (split.length == 0) {
      return "";
    }

    for (String s : split) {
      if (s.contains("Duration:")) {
        String[] split1 = s.strip().split("Duration:");
        for (String s1 : split1) {
          if (s1.isBlank() || s1.isEmpty()) {
            continue;
          }
          return s1.strip();
        }
      }
    }
    return "";
  }


}
