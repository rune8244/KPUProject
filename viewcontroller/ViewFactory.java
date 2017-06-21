package com.eteks.homeview3d.viewcontroller;

import com.eteks.homeview3d.model.BackgroundImage;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;

public interface ViewFactory {
  public abstract View createFurnitureCatalogView(FurnitureCatalog catalog,
                                           UserPreferences preferences,
                                           FurnitureCatalogController furnitureCatalogController);

  public abstract View createFurnitureView(Home home, UserPreferences preferences,
                                           FurnitureController furnitureController);

   public abstract PlanView createPlanView(Home home, UserPreferences preferences,
                                          PlanController planController);

  public abstract View createView3D(Home home, UserPreferences preferences,
                                    HomeController3D homeController3D);

  public abstract HomeView createHomeView(Home home, UserPreferences preferences,
                                          HomeController homeController);

  public abstract DialogView createWizardView(UserPreferences preferences,
                                              WizardController wizardController);

  public abstract View createBackgroundImageWizardStepsView(
                                 BackgroundImage backgroundImage,
                                 UserPreferences preferences, 
                                 BackgroundImageWizardController backgroundImageWizardController);

  public abstract ImportedFurnitureWizardStepsView createImportedFurnitureWizardStepsView(
                                 CatalogPieceOfFurniture piece,
                                 String modelName, boolean importHomePiece,
                                 UserPreferences preferences, 
                                 ImportedFurnitureWizardController importedFurnitureWizardController);

  public abstract View createImportedTextureWizardStepsView(
                                 CatalogTexture texture, String textureName,
                                 UserPreferences preferences,
                                 ImportedTextureWizardController importedTextureWizardController);

  
  public abstract ThreadedTaskView createThreadedTaskView(String taskMessage,
                                                          UserPreferences userPreferences, 
                                                          ThreadedTaskController threadedTaskController);

  public abstract DialogView createUserPreferencesView(
                                          UserPreferences preferences,
                                          UserPreferencesController userPreferencesController);
  
  public abstract DialogView createLevelView(UserPreferences preferences, LevelController levelController);

  
  public abstract DialogView createHomeFurnitureView(UserPreferences preferences,
                                         HomeFurnitureController homeFurnitureController);

  public abstract DialogView createWallView(UserPreferences preferences,
                                          WallController wallController);


  public abstract DialogView createRoomView(UserPreferences preferences,
                                            RoomController roomController);
  
  
  public abstract DialogView createPolylineView(UserPreferences preferences,
                                                PolylineController polylineController);

 
  public abstract DialogView createLabelView(boolean modification,
                                             UserPreferences preferences,
                                             LabelController labelController);

 
  public abstract DialogView createCompassView(UserPreferences preferences, 
                                               CompassController compassController);
  
 
  public abstract DialogView createObserverCameraView(UserPreferences preferences,
                                                      ObserverCameraController home3DAttributesController);
  
  
  public abstract DialogView createHome3DAttributesView(UserPreferences preferences,
                                           Home3DAttributesController home3DAttributesController);

  
  public abstract TextureChoiceView createTextureChoiceView(UserPreferences preferences,
                                                     TextureChoiceController textureChoiceController);

  
  public abstract View createBaseboardChoiceView(UserPreferences preferences,
                                                 BaseboardChoiceController baseboardChoiceController);

  
  public abstract View createModelMaterialsView(UserPreferences preferences,
                                                 ModelMaterialsController modelMaterialsController);

  
  public abstract DialogView createPageSetupView(UserPreferences preferences,
                                                    PageSetupController pageSetupController);

  
  public abstract DialogView createPrintPreviewView(Home home,
                                                    UserPreferences preferences,
                                                    HomeController homeController,
                                                    PrintPreviewController printPreviewController);

  
  public abstract DialogView createPhotoView(Home home, UserPreferences preferences, 
                                             PhotoController photoController);

  
  public abstract DialogView createPhotosView(Home home, UserPreferences preferences, 
                                              PhotosController photosController);

  
  public abstract DialogView createVideoView(Home home, UserPreferences preferences, 
                                             VideoController videoController);

 
  public abstract HelpView createHelpView(UserPreferences preferences,
                                          HelpController helpController);
}
