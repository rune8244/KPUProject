package com.eteks.homeview3d.j3d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.UIManager;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4f;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.Label;
import com.eteks.homeview3d.model.TextStyle;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.image.TextureLoader;

public class Label3D extends Object3DBranch {
  private static final TransparencyAttributes DEFAULT_TRANSPARENCY_ATTRIBUTES = 
      new TransparencyAttributes(TransparencyAttributes.NICEST, 0);
  private static final PolygonAttributes      DEFAULT_POLYGON_ATTRIBUTES = 
      new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0, false);
  private static final TextureAttributes MODULATE_TEXTURE_ATTRIBUTES = new TextureAttributes();
  
  static {
    MODULATE_TEXTURE_ATTRIBUTES.setTextureMode(TextureAttributes.MODULATE);
  }

  private String      text;
  private TextStyle   style;
  private Integer     color;
  private Transform3D baseLineTransform;
  private Texture     texture;
  
  public Label3D(Label label, Home home, boolean waitForLoading) {
    setUserData(label);

    setCapability(BranchGroup.ALLOW_DETACH);
    setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
    setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
    
    update();
  }

  @Override
  public void update() {
    Label label = (Label)getUserData();
    Float pitch = label.getPitch();
    TextStyle style = label.getStyle();
    if (pitch != null
        && style != null
        && (label.getLevel() == null
            || label.getLevel().isViewableAndVisible())) {
      String text = label.getText();
      Integer color = label.getColor();
      Integer outlineColor = label.getOutlineColor();
      if (!text.equals(this.text)
          || (style == null && this.style != null)
          || (style != null && !style.equals(this.style))
          || (color == null && this.color != null)
          || (color != null && !color.equals(this.color))) {
        int fontStyle = Font.PLAIN;
        if (style.isBold()) {
          fontStyle = Font.BOLD;
        }
        if (style.isItalic()) {
          fontStyle |= Font.ITALIC;
        }
        Font defaultFont; 
        if (style.getFontName() != null) {
          defaultFont = new Font(style.getFontName(), fontStyle, 1);
        } else {
          defaultFont = UIManager.getFont("TextField.font");
        }
        BasicStroke stroke = new BasicStroke(outlineColor != null ? style.getFontSize() * 0.05f : 0f); 
        Font font = defaultFont.deriveFont(fontStyle, style.getFontSize() - stroke.getLineWidth());
  
        BufferedImage dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = (Graphics2D)dummyImage.getGraphics();
        FontMetrics fontMetrics = g2D.getFontMetrics(font);
        g2D.dispose();
        
        Rectangle2D textBounds = fontMetrics.getStringBounds(text, g2D);
        float textWidth = (float)textBounds.getWidth() + 2 * stroke.getLineWidth();
        if (style.isItalic()) {
          textWidth += fontMetrics.getAscent() * 0.2;
        }
        float textHeight = (float)textBounds.getHeight() + 2 * stroke.getLineWidth();
        float textRatio = (float)Math.sqrt((float)textWidth / textHeight);
        int width;
        int height;
        float scale;
        if (textRatio > 1) {
          width = (int)Math.ceil(Math.max(255 * textRatio, Math.min(textWidth, 511 * textRatio)));
          scale = (float)(width / textWidth);
          height = (int)Math.ceil(scale * textHeight);
        } else {
          height = (int)Math.ceil(Math.max(255 * textRatio, Math.min(textHeight, 511 / textRatio)));
          scale = (float)(height / textHeight);
          width = (int)Math.ceil(scale * textWidth);
        }
  
        if (width > 0 && height > 0) {
          // 이미지에 텍스트 그려넣기
          BufferedImage textureImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);        
          g2D = (Graphics2D)textureImage.getGraphics();
          g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
          g2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
          g2D.setTransform(AffineTransform.getScaleInstance(scale, scale));
          g2D.translate(stroke.getLineWidth() / 2, -(float)(textBounds.getY()));
          if (outlineColor != null) {
            g2D.setColor(new Color(outlineColor));
            g2D.setStroke(stroke);
            TextLayout textLayout = new TextLayout(text, font, g2D.getFontRenderContext());
            g2D.draw(textLayout.getOutline(null));
          }
          g2D.setFont(font);
          g2D.setColor(color != null ?  new Color(color) : UIManager.getColor("TextField.foreground"));
          g2D.drawString(text, 0f, 0f);
          g2D.dispose();
  
          Transform3D scaleTransform = new Transform3D();
          scaleTransform.setScale(new Vector3d(textWidth, 1, textHeight));
          // 베이스 라인 중앙으로 이동
          this.baseLineTransform = new Transform3D();
          this.baseLineTransform.setTranslation(new Vector3d(0, 0, textHeight / 2 + textBounds.getY()));
          this.baseLineTransform.mul(scaleTransform);
          this.texture = new TextureLoader(textureImage).getTexture();
          this.text = text;
          this.style = style;
          this.color = color;
        } else {
          clear();
        }
      }
      
      if (this.texture != null) {
        if (numChildren() == 0) {
          BranchGroup group = new BranchGroup();
          group.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
          group.setCapability(BranchGroup.ALLOW_DETACH);
          
          TransformGroup transformGroup = new TransformGroup();
          transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
          transformGroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
          group.addChild(transformGroup);
  
          Appearance appearance = new Appearance();
          appearance.setMaterial(getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, 0));
          appearance.setPolygonAttributes(DEFAULT_POLYGON_ATTRIBUTES);
          appearance.setTextureAttributes(MODULATE_TEXTURE_ATTRIBUTES);
          appearance.setTransparencyAttributes(DEFAULT_TRANSPARENCY_ATTRIBUTES);
          appearance.setTexCoordGeneration(new TexCoordGeneration(TexCoordGeneration.OBJECT_LINEAR,
              TexCoordGeneration.TEXTURE_COORDINATE_2, new Vector4f(1, 0, 0, .5f), new Vector4f(0, 1, -1, .5f)));
          appearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
  
          Box box = new Box(0.5f, 0f, 0.5f, appearance);
          Shape3D shape = box.getShape(Box.TOP);
          box.removeChild(shape);
          transformGroup.addChild(shape);
          
          addChild(group);
        }
        
        TransformGroup transformGroup = (TransformGroup)(((Group)getChild(0)).getChild(0));
        Transform3D pitchRotation = new Transform3D();
        pitchRotation.rotX(pitch);
        pitchRotation.mul(this.baseLineTransform);
        Transform3D rotationY = new Transform3D();
        rotationY.rotY(-label.getAngle());
        rotationY.mul(pitchRotation);
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(label.getX(), label.getGroundElevation(), label.getY()));
        transform.mul(rotationY);
        transformGroup.setTransform(transform);
        ((Shape3D)transformGroup.getChild(0)).getAppearance().setTexture(this.texture);
      }
    } else {
      clear();
    }
  }

  private void clear() {
    removeAllChildren();
    this.text  = null;
    this.style = null;
    this.color = null;
    this.texture = null;
    this.baseLineTransform = null;
  }
}
