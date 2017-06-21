package com.eteks.homeview3d.swing;

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;

public class FurnitureTransferHandler extends LocatedTransferHandler {
  private final Home                 home;
  private final ContentManager       contentManager;
  private final HomeController       homeController;
  private List<HomePieceOfFurniture> copiedFurniture;
  private String                     copiedCSV;

  // 핸들러 생성
  public FurnitureTransferHandler(Home home, 
                                  ContentManager contentManager,
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
  protected Transferable createTransferable(JComponent source) {
    this.copiedFurniture = Home.getFurnitureSubList(this.home.getSelectedItems());
    final Transferable transferable = new HomeTransferableList(this.copiedFurniture);
    if (source instanceof FurnitureTable) {
      this.copiedCSV = ((FurnitureTable)source).getClipboardCSV();
      return new Transferable () {
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
          if (DataFlavor.stringFlavor.equals(flavor)) {
            return copiedCSV;
          } else {
            return transferable.getTransferData(flavor);
          }
        }

        public DataFlavor [] getTransferDataFlavors() {
          ArrayList<DataFlavor> dataFlavors = 
              new ArrayList<DataFlavor>(Arrays.asList(transferable.getTransferDataFlavors()));
          dataFlavors.add(DataFlavor.stringFlavor);
          return dataFlavors.toArray(new DataFlavor [dataFlavors.size()]);
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
          return transferable.isDataFlavorSupported(flavor)
            || DataFlavor.stringFlavor.equals(flavor);
        }
      };
    } else {
      return transferable;
    }
  }

  @Override
  protected void exportDone(JComponent source, Transferable data, int action) {
    if (action == MOVE) {
      this.homeController.cut(copiedFurniture);      
    }
    this.copiedFurniture = null;
    this.copiedCSV = null;
    this.homeController.enablePasteAction();
  }

  
  @Override
  public boolean canImportFlavor(DataFlavor [] flavors) {
    Level selectedLevel = this.home.getSelectedLevel();
    List<DataFlavor> flavorList = Arrays.asList(flavors);
    return (selectedLevel == null || selectedLevel.isViewable())
        && (flavorList.contains(HomeTransferableList.HOME_FLAVOR)
            || flavorList.contains(DataFlavor.javaFileListFlavor));
  }

  
  @Override
  public boolean importData(JComponent destination, Transferable transferable) {
    if (canImportFlavor(transferable.getTransferDataFlavors())) {
      try {
        List<DataFlavor> flavorList = Arrays.asList(transferable.getTransferDataFlavors());
        if (flavorList.contains(HomeTransferableList.HOME_FLAVOR)) {
          List<Selectable> items = (List<Selectable>)transferable.
              getTransferData(HomeTransferableList.HOME_FLAVOR);
          List<HomePieceOfFurniture> furniture = Home.getFurnitureSubList(items);
          if (isDrop()) {
            this.homeController.drop(furniture, 0, 0);
          } else {
            this.homeController.paste(furniture);          
          }
          return true;
        } else {
          List<File> files = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
          final List<String> importableModels = getModelContents(files, this.contentManager);
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                homeController.dropFiles(importableModels, 0, 0);          
              }
            });
          return !importableModels.isEmpty();
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
}
