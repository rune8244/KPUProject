package com.eteks.homeview3d.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.LengthUnit;
import com.eteks.homeview3d.model.Library;
import com.eteks.homeview3d.model.PatternsCatalog;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.TextureImage;
import com.eteks.homeview3d.model.TexturesCatalog;
import com.eteks.homeview3d.model.UserPreferences;

public class DefaultUserPreferences extends UserPreferences {
  public DefaultUserPreferences() {
    this(true, null);
  }
  DefaultUserPreferences(boolean readCatalogs,
                         UserPreferences localizedPreferences) {
    if (localizedPreferences == null) {
      localizedPreferences = this;
    } else {
      setLanguage(localizedPreferences.getLanguage());
    }
    // 디폴트 가구 카탈로그 불러옴
    setFurnitureCatalog(readCatalogs 
        ? new DefaultFurnitureCatalog(localizedPreferences, (File)null) 
        : new FurnitureCatalog());
    // 디폴트 텍스쳐 카탈로그 불러옴
    setTexturesCatalog(readCatalogs 
        ? new DefaultTexturesCatalog(localizedPreferences, (File)null)
        : new TexturesCatalog());
    // 디폴트 패턴 빌드
    List<TextureImage> patterns = new ArrayList<TextureImage>();
    patterns.add(new DefaultPatternTexture("foreground"));
    patterns.add(new DefaultPatternTexture("reversedHatchUp"));
    patterns.add(new DefaultPatternTexture("reversedHatchDown"));
    patterns.add(new DefaultPatternTexture("reversedCrossHatch"));
    patterns.add(new DefaultPatternTexture("background"));
    patterns.add(new DefaultPatternTexture("hatchUp"));
    patterns.add(new DefaultPatternTexture("hatchDown"));
    patterns.add(new DefaultPatternTexture("crossHatch"));
    PatternsCatalog patternsCatalog = new PatternsCatalog(patterns);
    setPatternsCatalog(patternsCatalog);
    setFurnitureCatalogViewedInTree(Boolean.parseBoolean(
        localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "furnitureCatalogViewedInTree")));
    setNavigationPanelVisible(Boolean.parseBoolean(localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "navigationPanelVisible")));  
    setAerialViewCenteredOnSelectionEnabled(Boolean.parseBoolean(getOptionalLocalizedString(localizedPreferences, "aerialViewCenteredOnSelectionEnabled", "false")));
    setUnit(LengthUnit.valueOf(localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "unit").toUpperCase(Locale.ENGLISH)));
    setRulersVisible(Boolean.parseBoolean(localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "rulersVisible")));
    setGridVisible(Boolean.parseBoolean(localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "gridVisible")));
    String osName = System.getProperty("os.name");
    setFurnitureViewedFromTop(Boolean.parseBoolean(getOptionalLocalizedString(localizedPreferences, "furnitureViewedFromTop." + osName, 
        localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "furnitureViewedFromTop"))));
    setFloorColoredOrTextured(Boolean.parseBoolean(getOptionalLocalizedString(localizedPreferences, "roomFloorColoredOrTextured." + osName,
        localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "roomFloorColoredOrTextured"))));
    setWallPattern(patternsCatalog.getPattern(localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "wallPattern")));
    String newWallPattern = localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "wallPattern");
    if (newWallPattern != null) {
      setNewWallPattern(patternsCatalog.getPattern(newWallPattern));
    }
    setNewWallThickness(Float.parseFloat(localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "newWallThickness")));
    setNewWallHeight(Float.parseFloat(localizedPreferences.getLocalizedString(DefaultUserPreferences.class, "newHomeWallHeight")));
    setNewWallBaseboardThickness(Float.parseFloat(getOptionalLocalizedString(localizedPreferences, "newWallBaseboardThickness", "1")));
    setNewWallBaseboardHeight(Float.parseFloat(getOptionalLocalizedString(localizedPreferences, "newWallBaseboardlHeight", "7")));
    setNewFloorThickness(Float.parseFloat(getOptionalLocalizedString(localizedPreferences, "newFloorThickness", "12")));
    setCheckUpdatesEnabled(Boolean.parseBoolean(getOptionalLocalizedString(localizedPreferences, "checkUpdatesEnabled", "false")));
    setAutoSaveDelayForRecovery(Integer.parseInt(getOptionalLocalizedString(localizedPreferences, "autoSaveDelayForRecovery", "0")));
    setCurrency(getOptionalLocalizedString(localizedPreferences, "currency", null)); 
    for (String property : new String [] {"LevelName", "HomePieceOfFurnitureName", "RoomName", "LabelText"}) {
      String autoCompletionStringsList = getOptionalLocalizedString(localizedPreferences, "autoCompletionStrings#" + property, null);
      if (autoCompletionStringsList != null) {
        String [] autoCompletionStrings = autoCompletionStringsList.trim().split(",");
        if (autoCompletionStrings.length > 0) {
          for (int i = 0; i < autoCompletionStrings.length; i++) {
            autoCompletionStrings [i] = autoCompletionStrings [i].trim();
          }
          setAutoCompletionStrings(property, Arrays.asList(autoCompletionStrings));
        }
      }
    }
  }
  
  private String getOptionalLocalizedString(UserPreferences localizedPreferences, 
                                            String   resourceKey,
                                            String   defaultValue) {
    try {
      return localizedPreferences.getLocalizedString(DefaultUserPreferences.class, resourceKey);
    } catch (IllegalArgumentException ex) {
      return defaultValue;
    }
  }

  @Override
  public void write() throws RecorderException {
    throw new UnsupportedOperationException("Default user preferences can't be written");
  }


  @Override
  public boolean languageLibraryExists(String name) throws RecorderException {
    throw new UnsupportedOperationException("Default user preferences can't manage language libraries");
  }


  @Override
  public void addLanguageLibrary(String name) throws RecorderException {
    throw new UnsupportedOperationException("Default user preferences can't manage language libraries");
  }
  

  @Override
  public boolean furnitureLibraryExists(String name) throws RecorderException {
    throw new UnsupportedOperationException("Default user preferences can't manage furniture libraries");
  }


  @Override
  public void addFurnitureLibrary(String name) throws RecorderException {
    throw new UnsupportedOperationException("Default user preferences can't manage furniture libraries");
  }
  
  @Override
  public boolean texturesLibraryExists(String name) throws RecorderException {
    throw new UnsupportedOperationException("Default user preferences can't manage textures libraries");
  }

  @Override
  public void addTexturesLibrary(String name) throws RecorderException {
    throw new UnsupportedOperationException("Default user preferences can't manage textures libraries");
  }


  @Override
  public List<Library> getLibraries() {
    throw new UnsupportedOperationException();
  }
}
