package com.eteks.homeview3d.plugin;

import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.viewcontroller.HomeController;

public abstract class Plugin {
  private ClassLoader         pluginClassLoader;
  private String              name;
  private String              description;
  private String              version;
  private String              license;
  private String              provider;
  private UserPreferences     userPreferences;
  private Home                home;
  private HomeController      homeController;
  private UndoableEditSupport undoableEditSupport;


  /**
   * �÷����� �ҷ��� �� ����� Ŭ���� �δ� ����.
   */
  final void setPluginClassLoader(ClassLoader pluginClassLoader) {
    this.pluginClassLoader = pluginClassLoader;
  }

  /**
   * Ŭ���� �δ� ��ȯ.
   */
  public final ClassLoader getPluginClassLoader() {
    return this.pluginClassLoader;
  }

  /**
   * �÷����� �̸� ����.
   */
  final void setName(String name) {
    this.name = name;
  }

  /**
   * �÷����� �̸� ��ȯ.
   */
  public final String getName() {
    return this.name;
  }
  
  /**
   * �÷����� ���� ����.
   */
  final void setDescription(String description) {
    this.description = description;
  }

  /**
   * �÷����� ���� ��ȯ.
   */
  public final String getDescription() {
    return this.description;
  }
  
  /**
   * �÷����� ���� ����.
   */
  final void setVersion(String version) {
    this.version = version;
  }

  /**
   * �÷����� ���� ��ȯ.
   */
  public final String getVersion() {
    return this.version;
  }
  
  /**
   * �÷����� ���̼��� ����.
   */
  final void setLicense(String license) {
    this.license = license;
  }

  /**
   * �÷����� ���̼��� ��ȯ.
   */
  public final String getLicense() {
    return this.license;
  }
  
  /**
   * �÷����� ������ ����.
   */
  final void setProvider(String provider) {
    this.provider = provider;
  }

  /**
   * �÷����� ������ ��ȯ.
   */
  public String getProvider() {
    return this.provider;
  }
  
  /**
   * ���� ���� ���α׷� ����� �⺻ ���� ����.
   */
  final void setUserPreferences(UserPreferences userPreferences) {
    this.userPreferences = userPreferences;    
  }
  
  /**
   * ���� ���� ���α׷� ����� �⺻ ���� ��ȯ.
   */
  public final UserPreferences getUserPreferences() {
    return this.userPreferences;
  }

  /**
   * �÷����� �ν��Ͻ��� ����� �� ����.
   */
  final void setHome(Home home) {
    this.home = home;
  }

  /**
   * �÷����� �ν��Ͻ��� ����� �� ��ȯ.
   */
  public final Home getHome() {
    return this.home;
  }

  /**
   * Ȩ ��Ʈ�ѷ� ����.
   */
  final void setHomeController(HomeController homeController) {
    this.homeController = homeController;
  }

  /**
   * Ȩ ��Ʈ�ѷ� ��ȯ
   */
  public HomeController getHomeController() {
    return this.homeController;
  }
  
  /**
   * ���� ��� ������ ���� ���� ����
   */
  final void setUndoableEditSupport(UndoableEditSupport undoableEditSupport) {
    this.undoableEditSupport = undoableEditSupport;
  }

  /**
   * ���� ��� ������ ���� ���� ��ȯ
   */
  public final UndoableEditSupport getUndoableEditSupport() {
    return this.undoableEditSupport;
  }
 
  public void destroy() {    
  }
  
  /**
   * �÷����ο��� ��� ������ �׼� ��ȯ
   */
  public abstract PluginAction [] getActions();
}
