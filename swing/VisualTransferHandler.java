package com.eteks.homeview3d.swing;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import com.eteks.homeview3d.tools.OperatingSystem;

public class VisualTransferHandler extends TransferHandler {
  private final DragGestureRecognizerWithVisualRepresentation dragGestureRecognizerWithVisualRepresentation = 
        new DragGestureRecognizerWithVisualRepresentation();

 
  public void exportAsDrag(JComponent source, InputEvent ev, int action) {
    int sourceActions = getSourceActions(source);
    int dragAction = sourceActions & action;
    if (DragSource.isDragImageSupported() 
        && dragAction != NONE 
        && (ev instanceof MouseEvent)) {
      this.dragGestureRecognizerWithVisualRepresentation.gestured(source, (MouseEvent)ev, sourceActions, dragAction);
    } else {
      super.exportAsDrag(source, ev, action);
    }
  }

 
  private class DragGestureRecognizerWithVisualRepresentation extends DragGestureRecognizer {
    public DragGestureRecognizerWithVisualRepresentation() {
      super(DragSource.getDefaultDragSource(), null, NONE, new DragListenerWithVisualRepresentation());
    }

    void gestured(JComponent component, MouseEvent ev, int sourceActions, int action) {
      setComponent(component);
      setSourceActions(sourceActions);
      appendEvent(ev);
      fireDragGestureRecognized(action, ev.getPoint());
    }

    @Override
    protected void registerListeners() {
    }

    @Override
    protected void unregisterListeners() {
    }
  };

 
  private class DragListenerWithVisualRepresentation implements DragGestureListener, DragSourceListener {
    private boolean autoscrolls;
    private final Image EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private final Point OFFSET = OperatingSystem.isMacOSX()
        ? (OperatingSystem.isJavaVersionBetween("1.7", "1.7.0_40") 
               ?  new Point(12, -120) 
               :  new Point(12, 24))
        : new Point(-12, -24);

    public void dragGestureRecognized(DragGestureEvent ev) {
      JComponent component = (JComponent)ev.getComponent();
      Transferable transferable = createTransferable(component);
      if (transferable != null) {
        this.autoscrolls = component.getAutoscrolls();
        component.setAutoscrolls(false);
        try {
          Icon icon = getVisualRepresentation(transferable);
          if (icon != null) {
            BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2D = (Graphics2D)image.getGraphics();
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.66f));
            icon.paintIcon(component, g2D, 0, 0);
            g2D.dispose();
            
            ev.startDrag(null, image, OFFSET,  transferable, this);
          } else {
            ev.startDrag(null, EMPTY_IMAGE, new Point(48, 48), transferable, this);
          }          
        } catch (InvalidDnDOperationException re) {
          component.setAutoscrolls(this.autoscrolls);
        }
      }

      exportDone(component, transferable, NONE);
    }

    public void dragEnter(DragSourceDragEvent ev) {
    }

    public void dragOver(DragSourceDragEvent ev) {
    }

    public void dragExit(DragSourceEvent ev) {
    }

    public void dragDropEnd(DragSourceDropEvent ev) {
      DragSourceContext dragSourceContext = ev.getDragSourceContext();
      JComponent component = (JComponent)dragSourceContext.getComponent();
      if (ev.getDropSuccess()) {
        exportDone(component, dragSourceContext.getTransferable(), ev.getDropAction());
      } else {
        exportDone(component, dragSourceContext.getTransferable(), NONE);
      }
      component.setAutoscrolls(this.autoscrolls);
    }

    public void dropActionChanged(DragSourceDragEvent ev) {
    }
  }
}
