package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Elevatable;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.UserPreferences;

public class LevelController implements Controller {
  public enum Property {VIEWABLE, NAME, ELEVATION, ELEVATION_INDEX, FLOOR_THICKNESS, HEIGHT, LEVELS, SELECT_LEVEL_INDEX}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final UndoableEditSupport   undoSupport;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  homeLevelView;

  private String   name;
  private Boolean  viewable;
  private Float    elevation;
  private Integer  elevationIndex;
  private Float    floorThickness;
  private Float    height;
  private Level [] levels;
  private Integer  selectedLevelIndex;
  
  public LevelController(Home home, 
                         UserPreferences preferences, 
                         ViewFactory viewFactory, 
                         UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.undoSupport = undoSupport;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }

  public DialogView getView() {
    if (this.homeLevelView == null) {
      this.homeLevelView = this.viewFactory.createLevelView(this.preferences, this); 
    }
    return this.homeLevelView;
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
    Level selectedLevel = this.home.getSelectedLevel();
    setLevels(duplicate(this.home.getLevels().toArray(new Level [0])));
    if (selectedLevel == null) {
      setSelectedLevelIndex(null);
      setName(null);
      setViewable(Boolean.TRUE);
      setElevation(null, false);
      setFloorThickness(null);
      setHeight(null);      
      setElevationIndex(null, false);
    } else {
      setSelectedLevelIndex(this.home.getLevels().indexOf(selectedLevel));
      setName(selectedLevel.getName());
      setViewable(selectedLevel.isViewable());
      setElevation(selectedLevel.getElevation(), false);
      setFloorThickness(selectedLevel.getFloorThickness());
      setHeight(selectedLevel.getHeight());
      setElevationIndex(selectedLevel.getElevationIndex(), false);
    }
  }  
  
  private Level [] duplicate(Level[] levels) {
    for (int i = 0; i < levels.length; i++) {
      levels [i] = levels [i].clone();
    }
    return levels;
  }

  public boolean isPropertyEditable(Property property) {
    return true;
  }
  
  public void setName(String name) {
    if (name != this.name) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
      if (this.selectedLevelIndex != null) {
        this.levels [this.selectedLevelIndex].setName(name);
        this.propertyChangeSupport.firePropertyChange(Property.LEVELS.name(), null, this.levels);
      }
    }
  }

  public String getName() {
    return this.name;
  }
  
  public void setViewable(Boolean viewable) {
    if (viewable != this.viewable) {
      Boolean oldViewable = viewable;
      this.viewable = viewable;
      this.propertyChangeSupport.firePropertyChange(Property.VIEWABLE.name(), oldViewable, viewable);
      if (viewable != null && this.selectedLevelIndex != null) {
        this.levels [this.selectedLevelIndex].setViewable(viewable);
        this.propertyChangeSupport.firePropertyChange(Property.LEVELS.name(), null, this.levels);
      }
    }
  }
  
  public Boolean getViewable() {
    return this.viewable;
  }
  
  public void setElevation(Float elevation) {
    setElevation(elevation, true);
  }

  private void setElevation(Float elevation, boolean updateLevels) {
    if (elevation != this.elevation) {
      Float oldElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldElevation, elevation);
      
      if (updateLevels 
          && elevation != null 
          && this.selectedLevelIndex != null) {
        int elevationIndex = updateLevelElevation(this.levels [this.selectedLevelIndex], 
            elevation, Arrays.asList(this.levels));
        setElevationIndex(elevationIndex, false);
        updateLevels();
      }
    }
  }

  private static int updateLevelElevation(Level level, float elevation, List<Level> levels) {
    int levelIndex = levels.size();
    int elevationIndex = 0;
    for (int i = 0; i < levels.size(); i++) {
      Level homeLevel = levels.get(i);
      if (homeLevel == level) {
        levelIndex = i;
      } else {
        if (homeLevel.getElevation() == elevation) {
          elevationIndex = homeLevel.getElevationIndex() + 1;
        } else if (i > levelIndex
            && homeLevel.getElevation() == level.getElevation()) {
          homeLevel.setElevationIndex(homeLevel.getElevationIndex() - 1);
        }
      }
    }
    level.setElevation(elevation);
    level.setElevationIndex(elevationIndex);
    return elevationIndex;
  }
  
  public Float getElevation() {
    return this.elevation;
  }
  
  public void setElevationIndex(Integer elevationIndex) {
    setElevationIndex(elevationIndex, true);
  }

  private void setElevationIndex(Integer elevationIndex, boolean updateLevels) {
    if (elevationIndex != this.elevationIndex) {
      Integer oldElevationIndex = this.elevationIndex;
      this.elevationIndex = elevationIndex;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION_INDEX.name(), oldElevationIndex, elevationIndex);
      
      if (updateLevels 
          && elevationIndex != null
          && this.selectedLevelIndex != null) {
        updateLevelElevationIndex(this.levels [this.selectedLevelIndex], elevationIndex, Arrays.asList(this.levels));
        updateLevels();
      }
    }
  }

  private static void updateLevelElevationIndex(Level level, int elevationIndex, List<Level> levels) {
    float elevationIndexSignum = Math.signum(elevationIndex - level.getElevationIndex());
    for (Level homeLevel : levels) {
      if (homeLevel != level
          && homeLevel.getElevation() == level.getElevation()
          && Math.signum(homeLevel.getElevationIndex() - level.getElevationIndex()) == elevationIndexSignum
          && Math.signum(homeLevel.getElevationIndex() - elevationIndex) != elevationIndexSignum) {
        homeLevel.setElevationIndex(homeLevel.getElevationIndex() - (int)elevationIndexSignum);
      } else if (homeLevel.getElevation() > level.getElevation()) {
        break;
      }
    }
    level.setElevationIndex(elevationIndex);
  }

  private void updateLevels() {
    Home tempHome = new Home();
    Level selectedLevel = this.levels [this.selectedLevelIndex];
    for (Level homeLevel : this.levels) {
      tempHome.addLevel(homeLevel);
    }
    List<Level> updatedLevels = tempHome.getLevels();
    setLevels(updatedLevels.toArray(new Level [updatedLevels.size()]));
    setSelectedLevelIndex(updatedLevels.indexOf(selectedLevel));
  }

  public Integer getElevationIndex() {
    return this.elevationIndex;
  }
    
  public void setFloorThickness(Float floorThickness) {
    if (floorThickness != this.floorThickness) {
      Float oldFloorThickness = this.floorThickness;
      this.floorThickness = floorThickness;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_THICKNESS.name(), oldFloorThickness, floorThickness);
      if (floorThickness != null && this.selectedLevelIndex != null) {
        this.levels [this.selectedLevelIndex].setFloorThickness(floorThickness);
        this.propertyChangeSupport.firePropertyChange(Property.LEVELS.name(), null, this.levels);
      }
    }
  }

  public Float getFloorThickness() {
    return this.floorThickness;
  }
  
  public void setHeight(Float height) {
    if (height != this.height) {
      Float oldHeight = this.height;
      this.height = height;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, height);
      if (height != null && this.selectedLevelIndex != null) {
        this.levels [this.selectedLevelIndex].setHeight(height);
        this.propertyChangeSupport.firePropertyChange(Property.LEVELS.name(), null, this.levels);
      }
    }
  }

  public Float getHeight() {
    return this.height;
  }
  
  private void setLevels(Level [] levels) {
    if (levels != this.levels) {
      Level [] oldLevels = this.levels;
      this.levels = levels;
      this.propertyChangeSupport.firePropertyChange(Property.LEVELS.name(), oldLevels, levels);
    }
  }

  public Level [] getLevels() {
    return this.levels.clone();
  }
  
  private void setSelectedLevelIndex(Integer selectedLevelIndex) {
    if (selectedLevelIndex != this.selectedLevelIndex) {
      Integer oldSelectedLevelIndex = this.selectedLevelIndex;
      this.selectedLevelIndex = selectedLevelIndex;
      this.propertyChangeSupport.firePropertyChange(Property.SELECT_LEVEL_INDEX.name(), oldSelectedLevelIndex, selectedLevelIndex);
    }
  }

  public Integer getSelectedLevelIndex() {
    return this.selectedLevelIndex;
  }
  
  public void modifyLevels() {
    Level selectedLevel = this.home.getSelectedLevel();
    if (selectedLevel != null) {
      List<Selectable> oldSelection = this.home.getSelectedItems(); 
      String name = getName();
      Boolean viewable = getViewable();
      Float elevation = getElevation();
      Float floorThickness = getFloorThickness();
      Float height = getHeight();
      Integer elevationIndex = getElevationIndex();
      
      ModifiedLevel modifiedLevel = new ModifiedLevel(selectedLevel);
      doModifyLevel(home, modifiedLevel, name, viewable, elevation, floorThickness, height, elevationIndex);
      if (this.undoSupport != null) {
        UndoableEdit undoableEdit = new LevelModificationUndoableEdit(
            this.home, this.preferences, oldSelection, modifiedLevel, 
            name, viewable,  elevation, floorThickness, height, elevationIndex);
        this.undoSupport.postEdit(undoableEdit);
      }
      if (name != null) {
        this.preferences.addAutoCompletionString("LevelName", name);
      }
    }
  }
  
  private static class LevelModificationUndoableEdit extends AbstractUndoableEdit {
    private final Home             home;
    private final UserPreferences  preferences;
    private final List<Selectable> oldSelection;
    private final ModifiedLevel    modifiedLevel;
    private final String           name;
    private final Boolean          viewable;
    private final Float            elevation;
    private final Float            floorThickness;
    private final Float            height;
    private final Integer          elevationIndex;

    private LevelModificationUndoableEdit(Home home,
                                          UserPreferences preferences, 
                                          List<Selectable> oldSelection,
                                          ModifiedLevel modifiedLevel, 
                                          String name,
                                          Boolean viewable,
                                          Float elevation,
                                          Float floorThickness,
                                          Float height,
                                          Integer elevationIndex) {
      this.home = home;
      this.preferences = preferences;
      this.oldSelection = oldSelection;
      this.modifiedLevel = modifiedLevel;
      this.name = name;
      this.viewable = viewable;
      this.elevation = elevation;
      this.floorThickness = floorThickness;
      this.height = height;
      this.elevationIndex = elevationIndex;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      undoModifyLevel(this.home, this.modifiedLevel);
      this.home.setSelectedLevel(this.modifiedLevel.getLevel()); 
      this.home.setSelectedItems(this.oldSelection); 
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      this.home.setSelectedLevel(this.modifiedLevel.getLevel()); 
      doModifyLevel(this.home, this.modifiedLevel, this.name, this.viewable, 
          this.elevation, this.floorThickness, this.height, this.elevationIndex); 
    }

    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(LevelController.class, "undoModifyLevelName");
    }
  }

  private static void doModifyLevel(Home home, ModifiedLevel modifiedLevel, 
                                    String name, Boolean viewable, Float elevation, 
                                    Float floorThickness, Float height,
                                    Integer elevationIndex) {
    Level level = modifiedLevel.getLevel();
    if (name != null) {
      level.setName(name);
    }
    if (viewable != null) {
      List<Selectable> selectedItems = home.getSelectedItems();
      level.setViewable(viewable);
      home.setSelectedItems(getViewableSublist(selectedItems));
    }
    if (elevation != null 
        && elevation != level.getElevation()) {
      updateLevelElevation(level, elevation, home.getLevels());
    }
    if (elevationIndex != null) {
      updateLevelElevationIndex(level, elevationIndex, home.getLevels());
    }
    if (!home.getEnvironment().isAllLevelsVisible()) {
      Level selectedLevel = home.getSelectedLevel();
      boolean visible = true;
      for (Level homeLevel : home.getLevels()) {
        homeLevel.setVisible(visible);
        if (homeLevel == selectedLevel) {
          visible = false;
        }
      }
    }
    if (floorThickness != null) {
      level.setFloorThickness(floorThickness);
    }
    if (height != null) {
      level.setHeight(height);
    }
  }
  
  private static List<Selectable> getViewableSublist(List<? extends Selectable> items) {
    List<Selectable> viewableItems = new ArrayList<Selectable>(items.size());
    for (Selectable item : items) {
      if (!(item instanceof Elevatable)
          || ((Elevatable)item).getLevel().isViewable()) {
        viewableItems.add(item);
      }
    }
    return viewableItems;
  }
  
  private static void undoModifyLevel(Home home, ModifiedLevel modifiedLevel) {
    modifiedLevel.reset();
    Level level = modifiedLevel.getLevel();
    if (modifiedLevel.getElevation() != level.getElevation()) {
      updateLevelElevation(level, modifiedLevel.getElevation(), home.getLevels());
    }
    if (modifiedLevel.getElevationIndex() != level.getElevationIndex()) {
      updateLevelElevationIndex(level, modifiedLevel.getElevationIndex(), home.getLevels());
    }
  }

  private static class ModifiedLevel {
    private final Level   level;
    private final String  name;
    private final boolean viewable;
    private final float   elevation;
    private final float   floorThickness;
    private final float   height;
    private final int     elevationIndex;

    public ModifiedLevel(Level level) {
      this.level = level;
      this.name = level.getName();
      this.viewable = level.isViewable();
      this.elevation = level.getElevation();
      this.floorThickness = level.getFloorThickness();
      this.height = level.getHeight();
      this.elevationIndex = level.getElevationIndex();
    }

    public Level getLevel() {
      return this.level;
    }

    public float getElevation() {
      return this.elevation;
    }
    
    public int getElevationIndex() {
      return this.elevationIndex;
    }
    
    public void reset() {
      this.level.setName(this.name);
      this.level.setViewable(this.viewable);
      this.level.setFloorThickness(this.floorThickness);
      this.level.setHeight(this.height);
    }
  }
}
