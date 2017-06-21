
package com.eteks.homeview3d.applet;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import com.eteks.homeview3d.io.HomeFileRecorder;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.model.HomeRecorder;
import com.eteks.homeview3d.model.InterruptedRecorderException;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.plugin.HomePluginController;
import com.eteks.homeview3d.plugin.PluginManager;
import com.eteks.homeview3d.swing.FileContentManager;
import com.eteks.homeview3d.swing.SwingTools;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.HomeView;
import com.eteks.homeview3d.viewcontroller.ThreadedTaskController;
import com.eteks.homeview3d.viewcontroller.UserPreferencesController;
import com.eteks.homeview3d.viewcontroller.ViewFactory;


public class HomeAppletController extends HomePluginController {
  private final Home               home;
  private final HomeApplication    application;
  private final ViewFactory        viewFactory;
  private final ContentManager     contentManager;
  private final boolean            newHomeEnabledByDefault;
  private final boolean            openEnabledByDefault;
  private final boolean            saveEnabledByDefault;
  private final boolean            saveAsEnabledByDefault;
  private final long               homeMaximumLength;
  private HomeView                 homeView;
  
  private static Map<Home, String> importedHomeNames = new WeakHashMap<Home, String>();

  public HomeAppletController(Home home, 
                              HomeApplication application, 
                              ViewFactory     viewFactory,
                              ContentManager  contentManager,
                              PluginManager   pluginManager,
                              boolean newHomeEnabled, 
                              boolean openEnabled, 
                              boolean saveEnabled, 
                              boolean saveAsEnabled) {
    this(home, application, viewFactory, contentManager, pluginManager, newHomeEnabled, openEnabled, saveEnabled, saveAsEnabled, -1);
  }
  
  public HomeAppletController(Home home, 
                              HomeApplication application, 
                              ViewFactory     viewFactory,
                              ContentManager  contentManager,
                              PluginManager   pluginManager,
                              boolean newHomeEnabled, 
                              boolean openEnabled, 
                              boolean saveEnabled, 
                              boolean saveAsEnabled,
                              long    homeMaximumLength) {
    super(home, application, viewFactory, contentManager, pluginManager);
    this.home = home;
    this.application = application;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.newHomeEnabledByDefault = newHomeEnabled;
    this.openEnabledByDefault = openEnabled;
    this.saveEnabledByDefault = saveEnabled;
    this.saveAsEnabledByDefault = saveAsEnabled;
    this.homeMaximumLength = homeMaximumLength;
  }
  
  /**
   * 이걸로 돌아가기
   */
  public HomeView getView() {
    if (this.homeView == null) {
      this.homeView = super.getView();
      
      // 업데이트 home view actions 
      this.homeView.setEnabled(HomeView.ActionType.EXIT, false);
      this.homeView.setEnabled(HomeView.ActionType.NEW_HOME, this.newHomeEnabledByDefault);
      this.homeView.setEnabled(HomeView.ActionType.OPEN, this.openEnabledByDefault);
      this.homeView.setEnabled(HomeView.ActionType.SAVE, this.saveEnabledByDefault);
      this.homeView.setEnabled(HomeView.ActionType.SAVE_AS, this.saveAsEnabledByDefault);
      
      this.homeView.setEnabled(HomeView.ActionType.PRINT_TO_PDF, false);
      this.homeView.setEnabled(HomeView.ActionType.EXPORT_TO_SVG, false);
      this.homeView.setEnabled(HomeView.ActionType.EXPORT_TO_OBJ, false);
      this.homeView.setEnabled(HomeView.ActionType.CREATE_PHOTO, false);
      
      this.homeView.setEnabled(HomeView.ActionType.DETACH_3D_VIEW, false);
    }
    return this.homeView;
  }

  /**
   * 저장/삭제후 새로운집 열기
   */
  @Override
  public void newHome() {
    close(new Runnable() {
        public void run() {
          HomeAppletController.super.newHome();
        }
      });
  }

  /**
   * 저장/삭제후 새로운집 열기
   */
  @Override
  public void open() {
    close(new Runnable() {
      public void run() {
        HomeAppletController.super.open();
      }
    });
  }
  

  @Override
  public void save() {
    if (this.home.getName() != null) {
      chekHomeLengthAndSave(new Runnable() {
          public void run() {
            HomeAppletController.super.save();
          }
        });
    } else {
      super.saveAs();
    }
  }
  

  @Override
  protected void saveAs(final HomeRecorder.Type recorderType, 
                        final Runnable postSaveTask) {
    chekHomeLengthAndSave(new Runnable() {
        public void run() {
          String homeName = importedHomeNames.get(home);
          if (homeName != null) {
            // 들여올 이름제안
            home.setName(homeName);
            HomeAppletController.super.saveAs(recorderType, new Runnable () {
                public void run() {
                  if (postSaveTask != null) {
                    postSaveTask.run();
                  }
                  importedHomeNames.remove(home);
                }
              });
            // 리셋
            home.setName(null);
          } else {
            HomeAppletController.super.saveAs(recorderType, postSaveTask);
          }
        }
      });
  }
  

  private void chekHomeLengthAndSave(final Runnable saveTask) {
    if (this.homeMaximumLength > 0) {
      // 스레드에 홈 길이 체크
      Callable<Void> exportToObjTask = new Callable<Void>() {
          public Void call() throws RecorderException {
            final long homeLength = ((HomeAppletRecorder)application.getHomeRecorder()).getHomeLength(home);
            getView().invokeLater(new Runnable() {
                public void run() {
                  if (homeLength > homeMaximumLength) {
                    String message = getHomeLengthMessage(homeLength);
                    getView().showError(message);
                  } else {
                    saveTask.run();
                  }
                }
              });
  
            return null;
          }
        };
      ThreadedTaskController.ExceptionHandler exceptionHandler = 
          new ThreadedTaskController.ExceptionHandler() {
            public void handleException(Exception ex) {
              if (!(ex instanceof InterruptedRecorderException)) {
                if (ex instanceof RecorderException) {
                  String message = application.getUserPreferences().getLocalizedString(
                      HomeController.class, "saveError", home.getName());
                  getView().showError(message);
                } else {
                  ex.printStackTrace();
                }
              }
            }
          };
      new ThreadedTaskController(exportToObjTask, 
          this.application.getUserPreferences().getLocalizedString(HomeAppletController.class, "chekHomeLengthMessage"), exceptionHandler, 
          this.application.getUserPreferences(), viewFactory).executeTask(getView());
    } else {
      saveTask.run();
    }
  }

  /**
   * 유저가이드 보여줌
   */
  @Override
  public void help() {
    try { 
      String helpIndex = this.application.getUserPreferences().getLocalizedString(HomeAppletController.class, "helpIndex");
      SwingTools.showDocumentInBrowser(new URL(helpIndex)); 
    } catch (MalformedURLException ex) {
      ex.printStackTrace();
    } 
  }

  public void exportToSH3D() {
    final String sh3dName = new FileContentManager(this.application.getUserPreferences()).showSaveDialog(getView(),
        this.application.getUserPreferences().getLocalizedString(HomeAppletController.class, "exportToSH3DDialog.title"), 
        ContentManager.ContentType.SWEET_HOME_3D, home.getName());    
    if (sh3dName != null) {
      Callable<Void> exportToObjTask = new Callable<Void>() {
          public Void call() throws RecorderException {
            new HomeFileRecorder(9).writeHome(home, sh3dName);
            return null;
          }
        };
      ThreadedTaskController.ExceptionHandler exceptionHandler = 
          new ThreadedTaskController.ExceptionHandler() {
            public void handleException(Exception ex) {
              if (!(ex instanceof InterruptedRecorderException)) {
                if (ex instanceof RecorderException) {
                  String message = application.getUserPreferences().getLocalizedString(
                      HomeAppletController.class, "exportToSH3DError", sh3dName);
                  getView().showError(message);
                } else {
                  ex.printStackTrace();
                }
              }
            }
          };
      new ThreadedTaskController(exportToObjTask, 
          this.application.getUserPreferences().getLocalizedString(HomeAppletController.class, "exportToSH3DMessage"), exceptionHandler, 
          this.application.getUserPreferences(), viewFactory).executeTask(getView());
    }
  }
  
  public void importFromSH3D() {
    close(new Runnable() {
      public void run() {
        final String sh3dName = new FileContentManager(application.getUserPreferences()).showOpenDialog(getView(),
            application.getUserPreferences().getLocalizedString(HomeAppletController.class, "importFromSH3DDialog.title"), 
            ContentManager.ContentType.SWEET_HOME_3D);    
        if (sh3dName != null) {
          Callable<Void> exportToObjTask = new Callable<Void>() {
              public Void call() throws RecorderException {
                final Home openedHome = new HomeFileRecorder(9, true, application.getUserPreferences(), true).readHome(sh3dName);
                String name = new File(sh3dName).getName();
                name = name.substring(0, name.lastIndexOf("."));
                importedHomeNames.put(openedHome, name);
                openedHome.setName(null);
                openedHome.setModified(true);
                final long homeLength = homeMaximumLength > 0
                    ? ((HomeAppletRecorder)application.getHomeRecorder()).getHomeLength(openedHome)
                    : -1;
                getView().invokeLater(new Runnable() {
                    public void run() {
                      application.addHome(openedHome);
                      if (homeLength > homeMaximumLength) {
                        String message = getHomeLengthMessage(homeLength);
                        getView().showMessage(message);
                      } 
                    }
                  });
                return null;
              }
            };
          ThreadedTaskController.ExceptionHandler exceptionHandler = 
              new ThreadedTaskController.ExceptionHandler() {
                public void handleException(Exception ex) {
                  if (!(ex instanceof InterruptedRecorderException)) {
                    if (ex instanceof RecorderException) {
                      String message = application.getUserPreferences().getLocalizedString(
                          HomeAppletController.class, "importFromSH3DError", sh3dName);
                      getView().showError(message);
                    } else {
                      ex.printStackTrace();
                    }
                  }
                }
              };
          new ThreadedTaskController(exportToObjTask, 
              application.getUserPreferences().getLocalizedString(HomeAppletController.class, "importFromSH3DMessage"), exceptionHandler, 
              application.getUserPreferences(), viewFactory).executeTask(getView());
        }
      }
    });
  }
  
  /**
   * 메시지 돌려줌
   */
  private String getHomeLengthMessage(long homeLength) {
    DecimalFormat decimalFormat = new DecimalFormat("#.#");
    String homeLengthText = decimalFormat.format(Math.max(0.1f, homeLength / 1048576f));
    String homeMaximumLengthText = decimalFormat.format(Math.max(0.1f, this.homeMaximumLength / 1048576f));
    return application.getUserPreferences().getLocalizedString(
        HomeAppletController.class, "homeLengthError", homeLengthText, homeMaximumLengthText);
  }
  
  @Override
  public void editPreferences() {
    new UserPreferencesController(this.application.getUserPreferences(), 
        this.viewFactory, this.contentManager) {
      public boolean isPropertyEditable(UserPreferencesController.Property property) {
        switch (property) {
          case CHECK_UPDATES_ENABLED :
          case AUTO_SAVE_DELAY_FOR_RECOVERY :
          case AUTO_SAVE_FOR_RECOVERY_ENABLED :
            return false;
          default :
            return super.isPropertyEditable(property);
        }
      }
    }.displayView(getView());
  }
}

