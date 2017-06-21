package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.eteks.homeview3d.model.LengthUnit;
import com.eteks.homeview3d.model.TextureImage;
import com.eteks.homeview3d.model.UserPreferences;

public class UserPreferencesController implements Controller {
  public enum Property {LANGUAGE, UNIT, MAGNETISM_ENABLED, RULERS_VISIBLE, GRID_VISIBLE, DEFAULT_FONT_NAME, 
      FURNITURE_VIEWED_FROM_TOP, ROOM_FLOOR_COLORED_OR_TEXTURED, WALL_PATTERN, NEW_WALL_PATTERN,   
      NEW_WALL_THICKNESS, NEW_WALL_HEIGHT, NEW_FLOOR_THICKNESS, FURNITURE_CATALOG_VIEWED_IN_TREE, 
      NAVIGATION_PANEL_VISIBLE, AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED,
      CHECK_UPDATES_ENABLED, AUTO_SAVE_DELAY_FOR_RECOVERY, AUTO_SAVE_FOR_RECOVERY_ENABLED}
  
  private final UserPreferences         preferences;
  private final ViewFactory             viewFactory;
  private final HomeController          homeController;
  private final PropertyChangeSupport   propertyChangeSupport;
  private DialogView                    userPreferencesView;

  private String                        language;
  private LengthUnit                    unit;
  private boolean                       furnitureCatalogViewedInTree;
  private boolean                       navigationPanelVisible;
  private boolean                       aerialViewCenteredOnSelectionEnabled;
  private boolean                       magnetismEnabled;
  private boolean                       rulersVisible;
  private boolean                       gridVisible;
  private String                        defaultFontName;
  private boolean                       furnitureViewedFromTop;
  private boolean                       roomFloorColoredOrTextured;
  private TextureImage                  wallPattern;
  private TextureImage                  newWallPattern;
  private float                         newWallThickness;
  private float                         newWallHeight;
  private float                         newFloorThickness;
  private boolean                       checkUpdatesEnabled;
  private int                           autoSaveDelayForRecovery;
  private boolean                       autoSaveForRecoveryEnabled;

  public UserPreferencesController(UserPreferences preferences,
                                   ViewFactory viewFactory, 
                                   ContentManager contentManager) {
    this(preferences, viewFactory, contentManager, null);
  }

  public UserPreferencesController(UserPreferences preferences,
                                   ViewFactory viewFactory, 
                                   ContentManager contentManager,
                                   HomeController homeController) {
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.homeController = homeController;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }

  public DialogView getView() {
    if (this.userPreferencesView == null) {
      this.userPreferencesView = this.viewFactory.createUserPreferencesView(this.preferences, this); 
    }
    return this.userPreferencesView;
  }

  public void displayView(View parentView) {
    getView().displayView(parentView);
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  protected void updateProperties() {
    setLanguage(this.preferences.getLanguage());
    setUnit(this.preferences.getLengthUnit());
    setFurnitureCatalogViewedInTree(this.preferences.isFurnitureCatalogViewedInTree());
    setNavigationPanelVisible(this.preferences.isNavigationPanelVisible());
    setAerialViewCenteredOnSelectionEnabled(this.preferences.isAerialViewCenteredOnSelectionEnabled());
    setMagnetismEnabled(this.preferences.isMagnetismEnabled());
    setRulersVisible(this.preferences.isRulersVisible());
    setGridVisible(this.preferences.isGridVisible());
    setDefaultFontName(this.preferences.getDefaultFontName());
    setFurnitureViewedFromTop(this.preferences.isFurnitureViewedFromTop());
    setRoomFloorColoredOrTextured(this.preferences.isRoomFloorColoredOrTextured());
    setWallPattern(this.preferences.getWallPattern());
    setNewWallPattern(this.preferences.getNewWallPattern());
    float minimumLength = getUnit().getMinimumLength();
    float maximumLength = getUnit().getMaximumLength();
    setNewWallThickness(Math.min(Math.max(minimumLength, this.preferences.getNewWallThickness()), maximumLength / 10));
    setNewWallHeight(Math.min(Math.max(minimumLength, this.preferences.getNewWallHeight()), maximumLength));
    setNewFloorThickness(Math.min(Math.max(minimumLength, this.preferences.getNewFloorThickness()), maximumLength / 10));
    setCheckUpdatesEnabled(this.preferences.isCheckUpdatesEnabled());
    setAutoSaveDelayForRecovery(this.preferences.getAutoSaveDelayForRecovery());
    setAutoSaveForRecoveryEnabled(this.preferences.getAutoSaveDelayForRecovery() > 0);
  }  

  public boolean isPropertyEditable(Property property) {
    switch (property) {
      case LANGUAGE :
        return this.preferences.isLanguageEditable();
      default :
        return true;
    }
  }
  
  public void setLanguage(String language) {
    if (language != this.language) {
      String oldLanguage = this.language;
      this.language = language;
      this.propertyChangeSupport.firePropertyChange(Property.LANGUAGE.name(), oldLanguage, language);
    }
  }

  public String getLanguage() {
    return this.language;
  }

  public void setUnit(LengthUnit unit) {
    if (unit != this.unit) {
      LengthUnit oldUnit = this.unit;
      this.unit = unit;
      this.propertyChangeSupport.firePropertyChange(Property.UNIT.name(), oldUnit, unit);
    }
  }

  public LengthUnit getUnit() {
    return this.unit;
  }

  public void setFurnitureCatalogViewedInTree(boolean furnitureCatalogViewedInTree) {
    if (this.furnitureCatalogViewedInTree != furnitureCatalogViewedInTree) {
      this.furnitureCatalogViewedInTree = furnitureCatalogViewedInTree;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_CATALOG_VIEWED_IN_TREE.name(), 
          !furnitureCatalogViewedInTree, furnitureCatalogViewedInTree);
    }
  }
  
  public boolean isFurnitureCatalogViewedInTree() {
    return this.furnitureCatalogViewedInTree;
  }
  
  public void setNavigationPanelVisible(boolean navigationPanelVisible) {
    if (this.navigationPanelVisible != navigationPanelVisible) {
      this.navigationPanelVisible = navigationPanelVisible;
      this.propertyChangeSupport.firePropertyChange(Property.NAVIGATION_PANEL_VISIBLE.name(), 
          !navigationPanelVisible, navigationPanelVisible);
    }
  }
  
  public boolean isNavigationPanelVisible() {
    return this.navigationPanelVisible;
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
  
  public void setMagnetismEnabled(boolean magnetismEnabled) {
    if (magnetismEnabled != this.magnetismEnabled) {
      this.magnetismEnabled = magnetismEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.MAGNETISM_ENABLED.name(), !magnetismEnabled, magnetismEnabled);
    }
  }

  public boolean isMagnetismEnabled() {
    return this.magnetismEnabled;
  }

  public void setRulersVisible(boolean rulersVisible) {
    if (rulersVisible != this.rulersVisible) {
      this.rulersVisible = rulersVisible;
      this.propertyChangeSupport.firePropertyChange(Property.RULERS_VISIBLE.name(), !rulersVisible, rulersVisible);
    }
  }

  public boolean isRulersVisible() {
    return this.rulersVisible;
  }

  public void setGridVisible(boolean gridVisible) {
    if (gridVisible != this.gridVisible) {
      this.gridVisible = gridVisible;
      this.propertyChangeSupport.firePropertyChange(Property.GRID_VISIBLE.name(), !gridVisible, gridVisible);
    }
  }

  public boolean isGridVisible() {
    return this.gridVisible;
  }

  public void setDefaultFontName(String defaultFontName) {
    if (defaultFontName != this.defaultFontName
        && (defaultFontName == null || !defaultFontName.equals(this.defaultFontName))) {
      String oldName = this.defaultFontName;
      this.defaultFontName = defaultFontName;
      this.propertyChangeSupport.firePropertyChange(Property.DEFAULT_FONT_NAME.name(), oldName, defaultFontName);
    }
  }
  
  public String getDefaultFontName() {
    return this.defaultFontName;
  }

  public void setFurnitureViewedFromTop(boolean furnitureViewedFromTop) {
    if (this.furnitureViewedFromTop != furnitureViewedFromTop) {
      this.furnitureViewedFromTop = furnitureViewedFromTop;
      this.propertyChangeSupport.firePropertyChange(Property.FURNITURE_VIEWED_FROM_TOP.name(), 
          !furnitureViewedFromTop, furnitureViewedFromTop);
    }
  }
  
  public boolean isFurnitureViewedFromTop() {
    return this.furnitureViewedFromTop;
  }

  public void setRoomFloorColoredOrTextured(boolean floorTextureVisible) {
    if (this.roomFloorColoredOrTextured != floorTextureVisible) {
      this.roomFloorColoredOrTextured = floorTextureVisible;
      this.propertyChangeSupport.firePropertyChange(Property.ROOM_FLOOR_COLORED_OR_TEXTURED.name(), 
          !floorTextureVisible, floorTextureVisible);
    }
  }

  public boolean isRoomFloorColoredOrTextured() {
    return this.roomFloorColoredOrTextured;
  }
  
  public void setWallPattern(TextureImage wallPattern) {
    if (this.wallPattern != wallPattern) {
      TextureImage oldWallPattern = this.wallPattern;
      this.wallPattern = wallPattern;
      this.propertyChangeSupport.firePropertyChange(Property.WALL_PATTERN.name(), 
          oldWallPattern, wallPattern);
    }
  }

  public TextureImage getWallPattern() {
    return this.wallPattern;
  }
  
  public void setNewWallPattern(TextureImage newWallPattern) {
    if (this.newWallPattern != newWallPattern) {
      TextureImage oldNewWallPattern = this.newWallPattern;
      this.newWallPattern = newWallPattern;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_PATTERN.name(), 
          oldNewWallPattern, newWallPattern);
    }
  }

  public TextureImage getNewWallPattern() {
    return this.newWallPattern;
  }
  
  public void setNewWallThickness(float newWallThickness) {
    if (newWallThickness != this.newWallThickness) {
      float oldNewWallThickness = this.newWallThickness;
      this.newWallThickness = newWallThickness;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_THICKNESS.name(), oldNewWallThickness, newWallThickness);
    }
  }

  public float getNewWallThickness() {
    return this.newWallThickness;
  }

  public void setNewWallHeight(float newWallHeight) {
    if (newWallHeight != this.newWallHeight) {
      float oldNewWallHeight = this.newWallHeight;
      this.newWallHeight = newWallHeight;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_WALL_HEIGHT.name(), oldNewWallHeight, newWallHeight);
    }
  }

  public float getNewWallHeight() {
    return this.newWallHeight;
  }

  public void setNewFloorThickness(float newFloorThickness) {
    if (newFloorThickness != this.newFloorThickness) {
      float oldNewFloorThickness = this.newFloorThickness;
      this.newFloorThickness = newFloorThickness;
      this.propertyChangeSupport.firePropertyChange(Property.NEW_FLOOR_THICKNESS.name(), oldNewFloorThickness, newFloorThickness);
    }
  }

  public float getNewFloorThickness() {
    return this.newFloorThickness;
  }

  public void setCheckUpdatesEnabled(boolean updatesChecked) {
    if (updatesChecked != this.checkUpdatesEnabled) {
      this.checkUpdatesEnabled = updatesChecked;
      this.propertyChangeSupport.firePropertyChange(Property.CHECK_UPDATES_ENABLED.name(), 
          !updatesChecked, updatesChecked);
    }
  }

  public boolean isCheckUpdatesEnabled() {
    return this.checkUpdatesEnabled;
  }

  public void setAutoSaveDelayForRecovery(int autoSaveDelayForRecovery) {
    if (autoSaveDelayForRecovery != this.autoSaveDelayForRecovery) {
      float oldAutoSaveDelayForRecovery = this.autoSaveDelayForRecovery;
      this.autoSaveDelayForRecovery = autoSaveDelayForRecovery;
      this.propertyChangeSupport.firePropertyChange(Property.AUTO_SAVE_DELAY_FOR_RECOVERY.name(), 
          oldAutoSaveDelayForRecovery, autoSaveDelayForRecovery);
    }
  }

  public int getAutoSaveDelayForRecovery() {
    return this.autoSaveDelayForRecovery;
  }

  public void setAutoSaveForRecoveryEnabled(boolean autoSaveForRecoveryEnabled) {
    if (autoSaveForRecoveryEnabled != this.autoSaveForRecoveryEnabled) {
      this.autoSaveForRecoveryEnabled = autoSaveForRecoveryEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.AUTO_SAVE_FOR_RECOVERY_ENABLED.name(), 
          !autoSaveForRecoveryEnabled, autoSaveForRecoveryEnabled);
    }
  }

  public boolean isAutoSaveForRecoveryEnabled() {
    return this.autoSaveForRecoveryEnabled;
  }

  public void checkUpdates() {
    if (this.homeController != null) {
      this.homeController.checkUpdates(false);
    }
  }

  public boolean mayImportLanguageLibrary() {
    return this.homeController != null;
  }
  
  public void importLanguageLibrary() {
    if (this.homeController != null) {
      this.homeController.importLanguageLibrary();
    }
  }

  public void resetDisplayedActionTips() {
    this.preferences.resetIgnoredActionTips();
  }

  public void modifyUserPreferences() {
    this.preferences.setLanguage(getLanguage());
    this.preferences.setUnit(getUnit());
    this.preferences.setFurnitureCatalogViewedInTree(isFurnitureCatalogViewedInTree());
    this.preferences.setNavigationPanelVisible(isNavigationPanelVisible());
    this.preferences.setAerialViewCenteredOnSelectionEnabled(isAerialViewCenteredOnSelectionEnabled());
    this.preferences.setMagnetismEnabled(isMagnetismEnabled());
    this.preferences.setRulersVisible(isRulersVisible());
    this.preferences.setGridVisible(isGridVisible());
    this.preferences.setDefaultFontName(getDefaultFontName());
    this.preferences.setFurnitureViewedFromTop(isFurnitureViewedFromTop());
    this.preferences.setFloorColoredOrTextured(isRoomFloorColoredOrTextured());
    this.preferences.setWallPattern(getWallPattern());
    this.preferences.setNewWallPattern(getNewWallPattern());
    this.preferences.setNewWallThickness(getNewWallThickness());
    this.preferences.setNewWallHeight(getNewWallHeight());
    this.preferences.setNewFloorThickness(getNewFloorThickness());
    this.preferences.setCheckUpdatesEnabled(isCheckUpdatesEnabled());
    this.preferences.setAutoSaveDelayForRecovery(isAutoSaveForRecoveryEnabled()
        ? getAutoSaveDelayForRecovery() : 0);
  }
}
