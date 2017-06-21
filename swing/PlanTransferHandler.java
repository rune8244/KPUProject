package com.eteks.homeview3d.swing;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.View;


public class PlanTransferHandler extends LocatedTransferHandler {
  private final Home           home;
  private final ContentManager contentManager;
  private final HomeController homeController;
  private List<Selectable>     copiedItems;
  private BufferedImage        copiedImage;
  private boolean              isDragging;
  private WindowAdapter        windowDeactivationListener;
  
 
  public PlanTransferHandler(Home home, ContentManager contentManager, 
                             HomeController homeController) {
    this.home = home;  
    this.contentManager = contentManager;
    this.homeController = homeController;  
  }
  
  
  @Override
  public int getSourceActions(JComponent source) {
    return COPY_OR_MOVE;
  }
  
  @Override
  protected Transferable createTransferable(final JComponent source) {
    this.copiedItems = this.home.getSelectedItems();
    final Transferable transferable = new HomeTransferableList(this.copiedItems);
    if (source instanceof PlanComponent) {
      this.copiedImage = ((PlanComponent)source).getClipboardImage();
      return new Transferable () {
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
          if (DataFlavor.imageFlavor.equals(flavor)) {
            return copiedImage;
          } else {
            return transferable.getTransferData(flavor);
          }
        }

        public DataFlavor [] getTransferDataFlavors() {
          ArrayList<DataFlavor> dataFlavors = 
              new ArrayList<DataFlavor>(Arrays.asList(transferable.getTransferDataFlavors()));
          dataFlavors.add(DataFlavor.imageFlavor);
          return dataFlavors.toArray(new DataFlavor [dataFlavors.size()]);
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
          return transferable.isDataFlavorSupported(flavor)
            || DataFlavor.imageFlavor.equals(flavor);
        }
      };
    } else {
      return transferable;
    }
  }
  
 
  @Override
  protected void exportDone(JComponent source, Transferable data, int action) {
    if (action == MOVE) {
      this.homeController.cut(this.copiedItems);      
    }
    this.copiedItems = null;
    this.copiedImage = null;
    this.homeController.enablePasteAction();    
  }

 
  @Override
  protected boolean canImportFlavor(DataFlavor [] flavors) {
    Level selectedLevel = this.home.getSelectedLevel();
    List<DataFlavor> flavorList = Arrays.asList(flavors);
    return (selectedLevel == null || selectedLevel.isViewable())
        && (flavorList.contains(HomeTransferableList.HOME_FLAVOR)
            || flavorList.contains(DataFlavor.javaFileListFlavor));
  }
  
  @Override
  protected void dragEntered(final JComponent destination, Transferable transferable, int dragAction) {
    if (transferable.isDataFlavorSupported(HomeTransferableList.HOME_FLAVOR)
        && destination instanceof PlanComponent
        && this.homeController.getPlanController() != null) {
      try {
        List<Selectable> transferedItems = 
            (List<Selectable>)transferable.getTransferData(HomeTransferableList.HOME_FLAVOR);
        Point2D dropLocation = getDropModelLocation(destination);
        this.homeController.getPlanController().startDraggedItems(transferedItems, 
            (float)dropLocation.getX(), (float)dropLocation.getY());
        this.windowDeactivationListener = new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
              dragExited(destination);
            }
          };
        SwingUtilities.getWindowAncestor(destination).addWindowListener(this.windowDeactivationListener);
        this.isDragging = true;
      } catch (UnsupportedFlavorException ex) {
        throw new RuntimeException("Can't import", ex);
      } catch (IOException ex) {
        throw new RuntimeException("Can't access to data", ex);
      }
    }
  }
  
  @Override
  protected void dragMoved(JComponent destination, Transferable transferable, int dragAction) {
    if (transferable.isDataFlavorSupported(HomeTransferableList.HOME_FLAVOR)
        && destination instanceof PlanComponent
        && this.homeController.getPlanController() != null) {
        Point2D dropLocation = getDropModelLocation(destination);
      this.homeController.getPlanController().moveMouse( 
          (float)dropLocation.getX(), (float)dropLocation.getY());
    }
  }
  
  
  @Override
  protected void dragExited(JComponent destination) {
    if (this.isDragging) {
      SwingUtilities.getWindowAncestor(destination).removeWindowListener(this.windowDeactivationListener);
      this.homeController.getPlanController().stopDraggedItems();
      this.isDragging = false;
    }
  }
  

  @Override
  public boolean importData(JComponent destination, Transferable transferable) {
    if (canImportFlavor(transferable.getTransferDataFlavors())) {
      try {
        if (this.isDragging) {
          dragExited(destination);
        }
        List<DataFlavor> flavorList = Arrays.asList(transferable.getTransferDataFlavors());
        if (flavorList.contains(HomeTransferableList.HOME_FLAVOR)) {
          return importHomeTransferableList(destination, 
              (List<Selectable>)transferable.getTransferData(HomeTransferableList.HOME_FLAVOR));
        } else {
          return importFileList(destination, 
              (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor));
        }
      } catch (UnsupportedFlavorException ex) {
        throw new RuntimeException("Can't import", ex);
      } catch (IOException ex) {
        throw new RuntimeException("Can't access to data", ex);
      }
    } else {
      return false;
    }
  }  

  private boolean importHomeTransferableList(final JComponent destination, 
                                             final List<Selectable> transferedItems) {
    if (isDrop()) {
      Point2D dropLocation = getDropModelLocation(destination);
      if (destination instanceof View) {
        this.homeController.drop(transferedItems, this.homeController.getPlanController().getView(), 
            (float)dropLocation.getX(), (float)dropLocation.getY());
      } else {
        this.homeController.drop(transferedItems,  
            (float)dropLocation.getX(), (float)dropLocation.getY());
      }
    } else {
      this.homeController.paste(transferedItems);
    }
    return true;
  }
  
  private boolean importFileList(final JComponent destination, List<File> files) {
    final Point2D dropLocation = isDrop() 
        ? getDropModelLocation(destination)
        : new Point2D.Float();
    final List<String> importableModels = getModelContents(files, contentManager);
    EventQueue.invokeLater(new Runnable() {
        public void run() {
          homeController.dropFiles(importableModels, 
              (float)dropLocation.getX(), (float)dropLocation.getY());        
        }
      });
    return !importableModels.isEmpty();
  }

  private Point2D getDropModelLocation(JComponent destination) {
    float x = 0;
    float y = 0;
    if (destination instanceof PlanComponent) {
      PlanComponent planView = (PlanComponent)destination;
      Point dropLocation = getDropLocation(); 
      SwingUtilities.convertPointFromScreen(dropLocation, planView);
      x = planView.convertXPixelToModel(dropLocation.x);
      y = planView.convertYPixelToModel(dropLocation.y);
    }
    return new Point2D.Float(x, y);
  }
}
