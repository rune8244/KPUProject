package com.eteks.homeview3d.model;

public class NotEnoughSpaceRecorderException extends RecorderException {
  private long missingSpace;

  public NotEnoughSpaceRecorderException(String message, long missingSpace) {
    super(message);
    this.missingSpace = missingSpace;

  public long getMissingSpace() {
    return this.missingSpace;
  }
}
