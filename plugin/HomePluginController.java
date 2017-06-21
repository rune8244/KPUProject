package com.eteks.homeview3d.plugin;

import java.util.Collections;
import java.util.List;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.ViewFactory;

public class HomePluginController extends HomeController {
  private final Home                  home;
  private final HomeApplication       application;
  private final PluginManager         pluginManager;

  /**
   * Ȩ�� ��Ʈ�ѷ� ����
   */
  public HomePluginController(Home home, 
                              HomeApplication application,
                              ViewFactory    viewFactory, 
                              ContentManager contentManager, 
                              PluginManager pluginManager) {
    super(home, application, viewFactory, contentManager);
    this.home = home;
    this.application = application;
    this.pluginManager = pluginManager;
  }

  /**
   * ��Ʈ�ѷ����� ��� ������ �÷����� ��ȯ.
   */
  public List<Plugin> getPlugins() {
    if (this.application != null && this.pluginManager != null) {
      // Ȩ �÷����� �˻�
      return this.pluginManager.getPlugins(
          this.application, this.home, this.application.getUserPreferences(), this, getUndoableEditSupport());
    } else {
      List<Plugin> plugins = Collections.emptyList();
      return plugins;
    }
  }

  /**
   * ������ġ���� �÷����� ������.
   */
  public void importPlugin(String pluginLocation) {
    if (this.pluginManager != null) {
      try {
        if (!this.pluginManager.pluginExists(pluginLocation) 
            || getView().confirmReplacePlugin(pluginLocation)) {
          this.pluginManager.addPlugin(pluginLocation);
          getView().showMessage(this.application.getUserPreferences().getLocalizedString(HomeController.class, 
              "importedPluginMessage"));
        }
      } catch (RecorderException ex) {
        String message = this.application.getUserPreferences().getLocalizedString(HomeController.class, 
            "importPluginError", pluginLocation);
        getView().showError(message);
      }
    }
  }
}
