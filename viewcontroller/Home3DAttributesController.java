package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.UserPreferences;

public class Home3DAttributesController implements Controller {
  public enum Property {GROUND_COLOR, GROUND_PAINT, SKY_COLOR, SKY_PAINT, LIGHT_COLOR, WALLS_ALPHA, WALLS_TOP_COLOR}
  public enum EnvironmentPaint {COLORED, TEXTURED} 
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;
  private final UndoableEditSupport   undoSupport;
  private TextureChoiceController     groundTextureController;
  private TextureChoiceController     skyTextureController;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  home3DAttributesView;

  private int               groundColor;
  private EnvironmentPaint  groundPaint;
  private int               skyColor;
  private EnvironmentPaint  skyPaint;
  private int               lightColor;
  private float             wallsAlpha;

 
  public Home3DAttributesController(Home home,
                                    UserPreferences preferences,
                                    ViewFactory viewFactory, 
                                    ContentManager contentManager,
                                    UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.undoSupport = undoSupport;    
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }

  
  public TextureChoiceController getGroundTextureController() {
    if (this.groundTextureController == null) {      
      this.groundTextureController = new TextureChoiceController(
          this.preferences.getLocalizedString(Home3DAttributesController.class, "groundTextureTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
      this.groundTextureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setGroundPaint(EnvironmentPaint.TEXTURED);
            }
          });
    }
    return this.groundTextureController;
  }
  
  public TextureChoiceController getSkyTextureController() {
    if (this.skyTextureController == null) {
      this.skyTextureController = new TextureChoiceController(
          this.preferences.getLocalizedString(Home3DAttributesController.class, "skyTextureTitle"), 
          false,
          this.preferences, this.viewFactory, this.contentManager);
      this.skyTextureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setSkyPaint(EnvironmentPaint.TEXTURED);
            }
          });
    }
    return this.skyTextureController;
  }

  public DialogView getView() {
    if (this.home3DAttributesView == null) {
      this.home3DAttributesView = this.viewFactory.createHome3DAttributesView(
          this.preferences, this); 
    }
    return this.home3DAttributesView;
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
    HomeEnvironment homeEnvironment = this.home.getEnvironment();
    setGroundColor(homeEnvironment.getGroundColor());
    HomeTexture groundTexture = homeEnvironment.getGroundTexture();
    getGroundTextureController().setTexture(groundTexture);
    if (groundTexture != null) {
      setGroundPaint(EnvironmentPaint.TEXTURED);
    } else {
      setGroundPaint(EnvironmentPaint.COLORED);
    }
    setSkyColor(homeEnvironment.getSkyColor());
    HomeTexture skyTexture = homeEnvironment.getSkyTexture();
    getSkyTextureController().setTexture(skyTexture);
    if (skyTexture != null) {
      setSkyPaint(EnvironmentPaint.TEXTURED);
    } else {
      setSkyPaint(EnvironmentPaint.COLORED);
    }
    setLightColor(homeEnvironment.getLightColor());
    setWallsAlpha(homeEnvironment.getWallsAlpha());
  }
  
  public void setGroundColor(int groundColor) {
    if (groundColor != this.groundColor) {
      int oldGroundColor = this.groundColor;
      this.groundColor = groundColor;
      this.propertyChangeSupport.firePropertyChange(Property.GROUND_COLOR.name(), oldGroundColor, groundColor);
      
      setGroundPaint(EnvironmentPaint.COLORED);
    }
  }

 
  public int getGroundColor() {
    return this.groundColor;
  }

 
  public void setGroundPaint(EnvironmentPaint groundPaint) {
    if (groundPaint != this.groundPaint) {
      EnvironmentPaint oldGroundPaint = this.groundPaint;
      this.groundPaint = groundPaint;
      this.propertyChangeSupport.firePropertyChange(Property.GROUND_PAINT.name(), oldGroundPaint, groundPaint);
    }
  }

  public EnvironmentPaint getGroundPaint() {
    return this.groundPaint;
  }

  public void setSkyColor(int skyColor) {
    if (skyColor != this.skyColor) {
      int oldSkyColor = this.skyColor;
      this.skyColor = skyColor;
      this.propertyChangeSupport.firePropertyChange(Property.SKY_COLOR.name(), oldSkyColor, skyColor);
    }
  }

  
  public int getSkyColor() {
    return this.skyColor;
  }

  public void setSkyPaint(EnvironmentPaint skyPaint) {
    if (skyPaint != this.skyPaint) {
      EnvironmentPaint oldSkyPaint = this.skyPaint;
      this.skyPaint = skyPaint;
      this.propertyChangeSupport.firePropertyChange(Property.SKY_PAINT.name(), oldSkyPaint, skyPaint);
    }
  }

  public EnvironmentPaint getSkyPaint() {
    return this.skyPaint;
  }


  public void setLightColor(int lightColor) {
    if (lightColor != this.lightColor) {
      int oldLightColor = this.lightColor;
      this.lightColor = lightColor;
      this.propertyChangeSupport.firePropertyChange(Property.LIGHT_COLOR.name(), oldLightColor, lightColor);
    }
  }

  public int getLightColor() {
    return this.lightColor;
  }

  public void setWallsAlpha(float wallsAlpha) {
    if (wallsAlpha != this.wallsAlpha) {
      float oldWallsAlpha = this.wallsAlpha;
      this.wallsAlpha = wallsAlpha;
      this.propertyChangeSupport.firePropertyChange(Property.WALLS_ALPHA.name(), oldWallsAlpha, wallsAlpha);
    }
  }

 
  public float getWallsAlpha() {
    return this.wallsAlpha;
  }

 
  public void modify3DAttributes() {
    int   groundColor = getGroundColor();
    HomeTexture groundTexture = getGroundPaint() == EnvironmentPaint.TEXTURED
        ? getGroundTextureController().getTexture()
        : null;
    int   skyColor = getSkyColor();
    HomeTexture skyTexture = getSkyPaint() == EnvironmentPaint.TEXTURED
        ? getSkyTextureController().getTexture()
        : null;
    int   lightColor  = getLightColor();
    float wallsAlpha = getWallsAlpha();

    HomeEnvironment homeEnvironment = this.home.getEnvironment();
    int   oldGroundColor = homeEnvironment.getGroundColor();
    HomeTexture oldGroundTexture = homeEnvironment.getGroundTexture();
    int   oldSkyColor = homeEnvironment.getSkyColor();
    HomeTexture oldSkyTexture = homeEnvironment.getSkyTexture();
    int   oldLightColor = homeEnvironment.getLightColor();
    float oldWallsAlpha = homeEnvironment.getWallsAlpha();
    
    doModify3DAttributes(home, groundColor, groundTexture, skyColor,
        skyTexture, lightColor, wallsAlpha); 
    if (this.undoSupport != null) {
      UndoableEdit undoableEdit = new Home3DAttributesModificationUndoableEdit(
          this.home, this.preferences,
          oldGroundColor, oldGroundTexture, oldSkyColor,
          oldSkyTexture, oldLightColor, oldWallsAlpha,
          groundColor, groundTexture, skyColor, 
          skyTexture, lightColor, wallsAlpha);
      this.undoSupport.postEdit(undoableEdit);
    }
  }

  
  private static class Home3DAttributesModificationUndoableEdit extends AbstractUndoableEdit {
    private final Home            home;
    private final UserPreferences preferences;
    private final int             oldGroundColor;
    private final HomeTexture     oldGroundTexture;
    private final int             oldSkyColor;
    private final HomeTexture     oldSkyTexture;
    private final int             oldLightColor;
    private final float           oldWallsAlpha;
    private final int             groundColor;
    private final HomeTexture     groundTexture;
    private final int             skyColor;
    private final HomeTexture     skyTexture;
    private final int             lightColor;
    private final float           wallsAlpha;

    private Home3DAttributesModificationUndoableEdit(Home home,
                                                     UserPreferences preferences,
                                                     int oldGroundColor,
                                                     HomeTexture oldGroundTexture,
                                                     int oldSkyColor, 
                                                     HomeTexture oldSkyTexture,
                                                     int oldLightColor,
                                                     float oldWallsAlpha,
                                                     int groundColor,
                                                     HomeTexture groundTexture,
                                                     int skyColor,
                                                     HomeTexture skyTexture,
                                                     int lightColor,
                                                     float wallsAlpha) {
      this.home = home;
      this.preferences = preferences;
      this.oldGroundColor = oldGroundColor;
      this.oldGroundTexture = oldGroundTexture;
      this.oldSkyColor = oldSkyColor;
      this.oldSkyTexture = oldSkyTexture;
      this.oldLightColor = oldLightColor;
      this.oldWallsAlpha = oldWallsAlpha;
      this.groundColor = groundColor;
      this.groundTexture = groundTexture;
      this.skyColor = skyColor;
      this.skyTexture = skyTexture;
      this.lightColor = lightColor;
      this.wallsAlpha = wallsAlpha;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      doModify3DAttributes(this.home, this.oldGroundColor, this.oldGroundTexture, this.oldSkyColor,
          this.oldSkyTexture, this.oldLightColor, this.oldWallsAlpha); 
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      doModify3DAttributes(this.home, this.groundColor, this.groundTexture, this.skyColor,
          this.skyTexture, this.lightColor, this.wallsAlpha); 
    }

    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(
          Home3DAttributesController.class, "undoModify3DAttributesName");
    }
  }

  
  private static void doModify3DAttributes(Home home,
                                           int groundColor, 
                                           HomeTexture groundTexture, 
                                           int skyColor, 
                                           HomeTexture skyTexture, int lightColor, 
                                           float wallsAlpha) {
    HomeEnvironment homeEnvironment = home.getEnvironment();
    homeEnvironment.setGroundColor(groundColor);
    homeEnvironment.setGroundTexture(groundTexture);
    homeEnvironment.setSkyColor(skyColor);
    homeEnvironment.setSkyTexture(skyTexture);
    homeEnvironment.setLightColor(lightColor);
    homeEnvironment.setWallsAlpha(wallsAlpha);
  }
}
