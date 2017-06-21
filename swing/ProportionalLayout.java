package com.eteks.homeview3d.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;

public class ProportionalLayout implements LayoutManager2 {
 
  public enum Constraints {TOP, BOTTOM}

  private Component topComponent;
  private Component bottomComponent;
  private int       gap;

  
  public ProportionalLayout() {
    this(5);
  }

 
  public ProportionalLayout(int gap) {
    this.gap = gap;
  }
 
  public void addLayoutComponent(Component component, Object constraints) {
    if (constraints == Constraints.TOP) {
      this.topComponent = component; 
    } else if (constraints == Constraints.BOTTOM) {
      this.bottomComponent = component;
    }
  }


  public void addLayoutComponent(String name, Component comp) {
    throw new IllegalArgumentException("Use addLayoutComponent with a Constraints object");
  }

 
  public void removeLayoutComponent(Component component) {
  }
  
 
  public float getLayoutAlignmentX(Container target) {
    return 0.5f;
  }


  public float getLayoutAlignmentY(Container target) {
    return 0f;
  }


  public void invalidateLayout(Container target) {
    // Sizes are computed on the fly each time
  }


  public void layoutContainer(Container parent) {
    Insets parentInsets = parent.getInsets();
    int parentAvailableWidth = parent.getWidth() - parentInsets.left - parentInsets.right;
    int parentAvailableHeight = parent.getHeight() - parentInsets.top - parentInsets.bottom;
    
    boolean topComponentUsed = this.topComponent != null && this.topComponent.getParent() != null;
    if (topComponentUsed) {
      this.topComponent.setBounds(parentInsets.left, parentInsets.top, 
          parentAvailableWidth, 
          Math.min(this.topComponent.getPreferredSize().height, parentAvailableHeight));
    }
preferred size
    if (this.bottomComponent != null && this.bottomComponent.getParent() != null) {
      Dimension bottomComponentPreferredSize = this.bottomComponent.getPreferredSize();
      int bottomComponentHeight = parentAvailableHeight;
      int bottomComponentY = parentInsets.top;
      if (topComponentUsed) {
        int occupiedHeight = this.topComponent.getHeight() + this.gap;
        bottomComponentHeight -= occupiedHeight;
        bottomComponentY += occupiedHeight;
      }
      int bottomComponentWidth = bottomComponentHeight * bottomComponentPreferredSize.width 
                                 / bottomComponentPreferredSize.height;
      int bottomComponentX = parentInsets.left;
      if (bottomComponentWidth > parentAvailableWidth) {
        bottomComponentWidth = parentAvailableWidth;
        int previousHeight = bottomComponentHeight;
        bottomComponentHeight = bottomComponentWidth * bottomComponentPreferredSize.height 
                                / bottomComponentPreferredSize.width;
        bottomComponentY += (previousHeight - bottomComponentHeight)  / 2;
      } else {
        bottomComponentX += (parentAvailableWidth - bottomComponentWidth)  / 2; 
      }
        
      this.bottomComponent.setBounds(bottomComponentX, bottomComponentY, 
          bottomComponentWidth, bottomComponentHeight);
    }
  }

  public Dimension minimumLayoutSize(Container parent) {
    Insets parentInsets = parent.getInsets();
    int minWidth = 0;
    int minHeight = 0;
    boolean topComponentUsed = this.topComponent != null && this.topComponent.getParent() != null;
    if (topComponentUsed) {
      Dimension topComponentMinSize = this.topComponent.getMinimumSize();
      minWidth = Math.max(minWidth, topComponentMinSize.width);
      minHeight = topComponentMinSize.height;
    }
    if (this.bottomComponent != null && this.bottomComponent.getParent() != null) {
      Dimension bottomComponentMinSize = this.bottomComponent.getMinimumSize();
      minWidth = Math.max(minWidth, bottomComponentMinSize.width);
      minHeight += bottomComponentMinSize.height;
      if (topComponentUsed) {
        minHeight += this.gap;
      }
    }
    
    return new Dimension(minWidth + parentInsets.left + parentInsets.right, 
        minHeight + parentInsets.top + parentInsets.bottom);
  }

  public Dimension maximumLayoutSize(Container parent) {
    Insets parentInsets = parent.getInsets();
    int maxWidth = 0;
    int maxHeight = 0;
    boolean topComponentUsed = this.topComponent != null && this.topComponent.getParent() != null;
    if (topComponentUsed) {
      Dimension topComponentMaxSize = this.topComponent.getMaximumSize();
      maxWidth = Math.max(maxWidth, topComponentMaxSize.width);
      maxHeight = topComponentMaxSize.height;
    }
    if (this.bottomComponent != null && this.bottomComponent.getParent() != null) {
      Dimension bottomComponentMaxSize = this.bottomComponent.getMaximumSize();
      maxWidth = Math.max(maxWidth, bottomComponentMaxSize.width);
      maxHeight += bottomComponentMaxSize.height;
      if (topComponentUsed) {
        maxHeight += this.gap;
      }
    }
    
    return new Dimension(maxWidth + parentInsets.left + parentInsets.right, 
        maxHeight + parentInsets.top + parentInsets.bottom);
  }

  
  public Dimension preferredLayoutSize(Container parent) {
    Insets parentInsets = parent.getInsets();
    int preferredWidth = 0;
    int preferredHeight = 0;
    boolean topComponentUsed = this.topComponent != null && this.topComponent.getParent() != null;
    if (topComponentUsed) {
      Dimension topComponentPreferredSize = this.topComponent.getPreferredSize();
      preferredWidth = Math.max(preferredWidth, topComponentPreferredSize.width);
      preferredHeight = topComponentPreferredSize.height;
    }
    if (this.bottomComponent != null && this.bottomComponent.getParent() != null) {
      Dimension bottomComponentPreferredSize = this.bottomComponent.getPreferredSize();
      preferredWidth = Math.max(preferredWidth, bottomComponentPreferredSize.width);
      preferredHeight += bottomComponentPreferredSize.height;
      if (topComponentUsed) {
        preferredHeight += this.gap;
      }
    }
    
    return new Dimension(preferredWidth + parentInsets.left + parentInsets.right, 
        preferredHeight + parentInsets.top + parentInsets.bottom);
  }
}