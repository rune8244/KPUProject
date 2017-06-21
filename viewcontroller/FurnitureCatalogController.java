package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.SelectionEvent;
import com.eteks.homeview3d.model.SelectionListener;
import com.eteks.homeview3d.model.UserPreferences;

public class FurnitureCatalogController implements Controller {
  private final FurnitureCatalog        catalog;
  private final UserPreferences         preferences;
  private final ViewFactory             viewFactory;
  private final ContentManager          contentManager;
  private final List<SelectionListener> selectionListeners;
  private List<CatalogPieceOfFurniture> selectedFurniture;
  private View                          catalogView;


 
  public FurnitureCatalogController(FurnitureCatalog catalog,
                                    ViewFactory viewFactory) {
    this(catalog, null, viewFactory, null);
  }

 
  public FurnitureCatalogController(FurnitureCatalog catalog, 
                                    UserPreferences preferences,
                                    ViewFactory     viewFactory, 
                                    ContentManager  contentManager) {
    this.catalog = catalog;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.selectionListeners = new ArrayList<SelectionListener>();
    this.selectedFurniture  = Collections.emptyList();
    
    this.catalog.addFurnitureListener(new FurnitureCatalogChangeListener(this));
    if (preferences != null) {
      preferences.addPropertyChangeListener(UserPreferences.Property.FURNITURE_CATALOG_VIEWED_IN_TREE, 
          new FurnitureCatalogViewChangeListener(this));
    }
  }

 
  private static class FurnitureCatalogChangeListener implements CollectionListener<CatalogPieceOfFurniture> {
    private WeakReference<FurnitureCatalogController> furnitureCatalogController;
    
    public FurnitureCatalogChangeListener(FurnitureCatalogController furnitureCatalogController) {
      this.furnitureCatalogController = new WeakReference<FurnitureCatalogController>(furnitureCatalogController);
    }
    
    public void collectionChanged(CollectionEvent<CatalogPieceOfFurniture> ev) {
      final FurnitureCatalogController controller = this.furnitureCatalogController.get();
      if (controller == null) {
        ((FurnitureCatalog)ev.getSource()).removeFurnitureListener(this);
      } else if (ev.getType() == CollectionEvent.Type.DELETE) {
        controller.deselectPieceOfFurniture(ev.getItem());
      }
    }
  }

  private static class FurnitureCatalogViewChangeListener implements PropertyChangeListener {
    private WeakReference<FurnitureCatalogController> controller;

    public FurnitureCatalogViewChangeListener(FurnitureCatalogController controller) {
      this.controller = new WeakReference<FurnitureCatalogController>(controller);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      FurnitureCatalogController controller = this.controller.get();
      if (controller == null) {
        ((UserPreferences)ev.getSource()).removePropertyChangeListener(
            UserPreferences.Property.FURNITURE_CATALOG_VIEWED_IN_TREE, this);
      } else {
        controller.catalogView = null;
      }
    }
  }

  
  public View getView() {
    if (this.catalogView == null) {
      this.catalogView = viewFactory.createFurnitureCatalogView(this.catalog, this.preferences, this);
    }
    return this.catalogView;
  }


  
  public void addSelectionListener(SelectionListener listener) {
    this.selectionListeners.add(listener);
  }

  
  public void removeSelectionListener(SelectionListener listener) {
    this.selectionListeners.remove(listener);
  }

  public List<CatalogPieceOfFurniture> getSelectedFurniture() {
    return Collections.unmodifiableList(this.selectedFurniture);
  }
  
  
  public void setSelectedFurniture(List<CatalogPieceOfFurniture> selectedFurniture) {
    this.selectedFurniture = new ArrayList<CatalogPieceOfFurniture>(selectedFurniture);
    if (!this.selectionListeners.isEmpty()) {
      SelectionEvent selectionEvent = new SelectionEvent(this, getSelectedFurniture());      
      SelectionListener [] listeners = this.selectionListeners.
        toArray(new SelectionListener [this.selectionListeners.size()]);
      for (SelectionListener listener : listeners) {
        listener.selectionChanged(selectionEvent);
      }
    }
  }

  private void deselectPieceOfFurniture(CatalogPieceOfFurniture piece) {
    int pieceSelectionIndex = this.selectedFurniture.indexOf(piece);
    if (pieceSelectionIndex != -1) {
      List<CatalogPieceOfFurniture> selectedItems = 
          new ArrayList<CatalogPieceOfFurniture>(getSelectedFurniture());
      selectedItems.remove(pieceSelectionIndex);
      setSelectedFurniture(selectedItems);
    }
  }

  public void modifySelectedFurniture() {
    if (this.preferences != null) {
      if (this.selectedFurniture.size() > 0) {
        CatalogPieceOfFurniture piece = this.selectedFurniture.get(0);
        if (piece.isModifiable()) {
          AddedFurnitureSelector addedFurnitureListener = new AddedFurnitureSelector();
          this.preferences.getFurnitureCatalog().addFurnitureListener(addedFurnitureListener);
          new ImportedFurnitureWizardController(piece, this.preferences, 
              this.viewFactory, this.contentManager).displayView(getView());
          addedFurnitureListener.selectAddedFurniture();
          this.preferences.getFurnitureCatalog().removeFurnitureListener(addedFurnitureListener);
        }
      }
    }
  }

  
  private class AddedFurnitureSelector implements CollectionListener<CatalogPieceOfFurniture> {
    private List<CatalogPieceOfFurniture> addedFurniture = new ArrayList<CatalogPieceOfFurniture>();

    public void collectionChanged(CollectionEvent<CatalogPieceOfFurniture> ev) {
      if (ev.getType() == CollectionEvent.Type.ADD) {
        this.addedFurniture.add(ev.getItem());
      }
    }
    
    public void selectAddedFurniture() {
      if (this.addedFurniture.size() > 0) {
        setSelectedFurniture(this.addedFurniture);
      }
    }
  }

  public void importFurniture() {
    if (this.preferences != null) {
      AddedFurnitureSelector addedFurnitureListener = new AddedFurnitureSelector();
      this.preferences.getFurnitureCatalog().addFurnitureListener(addedFurnitureListener);
      new ImportedFurnitureWizardController(this.preferences, 
          this.viewFactory, this.contentManager).displayView(getView());
      addedFurnitureListener.selectAddedFurniture();
      this.preferences.getFurnitureCatalog().removeFurnitureListener(addedFurnitureListener);
    }
  }

 
  private void importFurniture(String modelName) {
    if (this.preferences != null) {
      new ImportedFurnitureWizardController(modelName, this.preferences, 
          this.viewFactory, this.contentManager).displayView(getView());
    }
  }

  public void deleteSelection() {
    for (CatalogPieceOfFurniture piece : this.selectedFurniture) {
      if (piece.isModifiable()) {
        this.catalog.delete(piece);
      }
    }
  }

 
  public void dropFiles(List<String> importableModels) {
    AddedFurnitureSelector addedFurnitureListener = new AddedFurnitureSelector();
    this.preferences.getFurnitureCatalog().addFurnitureListener(addedFurnitureListener);
    for (String model : importableModels) {
      importFurniture(model);
    }
    addedFurnitureListener.selectAddedFurniture();
    this.preferences.getFurnitureCatalog().removeFurnitureListener(addedFurnitureListener);
  }
}
