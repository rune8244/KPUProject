package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.TextureImage;
import com.eteks.homeview3d.model.UserPreferences;

public class TextureChoiceController implements Controller {
  public enum Property {TEXTURE}

  private static final int MAX_RECENT_TEXTURES = 15;

  private final String                title;
  private final boolean               rotationSupported;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;  
  private final PropertyChangeSupport propertyChangeSupport;
  private TextureChoiceView           textureChoiceView;
  
  private HomeTexture           texture;

  public TextureChoiceController(String title, 
                                 UserPreferences preferences,
                                 ViewFactory    viewFactory,
                                 ContentManager contentManager) {
    this(title, true, preferences, viewFactory, contentManager);
  }

  public TextureChoiceController(String title, 
                                 boolean rotationSupported,
                                 UserPreferences preferences,
                                 ViewFactory    viewFactory,
                                 ContentManager contentManager) {
    this.title = title;
    this.rotationSupported = rotationSupported;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
  }

  public TextureChoiceView getView() {
    if (this.textureChoiceView == null) {
      this.textureChoiceView = this.viewFactory.createTextureChoiceView(this.preferences, this);
    }
    return this.textureChoiceView;
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  public void setTexture(HomeTexture texture) {
    if (this.texture != texture
        && (texture == null || !texture.equals(this.texture))) {
      HomeTexture oldTexture = this.texture;
      this.texture = texture;
      this.propertyChangeSupport.firePropertyChange(Property.TEXTURE.name(), oldTexture, texture);
    }
  }
 
  public HomeTexture getTexture() {
    return this.texture;
  }

  public String getDialogTitle() {
    return this.title;
  }

  public boolean isRotationSupported() {
    return this.rotationSupported;
  }
  
  public void importTexture() {
    new ImportedTextureWizardController(this.preferences, 
        this.viewFactory, this.contentManager).displayView(getView());
  }

  public void importTexture(String textureName) {
    new ImportedTextureWizardController(textureName, this.preferences, 
        this.viewFactory, this.contentManager).displayView(getView());
  }
  
  public void modifyTexture(CatalogTexture texture) {
    new ImportedTextureWizardController(texture, this.preferences, 
        this.viewFactory, this.contentManager).displayView(getView());
  }

  public void deleteTexture(CatalogTexture texture) {
    if (getView().confirmDeleteSelectedCatalogTexture()) {
      this.preferences.getTexturesCatalog().delete(texture);
    }
  }
  
  public void addRecentTexture(TextureImage texture) {
    List<TextureImage> recentTextures = new ArrayList<TextureImage>(this.preferences.getRecentTextures());
    for (int i = 0; i < recentTextures.size(); i++) {
      TextureImage recentTexture = recentTextures.get(i);
      if (recentTexture.getImage().equals(texture.getImage())) {
        if (i == 0) {
          return;
        } else {
          recentTextures.remove(i);
          break;
        }
      }
    }
    recentTextures.add(0, texture);
    while (recentTextures.size() > MAX_RECENT_TEXTURES) {
      recentTextures.remove(recentTextures.size() - 1);
    }
    this.preferences.setRecentTextures(recentTextures);     
  }
}
