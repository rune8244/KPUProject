package com.eteks.homeview3d.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;

import com.eteks.homeview3d.tools.OperatingSystem;

public class UnfocusableToolBar extends JToolBar {
  
  public UnfocusableToolBar() {
    addPropertyChangeListener("componentOrientation", 
        new PropertyChangeListener () {
          public void propertyChange(PropertyChangeEvent evt) {
            updateToolBarButtons();
          }
        });
    addContainerListener(new ContainerListener() {
        public void componentAdded(ContainerEvent ev) {
          updateToolBarButtons();
        }
        
        public void componentRemoved(ContainerEvent ev) {}
      });
  }

 
  private void updateToolBarButtons() {
    ComponentOrientation orientation = getComponentOrientation();
    Component previousComponent = null;
    for (int i = 0, n = getComponentCount(); i < n; i++) {        
      JComponent component = (JComponent)getComponentAtIndex(i); 
      component.setFocusable(false);
      
      if (!(component instanceof AbstractButton)) {
        previousComponent = null;
        continue;
      }          
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        Component nextComponent;
        if (i < n - 1) {
          nextComponent = getComponentAtIndex(i + 1);
        } else {
          nextComponent = null;
        }
        component.putClientProperty("JButton.buttonType", "segmentedTextured");
        if (previousComponent == null
            && !(nextComponent instanceof AbstractButton)) {
          component.putClientProperty("JButton.segmentPosition", "only");
        } else if (previousComponent == null) {
          component.putClientProperty("JButton.segmentPosition", 
              orientation.isLeftToRight() 
                ? "first"
                : "last");
        } else if (!(nextComponent instanceof AbstractButton)) {
          component.putClientProperty("JButton.segmentPosition",
              orientation.isLeftToRight() 
                ? "last"
                : "first");
        } else {
          component.putClientProperty("JButton.segmentPosition", "middle");
        }
        previousComponent = component;
      }
    }
  }
}
