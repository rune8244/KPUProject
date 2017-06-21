package com.eteks.homeview3d.model;

import java.util.List;

public class DamagedHomeRecorderException extends RecorderException {
  private static final long serialVersionUID = 1L;
  
  private Home damagedHome;
  private List<Content> invalidContent;

  public DamagedHomeRecorderException(Home damagedHome,
                                      List<Content> invalidContent) {
    super();
    this.damagedHome = damagedHome;
    this.invalidContent = invalidContent;
  }

  public DamagedHomeRecorderException(Home damagedHome,
                                      List<Content> invalidContent,
                                      String message) {
    super(message);
    this.damagedHome = damagedHome;
    this.invalidContent = invalidContent;
  }

  public Home getDamagedHome() {
    return this.damagedHome;
  }

  public List<Content> getInvalidContent() {
    return this.invalidContent;
  }
}
