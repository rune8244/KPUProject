package com.eteks.homeview3d.swing;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.eteks.homeview3d.viewcontroller.ContentManager;

public abstract class LocatedTransferHandler extends TransferHandler {
  private JComponent        currentDestination;
  private DropTargetAdapter destinationDropTargetListener;
  private Point             dropLocation;

  
  @Override
  public final boolean canImport(JComponent destination, DataFlavor [] flavors) {
    boolean canImportFlavor = canImportFlavor(flavors);
    
    // 리스너 생성
    if (canImportFlavor && this.currentDestination != destination) {
      if (this.currentDestination != null) {
        this.currentDestination.getDropTarget().removeDropTargetListener(this.destinationDropTargetListener);
      }
      try {
        this.destinationDropTargetListener = new DropTargetAdapter() {
            private boolean acceptedDragAction;
           
            public void drop(DropTargetDropEvent ev) {
              removeDropTargetListener();
            }
            
            @Override
            public void dragEnter(DropTargetDragEvent ev) {
              dropLocation = ev.getLocation();
              SwingUtilities.convertPointToScreen(dropLocation, ev.getDropTargetContext().getComponent());
              Component component = ev.getDropTargetContext().getComponent();
              if (component instanceof JComponent
                  && acceptDropAction(ev.getSourceActions(), ev.getDropAction())) {
                this.acceptedDragAction = true;
                dragEntered((JComponent)component, ev.getTransferable(), ev.getDropAction());
              }
            }
            
            @Override
            public void dragOver(DropTargetDragEvent ev) {
              dropLocation = ev.getLocation();
              SwingUtilities.convertPointToScreen(dropLocation, ev.getDropTargetContext().getComponent());
              Component component = ev.getDropTargetContext().getComponent();
              if (component instanceof JComponent) {
                if (acceptDropAction(ev.getSourceActions(), ev.getDropAction()) ^ this.acceptedDragAction) {
                  this.acceptedDragAction = !this.acceptedDragAction;
                  if (this.acceptedDragAction) {
                    dragEntered((JComponent)component, ev.getTransferable(), ev.getDropAction());
                  } else {
                    dragExited((JComponent)component);
                  }
                }
                if (this.acceptedDragAction) {
                  dragMoved((JComponent)component, ev.getTransferable(), ev.getDropAction());
                }
              }
            }
            
            @Override
            public void dragExit(DropTargetEvent ev) {
              removeDropTargetListener();
              Component component = ev.getDropTargetContext().getComponent();
              if (component instanceof JComponent) {
                dragExited((JComponent)component);
              }
            }
            
            private boolean acceptDropAction(int sourceActions, int dropAction) {
              return dropAction != DnDConstants.ACTION_NONE && (sourceActions & dropAction) == dropAction;                  
            }
            
            private void removeDropTargetListener() {
              currentDestination.getDropTarget().removeDropTargetListener(destinationDropTargetListener);
              destinationDropTargetListener = null;
              currentDestination = null;
              acceptedDragAction = false;
              dropLocation = null;
            }
          };
        destination.getDropTarget().addDropTargetListener(this.destinationDropTargetListener);
        this.currentDestination = destination;
      } catch (TooManyListenersException ex) {
        throw new RuntimeException("Swing doesn't support multicast on DropTarget anymore!");
      }
    }
    
    return canImportFlavor;
  }

  protected void dragEntered(JComponent destination, Transferable transferable, int dragAction) {
  }
  
   protected void dragMoved(JComponent destination, Transferable transferable, int dragAction) {
  }
  
  protected void dragExited(JComponent destination) {
  }
  
  
  protected abstract boolean canImportFlavor(DataFlavor [] flavors);
  
  
  protected Point getDropLocation() {
    if (this.dropLocation != null) {
      return new Point(this.dropLocation);
    } else {
      throw new IllegalStateException("Operation isn't a drag and drop");
    }
  }
  
  protected boolean isDrop() {
    return this.dropLocation != null;
  }  
  
  protected List<String> getModelContents(List<File> files, 
                                          ContentManager contentManager) {
    final List<String> importableModels = new ArrayList<String>();        
    for (File file : files) {
      final String absolutePath = file.getAbsolutePath();
      if (contentManager.isAcceptable(absolutePath, ContentManager.ContentType.MODEL)) {
        importableModels.add(absolutePath);
      }        
    }
    return importableModels;
  }
}
