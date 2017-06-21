package com.eteks.homeview3d.viewcontroller;

import com.eteks.homeview3d.model.BackgroundImage;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;

public class ViewFactoryAdapter implements ViewFactory {  
  public View createBackgroundImageWizardStepsView(BackgroundImage backgroundImage, UserPreferences preferences,
                                                   BackgroundImageWizardController backgroundImageWizardController) {
    throw new UnsupportedOperationException();
  }

  public View createFurnitureCatalogView(FurnitureCatalog catalog, UserPreferences preferences,
                                         FurnitureCatalogController furnitureCatalogController) {
    throw new UnsupportedOperationException();
  }

  public View createFurnitureView(Home home, UserPreferences preferences, FurnitureController furnitureController) {
    throw new UnsupportedOperationException();
  }

  public HelpView createHelpView(UserPreferences preferences, HelpController helpController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createHome3DAttributesView(UserPreferences preferences,
                                               Home3DAttributesController home3DAttributesController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createLevelView(UserPreferences preferences, LevelController levelController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createHomeFurnitureView(UserPreferences preferences,
                                            HomeFurnitureController homeFurnitureController) {
    throw new UnsupportedOperationException();
  }

  public HomeView createHomeView(Home home, UserPreferences preferences, HomeController homeController) {
    throw new UnsupportedOperationException();
  }

  public ImportedFurnitureWizardStepsView createImportedFurnitureWizardStepsView(CatalogPieceOfFurniture piece,
              String modelName, boolean importHomePiece, UserPreferences preferences,
              ImportedFurnitureWizardController importedFurnitureWizardController) {
    throw new UnsupportedOperationException();
  }

  public View createImportedTextureWizardStepsView(CatalogTexture texture, String textureName,
                                                   UserPreferences preferences,
                                                   ImportedTextureWizardController importedTextureWizardController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createLabelView(boolean modification, UserPreferences preferences,
                                    LabelController labelController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createPageSetupView(UserPreferences preferences, PageSetupController pageSetupController) {
    throw new UnsupportedOperationException();
  }

  public PlanView createPlanView(Home home, UserPreferences preferences, PlanController planController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createPrintPreviewView(Home home, UserPreferences preferences, HomeController homeController,
                                           PrintPreviewController printPreviewController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createRoomView(UserPreferences preferences, RoomController roomController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createPolylineView(UserPreferences preferences,
                                       PolylineController polylineController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createCompassView(UserPreferences preferences, CompassController compassController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createObserverCameraView(UserPreferences preferences,
                                             ObserverCameraController home3dAttributesController) {
    throw new UnsupportedOperationException();
  }

  public TextureChoiceView createTextureChoiceView(UserPreferences preferences,
                                                   TextureChoiceController textureChoiceController) {
    throw new UnsupportedOperationException();
  }
  
  public View createBaseboardChoiceView(UserPreferences preferences,
                                        BaseboardChoiceController baseboardChoiceController) {
    throw new UnsupportedOperationException();
  }

  public View createModelMaterialsView(UserPreferences preferences, 
                                       ModelMaterialsController modelMaterialsController) {
    throw new UnsupportedOperationException();
  }

  public ThreadedTaskView createThreadedTaskView(String taskMessage, UserPreferences preferences,
                                                 ThreadedTaskController controller) {
    throw new UnsupportedOperationException();
  }

  public DialogView createUserPreferencesView(UserPreferences preferences,
                                              UserPreferencesController userPreferencesController) {
    throw new UnsupportedOperationException();
  }

  public View createView3D(final Home home, UserPreferences preferences, final HomeController3D controller) {
    throw new UnsupportedOperationException();
  }

  public DialogView createWallView(UserPreferences preferences, WallController wallController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createWizardView(UserPreferences preferences, WizardController wizardController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createPhotoView(Home home, UserPreferences preferences, PhotoController photoController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createPhotosView(Home home, UserPreferences preferences, PhotosController photosController) {
    throw new UnsupportedOperationException();
  }

  public DialogView createVideoView(Home home, UserPreferences preferences, VideoController videoController) {
    throw new UnsupportedOperationException();
  }
}
