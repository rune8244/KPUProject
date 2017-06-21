package com.eteks.homeview3d.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

import com.eteks.homeview3d.model.CatalogItem;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.URLContent;

public class CatalogItemToolTip extends JToolTip {
  // 툴팁 정보 표시
  public enum DisplayedInformation {
    ICON, ICON_NAME_AUTHOR, ICON_NAME_AUTHOR_CATEGORY}

  private static final int ICON_SIZE = Math.round(128 * SwingTools.getResolutionScale());
  
  private final DisplayedInformation displayedInformation;
  private final UserPreferences      preferences;
  private final JLabel               itemIconLabel;
  private CatalogItem                catalogItem;

  // 해당 제품 관련된 정보 생성
  public CatalogItemToolTip(boolean ignoreCategory, 
                            UserPreferences preferences) {
    this(ignoreCategory 
           ? DisplayedInformation.ICON_NAME_AUTHOR 
           : DisplayedInformation.ICON_NAME_AUTHOR_CATEGORY, 
         preferences);
  }
  
  // 가구 정보 툴팁 생성
  public CatalogItemToolTip(DisplayedInformation displayedInformation,
                            UserPreferences preferences) {
    this.displayedInformation = displayedInformation;
    this.preferences = preferences;
    this.itemIconLabel = new JLabel();
    this.itemIconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
    this.itemIconLabel.setMinimumSize(this.itemIconLabel.getPreferredSize());
    this.itemIconLabel.setHorizontalAlignment(JLabel.CENTER);
    this.itemIconLabel.setVerticalAlignment(JLabel.CENTER);
    setLayout(new GridBagLayout());
    add(this.itemIconLabel, new GridBagConstraints(0, 0, 1, 1, 1, 1, 
        GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(3, 3, 3, 3), 0, 0));
  }
  
  // 카탈로그 물품 설정
  public void setCatalogItem(CatalogItem item) {
    if (item != this.catalogItem) {
      String tipTextCreator = null;
      if (this.preferences != null) {
        String creator = item.getCreator();
        if (creator != null) {
          tipTextCreator = this.preferences.getLocalizedString(CatalogItemToolTip.class, "tooltipCreator", creator);
        }
      }
      
      String tipText;
      boolean iconInHtmlImgTag = false;
      if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
        if (this.displayedInformation != DisplayedInformation.ICON) { 
          tipText = "<html><center>";
          if (this.displayedInformation == DisplayedInformation.ICON_NAME_AUTHOR_CATEGORY 
              && (item instanceof CatalogPieceOfFurniture)) {
            tipText += "- <b>" + ((CatalogPieceOfFurniture)item).getCategory().getName() + "</b> -<br>";
          }
          
          tipText += "<b>" + item.getName() + "</b>";
          if (tipTextCreator != null) {
            tipText += "<br>" + tipTextCreator;
          }
          tipText += "</center>";
        } else {
          tipText = "";
        }        
      } else if (isTipTextComplete()) {
       
        iconInHtmlImgTag = true;
        
        tipText = "<html><table>";
        if (this.displayedInformation != DisplayedInformation.ICON) { 
          tipText += "<tr><td align='center'>";
          if (this.displayedInformation == DisplayedInformation.ICON_NAME_AUTHOR_CATEGORY
              && (item instanceof CatalogPieceOfFurniture)) {
            tipText += "- <b>" + ((CatalogPieceOfFurniture)item).getCategory().getName() + "</b> -<br>";
          }
          tipText += "<b>" + item.getName() + "</b>";
          if (tipTextCreator != null) {
            tipText += "<br>" + tipTextCreator;
          }
          tipText += "</td></tr>";
        }
      } else if (this.displayedInformation != DisplayedInformation.ICON) {
          tipText = item.getName();
          if (tipTextCreator != null) {
            tipText += " " + tipTextCreator;
          }
      } else {
        tipText = null;
      }
      
      this.itemIconLabel.setIcon(null);
      if (item.getIcon() instanceof URLContent) {
        InputStream iconStream = null;
        try {
          iconStream = item.getIcon().openStream();
          BufferedImage image = ImageIO.read(iconStream);
          if (image != null) {
            int width = Math.round(ICON_SIZE * Math.min(1f, (float)image.getWidth() / image.getHeight()));
            int height = Math.round((float)width * image.getHeight() / image.getWidth());
            if (iconInHtmlImgTag) {
              tipText += "<tr><td width='" + ICON_SIZE + "' height='" + ICON_SIZE + "' align='center' valign='middle'><img width='" + width 
                  + "' height='" + height + "' src='" 
                  + ((URLContent)item.getIcon()).getURL() + "'></td></tr>";
            } else {
              this.itemIconLabel.setIcon(new ImageIcon(image.getHeight() != height
                  ? image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
                  : image));
            } 
          }
        } catch (IOException ex) {
        } finally {
          if (iconStream != null) {
            try {
              iconStream.close();
            } catch (IOException ex) {
            }
          }
        }
      }

      if (iconInHtmlImgTag) {
        tipText += "</table>";
      }
      setTipText(tipText);
      this.catalogItem = item;
    } 
  }
    
  public boolean isTipTextComplete() {
    return !OperatingSystem.isJavaVersionGreaterOrEqual("1.6")
        && OperatingSystem.isMacOSX();
  }
  
  @Override
  public Dimension getPreferredSize() {
    Dimension preferredSize = super.getPreferredSize();
    if (this.itemIconLabel.getIcon() != null) {
      preferredSize.width = Math.max(preferredSize.width, ICON_SIZE + 6);
      preferredSize.height += ICON_SIZE + 6;
    }
    return preferredSize;
  }
}

