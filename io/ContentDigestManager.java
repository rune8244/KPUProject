package com.eteks.homeview3d.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.tools.ResourceURLContent;
import com.eteks.homeview3d.tools.SimpleURLContent;
import com.eteks.homeview3d.tools.URLContent;

public class ContentDigestManager {
  private static final String  DIGEST_ALGORITHM = "SHA-1";
  private static final byte [] INVALID_CONTENT_DIGEST = {};

  private static ContentDigestManager instance;
  
  private Map<Content, byte []>  contentDigestsCache;
  
  private Map<URLContent, URL>   zipUrlsCache;
  private Map<URL, List<String>> zipUrlEntriesCache;

  private ContentDigestManager() {
    this.contentDigestsCache = new WeakHashMap<Content, byte[]>();
    this.zipUrlsCache = new WeakHashMap<URLContent, URL>();
    this.zipUrlEntriesCache = new WeakHashMap<URL, List<String>>();
  }
  

  public static ContentDigestManager getInstance() {
    if (instance == null) {
      synchronized (ContentDigestManager.class) {
        if (instance == null) {
          instance = new ContentDigestManager();
        }
      }
    }
    return instance;
  }

  public boolean equals(Content content1, Content content2) {
    byte [] content1Digest = getContentDigest(content1);
    if (content1Digest == INVALID_CONTENT_DIGEST) {
      return false;
    } else {
      return Arrays.equals(content1Digest, getContentDigest(content2));
    }
  }

  public boolean isContentDigestEqual(Content content, byte [] digest) {
    byte [] contentDigest = getContentDigest(content);
    if (contentDigest == INVALID_CONTENT_DIGEST) {
      return false;
    } else {
      return Arrays.equals(contentDigest, digest);
    }
  }


  public synchronized void setContentDigest(Content content, byte [] digest) {
    this.contentDigestsCache.put(content, digest);
  }
  
  public synchronized byte [] getContentDigest(Content content) {
    byte [] digest = this.contentDigestsCache.get(content);
    if (digest == null) {
      try {
        if (content instanceof ResourceURLContent) {
          digest = getResourceContentDigest((ResourceURLContent)content);
        } else if (content instanceof URLContent
                   && !(content instanceof SimpleURLContent)
                   && ((URLContent)content).isJAREntry()) {
          URLContent urlContent = (URLContent)content;
          if (urlContent instanceof HomeURLContent) {
            digest = getHomeContentDigest((HomeURLContent)urlContent);            
          } else {
            digest = getZipContentDigest(urlContent);
          }
        } else {
          digest = computeContentDigest(content);
        }
      } catch (NoSuchAlgorithmException ex) {
        throw new InternalError("No SHA-1 message digest is available");
      } catch (IOException ex) {
        digest = INVALID_CONTENT_DIGEST;
      }
      this.contentDigestsCache.put(content, digest);
    }
    return digest;
  }

  /**
   * 리소스 다이제스트 컨텐츠로 돌아감.
   */
  private byte [] getResourceContentDigest(ResourceURLContent urlContent) throws IOException, NoSuchAlgorithmException {
    if (urlContent.isMultiPartResource()) {
      if (urlContent.isJAREntry()) {
        URL zipUrl = urlContent.getJAREntryURL();
        String entryName = urlContent.getJAREntryName();
        int lastSlashIndex = entryName.lastIndexOf('/');
        if (lastSlashIndex != -1) {
          MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
          String entryDirectory = entryName.substring(0, lastSlashIndex + 1);
          for (String zipEntryName : getZipURLEntries(urlContent)) {
            if (zipEntryName.startsWith(entryDirectory) 
                && !zipEntryName.equals(entryDirectory)
                && isSignificant(zipEntryName)) {
              Content siblingContent = new URLContent(new URL("jar:" + zipUrl + "!/" 
                  + URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20")));
              updateMessageDigest(messageDigest, siblingContent);
            }
          }
          return messageDigest.digest();
        } else {
          return computeContentDigest(urlContent);
        }
      } else {
        try {
          MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
          File contentFile = new File(urlContent.getURL().toURI());
          File parentFile = new File(contentFile.getParent());
          File [] siblingFiles = parentFile.listFiles();
          Arrays.sort(siblingFiles);
          for (File siblingFile : siblingFiles) {
            if (!siblingFile.isDirectory()) {
              updateMessageDigest(messageDigest, new URLContent(siblingFile.toURI().toURL()));
            }
          }
          return messageDigest.digest();
        } catch (URISyntaxException ex) {
          IOException ex2 = new IOException();
          ex2.initCause(ex);
          throw ex2;
        }
      }
    } else {
      return computeContentDigest(urlContent);
    }
  }

  private byte [] getHomeContentDigest(HomeURLContent urlContent) throws IOException, NoSuchAlgorithmException {
    String entryName = urlContent.getJAREntryName();
    int slashIndex = entryName.indexOf('/');
    if (slashIndex > 0) {
      URL zipUrl = urlContent.getJAREntryURL();
      String entryDirectory = entryName.substring(0, slashIndex + 1);
      MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
      for (String zipEntryName : getZipURLEntries(urlContent)) {
        if (zipEntryName.startsWith(entryDirectory) 
            && !zipEntryName.equals(entryDirectory)
            && isSignificant(zipEntryName)) {
          Content siblingContent = new URLContent(new URL("jar:" + zipUrl + "!/" 
              + URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20")));
          updateMessageDigest(messageDigest, siblingContent);    
        }
      }
      return messageDigest.digest();
    } else {
      return computeContentDigest(urlContent);
    }
  }

  /**
   * 주어진 zip 컨텐츠로 돌아감.
   */
  private byte [] getZipContentDigest(URLContent urlContent) throws IOException, NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
    for (String zipEntryName : ContentDigestManager.getInstance().getZipURLEntries(urlContent)) {
      if (isSignificant(zipEntryName)) {
        Content siblingContent = new URLContent(new URL("jar:" + urlContent.getJAREntryURL() + "!/" 
            + URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20")));
        updateMessageDigest(messageDigest, siblingContent);
      }
    }
    return messageDigest.digest();
  }
  
  private boolean isSignificant(String entryName) {
    String entryNameUpperCase = entryName.toUpperCase();
    return !entryNameUpperCase.equals("LICENSE.TXT") 
          && !entryNameUpperCase.endsWith("/LICENSE.TXT");
  }

  synchronized List<String> getZipURLEntries(URLContent urlContent) throws IOException {
    URL zipUrl = this.zipUrlsCache.get(urlContent);
    if (zipUrl != null) {
      return this.zipUrlEntriesCache.get(zipUrl); 
    } else {
      zipUrl = urlContent.getJAREntryURL(); 
      for (Map.Entry<URL, List<String>> entry : this.zipUrlEntriesCache.entrySet()) {
        if (zipUrl.equals(entry.getKey())) {
          this.zipUrlsCache.put(urlContent, entry.getKey());
          return entry.getValue();
        }
      }
      List<String> zipUrlEntries = new ArrayList<String>();
      ZipInputStream zipIn = null;
      try {
        zipIn = new ZipInputStream(zipUrl.openStream());
        for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
          zipUrlEntries.add(entry.getName());
        }

        Collections.sort(zipUrlEntries); 
        this.zipUrlEntriesCache.put(zipUrl, zipUrlEntries);
        this.zipUrlsCache.put(urlContent, zipUrl);
        return zipUrlEntries;
      } finally {
        if (zipIn != null) {
          zipIn.close();
        }
      }
    }
  }

  private byte [] computeContentDigest(Content content) throws IOException, NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
    updateMessageDigest(messageDigest, content);
    return messageDigest.digest();
  }

  /**
   * 메시지 업데이트.
   */
  private void updateMessageDigest(MessageDigest messageDigest, Content content) throws IOException {
    InputStream in = null;
    try {
      in = content.openStream();
      byte [] buffer = new byte [8192];
      int size; 
      while ((size = in.read(buffer)) != -1) {
        messageDigest.update(buffer, 0, size);
      }
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }
}
