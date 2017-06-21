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
   * 플러그인 불러올 때 사용한 클래스 로더 설정.
   */
  final void setPluginClassLoader(ClassLoader pluginClassLoader) {
    this.pluginClassLoader = pluginClassLoader;
  }

  /**
   * 클래스 로더 반환.
   */
  public final ClassLoader getPluginClassLoader() {
    return this.pluginClassLoader;
  }

  /**
   * 플러그인 이름 설정.
   */
  final void setName(String name) {
    this.name = name;
  }

  /**
   * 플러그인 이름 반환.
   */
  public final String getName() {
    return this.name;
  }
  
  /**
   * 플러그인 설명 설정.
   */
  final void setDescription(String description) {
    this.description = description;
  }

  /**
   * 플러그인 설명 반환.
   */
  public final String getDescription() {
    return this.description;
  }
  
  /**
   * 플러그인 버전 설정.
   */
  final void setVersion(String version) {
    this.version = version;
  }

  /**
   * 플러그인 버전 반환.
   */
  public final String getVersion() {
    return this.version;
  }
  
  /**
   * 플러그인 라이센스 설정.
   */
  final void setLicense(String license) {
    this.license = license;
  }

  /**
   * 플러그인 라이센스 반환.
   */
  public final String getLicense() {
    return this.license;
  }
  
  /**
   * 플러그인 공급자 설정.
   */
  final void setProvider(String provider) {
    this.provider = provider;
  }

  /**
   * 플러그인 공급자 반환.
   */
  public String getProvider() {
    return this.provider;
  }
  
  /**
   * 현재 응용 프로그램 사용자 기본 설정 지정.
   */
  final void setUserPreferences(UserPreferences userPreferences) {
    this.userPreferences = userPreferences;    
  }
  
  /**
   * 현재 응용 프로그램 사용자 기본 설정 반환.
   */
  public final UserPreferences getUserPreferences() {
    return this.userPreferences;
  }

  /**
   * 플러그인 인스턴스에 연결된 집 설정.
   */
  final void setHome(Home home) {
    this.home = home;
  }

  /**
   * 플러그인 인스턴스에 연결된 집 반환.
   */
  public final Home getHome() {
    return this.home;
  }

  /**
   * 홈 컨트롤러 설정.
   */
  final void setHomeController(HomeController homeController) {
    this.homeController = homeController;
  }

  /**
   * 홈 컨트롤러 반환
   */
  public HomeController getHomeController() {
    return this.homeController;
  }
  
  /**
   * 실행 취소 가능한 편집 지원 설정
   */
  final void setUndoableEditSupport(UndoableEditSupport undoableEditSupport) {
    this.undoableEditSupport = undoableEditSupport;
  }

  /**
   * 실행 취소 가능한 편집 지원 반환
   */
  public final UndoableEditSupport getUndoableEditSupport() {
    return this.undoableEditSupport;
  }
 
  public void destroy() {    
  }
  
  /**
   * 플러그인에서 사용 가능한 액션 반환
   */
  public abstract PluginAction [] getActions();
}
