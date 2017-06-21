package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URL;

import com.eteks.homeview3d.model.UserPreferences;

public abstract class WizardController implements Controller {
  public enum Property {BACK_STEP_ENABLED, NEXT_STEP_ENABLED, LAST_STEP, 
      STEP_VIEW, STEP_ICON, TITLE, RESIZABLE}
  
  private final UserPreferences        preferences;
  private final ViewFactory            viewFactory;
  private final PropertyChangeSupport  propertyChangeSupport;
  private final PropertyChangeListener stepStatePropertyChangeListener;
  
  private DialogView                   wizardView;
  private WizardControllerStepState    stepState;

  private boolean backStepEnabled;
  private boolean nextStepEnabled;
  private boolean lastStep;
  private View    stepView;
  private URL     stepIcon;
  private String  title;
  private boolean resizable;

  
  public WizardController(UserPreferences preferences,
                          ViewFactory viewFactory) {
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.stepStatePropertyChangeListener = new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent ev) {
          switch (WizardControllerStepState.Property.valueOf(ev.getPropertyName())) {
            case FIRST_STEP :
              setBackStepEnabled(!stepState.isFirstStep());
              break;
            case LAST_STEP :
              setLastStep(stepState.isLastStep());
              break;
            case NEXT_STEP_ENABLED :
              setNextStepEnabled(stepState.isNextStepEnabled());
              break;
          }
        }
      };
      
    this.propertyChangeSupport = new PropertyChangeSupport(this);
  }
  
  public DialogView getView() {
    if (this.wizardView == null) {
      this.wizardView = this.viewFactory.createWizardView(this.preferences, this);
    }
    return this.wizardView;
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

  private void setBackStepEnabled(boolean backStepEnabled) {
    if (backStepEnabled != this.backStepEnabled) {
      this.backStepEnabled = backStepEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.BACK_STEP_ENABLED.name(), 
          !backStepEnabled, backStepEnabled);
    }
  }
  
  public boolean isBackStepEnabled() {
    return this.backStepEnabled;
  }
  
  private void setNextStepEnabled(boolean nextStepEnabled) {
    if (nextStepEnabled != this.nextStepEnabled) {
      this.nextStepEnabled = nextStepEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.NEXT_STEP_ENABLED.name(), 
          !nextStepEnabled, nextStepEnabled);
    }
  }
  
  public boolean isNextStepEnabled() {
    return this.nextStepEnabled;
  }
  
  private void setLastStep(boolean lastStep) {
    if (lastStep != this.lastStep) {
      this.lastStep = lastStep;
      this.propertyChangeSupport.firePropertyChange(Property.LAST_STEP.name(), !lastStep, lastStep);
    }
  }
  
  public boolean isLastStep() {
    return this.lastStep;
  }

  private void setStepView(View stepView) {
    if (stepView != this.stepView) {
      View oldStepView = this.stepView;
      this.stepView = stepView;
      this.propertyChangeSupport.firePropertyChange(Property.STEP_VIEW.name(), oldStepView, stepView);
    }
  }
  
  public View getStepView() {
    return this.stepView;
  }
  
  private void setStepIcon(URL stepIcon) {
    if (stepIcon != this.stepIcon) {
      URL oldStepIcon = this.stepIcon;
      this.stepIcon = stepIcon;
      this.propertyChangeSupport.firePropertyChange(Property.STEP_ICON.name(), oldStepIcon, stepIcon);
    }
  }
  
  public URL getStepIcon() {
    return this.stepIcon;
  }
  
  public void setTitle(String title) {
    if (title != this.title) {
      String oldTitle = this.title;
      this.title = title;
      this.propertyChangeSupport.firePropertyChange(Property.TITLE.name(), oldTitle, title);
    }
  }
  
  public String getTitle() {
    return this.title;
  }
  
  public void setResizable(boolean resizable) {
    if (resizable != this.resizable) {
      this.resizable = resizable;
      this.propertyChangeSupport.firePropertyChange(Property.RESIZABLE.name(), !resizable, resizable);
    }
  }
  
  public boolean isResizable() {
    return this.resizable;
  }
  
  protected void setStepState(WizardControllerStepState stepState) {
    if (this.stepState != null) {
      this.stepState.exit();
      this.stepState.removePropertyChangeListener(this.stepStatePropertyChangeListener);
    } 
    this.stepState = stepState;
    
    setBackStepEnabled(!stepState.isFirstStep());
    setNextStepEnabled(stepState.isNextStepEnabled());
    setStepView(stepState.getView());
    setStepIcon(stepState.getIcon());
    setLastStep(stepState.isLastStep());
    
    this.stepState.addPropertyChangeListener(this.stepStatePropertyChangeListener);
    this.stepState.enter();
  }
  
  protected WizardControllerStepState getStepState() {
    return this.stepState;
  }
  
  public void goToNextStep() {
    this.stepState.goToNextStep();
  }
  
  public void goBackToPreviousStep() {
    this.stepState.goBackToPreviousStep();
  }

  public abstract void finish();
  
  protected static abstract class WizardControllerStepState {
    private enum Property {NEXT_STEP_ENABLED, FIRST_STEP, LAST_STEP}
    
    private PropertyChangeSupport propertyChangeSupport;
    private boolean               firstStep;
    private boolean               lastStep;
    private boolean               nextStepEnabled;

    public WizardControllerStepState() {
      this.propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    private void addPropertyChangeListener(PropertyChangeListener listener) {
      this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    private void removePropertyChangeListener(PropertyChangeListener listener) {
      this.propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void enter() {
    }

    public void exit() {
    }

    public abstract View getView();
    
    public URL getIcon() {
      return null;
    }
    
    public void goBackToPreviousStep() {
    }

    public void goToNextStep() {
    }
    
    public boolean isFirstStep() {
      return this.firstStep;
    }
    
    public void setFirstStep(boolean firstStep) {
      if (firstStep != this.firstStep) {
        this.firstStep = firstStep;
        this.propertyChangeSupport.firePropertyChange(
            Property.FIRST_STEP.name(), !firstStep, firstStep);
      }
    }  
    
    public boolean isLastStep() {
      return this.lastStep;
    }   
    
    public void setLastStep(boolean lastStep) {
      if (lastStep != this.lastStep) {
        this.lastStep = lastStep;
        this.propertyChangeSupport.firePropertyChange(
            Property.LAST_STEP.name(), !lastStep, lastStep);
      }
    }  
    
    public boolean isNextStepEnabled() {
      return this.nextStepEnabled;
    }
    
    public void setNextStepEnabled(boolean nextStepEnabled) {
      if (nextStepEnabled != this.nextStepEnabled) {
        this.nextStepEnabled = nextStepEnabled;
        this.propertyChangeSupport.firePropertyChange(
            Property.NEXT_STEP_ENABLED.name(), !nextStepEnabled, nextStepEnabled);
      }
    }  
  }
}
