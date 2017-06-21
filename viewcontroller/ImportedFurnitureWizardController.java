package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.CatalogDoorOrWindow;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.FurnitureCategory;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.Sash;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.UserPreferences;

public class ImportedFurnitureWizardController extends WizardController 
                                               implements Controller {
  public enum Property {STEP, NAME, MODEL, WIDTH, DEPTH, HEIGHT, ELEVATION, MOVABLE, 
      DOOR_OR_WINDOW, COLOR, CATEGORY, BACK_FACE_SHOWN, MODEL_ROTATION, STAIRCASE_CUT_OUT_SHAPE,
      ICON_YAW, PROPORTIONAL}

  public enum Step {MODEL, ROTATION, ATTRIBUTES, ICON};
  
  private final Home                             home;
  private final CatalogPieceOfFurniture          piece;
  private final String                           modelName;
  private final UserPreferences                  preferences;
  private final FurnitureController              furnitureController;
  private final ContentManager                   contentManager;
  private final UndoableEditSupport              undoSupport;
  private final PropertyChangeSupport            propertyChangeSupport;

  private final ImportedFurnitureWizardStepState furnitureModelStepState;
  private final ImportedFurnitureWizardStepState furnitureOrientationStepState;
  private final ImportedFurnitureWizardStepState furnitureAttributesStepState;
  private final ImportedFurnitureWizardStepState furnitureIconStepState;
  private ImportedFurnitureWizardStepsView       stepsView;
  
  private Step                             step;
  private String                           name;
  private Content                          model;
  private float                            width;
  private float                            proportionalWidth;
  private float                            depth;
  private float                            proportionalDepth;
  private float                            height;
  private float                            proportionalHeight;
  private float                            elevation;
  private boolean                          movable;
  private boolean                          doorOrWindow;
  private String                           staircaseCutOutShape;
  private Integer                          color;
  private FurnitureCategory                category;
  private boolean                          backFaceShown;
  private float [][]                       modelRotation;
  private float                            iconYaw;
  private boolean                          proportional;
  private final ViewFactory viewFactory;
  
  public ImportedFurnitureWizardController(UserPreferences preferences,
                                           ViewFactory    viewFactory,
                                           ContentManager contentManager) {
    this(null, null, null, preferences, null, viewFactory, contentManager, null);
  }
  
  public ImportedFurnitureWizardController(String modelName,
                                           UserPreferences preferences,
                                           ViewFactory    viewFactory,
                                           ContentManager contentManager) {
    this(null, null, modelName, preferences, null, viewFactory, contentManager, null);
  }
 
  public ImportedFurnitureWizardController(CatalogPieceOfFurniture piece, 
                                           UserPreferences preferences,
                                           ViewFactory    viewFactory,
                                           ContentManager contentManager) {
    this(null, piece, null, preferences, null, viewFactory, contentManager, null);
  }
  
  public ImportedFurnitureWizardController(Home home, 
                                           UserPreferences preferences,
                                           FurnitureController furnitureController,
                                           ViewFactory    viewFactory,
                                           ContentManager contentManager,
                                           UndoableEditSupport undoSupport) {
    this(home, null, null, preferences, furnitureController, viewFactory, contentManager, undoSupport);
  }
  
  public ImportedFurnitureWizardController(Home home,
                                           String modelName,
                                           UserPreferences preferences,
                                           FurnitureController furnitureController,
                                           ViewFactory    viewFactory,
                                           ContentManager contentManager,
                                           UndoableEditSupport undoSupport) {
    this(home, null, modelName, preferences, furnitureController, viewFactory, contentManager, undoSupport);
  }
  
   private ImportedFurnitureWizardController(Home home, 
                                            CatalogPieceOfFurniture piece,
                                            String modelName,
                                            UserPreferences preferences,
                                            FurnitureController furnitureController,
                                            ViewFactory    viewFactory,
                                            ContentManager contentManager,
                                            UndoableEditSupport undoSupport) {
    super(preferences, viewFactory);
    this.home = home;
    this.piece = piece;
    this.modelName = modelName;
    this.preferences = preferences;
    this.furnitureController = furnitureController;
    this.viewFactory = viewFactory;
    this.undoSupport = undoSupport;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    setTitle(this.preferences.getLocalizedString(ImportedFurnitureWizardController.class,
        piece == null 
            ? "importFurnitureWizard.title" 
            : "modifyFurnitureWizard.title"));  
    this.furnitureModelStepState = new FurnitureModelStepState();
    this.furnitureOrientationStepState = new FurnitureOrientationStepState();
    this.furnitureAttributesStepState = new FurnitureAttributesStepState();
    this.furnitureIconStepState = new FurnitureIconStepState();
    setStepState(this.furnitureModelStepState);
  }

  @Override
  public void finish() {
    CatalogPieceOfFurniture newPiece;
    if (isDoorOrWindow()) {
      newPiece = new CatalogDoorOrWindow(getName(), getIcon(), getModel(), 
          getWidth(), getDepth(), getHeight(), getElevation(), 
          isMovable(), 1, 0, new Sash [0], getColor(), 
          getModelRotation(), isBackFaceShown(), 
          getIconYaw(), isProportional());
    } else {
      newPiece = new CatalogPieceOfFurniture(getName(), getIcon(), getModel(), 
          getWidth(), getDepth(), getHeight(), getElevation(), 
          isMovable(), getStaircaseCutOutShape(), 
          getColor(), getModelRotation(), isBackFaceShown(), 
          getIconYaw(), isProportional());
    }
    
    if (this.home != null) {
      addPieceOfFurniture(this.furnitureController.createHomePieceOfFurniture(newPiece));
    }
    FurnitureCatalog catalog = this.preferences.getFurnitureCatalog();
    if (this.piece != null) {
      catalog.delete(this.piece);
    }
    if (this.category != null) {
      catalog.add(this.category, newPiece);
    }
  }
  
  public void addPieceOfFurniture(HomePieceOfFurniture piece) {
    boolean basePlanLocked = this.home.isBasePlanLocked();
    boolean allLevelsSelection = this.home.isAllLevelsSelection();
    List<Selectable> oldSelection = this.home.getSelectedItems(); 
    int pieceIndex = this.home.getFurniture().size();
    
    this.home.addPieceOfFurniture(piece, pieceIndex);
    this.home.setSelectedItems(Arrays.asList(piece)); 
    if (!piece.isMovable() && basePlanLocked) {
      this.home.setBasePlanLocked(false);
    }
    this.home.setAllLevelsSelection(false);
    if (this.undoSupport != null) {
      UndoableEdit undoableEdit = new PieceOfFurnitureImportationUndoableEdit(
          this.home, this.preferences, oldSelection, basePlanLocked, allLevelsSelection,
          piece, pieceIndex);
      this.undoSupport.postEdit(undoableEdit);
    }
  }
  
  private static class PieceOfFurnitureImportationUndoableEdit extends AbstractUndoableEdit {
    private final Home                 home;
    private final UserPreferences      preferences;
    private final List<Selectable>     oldSelection;
    private final boolean              oldBasePlanLocked;
    private final boolean              oldAllLevelsSelection;
    private final HomePieceOfFurniture piece;
    private final int                  pieceIndex;

    private PieceOfFurnitureImportationUndoableEdit(Home home,
                                                    UserPreferences preferences, 
                                                    List<Selectable> oldSelection,
                                                    boolean oldBasePlanLocked,
                                                    boolean oldAllLevelsSelection, 
                                                    HomePieceOfFurniture piece, 
                                                    int pieceIndex) {
      this.home = home;
      this.preferences = preferences;
      this.oldSelection = oldSelection;
      this.oldBasePlanLocked = oldBasePlanLocked;
      this.oldAllLevelsSelection = oldAllLevelsSelection;
      this.piece = piece;
      this.pieceIndex = pieceIndex;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      this.home.deletePieceOfFurniture(this.piece);
      this.home.setSelectedItems(this.oldSelection);
      this.home.setAllLevelsSelection(this.oldAllLevelsSelection);
      this.home.setBasePlanLocked(this.oldBasePlanLocked);
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      this.home.addPieceOfFurniture(this.piece, this.pieceIndex);
      this.home.setSelectedItems(Arrays.asList(this.piece)); 
      if (!piece.isMovable() && this.oldBasePlanLocked) {
        this.home.setBasePlanLocked(false);
      }
      this.home.setAllLevelsSelection(false);
    }

    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(ImportedFurnitureWizardController.class, 
          "undoImportFurnitureName");
    }
  }

  public ContentManager getContentManager() {
    return this.contentManager;
  }

  @Override
  protected ImportedFurnitureWizardStepState getStepState() {
    return (ImportedFurnitureWizardStepState)super.getStepState();
  }
  
  protected ImportedFurnitureWizardStepState getFurnitureModelStepState() {
    return this.furnitureModelStepState;
  }

  protected ImportedFurnitureWizardStepState getFurnitureOrientationStepState() {
    return this.furnitureOrientationStepState;
  }

  protected ImportedFurnitureWizardStepState getFurnitureAttributesStepState() {
    return this.furnitureAttributesStepState;
  }
 
  protected ImportedFurnitureWizardStepState getFurnitureIconStepState() {
    return this.furnitureIconStepState;
  }
 
  protected ImportedFurnitureWizardStepsView getStepsView() {
    if (this.stepsView == null) {
      this.stepsView = this.viewFactory.createImportedFurnitureWizardStepsView(
          this.piece, this.modelName, this.home != null, this.preferences, this);
    }
    return this.stepsView;
  }

  protected void setStep(Step step) {
    if (step != this.step) {
      Step oldStep = this.step;
      this.step = step;
      this.propertyChangeSupport.firePropertyChange(Property.STEP.name(), oldStep, step);
    }
  }
 
  public Step getStep() {
    return this.step;
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  public Content getModel() {
    return this.model;
  }
  
  public void setModel(Content model) {
    if (model != this.model) {
      Content oldModel = this.model;
      this.model = model;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL.name(), oldModel, model);
    }
  }
  
  public boolean isBackFaceShown() {
    return this.backFaceShown;
  }

  public void setBackFaceShown(boolean backFaceShown) {
    if (backFaceShown != this.backFaceShown) {
      this.backFaceShown = backFaceShown;
      this.propertyChangeSupport.firePropertyChange(Property.BACK_FACE_SHOWN.name(), !backFaceShown, backFaceShown);
    }
  }

  public float [][] getModelRotation() {
    return this.modelRotation;
  }

  public void setModelRotation(float [][] modelRotation) {
    if (modelRotation != this.modelRotation) {
      float [][] oldModelRotation = this.modelRotation;
      this.modelRotation = modelRotation;
      this.propertyChangeSupport.firePropertyChange(Property.MODEL_ROTATION.name(), oldModelRotation, modelRotation);
    }
  }

  public String getName() {
    return this.name;
  }
  
  public void setName(String name) {
    if (name != this.name) {
      String oldName = this.name;
      this.name = name;
      if (this.propertyChangeSupport != null) {
        this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
      }
    }
  }
  
  public float getWidth() {
    return this.width;
  }
 
  public void setWidth(float width) {
    setWidth(width, false);
  }

  private void setWidth(float width, boolean keepProportionalWidthUnchanged) {
    float adjustedWidth = Math.max(width, 0.001f);
    if (adjustedWidth == width || !keepProportionalWidthUnchanged) {
      this.proportionalWidth = width;
    }
    if (adjustedWidth != this.width) {
      float oldWidth = this.width;
      this.width = adjustedWidth;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, adjustedWidth);
    }
  }

  public float getDepth() {
    return this.depth;
  }

  public void setDepth(float depth) {
    setDepth(depth, false);
  }

  private void setDepth(float depth, boolean keepProportionalDepthUnchanged) {
    float adjustedDepth = Math.max(depth, 0.001f);
    if (adjustedDepth == depth || !keepProportionalDepthUnchanged) {
      this.proportionalDepth = depth;
    }
    if (adjustedDepth != this.depth) {
      float oldDepth = this.depth;
      this.depth = adjustedDepth;
      this.propertyChangeSupport.firePropertyChange(Property.DEPTH.name(), oldDepth, adjustedDepth);
    }
  }

  public float getHeight() {
    return this.height;
  }

  public void setHeight(float height) {
    setHeight(height, false);
  }

  private void setHeight(float height, boolean keepProportionalHeightUnchanged) {
    float adjustedHeight = Math.max(height, 0.001f);
    if (adjustedHeight == height || !keepProportionalHeightUnchanged) {
      this.proportionalHeight = height;
    }
    if (adjustedHeight != this.height) {
      float oldHeight = this.height;
      this.height = adjustedHeight;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, adjustedHeight);
    }
  }

  public float getElevation() {
    return this.elevation;
  }
  
  public void setElevation(float elevation) {
    if (elevation != this.elevation) {
      float oldElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldElevation, elevation);
    }
  }
  
  public boolean isMovable() {
    return this.movable;
  }

  public void setMovable(boolean movable) {
    if (movable != this.movable) {
      this.movable = movable;
      this.propertyChangeSupport.firePropertyChange(Property.MOVABLE.name(), !movable, movable);
    }
  }

  public boolean isDoorOrWindow() {
    return this.doorOrWindow;
  }

  public void setDoorOrWindow(boolean doorOrWindow) {
    if (doorOrWindow != this.doorOrWindow) {
      this.doorOrWindow = doorOrWindow;
      this.propertyChangeSupport.firePropertyChange(Property.DOOR_OR_WINDOW.name(), !doorOrWindow, doorOrWindow);
      if (doorOrWindow) {
        setStaircaseCutOutShape(null);
        setMovable(false);
      }
    }
  }

  public String getStaircaseCutOutShape() {
    return this.staircaseCutOutShape;
  }
  
  public void setStaircaseCutOutShape(String staircaseCutOutShape) {
    if (staircaseCutOutShape != this.staircaseCutOutShape) {
      String oldStaircaseCutOutShape = this.staircaseCutOutShape;
      this.staircaseCutOutShape = staircaseCutOutShape;
      if (this.propertyChangeSupport != null) {
        this.propertyChangeSupport.firePropertyChange(Property.STAIRCASE_CUT_OUT_SHAPE.name(), oldStaircaseCutOutShape, staircaseCutOutShape);
      }
      if (this.staircaseCutOutShape != null) {
        setDoorOrWindow(false);
        setMovable(false);
      }
    }
  }
  
  public Integer getColor() {
    return this.color;
  }
  
  public void setColor(Integer color) {
    if (color != this.color) {
      Integer oldColor = this.color;
      this.color = color;
      this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, color);
    }
  }
  
  public FurnitureCategory getCategory() {
    return this.category;
  }
  
  public void setCategory(FurnitureCategory category) {
    if (category != this.category) {
      FurnitureCategory oldCategory = this.category;
      this.category = category;
      this.propertyChangeSupport.firePropertyChange(Property.CATEGORY.name(), oldCategory, category);
    }
  }
  
  private Content getIcon() {
    return getStepsView().getIcon();
  }

  public float getIconYaw() {
    return this.iconYaw;
  }
 
  public void setIconYaw(float iconYaw) {
    if (iconYaw != this.iconYaw) {
      float oldIconYaw = this.iconYaw;
      this.iconYaw = iconYaw;
      this.propertyChangeSupport.firePropertyChange(Property.ICON_YAW.name(), oldIconYaw, iconYaw);
    }
  }
  
  public boolean isProportional() {
    return this.proportional;
  }
 
  public void setProportional(boolean proportional) {
    if (proportional != this.proportional) {
      this.proportional = proportional;
      this.propertyChangeSupport.firePropertyChange(Property.PROPORTIONAL.name(), !proportional, proportional);
    }
  }

  public boolean isPieceOfFurnitureNameValid() {
    return this.name != null
        && this.name.length() > 0;
  }

  protected abstract class ImportedFurnitureWizardStepState extends WizardControllerStepState {
    private URL icon = ImportedFurnitureWizardController.class.getResource("resources/importedFurnitureWizard.png");
    
    public abstract Step getStep();

    @Override
    public void enter() {
      setStep(getStep());
    }
    
    @Override
    public View getView() {
      return getStepsView();
    }    
    
    @Override
    public URL getIcon() {
      return this.icon;
    }
  }
  
  private class FurnitureModelStepState extends ImportedFurnitureWizardStepState {
    private PropertyChangeListener modelChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          setNextStepEnabled(getModel() != null);
        }
      };
      
    @Override
    public void enter() {
      super.enter();
      setFirstStep(true);
      setNextStepEnabled(getModel() != null);
      addPropertyChangeListener(Property.MODEL, this.modelChangeListener);
    }

    @Override
    public Step getStep() {
      return Step.MODEL;
    }
    
    @Override
    public void goToNextStep() {
      setStepState(getFurnitureOrientationStepState());
    }
    
    @Override
    public void exit() {
      removePropertyChangeListener(Property.MODEL, this.modelChangeListener);
    }
  }

  private class FurnitureOrientationStepState extends ImportedFurnitureWizardStepState {
    @Override
    public void enter() {
      super.enter();
      setNextStepEnabled(true);
    }

    @Override
    public Step getStep() {
      return Step.ROTATION;
    }
    
    @Override
    public void goBackToPreviousStep() {
      setStepState(getFurnitureModelStepState());
    }
    
    @Override
    public void goToNextStep() {
      setStepState(getFurnitureAttributesStepState());
    }
  }

  private class FurnitureAttributesStepState extends ImportedFurnitureWizardStepState {
    PropertyChangeListener widthChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.DEPTH, depthChangeListener);
            ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
            
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setDepth(proportionalDepth * ratio, true); 
            setHeight(proportionalHeight * ratio, true);
            
            ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.DEPTH, depthChangeListener);
            ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
          }
        }
      };
    PropertyChangeListener depthChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.WIDTH, widthChangeListener);
            ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
            
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(proportionalWidth * ratio, true); 
            setHeight(proportionalHeight * ratio, true);
            
            ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.WIDTH, widthChangeListener);
            ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
          }
        }
      };
    PropertyChangeListener heightChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (isProportional()) {
            ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.WIDTH, widthChangeListener);
            ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.DEPTH, depthChangeListener);
            
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(proportionalWidth * ratio, true); 
            setDepth(proportionalDepth * ratio, true);
            
            ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.WIDTH, widthChangeListener);
            ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.DEPTH, depthChangeListener);
          }
        }
      };
    PropertyChangeListener nameAndCategoryChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          checkPieceOfFurnitureNameInCategory();
        }
      };
    
    @Override
    public void enter() {
      super.enter();
      ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.WIDTH, this.widthChangeListener);
      ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.DEPTH, this.depthChangeListener);
      ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.HEIGHT, this.heightChangeListener);
      ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.NAME, this.nameAndCategoryChangeListener);
      ImportedFurnitureWizardController.this.addPropertyChangeListener(Property.CATEGORY, this.nameAndCategoryChangeListener);
      checkPieceOfFurnitureNameInCategory();
    }

    private void checkPieceOfFurnitureNameInCategory() {      
      setNextStepEnabled(isPieceOfFurnitureNameValid());
    }

    @Override
    public Step getStep() {
      return Step.ATTRIBUTES;
    }
    
    @Override
    public void goBackToPreviousStep() {
      setStepState(getFurnitureOrientationStepState());
    }

    @Override
    public void goToNextStep() {
      setStepState(getFurnitureIconStepState());
    }
    
    @Override
    public void exit() {
      ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.WIDTH, this.widthChangeListener);
      ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.DEPTH, this.depthChangeListener);
      ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.HEIGHT, this.heightChangeListener);
      ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.NAME, this.nameAndCategoryChangeListener);
      ImportedFurnitureWizardController.this.removePropertyChangeListener(Property.CATEGORY, this.nameAndCategoryChangeListener);
    }
  }

  private class FurnitureIconStepState extends ImportedFurnitureWizardStepState {
    @Override
    public void enter() {
      super.enter();
      setLastStep(true);
      setNextStepEnabled(true);
    }
    
    @Override
    public Step getStep() {
      return Step.ICON;
    }
    
    @Override
    public void goBackToPreviousStep() {
      setStepState(getFurnitureAttributesStepState());
    }
  }
}
