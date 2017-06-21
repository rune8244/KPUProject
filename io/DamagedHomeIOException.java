package com.eteks.homeview3d.io;

import java.io.IOException;
import java.util.List;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.Home;


public class DamagedHomeIOException extends IOException {
  private static final long serialVersionUID = 1L;
  
  private Home damagedHome;
  private List<Content> invalidContent;

  /**
   * 劳剂记 积己.
   */
  public DamagedHomeIOException(Home damagedHome,
                                List<Content> invalidContent) {
    super();
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
