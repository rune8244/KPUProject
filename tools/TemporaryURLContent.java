package com.eteks.homeview3d.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import com.eteks.homeview3d.model.Content;

public class TemporaryURLContent extends URLContent {
  private static final long serialVersionUID = 1L;

  public TemporaryURLContent(URL temporaryUrl) {
    super(temporaryUrl);
  }

  public static TemporaryURLContent copyToTemporaryURLContent(Content content) throws IOException {
    String extension = ".tmp";
    if (content instanceof URLContent) {
      URLContent urlContent = (URLContent)content;
      String file = urlContent.isJAREntry() 
          ? urlContent.getJAREntryName()
          : urlContent.getURL().getFile();
      int lastIndex = file.lastIndexOf('.');
      if (lastIndex > 0) {
        extension = file.substring(lastIndex);
      }
    }
    File tempFile = OperatingSystem.createTemporaryFile("temp", extension);
    InputStream tempIn = null;
    OutputStream tempOut = null;
    try {
      tempIn = content.openStream();
      tempOut = new FileOutputStream(tempFile);
      byte [] buffer = new byte [8192];
      int size; 
      while ((size = tempIn.read(buffer)) != -1) {
        tempOut.write(buffer, 0, size);
      }
    } finally {
      if (tempIn != null) {
        tempIn.close();
      }
      if (tempOut != null) {
        tempOut.close();
      }
    }
    return new TemporaryURLContent(tempFile.toURI().toURL());
  }
}
