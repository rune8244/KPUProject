package com.eteks.homeview3d.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.FurnitureCategory;
import com.eteks.homeview3d.model.TexturesCategory;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.URLContent;

class HomeContentContext {
  private URL                      homeUrl;
  private boolean                  containsInvalidContents;
  private List<Content>            invalidContents;
  private List<URLContent>         validContentsNotInPreferences;

  private Map<URLContent, byte []> contentDigests;
  private Set<URLContent>          preferencesContentsCache;
  private boolean                  preferPreferencesContent;
  
  public HomeContentContext(URL homeSource,
                            UserPreferences preferences,
                            boolean preferPreferencesContent) {
    this.homeUrl = homeSource;
    this.preferPreferencesContent = preferPreferencesContent;
    this.contentDigests = readContentDigests(homeSource);
    this.invalidContents = new ArrayList<Content>();
    this.validContentsNotInPreferences = new ArrayList<URLContent>();
    if (preferences != null 
        && this.preferencesContentsCache == null) {
      this.preferencesContentsCache = getUserPreferencesContent(preferences);
    }
  }
  
  private Map<URLContent, byte []> readContentDigests(URL homeUrl) {
    ZipInputStream zipIn = null;
    try {
      zipIn = new ZipInputStream(homeUrl.openStream());
      ZipEntry entry = null;
      while ((entry = zipIn.getNextEntry()) != null) {
        if ("ContentDigests".equals(entry.getName())) {
          BufferedReader reader = new BufferedReader(new InputStreamReader(zipIn, "UTF-8"));
          String line = reader.readLine();
          if (line != null
              && line.trim().startsWith("ContentDigests-Version: 1")) {
            Map<URLContent, byte []> contentDigests = new HashMap<URLContent, byte[]>();
            String entryName = null;
            while ((line = reader.readLine()) != null) {
              if (line.startsWith("Name:")) {
                entryName = line.substring("Name:".length()).trim();
              } else if (line.startsWith("SHA-1-Digest:")) {
                byte [] digest = Base64.decode(line.substring("SHA-1-Digest:".length()).trim());
                if (entryName == null) {
                  throw new IOException("Missing entry name");
                } else {
                  URL url = new URL("jar:" + homeUrl + "!/" + entryName);
                  contentDigests.put(new HomeURLContent(url), digest);
                  entryName = null;
                }
              }
            }
            return contentDigests;
          }
        }
      }
    } catch (IOException ex) {
    } finally {
      if (zipIn != null) {
        try {
          zipIn.close();
        } catch (IOException ex) {
        }
      }
    }
    return null;
  }

  public Content lookupContent(String contentEntryName) throws IOException {
    URL fileURL = new URL("jar:" + this.homeUrl + "!/" + contentEntryName);
    HomeURLContent urlContent = new HomeURLContent(fileURL);
    ContentDigestManager contentDigestManager = ContentDigestManager.getInstance();
    if (!isValid(urlContent)) {
      this.containsInvalidContents = true;
      URLContent preferencesContent = findUserPreferencesContent(urlContent);
      if (preferencesContent != null) {
        return preferencesContent;
      } else {
        this.invalidContents.add(urlContent);
      }
    } else {
      for (URLContent content : this.validContentsNotInPreferences) {
        if (contentDigestManager.equals(urlContent, content)) {
          return content;
        }
      }
      if (Thread.interrupted()) {
        throw new InterruptedIOException();
      }
      byte [] contentDigest;
      if (this.contentDigests != null
          && (contentDigest = this.contentDigests.get(urlContent)) != null
          && !contentDigestManager.isContentDigestEqual(urlContent, contentDigest)) {
        this.containsInvalidContents = true;  
        URLContent preferencesContent = findUserPreferencesContent(urlContent);
        if (preferencesContent != null) {
          return preferencesContent;
        } else {
          this.invalidContents.add(urlContent);
        }
      } else {
        if (this.preferencesContentsCache != null
            && this.preferPreferencesContent) {
          for (URLContent preferencesContent : this.preferencesContentsCache) {
            if (contentDigestManager.equals(urlContent, preferencesContent)) {
              return preferencesContent;
            }
          }
        }
        this.validContentsNotInPreferences.add(urlContent);
      }
    }
    return urlContent;
  }

  private boolean isValid(Content content) {
    try {
      InputStream in = content.openStream();
      try {
        in.close();
        return true;
      } catch (NullPointerException e) {
      }
    } catch (IOException e) {
    }
    return false;
  }

  public boolean containsCheckedContents() {
    return this.contentDigests != null && this.invalidContents.size() == 0;
  }

  public boolean containsInvalidContents() {
    return this.containsInvalidContents;
  }
  

  private URLContent findUserPreferencesContent(URLContent content) {
    if (this.contentDigests != null
        && this.preferencesContentsCache != null) {
      byte [] contentDigest = this.contentDigests.get(content);
      if (contentDigest != null) {
        ContentDigestManager contentDigestManager = ContentDigestManager.getInstance();
        for (URLContent preferencesContent : this.preferencesContentsCache) {
          if (contentDigestManager.isContentDigestEqual(preferencesContent, contentDigest)) {
            return preferencesContent;
          }
        }
      }
    }
    return null;
  }
  
  public List<Content> getInvalidContents() {
    return Collections.unmodifiableList(this.invalidContents);
  }
  
  private Set<URLContent> getUserPreferencesContent(UserPreferences preferences) {
    Set<URLContent> preferencesContent = new HashSet<URLContent>();
    for (FurnitureCategory category : preferences.getFurnitureCatalog().getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        addURLContent(piece.getIcon(), preferencesContent);
        addURLContent(piece.getModel(), preferencesContent);
        addURLContent(piece.getPlanIcon(), preferencesContent);
      }
    }
    for (TexturesCategory category : preferences.getTexturesCatalog().getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        addURLContent(texture.getImage(), preferencesContent);
      }
    }
    return preferencesContent;
  }
  
  private void addURLContent(Content content, Set<URLContent> preferencesContent) {
    if (content instanceof URLContent) {
      preferencesContent.add((URLContent)content);
    }
  }
}