package com.eteks.homeview3d;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.media.j3d.Canvas3D;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.MouseInputAdapter;

import com.apple.eawt.AppEvent.FullScreenEvent;
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.FullScreenAdapter;
import com.apple.eawt.FullScreenListener;
import com.apple.eawt.FullScreenUtilities;
import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.swing.HomePane;
import com.eteks.homeview3d.swing.ResourceAction;
import com.eteks.homeview3d.swing.SwingTools;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.sun.j3d.exp.swing.JCanvas3D;


class MacOSXConfiguration {
  private static boolean fullScreen;

  private MacOSXConfiguration() {    
  }
 
  public static void bindToApplicationMenu(final HomeView3D homeApplication) {
    final Application macosxApplication = Application.getApplication();
    final HomeController defaultController = 
        homeApplication.createHomeFrameController(homeApplication.createHome()).getHomeController();
    final HomePane defaultHomeView = (HomePane)defaultController.getView();
    setDefaultActionsEnabled(defaultHomeView, false);
    final JMenuBar defaultMenuBar = defaultHomeView.getJMenuBar();
    
    JFrame frame = null;
    try {
      if (OperatingSystem.isJavaVersionBetween("1.7", "1.7.0_60")) {
        frame = createDummyFrameWithDefaultMenuBar(homeApplication, defaultHomeView, defaultMenuBar);
      } else if (UIManager.getLookAndFeel().getClass().getName().equals(UIManager.getSystemLookAndFeelClassName())) {
        macosxApplication.setDefaultMenuBar(defaultMenuBar);
        addWindowMenu(null, defaultMenuBar, homeApplication, defaultHomeView, true);
      }
    } catch (NoSuchMethodError ex) {
      frame = createDummyFrameWithDefaultMenuBar(homeApplication, defaultHomeView, defaultMenuBar);
    } 

    final JFrame defaultFrame = frame;
    macosxApplication.addApplicationListener(new ApplicationAdapter() {      
      @Override
      public void handleQuit(ApplicationEvent ev) { 
        Home modifiedHome = null;
        int modifiedHomesCount = 0;
        for (Home home : homeApplication.getHomes()) {
          if (home.isModified()) {
            modifiedHome = home;
            modifiedHomesCount++;
          }
        }
        
        if (modifiedHomesCount == 1) {
          homeApplication.getHomeFrame(modifiedHome).toFront();
          homeApplication.getHomeFrameController(modifiedHome).getHomeController().close(
              new Runnable() {
                public void run() {
                  for (Home home : homeApplication.getHomes()) {
                    if (home.isModified()) {
                      return;
                    }
                  }
                  System.exit(0);
                }
              });
        } else {
          handleAction(new Runnable() {
              public void run() {
                getHomeController().exit();
              }
            });
          if (homeApplication.getHomes().isEmpty()) {
            System.exit(0);
          }
        }
      }
      
      @Override
      public void handleAbout(ApplicationEvent ev) {
        handleAction(new Runnable() {
            public void run() {
              getHomeController().about();
            }
          });
        ev.setHandled(true);
      }

      @Override
      public void handlePreferences(ApplicationEvent ev) {
        handleAction(new Runnable() {
            public void run() {
              getHomeController().editPreferences();
            }
          });
      }
      
      private HomeController getHomeController() {
        if (defaultFrame != null) {
          Frame activeFrame = getActiveFrame();
          if (activeFrame != null) {
            for (Home home : homeApplication.getHomes()) {
              if (homeApplication.getHomeFrame(home) == activeFrame) {
                return homeApplication.getHomeFrameController(home).getHomeController();
              }
            }
          }
        }
        return defaultController;
      }
      
      private Frame getActiveFrame() {
        Frame activeFrame = null;
        for (Frame frame : Frame.getFrames()) {
          if (frame != defaultFrame && frame.isActive()) {
            activeFrame = frame;
            break;
          }
        }
        return activeFrame;
      }

      private void handleAction(Runnable runnable) {
        Frame activeFrame = getActiveFrame();        
        if (defaultFrame != null && activeFrame == null) {
          defaultFrame.setLocationRelativeTo(null);
          defaultFrame.toFront();
          defaultFrame.setAlwaysOnTop(true);
        }

        macosxApplication.setEnabledAboutMenu(false);
        macosxApplication.setEnabledPreferencesMenu(false);
        
        runnable.run();

        macosxApplication.setEnabledAboutMenu(true);
        macosxApplication.setEnabledPreferencesMenu(true);

        if (activeFrame != null) {
          activeFrame.toFront();
        }
        if (defaultFrame != null && activeFrame == null) {
          defaultFrame.setAlwaysOnTop(false);
          defaultFrame.toBack();
          defaultFrame.setLocation(-10, 0);
        }
      }

      @Override
      public void handleOpenFile(ApplicationEvent ev) {
        homeApplication.start(new String [] {"-open", ev.getFilename()});
      }
      
      @Override
      public void handleReOpenApplication(ApplicationEvent ev) {
        homeApplication.start(new String [0]);
      }
    });
    macosxApplication.setEnabledAboutMenu(true);
    macosxApplication.setEnabledPreferencesMenu(true);
    
    homeApplication.addHomesListener(new CollectionListener<Home>() {
      public void collectionChanged(CollectionEvent<Home> ev) {
        if (ev.getType() == CollectionEvent.Type.ADD) {
          final JFrame homeFrame = homeApplication.getHomeFrame(ev.getItem());
          if (!Boolean.getBoolean("com.eteks.homeview3d.no3D")
              && !OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
            homeFrame.setResizable(false);
            Executors.newSingleThreadExecutor().submit(new Runnable() {                
                public void run() {
                  try {
                    final AtomicBoolean canvas3D = new AtomicBoolean();
                    do {
                      Thread.sleep(50);
                      EventQueue.invokeAndWait(new Runnable() {
                          public void run() {
                            canvas3D.set(homeFrame.isShowing()
                                && isParentOfCanvas3D(homeFrame, Canvas3D.class, JCanvas3D.class));
                          }
                        });
                    } while (!canvas3D.get());                  
                  } catch (InterruptedException ex) {
                  } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                  } finally {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                          homeFrame.setResizable(true);
                        }
                      });
                  }
                }
                
                private boolean isParentOfCanvas3D(Container parent, Class<?> ... canvas3DClasses) {
                  for (int i = 0; i < parent.getComponentCount(); i++) {
                    Component child = parent.getComponent(i);
                    for (Class<?> canvas3DClass : canvas3DClasses) {
                      if (canvas3DClass.isInstance(child)
                          || child instanceof Container
                            && isParentOfCanvas3D((Container)child, canvas3DClasses)) {
                        return true;
                      }
                    }
                  }
                  if (parent instanceof Window) {
                    for (Window window : ((Window)parent).getOwnedWindows()) {
                      if (isParentOfCanvas3D(window, canvas3DClasses)) {
                        return true;
                      }
                    }
                  } 
                  return false;
                }
              });
          }
          MacOSXConfiguration.addWindowMenu(
              homeFrame, homeFrame.getJMenuBar(), homeApplication, defaultHomeView, false);
          
          if (OperatingSystem.isJavaVersionBetween("1.7", "1.7.0_60")) {
            homeFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent ev) {
                  List<Home> homes = homeApplication.getHomes();
                  defaultFrame.setVisible(false);
                  defaultFrame.setVisible(true);
                  if (homes.size() > 0) {
                    homeApplication.getHomeFrame(homes.get(0)).toFront();
                    defaultFrame.setVisible(false);
                  }
                }
              });
          }
          homeFrame.addWindowStateListener(new WindowStateListener() {
              public void windowStateChanged(WindowEvent ev) {
                enableDefaultActions(homeApplication, defaultHomeView);
              }
            });
          setDefaultActionsEnabled(defaultHomeView, false);
        } else if (ev.getType() == CollectionEvent.Type.DELETE) {
          enableDefaultActions(homeApplication, defaultHomeView);
        }
      };
    });

    if (!Boolean.getBoolean("homeview3d.bundle")) {
      try {
        String iconPath = homeApplication.getUserPreferences().getLocalizedString(HomePane.class, "about.icon");
        Image icon = ImageIO.read(HomePane.class.getResource(iconPath));
        macosxApplication.setDockIconImage(icon);
      } catch (NoSuchMethodError ex) {
      } catch (IOException ex) {
      }
    }
  }

  private static void enableDefaultActions(HomeView3D homeApplication, HomePane defaultHomeView) {
    for (Home home : homeApplication.getHomes()) {
      if ((homeApplication.getHomeFrame(home).getState() & JFrame.ICONIFIED) == 0) {
        setDefaultActionsEnabled(defaultHomeView, false);
        return;
      }
    }
    setDefaultActionsEnabled(defaultHomeView, true);
  }

  private static void setDefaultActionsEnabled(HomePane homeView, boolean enabled) {
    for (HomePane.ActionType action : HomePane.ActionType.values()) {
      switch (action) {
        case ABOUT :
        case NEW_HOME :
        case OPEN :
        case DELETE_RECENT_HOMES :
        case HELP :
          homeView.setEnabled(action, enabled);
          break;
        default :
          homeView.setEnabled(action, false);
      }
    }
  }

  private static JFrame createDummyFrameWithDefaultMenuBar(final HomeView3D homeApplication,
                                                           final HomePane defaultHomeView, 
                                                           final JMenuBar defaultMenuBar) {
    final JFrame frame = new JFrame();
    EventQueue.invokeLater(new Runnable() {
        public void run() {
          frame.setLocation(-10, 0);
          frame.setUndecorated(true);
          frame.setBackground(new Color(0, 0, 0, 0));
          frame.setVisible(true);
          frame.setJMenuBar(defaultMenuBar);
          frame.setContentPane(defaultHomeView);
          addWindowMenu(frame, defaultMenuBar, homeApplication, defaultHomeView, true);
        }
      });
    homeApplication.addHomesListener(new CollectionListener<Home>() {
        public void collectionChanged(CollectionEvent<Home> ev) {
          if (ev.getType() == CollectionEvent.Type.DELETE) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  if (frame.isActive()) {
                    List<Home> homes = homeApplication.getHomes();
                    if (homes.size() >= 1) {
                      homeApplication.getHomeFrame(homes.get(homes.size() - 1)).requestFocus();
                    }
                  }
                }
              });
          }
        }
      });
    return frame;
  }

  private static void addWindowMenu(final JFrame frame, 
                                    final JMenuBar menuBar, 
                                    final HomeView3D homeApplication,
                                    final HomePane defaultHomeView, 
                                    boolean defaultFrame) {
    UserPreferences preferences = homeApplication.getUserPreferences();
    final JMenu windowMenu = new JMenu(
        new ResourceAction(preferences, MacOSXConfiguration.class, "WINDOW_MENU", true));
    menuBar.add(windowMenu, menuBar.getComponentCount() - 1);
    windowMenu.add(new JMenuItem(
        new ResourceAction(preferences, MacOSXConfiguration.class, "MINIMIZE", !defaultFrame) {
            @Override
            public void actionPerformed(ActionEvent ev) {
              frame.setState(JFrame.ICONIFIED);
            }
          }));
    windowMenu.add(new JMenuItem(
        new ResourceAction(preferences, MacOSXConfiguration.class, "ZOOM", !defaultFrame) {
            @Override
            public void actionPerformed(ActionEvent ev) {
              if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) {
                frame.setExtendedState(frame.getExtendedState() & ~JFrame.MAXIMIZED_BOTH);
              } else {
                frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
              }
            }
          }));
    windowMenu.addSeparator();
    windowMenu.add(new JMenuItem(
        new ResourceAction(preferences, MacOSXConfiguration.class, "BRING_ALL_TO_FRONT", !defaultFrame) {
            @Override
            public void actionPerformed(ActionEvent ev) {
              frame.setAlwaysOnTop(true);
              for (Home home : homeApplication.getHomes()) {
                JFrame applicationFrame = homeApplication.getHomeFrame(home);
                if (applicationFrame != frame
                    && applicationFrame.getState() != JFrame.ICONIFIED) {
                  applicationFrame.setFocusableWindowState(false);
                  applicationFrame.toFront();
                  applicationFrame.setFocusableWindowState(true);
                }
              }
              frame.setAlwaysOnTop(false);
            }
          }));
    
    windowMenu.addMenuListener(new MenuListener() {
        public void menuSelected(MenuEvent ev) {
          boolean firstMenuItem = true;
          for (Home home : homeApplication.getHomes()) {
            final JFrame applicationFrame = homeApplication.getHomeFrame(home);
            JCheckBoxMenuItem windowMenuItem = new JCheckBoxMenuItem(
                new AbstractAction(applicationFrame.getTitle()) {
                    public void actionPerformed(ActionEvent ev) {
                      applicationFrame.toFront();
                    }
                  });
              
            if (frame == applicationFrame) {
              windowMenuItem.setSelected(true);
            }
            if (firstMenuItem) {
              windowMenu.addSeparator();
              firstMenuItem = false;
            }
            windowMenu.add(windowMenuItem);
          }
        }

        public void menuDeselected(MenuEvent ev) {
          for (int i = windowMenu.getMenuComponentCount() - 1; i >= 4; i--) {
            windowMenu.remove(i);
          }
        }

        public void menuCanceled(MenuEvent ev) {
          menuDeselected(ev);
        }
      });
  }
  public static void installToolBar(final JRootPane rootPane) {
    List<JToolBar> toolBars = SwingTools.findChildren(rootPane, JToolBar.class);
    if (OperatingSystem.isJavaVersionGreaterOrEqual("1.7.0_12")
        && toolBars.size() == 1) {
      rootPane.putClientProperty("apple.awt.brushMetalLook", true);
      final JToolBar toolBar = toolBars.get(0);
      toolBar.setFloatable(false);
      toolBar.setBorder(new AbstractBorder() {
          private final Color TOP_GRADIENT_COLOR_ACTIVATED_FRAME = OperatingSystem.isMacOSXYosemiteOrSuperior() 
              ? new Color(212, 212, 212)
              : new Color(222, 222, 222);
          private final Color BOTTOM_GRADIENT_COLOR_ACTIVATED_FRAME = OperatingSystem.isMacOSXYosemiteOrSuperior() 
              ? new Color(209, 209, 209)
              : new Color(178, 178, 178);
          private final Color TOP_GRADIENT_COLOR_DEACTIVATED_FRAME  = new Color(244, 244, 244);
          private final Color BOTTOM_GRADIENT_COLOR_DEACTIVATED_FRAME = TOP_GRADIENT_COLOR_ACTIVATED_FRAME;

          @Override
          public boolean isBorderOpaque() {
            return true;
          }
          
          @Override
          public Insets getBorderInsets(Component c) {
            return new Insets(-4, 4, 0, 4);
          }
          
          @Override
          public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Component root = SwingUtilities.getRoot(rootPane);
            boolean active = ((JFrame)root).isActive();
            ((Graphics2D)g).setPaint(new GradientPaint(0, y, 
                active ? TOP_GRADIENT_COLOR_ACTIVATED_FRAME : TOP_GRADIENT_COLOR_DEACTIVATED_FRAME, 
                0, y + height - 1, 
                active ? BOTTOM_GRADIENT_COLOR_ACTIVATED_FRAME : BOTTOM_GRADIENT_COLOR_DEACTIVATED_FRAME));
            g.fillRect(x, y, x + width, y + height);
          }
        });

      final MouseInputAdapter mouseListener = new MouseInputAdapter() {
          private Point lastLocation;
          
          @Override
          public void mousePressed(MouseEvent ev) {
            this.lastLocation = ev.getPoint();
            SwingUtilities.convertPointToScreen(this.lastLocation, ev.getComponent());
          }
    
          @Override
          public void mouseDragged(MouseEvent ev) {
            Point newLocation = ev.getPoint();
            SwingUtilities.convertPointToScreen(newLocation, ev.getComponent());
            Component root = SwingUtilities.getRoot(rootPane);
            root.setLocation(root.getX() + newLocation.x - this.lastLocation.x, 
                root.getY() + newLocation.y - this.lastLocation.y);
            this.lastLocation = newLocation;
          }
        };
      toolBar.addMouseListener(mouseListener);
      toolBar.addMouseMotionListener(mouseListener);
      
      toolBar.addAncestorListener(new AncestorListener() {
          private Object fullScreenListener;

          public void ancestorAdded(AncestorEvent ev) {
            ((Window)SwingUtilities.getRoot(toolBar)).addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent ev) {
                  toolBar.repaint();
                }
                
                @Override
                public void windowDeactivated(WindowEvent ev) {
                  toolBar.repaint();
                }
              });
            toolBar.repaint();

            try {
              Class fullScreenUtilitiesClass = Class.forName("com.apple.eawt.FullScreenUtilities");
              this.fullScreenListener = new FullScreenAdapter() {
                  public void windowEnteredFullScreen(FullScreenEvent ev) {
                    fullScreen = true;
                    toolBar.removeMouseListener(mouseListener);
                    toolBar.removeMouseMotionListener(mouseListener);
                  }
                  
                  public void windowExitedFullScreen(FullScreenEvent ev) {
                    fullScreen = false;
                    toolBar.addMouseListener(mouseListener);
                    toolBar.addMouseMotionListener(mouseListener);
                  }
                };
              FullScreenUtilities.addFullScreenListenerTo((Window)SwingUtilities.getRoot(rootPane), 
                  (FullScreenListener)this.fullScreenListener);
            } catch (ClassNotFoundException ex) {
            }
          }
  
          public void ancestorMoved(AncestorEvent ev) {
          }
  
          public void ancestorRemoved(AncestorEvent ev) {
            toolBar.removeAncestorListener(this);
            try {
              Class fullScreenUtilitiesClass = Class.forName("com.apple.eawt.FullScreenUtilities");
              FullScreenUtilities.removeFullScreenListenerFrom((Window)SwingUtilities.getRoot(rootPane), 
                  (FullScreenListener)this.fullScreenListener);
            } catch (ClassNotFoundException ex) {
            }
          }
        });

      List<JSplitPane> siblings = SwingTools.findChildren((JComponent)toolBar.getParent(), JSplitPane.class);
      if (siblings.size() >= 1) {
        JComponent siblingComponent = siblings.get(0);
        if (siblingComponent.getParent() == toolBar.getParent()) {
          Border border = siblingComponent.getBorder();
          final Insets borderInsets = border.getBorderInsets(siblingComponent);
          final Insets filledBorderInsets = new Insets(1, 0, 0, 0);
          siblingComponent.setBorder(new CompoundBorder(border, 
              new AbstractBorder() {
                @Override
                public Insets getBorderInsets(Component c) {
                  return filledBorderInsets;
                }
                
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                  Color background = c.getBackground();
                  g.setColor(background);
                  g.fillRect(x, y, width, 1);
                  g.fillRect(x - borderInsets.left, y, borderInsets.left, height + borderInsets.bottom);
                  g.fillRect(x + width, y, borderInsets.right, height + borderInsets.bottom);
                  g.fillRect(x, y + height, width, borderInsets.bottom);
                }              
              }));
        }
      }
    }
  }

  public static boolean isWindowFullScreen(final JFrame frame) {
    return fullScreen;
  }
}
