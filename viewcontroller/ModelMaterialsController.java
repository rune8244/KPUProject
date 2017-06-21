package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.HomeMaterial;
import com.eteks.homeview3d.model.UserPreferences;

public class ModelMaterialsController implements Controller {
  public enum Property {MODEL, MATERIALS}

  private final String                title;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;  
  private final PropertyChangeSupport propertyChangeSupport;
  private View                        materialsChoiceView;

  private TextureChoiceController     textureController;

  private Content                     model;
  private float                       modelWidth;
  private float                       modelDepth;
  private float                       modelHeight;
  private float [][]                  modelRotation;
  private boolean                     backFaceShown;
  private HomeMaterial []             materials;

  public ModelMaterialsController(String title, 
                                  UserPreferences preferences,
                                  ViewFactory    viewFactory,
                                  ContentManager contentManager) {
    this.title = title;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
  }

  public View getView() {
    if (this.materialsChoiceView == null) {
      this.materialsChoiceView = this.viewFactory.createModelMaterialsView(this.preferences, this);
    }
    return this.materialsChoiceView;
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  public void setModel(Content model) {
    if (this.model !=  model) {
      Content oldModel = this.model;
      this.model = model;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL.name(), oldModel, model);
    }
  }
  

  public Content getModel() {
    return this.model;
  }

  void setModelRotation(float [][] modelRotation) {
    this.modelRotation = modelRotation;
  }
 
  public float [][] getModelRotation() {
    return this.modelRotation;
  }

  void setModelSize(float width, float depth, float height) {
    this.modelWidth = width;
    this.modelDepth = depth;
    this.modelHeight = height;
  }
  
  public float getModelWidth() {
    return this.modelWidth;
  }
  
  public float getModelDepth() {
    return this.modelDepth;
  }
  
  public float getModelHeight() {
    return this.modelHeight;
  }
  
  void setBackFaceShown(boolean backFaceShown) {
    this.backFaceShown = backFaceShown;
  }

  public boolean isBackFaceShown() {
    return this.backFaceShown;
  }
  
  public void setMaterials(HomeMaterial [] materials) {
    if (!Arrays.equals(this.materials, materials)) {
      HomeMaterial [] oldMaterials = this.materials;
      this.materials = materials;
      this.propertyChangeSupport.firePropertyChange(Property.MATERIALS.name(), oldMaterials, materials);
    }
  }

  public HomeMaterial [] getMaterials() {
    return this.materials;
  }

  public String getDialogTitle() {
    return this.title;
  }
  
  public TextureChoiceController getTextureController() {
    if (this.textureController == null
        && this.contentManager != null) {
      this.textureController = new TextureChoiceController(
          this.preferences.getLocalizedString(ModelMaterialsController.class, "textureTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
    }
    return this.textureController;
  }
}
