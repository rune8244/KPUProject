package com.eteks.homeview3d.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyPermission;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public abstract class UserPreferences {
  public enum Property {LANGUAGE, SUPPORTED_LANGUAGES, UNIT, MAGNETISM_ENABLED, RULERS_VISIBLE, GRID_VISIBLE, DEFAULT_FONT_NAME, 
                        FURNITURE_VIEWED_FROM_TOP, ROOM_FLOOR_COLORED_OR_TEXTURED, WALL_PATTERN, NEW_WALL_PATTERN,    
                        NEW_WALL_THICKNESS, NEW_WALL_HEIGHT, NEW_WALL_SIDEBOARD_THICKNESS, NEW_WALL_SIDEBOARD_HEIGHT, NEW_FLOOR_THICKNESS, 
                        RECENT_HOMES, IGNORED_ACTION_TIP, FURNITURE_CATALOG_VIEWED_IN_TREE, NAVIGATION_PANEL_VISIBLE, 
                        AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED, CHECK_UPDATES_ENABLED, UPDATES_MINIMUM_DATE, AUTO_SAVE_DELAY_FOR_RECOVERY, 
                        AUTO_COMPLETION_STRINGS, RECENT_COLORS, RECENT_TEXTURES}
  
  public static final String FURNITURE_LIBRARY_TYPE = "Furniture library"; 
  public static final String TEXTURES_LIBRARY_TYPE  = "Textures library"; 
  public static final String LANGUAGE_LIBRARY_TYPE  = "Language library"; 
  
  private static final String [] DEFAULT_SUPPORTED_LANGUAGES; 
  private static final List<ClassLoader> DEFAULT_CLASS_LOADER = 
      Arrays.asList(new ClassLoader [] {UserPreferences.class.getClassLoader()});

  private static final TextStyle DEFAULT_TEXT_STYLE = new TextStyle(18f);
  private static final TextStyle DEFAULT_ROOM_TEXT_STYLE = new TextStyle(24f);

  static {
    Properties supportedLanguagesProperties = new Properties();
    String [] defaultSupportedLanguages;
    try {
      InputStream in = UserPreferences.class.getResourceAsStream("SupportedLanguages.properties");
      supportedLanguagesProperties.load(in);
      in.close();
      // supportedLanguages의 속성값 불러옴
      defaultSupportedLanguages = supportedLanguagesProperties.getProperty("supportedLanguages", "en").split("\\s");
    } catch (IOException ex) {
      defaultSupportedLanguages = new String [] {"en"};
    }
    DEFAULT_SUPPORTED_LANGUAGES = defaultSupportedLanguages;
  }
  
  private final PropertyChangeSupport          propertyChangeSupport;
  private final Map<Class<?>, ResourceBundle>  classResourceBundles;
  private final Map<String, ResourceBundle>    resourceBundles;

  private FurnitureCatalog furnitureCatalog;
  private TexturesCatalog  texturesCatalog;
  private PatternsCatalog  patternsCatalog;
  private final String     defaultCountry;
  private String []        supportedLanguages;
  private String           language;
  private String           currency;
  private LengthUnit       unit;
  private boolean          furnitureCatalogViewedInTree = true;
  private boolean          aerialViewCenteredOnSelectionEnabled;
  private boolean          navigationPanelVisible = true;
  private boolean          magnetismEnabled    = true;
  private boolean          rulersVisible       = true;
  private boolean          gridVisible         = true;
  private String           defaultFontName;
  private boolean          furnitureViewedFromTop;
  private boolean          roomFloorColoredOrTextured;
  private TextureImage     wallPattern;
  private TextureImage     newWallPattern;
  private float            newWallThickness;
  private float            newWallHeight;
  private float            newWallBaseboardThickness;
  private float            newWallBaseboardHeight;
  private float            newFloorThickness;
  private List<String>     recentHomes;
  private boolean          checkUpdatesEnabled;
  private Long             updatesMinimumDate;
  private int              autoSaveDelayForRecovery;
  private Map<String, List<String>>  autoCompletionStrings;
  private List<Integer>      recentColors;
  private List<TextureImage> recentTextures;

  public UserPreferences() {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.classResourceBundles = new HashMap<Class<?>, ResourceBundle>();
    this.resourceBundles = new HashMap<String, ResourceBundle>();
    this.autoCompletionStrings = new LinkedHashMap<String, List<String>>();
    this.recentHomes = Collections.emptyList();
    this.recentColors = Collections.emptyList();
    this.recentTextures = Collections.emptyList();

    this.supportedLanguages = DEFAULT_SUPPORTED_LANGUAGES;
    this.defaultCountry = Locale.getDefault().getCountry();    
    String defaultLanguage = Locale.getDefault().getLanguage();
    for (String supportedLanguage : this.supportedLanguages) {
      if (supportedLanguage.equals(defaultLanguage + "_" + this.defaultCountry)) {
        this.language = supportedLanguage;
        break; // 정확한 지원 언어 발견
      } else if (this.language == null 
                 && supportedLanguage.startsWith(defaultLanguage)) {
        this.language = supportedLanguage; 
      }
    }
    if (this.language == null) {
      this.language = Locale.ENGLISH.getLanguage();
    }
    updateDefaultLocale();
  }

  private void updateDefaultLocale() {
    try {
      int underscoreIndex = this.language.indexOf("_");
      if (underscoreIndex != -1) {
        Locale.setDefault(new Locale(this.language.substring(0, underscoreIndex), 
            this.language.substring(underscoreIndex + 1)));
      } else {
        Locale.setDefault(new Locale(this.language, this.defaultCountry));
      }
    } catch (AccessControlException ex) {
      this.language = Locale.getDefault().getLanguage();
    }
  }

  public abstract void write() throws RecorderException;

  public void addPropertyChangeListener(Property property, 
                                        PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, 
                                           PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  public FurnitureCatalog getFurnitureCatalog() {
    return this.furnitureCatalog;
  }

  protected void setFurnitureCatalog(FurnitureCatalog catalog) {
    this.furnitureCatalog = catalog;
  }

  public TexturesCatalog getTexturesCatalog() {
    return this.texturesCatalog;
  }

  protected void setTexturesCatalog(TexturesCatalog catalog) {
    this.texturesCatalog = catalog;
  }

  public PatternsCatalog getPatternsCatalog() {
    return this.patternsCatalog;
  }

  protected void setPatternsCatalog(PatternsCatalog catalog) {
    this.patternsCatalog = catalog;
  }

  public LengthUnit getLengthUnit() {
    return this.unit;
  }

  public void setUnit(LengthUnit unit) {
    if (this.unit != unit) {
      LengthUnit oldUnit = this.unit;
      this.unit = unit;
      this.propertyChangeSupport.firePropertyChange(Property.UNIT.name(), oldUnit, unit);
    }
  }

  public String getLanguage() {
    return this.language;
  }

  public void setLanguage(String language) {
    if (!language.equals(this.language)
        && isLanguageEditable()) {
      String oldLanguage = this.language;
      this.language = language;      
      updateDefaultLocale();
      this.classResourceBundles.clear();
      this.resourceBundles.clear();
      this.propertyChangeSupport.firePropertyChange(Property.LANGUAGE.name(), 
          oldLanguage, language);
    }
  }

  public boolean isLanguageEditable() {
    try {
      SecurityManager securityManager = System.getSecurityManager();
      if (securityManager != null) {
        securityManager.checkPermission(new PropertyPermission("user.language", "write"));
      }
      return true;
    } catch (AccessControlException ex) {
      return false;
    }
  }

  public String [] getDefaultSupportedLanguages() {
    return DEFAULT_SUPPORTED_LANGUAGES.clone();
  }

  public String [] getSupportedLanguages() {
    return this.supportedLanguages.clone();
  }

  protected void setSupportedLanguages(String [] supportedLanguages) {
    if (!Arrays.deepEquals(this.supportedLanguages, supportedLanguages)) {
      String [] oldSupportedLanguages = this.supportedLanguages;
      this.supportedLanguages = supportedLanguages.clone();
      this.propertyChangeSupport.firePropertyChange(Property.SUPPORTED_LANGUAGES.name(), 
          oldSupportedLanguages, supportedLanguages);
    }
  }

  public String getLocalizedString(Class<?> resourceClass,
                                   String   resourceKey, 
                                   Object ... resourceParameters) {
    ResourceBundle classResourceBundle = this.classResourceBundles.get(resourceClass);
    if (classResourceBundle == null) {
      try {      
        classResourceBundle = getResourceBundle(resourceClass.getName());
        this.classResourceBundles.put(resourceClass, classResourceBundle);
      } catch (IOException ex) {
        try {
          String className = resourceClass.getName();
          int lastIndex = className.lastIndexOf(".");
          String resourceFamily;
          if (lastIndex != -1) {
            resourceFamily = className.substring(0, lastIndex) + ".package";
          } else {
            resourceFamily = "package";
          }
          classResourceBundle = new PrefixedResourceBundle(getResourceBundle(resourceFamily), 
              resourceClass.getSimpleName() + ".");
          this.classResourceBundles.put(resourceClass, classResourceBundle);
        } catch (IOException ex2) {
          throw new IllegalArgumentException(
              "Can't find resource bundle for " + resourceClass, ex);
        }
      }
    } 

    return getLocalizedString(classResourceBundle, resourceKey, resourceParameters);
  }

  public String getLocalizedString(String resourceFamily,
                                   String resourceKey, 
                                   Object ... resourceParameters) {
    try {      
      ResourceBundle resourceBundle = getResourceBundle(resourceFamily);
      return getLocalizedString(resourceBundle, resourceKey, resourceParameters);
    } catch (IOException ex) {
      throw new IllegalArgumentException(
          "Can't find resource bundle for " + resourceFamily, ex);
    }
  }

  private ResourceBundle getResourceBundle(String resourceFamily) throws IOException {
    resourceFamily = resourceFamily.replace('.', '/');
    ResourceBundle resourceBundle = this.resourceBundles.get(resourceFamily);
    if (resourceBundle != null) {
      return resourceBundle;
    }
    Locale defaultLocale = Locale.getDefault();
    String language = defaultLocale.getLanguage();
    String country = defaultLocale.getCountry();
    String [] suffixes = {".properties",
                          "_" + language + ".properties",
                          "_" + language + "_" + country + ".properties"};
    for (String suffix : suffixes) {
      for (ClassLoader classLoader : getResourceClassLoaders()) {
        InputStream in = classLoader.getResourceAsStream(resourceFamily + suffix);
        if (in != null) {
          final ResourceBundle parentResourceBundle = resourceBundle;
          try {
            resourceBundle = new PropertyResourceBundle(in) {
              {
                setParent(parentResourceBundle);
              }
            };
            break;
          } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
          } finally {
            in.close();
          }
        }
      }
    }
    if (resourceBundle == null) {
      throw new IOException("No available resource bundle for " + resourceFamily);
    }
    this.resourceBundles.put(resourceFamily, resourceBundle);
    return resourceBundle;
  }

  private String getLocalizedString(ResourceBundle resourceBundle, 
                                    String         resourceKey, 
                                    Object...      resourceParameters) {
    try {
      String localizedString = resourceBundle.getString(resourceKey);
      if (resourceParameters.length > 0) {
        localizedString = String.format(localizedString, resourceParameters);
      }      
      return localizedString;
    } catch (MissingResourceException ex) {
      throw new IllegalArgumentException("Unknown key " + resourceKey);
    }
  }

  public List<ClassLoader> getResourceClassLoaders() {
    return DEFAULT_CLASS_LOADER;
  }

  public String getCurrency() {
    return this.currency;
  }

  protected void setCurrency(String currency) {
    this.currency = currency;
  }

  public boolean isFurnitureCatalogViewedInTree() {
    return this.furnitureCatalogViewedInTree;
  }

  public void setFurnitureCatalogViewedInTree(boolean furnitureCatalogViewedInTree) {
    if (this.furnitureCatalogViewedInTree != furnitureCatalogViewedInTree) {
      this.furnitureCatalogViewedInTree = furnitureCatalogViewedInTree;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_CATALOG_VIEWED_IN_TREE.name(), 
          !furnitureCatalogViewedInTree, furnitureCatalogViewedInTree);
    }
  }

  public boolean isNavigationPanelVisible() {
    return this.navigationPanelVisible;
  }

  public void setNavigationPanelVisible(boolean navigationPanelVisible) {
    if (this.navigationPanelVisible != navigationPanelVisible) {
      this.navigationPanelVisible = navigationPanelVisible;
      this.propertyChangeSupport.firePropertyChange(Property.NAVIGATION_PANEL_VISIBLE.name(), 
          !navigationPanelVisible, navigationPanelVisible);
    }
  }

  public void setAerialViewCenteredOnSelectionEnabled(boolean aerialViewCenteredOnSelectionEnabled) {
    if (aerialViewCenteredOnSelectionEnabled != this.aerialViewCenteredOnSelectionEnabled) {
      this.aerialViewCenteredOnSelectionEnabled = aerialViewCenteredOnSelectionEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED.name(), 
          !aerialViewCenteredOnSelectionEnabled, aerialViewCenteredOnSelectionEnabled);
    }
  }

  public boolean isAerialViewCenteredOnSelectionEnabled() {
    return this.aerialViewCenteredOnSelectionEnabled;
  }

  public boolean isMagnetismEnabled() {
    return this.magnetismEnabled;
  }

  public void setMagnetismEnabled(boolean magnetismEnabled) {
    if (this.magnetismEnabled != magnetismEnabled) {
      this.magnetismEnabled = magnetismEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.MAGNETISM_ENABLED.name(), 
          !magnetismEnabled, magnetismEnabled);
    }
  }

  public boolean isRulersVisible() {
    return this.rulersVisible;
  }

  public void setRulersVisible(boolean rulersVisible) {
    if (this.rulersVisible != rulersVisible) {
      this.rulersVisible = rulersVisible;
      this.propertyChangeSupport.firePropertyChange(Property.RULERS_VISIBLE.name(), 
          !rulersVisible, rulersVisible);
    }
  }
  
  public boolean isGridVisible() {
    return this.gridVisible;
  }

  public void setGridVisible(boolean gridVisible) {
    if (this.gridVisible != gridVisible) {
      this.gridVisible = gridVisible;
      this.propertyChangeSupport.firePropertyChange(Property.GRID_VISIBLE.name(), 
          !gridVisible, gridVisible);
    }
  }

  public String getDefaultFontName() {
    return this.defaultFontName;
  }

  public void setDefaultFontName(String defaultFontName) {
    if (defaultFontName != this.defaultFontName
        && (defaultFontName == null || !defaultFontName.equals(this.defaultFontName))) {
      String oldName = this.defaultFontName;
      this.defaultFontName = defaultFontName;
      this.propertyChangeSupport.firePropertyChange(Property.DEFAULT_FONT_NAME.name(), oldName, defaultFontName);
    }
  }

  public boolean isFurnitureViewedFromTop() {
    return this.furnitureViewedFromTop;
  }
  
  public void setFurnitureViewedFromTop(boolean furnitureViewedFromTop) {
    if (this.furnitureViewedFromTop != furnitureViewedFromTop) {
      this.furnitureViewedFromTop = furnitureViewedFromTop;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_VIEWED_FROM_TOP.name(), 
          !furnitureViewedFromTop, furnitureViewedFromTop);
    }
  }

  public boolean isRoomFloorColoredOrTextured() {
    return this.roomFloorColoredOrTextured;
  }

  public void setFloorColoredOrTextured(boolean roomFloorColoredOrTextured) {
    if (this.roomFloorColoredOrTextured != roomFloorColoredOrTextured) {
      this.roomFloorColoredOrTextured = roomFloorColoredOrTextured;
      this.propertyChangeSupport.firePropertyChange(Property.ROOM_FLOOR_COLORED_OR_TEXTURED.name(), 
          !roomFloorColoredOrTextured, roomFloorColoredOrTextured);
    }
  }

  public TextureImage getWallPattern() {
    return this.wallPattern;
  }
  
  public void setWallPattern(TextureImage wallPattern) {
    if (this.wallPattern != wallPattern) {
      TextureImage oldWallPattern = this.wallPattern;
      this.wallPattern = wallPattern;
      this.propertyChangeSupport.firePropertyChange(Property.WALL_PATTERN.name(), 
          oldWallPattern, wallPattern);
    }
  }

  public TextureImage getNewWallPattern() {
    return this.newWallPattern;
  }

  public void setNewWallPattern(TextureImage newWallPattern) {
    if (this.newWallPattern != newWallPattern) {
      TextureImage oldWallPattern = this.newWallPattern;
      this.newWallPattern = newWallPattern;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_PATTERN.name(), 
          oldWallPattern, newWallPattern);
    }
  }
  
  public float getNewWallThickness() {
    return this.newWallThickness;
  }

  public void setNewWallThickness(float newWallThickness) {
    if (this.newWallThickness != newWallThickness) {
      float oldDefaultThickness = this.newWallThickness;
      this.newWallThickness = newWallThickness;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_THICKNESS.name(), 
          oldDefaultThickness, newWallThickness);
    }
  }

  public float getNewWallHeight() {
    return this.newWallHeight;
  }

  public void setNewWallHeight(float newWallHeight) {
    if (this.newWallHeight != newWallHeight) {
      float oldWallHeight = this.newWallHeight;
      this.newWallHeight = newWallHeight;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_HEIGHT.name(), 
          oldWallHeight, newWallHeight);
    }
  }

  public float getNewWallBaseboardThickness() {
    return this.newWallBaseboardThickness;
  }

  public void setNewWallBaseboardThickness(float newWallBaseboardThickness) {
    if (this.newWallBaseboardThickness != newWallBaseboardThickness) {
      float oldThickness = this.newWallBaseboardThickness;
      this.newWallBaseboardThickness = newWallBaseboardThickness;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_SIDEBOARD_THICKNESS.name(), 
          oldThickness, newWallBaseboardThickness);
    }
  }

  public float getNewWallBaseboardHeight() {
    return this.newWallBaseboardHeight;
  }

  public void setNewWallBaseboardHeight(float newWallBaseboardHeight) {
    if (this.newWallBaseboardHeight != newWallBaseboardHeight) {
      float oldHeight = this.newWallBaseboardHeight;
      this.newWallBaseboardHeight = newWallBaseboardHeight;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_SIDEBOARD_HEIGHT.name(), 
          oldHeight, newWallBaseboardHeight);
    }
  }

  public float getNewFloorThickness() {
    return this.newFloorThickness;
  }

  public void setNewFloorThickness(float newFloorThickness) {
    if (this.newFloorThickness != newFloorThickness) {
      float oldFloorThickness = this.newFloorThickness;
      this.newFloorThickness = newFloorThickness;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_FLOOR_THICKNESS.name(), 
          oldFloorThickness, newFloorThickness);
    }
  }

  public boolean isCheckUpdatesEnabled() {
    return this.checkUpdatesEnabled;
  }

  public void setCheckUpdatesEnabled(boolean updatesChecked) {
    if (updatesChecked != this.checkUpdatesEnabled) {
      this.checkUpdatesEnabled = updatesChecked;
      this.propertyChangeSupport.firePropertyChange(Property.CHECK_UPDATES_ENABLED.name(), 
          !updatesChecked, updatesChecked);
    }
  }

  public Long getUpdatesMinimumDate() {
    return this.updatesMinimumDate;
  }

  public void setUpdatesMinimumDate(Long updatesMinimumDate) {
    if (this.updatesMinimumDate != updatesMinimumDate
        && (updatesMinimumDate == null || !updatesMinimumDate.equals(this.updatesMinimumDate))) {
      Long oldUpdatesMinimumDate = this.updatesMinimumDate;
      this.updatesMinimumDate = updatesMinimumDate;
      this.propertyChangeSupport.firePropertyChange(Property.UPDATES_MINIMUM_DATE.name(), 
          oldUpdatesMinimumDate, updatesMinimumDate);
    }
  }

  public int getAutoSaveDelayForRecovery() {
    return this.autoSaveDelayForRecovery;
  }

  public void setAutoSaveDelayForRecovery(int autoSaveDelayForRecovery) {
    if (this.autoSaveDelayForRecovery != autoSaveDelayForRecovery) {
      float oldAutoSaveDelayForRecovery = this.autoSaveDelayForRecovery;
      this.autoSaveDelayForRecovery = autoSaveDelayForRecovery;
      this.propertyChangeSupport.firePropertyChange(Property.AUTO_SAVE_DELAY_FOR_RECOVERY.name(), 
          oldAutoSaveDelayForRecovery, autoSaveDelayForRecovery);
    }
  }
  
  public List<String> getRecentHomes() {
    return Collections.unmodifiableList(this.recentHomes);
  }

  public void setRecentHomes(List<String> recentHomes) {
    if (!recentHomes.equals(this.recentHomes)) {
      List<String> oldRecentHomes = this.recentHomes;
      this.recentHomes = new ArrayList<String>(recentHomes);
      this.propertyChangeSupport.firePropertyChange(Property.RECENT_HOMES.name(), 
          oldRecentHomes, getRecentHomes());
    }
  }

  public int getRecentHomesMaxCount() {
    return 10;
  }

  public int getStoredCamerasMaxCount() {
    return 50;
  }

  public void setActionTipIgnored(String actionKey) {    
    this.propertyChangeSupport.firePropertyChange(Property.IGNORED_ACTION_TIP.name(), null, actionKey);
  }
  
  public boolean isActionTipIgnored(String actionKey) {
    return true;
  }

  public void resetIgnoredActionTips() {    
    this.propertyChangeSupport.firePropertyChange(Property.IGNORED_ACTION_TIP.name(), null, null);
  }

  public TextStyle getDefaultTextStyle(Class<? extends Selectable> selectableClass) {
    if (Room.class.isAssignableFrom(selectableClass)) {
      return DEFAULT_ROOM_TEXT_STYLE;
    } else {
      return DEFAULT_TEXT_STYLE;
    }
  }

  public List<String> getAutoCompletionStrings(String property) {
    List<String> propertyAutoCompletionStrings = this.autoCompletionStrings.get(property);
    if (propertyAutoCompletionStrings != null) {
      return Collections.unmodifiableList(propertyAutoCompletionStrings);
    } else {
      return Collections.emptyList();
    }
  }

  public void addAutoCompletionString(String property, String autoCompletionString) {
    if (autoCompletionString != null 
        && autoCompletionString.length() > 0) {
      List<String> propertyAutoCompletionStrings = this.autoCompletionStrings.get(property);
      if (propertyAutoCompletionStrings == null) {
        propertyAutoCompletionStrings = new ArrayList<String>();
      } else if (!propertyAutoCompletionStrings.contains(autoCompletionString)) {
        propertyAutoCompletionStrings = new ArrayList<String>(propertyAutoCompletionStrings);
      } else {
        return;
      }
      propertyAutoCompletionStrings.add(0, autoCompletionString);
      setAutoCompletionStrings(property, propertyAutoCompletionStrings);
    }
  }
 
  public void setAutoCompletionStrings(String property, List<String> autoCompletionStrings) {
    List<String> propertyAutoCompletionStrings = this.autoCompletionStrings.get(property);
    if (!autoCompletionStrings.equals(propertyAutoCompletionStrings)) {
      this.autoCompletionStrings.put(property, new ArrayList<String>(autoCompletionStrings));
      this.propertyChangeSupport.firePropertyChange(Property.AUTO_COMPLETION_STRINGS.name(), 
          null, property);
    }
  }

  public List<String> getAutoCompletedProperties() {
    if (this.autoCompletionStrings != null) {
      return Arrays.asList(this.autoCompletionStrings.keySet().toArray(new String [this.autoCompletionStrings.size()]));
    } else {
      return Collections.emptyList();
    }
  }

  public List<Integer> getRecentColors() {
    return Collections.unmodifiableList(this.recentColors);
  }

  public void setRecentColors(List<Integer> recentColors) {
    if (!recentColors.equals(this.recentColors)) {
      List<Integer> oldRecentColors = this.recentColors;
      this.recentColors = new ArrayList<Integer>(recentColors);
      this.propertyChangeSupport.firePropertyChange(Property.RECENT_COLORS.name(), 
          oldRecentColors, getRecentColors());
    }
  }

  public List<TextureImage> getRecentTextures() {
    return Collections.unmodifiableList(this.recentTextures);
  }

  public void setRecentTextures(List<TextureImage> recentTextures) {
    if (!recentTextures.equals(this.recentTextures)) {
      List<TextureImage> oldRecentTextures = this.recentTextures;
      this.recentTextures = new ArrayList<TextureImage>(recentTextures);
      this.propertyChangeSupport.firePropertyChange(Property.RECENT_TEXTURES.name(), 
          oldRecentTextures, getRecentTextures());
    }
  }

  public abstract void addLanguageLibrary(String languageLibraryLocation) throws RecorderException;

  public abstract boolean languageLibraryExists(String languageLibraryLocation) throws RecorderException;

  public abstract void addFurnitureLibrary(String furnitureLibraryLocation) throws RecorderException;
 
  public abstract boolean furnitureLibraryExists(String furnitureLibraryLocation) throws RecorderException;

  public abstract void addTexturesLibrary(String texturesLibraryLocation) throws RecorderException;

  public abstract boolean texturesLibraryExists(String texturesLibraryLocation) throws RecorderException;

  public abstract List<Library> getLibraries();

  private static class PrefixedResourceBundle extends ResourceBundle {
    private ResourceBundle resourceBundle;
    private String         keyPrefix;

    public PrefixedResourceBundle(ResourceBundle resourceBundle, 
                                  String keyPrefix) {
      this.resourceBundle = resourceBundle;
      this.keyPrefix = keyPrefix;
    }

    public Locale getLocale() {
      return this.resourceBundle.getLocale();
    }

    protected Object handleGetObject(String key) {
      key = this.keyPrefix + key;
      return this.resourceBundle.getObject(key);
    }    

    public Enumeration<String> getKeys() {
      return this.resourceBundle.getKeys();
    }    
  }
}
