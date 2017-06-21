package com.eteks.homeview3d;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.ServiceManagerStub;
import javax.jnlp.SingleInstanceListener;
import javax.jnlp.SingleInstanceService;
import javax.jnlp.UnavailableServiceException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

import com.eteks.homeview3d.io.AutoRecoveryManager;
import com.eteks.homeview3d.io.FileUserPreferences;
import com.eteks.homeview3d.io.HomeFileRecorder;
import com.eteks.homeview3d.j3d.Component3DManager;
import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.model.HomeRecorder;
import com.eteks.homeview3d.model.Library;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.plugin.HomePluginController;
import com.eteks.homeview3d.plugin.PluginManager;
import com.eteks.homeview3d.swing.FileContentManager;
import com.eteks.homeview3d.swing.SwingTools;
import com.eteks.homeview3d.swing.SwingViewFactory;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.View;
import com.eteks.homeview3d.viewcontroller.ViewFactory;

public class HomeView3D extends HomeApplication {
  private static final String     PREFERENCES_FOLDER             = "com.eteks.homeview3d.preferencesFolder";
  private static final String     APPLICATION_FOLDERS            = "com.eteks.homeview3d.applicationFolders";
  private static final String     APPLICATION_PLUGINS_SUB_FOLDER = "plugins";

  private HomeRecorder            homeRecorder;
  private HomeRecorder            compressedHomeRecorder;
  private UserPreferences         userPreferences;
  private ContentManager          contentManager;
  private ViewFactory             viewFactory;
  private PluginManager           pluginManager;
  private boolean                 pluginManagerInitialized;
  private boolean                 checkUpdatesNeeded;
  private AutoRecoveryManager     autoRecoveryManager;
  private final Map<Home, HomeFrameController> homeFrameControllers;

  protected HomeView3D() {
    this.homeFrameControllers = new HashMap<Home, HomeFrameController>();
  }

  @Override
  public HomeRecorder getHomeRecorder() {

    if (this.homeRecorder == null) {
      this.homeRecorder = new HomeFileRecorder(0, false, getUserPreferences(), false, true);
    }
    return this.homeRecorder;
  }

  @Override
  public HomeRecorder getHomeRecorder(HomeRecorder.Type type) {
    if (type == HomeRecorder.Type.COMPRESSED) {
      if (this.compressedHomeRecorder == null) {
        this.compressedHomeRecorder = new HomeFileRecorder(9, false, getUserPreferences(), false, true);
      }
      return this.compressedHomeRecorder;
    } else {
      return super.getHomeRecorder(type);
    }
  }

  @Override
  public UserPreferences getUserPreferences() {
    if (this.userPreferences == null) {
      String preferencesFolderProperty = System.getProperty(PREFERENCES_FOLDER, null);
      String applicationFoldersProperty = System.getProperty(APPLICATION_FOLDERS, null);
      File preferencesFolder = preferencesFolderProperty != null
          ? new File(preferencesFolderProperty)
          : null;
      File [] applicationFolders;
      if (applicationFoldersProperty != null) {
        String [] applicationFoldersProperties = applicationFoldersProperty.split(File.pathSeparator);
        applicationFolders = new File [applicationFoldersProperties.length];
        for (int i = 0; i < applicationFolders.length; i++) {
          applicationFolders [i] = new File(applicationFoldersProperties [i]);
        }
      } else {
        applicationFolders = null;
      }
      Executor eventQueueExecutor = new Executor() {
          public void execute(Runnable command) {
            EventQueue.invokeLater(command);
          }
        };
      this.userPreferences = new FileUserPreferences(preferencesFolder, applicationFolders, eventQueueExecutor) {
          @Override
          public List<Library> getLibraries() {
            if (userPreferences != null
                && getPluginManager() != null) {
              List<Library> pluginLibraries = getPluginManager().getPluginLibraries();
              if (!pluginLibraries.isEmpty()) {
                ArrayList<Library> libraries = new ArrayList<Library>(super.getLibraries());
                libraries.addAll(pluginLibraries);
                return Collections.unmodifiableList(libraries);
              }
            }
            return super.getLibraries();
          }
          
          @Override
          public void deleteLibraries(List<Library> libraries) throws RecorderException {
            if (userPreferences != null 
                && getPluginManager() != null) {
              super.deleteLibraries(libraries);
              List<Library> plugins = new ArrayList<Library>();
              for (Library library : libraries) {
                if (PluginManager.PLUGIN_LIBRARY_TYPE.equals(library.getType())) {
                  plugins.add(library);
                }
              }
              getPluginManager().deletePlugins(plugins);
            }
          }
        };
      this.checkUpdatesNeeded = this.userPreferences.isCheckUpdatesEnabled();
    }
    return this.userPreferences;
  }

  protected ContentManager getContentManager() {
    if (this.contentManager == null) {
      this.contentManager = new FileContentManagerWithRecordedLastDirectories(getUserPreferences(), getClass());
    }
    return this.contentManager;
  }

  protected ViewFactory getViewFactory() {
    if (this.viewFactory == null) {
      this.viewFactory = new SwingViewFactory();
    }
    return this.viewFactory;
  }

  protected PluginManager getPluginManager() {
    if (!this.pluginManagerInitialized) {
      try {
        UserPreferences userPreferences = getUserPreferences();
        if (userPreferences instanceof FileUserPreferences) {
          File [] applicationPluginsFolders = ((FileUserPreferences) userPreferences)
              .getApplicationSubfolders(APPLICATION_PLUGINS_SUB_FOLDER);
          this.pluginManager = new PluginManager(applicationPluginsFolders);
        }
      } catch (IOException ex) {
      }
      this.pluginManagerInitialized = true;
    }
    return this.pluginManager;
  }

  @Override
  public String getId() {
    String applicationId = System.getProperty("com.eteks.homeview3d.applicationId");
    if (applicationId != null && applicationId.length() > 0) {
      return applicationId;
    } else {
      try {
        return getUserPreferences().getLocalizedString(HomeView3D.class, "applicationId");
      } catch (IllegalArgumentException ex) {
        return super.getId();
      }
    }
  }
  

  @Override
  public String getName() {
    return getUserPreferences().getLocalizedString(HomeView3D.class, "applicationName");
  }

  public String getVersion() {
    String applicationVersion = System.getProperty("com.eteks.homeview3d.applicationVersion");
    if (applicationVersion != null) {
      return applicationVersion;
    } else {
      return getUserPreferences().getLocalizedString(HomeView3D.class, "applicationVersion");
    }
  }


  JFrame getHomeFrame(Home home) {
    return (JFrame)SwingUtilities.getRoot((JComponent)this.homeFrameControllers.get(home).getView());
  }

  HomeFrameController getHomeFrameController(Home home) {
    return this.homeFrameControllers.get(home);
  }

  private void showHomeFrame(Home home) {
    final JFrame homeFrame = getHomeFrame(home);
    homeFrame.setVisible(true);
    homeFrame.setState(JFrame.NORMAL);
    homeFrame.toFront();
  }


  public static void main(final String [] args) {
    new HomeView3D().init(args);
  }

  protected void init(final String [] args) {
    initSystemProperties();

    if (ServiceManager.getServiceNames() == null) {
      if (StandaloneSingleInstanceService.callSingleInstanceServer(args, getClass())) {
        System.exit(0);
      } else {
        SwingTools.showSplashScreenWindow(HomeView3D.class.getResource("resources/splashScreen.jpg"));
        ServiceManager.setServiceManagerStub(new StandaloneServiceManager(getClass()));
      }
    }

    SingleInstanceService service = null;
    final SingleInstanceListener singleInstanceListener = new SingleInstanceListener() {
      public void newActivation(final String [] args) {
        EventQueue.invokeLater(new Runnable() {
          public void run() {
            HomeView3D.this.start(args);
          }
        });
      }
    };
    try {
      service = (SingleInstanceService) ServiceManager.lookup("javax.jnlp.SingleInstanceService");
      service.addSingleInstanceListener(singleInstanceListener);
    } catch (UnavailableServiceException ex) {
    }

    final SingleInstanceService singleInstanceService = service;

    addHomesListener(new CollectionListener<Home>() {
      private boolean firstApplicationHomeAdded;

      public void collectionChanged(CollectionEvent<Home> ev) {
        switch (ev.getType()) {
          case ADD:
            Home home = ev.getItem();
            try {
              HomeFrameController controller = createHomeFrameController(home);
              controller.displayView();
              if (!this.firstApplicationHomeAdded) {
                this.firstApplicationHomeAdded = true;
                addNewHomeCloseListener(home, controller.getHomeController());
              }

              homeFrameControllers.put(home, controller);
            } catch (IllegalStateException ex) {
              if ("javax.media.j3d.IllegalRenderingStateException".equals(ex.getClass().getName())) {
                ex.printStackTrace();
                exitAfter3DError();
              } else {
                throw ex;
              }
            }
            break;
          case DELETE:
            homeFrameControllers.remove(ev.getItem());

            if (getHomes().isEmpty() && !OperatingSystem.isMacOSX()) {
              if (singleInstanceService != null) {
                singleInstanceService.removeSingleInstanceListener(singleInstanceListener);
              }
              EventQueue.invokeLater(new Runnable() {
                  public void run() {
                    System.exit(0);
                  }
                });
            }
            break;
        }
      };
    });

    addComponent3DRenderingErrorObserver();

    getUserPreferences();
    try {
      System.setProperty("http.agent", getId() + "/" + getVersion()  
           + " (" + System.getProperty("os.name") + " " + System.getProperty("os.version") + "; " + System.getProperty("os.arch") + "; " + Locale.getDefault() + ")");
    } catch (AccessControlException ex) {
    }
    initLookAndFeel();
    try {
      this.autoRecoveryManager = new AutoRecoveryManager(this);
    } catch (RecorderException ex) {
      ex.printStackTrace();
    }
    if (OperatingSystem.isMacOSX()) {
      MacOSXConfiguration.bindToApplicationMenu(this);
    }

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        HomeView3D.this.start(args);
      }
    });
  }

  protected HomeFrameController createHomeFrameController(Home home) {
    return new HomeFrameController(home, this, getViewFactory(), getContentManager(), getPluginManager());
  }

  private void initSystemProperties() {
    if (OperatingSystem.isMacOSX()) {
      String classPackage = HomeView3D.class.getName();
      classPackage = classPackage.substring(0, classPackage.lastIndexOf("."));
      ResourceBundle resource = ResourceBundle.getBundle(classPackage + "." + "package");
      String applicationName = resource.getString("HomeView3D.applicationName");
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", applicationName);
      System.setProperty("apple.awt.application.name", applicationName);
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.graphics.UseQuartz", "true");
      if (System.getProperty("com.eteks.homeview3d.dragAndDropWithoutTransferHandler") == null
          && OperatingSystem.isJavaVersionBetween("1.7", "1.8.0_40")) {
        System.setProperty("com.eteks.homeview3d.dragAndDropWithoutTransferHandler", "true");
      }
    }
    if (System.getProperty("java.net.useSystemProxies") == null) {
      System.setProperty("java.net.useSystemProxies", "true");
    }
  }

  private void initLookAndFeel() {
    try {
      UIManager.setLookAndFeel(System.getProperty("swing.defaultlaf", UIManager.getSystemLookAndFeelClassName()));
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        UIManager.put("TitledBorder.border", UIManager.getBorder("TitledBorder.aquaVariant"));
      }
      if (OperatingSystem.isMacOSXYosemiteOrSuperior()) {
        UIManager.put("SplitPaneDivider.border", new Border() {
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
              ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
              g.setColor(UIManager.getColor("SplitPane.background"));
              Shape clip = g.getClip();
              Area clipArea = new Area(new Rectangle2D.Float(x - 0.5f, y - 0.5f, width + 1f, height + 1f));
              clipArea.subtract(new Area(new Ellipse2D.Float(x + width / 2f - 3.4f, y + height / 2f - 3.2f, 6.8f, 6.8f)));
              JSplitPane splitPane = ((BasicSplitPaneDivider)c).getBasicSplitPaneUI().getSplitPane();
              if (splitPane.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
                clipArea.subtract(new Area(new Polygon(new int [] {x, x + 4, x + 8}, 
                    new int [] {y + height / 2 + 3, y + height / 2 - 3, y + height / 2 + 3}, 3)));
                clipArea.subtract(new Area(new Polygon(new int [] {x + 12, x + 15, x + 19}, 
                    new int [] {y + height / 2 - 2, y + height / 2 + 3, y + height / 2 - 2}, 3)));
              }
              g.setClip(clipArea);
              g.fillRect(x, y + height / 2 - 5, x + width, 11);
              g.setClip(clip);
            }
            
            public boolean isBorderOpaque() {
              return true;
            }
            
            public Insets getBorderInsets(Component c) {
              return new Insets(0, 0, 0, 0);
            }
          });
      }
      SwingTools.updateSwingResourceLanguage(getUserPreferences());
    } catch (Exception ex) {
    }
  }

  private void addNewHomeCloseListener(final Home home, final HomeController controller) {
    if (home.getName() == null) {
      final CollectionListener<Home> newHomeListener = new CollectionListener<Home>() {
        public void collectionChanged(CollectionEvent<Home> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            if (ev.getItem().getName() != null 
                && home.getName() == null
                && !home.isRecovered()) {
              if (OperatingSystem.isMacOSXLionOrSuperior()
                  && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")
                  && MacOSXConfiguration.isWindowFullScreen(getHomeFrame(home))) {
                new Timer(3000, new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      ((Timer)ev.getSource()).stop();
                      controller.close();
                    }
                  }).start();
              } else {
                controller.close();
              }
            }
            removeHomesListener(this);
          } else if (ev.getItem() == home && ev.getType() == CollectionEvent.Type.DELETE) {
            removeHomesListener(this);
          }
        }
      };
      addHomesListener(newHomeListener);
      home.addPropertyChangeListener(Home.Property.MODIFIED, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          removeHomesListener(newHomeListener);
          home.removePropertyChangeListener(Home.Property.MODIFIED, this);
        }
      });
    }
  }

  private void addComponent3DRenderingErrorObserver() {
    if (!Boolean.getBoolean("com.eteks.homeview3d.no3D")) { 
      Component3DManager.getInstance().setRenderingErrorObserver(new Component3DManager.RenderingErrorObserver() {
          public void errorOccured(int errorCode, String errorMessage) {
            System.err.print("Error in Java 3D : " + errorCode + " " + errorMessage);
            EventQueue.invokeLater(new Runnable() {
              public void run() {
                exitAfter3DError();
              }
            });
          }
        });
    }
  }

  private void exitAfter3DError() {
    boolean modifiedHomes = false;
    for (Home home : getHomes()) {
      if (home.isModified()) {
        modifiedHomes = true;
        break;
      }
    }

    if (!modifiedHomes) {
      show3DError();
    } else if (confirmSaveAfter3DError()) {
      for (Home home : getHomes()) {
        if (home.isModified()) {
          String homeName = home.getName();
          if (homeName == null) {
            JFrame homeFrame = getHomeFrame(home);
            homeFrame.toFront();
            homeName = contentManager.showSaveDialog((View) homeFrame.getRootPane(), null,
                ContentManager.ContentType.SWEET_HOME_3D, null);
          }
          if (homeName != null) {
            try {
              getHomeRecorder().writeHome(home, homeName);
            } catch (RecorderException ex) {
              ex.printStackTrace();
            }
          }
          deleteHome(home);
        }
      }
    }
    for (Home home : getHomes()) {
      deleteHome(home);
    }
    System.exit(0);
  }
  private void show3DError() {
    UserPreferences userPreferences = getUserPreferences();
    String message = userPreferences.getLocalizedString(HomeView3D.class, "3DError.message");
    String title = userPreferences.getLocalizedString(HomeView3D.class, "3DError.title");
    JOptionPane.showMessageDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(), message,
        title, JOptionPane.ERROR_MESSAGE);
  }
  private boolean confirmSaveAfter3DError() {
    UserPreferences userPreferences = getUserPreferences();
    String message = userPreferences.getLocalizedString(HomeView3D.class, "confirmSaveAfter3DError.message");
    String title = userPreferences.getLocalizedString(HomeView3D.class, "confirmSaveAfter3DError.title");
    String save = userPreferences.getLocalizedString(HomeView3D.class, "confirmSaveAfter3DError.save");
    String doNotSave = userPreferences.getLocalizedString(HomeView3D.class, "confirmSaveAfter3DError.doNotSave");

    return JOptionPane.showOptionDialog(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
        message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object [] {save, doNotSave},
        save) == JOptionPane.YES_OPTION;
  }

  protected void start(String [] args) {
    if (args.length == 2 && args [0].equals("-open") && args [1].length() > 0) {
      // If requested home is already opened, show it
      for (Home home : getHomes()) {
        if (args [1].equals(home.getName())) {
          showHomeFrame(home);
          return;
        }
      }
      
      if (getContentManager().isAcceptable(args [1], ContentManager.ContentType.SWEET_HOME_3D)) {
        addHomesListener(new CollectionListener<Home>() {
            public void collectionChanged(CollectionEvent<Home> ev) {
              if (ev.getType() == CollectionEvent.Type.ADD) {
                removeHomesListener(this);
                if (autoRecoveryManager != null) {
                  autoRecoveryManager.openRecoveredHomes();
                }
              }
            }
          });
        createHomeFrameController(createHome()).getHomeController().open(args [1]);
        checkUpdates();
      } else if (getContentManager().isAcceptable(args [1], ContentManager.ContentType.LANGUAGE_LIBRARY)) {
        showDefaultHomeFrame();
        final String languageLibraryName = args [1];
        EventQueue.invokeLater(new Runnable() {
          public void run() {
            List<String> supportedLanguages = Arrays.asList(getUserPreferences().getSupportedLanguages());
            createHomeFrameController(createHome()).getHomeController().importLanguageLibrary(languageLibraryName);
            for (String language : getUserPreferences().getSupportedLanguages()) {
              if (!supportedLanguages.contains(language)) {
                getUserPreferences().setLanguage(language);
                break;
              }
            }
            checkUpdates();
          }
        });
      } else if (getContentManager().isAcceptable(args [1], ContentManager.ContentType.FURNITURE_LIBRARY)) {
        showDefaultHomeFrame();
        final String furnitureLibraryName = args [1];
        EventQueue.invokeLater(new Runnable() {
          public void run() {
            createHomeFrameController(createHome()).getHomeController().importFurnitureLibrary(furnitureLibraryName);
            checkUpdates();
          }
        });
      } else if (getContentManager().isAcceptable(args [1], ContentManager.ContentType.TEXTURES_LIBRARY)) {
        showDefaultHomeFrame();
        final String texturesLibraryName = args [1];
        EventQueue.invokeLater(new Runnable() {
          public void run() {
            createHomeFrameController(createHome()).getHomeController().importTexturesLibrary(texturesLibraryName);
            checkUpdates();
          }
        });
      } else if (getContentManager().isAcceptable(args [1], ContentManager.ContentType.PLUGIN)) {
        showDefaultHomeFrame();
        final String pluginName = args [1];
        EventQueue.invokeLater(new Runnable() {
          public void run() {
 
            HomeController homeController = createHomeFrameController(createHome()).getHomeController();
            if (homeController instanceof HomePluginController) {
              ((HomePluginController)homeController).importPlugin(pluginName);
            }
            checkUpdates();
          }
        });
      }
    } else { 
      showDefaultHomeFrame();
      checkUpdates();
    }
  }

  private void showDefaultHomeFrame() {
    if (getHomes().isEmpty()) {
      if (this.autoRecoveryManager != null) {
        this.autoRecoveryManager.openRecoveredHomes();
      }
      if (getHomes().isEmpty()) {
        addHome(createHome());
      }
    } else {
      final List<Home> homes = getHomes();
      Home home = null;
      for (int i = homes.size() - 1; i >= 0; i--) {
        JFrame homeFrame = getHomeFrame(homes.get(i));
        if (homeFrame.isActive() || homeFrame.getState() != JFrame.ICONIFIED) {
          home = homes.get(i);
          break;
        }
      }
      if (home == null) {
        for (int i = homes.size() - 1; i >= 0; i--) {
          JFrame homeFrame = getHomeFrame(homes.get(i));
          if (homeFrame.isDisplayable()) {
            home = homes.get(i);
            break;
          }
        }
      }
 
      showHomeFrame(home);
    }
  }

  private void checkUpdates() {
    if (this.checkUpdatesNeeded) {
      this.checkUpdatesNeeded = false;
      new Timer(500, new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            ((Timer)ev.getSource()).stop();
            createHomeFrameController(createHome()).getHomeController().checkUpdates(true);
          }
        }).start();
    }
  }

  private static class FileContentManagerWithRecordedLastDirectories extends FileContentManager {
    private static final String LAST_DIRECTORY         = "lastDirectory#";
    private static final String LAST_DEFAULT_DIRECTORY = "lastDefaultDirectory";
    
    private final Class<? extends HomeView3D> mainClass;

    public FileContentManagerWithRecordedLastDirectories(UserPreferences preferences, 
                                                         Class<? extends HomeView3D> mainClass) {
      super(preferences);
      this.mainClass = mainClass;
    }

    @Override
    protected File getLastDirectory(ContentType contentType) {
      Preferences preferences = Preferences.userNodeForPackage(this.mainClass);
      String directoryPath = null;
      if (contentType != null) {
        directoryPath = preferences.get(LAST_DIRECTORY + contentType, null);
      }
      if (directoryPath == null) {
        directoryPath = preferences.get(LAST_DEFAULT_DIRECTORY, null);
      }
      if (directoryPath != null) {
        File directory = new File(directoryPath);
        if (directory.isDirectory()) {
          return directory;
        } 
      }
      return null;
    }
    
    @Override
    protected void setLastDirectory(ContentType contentType, File directory) {
      Preferences preferences = Preferences.userNodeForPackage(this.mainClass);
      if (directory == null) {
        preferences.remove(LAST_DIRECTORY + contentType);
      } else {
        String directoryPath = directory.getAbsolutePath();
        if (contentType != null) {
          preferences.put(LAST_DIRECTORY + contentType, directoryPath);
        }
        if (directoryPath != null) {
          preferences.put(LAST_DEFAULT_DIRECTORY, directoryPath);
        }
      }
      try {
        preferences.flush();
      } catch (BackingStoreException ex) {
      }
    }
  }

  private static class StandaloneServiceManager implements ServiceManagerStub {
    private final Class<? extends HomeView3D> mainClass;

    public StandaloneServiceManager(Class<? extends HomeView3D> mainClass) {
      this.mainClass = mainClass;
    }

    public Object lookup(final String name) throws UnavailableServiceException {
      if (name.equals("javax.jnlp.BasicService")) {
        return new StandaloneBasicService();
      } else if (name.equals("javax.jnlp.SingleInstanceService")) {
        return new StandaloneSingleInstanceService(this.mainClass);
      } else {
        throw new UnavailableServiceException(name);
      }
    }

    public String [] getServiceNames() {
      return new String [] {"javax.jnlp.BasicService", "javax.jnlp.SingleInstanceService"};
    }
  }

  private static class StandaloneBasicService implements BasicService {
    public boolean showDocument(URL url) {
      try {
        if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
          Class<?> desktopClass = Class.forName("java.awt.Desktop");
          Object desktopInstance = desktopClass.getMethod("getDesktop").invoke(null);
          desktopClass.getMethod("browse", URI.class).invoke(desktopInstance, url.toURI());
          return true;
        }
      } catch (Exception ex) {
        try {
          if (OperatingSystem.isMacOSX()) {
            Runtime.getRuntime().exec(new String [] {"open", url.toString()});
            return true;
          } else if (OperatingSystem.isLinux()) {
            Runtime.getRuntime().exec(new String [] {"xdg-open", url.toString()});
            return true;
          }  
        } catch (IOException ex2) {
        }
      }
      return false;
    }

    public URL getCodeBase() {
      return StandaloneServiceManager.class.getResource("resources");
    }

    public boolean isOffline() {
      return false;
    }

    public boolean isWebBrowserSupported() {
      if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
        try {
          Class<?> desktopClass = Class.forName("java.awt.Desktop");
          Object desktopInstance = desktopClass.getMethod("getDesktop").invoke(null);
          Class<?> desktopActionClass = Class.forName("java.awt.Desktop$Action");
          Object desktopBrowseAction = desktopActionClass.getMethod("valueOf", String.class).invoke(null, "BROWSE");
          if ((Boolean)desktopClass.getMethod("isSupported", desktopActionClass).invoke(desktopInstance,
              desktopBrowseAction)) {
            return true;
          }
        } catch (Exception ex) {
        }
      }
      return OperatingSystem.isMacOSX() || OperatingSystem.isLinux();
    }
  }

  private static class StandaloneSingleInstanceService implements SingleInstanceService {
    private static final String                SINGLE_INSTANCE_PORT    = "singleInstancePort";

    private final Class<? extends HomeView3D> mainClass;
    private final List<SingleInstanceListener> singleInstanceListeners = new ArrayList<SingleInstanceListener>();

    public StandaloneSingleInstanceService(Class<? extends HomeView3D> mainClass) {
      this.mainClass = mainClass;
    }

    public void addSingleInstanceListener(SingleInstanceListener l) {
      if (this.singleInstanceListeners.isEmpty()) {
        if (!OperatingSystem.isMacOSX()) {
          launchSingleInstanceServer();
        }
      }
      this.singleInstanceListeners.add(l);
    }

    private void launchSingleInstanceServer() {
      final ServerSocket serverSocket;
      try {
        serverSocket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));
        Preferences preferences = Preferences.userNodeForPackage(this.mainClass);
        preferences.putInt(SINGLE_INSTANCE_PORT, serverSocket.getLocalPort());
        preferences.flush();
      } catch (IOException ex) {
        return;
      } catch (BackingStoreException ex) {
        return;
      }

      Executors.newSingleThreadExecutor().execute(new Runnable() {
        public void run() {
          try {
            while (true) {
              Socket socket = serverSocket.accept();
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
              String [] params = reader.readLine().split("\t");
              reader.close();
              socket.close();

              SingleInstanceListener [] listeners = singleInstanceListeners
                  .toArray(new SingleInstanceListener [singleInstanceListeners.size()]);
              for (SingleInstanceListener listener : listeners) {
                listener.newActivation(params);
              }
            }
          } catch (IOException ex) {
            launchSingleInstanceServer();
          }
        }
      });
    }

    public void removeSingleInstanceListener(SingleInstanceListener l) {
      this.singleInstanceListeners.remove(l);
      if (this.singleInstanceListeners.isEmpty()) {
        Preferences preferences = Preferences.userNodeForPackage(this.mainClass);
        preferences.remove(SINGLE_INSTANCE_PORT);
        try {
          preferences.flush();
        } catch (BackingStoreException ex) {
          throw new RuntimeException(ex);
        }
      }
    }

    public static boolean callSingleInstanceServer(String [] mainArgs, Class<? extends HomeView3D> mainClass) {
      if (!OperatingSystem.isMacOSX()) {
        Preferences preferences = Preferences.userNodeForPackage(mainClass);
        int singleInstancePort = preferences.getInt(SINGLE_INSTANCE_PORT, -1);
        if (singleInstancePort != -1) {
          try {
            Socket socket = new Socket("127.0.0.1", singleInstancePort);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            for (String arg : mainArgs) {
              writer.write(arg);
              writer.write("\t");
            }
            writer.write("\n");
            writer.close();
            socket.close();
            return true;
          } catch (IOException ex) {
          }
        }
      }
      return false;
    }
  }
}
