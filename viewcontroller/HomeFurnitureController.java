package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeDoorOrWindow;
import com.eteks.homeview3d.model.HomeFurnitureGroup;
import com.eteks.homeview3d.model.HomeLight;
import com.eteks.homeview3d.model.HomeMaterial;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.UserPreferences;

public class HomeFurnitureController implements Controller {  
  public enum Property {ICON, NAME, NAME_VISIBLE, DESCRIPTION, PRICE, X, Y, ELEVATION, ANGLE_IN_DEGREES, BASE_PLAN_ITEM, 
      WIDTH, DEPTH,  HEIGHT, PROPORTIONAL, COLOR, PAINT, SHININESS, VISIBLE, MODEL_MIRRORED, LIGHT_POWER, 
      RESIZABLE, DEFORMABLE, TEXTURABLE}
    
  public enum FurniturePaint {DEFAULT, COLORED, TEXTURED, MODEL_MATERIALS} 

  public enum FurnitureShininess {DEFAULT, MATT, SHINY} 

  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;
  private final UndoableEditSupport   undoSupport;
  private final PropertyChangeSupport propertyChangeSupport;
  private TextureChoiceController     textureController;
  private ModelMaterialsController    modelMaterialsController;
  private DialogView                  homeFurnitureView;

  private Content            icon;
  private String             name;
  private String             description;
  private BigDecimal         price;
  private Boolean            nameVisible;
  private Float              x;
  private Float              y;
  private Float              elevation;
  private Integer            angleInDegrees;
  private Float              angle;
  private Float              width;
  private Float              proportionalWidth;
  private Float              depth;
  private Float              proportionalDepth;
  private Float              height;
  private Float              proportionalHeight;
  private boolean            proportional;
  private Integer            color;
  private FurniturePaint     paint;
  private FurnitureShininess shininess;
  private Boolean            visible;
  private Boolean            modelMirrored;
  private Boolean            basePlanItem;
  private boolean            basePlanItemEnabled;
  private boolean            lightPowerEditable;
  private Float              lightPower;
  private boolean            resizable;
  private boolean            deformable;
  private boolean            texturable;
  private boolean            visibleEditable;

  public HomeFurnitureController(Home home, 
                                 UserPreferences preferences, 
                                 ViewFactory viewFactory, 
                                 UndoableEditSupport undoSupport) {
    this(home, preferences, viewFactory, null, undoSupport);
  }

  public HomeFurnitureController(Home home, 
                                 UserPreferences preferences, 
                                 ViewFactory viewFactory,
                                 ContentManager  contentManager,
                                 UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.undoSupport = undoSupport;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }

  public TextureChoiceController getTextureController() {
    if (this.textureController == null
        && this.contentManager != null) {
      this.textureController = new TextureChoiceController(
          this.preferences.getLocalizedString(HomeFurnitureController.class, "textureTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
      this.textureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setPaint(FurniturePaint.TEXTURED);
            }
          });
    }
    return this.textureController;
  }

  public ModelMaterialsController getModelMaterialsController() {
    if (this.modelMaterialsController == null
        && this.contentManager != null) {
      this.modelMaterialsController = new ModelMaterialsController(
          this.preferences.getLocalizedString(HomeFurnitureController.class, "modelMaterialsTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
      this.modelMaterialsController.addPropertyChangeListener(ModelMaterialsController.Property.MATERIALS,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setPaint(FurniturePaint.MODEL_MATERIALS);
            }
          });
      
      PropertyChangeListener sizeChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            if (getWidth() != null && getDepth() != null && getHeight() != null) {
              modelMaterialsController.setModelSize(getWidth(), getDepth(), getHeight());
            }
          }
        };
      addPropertyChangeListener(Property.WIDTH, sizeChangeListener);
      addPropertyChangeListener(Property.DEPTH, sizeChangeListener);
      addPropertyChangeListener(Property.HEIGHT, sizeChangeListener);
    }
    return this.modelMaterialsController;
  }

  public DialogView getView() {
    if (this.homeFurnitureView == null) {
      this.homeFurnitureView = this.viewFactory.createHomeFurnitureView(
          this.preferences, this); 
    }
    return this.homeFurnitureView;
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
    List<HomePieceOfFurniture> selectedFurniture = 
        Home.getFurnitureSubList(this.home.getSelectedItems());
    TextureChoiceController textureController = getTextureController();
    ModelMaterialsController modelMaterialsController = getModelMaterialsController();
    if (selectedFurniture.isEmpty()) {
      setIcon(null);
      setName(null);
      setNameVisible(null); 
      setDescription(null);
      setPrice(null);
      setAngleInDegrees(null);
      setX(null);
      setY(null);
      setElevation(null);
      this.basePlanItemEnabled = false;
      setWidth(null, true, false);
      setDepth(null, true, false);
      setHeight(null, true, false);
      setColor(null);
      if (textureController != null) {
        textureController.setTexture(null);
      }
      if (modelMaterialsController != null) {
        modelMaterialsController.setMaterials(null);
        modelMaterialsController.setModel(null);
      }
      setPaint(null);
      setShininess(null);
      this.visibleEditable = false;
      setVisible(null);
      setModelMirrored(null);
      this.lightPowerEditable = false;
      setLightPower(null);
      setResizable(true);
      setDeformable(true);
      setTexturable(true);
      setProportional(false);
    } else {
      HomePieceOfFurniture firstPiece = selectedFurniture.get(0);
      Content icon = firstPiece.getIcon();
      if (icon != null) {
        for (int i = 1; i < selectedFurniture.size(); i++) {
          if (!icon.equals(selectedFurniture.get(i).getIcon())) {
            icon = null;
            break;
          }
        }
      }
      setIcon(icon);
      
      String name = firstPiece.getName();
      if (name != null) {
        for (int i = 1; i < selectedFurniture.size(); i++) {
          if (!name.equals(selectedFurniture.get(i).getName())) {
            name = null;
            break;
          }
        }
      }
      setName(name);
      
      Boolean nameVisible = firstPiece.isNameVisible();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (nameVisible != selectedFurniture.get(i).isNameVisible()) {
          nameVisible = null;
          break;
        }
      }
      setNameVisible(nameVisible);
      
      String description = firstPiece.getDescription();
      if (description != null) {
        for (int i = 1; i < selectedFurniture.size(); i++) {
          if (!description.equals(selectedFurniture.get(i).getDescription())) {
            description = null;
            break;
          }
        }
      }
      setDescription(description);
      
      BigDecimal price = firstPiece.getPrice();
      if (price != null) {
        for (int i = 1; i < selectedFurniture.size(); i++) {
          if (!price.equals(selectedFurniture.get(i).getPrice())) {
            price = null;
            break;
          }
        }
      }
      setPrice(price);
      
      Float angle = firstPiece.getAngle();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (angle.floatValue() != selectedFurniture.get(i).getAngle()) {
          angle = null;
          break;
        }
      }
      setAngle(angle);

      Float x = firstPiece.getX();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (x.floatValue() != selectedFurniture.get(i).getX()) {
          x = null;
          break;
        }
      }
      setX(x);

      Float y = firstPiece.getY();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (y.floatValue() != selectedFurniture.get(i).getY()) {
          y = null;
          break;
        }
      }
      setY(y);

      Float elevation = firstPiece.getElevation();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (elevation.floatValue() != selectedFurniture.get(i).getElevation()) {
          elevation = null;
          break;
        }
      }
      setElevation(elevation);

      boolean basePlanItemEnabled = !firstPiece.isDoorOrWindow();
      for (int i = 1; !basePlanItemEnabled && i < selectedFurniture.size(); i++) {
        if (!selectedFurniture.get(i).isDoorOrWindow()) {
          basePlanItemEnabled = true;
        }
      }
      this.basePlanItemEnabled = basePlanItemEnabled;

      Boolean basePlanItem = !firstPiece.isMovable();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (basePlanItem.booleanValue() != !selectedFurniture.get(i).isMovable()) {
          basePlanItem = null;
          break;
        }
      }
      setBasePlanItem(basePlanItem);

      Float width = firstPiece.getWidth();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (width.floatValue() != selectedFurniture.get(i).getWidth()) {
          width = null;
          break;
        }
      }
      setWidth(width, true, false);

      Float depth = firstPiece.getDepth();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (depth.floatValue() != selectedFurniture.get(i).getDepth()) {
          depth = null;
          break;
        }
      }
      setDepth(depth, true, false);

      Float height = firstPiece.getHeight();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (height.floatValue() != selectedFurniture.get(i).getHeight()) {
          height = null;
          break;
        }
      }
      setHeight(height, true, false);

      List<HomePieceOfFurniture> selectedFurnitureWithoutGroups = getFurnitureWithoutGroups(selectedFurniture);
      HomePieceOfFurniture firstPieceExceptGroup = selectedFurnitureWithoutGroups.get(0);
      Integer color = firstPieceExceptGroup.getColor();
      if (color != null) {
        for (int i = 1; i < selectedFurnitureWithoutGroups.size(); i++) {
          if (!color.equals(selectedFurnitureWithoutGroups.get(i).getColor())) {
            color = null;
            break;
          }
        }
      }
      setColor(color);

      HomeTexture texture = firstPieceExceptGroup.getTexture();
      if (texture != null) {
        for (int i = 1; i < selectedFurnitureWithoutGroups.size(); i++) {
          if (!texture.equals(selectedFurnitureWithoutGroups.get(i).getTexture())) {
            texture = null;
            break;
          }
        }
      }
      if (textureController != null) {
        textureController.setTexture(texture);
      }
     
      HomeMaterial [] modelMaterials = firstPieceExceptGroup.getModelMaterials();
      Content model = firstPieceExceptGroup.getModel();
      if (model != null) {
        for (int i = 1; i < selectedFurnitureWithoutGroups.size(); i++) {
          HomePieceOfFurniture piece = selectedFurnitureWithoutGroups.get(i);
          if (!Arrays.equals(modelMaterials, piece.getModelMaterials())
              || model != piece.getModel()) {
            modelMaterials = null;
            model = null;
            break;
          }
        }
      }
      if (modelMaterialsController != null) {
        modelMaterialsController.setMaterials(modelMaterials);
        modelMaterialsController.setModel(model);
        modelMaterialsController.setModelSize(firstPieceExceptGroup.getWidth(), firstPieceExceptGroup.getDepth(), firstPieceExceptGroup.getHeight());
        modelMaterialsController.setModelRotation(firstPieceExceptGroup.getModelRotation());
        modelMaterialsController.setBackFaceShown(firstPieceExceptGroup.isBackFaceShown());
      }
      
      boolean defaultColorsAndTextures = true;
      for (int i = 0; i < selectedFurnitureWithoutGroups.size(); i++) {
        HomePieceOfFurniture piece = selectedFurnitureWithoutGroups.get(i);
        if (piece.getColor() != null
            || piece.getTexture() != null
            || piece.getModelMaterials() != null) {
          defaultColorsAndTextures = false;
          break;
        }
      }

      if (color != null) {
        setPaint(FurniturePaint.COLORED);
      } else if (texture != null) {
        setPaint(FurniturePaint.TEXTURED);
      } else if (modelMaterials != null) {
        setPaint(FurniturePaint.MODEL_MATERIALS);
      } else if (defaultColorsAndTextures) {
        setPaint(FurniturePaint.DEFAULT);
      } else {
        setPaint(null);
      }

      Float firstPieceShininess = firstPieceExceptGroup.getShininess();
      FurnitureShininess shininess = firstPieceShininess == null 
          ? FurnitureShininess.DEFAULT
          : (firstPieceShininess.floatValue() == 0
              ? FurnitureShininess.MATT
              : FurnitureShininess.SHINY);
      for (int i = 1; i < selectedFurnitureWithoutGroups.size(); i++) {
        HomePieceOfFurniture piece = selectedFurnitureWithoutGroups.get(i);
        if (firstPieceShininess != piece.getShininess()
            || (firstPieceShininess != null && !firstPieceShininess.equals(piece.getShininess()))) {
          shininess = null;
          break;
        }
      }
      setShininess(shininess);

      boolean visibleEditable = true;
      List<HomePieceOfFurniture> homeFurniture = this.home.getFurniture();
      for (HomePieceOfFurniture piece : selectedFurniture) {
        if (!homeFurniture.contains(piece)) {
          visibleEditable = false;
          break;
        }
      }
      this.visibleEditable = visibleEditable;

      if (visibleEditable) {
        Boolean visible = firstPiece.isVisible();
        for (int i = 1; i < selectedFurniture.size(); i++) {
          if (visible != selectedFurniture.get(i).isVisible()) {
            visible = null;
            break;
          }
        }
        setVisible(visible);  
      } else {
        setVisible(null);
      }

      Boolean modelMirrored = firstPiece.isModelMirrored();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (modelMirrored != selectedFurniture.get(i).isModelMirrored()) {
          modelMirrored = null;
          break;
        }
      }
      setModelMirrored(modelMirrored);     

      boolean lightPowerEditable = firstPiece instanceof HomeLight;
      for (int i = 1; lightPowerEditable && i < selectedFurniture.size(); i++) {
        if (!(selectedFurniture.get(i) instanceof HomeLight)) {
          lightPowerEditable = false;
        }
      }
      this.lightPowerEditable = lightPowerEditable;

      if (lightPowerEditable) {
        Float lightPower = ((HomeLight)firstPiece).getPower();
        for (int i = 1; i < selectedFurniture.size(); i++) {
          if (lightPower.floatValue() != ((HomeLight)selectedFurniture.get(i)).getPower()) {
            lightPower = null;
            break;
          }
        }
        setLightPower(lightPower);
      } else {
        setLightPower(null);
      }
      
      Boolean resizable = firstPiece.isResizable();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (resizable.booleanValue() != selectedFurniture.get(i).isResizable()) {
          resizable = null;
          break;
        }
      }
      setResizable(resizable != null && resizable.booleanValue());

      Boolean deformable = firstPiece.isDeformable();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (deformable.booleanValue() != selectedFurniture.get(i).isDeformable()) {
          deformable = null;
          break;
        }
      }
      setDeformable(deformable != null && deformable.booleanValue());
      if (!isDeformable()) {
        setProportional(true);
      }

      Boolean texturable = firstPiece.isTexturable();
      for (int i = 1; i < selectedFurniture.size(); i++) {
        if (texturable.booleanValue() != selectedFurniture.get(i).isTexturable()) {
          texturable = null;
          break;
        }
      }
      setTexturable(texturable == null || texturable.booleanValue());
    }
  }  
  

  private List<HomePieceOfFurniture> getFurnitureWithoutGroups(List<HomePieceOfFurniture> furniture) {
    List<HomePieceOfFurniture> pieces = new ArrayList<HomePieceOfFurniture>();
    for (HomePieceOfFurniture piece : furniture) {
      if (piece instanceof HomeFurnitureGroup) {
        pieces.addAll(getFurnitureWithoutGroups(((HomeFurnitureGroup)piece).getFurniture()));
      } else {
        pieces.add(piece);
      }
    }
    return pieces;
  }

 
  public boolean isPropertyEditable(Property property) {
    switch (property) {
      case DESCRIPTION :
      case PRICE :
        return false;
      case LIGHT_POWER :
        return isLightPowerEditable();
      case VISIBLE :
        return this.visibleEditable;
      default :
        return true;
    }
  }
  

  private void setIcon(Content icon) {
    if (icon != this.icon) {
      Content oldIcon = this.icon;
      this.icon = icon;
      this.propertyChangeSupport.firePropertyChange(Property.ICON.name(), oldIcon, icon);
    }
  }

  
  public Content getIcon() {
    return this.icon;
  }
  
 
  public void setName(String name) {
    if (name != this.name) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  public String getName() {
    return this.name;
  }
  
  
  public void setNameVisible(Boolean nameVisible) {
    if (nameVisible != this.nameVisible) {
      Boolean oldNameVisible = this.nameVisible;
      this.nameVisible = nameVisible;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_VISIBLE.name(), oldNameVisible, nameVisible);
    }
  }
  
  
  public Boolean getNameVisible() {
    return this.nameVisible;  
  }
  
  
  public void setDescription(String description) {
    if (description != this.description) {
      String oldDescription = this.description;
      this.description = description;
      this.propertyChangeSupport.firePropertyChange(Property.DESCRIPTION.name(), oldDescription, description);
    }
  }

  
  public String getDescription() {
    return this.description;
  }
  
  
  public void setPrice(BigDecimal price) {
    if (price != this.price) {
      BigDecimal oldPrice = this.price;
      this.price = price;
      this.propertyChangeSupport.firePropertyChange(Property.PRICE.name(), oldPrice, price);
    }
  }

  
  public BigDecimal getPrice() {
    return this.price;
  }
  
  
  public void setX(Float x) {
    if (x != this.x) {
      Float oldX = this.x;
      this.x = x;
      this.propertyChangeSupport.firePropertyChange(Property.X.name(), oldX, x);
    }
  }

  
  public Float getX() {
    return this.x;
  }
  
  
  public void setY(Float y) {
    if (y != this.y) {
      Float oldY = this.y;
      this.y = y;
      this.propertyChangeSupport.firePropertyChange(Property.Y.name(), oldY, y);
    }
  }

  
  public Float getY() {
    return this.y;
  }
  
  
  public void setElevation(Float elevation) {
    if (elevation != this.elevation) {
      Float oldElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldElevation, elevation);
    }
  }

  
  public Float getElevation() {
    return this.elevation;
  }
  
  
  public void setAngleInDegrees(Integer angleInDegrees) {
    if (angleInDegrees != this.angleInDegrees) {
      Integer oldAngleInDegrees = this.angleInDegrees;
      this.angleInDegrees = angleInDegrees;
      if (this.angleInDegrees == null) {
        this.angle = null;
      } else {
        this.angle = new Float(Math.toRadians(this.angleInDegrees));
      }
      this.propertyChangeSupport.firePropertyChange(Property.ANGLE_IN_DEGREES.name(), oldAngleInDegrees, angleInDegrees);
    }
  }

 
  public Integer getAngleInDegrees() {
    return this.angleInDegrees;
  }
  
 
  public void setAngle(Float angle) {
    if (angle != this.angle) {
      if (angle == null) {
        setAngleInDegrees(null);
      } else {
        setAngleInDegrees((int)(Math.round(Math.toDegrees(angle)) + 360) % 360);
      }      
      this.angle = angle;
    }
  }

 
  public Float getAngle() {
    return this.angle;
  }
  
  
  public boolean isBasePlanItemEnabled() {
    return this.basePlanItemEnabled;
  }
  
  
  public boolean isBasePlanItemEditable() {
    return this.basePlanItemEnabled;
  }
  
  
  public void setBasePlanItem(Boolean basePlanItem) {
    if (basePlanItem != this.basePlanItem) {
      Boolean oldMovable = this.basePlanItem;
      this.basePlanItem = basePlanItem;
      this.propertyChangeSupport.firePropertyChange(Property.BASE_PLAN_ITEM.name(), oldMovable, basePlanItem);
    }
  }

 
  public Boolean getBasePlanItem() {
    return this.basePlanItem;
  }
  
  public void setWidth(Float width) {
    setWidth(width, false, isProportional());
  }

  private void setWidth(Float width, boolean keepProportionalWidthUnchanged, boolean updateDepthAndHeight) {
    Float adjustedWidth = width != null 
        ? Math.max(width, 0.001f)
        : null;
    if (adjustedWidth == width 
        || adjustedWidth != null && adjustedWidth.equals(width)
        || !keepProportionalWidthUnchanged) {
      this.proportionalWidth = width;
    }
    if (adjustedWidth == null && this.width != null
        || adjustedWidth != null && !adjustedWidth.equals(this.width)) {
      Float oldWidth = this.width;
      this.width = adjustedWidth;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, adjustedWidth);
      if (oldWidth != null && adjustedWidth != null && updateDepthAndHeight) {
        float ratio = adjustedWidth / oldWidth;
        if (this.proportionalDepth != null) {
          setDepth(this.proportionalDepth * ratio, true, false);
        }
        if (this.proportionalHeight != null) {
          setHeight(this.proportionalHeight * ratio, true, false);
        }
      }
    }
  }

 
  public Float getWidth() {
    return this.width;
  }
  
  public void setDepth(Float depth) {
    setDepth(depth, false, isProportional());
  }

  private void setDepth(Float depth, boolean keepProportionalDepthUnchanged, boolean updateWidthAndHeight) {
    Float adjustedDepth = depth != null 
        ? Math.max(depth, 0.001f)
        : null;
    if (adjustedDepth == depth 
        || adjustedDepth != null && adjustedDepth.equals(depth)
        || !keepProportionalDepthUnchanged) {
      this.proportionalDepth = depth;
    }
    if (adjustedDepth == null && this.depth != null
        || adjustedDepth != null && !adjustedDepth.equals(this.depth)) {
      Float oldDepth = this.depth;
      this.depth = adjustedDepth;
      this.propertyChangeSupport.firePropertyChange(Property.DEPTH.name(), oldDepth, adjustedDepth);
      if (oldDepth != null && adjustedDepth != null && updateWidthAndHeight) {
        float ratio = adjustedDepth / oldDepth;
        if (this.proportionalWidth != null) {
          setWidth(this.proportionalWidth * ratio, true, false);
        }
        if (this.proportionalHeight != null) {
          setHeight(this.proportionalHeight * ratio, true, false);
        }
      }
    }
  }

  
  public Float getDepth() {
    return this.depth;
  }
  
  
  public void setHeight(Float height) {
    setHeight(height, false, isProportional());
  }

  private void setHeight(Float height, boolean keepProportionalHeightUnchanged, boolean updateWidthAndDepth) {
    Float adjustedHeight = height != null 
        ? Math.max(height, 0.001f)
        : null;
    if (adjustedHeight == height 
        || adjustedHeight != null && adjustedHeight.equals(height)
        || !keepProportionalHeightUnchanged) {
      this.proportionalHeight = height;
    }
    if (adjustedHeight == null && this.height != null
        || adjustedHeight != null && !adjustedHeight.equals(this.height)) {
      Float oldHeight = this.height;
      this.height = adjustedHeight;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, adjustedHeight);
      if (oldHeight != null && adjustedHeight != null && updateWidthAndDepth) {
        float ratio = adjustedHeight / oldHeight;
        if (this.proportionalWidth != null) {
          setWidth(this.proportionalWidth * ratio, true, false);
        }
        if (this.proportionalDepth != null) {
          setDepth(this.proportionalDepth * ratio, true, false);
        }
      }
    }
  }

  
  public Float getHeight() {
    return this.height;
  }
  
  
  public void setProportional(boolean proportional) {
    if (proportional != this.proportional) {
      boolean oldProportional = this.proportional;
      this.proportional = proportional;
      this.propertyChangeSupport.firePropertyChange(Property.PROPORTIONAL.name(), oldProportional, proportional);
    }
  }

  public boolean isProportional() {
    return this.proportional;
  }
  

  public void setColor(Integer color) {
    if (color != this.color) {
      Integer oldColor = this.color;
      this.color = color;
      this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, color);
    }
  }

 
  public Integer getColor() {
    return this.color;
  }
  
 
  public void setPaint(FurniturePaint paint) {
    if (paint != this.paint) {
      FurniturePaint oldPaint = this.paint;
      this.paint = paint;
      this.propertyChangeSupport.firePropertyChange(Property.PAINT.name(), oldPaint, paint);
    }
  }
  
  
  public FurniturePaint getPaint() {
    return this.paint;
  }

 
  public void setShininess(FurnitureShininess shininess) {
    if (shininess != this.shininess) {
      FurnitureShininess oldShininess = this.shininess;
      this.shininess = shininess;
      this.propertyChangeSupport.firePropertyChange(Property.SHININESS.name(), oldShininess, shininess);
    }
  }
  
  
  public FurnitureShininess getShininess() {
    return this.shininess;
  }

 
  public void setVisible(Boolean visible) {
    if (visible != this.visible) {
      Boolean oldVisible = this.visible;
      this.visible = visible;
      this.propertyChangeSupport.firePropertyChange(Property.VISIBLE.name(), oldVisible, visible);
    }
  }

  
  public Boolean getVisible() {
    return this.visible;
  }

 
  public void setModelMirrored(Boolean modelMirrored) {
    if (modelMirrored != this.modelMirrored) {
      Boolean oldModelMirrored = this.modelMirrored;
      this.modelMirrored = modelMirrored;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL_MIRRORED.name(), oldModelMirrored, modelMirrored);
    }
  }

 
  public Boolean getModelMirrored() {
    return this.modelMirrored;
  }
  
  
  public boolean isLightPowerEditable() {
    return this.lightPowerEditable;
  }
  
 
  public Float getLightPower() {
    return this.lightPower;
  }
  
  
  public void setLightPower(Float lightPower) {
    if (lightPower != this.lightPower) {
      Float oldLightPower = this.lightPower;
      this.lightPower = lightPower;
      this.propertyChangeSupport.firePropertyChange(Property.LIGHT_POWER.name(), oldLightPower, lightPower);
    }
  }

  
  private void setResizable(boolean resizable) {
    if (resizable != this.resizable) {
      boolean oldResizable = this.resizable;
      this.resizable = resizable;
      this.propertyChangeSupport.firePropertyChange(Property.RESIZABLE.name(), oldResizable, resizable);
    }
  }

  
  public boolean isResizable() {
    return this.resizable;
  }

  
  private void setDeformable(boolean deformable) {
    if (deformable != this.deformable) {
      boolean oldDeformable = this.deformable;
      this.deformable = deformable;
      this.propertyChangeSupport.firePropertyChange(Property.DEFORMABLE.name(), oldDeformable, deformable);
    }
  }

  
  public boolean isDeformable() {
    return this.deformable;
  }
  
  
  private void setTexturable(boolean texturable) {
    if (texturable != this.texturable) {
      boolean oldTexturable = this.texturable;
      this.texturable = texturable;
      this.propertyChangeSupport.firePropertyChange(Property.TEXTURABLE.name(), oldTexturable, texturable);
    }
  }

  
  public boolean isTexturable() {
    return this.texturable;
  }
  
  
  public void modifyFurniture() {
    List<Selectable> oldSelection = this.home.getSelectedItems(); 
    List<HomePieceOfFurniture> selectedFurniture = Home.getFurnitureSubList(oldSelection);
    if (!selectedFurniture.isEmpty()) {
      String name = getName();
      Boolean nameVisible = getNameVisible();
      String description = getDescription();
      BigDecimal price = getPrice();
      Float x = getX();
      Float y = getY();
      Float elevation = getElevation();
      Float angle = getAngle();
      Boolean basePlanItem = getBasePlanItem();
      Float width = getWidth();
      Float depth = getDepth();
      Float height = getHeight();
      FurniturePaint paint = getPaint();
      Integer color = paint == FurniturePaint.COLORED 
          ? getColor() 
          : null;
      TextureChoiceController textureController = getTextureController();
      HomeTexture texture;
      if (textureController != null && paint == FurniturePaint.TEXTURED) {
        texture = textureController.getTexture();
      } else {
        texture = null;
      }
      ModelMaterialsController modelMaterialsController = getModelMaterialsController();
      HomeMaterial [] modelMaterials;
      if (modelMaterialsController != null && paint == FurniturePaint.MODEL_MATERIALS) {
        modelMaterials = modelMaterialsController.getMaterials();
      } else {
        modelMaterials = null;
      }
      boolean defaultShininess = getShininess() == FurnitureShininess.DEFAULT;
      Float shininess = getShininess() == FurnitureShininess.SHINY
          ? new Float(0.5f)
          : (getShininess() == FurnitureShininess.MATT
              ? new Float(0) : null);
      Boolean visible = getVisible();
      Boolean modelMirrored = getModelMirrored();
      Float lightPower = getLightPower();
      
      ModifiedPieceOfFurniture [] modifiedFurniture = 
          new ModifiedPieceOfFurniture [selectedFurniture.size()]; 
      for (int i = 0; i < modifiedFurniture.length; i++) {
        HomePieceOfFurniture piece = selectedFurniture.get(i);
        if (piece instanceof HomeLight) {
          modifiedFurniture [i] = new ModifiedLight((HomeLight)piece);
        } else if (piece instanceof HomeDoorOrWindow) {
          modifiedFurniture [i] = new ModifiedDoorOrWindow((HomeDoorOrWindow)piece);
        } else if (piece instanceof HomeFurnitureGroup) {
          modifiedFurniture [i] = new ModifiedFurnitureGroup((HomeFurnitureGroup)piece);
        } else {
          modifiedFurniture [i] = new ModifiedPieceOfFurniture(piece);
        }
      }
      doModifyFurniture(modifiedFurniture, name, nameVisible, description, price, x, y, elevation, angle, basePlanItem, width, depth, height, 
          paint, color, texture, modelMaterials, defaultShininess, shininess, visible, modelMirrored, lightPower);
      if (this.undoSupport != null) {
        List<Selectable> newSelection = this.home.getSelectedItems(); 
        UndoableEdit undoableEdit = new FurnitureModificationUndoableEdit(
            this.home, this.preferences, oldSelection, newSelection, modifiedFurniture, 
            name, nameVisible, description, price, x, y, elevation, angle, basePlanItem, width, depth, height, 
            paint, color, texture, modelMaterials, defaultShininess, shininess, visible, modelMirrored, lightPower);
        this.undoSupport.postEdit(undoableEdit);
      }
      if (name != null) {
        this.preferences.addAutoCompletionString("HomePieceOfFurnitureName", name);
      }
      if (description != null) {
        this.preferences.addAutoCompletionString("HomePieceOfFurnitureDescription", description);
      }
    }
  }
  
  
  private static class FurnitureModificationUndoableEdit extends AbstractUndoableEdit {
    private final Home                        home;
    private final UserPreferences             preferences;
    private final ModifiedPieceOfFurniture [] modifiedFurniture;
    private final List<Selectable>            oldSelection;
    private final List<Selectable>            newSelection;
    private final String                      name;
    private final Boolean                     nameVisible;
    private final String                      description;
    private final BigDecimal                  price;
    private final Float                       x;
    private final Float                       y;
    private final Float                       elevation;
    private final Float                       angle;
    private final Boolean                     basePlanItem;
    private final Float                       width;
    private final Float                       depth;
    private final Float                       height;
    private final FurniturePaint              paint;
    private final Integer                     color;
    private final HomeTexture                 texture;
    private final HomeMaterial []             modelMaterials;
    private final boolean                     defaultShininess;
    private final Float                       shininess;
    private final Boolean                     visible;
    private final Boolean                     modelMirrored;
    private final Float                       lightPower;

    private FurnitureModificationUndoableEdit(Home home,
                                              UserPreferences preferences, 
                                              List<Selectable> oldSelection,
                                              List<Selectable> newSelection,
                                              ModifiedPieceOfFurniture [] modifiedFurniture,
                                              String name, Boolean nameVisible, String description, BigDecimal price, 
                                              Float x, Float y, Float elevation, Float angle, Boolean basePlanItem, 
                                              Float width, Float depth, Float height,
                                              FurniturePaint paint, Integer color, HomeTexture texture,
                                              HomeMaterial [] modelMaterials,
                                              boolean defaultShininess, Float shininess,
                                              Boolean visible,
                                              Boolean modelMirrored,
                                              Float lightPower) {
      this.home = home;
      this.preferences = preferences;
      this.oldSelection = oldSelection;
      this.newSelection = newSelection;
      this.modifiedFurniture = modifiedFurniture;
      this.name = name;
      this.nameVisible = nameVisible;
      this.description = description;
      this.price = price;
      this.x = x;
      this.y = y;
      this.elevation = elevation;
      this.angle = angle;
      this.basePlanItem = basePlanItem;
      this.width = width;
      this.depth = depth;
      this.height = height;
      this.paint = paint;
      this.color = color;
      this.texture = texture;
      this.modelMaterials = modelMaterials;
      this.defaultShininess = defaultShininess;
      this.shininess = shininess;
      this.visible = visible;
      this.modelMirrored = modelMirrored;
      this.lightPower = lightPower;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      undoModifyFurniture(this.modifiedFurniture);
      this.home.setSelectedItems(this.oldSelection); 
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      doModifyFurniture(this.modifiedFurniture, 
          this.name, this.nameVisible, this.description, this.price, this.x, this.y, this.elevation, 
          this.angle, this.basePlanItem, this.width, this.depth, this.height, 
          this.paint, this.color, this.texture, this.modelMaterials, 
          this.defaultShininess, this.shininess,
          this.visible, this.modelMirrored, this.lightPower); 
      this.home.setSelectedItems(this.newSelection); 
    }

    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(HomeFurnitureController.class, 
          "undoModifyFurnitureName");
    }
  }

  
  private static void doModifyFurniture(ModifiedPieceOfFurniture [] modifiedFurniture, 
                                        String name, Boolean nameVisible, String description, BigDecimal price, 
                                        Float x, Float y, Float elevation, Float angle, Boolean basePlanItem, 
                                        Float width, Float depth, Float height, 
                                        FurniturePaint paint, Integer color, HomeTexture texture, 
                                        HomeMaterial [] modelMaterials,
                                        boolean defaultShininess, Float shininess,
                                        Boolean visible, Boolean modelMirrored, 
                                        Float lightPower) {
    for (ModifiedPieceOfFurniture modifiedPiece : modifiedFurniture) {
      HomePieceOfFurniture piece = modifiedPiece.getPieceOfFurniture();
      if (name != null) {
        piece.setName(name);
      }
      if (nameVisible != null) {
        piece.setNameVisible(nameVisible);
      }
      if (description != null) {
        piece.setDescription(description);
      }
      if (price != null
          &&!(piece instanceof HomeFurnitureGroup)) {
        piece.setPrice(price);
      }
      if (x != null) {
        piece.setX(x);
      }
      if (y != null) {
        piece.setY(y);
      }
      if (elevation != null) {
        piece.setElevation(elevation); 
      }
      if (angle != null) {
        piece.setAngle(angle);
      }
      if (basePlanItem != null && !piece.isDoorOrWindow()) {
        piece.setMovable(!basePlanItem);
      }
      if (piece.isResizable()) {
        if (width != null) {
          piece.setWidth(width);
        }
        if (depth != null) {
          piece.setDepth(depth);
        }
        if (height != null) {
          piece.setHeight(height);
        }
        if (modelMirrored != null) {
          piece.setModelMirrored(modelMirrored);
        }
      }
      if (piece.isTexturable()) {
        if (paint != null) {
          switch (paint) {
            case DEFAULT :
              piece.setColor(null);
              piece.setTexture(null);
              piece.setModelMaterials(null);
              break;
            case COLORED :
              piece.setColor(color);
              piece.setTexture(null);
              piece.setModelMaterials(null);
              break;
            case TEXTURED :
              piece.setColor(null);
              piece.setTexture(texture);
              piece.setModelMaterials(null);
              break;
            case MODEL_MATERIALS :
              piece.setColor(null);
              piece.setTexture(null);
              piece.setModelMaterials(modelMaterials);
              break;
          }
        }
        if (defaultShininess) {
          piece.setShininess(null);
        } else if (shininess != null) {
          piece.setShininess(shininess);
        }
      }
      if (visible != null) {
        piece.setVisible(visible);
      }
      if (lightPower != null) {
        ((HomeLight)piece).setPower(lightPower);
      }
    }
  }

  
  private static void undoModifyFurniture(ModifiedPieceOfFurniture [] modifiedFurniture) {
    for (ModifiedPieceOfFurniture modifiedPiece : modifiedFurniture) {
      modifiedPiece.reset();
    }
  }

 
  private static class ModifiedPieceOfFurniture {
    private final HomePieceOfFurniture piece;
    private final String               name;
    private final boolean              nameVisible;
    private final String               description;
    private final BigDecimal           price;
    private final float                x;
    private final float                y;
    private final float                elevation;
    private final float                angle;
    private final boolean              movable;
    private final float                width;
    private final float                depth;
    private final float                height;
    private final Integer              color;
    private final HomeTexture          texture;
    private final HomeMaterial []      modelMaterials;
    private final Float                shininess;
    private final boolean              visible;
    private final boolean              modelMirrored;

    public ModifiedPieceOfFurniture(HomePieceOfFurniture piece) {
      this.piece = piece;
      this.name = piece.getName();
      this.nameVisible = piece.isNameVisible();
      this.description = piece.getDescription();
      this.price = piece.getPrice();
      this.x = piece.getX();
      this.y = piece.getY();
      this.elevation = piece.getElevation();
      this.angle = piece.getAngle();
      this.movable = piece.isMovable();
      this.width = piece.getWidth();
      this.depth = piece.getDepth();
      this.height = piece.getHeight();
      this.color = piece.getColor();
      this.texture = piece.getTexture();
      this.modelMaterials = piece.getModelMaterials();
      this.shininess = piece.getShininess();
      this.visible = piece.isVisible();
      this.modelMirrored = piece.isModelMirrored();
    }

    public HomePieceOfFurniture getPieceOfFurniture() {
      return this.piece;
    }
        
    public void reset() {
      this.piece.setName(this.name);
      this.piece.setNameVisible(this.nameVisible);
      this.piece.setDescription(this.description);
      if (!(this.piece instanceof HomeFurnitureGroup)) {
        this.piece.setPrice(this.price);
      }
      this.piece.setX(this.x);
      this.piece.setY(this.y);
      this.piece.setElevation(this.elevation);
      this.piece.setAngle(this.angle);
      this.piece.setMovable(this.movable);
      if (this.piece.isResizable()) {
        this.piece.setWidth(this.width);
        this.piece.setDepth(this.depth);
        this.piece.setHeight(this.height);
        this.piece.setModelMirrored(this.modelMirrored);
      }
      if (this.piece.isTexturable()) {
        this.piece.setColor(this.color);
        this.piece.setTexture(this.texture);
        this.piece.setModelMaterials(this.modelMaterials);
        this.piece.setShininess(this.shininess);
      }
      this.piece.setVisible(this.visible);
    }
  }
  
  
  private static class ModifiedDoorOrWindow extends ModifiedPieceOfFurniture {
    private final boolean boundToWall;
    
    public ModifiedDoorOrWindow(HomeDoorOrWindow doorOrWindow) {
      super(doorOrWindow);
      this.boundToWall = doorOrWindow.isBoundToWall();
    }

    public void reset() {
      super.reset();
      ((HomeDoorOrWindow)getPieceOfFurniture()).setBoundToWall(this.boundToWall);
    }
  }

 
  private static class ModifiedLight extends ModifiedPieceOfFurniture {
    private final float power;
    
    public ModifiedLight(HomeLight light) {
      super(light);
      this.power = light.getPower();
    }

    public void reset() {
      super.reset();
      ((HomeLight)getPieceOfFurniture()).setPower(this.power);
    }
  }

  private static class ModifiedFurnitureGroup extends ModifiedPieceOfFurniture {
    private final float [] groupFurnitureX;
    private final float [] groupFurnitureY;
    private final float [] groupFurnitureWidth;
    private final float [] groupFurnitureDepth;
    private final Integer []     groupFurnitureColor;
    private final HomeTexture [] groupFurnitureTexture;
    private final HomeMaterial [][] groupFurnitureModelMaterials;
    private final Float []          groupFurnitureShininess;
    
    public ModifiedFurnitureGroup(HomeFurnitureGroup group) {
      super(group);
      List<HomePieceOfFurniture> groupFurniture = getGroupFurniture((HomeFurnitureGroup)group);
      this.groupFurnitureX = new float [groupFurniture.size()];
      this.groupFurnitureY = new float [groupFurniture.size()];
      this.groupFurnitureWidth = new float [groupFurniture.size()];
      this.groupFurnitureDepth = new float [groupFurniture.size()];
      this.groupFurnitureColor = new Integer [groupFurniture.size()];
      this.groupFurnitureTexture = new HomeTexture [groupFurniture.size()];
      this.groupFurnitureShininess = new Float [groupFurniture.size()];
      this.groupFurnitureModelMaterials = new HomeMaterial [groupFurniture.size()][];
      for (int i = 0; i < groupFurniture.size(); i++) {
        HomePieceOfFurniture groupPiece = groupFurniture.get(i);
        this.groupFurnitureX [i] = groupPiece.getX();
        this.groupFurnitureY [i] = groupPiece.getY();
        this.groupFurnitureWidth [i] = groupPiece.getWidth();
        this.groupFurnitureDepth [i] = groupPiece.getDepth();
        this.groupFurnitureColor [i] = groupPiece.getColor();
        this.groupFurnitureTexture [i] = groupPiece.getTexture();
        this.groupFurnitureShininess [i] = groupPiece.getShininess();
        this.groupFurnitureModelMaterials [i] = groupPiece.getModelMaterials();
      }
    }

    public void reset() {
      super.reset();
      HomeFurnitureGroup group = (HomeFurnitureGroup)getPieceOfFurniture();
      List<HomePieceOfFurniture> groupFurniture = getGroupFurniture(group);
      for (int i = 0; i < groupFurniture.size(); i++) {
        HomePieceOfFurniture groupPiece = groupFurniture.get(i);
        if (group.isResizable()) {
          groupPiece.setX(this.groupFurnitureX [i]);
          groupPiece.setY(this.groupFurnitureY [i]);
          groupPiece.setWidth(this.groupFurnitureWidth [i]);
          groupPiece.setDepth(this.groupFurnitureDepth [i]);
        }
        if (group.isTexturable()
            && !(groupPiece instanceof HomeFurnitureGroup)) {
          groupPiece.setColor(this.groupFurnitureColor [i]);
          groupPiece.setTexture(this.groupFurnitureTexture [i]);
          groupPiece.setModelMaterials(this.groupFurnitureModelMaterials [i]);
          groupPiece.setShininess(this.groupFurnitureShininess [i]);
        }
      }
    }
    
   
    private List<HomePieceOfFurniture> getGroupFurniture(HomeFurnitureGroup furnitureGroup) {
      List<HomePieceOfFurniture> pieces = new ArrayList<HomePieceOfFurniture>();
      for (HomePieceOfFurniture piece : furnitureGroup.getFurniture()) {
        pieces.add(piece);
        if (piece instanceof HomeFurnitureGroup) {
          pieces.addAll(getGroupFurniture((HomeFurnitureGroup)piece));
        } 
      }
      return pieces;
    }
  }
}
