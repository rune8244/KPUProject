package com.eteks.homeview3d.viewcontroller;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;

public class PrintPreviewController implements Controller {
  private final Home            home;
  private final UserPreferences preferences;
  private final HomeController  homeController;
  private final ViewFactory     viewFactory;
  private DialogView            printPreviewView;

  public PrintPreviewController(Home home,
                                UserPreferences preferences,
                                HomeController homeController,
                                ViewFactory viewFactory) {
    this.home = home;
    this.preferences = preferences;
    this.homeController = homeController;
    this.viewFactory = viewFactory;
  }

  public DialogView getView() {
    if (this.printPreviewView == null) {
      this.printPreviewView = this.viewFactory.createPrintPreviewView(this.home, 
          this.preferences, this.homeController, this);
    }
    return this.printPreviewView;
  }
  
  public void displayView(View parentView) {
    getView().displayView(parentView);
  }
}
