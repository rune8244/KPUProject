package com.eteks.homeview3d;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.Timer;

import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.swing.SwingTools;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.HomeView;
import com.eteks.homeview3d.viewcontroller.View;

public class HomeFramePane extends JRootPane implements View {
  private static final String FRAME_X_VISUAL_PROPERTY         = "com.eteks.homeview3d.HomeView3D.FrameX";
  private static final String FRAME_Y_VISUAL_PROPERTY         = "com.eteks.homeview3d.HomeView3D.FrameY";
  private static final String FRAME_WIDTH_VISUAL_PROPERTY     = "com.eteks.homeview3d.HomeView3D.FrameWidth";
  private static final String FRAME_HEIGHT_VISUAL_PROPERTY    = "com.eteks.homeview3d.HomeView3D.FrameHeight";
  private static final String FRAME_MAXIMIZED_VISUAL_PROPERTY = "com.eteks.homeview3d.HomeView3D.FrameMaximized";
  private static final String SCREEN_WIDTH_VISUAL_PROPERTY    = "com.eteks.homeview3d.HomeView3D.ScreenWidth";
  private static final String SCREEN_HEIGHT_VISUAL_PROPERTY   = "com.eteks.homeview3d.HomeView3D.ScreenHeight";
  
  private final Home                    home;
  private final HomeApplication         application;
  private final ContentManager          contentManager;
  private final HomeFrameController     controller;
  private static int                    newHomeCount;
  private int                           newHomeNumber;
  
  public HomeFramePane(Home home,
                       HomeApplication application,
                       ContentManager contentManager, 
                       HomeFrameController controller) {
    this.home = home;
    this.controller = controller;
    this.application = application;
    this.contentManager = contentManager;
    // 이름이 없을 경우 넘버링
    if (home.getName() == null) {
      this.newHomeNumber = ++newHomeCount;
    }
    HomeView homeView = this.controller.getHomeController().getView();
    setContentPane((JComponent)homeView);
  }

  /**
   *쇼랑 프레임 빌드.
   */
  public void displayView() {
    final JFrame homeFrame = new JFrame() {
      {
        setRootPane(HomeFramePane.this);
      }
    };
    // 프레임 이미지와 타이틀 업데이트
    List<Image> frameImages = new ArrayList<Image>(3);
    frameImages.add(new ImageIcon(HomeFramePane.class.getResource("resources/frameIcon.png")).getImage());
    frameImages.add(new ImageIcon(HomeFramePane.class.getResource("resources/frameIcon32x32.png")).getImage());
    if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
      frameImages.add(new ImageIcon(HomeFramePane.class.getResource("resources/frameIcon128x128.png")).getImage());
    }
    try {
      homeFrame.getClass().getMethod("setIconImages", List.class).invoke(homeFrame, frameImages);
    } catch (Exception ex) {
      homeFrame.setIconImage(frameImages.get(0));
    }
    if (OperatingSystem.isMacOSXLionOrSuperior()) {
      MacOSXConfiguration.installToolBar(this);
    }
    updateFrameTitle(homeFrame, this.home, this.application);
    applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));    
    computeFrameBounds(this.home, homeFrame);
    getToolkit().setDynamicLayout(true); 
    HomeView homeView = this.controller.getHomeController().getView();
    if (homeView instanceof JRootPane) {
      JRootPane homePane = (JRootPane)homeView;
      setJMenuBar(homePane.getJMenuBar());
      homePane.setJMenuBar(null);
    }

    addListeners(this.home, this.application, this.controller.getHomeController(), homeFrame);
    
    homeFrame.setVisible(true);
    EventQueue.invokeLater(new Runnable() {
        public void run() {
          addWindowStateListener(home, application, controller.getHomeController(), homeFrame);

          homeFrame.toFront();
        }
      });
  }

  private void addListeners(final Home home,
                            final HomeApplication application,
                            final HomeController controller,
                            final JFrame frame) {
    final ComponentAdapter componentListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent ev) {
          if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH) {
            controller.setHomeProperty(FRAME_WIDTH_VISUAL_PROPERTY, String.valueOf(frame.getWidth()));
            controller.setHomeProperty(FRAME_HEIGHT_VISUAL_PROPERTY, String.valueOf(frame.getHeight()));
          }
          Dimension userScreenSize = getUserScreenSize();
          controller.setHomeProperty(SCREEN_WIDTH_VISUAL_PROPERTY, String.valueOf(userScreenSize.width));
          controller.setHomeProperty(SCREEN_HEIGHT_VISUAL_PROPERTY, String.valueOf(userScreenSize.height));
        }
        
        @Override
        public void componentMoved(ComponentEvent ev) {
          if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH) {
            controller.setHomeProperty(FRAME_X_VISUAL_PROPERTY, String.valueOf(frame.getX()));
            controller.setHomeProperty(FRAME_Y_VISUAL_PROPERTY, String.valueOf(frame.getY()));
          }
        }
      };
    frame.addComponentListener(componentListener);
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    final WindowAdapter windowListener = new WindowAdapter () {
        private Component mostRecentFocusOwner;

        @Override
        public void windowClosing(WindowEvent ev) {
          controller.close();
        }
        
        @Override
        public void windowDeactivated(WindowEvent ev) {
          Component mostRecentFocusOwner = frame.getMostRecentFocusOwner();          
          if (!(mostRecentFocusOwner instanceof JFrame)
              && mostRecentFocusOwner != null) {
            this.mostRecentFocusOwner = mostRecentFocusOwner;
          }
        }

        @Override
        public void windowActivated(WindowEvent ev) {                    
          if (this.mostRecentFocusOwner != null) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  mostRecentFocusOwner.requestFocusInWindow();
                }
              });
          }
        } 
      };
    frame.addWindowListener(windowListener);    
    application.getUserPreferences().addPropertyChangeListener(UserPreferences.Property.LANGUAGE, 
        new LanguageChangeListener(frame, this));
    application.addHomesListener(new CollectionListener<Home>() {
        public void collectionChanged(CollectionEvent<Home> ev) {
          if (ev.getItem() == home
              && ev.getType() == CollectionEvent.Type.DELETE) {
            application.removeHomesListener(this);
            frame.dispose();
            frame.removeWindowListener(windowListener);
            frame.removeComponentListener(componentListener);
          }
        };
      });
    PropertyChangeListener frameTitleChangeListener = new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent ev) {
          updateFrameTitle(frame, home, application);
        }
      };
    home.addPropertyChangeListener(Home.Property.NAME, frameTitleChangeListener);
    home.addPropertyChangeListener(Home.Property.MODIFIED, frameTitleChangeListener);
    home.addPropertyChangeListener(Home.Property.RECOVERED, frameTitleChangeListener);
    home.addPropertyChangeListener(Home.Property.REPAIRED, frameTitleChangeListener);
  }

  private static class LanguageChangeListener implements PropertyChangeListener {
    private WeakReference<JFrame>        frame;
    private WeakReference<HomeFramePane> homeFramePane;

    public LanguageChangeListener(JFrame frame, HomeFramePane homeFramePane) {
      this.frame = new WeakReference<JFrame>(frame);
      this.homeFramePane = new WeakReference<HomeFramePane>(homeFramePane);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      HomeFramePane homeFramePane = this.homeFramePane.get();
      UserPreferences preferences = (UserPreferences)ev.getSource();
      if (homeFramePane == null) {
        preferences.removePropertyChangeListener(
            UserPreferences.Property.LANGUAGE, this);
      } else {
        this.frame.get().applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        homeFramePane.updateFrameTitle(this.frame.get(), homeFramePane.home, homeFramePane.application);
      }
    }
  }

  private void addWindowStateListener(final Home home,
                                      final HomeApplication application,
                                      final HomeController controller,
                                      final JFrame frame) {
    final WindowStateListener windowStateListener = new WindowStateListener () {
        public void windowStateChanged(WindowEvent ev) {
          controller.setHomeProperty(FRAME_MAXIMIZED_VISUAL_PROPERTY, 
              String.valueOf((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH));
        }
      };
    frame.addWindowStateListener(windowStateListener);    
    application.addHomesListener(new CollectionListener<Home>() {
        public void collectionChanged(CollectionEvent<Home> ev) {
          if (ev.getItem() == home
              && ev.getType() == CollectionEvent.Type.DELETE) {
            application.removeHomesListener(this);
            frame.removeWindowStateListener(windowStateListener);
          }
        };
      });
  }

  private void computeFrameBounds(Home home, final JFrame frame) {
    Number x = home.getNumericProperty(FRAME_X_VISUAL_PROPERTY);
    Number y = home.getNumericProperty(FRAME_Y_VISUAL_PROPERTY);
    Number width = home.getNumericProperty(FRAME_WIDTH_VISUAL_PROPERTY);
    Number height = home.getNumericProperty(FRAME_HEIGHT_VISUAL_PROPERTY);
    boolean maximized = Boolean.parseBoolean(home.getProperty(FRAME_MAXIMIZED_VISUAL_PROPERTY));
    Number screenWidth = home.getNumericProperty(SCREEN_WIDTH_VISUAL_PROPERTY);
    Number screenHeight = home.getNumericProperty(SCREEN_HEIGHT_VISUAL_PROPERTY);
    
    Dimension screenSize = getUserScreenSize();
    if (x != null && y != null 
        && width != null && height != null 
        && screenWidth != null && screenHeight != null
        && screenWidth.intValue() <= screenSize.width
        && screenHeight.intValue() <= screenSize.height) {
      final Rectangle frameBounds = new Rectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue());
      if (maximized) {
        if (OperatingSystem.isMacOSX() 
            && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
          Insets insets = frame.getInsets();
          frame.setSize(screenSize.width + insets.left + insets.right, 
              screenSize.height + insets.bottom);
        } else if (OperatingSystem.isLinux()) {
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
          });
        } else {
          frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        WindowAdapter windowStateListener = new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent ev) {
              if ((ev.getOldState() == JFrame.MAXIMIZED_BOTH 
                    || (OperatingSystem.isMacOSX() 
                        && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")
                        && ev.getOldState() == JFrame.NORMAL))
                  && ev.getNewState() == JFrame.NORMAL) {
                if (OperatingSystem.isMacOSXLionOrSuperior()) {
                  new Timer(20, new ActionListener() {
                      public void actionPerformed(ActionEvent ev) {
                        if (frame.getHeight() < 40) {
                          ((Timer)ev.getSource()).stop();
                          frame.setBounds(frameBounds);
                        }
                      }
                    }).start();
                } else {
                  frame.setBounds(frameBounds);
                }
                frame.removeWindowStateListener(this);
              }
            }
            
            @Override
            public void windowClosed(WindowEvent ev) {
              frame.removeWindowListener(this);
              frame.removeWindowStateListener(this);
            }
          };
        frame.addWindowStateListener(windowStateListener);
        frame.addWindowListener(windowStateListener);
      } else {
        frame.setBounds(frameBounds);
        frame.setLocationByPlatform(!SwingTools.isRectangleVisibleAtScreen(frameBounds));
      }
    } else {      
      frame.setLocationByPlatform(true);
      frame.pack();
      frame.setSize(Math.min(screenSize.width * 4 / 5, frame.getWidth()), 
              Math.min(screenSize.height * 4 / 5, frame.getHeight()));
      if (OperatingSystem.isMacOSX() 
          && OperatingSystem.isJavaVersionBetween("1.7", "1.9")) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Frame applicationFrame : Frame.getFrames()) {
          if (applicationFrame.isShowing() 
              && applicationFrame.getBackground().getAlpha() != 0) {
            minX = Math.min(minX, applicationFrame.getX());
            minY = Math.min(minY, applicationFrame.getY());
            maxX = Math.max(maxX, applicationFrame.getX());
            maxY = Math.max(maxY, applicationFrame.getY());
          }
        }
        
        if (minX == Integer.MAX_VALUE || minX >= 23) {
          x = 0;
        } else {
          x = maxX + 23;
        }
        if (minY == Integer.MAX_VALUE || minY >= 23) {
          y = 0;
        } else {
          y = maxY + 23;
        }
        frame.setLocation(x.intValue(), y.intValue());
      }
    }
  }
  private Dimension getUserScreenSize() {
    Dimension screenSize = getToolkit().getScreenSize();
    Insets screenInsets = getToolkit().getScreenInsets(getGraphicsConfiguration());
    screenSize.width -= screenInsets.left + screenInsets.right;
    screenSize.height -= screenInsets.top + screenInsets.bottom;
    return screenSize;
  }
  private void updateFrameTitle(JFrame frame, 
                                Home home,
                                HomeApplication application) {
    String homeName = home.getName();
    String homeDisplayedName;
    if (homeName == null) {
      homeDisplayedName = application.getUserPreferences().getLocalizedString(HomeFramePane.class, "untitled"); 
      if (newHomeNumber > 1) {
        homeDisplayedName += " " + newHomeNumber;
      }
    } else {
      homeDisplayedName = this.contentManager.getPresentationName(
          homeName, ContentManager.ContentType.SWEET_HOME_3D);
    }
    
    if (home.isRecovered()) {
      homeDisplayedName += " " + application.getUserPreferences().getLocalizedString(HomeFramePane.class, "recovered");
    } 
    if (home.isRepaired()) {
      homeDisplayedName += " " + application.getUserPreferences().getLocalizedString(HomeFramePane.class, "repaired");
    }
    
    String title = homeDisplayedName;
    if (OperatingSystem.isMacOSX()) {
      Boolean homeModified = Boolean.valueOf(home.isModified() || home.isRecovered());
      putClientProperty("windowModified", homeModified);
      
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        putClientProperty("Window.documentModified", homeModified);
        
        if (homeName != null) {        
          File homeFile = new File(homeName);
          if (homeFile.exists()) {
            putClientProperty("Window.documentFile", homeFile);
          }
        }
      }

      if (!frame.isVisible() 
          && OperatingSystem.isMacOSXLionOrSuperior()) {
        try {
          Class.forName("com.apple.eawt.FullScreenUtilities").
              getMethod("setWindowCanFullScreen", new Class<?> [] {Window.class, boolean.class}).
              invoke(null, frame, true);
        } catch (Exception ex) {
        }
      }
    } else {
      title += " - " + application.getName(); 
      if (home.isModified() || home.isRecovered()) {
        title = "* " + title;
      }
    }
    frame.setTitle(title);
  }
}
