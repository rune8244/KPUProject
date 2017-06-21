package com.eteks.homeview3d.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import com.eteks.homeview3d.model.DamagedHomeRecorderException;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeRecorder;
import com.eteks.homeview3d.model.InterruptedRecorderException;
import com.eteks.homeview3d.model.NotEnoughSpaceRecorderException;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;

public class HomeFileRecorder implements HomeRecorder {
  private final int             compressionLevel;
  private final boolean         includeOnlyTemporaryContent;
  private final UserPreferences preferences;
  private final boolean         preferPreferencesContent;
  private final boolean         preferXmlEntry;

  public HomeFileRecorder() {
    this(0);
  }


  public HomeFileRecorder(int compressionLevel) {
    this(compressionLevel, false);
  }


  public HomeFileRecorder(int     compressionLevel, 
                          boolean includeOnlyTemporaryContent) {
    this(compressionLevel, includeOnlyTemporaryContent, null, false);    
  }

  public HomeFileRecorder(int             compressionLevel, 
                          boolean         includeOnlyTemporaryContent,
                          UserPreferences preferences,
                          boolean         preferPreferencesContent) {
    this(compressionLevel, includeOnlyTemporaryContent, preferences, preferPreferencesContent, false);
  }


  public HomeFileRecorder(int             compressionLevel, 
                          boolean         includeOnlyTemporaryContent,
                          UserPreferences preferences,
                          boolean         preferPreferencesContent,
                          boolean         preferXmlEntry) {
    this.compressionLevel = compressionLevel;
    this.includeOnlyTemporaryContent = includeOnlyTemporaryContent;
    this.preferences = preferences;
    this.preferPreferencesContent = preferPreferencesContent;
    this.preferXmlEntry = preferXmlEntry;
  }


  public void writeHome(Home home, String name) throws RecorderException {
    File homeFile = new File(name);
    if (homeFile.exists()
        && !homeFile.canWrite()) {
      throw new RecorderException("Can't write over file " + name);
    }
    
    DefaultHomeOutputStream homeOut = null;
    File tempFile = null;
    try {
      tempFile = OperatingSystem.createTemporaryFile("save", ".homeview3d");
      homeOut = new DefaultHomeOutputStream(new FileOutputStream(tempFile), 
          this.compressionLevel, 
          this.includeOnlyTemporaryContent  
              ? ContentRecording.INCLUDE_TEMPORARY_CONTENT
              : ContentRecording.INCLUDE_ALL_CONTENT,
          true,
          this.preferXmlEntry 
              ? getHomeXMLExporter() 
              : null);
      homeOut.writeHome(home);
    } catch (InterruptedIOException ex) {
      throw new InterruptedRecorderException("Save " + name + " interrupted");
    } catch (IOException ex) {
      throw new RecorderException("Can't save home " + name, ex);
    } finally {
      try {
        if (homeOut != null) {
          homeOut.close();
        }
      } catch (IOException ex) {
        throw new RecorderException("Can't close temporary file " + name, ex);
      }
    }

    try {
      long usableSpace = (Long)File.class.getMethod("getUsableSpace").invoke(homeFile);
      long requiredSpace = tempFile.length();
      if (homeFile.exists()) {
        requiredSpace -= homeFile.length();
      }
      if (usableSpace != 0
          && usableSpace < requiredSpace) {
        throw new NotEnoughSpaceRecorderException("Not enough disk space to save file " + name, requiredSpace - usableSpace);
      }
    } catch (NoSuchMethodException ex) {
    } catch (NotEnoughSpaceRecorderException ex) {
      if (tempFile != null) {
        tempFile.delete();
      }
      throw ex;
    } catch (Exception ex) {
      ex.printStackTrace();
    }    
    
    OutputStream out;
    try {
      out = new FileOutputStream(homeFile);
    } catch (FileNotFoundException ex) {
      if (tempFile != null) {
        tempFile.delete();
      }
      throw new RecorderException("Can't save file " + name, ex);
    }
    
    byte [] buffer = new byte [8192];
    InputStream in = null;
    try {
      in = new FileInputStream(tempFile);          
      int size; 
      while ((size = in.read(buffer)) != -1) {
        out.write(buffer, 0, size);
      }
    } catch (IOException ex) { 
      throw new RecorderException("Can't copy file " + tempFile + " to " + name);
    } finally {
      try {
        if (out != null) {          
          out.close();
        }
      } catch (IOException ex) {
        throw new RecorderException("Can't close file " + name, ex);
      }
      try {
        if (in != null) {          
          in.close();
          tempFile.delete();
        }
      } catch (IOException ex) {
      }
    }
  }

  protected HomeXMLExporter getHomeXMLExporter() {
    return new HomeXMLExporter();
  }
  
  public Home readHome(String name) throws RecorderException {
    DefaultHomeInputStream in = null;
    try {
      in = new DefaultHomeInputStream(new FileInputStream(name), ContentRecording.INCLUDE_ALL_CONTENT,
          this.preferXmlEntry ? getHomeXMLHandler() : null, 
          this.preferences, this.preferPreferencesContent);
      Home home = in.readHome();
      return home;
    } catch (InterruptedIOException ex) {
      throw new InterruptedRecorderException("Read " + name + " interrupted");
    } catch (DamagedHomeIOException ex) {
      throw new DamagedHomeRecorderException(ex.getDamagedHome(), ex.getInvalidContent());
    } catch (IOException ex) {
      throw new RecorderException("Can't read home from " + name, ex);
    } catch (ClassNotFoundException ex) {
      throw new RecorderException("Missing classes to read home from " + name, ex);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {
        throw new RecorderException("Can't close file " + name, ex);
      }
    }
  }

  protected HomeXMLHandler getHomeXMLHandler() {
    return new HomeXMLHandler(this.preferences);
  }
  
  /**
   * Returns <code>true</code> if the file <code>name</code> exists.
   */
  public boolean exists(String name) throws RecorderException {
    return new File(name).exists();
  }
}
