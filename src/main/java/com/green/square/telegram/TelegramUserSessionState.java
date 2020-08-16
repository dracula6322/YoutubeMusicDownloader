package com.green.square.telegram;

import com.green.square.model.DownloadState;
import com.green.square.model.VideoInterval;
import java.util.Map;

public class TelegramUserSessionState {

  State currentState;

  public TelegramUserSessionState() {
    this.currentState = new EmptyState();
  }


  public void setIntervals(Map<Long, VideoInterval> integerPathMapNewVersion) {
    getCurrentState().setIntervals(integerPathMapNewVersion);

  }

  private State getCurrentState() {
    return currentState;
  }

  private void setCurrentState(State currentState) {
    this.currentState = currentState;
  }

  private abstract class State {

    private String youtubeUrl;
    private long userId;
    private final long longIncrement = 0;
    public Map<Long, VideoInterval> intervals;
    private DownloadState currentDownloadState;

    public void setIntervals(Map<Long, VideoInterval> currentState) {
    }
  }

  private class EmptyState extends State {

  }

  private class DefiningIntervalState extends State {

    public DefiningIntervalState() {
    }

    @Override
    public void setIntervals(Map<Long, VideoInterval> currentState) {
      setCurrentState(new DefinedIntervalState(currentState));
    }
  }

  private class DefinedIntervalState extends State {

    public DefinedIntervalState(Map<Long, VideoInterval> intervals) {
      this.intervals = intervals;
    }
  }

}
