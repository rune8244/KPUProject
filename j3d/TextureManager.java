package com.eteks.homeview3d.j3d;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Texture;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.tools.URLContent;
import com.sun.j3d.utils.image.TextureLoader;

public class TextureManager {
  private static TextureManager instance;
  private final Texture         errorTexture;
  private final Texture         waitTexture;
  private final Map<Content, List<ComparableTextureAngleTuple>> contentTextures;
  private final Map<Texture, ComparableTexture>                 textures;
  private Map<RotatedContentKey, List<TextureObserver>>         loadingTextureObservers;
  private ExecutorService       texturesLoader;

  private TextureManager() {
    this.errorTexture = getColoredImageTexture(Color.RED);
    this.waitTexture = getColoredImageTexture(Color.WHITE);
    this.contentTextures = new WeakHashMap<Content, List<ComparableTextureAngleTuple>>();
    this.textures = new WeakHashMap<Texture, ComparableTexture>();
    this.loadingTextureObservers = new HashMap<RotatedContentKey, List<TextureObserver>>();
  }

  public static TextureManager getInstance() {
    if (instance == null) {
      instance = new TextureManager();
    }
    return instance;
  }

  public void clear() {
    if (this.texturesLoader != null) {
      this.texturesLoader.shutdownNow();
      this.texturesLoader = null;
    }
    synchronized (this.textures) {
      this.contentTextures.clear();
      this.textures.clear();
    }
    this.loadingTextureObservers.clear();
  }

  private Texture getColoredImageTexture(Color color) {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    Graphics g = image.getGraphics();
    g.setColor(color);
    g.drawLine(0, 0, 0, 0);
    g.dispose();
    Texture texture = new TextureLoader(image).getTexture();
    texture.setCapability(Texture.ALLOW_IMAGE_READ);
    texture.setCapability(Texture.ALLOW_FORMAT_READ);
    texture.getImage(0).setCapability(ImageComponent2D.ALLOW_IMAGE_READ);
    texture.getImage(0).setCapability(ImageComponent2D.ALLOW_FORMAT_READ);
    return texture;
  }
  public void loadTexture(final Content content, final TextureObserver textureObserver) {
    loadTexture(content, false, textureObserver);
  }
  
  public void loadTexture(final Content content,
                          boolean synchronous,
                          final TextureObserver textureObserver) {
    loadTexture(content, 0, synchronous, textureObserver);
  }

  public void loadTexture(final Content content,
                          final float   angle,
                          boolean synchronous,
                          final TextureObserver textureObserver) {
    Texture texture = null;
    synchronized (this.textures) { // Use one mutex for both maps
      List<ComparableTextureAngleTuple> contentTexturesList = this.contentTextures.get(content);
      if (contentTexturesList != null) {
        for (ComparableTextureAngleTuple textureAngleTuple : contentTexturesList) {
          if (textureAngleTuple.getAngle() == angle) {
            texture = textureAngleTuple.getTexture(); 
          }
        }
      }
    }
    if (texture == null) {
      if (synchronous) {
        texture = shareTexture(loadTexture(content, angle), angle, content);
        textureObserver.textureUpdated(texture);
      } else if (!EventQueue.isDispatchThread()) {
        throw new IllegalStateException("Asynchronous call out of Event Dispatch Thread");
      } else {
        textureObserver.textureUpdated(this.waitTexture);
        if (this.texturesLoader == null) {
          this.texturesLoader = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        
        final RotatedContentKey contentKey = new RotatedContentKey(content, angle);
        List<TextureObserver> observers = this.loadingTextureObservers.get(contentKey);
        if (observers != null) {
          observers.add(textureObserver);
        } else {
          observers = new ArrayList<TextureObserver>();
          observers.add(textureObserver);
          this.loadingTextureObservers.put(contentKey, observers);

          this.texturesLoader.execute(new Runnable () {
              public void run() {
                final Texture texture = shareTexture(loadTexture(content, angle), angle, content);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      List<TextureObserver> observers = loadingTextureObservers.remove(contentKey);
                      if (observers != null) {
                        for (TextureObserver observer : observers) {
                          observer.textureUpdated(texture);
                        }
                      }
                    }
                  });
              }
            });
        }
      }
    } else {
      textureObserver.textureUpdated(texture);
    }
  }

  public Texture loadTexture(final Content content) {
    return loadTexture(content, 0);
  }

  private Texture loadTexture(final Content content, float angle) {
    try {
      InputStream contentStream = content.openStream();
      BufferedImage image;          
      try {
        image = ImageIO.read(contentStream);
      } catch (ConcurrentModificationException ex) {
        contentStream.close();
        contentStream = content.openStream();
       image = ImageIO.read(contentStream);
      }
      if (angle != 0) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        BufferedImage rotatedImage = new BufferedImage((int)Math.round(Math.abs(image.getWidth() * cos) + Math.abs(image.getHeight() * sin)), 
            (int)Math.round(Math.abs(image.getWidth() * sin) + Math.abs(image.getHeight() * cos)), 
            image.getTransparency() == BufferedImage.TRANSLUCENT ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g2D = (Graphics2D)rotatedImage.getGraphics();
        g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2D.setPaint(new TexturePaint(image, 
                        new Rectangle2D.Float(0, 0, image.getWidth(), image.getHeight())));
        g2D.rotate(angle);
        float maxDimension = Math.max(rotatedImage.getWidth(), rotatedImage.getHeight());
        g2D.fill(new Rectangle2D.Float(-maxDimension, -maxDimension, 3 * maxDimension, 3 * maxDimension));
        g2D.dispose();
        image = rotatedImage;
      }
      contentStream.close();
      if (image != null) {
        Texture texture = new TextureLoader(image).getTexture();
        if (content instanceof URLContent && angle == 0) {
          texture.setUserData(((URLContent)content).getURL());
        }
        return texture;
      } else {
        return this.errorTexture;
      }
    } catch (IOException ex) {
      return this.errorTexture;
    } catch (RuntimeException ex) {
      if (ex.getClass().getName().equals("com.sun.j3d.utils.image.ImageException")) {
        return this.errorTexture;
      } else {
        throw ex;
      }
    }            
  }

  public Texture shareTexture(Texture texture) {
    return shareTexture(texture, 0, null);
  }

  private Texture shareTexture(final Texture texture,
                               final float   angle,
                               final Content content) {
    ComparableTexture textureData = new ComparableTexture(texture);
    Texture sharedTexture = null;
    synchronized (this.textures) {
      for (Map.Entry<Texture, ComparableTexture> entry : this.textures.entrySet()) {
        if (textureData.equalsImage(entry.getValue())) {
          sharedTexture = entry.getKey();
          textureData = entry.getValue(); 
          break;
        }
      }
      if (sharedTexture == null) {
        sharedTexture = texture;
        setSharedTextureAttributesAndCapabilities(sharedTexture);
        this.textures.put(sharedTexture, textureData);
      }
      if (content != null) {
        List<ComparableTextureAngleTuple> contentTexturesList = this.contentTextures.get(content);
        if (contentTexturesList == null) {
          contentTexturesList = new ArrayList<ComparableTextureAngleTuple>(1);
          this.contentTextures.put(content, contentTexturesList);
        }
        contentTexturesList.add(new ComparableTextureAngleTuple(textureData, angle));
      }
    }
    return sharedTexture;
  }

  private void setSharedTextureAttributesAndCapabilities(Texture texture) {
    if (!texture.isLive()) {
      texture.setMinFilter(Texture.NICEST);
      texture.setMagFilter(Texture.NICEST);
      texture.setCapability(Texture.ALLOW_FORMAT_READ);
      texture.setCapability(Texture.ALLOW_IMAGE_READ);
      for (ImageComponent image : texture.getImages()) {
        if (!image.isLive()) {
          image.setCapability(ImageComponent.ALLOW_FORMAT_READ);
          image.setCapability(ImageComponent.ALLOW_IMAGE_READ);
        }
      }
    }
  }

  public boolean isTextureTransparent(Texture texture) {
    synchronized (this.textures) { // Use one mutex for both maps
      ComparableTexture textureData = this.textures.get(texture);
      if (textureData != null) {
        return textureData.isTransparent();
      }
      return texture.getFormat() == Texture.RGBA;
    }
  }

  public float getRotatedTextureWidth(HomeTexture texture) {
    float angle = texture.getAngle();
    if (angle != 0) {
      return (float)(texture.getWidth() * Math.cos(angle) 
          + texture.getHeight() * Math.sin(angle));
    } else {
      return texture.getWidth();
    }
  }

  public float getRotatedTextureHeight(HomeTexture texture) {
    float angle = texture.getAngle();
    if (angle != 0) {
      return (float)(texture.getWidth() * Math.sin(angle) 
          + texture.getHeight() * Math.cos(angle));
    } else {
      return texture.getHeight();
    }
  }

  public static interface TextureObserver {
    public void textureUpdated(Texture texture); 
  }

  private static class RotatedContentKey {
    private Content content;
    private float   angle;
    
    public RotatedContentKey(Content content, float angle) {
      this.content = content;
      this.angle = angle;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj instanceof RotatedContentKey) {
        RotatedContentKey rotatedContentKey = (RotatedContentKey)obj;
        return this.content.equals(rotatedContentKey.content)
            && this.angle == rotatedContentKey.angle;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.content.hashCode() 
          + Float.floatToIntBits(this.angle);
    }    
  }

  private static class ComparableTexture {
    private Texture               texture;
    private WeakReference<int []> imageBits;
    private Integer               imageBitsHashCode;
    private Boolean               transparent;

    public ComparableTexture(Texture texture) {
      this.texture = texture;      
    }
    
    public Texture getTexture() {
      return this.texture;
    }
 
    private int [] getImageBits() {
      int [] imageBits = null;
      if (this.imageBits != null) {
        imageBits = this.imageBits.get();
      }
      if (imageBits == null) {
        BufferedImage image = ((ImageComponent2D)this.texture.getImage(0)).getImage();
        if (image.getType() != BufferedImage.TYPE_INT_RGB
            && image.getType() != BufferedImage.TYPE_INT_ARGB) {
          BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), 
              this.texture.getFormat() == Texture.RGBA ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
          Graphics2D g = (Graphics2D)tmp.getGraphics();
          g.drawImage(image, null, 0, 0);
          g.dispose();
          image = tmp;
        }
        imageBits = (int [])image.getRaster().getDataElements(0, 0, image.getWidth(), image.getHeight(), null);
        this.transparent = image.getTransparency() != BufferedImage.OPAQUE;
        if (this.transparent) {
          this.transparent = containsTransparentPixels(imageBits);
        }
        this.imageBits = new WeakReference<int[]>(imageBits);
      }
      return imageBits;
    }

    private int getImageBitsHashCode() {
      if (this.imageBitsHashCode == null) {
        this.imageBitsHashCode = Arrays.hashCode(getImageBits());
      }
      return this.imageBitsHashCode;
    }

    private boolean containsTransparentPixels(int [] imageBits) {
      boolean transparentPixel = false;
      for (int argb : imageBits) {
        if ((argb & 0xFF000000) != 0xFF000000) {
          transparentPixel = true;
          break;
        }
      }
      return transparentPixel;
    }

    public boolean isTransparent() {
      if (this.transparent == null) {
        getImageBits();
      }
      return this.transparent;
    }

    public boolean equalsImage(ComparableTexture comparableTexture) {
      if (this == comparableTexture) {
        return true;
      } else if (this.texture == comparableTexture.texture) {
        return true;
      } else if (getImageBitsHashCode() == comparableTexture.getImageBitsHashCode()){
        return Arrays.equals(getImageBits(), comparableTexture.getImageBits());
      }
      return false;
    }
  }

  private static class ComparableTextureAngleTuple {
    private ComparableTexture texture;
    private float             angle;

    public ComparableTextureAngleTuple(ComparableTexture texture, float angle) {
      this.texture = texture;
      this.angle = angle;
    }

    public Texture getTexture() {
      return this.texture.getTexture();
    }
    
    public float getAngle() {
      return this.angle;
    }
  }
}
