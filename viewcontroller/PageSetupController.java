package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomePrint;
import com.eteks.homeview3d.model.UserPreferences;

public class PageSetupController implements Controller {
  public enum Property {PRINT}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final UndoableEditSupport   undoSupport;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  pageSetupView;

  private HomePrint print;
  
  public PageSetupController(Home home,
                             UserPreferences preferences,
                             ViewFactory viewFactory, 
                             UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.undoSupport = undoSupport;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    setPrint(home.getPrint());
  }

  public DialogView getView() {
    if (this.pageSetupView == null) {
      this.pageSetupView = this.viewFactory.createPageSetupView(this.preferences, this);
    }
    return this.pageSetupView;
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

  public void setPrint(HomePrint print) {
    if (print != this.print) {
      HomePrint oldPrint = this.print;
      this.print = print;
      this.propertyChangeSupport.firePropertyChange(Property.PRINT.name(), oldPrint, print);
    }
  }

  public HomePrint getPrint() {
    return this.print;
  }

  public void modifyPageSetup() {
    HomePrint oldHomePrint = this.home.getPrint();
    HomePrint homePrint = getPrint();
    this.home.setPrint(homePrint);
    UndoableEdit undoableEdit = new HomePrintModificationUndoableEdit(
        this.home, this.preferences,oldHomePrint, homePrint);
    this.undoSupport.postEdit(undoableEdit);
  }
 
  private static class HomePrintModificationUndoableEdit extends AbstractUndoableEdit {
    private final Home            home;
    private final UserPreferences preferences;
    private final HomePrint       oldHomePrint;
    private final HomePrint       homePrint;

    private HomePrintModificationUndoableEdit(Home home,
                                              UserPreferences preferences,
                                              HomePrint oldHomePrint,
                                              HomePrint homePrint) {
      this.home = home;
      this.preferences = preferences;
      this.oldHomePrint = oldHomePrint;
      this.homePrint = homePrint;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      this.home.setPrint(this.oldHomePrint);
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      this.home.setPrint(this.homePrint);
    }

    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(PageSetupController.class, "undoPageSetupName");
    }
  }
}
