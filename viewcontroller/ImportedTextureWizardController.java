package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;

import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.TexturesCatalog;
import com.eteks.homeview3d.model.TexturesCategory;
import com.eteks.homeview3d.model.UserPreferences;

public class ImportedTextureWizardController extends WizardController 
                                             implements Controller {
  public enum Property {STEP, IMAGE, NAME, CATEGORY, WIDTH, HEIGHT}

  public enum Step {IMAGE, ATTRIBUTES};
  
  private final CatalogTexture                 texture;
  private final String                         textureName;
  private final UserPreferences                preferences;
  private final ViewFactory                    viewFactory;
  private final ContentManager                 contentManager;
  private final PropertyChangeSupport          propertyChangeSupport;

  private final ImportedTextureWizardStepState textureImageStepState;
  private final ImportedTextureWizardStepState textureAttributesStepState;
  private View                                 stepsView;

  private Step              step;
  private Content           image;
  private String            name;
  private TexturesCategory  category;
  private float             width;
  private float             height;
  
  public ImportedTextureWizardController(UserPreferences preferences,
                                         ViewFactory    viewFactory,
                                         ContentManager contentManager) {
    this(null, null, preferences, viewFactory, contentManager);    
  }
  
  public ImportedTextureWizardController(String textureName,
                                         UserPreferences preferences,
                                         ViewFactory    viewFactory,
                                         ContentManager contentManager) {
    this(null, textureName, preferences, viewFactory, contentManager);    
  }
  
  public ImportedTextureWizardController(CatalogTexture texture,
                                         UserPreferences preferences,
                                         ViewFactory    viewFactory,
                                         ContentManager contentManager) {
    this(texture, null, preferences, viewFactory, contentManager);    
  }
  
  private ImportedTextureWizardController(CatalogTexture texture,
                                          String textureName,
                                          UserPreferences preferences,
                                          ViewFactory    viewFactory,
                                          ContentManager contentManager) {
    super(preferences, viewFactory);
    this.texture = texture;
    this.textureName = textureName;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    setTitle(this.preferences.getLocalizedString(ImportedTextureWizardController.class, 
        texture == null 
          ? "importTextureWizard.title" 
          : "modifyTextureWizard.title"));  
    this.textureImageStepState = new TextureImageStepState();
    this.textureAttributesStepState = new TextureAttributesStepState();
    setStepState(this.textureImageStepState);
  }

  @Override
  public void finish() {
    CatalogTexture newTexture = new CatalogTexture(getName(), getImage(), 
        getWidth(), getHeight(), true);
    TexturesCatalog catalog = this.preferences.getTexturesCatalog();
    if (this.texture != null) {
      catalog.delete(this.texture);
    }
    catalog.add(this.category, newTexture);
  }
  
  public ContentManager getContentManager() {
    return this.contentManager;
  }

  @Override
  protected ImportedTextureWizardStepState getStepState() {
    return (ImportedTextureWizardStepState)super.getStepState();
  }
  
  protected ImportedTextureWizardStepState getTextureImageStepState() {
    return this.textureImageStepState;
  }

  protected ImportedTextureWizardStepState getTextureAttributesStepState() {
    return this.textureAttributesStepState;
  }
 
  protected View getStepsView() {
    if (this.stepsView == null) {
      this.stepsView = this.viewFactory.createImportedTextureWizardStepsView(this.texture, this.textureName, 
          this.preferences, this);
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

  public void setImage(Content image) {
    if (image != this.image) {
      Content oldImage = this.image;
      this.image = image;
      this.propertyChangeSupport.firePropertyChange(Property.IMAGE.name(), oldImage, image);
    }
  }
  
  public Content getImage() {
    return this.image;
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
  
  public TexturesCategory getCategory() {
    return this.category;
  }
  
  public void setCategory(TexturesCategory category) {
    if (category != this.category) {
      TexturesCategory oldCategory = this.category;
      this.category = category;
      this.propertyChangeSupport.firePropertyChange(Property.CATEGORY.name(), oldCategory, category);
    }
  }
  
  public float getWidth() {
    return this.width;
  }
  
  public void setWidth(float width) {
    if (width != this.width) {
      float oldWidth = this.width;
      this.width = width;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, width);
    }
  }

  public float getHeight() {
    return this.height;
  }

  public void setHeight(float height) {
    if (height != this.height) {
      float oldHeight = this.height;
      this.height = height;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, height);
    }
  }

  public boolean isTextureNameValid() {
    return this.name != null
        && this.name.length() > 0
        && this.category != null;
  }

  protected abstract class ImportedTextureWizardStepState extends WizardControllerStepState {
    private URL icon = ImportedTextureWizardController.class.getResource("resources/importedTextureWizard.png");
    
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
   
  private class TextureImageStepState extends ImportedTextureWizardStepState {
    public TextureImageStepState() {
      ImportedTextureWizardController.this.addPropertyChangeListener(Property.IMAGE, 
          new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent evt) {
                setNextStepEnabled(getImage() != null);
              }
            });
    }
    
    @Override
    public void enter() {
      super.enter();
      setFirstStep(true);
      setNextStepEnabled(getImage() != null);
    }
    
    @Override
    public Step getStep() {
      return Step.IMAGE;
    }
    
    @Override
    public void goToNextStep() {
      setStepState(getTextureAttributesStepState());
    }
  }

  private class TextureAttributesStepState extends ImportedTextureWizardStepState {
    private PropertyChangeListener widthChangeListener;
    private PropertyChangeListener heightChangeListener;
    private PropertyChangeListener nameAndCategoryChangeListener;
    
    public TextureAttributesStepState() {
      this.widthChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            ImportedTextureWizardController.this.removePropertyChangeListener(Property.HEIGHT, heightChangeListener);
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setHeight(getHeight() * ratio);
            ImportedTextureWizardController.this.addPropertyChangeListener(Property.HEIGHT, heightChangeListener);
          }
        };
      this.heightChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            ImportedTextureWizardController.this.removePropertyChangeListener(Property.WIDTH, widthChangeListener);
            float ratio = (Float)ev.getNewValue() / (Float)ev.getOldValue();
            setWidth(getWidth() * ratio); 
            ImportedTextureWizardController.this.addPropertyChangeListener(Property.WIDTH, widthChangeListener);
          }
        };
      this.nameAndCategoryChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            setNextStepEnabled(isTextureNameValid());
          }
        };
    }
    
    @Override
    public void enter() {
      super.enter();
      setLastStep(true);
      
      ImportedTextureWizardController.this.addPropertyChangeListener(Property.WIDTH, this.widthChangeListener);
      ImportedTextureWizardController.this.addPropertyChangeListener(Property.HEIGHT, this.heightChangeListener);
      ImportedTextureWizardController.this.addPropertyChangeListener(Property.NAME, this.nameAndCategoryChangeListener);
      ImportedTextureWizardController.this.addPropertyChangeListener(Property.CATEGORY, this.nameAndCategoryChangeListener);
      
      setNextStepEnabled(isTextureNameValid());
    }
    
    @Override
    public Step getStep() {
      return Step.ATTRIBUTES;
    }
    
    @Override
    public void goBackToPreviousStep() {
      setStepState(getTextureImageStepState());
    }
    
    @Override
    public void exit() {
      ImportedTextureWizardController.this.removePropertyChangeListener(Property.WIDTH, this.widthChangeListener);
      ImportedTextureWizardController.this.removePropertyChangeListener(Property.HEIGHT, this.heightChangeListener);
      ImportedTextureWizardController.this.removePropertyChangeListener(Property.NAME, this.nameAndCategoryChangeListener);
      ImportedTextureWizardController.this.removePropertyChangeListener(Property.CATEGORY, this.nameAndCategoryChangeListener);
    }
  }
}
