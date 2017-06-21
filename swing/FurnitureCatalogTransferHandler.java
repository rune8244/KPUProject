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

import javax.swing.Icon;
import javax.swing.JComponent;

import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.PieceOfFurniture;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.FurnitureCatalogController;
import com.eteks.homeview3d.viewcontroller.FurnitureController;

public class FurnitureCatalogTransferHandler extends VisualTransferHandler {
  private final ContentManager             contentManager;
  private final FurnitureCatalogController catalogController;
  private final FurnitureController        furnitureController;
  
  // 선택된 가구 카탈로그 전송 핸들러 생성
  public FurnitureCatalogTransferHandler(ContentManager contentManager,
                                         FurnitureCatalogController catalogController,
                                         FurnitureController furnitureController) {
    this.contentManager = contentManager;
    this.catalogController = catalogController;
    this.furnitureController = furnitureController;
  }

  // 카피 명령어 리턴
  @Override
  public int getSourceActions(JComponent source) {
    return COPY;
  }

  @Override
  public Icon getVisualRepresentation(Transferable transferable) {
    try {
      if (transferable.isDataFlavorSupported(HomeTransferableList.HOME_FLAVOR)) {
        List<Selectable> transferedItems = (List<Selectable>)transferable.
            getTransferData(HomeTransferableList.HOME_FLAVOR);
        if (transferedItems.size() == 1) {
          Selectable transferedItem = transferedItems.get(0);
          if(transferedItem instanceof PieceOfFurniture) {
            return IconManager.getInstance().
                getIcon(((PieceOfFurniture)transferedItem).getIcon(), 48, null);
          }
        }        
      } 
    } catch (UnsupportedFlavorException ex) {
    } catch (IOException ex) {
    }
    return super.getVisualRepresentation(transferable);
  }
  
  @Override
  protected Transferable createTransferable(JComponent source) {
    List<CatalogPieceOfFurniture> selectedCatalogFurniture = this.catalogController.getSelectedFurniture();
    if (!selectedCatalogFurniture.isEmpty()) {
      List<HomePieceOfFurniture> transferedFurniture = 
          new ArrayList<HomePieceOfFurniture>(selectedCatalogFurniture.size());
      for (CatalogPieceOfFurniture catalogPiece : selectedCatalogFurniture) {
        transferedFurniture.add(this.furnitureController.createHomePieceOfFurniture(catalogPiece));
      }
      return new HomeTransferableList(transferedFurniture);
    } else {
      return null;
    }
  }


  @Override
  public boolean canImport(JComponent destination, DataFlavor [] flavors) {
    return this.catalogController != null
        && Arrays.asList(flavors).contains(DataFlavor.javaFileListFlavor);
  }

  // 전송 가능 가구 카탈로그 추가
  @Override
  public boolean importData(JComponent destination, Transferable transferable) {
    if (canImport(destination, transferable.getTransferDataFlavors())) {
      try {
        List<File> files = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
        final List<String> importableModels = new ArrayList<String>();        
        for (File file : files) {
          final String absolutePath = file.getAbsolutePath();
          if (this.contentManager.isAcceptable(absolutePath, ContentManager.ContentType.MODEL)) {
            importableModels.add(absolutePath);
          }        
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
              catalogController.dropFiles(importableModels);
            }
          });
        return !importableModels.isEmpty();
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
