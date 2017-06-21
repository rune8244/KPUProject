package com.eteks.homeview3d;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.plugin.HomePluginController;
import com.eteks.homeview3d.plugin.PluginManager;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.Controller;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.View;
import com.eteks.homeview3d.viewcontroller.ViewFactory;

public class HomeFrameController implements Controller {
  private final Home            home;
  private final HomeApplication application;
  private final ViewFactory     viewFactory;
  private final ContentManager  contentManager;
  private final PluginManager   pluginManager;
  private View                  homeFrameView;

  private HomeController        homeController;
  
  public HomeFrameController(Home home, HomeApplication application, 
                             ViewFactory viewFactory,
                             ContentManager contentManager, 
                             PluginManager pluginManager) {
    this.home = home;
    this.application = application;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.pluginManager = pluginManager;
  }

  public View getView() {
    if (this.homeFrameView == null) {
      this.homeFrameView = new HomeFramePane(this.home, this.application, this.contentManager, this);
    }
    return this.homeFrameView;
  }
  
  /**
   * 이 컨트롤러에의한 컨트롤러 리턴.
   */
  public HomeController getHomeController() {
    // 서브 컨트롤러 생성
    if (this.homeController == null) {
      this.homeController = new HomePluginController(
          this.home, this.application, this.viewFactory, this.contentManager, this.pluginManager);
    }
    return this.homeController;
  }
  
  public void displayView() {
    ((HomeFramePane)getView()).displayView();
  }
}
