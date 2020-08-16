package com.green.square;

import com.green.square.FfmpegController.ParsingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FfmpegParsingState {

  FfmpegController ffmpegController;
  ParsingResult parsingResult = ParsingResult.builder().build();

  @Autowired
  public FfmpegParsingState(FfmpegController ffmpegController) {
    this.ffmpegController = ffmpegController;
  }

  public FfmpegParsingState() {
    this.ffmpegController = new FfmpegController();
  }

  public ParsingResult processString(String s) {
    if (parsingResult.duration == null) {
      String resultDuration = ffmpegController.tryGetDurationFromString(s);
      if (!resultDuration.isEmpty()) {
        parsingResult = parsingResult.toBuilder().duration(resultDuration).build();
        long durationToSeconds = ffmpegController.parsingDurationToSeconds(resultDuration);
        parsingResult = parsingResult.toBuilder().durationInSeconds(durationToSeconds).build();
      }
    }

    if (ffmpegController.isStringWithProgress(s)) {

      String strTime = "time=";
      int indexOfTime = s.indexOf(strTime);

      String srtBitrate = "bitrate";
      int indexOfBitrate = s.indexOf(srtBitrate);

      String timeSubString = s.substring(indexOfTime, indexOfBitrate).strip();
      String time = timeSubString.split("=")[1].strip();
      parsingResult = parsingResult.toBuilder().currentDuration(time).build();
      long currentDurationToSeconds = ffmpegController.parsingDurationToSeconds(time);
      parsingResult = parsingResult.toBuilder().currentDurationInSeconds(currentDurationToSeconds).build();
      parsingResult = parsingResult.toBuilder()
          .percent((double) (currentDurationToSeconds) / (double) (parsingResult.durationInSeconds)).build();
    }

    System.out.println(parsingResult);
    return parsingResult;
  }

  public void clean() {
    parsingResult = ParsingResult.builder().build();
  }

}
