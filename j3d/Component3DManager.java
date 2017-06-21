package com.eteks.homeview3d.j3d;

import java.awt.Graphics;
import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.IllegalRenderingStateException;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.RenderingError;
import javax.media.j3d.RenderingErrorListener;
import javax.media.j3d.Screen3D;
import javax.media.j3d.View;
import javax.media.j3d.VirtualUniverse;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.eteks.homeview3d.tools.OperatingSystem;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;

public class Component3DManager {
  private static final String CHECK_OFF_SCREEN_IMAGE_SUPPORT = "com.eteks.homeview3d.j3d.checkOffScreenSupport";
  
  private static Component3DManager instance;
  
  private RenderingErrorObserver renderingErrorObserver;
  private Object                 renderingErrorListener; 
  private Boolean                offScreenImageSupported;
  private GraphicsConfiguration  defaultScreenConfiguration;

  private Component3DManager() {
    if (!GraphicsEnvironment.isHeadless()) {
      GraphicsConfigTemplate3D template = createGraphicsConfigurationTemplate3D();
      GraphicsDevice defaultScreenDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      this.defaultScreenConfiguration = defaultScreenDevice.getBestConfiguration(template);
      if (this.defaultScreenConfiguration == null) {
        this.defaultScreenConfiguration = defaultScreenDevice.getBestConfiguration(new GraphicsConfigTemplate3D());
      }
    } else {
      this.offScreenImageSupported = Boolean.FALSE;
    }
  }

  /**
   * 템플릿으로 돌아가서 3d 캔버스 조정
   */
  private GraphicsConfigTemplate3D createGraphicsConfigurationTemplate3D() {
    if (System.getProperty("j3d.implicitAntialiasing") == null) {
      System.setProperty("j3d.implicitAntialiasing", "true");
    }
    GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
    template.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);
    if (OperatingSystem.isMacOSX() && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
      // 깊이 사이즈 요청
      template.setDepthSize(24);
    }

    String stereo = System.getProperty("j3d.stereo");
    if (stereo != null) {
      if ("REQUIRED".equals(stereo))
        template.setStereo(GraphicsConfigTemplate.REQUIRED);
      else if ("PREFERRED".equals(stereo))
        template.setStereo(GraphicsConfigTemplate.PREFERRED);
    }
    return template;
  }
  
  /**
   * 이 싱글턴 인스턴스로 반환. 
   */
  public static Component3DManager getInstance() {
    if (instance == null) {
      instance = new Component3DManager();
    }
    return instance;
  }
  
  /**
   * 현재 렌더링 에러 세팅.
   */
  public void setRenderingErrorObserver(RenderingErrorObserver observer) {
    try {
      Class.forName("javax.media.j3d.RenderingErrorListener");
      this.renderingErrorListener = RenderingErrorListenerManager.setRenderingErrorObserver(
          observer, this.renderingErrorListener);
      this.renderingErrorObserver = observer;
    } catch (ClassNotFoundException ex) {
    }
  }
  
  public RenderingErrorObserver getRenderingErrorObserver() {
    return this.renderingErrorObserver;
  }

  public boolean isOffScreenImageSupported() {
    if (this.offScreenImageSupported == null) {
      if ("false".equalsIgnoreCase(System.getProperty(CHECK_OFF_SCREEN_IMAGE_SUPPORT, "true"))) {
        this.offScreenImageSupported = Boolean.FALSE;
      } else {
        SimpleUniverse universe = null;
        try {
          // 유니버스 바운드 생성 3d아님
          ViewingPlatform viewingPlatform = new ViewingPlatform();
          Viewer viewer = new Viewer(new Canvas3D [0]);
          universe = new SimpleUniverse(viewingPlatform, viewer);     
          getOffScreenImage(viewer.getView(), 1, 1);
          this.offScreenImageSupported = Boolean.TRUE;
        } catch (IllegalRenderingStateException ex) {
          this.offScreenImageSupported = Boolean.FALSE;
        } catch (NullPointerException ex) {
          this.offScreenImageSupported = Boolean.FALSE;
        } catch (IllegalArgumentException ex) {
          this.offScreenImageSupported = Boolean.FALSE;
        } finally {
          if (universe != null) {
            universe.cleanup();
          }
        }
      }
    }
    return this.offScreenImageSupported;
  }

  private Canvas3D getCanvas3D(GraphicsConfiguration deviceConfiguration,
                               boolean offscreen,
                               final RenderingObserver renderingObserver) {
    GraphicsConfiguration configuration;
    if (GraphicsEnvironment.isHeadless()) {
      configuration = null;
    } else if (deviceConfiguration == null
               || deviceConfiguration.getDevice() == this.defaultScreenConfiguration.getDevice()) {
      configuration = this.defaultScreenConfiguration;
    } else {
      GraphicsConfigTemplate3D template = createGraphicsConfigurationTemplate3D();      
      configuration = deviceConfiguration.getDevice().getBestConfiguration(template);
      if (configuration == null) {
        configuration = deviceConfiguration.getDevice().getBestConfiguration(new GraphicsConfigTemplate3D());
      }
    }
    if (configuration == null) {
      throw new IllegalRenderingStateException("Can't create graphics environment for Canvas 3D");
    }
    try {
      System.gc();
      
      // 자바 3d 캔버스 생성
      final Canvas3D canvas3D;
      if (renderingObserver != null) {
        canvas3D = new ObservedCanvas3D(configuration, offscreen, renderingObserver);
      } else {
        canvas3D = new Canvas3D(configuration, offscreen);
      }
      
      if (!offscreen
          && OperatingSystem.isLinux() 
          && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
        final WindowListener parentActivationListener = new WindowAdapter() {
            private Timer timer;

            @Override
            public void windowActivated(WindowEvent ev) {
              if (this.timer == null) {
                this.timer = new Timer(100, new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                      canvas3D.repaint();
                    }
                  });
                this.timer.setRepeats(false);
              }
              this.timer.restart();
            }
            
            @Override
            public void windowDeactivated(WindowEvent ev) {
              windowActivated(null);
            }
          };
        canvas3D.addHierarchyListener(new HierarchyListener() {
            private Window parentWindow;
            
            public void hierarchyChanged(HierarchyEvent ev) {
              Window window = SwingUtilities.windowForComponent(canvas3D);
              if (window != null) {
                if (this.parentWindow != window) {
                  window.addWindowListener(parentActivationListener);
                }
              } else if (this.parentWindow != null) {
                this.parentWindow.removeWindowListener(parentActivationListener);
              }
              this.parentWindow = window;
            }
          });
      }
      
      return canvas3D;
    } catch (IllegalArgumentException ex) {
      IllegalRenderingStateException ex2 = new IllegalRenderingStateException("Can't create Canvas 3D");
      ex2.initCause(ex);
      throw ex2;
    }
  }


  public Canvas3D getOnscreenCanvas3D() {
    return getOnscreenCanvas3D(null);
  }
  

  public Canvas3D getOnscreenCanvas3D(RenderingObserver renderingObserver) {
    return getCanvas3D(null, false, renderingObserver);
  }
  

  public Canvas3D getOnscreenCanvas3D(GraphicsConfiguration deviceConfiguration, 
                                      RenderingObserver renderingObserver) {
    return getCanvas3D(deviceConfiguration, false, renderingObserver);
  }


  public Canvas3D getOffScreenCanvas3D(int width, int height) {
    Canvas3D offScreenCanvas = getCanvas3D(null, true, null);
    // Configure canvas 3D for offscreen
    Screen3D screen3D = offScreenCanvas.getScreen3D();
    screen3D.setSize(width, height);
    screen3D.setPhysicalScreenWidth(2f);
    screen3D.setPhysicalScreenHeight(2f / width * height);
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    ImageComponent2D imageComponent2D = new ImageComponent2D(ImageComponent2D.FORMAT_RGB, image);
    imageComponent2D.setCapability(ImageComponent2D.ALLOW_IMAGE_READ);
    offScreenCanvas.setOffScreenBuffer(imageComponent2D);
    return offScreenCanvas;
  }
  

  public BufferedImage getOffScreenImage(View view, int width, int height)  {
    Canvas3D offScreenCanvas = null;
    RenderingErrorObserver previousRenderingErrorObserver = getRenderingErrorObserver();
    try {

      final CountDownLatch latch = new CountDownLatch(1); 
      setRenderingErrorObserver(new RenderingErrorObserver() {
          public void errorOccured(int errorCode, String errorMessage) {
            latch.countDown();
          }
        });
      
      // 오프스크린 생성 및 바인드
      offScreenCanvas = getOffScreenCanvas3D(width, height);
      view.addCanvas3D(offScreenCanvas);
      
      // 오프 스크린 렌더링
      offScreenCanvas.renderOffScreenBuffer();
      offScreenCanvas.waitForOffScreenRendering();

      if (latch.await(10, TimeUnit.MILLISECONDS)) {
        throw new IllegalRenderingStateException("Off screen rendering unavailable");
      }
      
      return offScreenCanvas.getOffScreenBuffer().getImage();
    } catch (InterruptedException ex) {
      IllegalRenderingStateException ex2 = 
          new IllegalRenderingStateException("Off screen rendering interrupted");
      ex2.initCause(ex);
      throw ex2;
    } finally {
      if (offScreenCanvas != null) {
        view.removeCanvas3D(offScreenCanvas);
        try {
          offScreenCanvas.setOffScreenBuffer(null);
        } catch (NullPointerException ex) {
        }
      }
      // 이전의 렌더링 에러 리셋
      setRenderingErrorObserver(previousRenderingErrorObserver);
    }
  }

  public static interface RenderingErrorObserver {
    void errorOccured(int errorCode, String errorMessage);
  }
  

  private static class RenderingErrorListenerManager {
    public static Object setRenderingErrorObserver(final RenderingErrorObserver observer,
                                                   Object previousRenderingErrorListener) {
      if (previousRenderingErrorListener != null) {
        VirtualUniverse.removeRenderingErrorListener(
            (RenderingErrorListener)previousRenderingErrorListener);
      }
      RenderingErrorListener renderingErrorListener = new RenderingErrorListener() {
        public void errorOccurred(RenderingError error) {
          observer.errorOccured(error.getErrorCode(), error.getErrorMessage());
        }
      }; 
      VirtualUniverse.addRenderingErrorListener(renderingErrorListener);
      return renderingErrorListener;
    }
  }


  public static interface RenderingObserver {
 
    public void canvas3DPreRendered(Canvas3D canvas3D); 


    public void canvas3DPostRendered(Canvas3D canvas3D); 

 
    public void canvas3DSwapped(Canvas3D canvas3D); 
  }


  private static class ObservedCanvas3D extends Canvas3D {
    private final RenderingObserver renderingObserver;
    private final boolean           paintDelayed;
    private Timer timer;

    private ObservedCanvas3D(GraphicsConfiguration graphicsConfiguration, 
                             boolean offScreen,
                             RenderingObserver renderingObserver) {
      super(graphicsConfiguration, offScreen);
      this.renderingObserver = renderingObserver;

      this.paintDelayed = OperatingSystem.isWindows()  
          && OperatingSystem.isJavaVersionGreaterOrEqual("1.7");
    }

    @Override
    public void preRender() {
      this.renderingObserver.canvas3DPreRendered(this);
    }

    @Override
    public void postRender() {
      this.renderingObserver.canvas3DPostRendered(this);
    }

    @Override
    public void postSwap() {
      this.renderingObserver.canvas3DSwapped(this);
    }
    
    @Override
    public void paint(Graphics g) {
      if (this.paintDelayed) {
        if (this.timer == null) {
          this.timer = new Timer(100, new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                ObservedCanvas3D.super.paint(null);
              }
            });
          this.timer.setRepeats(false);            
        }
        this.timer.restart();
      } else {
        super.paint(g);
      }
    }
  }
}
