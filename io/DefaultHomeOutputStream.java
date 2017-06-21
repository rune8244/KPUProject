package com.eteks.homeview3d.io;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.tools.ResourceURLContent;
import com.eteks.homeview3d.tools.SimpleURLContent;
import com.eteks.homeview3d.tools.TemporaryURLContent;
import com.eteks.homeview3d.tools.URLContent;

public class DefaultHomeOutputStream extends FilterOutputStream {
  private int              compressionLevel;
  private ContentRecording contentRecording;
  private boolean          serializedHome;
  private HomeXMLExporter  homeXmlExporter;
  
  public DefaultHomeOutputStream(OutputStream out) throws IOException {
    this(out, 0, false);
  }

  public DefaultHomeOutputStream(OutputStream out,
                                 int          compressionLevel, 
                                 boolean      includeTemporaryContent) throws IOException {
    this(out, compressionLevel, 
        includeTemporaryContent 
            ? ContentRecording.INCLUDE_TEMPORARY_CONTENT
            : ContentRecording.INCLUDE_ALL_CONTENT);
  }

  public DefaultHomeOutputStream(OutputStream out,
                                 int          compressionLevel, 
                                 ContentRecording contentRecording) throws IOException {
    this(out, compressionLevel, contentRecording, true, null);
  }


  public DefaultHomeOutputStream(OutputStream out,
                                 int          compressionLevel, 
                                 ContentRecording contentRecording,
                                 boolean          serializedHome,
                                 HomeXMLExporter  homeXmlExporter) throws IOException {
    super(out);
    if (!serializedHome && homeXmlExporter == null) {
      throw new IllegalArgumentException("No entry specified for home data");
    }
    this.compressionLevel = compressionLevel;
    this.contentRecording = contentRecording;
    this.serializedHome = serializedHome;
    this.homeXmlExporter = homeXmlExporter;
  }


  private static void checkCurrentThreadIsntInterrupted() throws InterruptedIOException {
    if (Thread.interrupted()) {
      throw new InterruptedIOException();
    }
  }

  public void writeHome(Home home) throws IOException {
    ZipOutputStream zipOut = new ZipOutputStream(this.out);
    zipOut.setLevel(this.compressionLevel);
    checkCurrentThreadIsntInterrupted();
    HomeContentObjectsTracker contentTracker = new HomeContentObjectsTracker(new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
      });
    contentTracker.writeObject(home);
    Map<Content, String> savedContentNames = contentTracker.getSavedContentNames();

    if (this.serializedHome) {
      zipOut.putNextEntry(new ZipEntry("Home"));
      HomeObjectOutputStream objectOut = new HomeObjectOutputStream(zipOut, savedContentNames);
      objectOut.writeObject(home);
      objectOut.flush();
      zipOut.closeEntry();
    }

    if (this.homeXmlExporter != null) {
      zipOut.putNextEntry(new ZipEntry("Home.xml"));
      XMLWriter xmlWriter = new XMLWriter(zipOut);
      this.homeXmlExporter.setSavedContentNames(savedContentNames);
      this.homeXmlExporter.writeElement(xmlWriter, home);
      xmlWriter.flush();
      zipOut.closeEntry();
    }
    
    if (savedContentNames.size() > 0) {
      Set<String> contentEntryNames = new HashSet<String>();    
      zipOut.putNextEntry(new ZipEntry("ContentDigests"));
      OutputStreamWriter writer = new OutputStreamWriter(zipOut, "UTF-8");
      ContentDigestManager digestManager = ContentDigestManager.getInstance();
      writer.write("ContentDigests-Version: 1.0\n\n");
      for (Map.Entry<Content, String> savedContent : savedContentNames.entrySet()) {
        String contentEntryName = savedContent.getValue();
        if (!contentEntryNames.contains(contentEntryName)) {
          contentEntryNames.add(contentEntryName);
          writer.write("Name: " + contentEntryName + "\n");
          writer.write("SHA-1-Digest: " + Base64.encodeBytes(digestManager.getContentDigest(savedContent.getKey())) + "\n\n");
        }
      }
      writer.flush();
      zipOut.closeEntry();
    
      contentEntryNames.clear(); 
      for (Map.Entry<Content, String> savedContent : savedContentNames.entrySet()) {
        String contentEntryName = savedContent.getValue();
        if (!contentEntryNames.contains(contentEntryName)) {
          contentEntryNames.add(contentEntryName);
          Content content = savedContent.getKey();
          int slashIndex = contentEntryName.indexOf('/');
          if (slashIndex > 0) {
            contentEntryName = contentEntryName.substring(0, slashIndex);
          }
          if (content instanceof ResourceURLContent) {
            writeResourceZipEntries(zipOut, contentEntryName, (ResourceURLContent)content);
          } else if (content instanceof URLContent
                     && !(content instanceof SimpleURLContent)
                     && ((URLContent)content).isJAREntry()) {
            URLContent urlContent = (URLContent)content;
            if (urlContent instanceof HomeURLContent) {
              writeHomeZipEntries(zipOut, contentEntryName, (HomeURLContent)urlContent);            
            } else {
              writeZipEntries(zipOut, contentEntryName, urlContent);
            }
          } else {
            writeZipEntry(zipOut, contentEntryName, content);
          }
        }
      }  
    }
    zipOut.finish();
  }

  private void writeResourceZipEntries(ZipOutputStream zipOut,
                                       String entryNameOrDirectory,
                                       ResourceURLContent urlContent) throws IOException {
    if (urlContent.isMultiPartResource()) {
      if (urlContent.isJAREntry()) {
        URL zipUrl = urlContent.getJAREntryURL();
        String entryName = urlContent.getJAREntryName();
        int lastSlashIndex = entryName.lastIndexOf('/');
        if (lastSlashIndex != -1) {
          String entryDirectory = entryName.substring(0, lastSlashIndex + 1);
          for (String zipEntryName : ContentDigestManager.getInstance().getZipURLEntries(urlContent)) {
            if (zipEntryName.startsWith(entryDirectory)) {
              Content siblingContent = new URLContent(new URL("jar:" + zipUrl + "!/" 
                  + URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20")));
              writeZipEntry(zipOut, entryNameOrDirectory + zipEntryName.substring(lastSlashIndex), siblingContent);
            }
          }
        } else {
          writeZipEntry(zipOut, entryNameOrDirectory, urlContent);
        }
      } else {
        try {
          File contentFile = new File(urlContent.getURL().toURI());
          File parentFile = new File(contentFile.getParent());
          File [] siblingFiles = parentFile.listFiles();
          for (File siblingFile : siblingFiles) {
            if (!siblingFile.isDirectory()) {
              writeZipEntry(zipOut, entryNameOrDirectory + "/" + siblingFile.getName(), 
                  new URLContent(siblingFile.toURI().toURL()));
            }
          }
        } catch (URISyntaxException ex) {
          IOException ex2 = new IOException();
          ex2.initCause(ex);
          throw ex2;
        }
      }
    } else {
      writeZipEntry(zipOut, entryNameOrDirectory, urlContent);
    }
  }

  private void writeHomeZipEntries(ZipOutputStream zipOut,
                                   String entryNameOrDirectory,
                                   HomeURLContent urlContent) throws IOException {
    String entryName = urlContent.getJAREntryName();
    int slashIndex = entryName.indexOf('/');
    if (slashIndex > 0) {
      URL zipUrl = urlContent.getJAREntryURL();
      String entryDirectory = entryName.substring(0, slashIndex + 1);
      for (String zipEntryName : ContentDigestManager.getInstance().getZipURLEntries(urlContent)) {
        if (zipEntryName.startsWith(entryDirectory)) {
          Content siblingContent = new URLContent(new URL("jar:" + zipUrl + "!/" 
              + URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20")));
          writeZipEntry(zipOut, entryNameOrDirectory + zipEntryName.substring(slashIndex), siblingContent);
        }
      }
    } else {
      writeZipEntry(zipOut, entryNameOrDirectory, urlContent);
    }
  }

  private void writeZipEntries(ZipOutputStream zipOut, 
                               String directory,
                               URLContent urlContent) throws IOException {
    for (String zipEntryName : ContentDigestManager.getInstance().getZipURLEntries(urlContent)) {
      Content siblingContent = new URLContent(new URL("jar:" + urlContent.getJAREntryURL() + "!/" 
          + URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20")));
      writeZipEntry(zipOut, directory + "/" + zipEntryName, siblingContent);
    }
  }

  private void writeZipEntry(ZipOutputStream zipOut, String entryName, Content content) throws IOException {
    checkCurrentThreadIsntInterrupted();
    byte [] buffer = new byte [8192];
    InputStream contentIn = null;
    try {
      zipOut.putNextEntry(new ZipEntry(entryName));
      contentIn = content.openStream();          
      int size; 
      while ((size = contentIn.read(buffer)) != -1) {
        zipOut.write(buffer, 0, size);
      }
      zipOut.closeEntry();  
    } finally {
      if (contentIn != null) {          
        contentIn.close();
      }
    }
  }

  private class HomeContentObjectsTracker extends ObjectOutputStream {
    private Map<Content, String> savedContentNames = new LinkedHashMap<Content, String>();
    private int savedContentIndex = 0;

    public HomeContentObjectsTracker(OutputStream out) throws IOException {
      super(out);
      if (contentRecording != ContentRecording.INCLUDE_NO_CONTENT) {
        enableReplaceObject(true);
      }
    }

    @Override
    protected Object replaceObject(Object obj) throws IOException {
      if (obj instanceof TemporaryURLContent 
          || obj instanceof HomeURLContent
          || (contentRecording == ContentRecording.INCLUDE_ALL_CONTENT && obj instanceof Content)) {
        String subEntryName = "";
        if (obj instanceof URLContent) {
          URLContent urlContent = (URLContent)obj;
          ContentDigestManager contentDigestManager = ContentDigestManager.getInstance();
          for (Map.Entry<Content, String> contentEntry : this.savedContentNames.entrySet()) {
            if (contentDigestManager.equals(urlContent, contentEntry.getKey())) {
              this.savedContentNames.put((Content)obj, contentEntry.getValue());
              return obj;
            }
          }
          checkCurrentThreadIsntInterrupted();
          if (urlContent.isJAREntry()) {
            String entryName = urlContent.getJAREntryName();
            if (urlContent instanceof HomeURLContent) {
              int slashIndex = entryName.indexOf('/');
              if (slashIndex > 0) {
                subEntryName = entryName.substring(slashIndex);
              }
            } else if (urlContent instanceof ResourceURLContent) {
              ResourceURLContent resourceUrlContent = (ResourceURLContent)urlContent;
              if (resourceUrlContent.isMultiPartResource()) {
                int lastSlashIndex = entryName.lastIndexOf('/');
                if (lastSlashIndex != -1) {
                  subEntryName = entryName.substring(lastSlashIndex);
                }
              }
            } else if (!(urlContent instanceof SimpleURLContent)) {
              subEntryName = "/" + entryName;
            }            
          } else if (urlContent instanceof ResourceURLContent) {
            ResourceURLContent resourceUrlContent = (ResourceURLContent)urlContent;
            if (resourceUrlContent.isMultiPartResource()) {
              try {
                subEntryName = "/" + new File(resourceUrlContent.getURL().toURI()).getName();
              } catch (URISyntaxException ex) {
                IOException ex2 = new IOException();
                ex2.initCause(ex);
                throw ex2;
              }
            }
          }
        } 

        String homeContentPath = this.savedContentIndex++ + subEntryName;
        this.savedContentNames.put((Content)obj, homeContentPath);
      } 
      return obj;
    }
    
    public Map<Content, String> getSavedContentNames() {
      return this.savedContentNames;
    }
  }
  private class HomeObjectOutputStream extends ObjectOutputStream {
    private Map<Content, String>    savedContentNames;
    private Map<String, URLContent> replacedContents = new HashMap<String, URLContent>();

    public HomeObjectOutputStream(OutputStream out,
                                  Map<Content, String> savedContentNames) throws IOException {
      super(out);
      this.savedContentNames = savedContentNames;
      if (contentRecording != ContentRecording.INCLUDE_NO_CONTENT) {
        enableReplaceObject(true);
      }
    }

    @Override
    protected Object replaceObject(Object obj) throws IOException {
      if (obj instanceof Content) {
        String savedContentName = this.savedContentNames.get((Content)obj);
        if (savedContentName != null) {
          checkCurrentThreadIsntInterrupted();
          URLContent replacedContent = this.replacedContents.get(savedContentName);
          if (replacedContent == null) {
            replacedContent = new URLContent(new URL("jar:file:temp!/" + savedContentName));
            this.replacedContents.put(savedContentName, replacedContent);
          }
          return replacedContent;          
        }
      }
      return obj;
    }
  }
}