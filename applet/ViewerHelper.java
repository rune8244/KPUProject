package com.eteks.homeview3d.applet;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import com.eteks.homeview3d.io.DefaultHomeInputStream;
import com.eteks.homeview3d.j3d.Component3DManager;
import com.eteks.homeview3d.j3d.ModelManager;
import com.eteks.homeview3d.j3d.TextureManager;
import com.eteks.homeview3d.model.Camera;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.InterruptedRecorderException;
import com.eteks.homeview3d.model.LengthUnit;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Library;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.swing.HomeComponent3D;
import com.eteks.homeview3d.swing.ThreadedTaskPanel;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.HomeController3D;
import com.eteks.homeview3d.viewcontroller.ThreadedTaskController;
import com.eteks.homeview3d.viewcontroller.ThreadedTaskView;
import com.eteks.homeview3d.viewcontroller.View;
import com.eteks.homeview3d.viewcontroller.ViewFactory;
import com.eteks.homeview3d.viewcontroller.ViewFactoryAdapter;

public final class ViewerHelper {
  private static final String HOME_URL_PARAMETER                   = "homeURL";
  private static final String LEVEL_PARAMETER                      = "level";
  private static final String CAMERA_PARAMETER                     = "camera";
  private static final String SELECTABLE_LEVELS_PARAMETER          = "selectableLevels";
  private static final String SELECTABLE_CAMERAS_PARAMETER         = "selectableCameras";
  private static final String IGNORE_CACHE_PARAMETER               = "ignoreCache";
  private static final String NAVIGATION_PANEL                     = "navigationPanel";
  private static final String ACTIVATE_CAMERA_SWITCH_KEY_PARAMETER = "activateCameraSwitchKey";
  
  public ViewerHelper(final JApplet applet) {
    // 디폴트 유저 프레퍼런스 만듦
    final UserPreferences preferences = new UserPreferences() {
        @Override
        public void addLanguageLibrary(String languageLibraryName) throws RecorderException {
          throw new UnsupportedOperationException();
        }
  
        @Override
        public boolean languageLibraryExists(String languageLibraryName) throws RecorderException {
          throw new UnsupportedOperationException();
        }

        @Override
        public void addFurnitureLibrary(String furnitureLibraryName) throws RecorderException {
          throw new UnsupportedOperationException();
        }
  
        @Override
        public boolean furnitureLibraryExists(String furnitureLibraryName) throws RecorderException {
          throw new UnsupportedOperationException();
        }
  
        @Override
        public boolean texturesLibraryExists(String name) throws RecorderException {
          throw new UnsupportedOperationException();
        }

        @Override
        public void addTexturesLibrary(String name) throws RecorderException {
          throw new UnsupportedOperationException();
        }

        @Override
        public List<Library> getLibraries() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void write() throws RecorderException {
          throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean isNavigationPanelVisible() {
          return "true".equalsIgnoreCase(applet.getParameter(NAVIGATION_PANEL));
        }
        
        @Override
        public LengthUnit getLengthUnit() {
          return LengthUnit.CENTIMETER;
        }
      };
    
    final ViewFactory viewFactory = new ViewFactoryAdapter() {
        public ThreadedTaskView createThreadedTaskView(String taskMessage, UserPreferences preferences,
                                                       ThreadedTaskController controller) {
          return new ThreadedTaskPanel(taskMessage, preferences, controller) {
              private boolean taskRunning;
  
              public void setTaskRunning(boolean taskRunning, View executingView) {
                if (taskRunning && !this.taskRunning) {
                  this.taskRunning = taskRunning;
                  JPanel contentPane = new JPanel(new GridBagLayout());
                  contentPane.add(this, new GridBagConstraints());
                  applet.setContentPane(contentPane);
                  applet.getRootPane().revalidate();
                } 
              }
            };
        }

        public View createView3D(final Home home, UserPreferences preferences, final HomeController3D controller) {
          HomeComponent3D homeComponent3D = new HomeComponent3D(home, preferences, controller);
          String activateCameraSwitchKeyParameter = applet.getParameter(ACTIVATE_CAMERA_SWITCH_KEY_PARAMETER);
          if (activateCameraSwitchKeyParameter == null
              || "true".equalsIgnoreCase(activateCameraSwitchKeyParameter)) {
            // 탭 키 추가
            InputMap inputMap = homeComponent3D.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(KeyStroke.getKeyStroke("SPACE"), "changeCamera");
            ActionMap actionMap = homeComponent3D.getActionMap();
            actionMap.put("changeCamera", new AbstractAction() {
                public void actionPerformed(ActionEvent ev) {
                  if (home.getCamera() == home.getTopCamera()) {
                    controller.viewFromObserver();
                  } else {
                    controller.viewFromTop();
                  }
                }
              });
          }
          return homeComponent3D;
        }
      };

    if (OperatingSystem.isMacOSX()            
        && applet.getAppletContext() != null
        && applet.getAppletContext().getClass().getName().startsWith("sun.plugin2.applet.Plugin2Manager")
        && OperatingSystem.isJavaVersionBetween("1.6", "1.7")) {
      System.setProperty("com.eteks.homeview3d.j3d.useOffScreen3DView", "true");
    }

    initLookAndFeel();

    addComponent3DRenderingErrorObserver(applet.getRootPane(), preferences);

    String homeUrlParameter = applet.getParameter(HOME_URL_PARAMETER);
    if (homeUrlParameter == null) {
      homeUrlParameter = "default.sh3d";
    }
    final String levelParameter = applet.getParameter(LEVEL_PARAMETER);
    final String cameraParameter = applet.getParameter(CAMERA_PARAMETER);
    String selectableLevelsParameter = applet.getParameter(SELECTABLE_LEVELS_PARAMETER);
    final String [] selectableLevels;
    if (selectableLevelsParameter != null) {
      selectableLevels = selectableLevelsParameter.split("\\s*,\\s*");
    } else {
      selectableLevels = new String [0];
    }
    String selectableCamerasParameter = applet.getParameter(SELECTABLE_CAMERAS_PARAMETER);
    final String [] selectableCameras;
    if (selectableCamerasParameter != null) {
      selectableCameras = selectableCamerasParameter.split("\\s*,\\s*");
    } else {
      selectableCameras = new String [0];
    }
    String ignoreCacheParameter = applet.getParameter(IGNORE_CACHE_PARAMETER);
    final boolean ignoreCache = ignoreCacheParameter != null 
        && "true".equalsIgnoreCase(ignoreCacheParameter);
    try {
      final URL homeUrl = new URL(applet.getDocumentBase(), homeUrlParameter);
      // 스레드에서 홈 읽기
      Callable<Void> openTask = new Callable<Void>() {
            public Void call() throws RecorderException {
              // 레코더로 홈 읽기
              Home openedHome = readHome(homeUrl, ignoreCache);
              displayHome(applet.getRootPane(), openedHome, levelParameter, cameraParameter, 
                  selectableLevels, selectableCameras, preferences, viewFactory);
              return null;
            }
          };
      ThreadedTaskController.ExceptionHandler exceptionHandler = 
          new ThreadedTaskController.ExceptionHandler() {
            public void handleException(Exception ex) {
              if (!(ex instanceof InterruptedRecorderException)) {
                if (ex instanceof RecorderException) {
                  showError(applet.getRootPane(), 
                      preferences.getLocalizedString(ViewerHelper.class, "openError", homeUrl));
                } else {
                  ex.printStackTrace();
                }
              }
            }
          };
      new ThreadedTaskController(openTask, 
          preferences.getLocalizedString(ViewerHelper.class, "openMessage"), exceptionHandler, 
          null, viewFactory).executeTask(null);
    } catch (MalformedURLException ex) {
      showError(applet.getRootPane(), 
          preferences.getLocalizedString(ViewerHelper.class, "openError", homeUrlParameter));
      return;
    } 
  }
  
  public void destroy() {
    // 삭제된 오브젝트 모음
    System.gc();
    // 관리자 스레드 멈춤
    TextureManager.getInstance().clear();
    ModelManager.getInstance().clear();
  }
  
  private void initLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      Toolkit.getDefaultToolkit().setDynamicLayout(true);
    } catch (Exception ex) {
    }
  }

  private void addComponent3DRenderingErrorObserver(final JRootPane rootPane,
                                                    final UserPreferences preferences) {
    Component3DManager.getInstance().setRenderingErrorObserver(
        new Component3DManager.RenderingErrorObserver() {
          public void errorOccured(int errorCode, String errorMessage) {
            System.err.print("Error in Java 3D : " + errorCode + " " + errorMessage);
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  String message = preferences.getLocalizedString(
                      ViewerHelper.class, "3DErrorMessage");
                  showError(rootPane, message);
                }
              });
          }
        });
  }

  /**
   * Shows the given text in a label.
   */
  private static void showError(final JRootPane rootPane, String text) {
    JLabel label = new JLabel(text, JLabel.CENTER);
    rootPane.setContentPane(label);
    rootPane.revalidate();
  }

  /**
   * Reads a home from its URL.
   */
  private Home readHome(URL homeUrl, boolean ignoreCache) throws RecorderException {
    URLConnection connection = null;
    DefaultHomeInputStream in = null;
    try {
      // Open a home input stream to server 
      connection = homeUrl.openConnection();
      connection.setRequestProperty("Content-Type", "charset=UTF-8");
      connection.setUseCaches(!ignoreCache);
      in = new DefaultHomeInputStream(connection.getInputStream());
      // Read home with HomeInputStream
      Home home = in.readHome();
      return home;
    } catch (InterruptedIOException ex) {
      throw new InterruptedRecorderException("Read " + homeUrl + " interrupted");
    } catch (IOException ex) {
      throw new RecorderException("Can't read home from " + homeUrl, ex);
    } catch (ClassNotFoundException ex) {
      throw new RecorderException("Missing classes to read home from " + homeUrl, ex);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {
        throw new RecorderException("Can't close stream", ex);
      }
    }
  }
  
  /**
   * Displays the given <code>home</code> in the main pane of <code>rootPane</code>. 
   */
  private void displayHome(final JRootPane rootPane, 
                           final Home home,
                           final String levelName,
                           final String cameraName,
                           final String [] selectableLevels, 
                           final String [] selectableCameras, 
                           final UserPreferences preferences, 
                           final ViewFactory viewFactory) {
    EventQueue.invokeLater(new Runnable() {
        public void run() {
          final HomeController3D controller = 
              new HomeController3D(home, preferences, viewFactory, null, null);
          JComponent view3D = (JComponent)controller.getView();
          
          // Select default level
          if (levelName != null) {
            Level level = getLevel(home, levelName);
            if (level != null) {
              home.setSelectedLevel(level);
            }
          }
          // Select default camera
          if (cameraName != null) {
            Camera camera = getStoredCamera(home, cameraName);
            if (camera != null) {
              controller.goToCamera(camera);
            }
          }
          // Add menu items to 3D view contextual menu
          List<Level> selectableLevelsList = new ArrayList<Level>();
          for (String selectableLevel : selectableLevels) {
            Level level = getLevel(home, selectableLevel);
            if (level != null) {
              selectableLevelsList.add(level);
            }
          }
          List<Camera> selectableCamerasList = new ArrayList<Camera>();
          for (String selectableCamera : selectableCameras) {
            Camera level = getStoredCamera(home, selectableCamera);
            if (level != null) {
              selectableCamerasList.add(level);
            }
          }
          if (!selectableLevelsList.isEmpty()
              || !selectableCamerasList.isEmpty()) {
            JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.setLightWeightPopupEnabled(false);
            ButtonGroup levelMenuItemsGoup = new ButtonGroup();
            for (final Level level : selectableLevelsList) {
              AbstractAction action = new AbstractAction(level.getName()) {
                  public void actionPerformed(ActionEvent ev) {
                    home.setSelectedLevel(level);
                  }
                };
              JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(action);
              menuItem.setSelected(level == home.getSelectedLevel());
              levelMenuItemsGoup.add(menuItem);
              popupMenu.add(menuItem);
            }
            if (!selectableLevelsList.isEmpty()
                && !selectableCamerasList.isEmpty()) {
              popupMenu.addSeparator();
            }
            for (final Camera camera : selectableCamerasList) {
              popupMenu.add(new AbstractAction(camera.getName()) {
                  public void actionPerformed(ActionEvent ev) {
                    controller.goToCamera(camera);
                  }
                });
            }
            view3D.setComponentPopupMenu(popupMenu);
          }

          rootPane.setContentPane(view3D);
          rootPane.revalidate();
        }
      });
  }

  /**
   * Returns a home level from its name.
   */
  private Level getLevel(Home home, String levelName) {
    for (Level level : home.getLevels()) {
      if (levelName.equals(level.getName())) {
        return level;
      }
    }
    return null;
  }

  /**
   * Returns a home stored camera from its name.
   */
  private Camera getStoredCamera(Home home, String cameraName) {
    for (Camera level : home.getStoredCameras()) {
      if (cameraName.equals(level.getName())) {
        return level;
      }
    }
    return null;
  }
}
