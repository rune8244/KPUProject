package com.eteks.homeview3d.applet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import com.eteks.homeview3d.io.ContentRecording;
import com.eteks.homeview3d.io.DefaultHomeInputStream;
import com.eteks.homeview3d.io.DefaultHomeOutputStream;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeRecorder;
import com.eteks.homeview3d.model.InterruptedRecorderException;
import com.eteks.homeview3d.model.RecorderException;


public class HomeAppletRecorder implements HomeRecorder {
  private final String           writeHomeURL;
  private final String           readHomeURL;
  private final String           listHomesURL;
  private final String           deleteHomeURL;
  private final ContentRecording contentRecording;
  private long                   availableHomesCacheTime;
  private String []              availableHomesCache;

  public HomeAppletRecorder(String writeHomeURL, 
                            String readHomeURL,
                            String listHomesURL) {
    this(writeHomeURL, readHomeURL, listHomesURL, true);
  }
  
  /**
   * 레코더 만들기
   */
  public HomeAppletRecorder(String writeHomeURL, 
                            String readHomeURL,
                            String listHomesURL,
                            boolean includeTemporaryContent) {
    this(writeHomeURL, readHomeURL, listHomesURL, 
        includeTemporaryContent 
            ? ContentRecording.INCLUDE_TEMPORARY_CONTENT
            : ContentRecording.INCLUDE_ALL_CONTENT);
  }
  
  public HomeAppletRecorder(String writeHomeURL, 
                            String readHomeURL,
                            String listHomesURL,
                            ContentRecording contentRecording) {
    this(writeHomeURL, readHomeURL, listHomesURL, null, contentRecording);
  }
  

  public HomeAppletRecorder(String writeHomeURL, 
                            String readHomeURL,
                            String listHomesURL,
                            String deleteHomeURL,
                            ContentRecording contentRecording) {
    this.writeHomeURL = writeHomeURL;
    this.readHomeURL = readHomeURL;
    this.listHomesURL = listHomesURL;
    this.deleteHomeURL = deleteHomeURL;
    this.contentRecording = contentRecording;
  }
  
  public void writeHome(Home home, String name) throws RecorderException {
    HttpURLConnection connection = null;
    try {
      // 시스템에서 서버 열기 
      connection = (HttpURLConnection)new URL(this.writeHomeURL).openConnection();
      connection.setRequestMethod("POST");
      String multiPartBoundary = "---------#@&$!d3emohteews!$&@#---------";
      connection.setRequestProperty("Content-Type", "multipart/form-data; charset=UTF-8; boundary=" + multiPartBoundary);
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setUseCaches(false);
      
      // 포스트 홈파트
      OutputStream out = connection.getOutputStream();
      out.write(("--" + multiPartBoundary + "\r\n").getBytes("UTF-8"));
      out.write(("Content-Disposition: form-data; name=\"home\"; filename=\"" 
          + name.replace('\"', '\'') + "\"\r\n").getBytes("UTF-8"));
      out.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes("UTF-8"));
      out.flush();
      DefaultHomeOutputStream homeOut = createHomeOutputStream(out);
      // 홈 쓰기
      homeOut.writeHome(home);
      homeOut.flush();
      
      out.write(("\r\n--" + multiPartBoundary + "--\r\n").getBytes("UTF-8"));
      out.close();
      
      // 리스폰스 읽기
      InputStream in = connection.getInputStream();
      int read = in.read();
      in.close();
      if (read != '1') {
        throw new RecorderException("Saving home " + name + " failed");
      }
      this.availableHomesCache = null; 
    } catch (InterruptedIOException ex) {
      throw new InterruptedRecorderException("Save " + name + " interrupted");
    } catch (IOException ex) {
      throw new RecorderException("Can't save home " + name, ex);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  /**
   * 필터로 돌아감
   */
  private DefaultHomeOutputStream createHomeOutputStream(OutputStream out) throws IOException {
    return new DefaultHomeOutputStream(out, 9, this.contentRecording, true, null);
  }

  public Home readHome(String name) throws RecorderException {
    URLConnection connection = null;
    DefaultHomeInputStream in = null;
    try {
      String readHomeURL = String.format(this.readHomeURL.replaceAll("(%[^s])", "%$1"), 
          URLEncoder.encode(name, "UTF-8"));
      connection = new URL(readHomeURL).openConnection();
      connection.setRequestProperty("Content-Type", "charset=UTF-8");
      connection.setUseCaches(false);
      in = createHomeInputStream(connection.getInputStream());
      Home home = in.readHome();
      return home;
    } catch (InterruptedIOException ex) {
      throw new InterruptedRecorderException("Read " + name + " interrupted");
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

  private DefaultHomeInputStream createHomeInputStream(InputStream in) throws IOException {
    return new DefaultHomeInputStream(in, this.contentRecording, null, null, false);
  }

  public boolean exists(String name) throws RecorderException {
    String [] availableHomes;    
    if (this.availableHomesCache != null 
        && this.availableHomesCacheTime + 100 > System.currentTimeMillis()) {
      availableHomes = this.availableHomesCache;
    } else {
      availableHomes = getAvailableHomes();
    }
    for (String home : availableHomes) {
      if (home.equals(name)) {
        return true;
      }
    }
    return false;
  }

  public String [] getAvailableHomes() throws RecorderException {
    URLConnection connection = null;
    InputStream in = null;
    try {
      connection = new URL(this.listHomesURL).openConnection();
      connection.setUseCaches(false);
      in = connection.getInputStream();
      String contentEncoding = connection.getContentEncoding();
      if (contentEncoding == null) {
        contentEncoding = "UTF-8";
      }
      Reader reader = new InputStreamReader(in, contentEncoding);
      StringWriter homes = new StringWriter();
      for (int c; (c = reader.read()) != -1; ) {
        homes.write(c);
      }
      String [] availableHomes = homes.toString().split("\n");
      if (availableHomes.length == 1 && availableHomes [0].length() == 0) {
        this.availableHomesCache = new String [0];
      } else {
        this.availableHomesCache = availableHomes;
      }
      this.availableHomesCacheTime = System.currentTimeMillis();
      return this.availableHomesCache;
    } catch (IOException ex) {
      throw new RecorderException("Can't read homes from server", ex);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {
        throw new RecorderException("Can't close connection", ex);
      }
    }
  }
  
  public void deleteHome(String name) throws RecorderException {
    if (!isHomeDeletionAvailable()) {
      throw new RecorderException("Deletion isn't available");
    }
    HttpURLConnection connection = null;
    try {
      // 시퀀스 대체
      String deletedHomeURL = String.format(this.deleteHomeURL.replaceAll("(%[^s])", "%$1"), 
          URLEncoder.encode(name, "UTF-8"));
      // 서버로 요청 보내기
      connection = (HttpURLConnection)new URL(deletedHomeURL).openConnection();
      connection.setRequestProperty("Content-Type", "charset=UTF-8");
      connection.setUseCaches(false);
      // 리스폰스 읽기
      InputStream in = connection.getInputStream();
      int read = in.read();
      in.close();
      if (read != '1') {
        throw new RecorderException("Deleting home " + name + " failed");
      }
      this.availableHomesCache = null; 
    } catch (InterruptedIOException ex) {
      throw new InterruptedRecorderException("Delete " + name + " interrupted");
    } catch (IOException ex) {
      throw new RecorderException("Can't delete home " + name, ex);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
  

  public boolean isHomeDeletionAvailable() {
    return this.deleteHomeURL != null;
  }

  public long getHomeLength(Home home) throws RecorderException {
    try {
      LengthOutputStream out = new LengthOutputStream();
      DefaultHomeOutputStream homeOut = createHomeOutputStream(out);
      homeOut.writeHome(home);
      homeOut.flush();
      return out.getLength();
    } catch (InterruptedIOException ex) {
      throw new InterruptedRecorderException("Home length computing interrupted");
    } catch (IOException ex) {
      throw new RecorderException("Can't compute home length", ex);
    }
  }
  
  private class LengthOutputStream extends OutputStream {
    private long length;
    
    @Override
    public void write(int b) throws IOException {
      this.length++;
    }
    
    public long getLength() {
      return this.length;
    }
  }
}
