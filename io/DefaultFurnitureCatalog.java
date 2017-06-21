package com.eteks.homeview3d.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import com.eteks.homeview3d.model.CatalogDoorOrWindow;
import com.eteks.homeview3d.model.CatalogLight;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.FurnitureCategory;
import com.eteks.homeview3d.model.Library;
import com.eteks.homeview3d.model.LightSource;
import com.eteks.homeview3d.model.Sash;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.ResourceURLContent;
import com.eteks.homeview3d.tools.TemporaryURLContent;
import com.eteks.homeview3d.tools.URLContent;

/**
 * 가구 디폴트 카탈로그
 */
public class DefaultFurnitureCatalog extends FurnitureCatalog {
  /**
   * 키 프로퍼티
   */
  public enum PropertyKey {
    ID("id"),
    NAME("name"),
    DESCRIPTION("description"),
    INFORMATION("information"),
    TAGS("tags"),
    CREATION_DATE("creationDate"),
    GRADE("grade"),
    CATEGORY("category"),
    ICON("icon"),
    ICON_DIGEST("iconDigest"),
    PLAN_ICON("planIcon"),
    PLAN_ICON_DIGEST("planIconDigest"),
    MODEL("model"),
    MODEL_DIGEST("modelDigest"),
    MULTI_PART_MODEL("multiPartModel"),
    WIDTH("width"),
    DEPTH("depth"),
    HEIGHT("height"),
    MOVABLE("movable"),
    DOOR_OR_WINDOW("doorOrWindow"),
    DOOR_OR_WINDOW_CUT_OUT_SHAPE("doorOrWindowCutOutShape"),
    DOOR_OR_WINDOW_WALL_THICKNESS("doorOrWindowWallThickness"),
    DOOR_OR_WINDOW_WALL_DISTANCE("doorOrWindowWallDistance"),
    DOOR_OR_WINDOW_SASH_X_AXIS("doorOrWindowSashXAxis"),
    DOOR_OR_WINDOW_SASH_Y_AXIS("doorOrWindowSashYAxis"),
    DOOR_OR_WINDOW_SASH_WIDTH("doorOrWindowSashWidth"),
    DOOR_OR_WINDOW_SASH_START_ANGLE("doorOrWindowSashStartAngle"),
    DOOR_OR_WINDOW_SASH_END_ANGLE("doorOrWindowSashEndAngle"),
    LIGHT_SOURCE_X("lightSourceX"),
    LIGHT_SOURCE_Y("lightSourceY"),
    LIGHT_SOURCE_Z("lightSourceZ"),
    LIGHT_SOURCE_COLOR("lightSourceColor"),
    LIGHT_SOURCE_DIAMETER("lightSourceDiameter"),
    STAIRCASE_CUT_OUT_SHAPE("staircaseCutOutShape"),
    ELEVATION("elevation"),
    DROP_ON_TOP_ELEVATION("dropOnTopElevation"),
    MODEL_ROTATION("modelRotation"),
    CREATOR("creator"),
    RESIZABLE("resizable"),
    DEFORMABLE("deformable"),
    TEXTURABLE("texturable"),
    PRICE("price"),
    VALUE_ADDED_TAX_PERCENTAGE("valueAddedTaxPercentage"),
    CURRENCY("currency");
    
    private String keyPrefix;

    private PropertyKey(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }
    
    /**
     * 인덱스 키로 돌아감.
     */
    public String getKey(int pieceIndex) {
      return keyPrefix + "#" + pieceIndex;
    }
  }

  public static final String PLUGIN_FURNITURE_CATALOG_FAMILY = "PluginFurnitureCatalog";
  
  private static final String CONTRIBUTED_FURNITURE_CATALOG_FAMILY = "ContributedFurnitureCatalog";
  private static final String ADDITIONAL_FURNITURE_CATALOG_FAMILY  = "AdditionalFurnitureCatalog";
  
  private List<Library> libraries = new ArrayList<Library>();
  
  /**
   * 디폴트 가구 카탈로그 생성.
   */
  public DefaultFurnitureCatalog() {
    this((File)null);
  }
  
  public DefaultFurnitureCatalog(File furniturePluginFolder) {
    this(null, furniturePluginFolder);
  }
  
  /**
   * 리소스랑 플러그인에서 불러와서 생성
   */
  public DefaultFurnitureCatalog(final UserPreferences preferences, 
                                 File furniturePluginFolder) {
    this(preferences, furniturePluginFolder == null ? null : new File [] {furniturePluginFolder});
  }
  
  public DefaultFurnitureCatalog(final UserPreferences preferences, 
                                 File [] furniturePluginFolders) {
    Map<FurnitureCategory, Map<CatalogPieceOfFurniture, Integer>> furnitureHomonymsCounter = 
        new HashMap<FurnitureCategory, Map<CatalogPieceOfFurniture,Integer>>();
    List<String> identifiedFurniture = new ArrayList<String>();
    
    readDefaultFurnitureCatalogs(preferences, furnitureHomonymsCounter, identifiedFurniture);
    
    if (furniturePluginFolders != null) {
      for (File furniturePluginFolder : furniturePluginFolders) {
        File [] pluginFurnitureCatalogFiles = furniturePluginFolder.listFiles(new FileFilter () {
          public boolean accept(File pathname) {
            return pathname.isFile();
          }
        });
        
        if (pluginFurnitureCatalogFiles != null) {
          Arrays.sort(pluginFurnitureCatalogFiles, Collections.reverseOrder(OperatingSystem.getFileVersionComparator()));
          for (File pluginFurnitureCatalogFile : pluginFurnitureCatalogFiles) {
            readPluginFurnitureCatalog(pluginFurnitureCatalogFile, identifiedFurniture);
          }
        }
      }
    }
  }

  public DefaultFurnitureCatalog(URL [] pluginFurnitureCatalogUrls) {
    this(pluginFurnitureCatalogUrls, null);
  }
  
  public DefaultFurnitureCatalog(URL [] pluginFurnitureCatalogUrls,
                                 URL    furnitureResourcesUrlBase) {
    List<String> identifiedFurniture = new ArrayList<String>();
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (securityManager != null) {
        securityManager.checkCreateClassLoader();
      }

      for (URL pluginFurnitureCatalogUrl : pluginFurnitureCatalogUrls) {
        try {        
          ResourceBundle resource = ResourceBundle.getBundle(PLUGIN_FURNITURE_CATALOG_FAMILY, Locale.getDefault(), 
              new URLContentClassLoader(pluginFurnitureCatalogUrl));
          this.libraries.add(0, new DefaultLibrary(pluginFurnitureCatalogUrl.toExternalForm(), 
              UserPreferences.FURNITURE_LIBRARY_TYPE, resource));
          readFurniture(resource, pluginFurnitureCatalogUrl, furnitureResourcesUrlBase, identifiedFurniture);
        } catch (MissingResourceException ex) {
        } catch (IllegalArgumentException ex) {
        }
      }
    } catch (AccessControlException ex) {
      ResourceBundle resource = ResourceBundle.getBundle(PLUGIN_FURNITURE_CATALOG_FAMILY, Locale.getDefault());
      readFurniture(resource, null, furnitureResourcesUrlBase, identifiedFurniture);
    }
  }
  
  public List<Library> getLibraries() {
    return Collections.unmodifiableList(this.libraries);
  }

  private static final Map<File,URL> pluginFurnitureCatalogUrlUpdates = new HashMap<File, URL>(); 
  
  private void readPluginFurnitureCatalog(File pluginFurnitureCatalogFile,
                                          List<String> identifiedFurniture) {
    try {
      final URL pluginFurnitureCatalogUrl;
      long urlModificationDate = pluginFurnitureCatalogFile.lastModified();
      URL urlUpdate = pluginFurnitureCatalogUrlUpdates.get(pluginFurnitureCatalogFile);
      if (pluginFurnitureCatalogFile.canWrite()
          && (urlUpdate == null 
              || urlUpdate.openConnection().getLastModified() < urlModificationDate)) {
        TemporaryURLContent contentCopy = TemporaryURLContent.copyToTemporaryURLContent(new URLContent(pluginFurnitureCatalogFile.toURI().toURL()));
        URL temporaryFurnitureCatalogUrl = contentCopy.getURL();
        pluginFurnitureCatalogUrlUpdates.put(pluginFurnitureCatalogFile, temporaryFurnitureCatalogUrl);
        pluginFurnitureCatalogUrl = temporaryFurnitureCatalogUrl;
      } else if (urlUpdate != null) {
        pluginFurnitureCatalogUrl = urlUpdate;
      } else {
        pluginFurnitureCatalogUrl = pluginFurnitureCatalogFile.toURI().toURL();
      }
      
      final ClassLoader urlLoader = new URLContentClassLoader(pluginFurnitureCatalogUrl);
      ResourceBundle resourceBundle = ResourceBundle.getBundle(PLUGIN_FURNITURE_CATALOG_FAMILY, Locale.getDefault(), urlLoader);
      this.libraries.add(0, new DefaultLibrary(pluginFurnitureCatalogFile.getCanonicalPath(), 
          UserPreferences.FURNITURE_LIBRARY_TYPE, resourceBundle));
      readFurniture(resourceBundle, pluginFurnitureCatalogUrl, null, identifiedFurniture);
    } catch (MissingResourceException ex) {
    } catch (IllegalArgumentException ex) {
    } catch (IOException ex) {
    }
  }
  
  /**
   * 디폴트 퍼니쳐 불러오기.
   */
  private void readDefaultFurnitureCatalogs(UserPreferences preferences,
                                            Map<FurnitureCategory, Map<CatalogPieceOfFurniture, Integer>> furnitureHomonymsCounter,
                                            List<String> identifiedFurniture) {
    String defaultFurnitureCatalogFamily = DefaultFurnitureCatalog.class.getName();
    readFurnitureCatalog(defaultFurnitureCatalogFamily, 
        preferences, furnitureHomonymsCounter, identifiedFurniture);
    
    String classPackage = defaultFurnitureCatalogFamily.substring(0, defaultFurnitureCatalogFamily.lastIndexOf("."));
    readFurnitureCatalog(classPackage + "." + CONTRIBUTED_FURNITURE_CATALOG_FAMILY, 
        preferences, furnitureHomonymsCounter, identifiedFurniture);

    readFurnitureCatalog(classPackage + "." + ADDITIONAL_FURNITURE_CATALOG_FAMILY, 
        preferences, furnitureHomonymsCounter, identifiedFurniture);
  }
  
  /**
   * 주어진 카탈로그에서 가구 불러오기.
   */
  private void readFurnitureCatalog(final String furnitureCatalogFamily,
                                    final UserPreferences preferences,
                                    Map<FurnitureCategory, Map<CatalogPieceOfFurniture, Integer>> furnitureHomonymsCounter,
                                    List<String> identifiedFurniture) {
    ResourceBundle resource;
    if (preferences != null) {
      resource = new ResourceBundle() {
          @Override
          protected Object handleGetObject(String key) {
            try {
              return preferences.getLocalizedString(furnitureCatalogFamily, key);
            } catch (IllegalArgumentException ex) {
              throw new MissingResourceException("Unknown key " + key, 
                  furnitureCatalogFamily + "_" + Locale.getDefault(), key);
            }
          }
          
          @Override
          public Enumeration<String> getKeys() {
            // 안 끝남
            throw new UnsupportedOperationException();
          }
        };
    } else {
      try {
        resource = ResourceBundle.getBundle(furnitureCatalogFamily);
      } catch (MissingResourceException ex) {
        return;
      }
    }
    readFurniture(resource, null, null, identifiedFurniture);
  }
  
  private void readFurniture(ResourceBundle resource, 
                             URL furnitureCatalogUrl,
                             URL furnitureResourcesUrlBase,
                             List<String> identifiedFurniture) {
    CatalogPieceOfFurniture piece;
    for (int i = 1; (piece = readPieceOfFurniture(resource, i, furnitureCatalogUrl, furnitureResourcesUrlBase)) != null; i++) {
      if (piece.getId() != null) {
        if (identifiedFurniture.contains(piece.getId())) {
          continue;
        } else {
          identifiedFurniture.add(piece.getId());
        }
      }
      FurnitureCategory pieceCategory = readFurnitureCategory(resource, i);
      add(pieceCategory, piece);
    }
  }

  protected CatalogPieceOfFurniture readPieceOfFurniture(ResourceBundle resource, 
                                                         int index, 
                                                         URL furnitureCatalogUrl,
                                                         URL furnitureResourcesUrlBase) {
    String name = null;
    try {
      name = resource.getString(PropertyKey.NAME.getKey(index));
    } catch (MissingResourceException ex) {
      return null;
    }
    String id = getOptionalString(resource, PropertyKey.ID.getKey(index), null);
    String description = getOptionalString(resource, PropertyKey.DESCRIPTION.getKey(index), null);
    String information = getOptionalString(resource, PropertyKey.INFORMATION.getKey(index), null);
    String tagsString = getOptionalString(resource, PropertyKey.TAGS.getKey(index), null);
    String [] tags;
    if (tagsString != null) {
      tags = tagsString.split("\\s*,\\s*");
    } else {
      tags = new String [0];
    }
    String creationDateString = getOptionalString(resource, PropertyKey.CREATION_DATE.getKey(index), null);
    Long creationDate = null;
    if (creationDateString != null) {
      try {
        creationDate = new SimpleDateFormat("yyyy-MM-dd").parse(creationDateString).getTime();
      } catch (ParseException ex) {
        throw new IllegalArgumentException("Can't parse date "+ creationDateString, ex);
      }
    }
    String gradeString = getOptionalString(resource, PropertyKey.GRADE.getKey(index), null);
    Float grade = null;
    if (gradeString != null) {
      grade = Float.valueOf(gradeString);
    }
    Content icon  = getContent(resource, PropertyKey.ICON.getKey(index), PropertyKey.ICON_DIGEST.getKey(index), 
        furnitureCatalogUrl, furnitureResourcesUrlBase, false, false);
    Content planIcon = getContent(resource, PropertyKey.PLAN_ICON.getKey(index), PropertyKey.PLAN_ICON_DIGEST.getKey(index), 
        furnitureCatalogUrl, furnitureResourcesUrlBase, false, true);
    boolean multiPartModel = getOptionalBoolean(resource, PropertyKey.MULTI_PART_MODEL.getKey(index), false);
    Content model = getContent(resource, PropertyKey.MODEL.getKey(index), PropertyKey.MODEL_DIGEST.getKey(index), 
        furnitureCatalogUrl, furnitureResourcesUrlBase, multiPartModel, false);
    float width = Float.parseFloat(resource.getString(PropertyKey.WIDTH.getKey(index)));
    float depth = Float.parseFloat(resource.getString(PropertyKey.DEPTH.getKey(index)));
    float height = Float.parseFloat(resource.getString(PropertyKey.HEIGHT.getKey(index)));
    float elevation = getOptionalFloat(resource, PropertyKey.ELEVATION.getKey(index), 0);
    float dropOnTopElevation = getOptionalFloat(resource, PropertyKey.DROP_ON_TOP_ELEVATION.getKey(index), height) / height;
    boolean movable = Boolean.parseBoolean(resource.getString(PropertyKey.MOVABLE.getKey(index)));
    boolean doorOrWindow = Boolean.parseBoolean(resource.getString(PropertyKey.DOOR_OR_WINDOW.getKey(index)));
    String staircaseCutOutShape = getOptionalString(resource, PropertyKey.STAIRCASE_CUT_OUT_SHAPE.getKey(index), null);     
    float [][] modelRotation = getModelRotation(resource, PropertyKey.MODEL_ROTATION.getKey(index));
    String creator = getOptionalString(resource, PropertyKey.CREATOR.getKey(index), null);
    boolean resizable = getOptionalBoolean(resource, PropertyKey.RESIZABLE.getKey(index), true);
    boolean deformable = getOptionalBoolean(resource, PropertyKey.DEFORMABLE.getKey(index), true);
    boolean texturable = getOptionalBoolean(resource, PropertyKey.TEXTURABLE.getKey(index), true);
    BigDecimal price = null;
    try {
      price = new BigDecimal(resource.getString(PropertyKey.PRICE.getKey(index)));
    } catch (MissingResourceException ex) {
    }
    BigDecimal valueAddedTaxPercentage = null;
    try {
      valueAddedTaxPercentage = new BigDecimal(resource.getString(PropertyKey.VALUE_ADDED_TAX_PERCENTAGE.getKey(index)));
    } catch (MissingResourceException ex) {
      // 디폴트 가격은 널
    }
    String currency = getOptionalString(resource, PropertyKey.CURRENCY.getKey(index), null);

    if (doorOrWindow) {
      String doorOrWindowCutOutShape = getOptionalString(resource, PropertyKey.DOOR_OR_WINDOW_CUT_OUT_SHAPE.getKey(index), null);     
      float wallThicknessPercentage = getOptionalFloat(
          resource, PropertyKey.DOOR_OR_WINDOW_WALL_THICKNESS.getKey(index), depth) / depth;
      float wallDistancePercentage = getOptionalFloat(
          resource, PropertyKey.DOOR_OR_WINDOW_WALL_DISTANCE.getKey(index), 0) / depth;
      Sash [] sashes = getDoorOrWindowSashes(resource, index, width, depth);
      return new CatalogDoorOrWindow(id, name, description, information, tags, creationDate, grade, 
          icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable, 
          doorOrWindowCutOutShape, wallThicknessPercentage, wallDistancePercentage, sashes,
          modelRotation, creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);
    } else {
      LightSource [] lightSources = getLightSources(resource, index, width, depth, height);
      if (lightSources != null) {
        return new CatalogLight(id, name, description, information, tags, creationDate, grade, 
            icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable, 
            lightSources, staircaseCutOutShape, modelRotation, creator, 
            resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);
      } else {
        return new CatalogPieceOfFurniture(id, name, description, information, tags, creationDate, grade, 
            icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable, 
            staircaseCutOutShape, modelRotation, creator, 
            resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);
      }
    }
  }
  
  protected FurnitureCategory readFurnitureCategory(ResourceBundle resource, int index) {
    String category = resource.getString(PropertyKey.CATEGORY.getKey(index));
    return new FurnitureCategory(category);
  }
    
  private Content getContent(ResourceBundle resource, 
                             String contentKey, 
                             String contentDigestKey,
                             URL furnitureUrl,
                             URL resourceUrlBase, 
                             boolean multiPartModel,
                             boolean optional) {
    String contentFile = optional
        ? getOptionalString(resource, contentKey, null)
        : resource.getString(contentKey);
    if (optional && contentFile == null) {
      return null;
    }
    URLContent content;
    try {
      URL url;
      if (resourceUrlBase == null) {
        url = new URL(contentFile);
      } else {
        url = contentFile.startsWith("?") 
            ? new URL(resourceUrlBase + contentFile)
            : new URL(resourceUrlBase, contentFile);
        if (contentFile.indexOf('!') >= 0 && !contentFile.startsWith("jar:")) {
          url = new URL("jar:" + url);
        }
      }
      content = new URLContent(url);
    } catch (MalformedURLException ex) {
      if (furnitureUrl == null) {
        content = new ResourceURLContent(DefaultFurnitureCatalog.class, contentFile, multiPartModel);
      } else {
        try {
          content = new ResourceURLContent(new URL("jar:" + furnitureUrl + "!" + contentFile), multiPartModel);
        } catch (MalformedURLException ex2) {
          throw new IllegalArgumentException("Invalid URL", ex2);
        }
      }
    }
    
    String contentDigest = getOptionalString(resource, contentDigestKey, null);
    if (contentDigest != null && contentDigest.length() > 0) {
      try {        
        ContentDigestManager.getInstance().setContentDigest(content, Base64.decode(contentDigest));
      } catch (IOException ex) {
      }
    }
    return content;
  }
  
  private float [][] getModelRotation(ResourceBundle resource, String key) {
    try {
      String modelRotationString = resource.getString(key);
      String [] values = modelRotationString.split(" ", 9);
      
      if (values.length == 9) {
        return new float [][] {{Float.parseFloat(values [0]), 
                                Float.parseFloat(values [1]), 
                                Float.parseFloat(values [2])}, 
                               {Float.parseFloat(values [3]), 
                                Float.parseFloat(values [4]), 
                                Float.parseFloat(values [5])}, 
                               {Float.parseFloat(values [6]), 
                                Float.parseFloat(values [7]), 
                                Float.parseFloat(values [8])}};
      } else {
        return null;
      }
    } catch (MissingResourceException ex) {
      return null;
    } catch (NumberFormatException ex) {
      return null;
    }
  }
  
  private Sash [] getDoorOrWindowSashes(ResourceBundle resource, int index, 
                                        float doorOrWindowWidth, 
                                        float doorOrWindowDepth) throws MissingResourceException {
    Sash [] sashes;
    String sashXAxisString = getOptionalString(resource, PropertyKey.DOOR_OR_WINDOW_SASH_X_AXIS.getKey(index), null);
    if (sashXAxisString != null) {
      String [] sashXAxisValues = sashXAxisString.split(" ");
      String [] sashYAxisValues = resource.getString(PropertyKey.DOOR_OR_WINDOW_SASH_Y_AXIS.getKey(index)).split(" ");
      if (sashYAxisValues.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_Y_AXIS.getKey(index) + " key");
      }
      String [] sashWidths = resource.getString(PropertyKey.DOOR_OR_WINDOW_SASH_WIDTH.getKey(index)).split(" ");
      if (sashWidths.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_WIDTH.getKey(index) + " key");
      }
      String [] sashStartAngles = resource.getString(PropertyKey.DOOR_OR_WINDOW_SASH_START_ANGLE.getKey(index)).split(" ");
      if (sashStartAngles.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_START_ANGLE.getKey(index) + " key");
      }
      String [] sashEndAngles = resource.getString(PropertyKey.DOOR_OR_WINDOW_SASH_END_ANGLE.getKey(index)).split(" ");
      if (sashEndAngles.length != sashXAxisValues.length) {
        throw new IllegalArgumentException(
            "Expected " + sashXAxisValues.length + " values in " + PropertyKey.DOOR_OR_WINDOW_SASH_END_ANGLE.getKey(index) + " key");
      }
      
      sashes = new Sash [sashXAxisValues.length];
      for (int i = 0; i < sashes.length; i++) {
        sashes [i] = new Sash(Float.parseFloat(sashXAxisValues [i]) / doorOrWindowWidth, 
            Float.parseFloat(sashYAxisValues [i]) / doorOrWindowDepth, 
            Float.parseFloat(sashWidths [i]) / doorOrWindowWidth, 
            (float)Math.toRadians(Float.parseFloat(sashStartAngles [i])), 
            (float)Math.toRadians(Float.parseFloat(sashEndAngles [i])));
      }
    } else {
      sashes = new Sash [0];
    }
    
    return sashes;
  }

  private LightSource [] getLightSources(ResourceBundle resource, int index, 
                                         float lightWidth, 
                                         float lightDepth,
                                         float lightHeight) throws MissingResourceException {
    LightSource [] lightSources = null;
    String lightSourceXString = getOptionalString(resource, PropertyKey.LIGHT_SOURCE_X.getKey(index), null);
    if (lightSourceXString != null) {
      String [] lightSourceX = lightSourceXString.split(" ");
      String [] lightSourceY = resource.getString(PropertyKey.LIGHT_SOURCE_Y.getKey(index)).split(" ");
      if (lightSourceY.length != lightSourceX.length) {
        throw new IllegalArgumentException(
            "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_Y.getKey(index) + " key");
      }
      String [] lightSourceZ = resource.getString(PropertyKey.LIGHT_SOURCE_Z.getKey(index)).split(" ");
      if (lightSourceZ.length != lightSourceX.length) {
        throw new IllegalArgumentException(
            "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_Z.getKey(index) + " key");
      }
      String [] lightSourceColors = resource.getString(PropertyKey.LIGHT_SOURCE_COLOR.getKey(index)).split(" ");
      if (lightSourceColors.length != lightSourceX.length) {
        throw new IllegalArgumentException(
            "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_COLOR.getKey(index) + " key");
      }
      String lightSourceDiametersString = getOptionalString(resource, PropertyKey.LIGHT_SOURCE_DIAMETER.getKey(index), null);
      String [] lightSourceDiameters;
      if (lightSourceDiametersString != null) {
        lightSourceDiameters = lightSourceDiametersString.split(" ");
        if (lightSourceDiameters.length != lightSourceX.length) {
          throw new IllegalArgumentException(
              "Expected " + lightSourceX.length + " values in " + PropertyKey.LIGHT_SOURCE_DIAMETER.getKey(index) + " key");
        }
      } else {
        lightSourceDiameters = null;
      }
      
      lightSources = new LightSource [lightSourceX.length];
      for (int i = 0; i < lightSources.length; i++) {
        int color = lightSourceColors [i].startsWith("#")
            ? Integer.parseInt(lightSourceColors [i].substring(1), 16)
            : Integer.parseInt(lightSourceColors [i]);
        lightSources [i] = new LightSource(Float.parseFloat(lightSourceX [i]) / lightWidth, 
            Float.parseFloat(lightSourceY [i]) / lightDepth, 
            Float.parseFloat(lightSourceZ [i]) / lightHeight, 
            color,
            lightSourceDiameters != null
                ? Float.parseFloat(lightSourceDiameters [i]) / lightWidth
                : null);
      }
    }     
    return lightSources;
  }

  private String getOptionalString(ResourceBundle resource, 
                                   String propertyKey,
                                   String defaultValue) {
    try {
      return resource.getString(propertyKey);
    } catch (MissingResourceException ex) {
      return defaultValue;
    }
  }

  private float getOptionalFloat(ResourceBundle resource, 
                                 String propertyKey,
                                 float defaultValue) {
    try {
      return Float.parseFloat(resource.getString(propertyKey));
    } catch (MissingResourceException ex) {
      return defaultValue;
    }
  }

  private boolean getOptionalBoolean(ResourceBundle resource, 
                                     String propertyKey,
                                     boolean defaultValue) {
    try {
      return Boolean.parseBoolean(resource.getString(propertyKey));
    } catch (MissingResourceException ex) {
      return defaultValue;
    }
  }
}

