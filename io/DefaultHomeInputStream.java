package com.eteks.homeview3d.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.URLContent;

public class DefaultHomeInputStream extends FilterInputStream {
  private final ContentRecording   contentRecording;
  private final HomeXMLHandler     xmlHandler;
  private final UserPreferences    preferences;
  private final boolean            preferPreferencesContent;
  
  private File file;

  public DefaultHomeInputStream(InputStream in) throws IOException {
    this(in, ContentRecording.INCLUDE_ALL_CONTENT);
  }

  public DefaultHomeInputStream(InputStream in, 
                                ContentRecording contentRecording) throws IOException {
    this(in, contentRecording, null, false);
  }

  public DefaultHomeInputStream(InputStream in, 
                                ContentRecording contentRecording, 
                                UserPreferences preferences,
                                boolean preferPreferencesContent) {
    this(in, contentRecording, null, preferences, preferPreferencesContent);
  }

  public DefaultHomeInputStream(InputStream in, 
                                ContentRecording contentRecording,
                                HomeXMLHandler xmlHandler,
                                UserPreferences preferences,
                                boolean preferPreferencesContent) {
    super(in);
    this.contentRecording = contentRecording;
    this.xmlHandler = xmlHandler;
    this.preferences = preferences;
    this.preferPreferencesContent = preferPreferencesContent;
  }


  public DefaultHomeInputStream(File file, 
                                ContentRecording contentRecording,
                                HomeXMLHandler xmlHandler,
                                UserPreferences preferences,
                                boolean preferPreferencesContent) throws FileNotFoundException {
    super(new FileInputStream(file));
    this.file = file;
    this.contentRecording = contentRecording;
    this.xmlHandler = xmlHandler;
    this.preferences = preferences;
    this.preferPreferencesContent = preferPreferencesContent;
  }

  private static void checkCurrentThreadIsntInterrupted() throws InterruptedIOException {
    if (Thread.interrupted()) {
      throw new InterruptedIOException();
    }
  }
  
  public Home readHome() throws IOException, ClassNotFoundException {
    boolean validZipFile = true;
    HomeContentContext contentContext = null;
    if (this.contentRecording != ContentRecording.INCLUDE_NO_CONTENT) {
      InputStream homeIn;
      if (this.file == null) {
        this.file = OperatingSystem.createTemporaryFile("open", ".homeview3d");
        OutputStream fileCopyOut = new BufferedOutputStream(new FileOutputStream(this.file));
        homeIn = new CopiedInputStream(new BufferedInputStream(this.in), fileCopyOut);
      } else {
        homeIn = this.in;
      }
      List<ZipEntry> validEntries = new ArrayList<ZipEntry>();
      validZipFile = isZipFileValidUsingInputStream(homeIn, validEntries) && validEntries.size() > 0;
      if (!validZipFile) {
        int validEntriesCount = validEntries.size();
        validEntries.clear();
        isZipFileValidUsingDictionnary(this.file, validEntries);
        if (validEntries.size() > validEntriesCount) {
          this.file = createTemporaryFileFromValidEntries(this.file, validEntries);
        } else {
          this.file = createTemporaryFileFromValidEntriesCount(this.file, validEntriesCount);
        }
      } 
      contentContext = new HomeContentContext(this.file.toURI().toURL(),
          this.preferences, this.preferPreferencesContent);
    }
    
    boolean homeEntry = false;
    boolean homeXmlEntry = false;
    ZipInputStream zipIn = new ZipInputStream(this.contentRecording == ContentRecording.INCLUDE_NO_CONTENT
        ? this.in : new FileInputStream(this.file));
    ZipEntry entry = null;
    try {
      while ((entry = zipIn.getNextEntry()) != null) {
        if ("Home".equals(entry.getName())) {
          homeEntry = true;
        } else if (this.xmlHandler != null 
                  && "Home.xml".equals(entry.getName())) {
          homeXmlEntry = true;
        }
        
        if (this.contentRecording == ContentRecording.INCLUDE_NO_CONTENT) {
          if (homeEntry || homeXmlEntry) {
            break;
          }
        } else {
          if (homeEntry && homeXmlEntry) {
            break;
          }
        }
      }
      
      checkCurrentThreadIsntInterrupted();
      if (!homeEntry && !homeXmlEntry) {
        throw new IOException("Missing entry \"Home\" or \"Home.xml\"");
      }

      if (this.contentRecording != ContentRecording.INCLUDE_NO_CONTENT) {
        zipIn.close();
        zipIn = new ZipInputStream(new FileInputStream(this.file));
        do {
          entry = zipIn.getNextEntry();
        } while (homeXmlEntry && !"Home.xml".equals(entry.getName()) 
            || !homeXmlEntry && !"Home".equals(entry.getName()));
      }
      
      checkCurrentThreadIsntInterrupted();
      Home home;
      if ("Home".equals(entry.getName())) {
        HomeObjectInputStream objectStream = new HomeObjectInputStream(zipIn, contentContext);
        home = (Home)objectStream.readObject();
      } else {
        try {
          SAXParserFactory factory = SAXParserFactory.newInstance();
          SAXParser saxParser = factory.newSAXParser();
          this.xmlHandler.setContentContext(contentContext);
          saxParser.parse(zipIn, this.xmlHandler);
          home = this.xmlHandler.getHome();
        } catch (ParserConfigurationException ex) {
          IOException ex2 = new IOException("Can't parse home XML stream");
          ex2.initCause(ex);
          throw ex2;
        } catch (SAXException ex) {
          IOException ex2 = new IOException("Can't parse home XML stream");
          ex2.initCause(ex);
          throw ex2;
        }
      }
      if (contentContext != null && (!validZipFile || contentContext.containsInvalidContents())) {
        if (contentContext.containsCheckedContents()) { 
          home.setRepaired(true);
        } else {
          throw new DamagedHomeIOException(home, contentContext.getInvalidContents());
        }
      }
      return home;
    } finally {
      if (zipIn != null) {
        zipIn.close();
      }
    }
  }

  private boolean isZipFileValidUsingInputStream(InputStream in, List<ZipEntry> validEntries) throws IOException {
    ZipInputStream zipIn = null;
    try {
      zipIn = new ZipInputStream(in);
      byte [] buffer = new byte [8192];
      for (ZipEntry zipEntry = null; (zipEntry = zipIn.getNextEntry()) != null; ) {
        while (zipIn.read(buffer) != -1) {
        }
        validEntries.add(zipEntry);
        checkCurrentThreadIsntInterrupted();
      }
      return true;
    } catch (IOException ex) {
      return false;
    } finally {
      if (zipIn != null) {
        zipIn.close();
      }
    }
  }
  
  private boolean isZipFileValidUsingDictionnary(File file, List<ZipEntry> validEntries) throws IOException {
    ZipFile zipFile = null;
    boolean validZipFile = true;
    try {
      zipFile = new ZipFile(file);
      for (Enumeration<? extends ZipEntry> enumEntries = zipFile.entries(); enumEntries.hasMoreElements(); ) {
        try {
          ZipEntry zipEntry = enumEntries.nextElement();
          InputStream zipIn = zipFile.getInputStream(zipEntry);
          byte [] buffer = new byte [8192];
          while (zipIn.read(buffer) != -1) {
          }
          zipIn.close();
          validEntries.add(zipEntry);
          checkCurrentThreadIsntInterrupted();
        } catch (IOException ex) {
          validZipFile = false;
        } 
      }
    } catch (Exception ex) {
      validZipFile = false;
    } finally {
      if (zipFile != null) {
        zipFile.close();
      }
    }
    return validZipFile;
  }

  private File createTemporaryFileFromValidEntriesCount(File file, int entriesCount) throws IOException {
    if (entriesCount <= 0) {
      throw new IOException("No valid entries");
    }
    File tempfile = OperatingSystem.createTemporaryFile("part", ".sh3d");
    ZipOutputStream zipOut = null;
    ZipInputStream zipIn = null;
    try {
      zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
      zipOut = new ZipOutputStream(new FileOutputStream(tempfile));
      zipOut.setLevel(0);
      while (entriesCount-- > 0) {
        copyEntry(zipIn, zipIn.getNextEntry(), zipOut);
      }
      return tempfile;
    } finally {
      if (zipOut != null) {
        zipOut.close();
      }
      if (zipIn != null) {
        zipIn.close();
      }
    }
  }

  private File createTemporaryFileFromValidEntries(File file, List<ZipEntry> validEntries) throws IOException {
    if (validEntries.size() <= 0) {      
      throw new IOException("No valid entries");
    }
    File tempfile = OperatingSystem.createTemporaryFile("part", ".sh3d");
    ZipOutputStream zipOut = null;
    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(file);
      zipOut = new ZipOutputStream(new FileOutputStream(tempfile));
      zipOut.setLevel(0);
      for (ZipEntry zipEntry : validEntries) {
        InputStream zipIn = zipFile.getInputStream(zipEntry);
        copyEntry(zipIn, zipEntry, zipOut);
        zipIn.close();
      }
      return tempfile;
    } finally {
      if (zipOut != null) {
        zipOut.close();
      }
      if (zipFile != null) {
        zipFile.close();
      }
    }
  }
  
  private void copyEntry(InputStream zipIn, ZipEntry entry, ZipOutputStream zipOut) throws IOException {
    checkCurrentThreadIsntInterrupted();
    ZipEntry entryCopy = new ZipEntry(entry.getName());
    entryCopy.setComment(entry.getComment());
    entryCopy.setTime(entry.getTime());
    entryCopy.setExtra(entry.getExtra());
    zipOut.putNextEntry(entryCopy);
    byte [] buffer = new byte [8192];
    int size; 
    while ((size = zipIn.read(buffer)) != -1) {
      zipOut.write(buffer, 0, size);
    }
    zipOut.closeEntry();
  }

  private class CopiedInputStream extends FilterInputStream {
    private OutputStream out;

    protected CopiedInputStream(InputStream in, OutputStream out) {
      super(in);
      this.out = out;
    }
    
    @Override
    public int read() throws IOException {
      int b = super.read();
      if (b != -1) {
        this.out.write(b);
      }
      return b;
    }

    @Override
    public int read(byte [] b, int off, int len) throws IOException {
      int size = super.read(b, off, len);
      if (size != -1) {
        this.out.write(b, off, size);
      }
      return size;
    }
    
    @Override
    public void close() throws IOException {
      try {
        byte [] buffer = new byte [8192];
        int size; 
        while ((size = this.in.read(buffer)) != -1) {
          this.out.write(buffer, 0, size);
        }
        this.out.flush();
      } finally {
        this.out.close();
        super.close();
      }
    }
  }

  private class HomeObjectInputStream extends ObjectInputStream {
    private HomeContentContext contentContext;

    public HomeObjectInputStream(InputStream in, 
                                 HomeContentContext contentContext) throws IOException {
      super(in);
      if (contentRecording != ContentRecording.INCLUDE_NO_CONTENT) {
        enableResolveObject(true);
        this.contentContext = contentContext;
      }
    }
    
    @Override
    protected Object resolveObject(Object obj) throws IOException {
      if (obj instanceof URLContent) {
        String url = ((URLContent)obj).getURL().toString();
        if (url.startsWith("jar:file:temp!/")) {
          return this.contentContext.lookupContent(url.substring(url.indexOf('!') + 2));
        }
      } 
      return obj;
    }
  }
}