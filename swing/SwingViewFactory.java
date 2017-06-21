package com.eteks.homeview3d.swing;

import java.security.AccessControlException;

import com.eteks.homeview3d.model.BackgroundImage;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.viewcontroller.BackgroundImageWizardController;
import com.eteks.homeview3d.viewcontroller.BaseboardChoiceController;
import com.eteks.homeview3d.viewcontroller.CompassController;
import com.eteks.homeview3d.viewcontroller.DialogView;
import com.eteks.homeview3d.viewcontroller.FurnitureCatalogController;
import com.eteks.homeview3d.viewcontroller.FurnitureController;
import com.eteks.homeview3d.viewcontroller.HelpController;
import com.eteks.homeview3d.viewcontroller.HelpView;
import com.eteks.homeview3d.viewcontroller.Home3DAttributesController;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.HomeController3D;
import com.eteks.homeview3d.viewcontroller.HomeFurnitureController;
import com.eteks.homeview3d.viewcontroller.HomeView;
import com.eteks.homeview3d.viewcontroller.ImportedFurnitureWizardController;
import com.eteks.homeview3d.viewcontroller.ImportedFurnitureWizardStepsView;
import com.eteks.homeview3d.viewcontroller.ImportedTextureWizardController;
import com.eteks.homeview3d.viewcontroller.LabelController;
import com.eteks.homeview3d.viewcontroller.LevelController;
import com.eteks.homeview3d.viewcontroller.ModelMaterialsController;
import com.eteks.homeview3d.viewcontroller.ObserverCameraController;
import com.eteks.homeview3d.viewcontroller.PageSetupController;
import com.eteks.homeview3d.viewcontroller.PhotoController;
import com.eteks.homeview3d.viewcontroller.PhotosController;
import com.eteks.homeview3d.viewcontroller.PlanController;
import com.eteks.homeview3d.viewcontroller.PlanView;
import com.eteks.homeview3d.viewcontroller.PolylineController;
import com.eteks.homeview3d.viewcontroller.PrintPreviewController;
import com.eteks.homeview3d.viewcontroller.RoomController;
import com.eteks.homeview3d.viewcontroller.TextureChoiceController;
import com.eteks.homeview3d.viewcontroller.TextureChoiceView;
import com.eteks.homeview3d.viewcontroller.ThreadedTaskController;
import com.eteks.homeview3d.viewcontroller.ThreadedTaskView;
import com.eteks.homeview3d.viewcontroller.UserPreferencesController;
import com.eteks.homeview3d.viewcontroller.VideoController;
import com.eteks.homeview3d.viewcontroller.View;
import com.eteks.homeview3d.viewcontroller.ViewFactory;
import com.eteks.homeview3d.viewcontroller.WallController;
import com.eteks.homeview3d.viewcontroller.WizardController;

public class SwingViewFactory implements ViewFactory {
  static {
    SwingTools.updateComponentDefaults();
  }
 
  public View createFurnitureCatalogView(FurnitureCatalog catalog,
                                         UserPreferences preferences,
                                         FurnitureCatalogController furnitureCatalogController) {
    if (preferences == null || preferences.isFurnitureCatalogViewedInTree()) {
      return new FurnitureCatalogTree(catalog, preferences, furnitureCatalogController);
    } else {
      return new FurnitureCatalogListPanel(catalog, preferences, furnitureCatalogController);
    }
  }
  
 
  public View createFurnitureView(Home home, UserPreferences preferences,
                                  FurnitureController furnitureController) {
    return new FurnitureTable(home, preferences, furnitureController);
  }

  public PlanView createPlanView(Home home, UserPreferences preferences,
                                 PlanController planController) {
    return new MultipleLevelsPlanPanel(home, preferences, planController);
  }

  public View createView3D(Home home, UserPreferences preferences,
                           HomeController3D homeController3D) {
    try {
      if (!Boolean.getBoolean("com.eteks.homeview3d.no3D")) {
        return new HomeComponent3D(home, preferences, homeController3D);
      }
    } catch (AccessControlException ex) {
    }
    return null;
  }

 
  public HomeView createHomeView(Home home, UserPreferences preferences,
                                 HomeController homeController) {
    return new HomePane(home, preferences, homeController);
  }

  public DialogView createWizardView(UserPreferences preferences,
                                     WizardController wizardController) {
    return new WizardPane(preferences, wizardController);
  }

 
  public View createBackgroundImageWizardStepsView(BackgroundImage backgroundImage,
                      UserPreferences preferences, 
                      BackgroundImageWizardController backgroundImageWizardController) {
    return new BackgroundImageWizardStepsPanel(backgroundImage, preferences,  
        backgroundImageWizardController);
  }

  public ImportedFurnitureWizardStepsView createImportedFurnitureWizardStepsView(
                      CatalogPieceOfFurniture piece,
                      String modelName, boolean importHomePiece,
                      UserPreferences preferences, 
                      ImportedFurnitureWizardController importedFurnitureWizardController) {
    return new ImportedFurnitureWizardStepsPanel(piece, modelName, importHomePiece,
        preferences, importedFurnitureWizardController);
  }

 
  public View createImportedTextureWizardStepsView(
                      CatalogTexture texture, String textureName,
                      UserPreferences preferences,
                      ImportedTextureWizardController importedTextureWizardController) {
    return new ImportedTextureWizardStepsPanel(texture, textureName, preferences,
        importedTextureWizardController);
  }

  
  public ThreadedTaskView createThreadedTaskView(String taskMessage,
                                                 UserPreferences preferences,
                                                 ThreadedTaskController threadedTaskController) {
    return new ThreadedTaskPanel(taskMessage, preferences, threadedTaskController);
  }

 
  public DialogView createUserPreferencesView(UserPreferences preferences,
                                          UserPreferencesController userPreferencesController) {
    return new UserPreferencesPanel(preferences, userPreferencesController);
  }

  public DialogView createLevelView(UserPreferences preferences, LevelController levelController) {
    return new LevelPanel(preferences, levelController);
  }

  public DialogView createHomeFurnitureView(UserPreferences preferences,
                               HomeFurnitureController homeFurnitureController) {
    return new HomeFurniturePanel(preferences, homeFurnitureController);
  }

 
  public DialogView createWallView(UserPreferences preferences,
                                 WallController wallController) {
    return new WallPanel(preferences, wallController);
  }
 
  public DialogView createRoomView(UserPreferences preferences,
                                   RoomController roomController) {
    return new RoomPanel(preferences, roomController);
  }
  
  public DialogView createPolylineView(UserPreferences preferences,
                                       PolylineController polylineController) {
    return new PolylinePanel(preferences, polylineController);
  }


  public DialogView createLabelView(boolean modification,
                                    UserPreferences preferences,
                                    LabelController labelController) {
    return new LabelPanel(modification, preferences, labelController);
  }

  public DialogView createCompassView(UserPreferences preferences,
                                    CompassController compassController) {
    return new CompassPanel(preferences, compassController);
  }
  
 
  public DialogView createHome3DAttributesView(UserPreferences preferences,
                                  Home3DAttributesController home3DAttributesController) {
    return new Home3DAttributesPanel(preferences, home3DAttributesController);    
  }
  
  
  public DialogView createObserverCameraView(UserPreferences preferences,
                                             ObserverCameraController observerCameraController) {
    return new ObserverCameraPanel(preferences, observerCameraController);    
  }
  
  public TextureChoiceView createTextureChoiceView(UserPreferences preferences,
                                            TextureChoiceController textureChoiceController) {
    return new TextureChoiceComponent(preferences, textureChoiceController);
  }

 
  public View createBaseboardChoiceView(UserPreferences preferences,
                                        BaseboardChoiceController baseboardChoiceController) {
    return new BaseboardChoiceComponent(preferences, baseboardChoiceController);
  }

  
  public View createModelMaterialsView(UserPreferences preferences,
                                        ModelMaterialsController controller) {
    return new ModelMaterialsComponent(preferences, controller);
  }

  public DialogView createPageSetupView(UserPreferences preferences,
                                        PageSetupController pageSetupController) {
    return new PageSetupPanel(preferences, pageSetupController);
  }

 
  public DialogView createPrintPreviewView(Home home,
                                           UserPreferences preferences,
                                           HomeController homeController,
                                           PrintPreviewController printPreviewController) {
    return new PrintPreviewPanel(home, preferences, homeController, printPreviewController);
  }
  

  public DialogView createPhotosView(Home home, UserPreferences preferences, 
                                     PhotosController photosController) {
    return new PhotosPanel(home, preferences, photosController);
  }
  
 
  public DialogView createPhotoView(Home home, 
                                    UserPreferences preferences, 
                                    PhotoController photoController) {
    return new PhotoPanel(home, preferences, photoController);
  }
  
 
  public DialogView createVideoView(Home home, 
                                    UserPreferences preferences, 
                                    VideoController videoController) {
    return new VideoPanel(home, preferences, videoController);
  }
  
  public HelpView createHelpView(UserPreferences preferences,
                                 HelpController helpController) {
    return new HelpPane(preferences, helpController);
  }
}
