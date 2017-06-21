package com.eteks.homeview3d.swing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.tools.ResourceURLContent;

public class IconManager {
  private static IconManager                     instance;
  private final Content                          errorIconContent;
  private final Content                          waitIconContent;
  private final Map<Content, Map<Integer, Icon>> icons;
  private ExecutorService                        iconsLoader;

  private IconManager() {
    this.errorIconContent = new ResourceURLContent(IconManager.class, "resources/icons/tango/image-missing.png");
    this.waitIconContent = new ResourceURLContent(IconManager.class, "resources/icons/tango/image-loading.png");
    this.icons = Collections.synchronizedMap(new WeakHashMap<Content, Map<Integer, Icon>>());
  }
  
  public static IconManager getInstance() {
    if (instance == null) {
      instance = new IconManager();
    }
    return instance;
  }

  // 로드 된 자원들 삭제
  public void clear() {
    if (this.iconsLoader != null) {
      this.iconsLoader.shutdownNow();
      this.iconsLoader = null;
    }
    this.icons.clear();
  }
  
  
  public Icon getErrorIcon(int height) {
    return getIcon(this.errorIconContent, height, null);
  }
 
  public Icon getErrorIcon() {
    return getIcon(this.errorIconContent, -1, null);
  }
  
  public boolean isErrorIcon(Icon icon) {
    Map<Integer, Icon> errorIcons = this.icons.get(this.errorIconContent);
    return errorIcons != null
        && (errorIcons.containsValue(icon)
            || icon instanceof IconProxy
                && errorIcons.containsValue(((IconProxy)icon).getIcon()));
  }

 
  public Icon getWaitIcon(int height) {
    return getIcon(this.waitIconContent, height, null);
  }
  

  public Icon getWaitIcon() {
    return getIcon(this.waitIconContent, -1, null);
  }
  
  
  public boolean isWaitIcon(Icon icon) {
    Map<Integer, Icon> waitIcons = this.icons.get(this.waitIconContent);
    return waitIcons != null
        && (waitIcons.containsValue(icon)
            || icon instanceof IconProxy
                && waitIcons.containsValue(((IconProxy)icon).getIcon()));
  }


  public Icon getIcon(Content content, Component waitingComponent) {
    return getIcon(content, -1, waitingComponent);
  }
  

  public Icon getIcon(Content content, final int height, Component waitingComponent) {
    Map<Integer, Icon> contentIcons = this.icons.get(content);
    if (contentIcons == null) {
      contentIcons = Collections.synchronizedMap(new HashMap<Integer, Icon>());
      this.icons.put(content, contentIcons);
    }
    Icon icon = contentIcons.get(height);
    if (icon == null) {
      if (content == null) {
        icon = new Icon() {
          public void paintIcon(Component c, Graphics g, int x, int y) {
          }
          
          public int getIconWidth() {
            return Math.max(0, height);
          }
          
          public int getIconHeight() {
            return Math.max(0, height);
          }
        };
      } else if (content == this.errorIconContent ||
                 content == this.waitIconContent) {
        icon = createIcon(content, height, null); 
      } else if (waitingComponent == null) {
        icon = createIcon(content, height, 
            getIcon(this.errorIconContent, height, null)); 
      } else {
        icon = new IconProxy(content, height, waitingComponent,
                 getIcon(this.errorIconContent, height, null),
                 getIcon(this.waitIconContent, height, null));
      }
      contentIcons.put(height, icon);
    }
    return icon;    
  }
  
  private Icon createIcon(Content content, int height, Icon errorIcon) {
    try {
      InputStream contentStream = content.openStream();
      BufferedImage image = ImageIO.read(contentStream);
      contentStream.close();
      if (image != null) {
        if (height != -1 && height != image.getHeight()) {
          int width = image.getWidth() * height / image.getHeight();
          BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
          Graphics g = scaledImage.getGraphics();
          g.drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
          g.dispose();
          return new ImageIcon(scaledImage);
        } else {
          return new ImageIcon(image);
        }
      }
    } catch (IOException ex) {
    }
    return errorIcon;
  }
 
  private class IconProxy implements Icon {
    private Icon icon;
    
    public IconProxy(final Content content, final int height,
                     final Component waitingComponent,
                     final Icon errorIcon, Icon waitIcon) {
      this.icon = waitIcon;
      if (iconsLoader == null) {
        iconsLoader = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
      }
      iconsLoader.execute(new Runnable () {
          public void run() {
            icon = createIcon(content, height, errorIcon);
            waitingComponent.repaint();
          }
        });
    }

    public int getIconWidth() {
      return this.icon.getIconWidth();
    }

    public int getIconHeight() {
      return this.icon.getIconHeight();
    }
    
    public void paintIcon(Component c, Graphics g, int x, int y) {
      this.icon.paintIcon(c, g, x, y);
    }
    
    public Icon getIcon() {
      return this.icon;
    }
  }
}
