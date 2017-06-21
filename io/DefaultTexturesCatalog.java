package com.eteks.homeview3d.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.Library;
import com.eteks.homeview3d.model.TexturesCatalog;
import com.eteks.homeview3d.model.TexturesCategory;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.ResourceURLContent;
import com.eteks.homeview3d.tools.TemporaryURLContent;
import com.eteks.homeview3d.tools.URLContent;

public class DefaultTexturesCatalog extends TexturesCatalog {
  public enum PropertyKey {
    ID("id"),
    NAME("name"),
    CATEGORY("category"),
    IMAGE("image"),
    IMAGE_DIGEST("imageDigest"),
    WIDTH("width"),
    HEIGHT("height"),
    CREATOR("creator");

    private String keyPrefix;

    private PropertyKey(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }
    public String getKey(int textureIndex) {
      return keyPrefix + "#" + textureIndex;
    }
  }

  public static final String PLUGIN_TEXTURES_CATALOG_FAMILY = "PluginTexturesCatalog";

  private static final String ADDITIONAL_TEXTURES_CATALOG_FAMILY  = "AdditionalTexturesCatalog";

  private List<Library> libraries = new ArrayList<Library>();
  
  public DefaultTexturesCatalog() {
    this((File)null);
  }
  
  public DefaultTexturesCatalog(File texturesPluginFolder) {
    this(null, texturesPluginFolder);
  }
  
  public DefaultTexturesCatalog(final UserPreferences preferences, 
                                File texturesPluginFolder) {
    this(preferences, texturesPluginFolder == null ? null : new File [] {texturesPluginFolder});
  }
  
  public DefaultTexturesCatalog(final UserPreferences preferences, 
                                File [] texturesPluginFolders) {
    List<String> identifiedTextures = new ArrayList<String>();

    readDefaultTexturesCatalogs(preferences, identifiedTextures);

    if (texturesPluginFolders != null) {
      for (File texturesPluginFolder : texturesPluginFolders) {
        File [] pluginTexturesCatalogFiles = texturesPluginFolder.listFiles(new FileFilter () {
          public boolean accept(File pathname) {
            return pathname.isFile();
          }
        });
        
        if (pluginTexturesCatalogFiles != null) {
          Arrays.sort(pluginTexturesCatalogFiles, Collections.reverseOrder(OperatingSystem.getFileVersionComparator()));
          for (File pluginTexturesCatalogFile : pluginTexturesCatalogFiles) {
            readPluginTexturesCatalog(pluginTexturesCatalogFile, identifiedTextures);
          }
        }
      }
    }
  }

  public DefaultTexturesCatalog(URL [] pluginTexturesCatalogUrls) {
    this(pluginTexturesCatalogUrls, null);
  }
  
  public DefaultTexturesCatalog(URL [] pluginTexturesCatalogUrls,
                                URL    texturesResourcesUrlBase) {
    List<String> identifiedTextures = new ArrayList<String>();
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (securityManager != null) {
        securityManager.checkCreateClassLoader();
      }

      for (URL pluginTexturesCatalogUrl : pluginTexturesCatalogUrls) {
        try {
          ResourceBundle resource = ResourceBundle.getBundle(PLUGIN_TEXTURES_CATALOG_FAMILY, Locale.getDefault(),
              new URLContentClassLoader(pluginTexturesCatalogUrl));
          this.libraries.add(0, new DefaultLibrary(pluginTexturesCatalogUrl.toExternalForm(), 
              UserPreferences.TEXTURES_LIBRARY_TYPE, resource));
          readTextures(resource, pluginTexturesCatalogUrl, texturesResourcesUrlBase, identifiedTextures);
        } catch (MissingResourceException ex) {
        } catch (IllegalArgumentException ex) {
        }
      }
    } catch (AccessControlException ex) {
      ResourceBundle resource = ResourceBundle.getBundle(PLUGIN_TEXTURES_CATALOG_FAMILY, Locale.getDefault());
      readTextures(resource, null, texturesResourcesUrlBase, identifiedTextures);
    }
  }
  public List<Library> getLibraries() {
    return Collections.unmodifiableList(this.libraries);
  }

  private static final Map<File,URL> pluginTexturesCatalogUrlUpdates = new HashMap<File,URL>(); 
  
  private void readPluginTexturesCatalog(File pluginTexturesCatalogFile,
                                         List<String> identifiedTextures) {
    try {
      final URL pluginTexturesCatalogUrl;;
      long urlModificationDate = pluginTexturesCatalogFile.lastModified();
      URL urlUpdate = pluginTexturesCatalogUrlUpdates.get(pluginTexturesCatalogFile);
      if (pluginTexturesCatalogFile.canWrite()
          && (urlUpdate == null 
              || urlUpdate.openConnection().getLastModified() < urlModificationDate)) {
        TemporaryURLContent contentCopy = TemporaryURLContent.copyToTemporaryURLContent(new URLContent(pluginTexturesCatalogFile.toURI().toURL()));
        URL temporaryTexturesCatalogUrl = contentCopy.getURL();
        pluginTexturesCatalogUrlUpdates.put(pluginTexturesCatalogFile, temporaryTexturesCatalogUrl);
        pluginTexturesCatalogUrl = temporaryTexturesCatalogUrl;
      } else if (urlUpdate != null) {
        pluginTexturesCatalogUrl = urlUpdate;
      } else {
        pluginTexturesCatalogUrl = pluginTexturesCatalogFile.toURI().toURL();
      }
      
      final ClassLoader urlLoader = new URLContentClassLoader(pluginTexturesCatalogUrl);
      ResourceBundle resourceBundle = ResourceBundle.getBundle(PLUGIN_TEXTURES_CATALOG_FAMILY, Locale.getDefault(), urlLoader);      
      this.libraries.add(0, new DefaultLibrary(pluginTexturesCatalogFile.getCanonicalPath(), 
          UserPreferences.TEXTURES_LIBRARY_TYPE, resourceBundle));
      readTextures(resourceBundle, pluginTexturesCatalogUrl, null, identifiedTextures);
    } catch (MissingResourceException ex) {
    } catch (IllegalArgumentException ex) {
    } catch (IOException ex) {
    }
  }
  
  private void readDefaultTexturesCatalogs(final UserPreferences preferences,
                                           List<String> identifiedTextures) {
    final String defaultTexturesCatalogFamily = DefaultTexturesCatalog.class.getName();
    readTexturesCatalog(defaultTexturesCatalogFamily, 
        preferences, identifiedTextures);

    String classPackage = defaultTexturesCatalogFamily.substring(0, defaultTexturesCatalogFamily.lastIndexOf("."));
    readTexturesCatalog(classPackage + "." + ADDITIONAL_TEXTURES_CATALOG_FAMILY, 
        preferences, identifiedTextures);
  }

  private void readTexturesCatalog(final String texturesCatalogFamily,
                                   final UserPreferences preferences,
                                   List<String> identifiedTextures) {
    ResourceBundle resource;
    if (preferences != null) {
      resource = new ResourceBundle() {
          @Override
          protected Object handleGetObject(String key) {
            try {
              return preferences.getLocalizedString(texturesCatalogFamily, key);
            } catch (IllegalArgumentException ex) {
              throw new MissingResourceException("Unknown key " + key, 
                  texturesCatalogFamily + "_" + Locale.getDefault(), key);
            }
          }
          
          @Override
          public Enumeration<String> getKeys() {
            throw new UnsupportedOperationException();
          }
        };
    } else {
      try {
        resource = ResourceBundle.getBundle(texturesCatalogFamily);
      } catch (MissingResourceException ex) {
        return;
      }
    }
    readTextures(resource, null, null, identifiedTextures);
  }
  
  private void readTextures(ResourceBundle resource, 
                            URL texturesCatalogUrl,
                            URL texturesResourcesUrlBase,
                            List<String> identifiedTextures) {
    CatalogTexture texture;
    for (int i = 1; (texture = readTexture(resource, i, texturesCatalogUrl, texturesResourcesUrlBase)) != null; i++) {
      if (texture.getId() != null) {
        if (identifiedTextures.contains(texture.getId())) {
          continue;
        } else {
          identifiedTextures.add(texture.getId());
        }
      }
      TexturesCategory textureCategory = readTexturesCategory(resource, i);
      add(textureCategory, texture);
    }
  }
  
  protected CatalogTexture readTexture(ResourceBundle resource,
                                       int index,
                                       URL texturesUrl,
                                       URL texturesResourcesUrlBase) {
    String name = null;
    try {
      name = resource.getString(PropertyKey.NAME.getKey(index));
    } catch (MissingResourceException ex) {
      return null;
    }
    Content image  = getContent(resource, PropertyKey.IMAGE.getKey(index), PropertyKey.IMAGE_DIGEST.getKey(index), 
        texturesUrl, texturesResourcesUrlBase);
    float width = Float.parseFloat(resource.getString(PropertyKey.WIDTH.getKey(index)));
    float height = Float.parseFloat(resource.getString(PropertyKey.HEIGHT.getKey(index)));
    String creator = getOptionalString(resource, PropertyKey.CREATOR.getKey(index));
    String id = getOptionalString(resource, PropertyKey.ID.getKey(index));

    return new CatalogTexture(id, name, image, width, height, creator);
  }
  
  protected TexturesCategory readTexturesCategory(ResourceBundle resource, int index) {
    String category = resource.getString(PropertyKey.CATEGORY.getKey(index));
    return new TexturesCategory(category);
  }
    
  private Content getContent(ResourceBundle resource, 
                             String         contentKey,
                             String         contentDigestKey,
                             URL            texturesUrl,
                             URL            resourceUrlBase) {
    String contentFile = resource.getString(contentKey);
    URLContent content;
    try {
      URL url;
      if (resourceUrlBase == null) {
        url = new URL(contentFile);
      } else {
        url = contentFile.startsWith("?") 
            ? new URL(resourceUrlBase + contentFile)
            : new URL(resourceUrlBase, contentFile);
      }
      content = new URLContent(url);
    } catch (MalformedURLException ex) {
      if (texturesUrl == null) {
        content = new ResourceURLContent(DefaultTexturesCatalog.class, contentFile);
      } else {
        try {
          content = new ResourceURLContent(new URL("jar:" + texturesUrl + "!" + contentFile), false);
        } catch (MalformedURLException ex2) {
          throw new IllegalArgumentException("Invalid URL", ex2);
        }
      }
    }
    String contentDigest = getOptionalString(resource, contentDigestKey);
    if (contentDigest != null && contentDigest.length() > 0) {
      try {
        ContentDigestManager.getInstance().setContentDigest(content, Base64.decode(contentDigest));
      } catch (IOException ex) {
      }
    }
    return content; 
  }

  private String getOptionalString(ResourceBundle resource, 
                                   String propertyKey) {
    try {
      return resource.getString(propertyKey);
    } catch (MissingResourceException ex) {
      return null;
    }
  }
}
