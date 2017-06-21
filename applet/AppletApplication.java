package com.eteks.homeview3d.applet;

import java.applet.AppletContext;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.ServiceManagerStub;
import javax.jnlp.UnavailableServiceException;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import com.eteks.homeview3d.io.ContentRecording;
import com.eteks.homeview3d.j3d.Component3DManager;
import com.eteks.homeview3d.j3d.ModelManager;
import com.eteks.homeview3d.j3d.TextureManager;
import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.model.HomeFurnitureGroup;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomeRecorder;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.plugin.PluginAction;
import com.eteks.homeview3d.plugin.PluginManager;
import com.eteks.homeview3d.swing.ControllerAction;
import com.eteks.homeview3d.swing.FurnitureTable;
import com.eteks.homeview3d.swing.IconManager;
import com.eteks.homeview3d.swing.ResourceAction;
import com.eteks.homeview3d.swing.SwingTools;
import com.eteks.homeview3d.swing.SwingViewFactory;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.URLContent;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.FurnitureController;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.HomeView;
import com.eteks.homeview3d.viewcontroller.View;
import com.eteks.homeview3d.viewcontroller.ViewFactory;


public class AppletApplication extends HomeApplication {
  private static final String FURNITURE_CATALOG_URLS_PARAMETER       = "furnitureCatalogURLs";
  private static final String FURNITURE_RESOURCES_URL_BASE_PARAMETER = "furnitureResourcesURLBase";
  private static final String TEXTURES_CATALOG_URLS_PARAMETER        = "texturesCatalogURLs";
  private static final String TEXTURES_RESOURCES_URL_BASE_PARAMETER  = "texturesResourcesURLBase";
  private static final String PLUGIN_URLS_PARAMETER                  = "pluginURLs";
  private static final String WRITE_HOME_URL_PARAMETER               = "writeHomeURL";
  private static final String HOME_MAXIMUM_LENGTH                    = "homeMaximumLength";
  private static final String READ_HOME_URL_PARAMETER                = "readHomeURL";
  private static final String DELETE_HOME_URL_PARAMETER              = "deleteHomeURL";
  private static final String LIST_HOMES_URL_PARAMETER               = "listHomesURL";
  private static final String READ_PREFERENCES_URL_PARAMETER         = "readPreferencesURL";
  private static final String WRITE_PREFERENCES_URL_PARAMETER        = "writePreferencesURL";
  private static final String DEFAULT_HOME_PARAMETER                 = "defaultHome";
  private static final String ENABLE_EXPORT_TO_SH3D                  = "enableExportToSH3D";
  private static final String ENABLE_IMPORT_FROM_SH3D                = "enableImportFromSH3D";
  private static final String ENABLE_EXPORT_TO_CSV                   = "enableExportToCSV";
  private static final String ENABLE_EXPORT_TO_SVG                   = "enableExportToSVG";
  private static final String ENABLE_EXPORT_TO_OBJ                   = "enableExportToOBJ";
  private static final String ENABLE_PRINT_TO_PDF                    = "enablePrintToPDF";
  private static final String ENABLE_CREATE_PHOTO                    = "enableCreatePhoto";
  private static final String ENABLE_CREATE_VIDEO                    = "enableCreateVideo";
  private static final String SHOW_MEMORY_STATUS_PARAMETER           = "showMemoryStatus";
  private static final String USER_LANGUAGE                          = "userLanguage";
  
  private JApplet         applet;
  private final String    name;
  private HomeRecorder    homeRecorder;
  private UserPreferences userPreferences;
  private ContentManager  contentManager;
  private ViewFactory     viewFactory;
  private PluginManager   pluginManager;
  private Timer           memoryStatusTimer;

  public AppletApplication(final JApplet applet) {
    this.applet = applet;
    if (applet.getName() == null) {
      this.name = super.getName();
    } else {
      this.name = applet.getName();
    }
    
    final String readHomeURL = getAppletParameter(applet, READ_HOME_URL_PARAMETER, "readHome.php?home=%s");
    final String defaultHome = getAppletParameter(applet, DEFAULT_HOME_PARAMETER, "");    
    final boolean showMemoryStatus = getAppletBooleanParameter(applet, SHOW_MEMORY_STATUS_PARAMETER);

    URL codeBase = applet.getCodeBase();

    try {
      //전송없이 DnD 매니지 먼트 사용 / Mac OS X, Oracle Java / Mac OS X or OpenJDK / Linux 
      boolean plugin2 = applet.getAppletContext() != null
                     && applet.getAppletContext().getClass().getName().startsWith("sun.plugin2.applet.Plugin2Manager");
      if (OperatingSystem.isMacOSX() && (plugin2 || OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) 
          || OperatingSystem.isLinux() && System.getProperty("java.runtime.name", "").startsWith("OpenJDK")) {
        System.setProperty("com.eteks.homeview3d.dragAndDropWithoutTransferHandler", "true");
      }
    } catch (AccessControlException ex) {
      // Unsigned applet
    }
    
    checkJavaWebStartBasicService(applet, codeBase);          
 
    initLookAndFeel();
   

    addHomesListener(new CollectionListener<Home>() {
        private boolean firstHome = true;
        
        public void collectionChanged(CollectionEvent<Home> ev) {
          Home home = ev.getItem();
          switch (ev.getType()) {
            case ADD :
              try {
                final HomeController controller = createHomeController(home);
                // 애플릿 컨텐츠 변환 
                applet.setContentPane((JComponent)controller.getView());
                applet.getRootPane().revalidate();

             
                if (this.firstHome) {
                  this.firstHome = false;
                  if (defaultHome.length() > 0 && readHomeURL.length() != 0) {
                    controller.open(defaultHome);
                  }
                }
              } catch (IllegalStateException ex) {

                if ("javax.media.j3d.IllegalRenderingStateException".equals(ex.getClass().getName())) {
                  ex.printStackTrace();
                  show3DError();
                } else {
                  throw ex;
                }
              }
              break;
          }
        }
      });

    addComponent3DRenderingErrorObserver();
    
    EventQueue.invokeLater(new Runnable() {
        public void run() { 
          addHome(createHome());
          
          if (showMemoryStatus) {
            final String memoryStatus = getUserPreferences().getLocalizedString(AppletApplication.class, "memoryStatus");
            // 타이머 시작
            memoryStatusTimer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                  Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                  if (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, applet)) {
                    Runtime runtime = Runtime.getRuntime();
                    applet.showStatus(String.format(memoryStatus, 
                        Math.round(100f * (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()),
                        runtime.maxMemory() / 1024 / 1024));
                  }
                }
              });
            memoryStatusTimer.start();
          }
        }
      });
  }

  public void destroy() {
    if (this.memoryStatusTimer != null) {
      this.memoryStatusTimer.stop();
      this.memoryStatusTimer = null;
    }
    for (Home home : getHomes()) {
      // 홈 디렉토리 삭제 
      deleteHome(home);
    }
    System.gc();
    try {
      if (!Boolean.getBoolean("com.eteks.homeview3d.no3D")) { 
        // 관리자 스레드 멈춤
        TextureManager.getInstance().clear();
        ModelManager.getInstance().clear();
      }
      IconManager.getInstance().clear();
    } catch (AccessControlException ex) {
     
    }
    // 임시파일 삭제
    OperatingSystem.deleteTemporaryFiles();
  }

  public boolean isModified() {
    for (Home home : getHomes()) {
      if (home.isModified()) {
        return true;
      }
    }
    return false;
  }

  private URL [] getURLs(URL codeBase, String urlList) {
    String [] urlStrings = urlList.split("\\s|,");
    List<URL> urls = new ArrayList<URL>(urlStrings.length);
    for (String urlString : urlStrings) {
      URL url = getURLWithCodeBase(codeBase, urlString);
      if (url != null) {
        urls.add(url);
      }
    }
    return urls.toArray(new URL [urls.size()]);
  }

  private URL getURLWithCodeBase(URL codeBase, String url) {
    if (url != null 
        && url.length() > 0) {
      try {
        return new URL(codeBase, url);
      } catch (MalformedURLException ex) {
      }
    }
    return null;
  }

  private String getURLStringWithCodeBase(URL codeBase, String url) {
    if (url.length() > 0) {
      try {
        return new URL(codeBase, url).toString();
      } catch (MalformedURLException ex) {
      }
    }
    return null;
  }
  

  private String getAppletParameter(JApplet applet, String parameter, String defaultValue) {
    String parameterValue = applet.getParameter(parameter);
    if (parameterValue == null) {
      return defaultValue;
    } else {
      return parameterValue;
    }
  }

  private boolean getAppletBooleanParameter(JApplet applet, String parameter) {
    return "true".equalsIgnoreCase(getAppletParameter(applet, parameter, "false"));
  }
  
  protected HomeController createHomeController(Home home) {
    final String writeHomeURL = getAppletParameter(this.applet, WRITE_HOME_URL_PARAMETER, "writeHome.php");    
    final String readHomeURL = getAppletParameter(this.applet, READ_HOME_URL_PARAMETER, "readHome.php?home=%s");
    final String listHomesURL = getAppletParameter(this.applet, LIST_HOMES_URL_PARAMETER, "listHomes.php");
    final String defaultHome = getAppletParameter(this.applet, DEFAULT_HOME_PARAMETER, "");    
    
    boolean newHomeEnabled = 
        writeHomeURL.length() != 0 && listHomesURL.length() != 0;
    boolean openEnabled = 
        readHomeURL.length() != 0 && listHomesURL.length() != 0;
    boolean saveEnabled = writeHomeURL.length() != 0 
        && (defaultHome.length() != 0 || listHomesURL.length() != 0);
    boolean saveAsEnabled = 
        writeHomeURL.length() != 0 && listHomesURL.length() != 0;
    long homeMaximumLength = Long.valueOf(getAppletParameter(applet, HOME_MAXIMUM_LENGTH, "-1"));    
    
    final HomeController controller = new HomeAppletController(
        home, AppletApplication.this, getViewFactory(), getContentManager(), getPluginManager(),
        newHomeEnabled, openEnabled, saveEnabled, saveAsEnabled, homeMaximumLength);
    
    JRootPane homeView = (JRootPane)controller.getView();
    // 메뉴바 제거
    homeView.setJMenuBar(null);
    
    for (HomeView.ActionType actionType : HomeView.ActionType.values()) {
      Action action = homeView.getActionMap().get(actionType);
      if (action != null) {
        ResourceAction.MenuItemAction menuAction = new ResourceAction.MenuItemAction(action);
        KeyStroke accelerator = (KeyStroke)menuAction.getValue(Action.ACCELERATOR_KEY);
        if (accelerator != null) {
          homeView.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(accelerator, actionType);
        }
      }
    }
    
    JToolBar toolBar = (JToolBar)homeView.getContentPane().getComponent(0);
    toolBar.setFloatable(false);    
    List<JComponent> pluginButtons = new ArrayList<JComponent>();
    for (int i = 0; i < toolBar.getComponentCount(); i++) {
      JComponent component = (JComponent)toolBar.getComponent(i);
      if (component instanceof AbstractButton
          && ((AbstractButton)component).getAction().
                getValue(PluginAction.Property.TOOL_BAR.name()) == Boolean.TRUE) {
        pluginButtons.add(component);
      }
    }
    toolBar.removeAll();
    addEnabledActionToToolBar(homeView, HomeView.ActionType.NEW_HOME, toolBar);
    addEnabledActionToToolBar(homeView, HomeView.ActionType.OPEN, toolBar);
    addEnabledActionToToolBar(homeView, HomeView.ActionType.SAVE, toolBar);
    addEnabledActionToToolBar(homeView, HomeView.ActionType.SAVE_AS, toolBar);
    
    if (getAppletBooleanParameter(this.applet, ENABLE_EXPORT_TO_SH3D)) {
      try {

        Action exportToSH3DAction = new ControllerAction(getUserPreferences(), 
            AppletApplication.class, "EXPORT_TO_SH3D", controller, "exportToSH3D");
        exportToSH3DAction.setEnabled(true);
        addActionToToolBar(new ResourceAction.ToolBarAction(exportToSH3DAction), toolBar);
      } catch (NoSuchMethodException ex) {
        ex.printStackTrace();
      }
    }
    if (getAppletBooleanParameter(this.applet, ENABLE_IMPORT_FROM_SH3D)) {
      try {
        Action importFromSH3DAction = new ControllerAction(getUserPreferences(), 
            AppletApplication.class, "IMPORT_FROM_SH3D", controller, "importFromSH3D");
        importFromSH3DAction.setEnabled(true);
        addActionToToolBar(new ResourceAction.ToolBarAction(importFromSH3DAction), toolBar);
      } catch (NoSuchMethodException ex) {
        ex.printStackTrace();
      }
    }
    
    if (toolBar.getComponentCount() > 0) {
      toolBar.add(Box.createRigidArea(new Dimension(2, 2)));
    }
    addActionToToolBar(homeView, HomeView.ActionType.PAGE_SETUP, toolBar);
    addActionToToolBar(homeView, HomeView.ActionType.PRINT, toolBar);
    Action printToPdfAction = getToolBarAction(homeView, HomeView.ActionType.PRINT_TO_PDF);
    if (printToPdfAction != null 
        && getAppletBooleanParameter(this.applet, ENABLE_PRINT_TO_PDF) 
        && !OperatingSystem.isMacOSX()) {
      controller.getView().setEnabled(HomeView.ActionType.PRINT_TO_PDF, true);
      addActionToToolBar(printToPdfAction, toolBar);
    }
    Action preferencesAction = getToolBarAction(homeView, HomeView.ActionType.PREFERENCES);
    if (preferencesAction != null) {
      toolBar.add(Box.createRigidArea(new Dimension(2, 2)));
      addActionToToolBar(preferencesAction, toolBar);
    }
    toolBar.addSeparator();

    addActionToToolBar(homeView, HomeView.ActionType.UNDO, toolBar);
    addActionToToolBar(homeView, HomeView.ActionType.REDO, toolBar);
    toolBar.add(Box.createRigidArea(new Dimension(2, 2)));
    addActionToToolBar(homeView, HomeView.ActionType.CUT, toolBar);
    addActionToToolBar(homeView, HomeView.ActionType.COPY, toolBar);
    addActionToToolBar(homeView, HomeView.ActionType.PASTE, toolBar);
    toolBar.add(Box.createRigidArea(new Dimension(2, 2)));
    addActionToToolBar(homeView, HomeView.ActionType.DELETE, toolBar);
    toolBar.addSeparator();

    Action addHomeFurnitureAction = getToolBarAction(homeView, HomeView.ActionType.ADD_HOME_FURNITURE);
    if (addHomeFurnitureAction != null) {
      addActionToToolBar(addHomeFurnitureAction, toolBar);
      toolBar.addSeparator();
    }
    
    addToggleActionToToolBar(homeView, HomeView.ActionType.SELECT, toolBar);
    addToggleActionToToolBar(homeView, HomeView.ActionType.PAN, toolBar);
    addToggleActionToToolBar(homeView, HomeView.ActionType.CREATE_WALLS, toolBar);
    addToggleActionToToolBar(homeView, HomeView.ActionType.CREATE_ROOMS, toolBar);
    addToggleActionToToolBar(homeView, HomeView.ActionType.CREATE_POLYLINES, toolBar);
    addToggleActionToToolBar(homeView, HomeView.ActionType.CREATE_DIMENSION_LINES, toolBar);
    addToggleActionToToolBar(homeView, HomeView.ActionType.CREATE_LABELS, toolBar);
    toolBar.add(Box.createRigidArea(new Dimension(2, 2)));
    
    addActionToToolBar(homeView, HomeView.ActionType.ZOOM_OUT, toolBar);
    addActionToToolBar(homeView, HomeView.ActionType.ZOOM_IN, toolBar);

    boolean no3D;
    try {
      no3D = Boolean.getBoolean("com.eteks.homeview3d.no3D");
    } catch (AccessControlException ex) {
      no3D = true;
    }
    if (!no3D) {
      Action createPhotoAction = getToolBarAction(homeView, HomeView.ActionType.CREATE_PHOTO);
      if (createPhotoAction != null) {
        boolean enableCreatePhoto = getAppletBooleanParameter(this.applet, ENABLE_CREATE_PHOTO);
        controller.getView().setEnabled(HomeView.ActionType.CREATE_PHOTO, enableCreatePhoto);
        controller.getView().setEnabled(HomeView.ActionType.CREATE_PHOTOS_AT_POINTS_OF_VIEW, 
            enableCreatePhoto && !home.getStoredCameras().isEmpty());
        if (enableCreatePhoto) {
          toolBar.addSeparator();
          addActionToToolBar(createPhotoAction, toolBar);
        } else {

          homeView.getActionMap().get(HomeView.ActionType.CREATE_PHOTOS_AT_POINTS_OF_VIEW).addPropertyChangeListener(
              new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent ev) {  
                  if ("enabled".equals(ev.getPropertyName())) {
                    Action action = (Action)ev.getSource();
                    action.removePropertyChangeListener(this);
                    action.setEnabled(false);
                    action.addPropertyChangeListener(this);
                  }
                }
              });
        }
      }
    }

    // 플러그인버튼
    if (pluginButtons.size() > 0) {
      toolBar.addSeparator();
      for (JComponent pluginButton : pluginButtons) {
        toolBar.add(pluginButton);
      }
    }
    
    Action aboutAction = getToolBarAction(homeView, HomeView.ActionType.ABOUT);
    if (aboutAction != null) {
      toolBar.addSeparator();
      addActionToToolBar(aboutAction, toolBar);
    }
    
    controller.getView().setEnabled(HomeView.ActionType.EXPORT_TO_CSV, 
        getAppletBooleanParameter(this.applet, ENABLE_EXPORT_TO_CSV));
    controller.getView().setEnabled(HomeView.ActionType.EXPORT_TO_SVG, 
        getAppletBooleanParameter(this.applet, ENABLE_EXPORT_TO_SVG));
    controller.getView().setEnabled(HomeView.ActionType.EXPORT_TO_OBJ, 
        getAppletBooleanParameter(this.applet, ENABLE_EXPORT_TO_OBJ) && !no3D);
    controller.getView().setEnabled(HomeView.ActionType.CREATE_VIDEO, 
        getAppletBooleanParameter(this.applet, ENABLE_CREATE_VIDEO) && !no3D);
    
    // 경계선
    homeView.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    
    return controller;
  }
  

  private void addEnabledActionToToolBar(JComponent homeView, HomeView.ActionType actionType, JToolBar toolBar) {
    Action action = getToolBarAction(homeView, actionType);
    if (action != null && action.isEnabled()) {
      addActionToToolBar(action, toolBar);
    }
  }
 
  private void addActionToToolBar(JComponent homeView, HomeView.ActionType actionType, JToolBar toolBar) {
    Action action = getToolBarAction(homeView, actionType);
    if (action != null) {
      addActionToToolBar(action, toolBar);
    }
  }


  private Action getToolBarAction(JComponent homeView, HomeView.ActionType actionType) {
    Action action = homeView.getActionMap().get(actionType);    
    return action != null 
        ? new ResourceAction.ToolBarAction(action)
        : null;
  }
  

  private void addActionToToolBar(Action action, JToolBar toolBar) {
    if (OperatingSystem.isMacOSXLeopardOrSuperior() && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
    
      toolBar.add(new JButton(action) {
          @Override
          public Insets getInsets() {
            Insets insets = super.getInsets();
            insets.top += 3;
            insets.bottom += 3;
            return insets;
          }
        });
    } else {
      toolBar.add(new JButton(action));
    }
  }


  private void addToggleActionToToolBar(JComponent homeView, HomeView.ActionType actionType, JToolBar toolBar) {
    Action action = getToolBarAction(homeView, actionType);
    if (action != null) {
      JToggleButton toggleButton;
      if (OperatingSystem.isMacOSXLeopardOrSuperior() && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
       
        toggleButton = new JToggleButton(action) {
            @Override
            public Insets getInsets() {
              Insets insets = super.getInsets();
              insets.top += 3;
              insets.bottom += 3;
              return insets;
            }
          };
      } else {
        toggleButton = new JToggleButton(action);
      }
      toggleButton.setModel((ButtonModel)action.getValue(ResourceAction.TOGGLE_BUTTON_MODEL));
      toolBar.add(toggleButton);
    }
  }


  @Override
  public HomeRecorder getHomeRecorder() {
    if (this.homeRecorder == null) {
      URL codeBase = this.applet.getCodeBase();
      final String writeHomeURL = getAppletParameter(this.applet, WRITE_HOME_URL_PARAMETER, "writeHome.php");    
      final String readHomeURL = getAppletParameter(this.applet, READ_HOME_URL_PARAMETER, "readHome.php?home=%s");
      final String listHomesURL = getAppletParameter(this.applet, LIST_HOMES_URL_PARAMETER, "listHomes.php");
      final String deleteHomeURL = getAppletParameter(this.applet, DELETE_HOME_URL_PARAMETER, "");
      this.homeRecorder =  new HomeAppletRecorder(getURLStringWithCodeBase(codeBase, writeHomeURL), 
          getURLStringWithCodeBase(codeBase, readHomeURL), 
          getURLStringWithCodeBase(codeBase, listHomesURL),
          getURLStringWithCodeBase(codeBase, deleteHomeURL),
          ContentRecording.INCLUDE_TEMPORARY_CONTENT);
    }
    return this.homeRecorder;
  }
  

  @Override
  public UserPreferences getUserPreferences() {
    if (this.userPreferences == null) {
      URL codeBase = this.applet.getCodeBase();
      final String furnitureCatalogURLs = getAppletParameter(this.applet, FURNITURE_CATALOG_URLS_PARAMETER, "catalog.zip");
      final String furnitureResourcesUrlBase = getAppletParameter(this.applet, FURNITURE_RESOURCES_URL_BASE_PARAMETER, null);
      final String texturesCatalogURLs = getAppletParameter(this.applet, TEXTURES_CATALOG_URLS_PARAMETER, "catalog.zip");
      final String texturesResourcesUrlBase = getAppletParameter(this.applet, TEXTURES_RESOURCES_URL_BASE_PARAMETER, null);
      final String readPreferencesURL = getAppletParameter(this.applet, READ_PREFERENCES_URL_PARAMETER, "");    
      final String writePreferencesURL = getAppletParameter(this.applet, WRITE_PREFERENCES_URL_PARAMETER, "");    
      final String userLanguage = getAppletParameter(this.applet, USER_LANGUAGE, null);    
      this.userPreferences = new AppletUserPreferences(
          getURLs(codeBase, furnitureCatalogURLs), 
          getURLWithCodeBase(codeBase, furnitureResourcesUrlBase), 
          getURLs(codeBase, texturesCatalogURLs),
          getURLWithCodeBase(codeBase, texturesResourcesUrlBase), 
          getURLWithCodeBase(codeBase, writePreferencesURL), 
          getURLWithCodeBase(codeBase, readPreferencesURL), 
          new Executor() {
              public void execute(Runnable command) {
                EventQueue.invokeLater(command);
              }
            },
          userLanguage);
    }
    return this.userPreferences;
  }


  protected ContentManager getContentManager() {
    if (this.contentManager == null) {
      this.contentManager = new AppletContentManager(getHomeRecorder(), getUserPreferences(), getViewFactory());
    }
    return this.contentManager;
  }
  

  protected ViewFactory getViewFactory() {
    if (this.viewFactory == null) {
     this.viewFactory = new SwingViewFactory() {
         @Override
         public View createFurnitureView(Home home, UserPreferences preferences, FurnitureController furnitureController) {
           return new AppletFurnitureTable(home, preferences, furnitureController);
         }
       };
    }
    return this.viewFactory;
  }


  protected PluginManager getPluginManager() {
    if (this.pluginManager == null) {
      URL codeBase = this.applet.getCodeBase();
      String pluginURLs = getAppletParameter(this.applet, PLUGIN_URLS_PARAMETER, "");
      this.pluginManager = new PluginManager(getURLs(codeBase, pluginURLs));
    }
    return this.pluginManager;
  }


  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getVersion() {
    return getUserPreferences().getLocalizedString(AppletApplication.class, "applicationVersion");
  }
  

  private void initLookAndFeel() {
    try {

      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        UIManager.put("TitledBorder.border", 
            UIManager.getBorder("TitledBorder.aquaVariant"));
      }
      Toolkit.getDefaultToolkit().setDynamicLayout(true);
      SwingTools.updateSwingResourceLanguage();
    } catch (Exception ex) {
    }
  }
  

  private void addComponent3DRenderingErrorObserver() {
    try {
      if (!Boolean.getBoolean("com.eteks.homeview3d.no3D")) {
        Component3DManager.getInstance().setRenderingErrorObserver(
            new Component3DManager.RenderingErrorObserver() {
              public void errorOccured(int errorCode, String errorMessage) {
                System.err.print("Error in Java 3D : " + errorCode + " " + errorMessage);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      show3DError();
                    }
                  });
              }
            });
      }
    } catch (AccessControlException ex) {
    }
  }


  private void show3DError() {
    String message = getUserPreferences().getLocalizedString(AppletApplication.class, "3DError.message");
    String title = getUserPreferences().getLocalizedString(AppletApplication.class, "3DError.title");
    JOptionPane.showMessageDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(), 
        message, title, JOptionPane.ERROR_MESSAGE);
  }


  private void checkJavaWebStartBasicService(final JApplet applet, URL codeBase) {
    boolean serviceManagerAvailable = ServiceManager.getServiceNames() != null; 
    if (serviceManagerAvailable) {
      try { 
        ServiceManager.lookup("javax.jnlp.BasicService");
      } catch (Exception ex) {
        if ("javax.jnlp.UnavailableServiceException".equals(ex.getClass().getName())) {
          serviceManagerAvailable = false;
        } else {
          throw new RuntimeException(ex);
        }
      }
    }

    if (!serviceManagerAvailable) {
      ServiceManager.setServiceManagerStub(
          new StandaloneServiceManager(applet.getAppletContext(), codeBase));
    }
  }


  private static final class AppletFurnitureTable extends FurnitureTable {
    private TableCellRenderer nameRenderer = new TableCellRenderer() {
        private Font defaultFont;
        private Font importedPieceFont;
      
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
          JComponent rendererComponent = (JComponent)AppletFurnitureTable.super.getCellRenderer(row, column).
              getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          if (this.defaultFont == null) {
            this.defaultFont = table.getFont();
            this.importedPieceFont = 
                new Font(this.defaultFont.getFontName(), Font.ITALIC, this.defaultFont.getSize());        
          }
          
          HomePieceOfFurniture piece = (HomePieceOfFurniture)getValueAt(row, column);
          URLContent model = (URLContent)piece.getModel();
          boolean importedPiece = model.getClass() != URLContent.class;
          rendererComponent.setFont(importedPiece  ? this.importedPieceFont  : this.defaultFont);
          return rendererComponent;
        }
      };

    private AppletFurnitureTable(Home home, UserPreferences preferences, FurnitureController controller) {
      super(home, preferences, controller);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
      if (getColumnModel().getColumn(column).getIdentifier() == HomePieceOfFurniture.SortableProperty.NAME
          && !(getValueAt(row, column) instanceof HomeFurnitureGroup)) {
        return this.nameRenderer;
      } else {
        return super.getCellRenderer(row, column);
      }
    }
  }

  private static class StandaloneServiceManager implements ServiceManagerStub {
    private BasicService basicService;

    public StandaloneServiceManager(AppletContext appletContext,
                                    URL codeBase) {
      this.basicService = new AppletBasicService(appletContext, codeBase);
    }

    public Object lookup(final String name) throws UnavailableServiceException {
      if (name.equals("javax.jnlp.BasicService")) {
        return this.basicService;
      } else {
        throw new UnavailableServiceException(name);
      }
    }
    
    public String[] getServiceNames() {
      return new String[]  {"javax.jnlp.BasicService"};
    }
  }    

  private static class AppletBasicService implements BasicService {
    private final AppletContext appletContext;
    private final URL    codeBase;

    public AppletBasicService(AppletContext appletContext,
                              URL codeBase) {
      this.appletContext = appletContext;
      this.codeBase = codeBase;
    }

    public boolean showDocument(URL url) {
      this.appletContext.showDocument(url);
      return true;
    }

    public URL getCodeBase() {
      return this.codeBase;
    }

    public boolean isOffline() {
      return false;
    }

    public boolean isWebBrowserSupported() {
      return true;
    }
  }
}