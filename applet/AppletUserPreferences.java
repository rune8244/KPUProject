
package com.eteks.homeview3d.applet;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.eteks.homeview3d.io.DefaultFurnitureCatalog;
import com.eteks.homeview3d.io.DefaultTexturesCatalog;
import com.eteks.homeview3d.io.DefaultUserPreferences;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.FurnitureCategory;
import com.eteks.homeview3d.model.LengthUnit;
import com.eteks.homeview3d.model.Library;
import com.eteks.homeview3d.model.PatternsCatalog;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.TextureImage;
import com.eteks.homeview3d.model.TexturesCatalog;
import com.eteks.homeview3d.model.TexturesCategory;
import com.eteks.homeview3d.model.UserPreferences;


public class AppletUserPreferences extends UserPreferences {
  private static final String LANGUAGE                                  = "language";
  private static final String UNIT                                      = "unit";
  private static final String FURNITURE_CATALOG_VIEWED_IN_TREE          = "furnitureCatalogViewedInTree";
  private static final String NAVIGATION_PANEL_VISIBLE                  = "navigationPanelVisible";
  private static final String AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED = "aerialViewCenteredOnSelectionEnabled";
  private static final String MAGNETISM_ENABLED                         = "magnetismEnabled";
  private static final String RULERS_VISIBLE                            = "rulersVisible";
  private static final String GRID_VISIBLE                              = "gridVisible";
  private static final String DEFAULT_FONT_NAME                         = "defaultFontName";
  private static final String FURNITURE_VIEWED_FROM_TOP                 = "furnitureViewedFromTop";
  private static final String ROOM_FLOOR_COLORED_OR_TEXTURED            = "roomFloorColoredOrTextured";
  private static final String WALL_PATTERN                              = "wallPattern";
  private static final String NEW_WALL_PATTERN                          = "newWallPattern";
  private static final String NEW_WALL_HEIGHT                           = "newHomeWallHeight";
  private static final String NEW_WALL_THICKNESS                        = "newWallThickness";
  private static final String NEW_FLOOR_THICKNESS                       = "newFloorThickness";
  private static final String RECENT_HOMES                              = "recentHomes#";
  private static final String IGNORED_ACTION_TIP                        = "ignoredActionTip#";

  private final URL []  pluginFurnitureCatalogURLs;
  private final URL     furnitureResourcesUrlBase;
  private final URL []  pluginTexturesCatalogURLs;
  private final URL     texturesResourcesUrlBase;
  private Properties    properties;
  private final URL     writePreferencesURL;
  private final URL     readPreferencesURL;
  private Executor      catalogsLoader;
  private Executor      updater;
  private boolean       writtenPropertiesUpdated;
  
  private final Map<String, Boolean> ignoredActionTips = new HashMap<String, Boolean>();

  public AppletUserPreferences(URL [] pluginFurnitureCatalogURLs,
                               URL [] pluginTexturesCatalogURLs) {
    this(pluginFurnitureCatalogURLs, pluginTexturesCatalogURLs, null, null);
  }

  public AppletUserPreferences(URL [] pluginFurnitureCatalogURLs,
                               URL [] pluginTexturesCatalogURLs, 
                               URL writePreferencesURL, 
                               URL readPreferencesURL) {
    this(pluginFurnitureCatalogURLs, pluginTexturesCatalogURLs, writePreferencesURL, readPreferencesURL, null);
  }
  

  public AppletUserPreferences(URL [] pluginFurnitureCatalogURLs,
                               URL [] pluginTexturesCatalogURLs, 
                               URL writePreferencesURL, 
                               URL readPreferencesURL,
                               String userLanguage) {
    this(pluginFurnitureCatalogURLs, null, pluginTexturesCatalogURLs, null, 
        writePreferencesURL, readPreferencesURL, userLanguage);
  }

  public AppletUserPreferences(URL [] pluginFurnitureCatalogURLs,
                               URL    furnitureResourcesUrlBase,
                               URL [] pluginTexturesCatalogURLs,
                               URL    texturesResourcesUrlBase,
                               URL writePreferencesURL, 
                               URL readPreferencesURL,
                               String userLanguage) {
    this(pluginFurnitureCatalogURLs, furnitureResourcesUrlBase, pluginTexturesCatalogURLs, texturesResourcesUrlBase,
        writePreferencesURL, readPreferencesURL, null, userLanguage);
  }
  

  public AppletUserPreferences(URL [] pluginFurnitureCatalogURLs,
                               URL    furnitureResourcesUrlBase,
                               URL [] pluginTexturesCatalogURLs,
                               URL    texturesResourcesUrlBase,
                               URL writePreferencesURL, 
                               URL readPreferencesURL,
                               Executor updater,
                               String userLanguage) {
    this.pluginFurnitureCatalogURLs = pluginFurnitureCatalogURLs;
    this.furnitureResourcesUrlBase = furnitureResourcesUrlBase;
    this.pluginTexturesCatalogURLs = pluginTexturesCatalogURLs;
    this.texturesResourcesUrlBase = texturesResourcesUrlBase;
    this.writePreferencesURL = writePreferencesURL;
    this.readPreferencesURL = readPreferencesURL;
    if (updater == null) {
      this.catalogsLoader =
      this.updater = new Executor() {
          public void execute(Runnable command) {
            command.run();
          }
        };
    } else {
      this.catalogsLoader = Executors.newSingleThreadExecutor();
      this.updater = updater;
    }
    
    final Properties properties = getProperties();
    
    if (userLanguage == null) {
      userLanguage = getLanguage();
    } 
    if (!Arrays.asList(getSupportedLanguages()).contains(userLanguage)) {
      userLanguage = Locale.ENGLISH.getLanguage();
    }
    setLanguage(properties.getProperty(LANGUAGE, userLanguage));    

    // 디폴트 가구 및 텍스쳐 읽어옴
    setFurnitureCatalog(new FurnitureCatalog());
    setTexturesCatalog(new TexturesCatalog());
    updateDefaultCatalogs();
 
    DefaultUserPreferences defaultPreferences = new DefaultUserPreferences();
    defaultPreferences.setLanguage(getLanguage());
    
    // 디폴트 패턴 카탈로그 로 채움 
    PatternsCatalog patternsCatalog = defaultPreferences.getPatternsCatalog();
    setPatternsCatalog(patternsCatalog);
 
    setUnit(LengthUnit.valueOf(properties.getProperty(UNIT, defaultPreferences.getLengthUnit().name())));
    setFurnitureCatalogViewedInTree(Boolean.parseBoolean(properties.getProperty(FURNITURE_CATALOG_VIEWED_IN_TREE, 
        String.valueOf(defaultPreferences.isFurnitureCatalogViewedInTree()))));
    setNavigationPanelVisible(Boolean.parseBoolean(properties.getProperty(NAVIGATION_PANEL_VISIBLE, 
        String.valueOf(defaultPreferences.isNavigationPanelVisible()))));
    setAerialViewCenteredOnSelectionEnabled(Boolean.parseBoolean(properties.getProperty(AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED, 
        String.valueOf(defaultPreferences.isAerialViewCenteredOnSelectionEnabled()))));
    setMagnetismEnabled(Boolean.parseBoolean(properties.getProperty(MAGNETISM_ENABLED, "true")));
    setRulersVisible(Boolean.parseBoolean(properties.getProperty(RULERS_VISIBLE, 
        String.valueOf(defaultPreferences.isMagnetismEnabled()))));
    setGridVisible(Boolean.parseBoolean(properties.getProperty(GRID_VISIBLE, 
        String.valueOf(defaultPreferences.isGridVisible()))));
    setDefaultFontName(properties.getProperty(DEFAULT_FONT_NAME, defaultPreferences.getDefaultFontName()));
    setFurnitureViewedFromTop(Boolean.parseBoolean(properties.getProperty(FURNITURE_VIEWED_FROM_TOP, 
        String.valueOf(defaultPreferences.isFurnitureViewedFromTop()))));
    setFloorColoredOrTextured(Boolean.parseBoolean(properties.getProperty(ROOM_FLOOR_COLORED_OR_TEXTURED, 
        String.valueOf(defaultPreferences.isRoomFloorColoredOrTextured()))));
    try {
      setWallPattern(patternsCatalog.getPattern(properties.getProperty(WALL_PATTERN, 
          defaultPreferences.getWallPattern().getName())));
    } catch (IllegalArgumentException ex) {
      setWallPattern(defaultPreferences.getWallPattern());
    }
    try {
      if (defaultPreferences.getNewWallPattern() != null) {
        setNewWallPattern(patternsCatalog.getPattern(properties.getProperty(NEW_WALL_PATTERN, 
            defaultPreferences.getNewWallPattern().getName())));
      }
    } catch (IllegalArgumentException ex) {
    }
    setNewWallThickness(Float.parseFloat(properties.getProperty(NEW_WALL_THICKNESS, 
            String.valueOf(defaultPreferences.getNewWallThickness()))));
    setNewWallHeight(Float.parseFloat(properties.getProperty(NEW_WALL_HEIGHT,
        String.valueOf(defaultPreferences.getNewWallHeight()))));    
    setNewWallBaseboardThickness(defaultPreferences.getNewWallBaseboardThickness());
    setNewWallBaseboardHeight(defaultPreferences.getNewWallBaseboardHeight());
    setNewFloorThickness(Float.parseFloat(properties.getProperty(NEW_FLOOR_THICKNESS, 
        String.valueOf(defaultPreferences.getNewFloorThickness()))));
    setCurrency(defaultPreferences.getCurrency());    
    // 최근의 집 파일 읽어옴
    List<String> recentHomes = new ArrayList<String>();
    for (int i = 1; i <= getRecentHomesMaxCount(); i++) {
      String recentHome = properties.getProperty(RECENT_HOMES + i, null);
      if (recentHome != null) {
        recentHomes.add(recentHome);
      }
    }
    setRecentHomes(recentHomes);
    for (int i = 1; ; i++) {
      String ignoredActionTip = properties.getProperty(IGNORED_ACTION_TIP + i, "");
      if (ignoredActionTip.length() == 0) {
        break;
      } else {
        this.ignoredActionTips.put(ignoredActionTip, true);
      }
    }
    
    addPropertyChangeListener(Property.LANGUAGE, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          updateDefaultCatalogs();
        }
      });
    
    PropertyChangeListener savedPropertyListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          writtenPropertiesUpdated = true;
        }
      };
    Property [] writtenProperties = {
        Property.LANGUAGE,
        Property.UNIT,
        Property.FURNITURE_CATALOG_VIEWED_IN_TREE,
        Property.NAVIGATION_PANEL_VISIBLE,
        Property.AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED,
        Property.MAGNETISM_ENABLED,
        Property.RULERS_VISIBLE,
        Property.GRID_VISIBLE,
        Property.DEFAULT_FONT_NAME,
        Property.FURNITURE_VIEWED_FROM_TOP,
        Property.ROOM_FLOOR_COLORED_OR_TEXTURED,
        Property.WALL_PATTERN,
        Property.NEW_WALL_PATTERN,
        Property.NEW_WALL_THICKNESS,
        Property.NEW_WALL_HEIGHT,
        Property.NEW_FLOOR_THICKNESS,
        Property.RECENT_HOMES,
        Property.IGNORED_ACTION_TIP};
    for (Property writtenProperty : writtenProperties) {
      addPropertyChangeListener(writtenProperty, savedPropertyListener);
    }
  }

  private void updateDefaultCatalogs() {
    // 현재 가구의 디폴트 조각들 삭제        
    final FurnitureCatalog furnitureCatalog = getFurnitureCatalog();
    for (FurnitureCategory category : furnitureCatalog.getCategories()) {
      for (CatalogPieceOfFurniture piece : category.getFurniture()) {
        if (!piece.isModifiable()) {
          furnitureCatalog.delete(piece);
        }
      }
    }
    // 디폴트 조각 더하기
    this.catalogsLoader.execute(new Runnable() {
        public void run() {
          final DefaultFurnitureCatalog defaultFurnitureCatalog = 
              new DefaultFurnitureCatalog(pluginFurnitureCatalogURLs, furnitureResourcesUrlBase);
          for (final FurnitureCategory category : defaultFurnitureCatalog.getCategories()) {
            for (final CatalogPieceOfFurniture piece : category.getFurniture()) {
              updater.execute(new Runnable() {
                  public void run() {
                    furnitureCatalog.add(category, piece);
                  }
                });
            }
          }
        }
      });

    // 현재 텍스쳐 카탈로그의 디폴트 삭제         
    final TexturesCatalog texturesCatalog = getTexturesCatalog();
    for (TexturesCategory category : texturesCatalog.getCategories()) {
      for (CatalogTexture texture : category.getTextures()) {
        if (!texture.isModifiable()) {
          texturesCatalog.delete(texture);
        }
      }
    }
    // 디폴트 더하기
    this.catalogsLoader.execute(new Runnable() {
        public void run() {
          final DefaultTexturesCatalog defaultTexturesCatalog = 
              new DefaultTexturesCatalog(pluginTexturesCatalogURLs, texturesResourcesUrlBase);
          for (final TexturesCategory category : defaultTexturesCatalog.getCategories()) {
            for (final CatalogTexture texture : category.getTextures()) {
              updater.execute(new Runnable() {
                  public void run() {
                    texturesCatalog.add(category, texture);
                  }
                });
            }
          }
        }
      });
  }

  @Override
  public void write() throws RecorderException {
    if (this.writtenPropertiesUpdated) {
      this.writtenPropertiesUpdated = false;
      
      Properties properties = getProperties();
      properties.setProperty(LANGUAGE, getLanguage());
      properties.setProperty(UNIT, getLengthUnit().name());   
      properties.setProperty(FURNITURE_CATALOG_VIEWED_IN_TREE, String.valueOf(isFurnitureCatalogViewedInTree()));
      properties.setProperty(NAVIGATION_PANEL_VISIBLE, String.valueOf(isNavigationPanelVisible()));    
      properties.setProperty(AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED, String.valueOf(isAerialViewCenteredOnSelectionEnabled()));    
      properties.setProperty(MAGNETISM_ENABLED, String.valueOf(isMagnetismEnabled()));
      properties.setProperty(RULERS_VISIBLE, String.valueOf(isRulersVisible()));
      properties.setProperty(GRID_VISIBLE, String.valueOf(isGridVisible()));
      String defaultFontName = getDefaultFontName();
      if (defaultFontName == null) {
        properties.remove(DEFAULT_FONT_NAME);
      } else {
        properties.put(DEFAULT_FONT_NAME, defaultFontName);
      }
      properties.setProperty(FURNITURE_VIEWED_FROM_TOP, String.valueOf(isFurnitureViewedFromTop()));
      properties.setProperty(ROOM_FLOOR_COLORED_OR_TEXTURED, String.valueOf(isRoomFloorColoredOrTextured()));
      properties.setProperty(WALL_PATTERN, getWallPattern().getName());
      TextureImage newWallPattern = getNewWallPattern();
      if (newWallPattern != null) {
        properties.setProperty(NEW_WALL_PATTERN, newWallPattern.getName());
      }
      properties.setProperty(NEW_WALL_THICKNESS, String.valueOf(getNewWallThickness()));   
      properties.setProperty(NEW_WALL_HEIGHT, String.valueOf(getNewWallHeight()));
      properties.setProperty(NEW_FLOOR_THICKNESS, String.valueOf(getNewFloorThickness()));   
      // 최근의 집 리스트 쓰기
      int i = 1;
      for (Iterator<String> it = getRecentHomes().iterator(); it.hasNext() && i <= getRecentHomesMaxCount(); i ++) {
        properties.setProperty(RECENT_HOMES + i, it.next());
      }
      i = 1;
      for (Iterator<Map.Entry<String, Boolean>> it = this.ignoredActionTips.entrySet().iterator();
          it.hasNext(); ) {
        Entry<String, Boolean> ignoredActionTipEntry = it.next();
        if (ignoredActionTipEntry.getValue()) {
          properties.setProperty(IGNORED_ACTION_TIP + i++, ignoredActionTipEntry.getKey());
        } 
      }
      
      try {
        if (this.writePreferencesURL != null) {
          writePreferences(getProperties());
        }
      } catch (IOException ex) {
        throw new RecorderException("Couldn't write preferences", ex);
      }
    }
  }

  private Properties getProperties() {
    if (this.properties == null) {
      this.properties = new Properties();
      if (this.readPreferencesURL != null) {
        readPreferences(this.properties);
      }
    }
    return this.properties;
  }
  
  private void readPreferences(Properties properties) {
    URLConnection connection = null;
    InputStream in = null;
    try {
      connection = this.readPreferencesURL.openConnection();
      connection.setRequestProperty("Content-Type", "charset=UTF-8");
      connection.setUseCaches(false);
      in = connection.getInputStream();
      properties.loadFromXML(in);
    } catch (IOException ex) {
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {
      }
    }
  }
  
  private void writePreferences(Properties properties) throws IOException {
    HttpURLConnection connection = null;
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      properties.storeToXML(bytes, "Applet user preferences 1.0");
      bytes.close();

      connection = (HttpURLConnection)this.writePreferencesURL.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setUseCaches(false);
      OutputStream out = connection.getOutputStream();
      out.write("preferences=".getBytes("UTF-8"));
      out.write(URLEncoder.encode(new String(bytes.toByteArray(), "UTF-8"), "UTF-8").getBytes("UTF-8"));
      out.close();

      // 리스폰스 읽어옴
      InputStream in = connection.getInputStream();
      int read = in.read();
      in.close();
      
      if (read != '1') {
        throw new IOException("Saving preferences failed");
      } 
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }


  @Override
  public void setActionTipIgnored(String actionKey) {   
    this.ignoredActionTips.put(actionKey, true);
    super.setActionTipIgnored(actionKey);
  }

  @Override
  public boolean isActionTipIgnored(String actionKey) {
    Boolean ignoredActionTip = this.ignoredActionTips.get(actionKey);
    return ignoredActionTip != null && ignoredActionTip.booleanValue();
  }
  

  @Override
  public void resetIgnoredActionTips() {
    for (Iterator<Map.Entry<String, Boolean>> it = this.ignoredActionTips.entrySet().iterator();
         it.hasNext(); ) {
      Entry<String, Boolean> ignoredActionTipEntry = it.next();
      ignoredActionTipEntry.setValue(false);
    }
    super.resetIgnoredActionTips();
  }


  @Override
  public void addLanguageLibrary(String location) throws RecorderException {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean languageLibraryExists(String location) throws RecorderException {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean furnitureLibraryExists(String location) throws RecorderException {
    throw new UnsupportedOperationException();
  }


  @Override
  public void addFurnitureLibrary(String location) throws RecorderException {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean texturesLibraryExists(String location) throws RecorderException {
    throw new UnsupportedOperationException();
  }


  @Override
  public void addTexturesLibrary(String location) throws RecorderException {
    throw new UnsupportedOperationException();
  }


  @Override
  public List<Library> getLibraries() {
    throw new UnsupportedOperationException();
  }
}
