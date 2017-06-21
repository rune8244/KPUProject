package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Camera;
import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Elevatable;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.Label;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.ObserverCamera;
import com.eteks.homeview3d.model.Room;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.SelectionEvent;
import com.eteks.homeview3d.model.SelectionListener;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.model.Wall;

public class HomeController3D implements Controller {
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;
  private final UndoableEditSupport   undoSupport;
  private View                        home3DView;
  private final CameraControllerState topCameraState;
  private final CameraControllerState observerCameraState;
  private CameraControllerState       cameraState;

  
  public HomeController3D(final Home home, 
                          UserPreferences preferences,
                          ViewFactory viewFactory, 
                          ContentManager contentManager, 
                          UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.undoSupport = undoSupport;
    this.topCameraState = new TopCameraState(preferences);
    this.observerCameraState = new ObserverCameraState();
    setCameraState(home.getCamera() == home.getTopCamera() 
        ? this.topCameraState
        : this.observerCameraState);
    addModelListeners(home);
  }

  private void addModelListeners(final Home home) {
    home.addPropertyChangeListener(Home.Property.CAMERA, new PropertyChangeListener() {      
        public void propertyChange(PropertyChangeEvent ev) {
          setCameraState(home.getCamera() == home.getTopCamera() 
              ? topCameraState
              : observerCameraState);
        }
      });
    final PropertyChangeListener levelElevationChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (Level.Property.ELEVATION.name().equals(ev.getPropertyName()) 
              && home.getEnvironment().isObserverCameraElevationAdjusted()) {
            home.getObserverCamera().setZ(Math.max(getObserverCameraMinimumElevation(home), 
                home.getObserverCamera().getZ() + (Float)ev.getNewValue() - (Float)ev.getOldValue()));
          }
        }
      };
    Level selectedLevel = home.getSelectedLevel();
    if (selectedLevel != null) {
      selectedLevel.addPropertyChangeListener(levelElevationChangeListener);
    }
    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          Level oldSelectedLevel = (Level)ev.getOldValue();
          Level selectedLevel = home.getSelectedLevel();
          if (home.getEnvironment().isObserverCameraElevationAdjusted()) {
            home.getObserverCamera().setZ(Math.max(getObserverCameraMinimumElevation(home), 
                home.getObserverCamera().getZ() 
                + (selectedLevel == null ? 0 : selectedLevel.getElevation()) 
                - (oldSelectedLevel == null ? 0 : oldSelectedLevel.getElevation())));
          }
          if (oldSelectedLevel != null) {
            oldSelectedLevel.removePropertyChangeListener(levelElevationChangeListener);
          }
          if (selectedLevel != null) {
            selectedLevel.addPropertyChangeListener(levelElevationChangeListener);
          }
        }
      }); 
    PropertyChangeListener selectedLevelListener = new PropertyChangeListener() {
         public void propertyChange(PropertyChangeEvent ev) {
           List<Level> levels = home.getLevels();
           Level selectedLevel = home.getSelectedLevel();
           boolean visible = true;
           for (int i = 0; i < levels.size(); i++) {
             levels.get(i).setVisible(visible);
             if (levels.get(i) == selectedLevel
                 && !home.getEnvironment().isAllLevelsVisible()) {
               visible = false;
             }
           }
         }
       };
    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, selectedLevelListener);     
    home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.ALL_LEVELS_VISIBLE, selectedLevelListener);
  }

  private float getObserverCameraMinimumElevation(final Home home) {
    List<Level> levels = home.getLevels();
    float minimumElevation = levels.size() == 0  ? 10  : 10 + levels.get(0).getElevation();
    return minimumElevation;
  }

 
  public View getView() {
    if (this.home3DView == null) {
      this.home3DView = this.viewFactory.createView3D(this.home, this.preferences, this);
    }
    return this.home3DView;
  }

 
  public void viewFromTop() {
    this.home.setCamera(this.home.getTopCamera());
  }
  
  
  public void viewFromObserver() {
    this.home.setCamera(this.home.getObserverCamera());
  }
  
  
  public void storeCamera(String name) {
    Camera camera = this.home.getCamera().clone();
    camera.setName(name);
    List<Camera> homeStoredCameras = this.home.getStoredCameras();
    ArrayList<Camera> storedCameras = new ArrayList<Camera>(homeStoredCameras.size() + 1);
    storedCameras.addAll(homeStoredCameras);
    for (int i = storedCameras.size() - 1; i >= 0; i--) {
      Camera storedCamera = storedCameras.get(i);
      if (name.equals(storedCamera.getName())
          || (camera.getX() == storedCamera.getX()
              && camera.getY() == storedCamera.getY()
              && camera.getZ() == storedCamera.getZ()
              && camera.getPitch() == storedCamera.getPitch()
              && camera.getYaw() == storedCamera.getYaw()
              && camera.getFieldOfView() == storedCamera.getFieldOfView()
              && camera.getTime() == storedCamera.getTime()
              && camera.getLens() == storedCamera.getLens())) {
        storedCameras.remove(i);
      }
    }
    storedCameras.add(0, camera);
    while (storedCameras.size() > this.preferences.getStoredCamerasMaxCount()) {
      storedCameras.remove(storedCameras.size() - 1);
    }
    this.home.setStoredCameras(storedCameras);
  }
  
  public void goToCamera(Camera camera) {
    if (camera instanceof ObserverCamera) {
      viewFromObserver();
    } else {
      viewFromTop();
    }
    this.cameraState.goToCamera(camera);
    ArrayList<Camera> storedCameras = new ArrayList<Camera>(this.home.getStoredCameras());
    storedCameras.remove(camera);
    storedCameras.add(0, camera);
    this.home.setStoredCameras(storedCameras);
  }


  public void deleteCameras(List<Camera> cameras) {
    List<Camera> homeStoredCameras = this.home.getStoredCameras();
    ArrayList<Camera> storedCameras = new ArrayList<Camera>(homeStoredCameras.size() - cameras.size());
    for (Camera camera : homeStoredCameras) {
      if (!cameras.contains(camera)) {
        storedCameras.add(camera);
      }
    }
    this.home.setStoredCameras(storedCameras);
  }

  public void displayAllLevels() {
    this.home.getEnvironment().setAllLevelsVisible(true);
  }
  
  public void displaySelectedLevel() {
    this.home.getEnvironment().setAllLevelsVisible(false);
  }
  
  public void modifyAttributes() {
    new Home3DAttributesController(this.home, this.preferences, 
        this.viewFactory, this.contentManager, this.undoSupport).displayView(getView());    
  }
  
  protected void setCameraState(CameraControllerState state) {
    if (this.cameraState != null) {
      this.cameraState.exit();
    }
    this.cameraState = state;
    this.cameraState.enter();
  }
   
  public void moveCamera(float delta) {
    this.cameraState.moveCamera(delta);
  }


  public void moveCameraSideways(float delta) {
    this.cameraState.moveCameraSideways(delta);
  }

  public void elevateCamera(float delta) {
    this.cameraState.elevateCamera(delta);
  }

  public void rotateCameraYaw(float delta) {
    this.cameraState.rotateCameraYaw(delta);
  }
 
  public void rotateCameraPitch(float delta) {
    this.cameraState.rotateCameraPitch(delta);
  }

  protected CameraControllerState getObserverCameraState() {
    return this.observerCameraState;
  }

  protected CameraControllerState getTopCameraState() {
    return this.topCameraState;
  }

  protected static abstract class CameraControllerState {
    public void enter() {
    }

    public void exit() {
    }

    public void moveCamera(float delta) {
    }
    
    public void moveCameraSideways(float delta) {
    }

    public void elevateCamera(float delta) {     
    }
    
    public void rotateCameraYaw(float delta) {
    }

    public void rotateCameraPitch(float delta) {
    }

    public void goToCamera(Camera camera) {
    }
  }
  
  
  private class TopCameraState extends CameraControllerState {
    private final float MIN_WIDTH  = 100;
    private final float MIN_DEPTH  = MIN_WIDTH;
    private final float MIN_HEIGHT = 20;
    
    private Camera      topCamera;
    private float []    aerialViewBoundsLowerPoint;
    private float []    aerialViewBoundsUpperPoint;
    private float       minDistanceToAerialViewCenter;
    private float       maxDistanceToAerialViewCenter;
    private boolean     aerialViewCenteredOnSelectionEnabled;
    private PropertyChangeListener objectChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          updateCameraFromHomeBounds(false);
        }
      };
    private CollectionListener<Level> levelsListener = new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            ev.getItem().addPropertyChangeListener(objectChangeListener);
          } else if (ev.getType() == CollectionEvent.Type.DELETE) {
            ev.getItem().removePropertyChangeListener(objectChangeListener);
          } 
          updateCameraFromHomeBounds(false);
        }
      };
    private CollectionListener<Wall> wallsListener = new CollectionListener<Wall>() {
        public void collectionChanged(CollectionEvent<Wall> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            ev.getItem().addPropertyChangeListener(objectChangeListener);
          } else if (ev.getType() == CollectionEvent.Type.DELETE) {
            ev.getItem().removePropertyChangeListener(objectChangeListener);
          } 
          updateCameraFromHomeBounds(false);
        }
      };
    private CollectionListener<HomePieceOfFurniture> furnitureListener = new CollectionListener<HomePieceOfFurniture>() {
        public void collectionChanged(CollectionEvent<HomePieceOfFurniture> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            ev.getItem().addPropertyChangeListener(objectChangeListener);
            updateCameraFromHomeBounds(home.getFurniture().size() == 1
                && home.getWalls().isEmpty()
                && home.getRooms().isEmpty());
          } else if (ev.getType() == CollectionEvent.Type.DELETE) {
            ev.getItem().removePropertyChangeListener(objectChangeListener);
            updateCameraFromHomeBounds(false);
          } 
        }
      };
    private CollectionListener<Room> roomsListener = new CollectionListener<Room>() {
        public void collectionChanged(CollectionEvent<Room> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            ev.getItem().addPropertyChangeListener(objectChangeListener);
          } else if (ev.getType() == CollectionEvent.Type.DELETE) {
            ev.getItem().removePropertyChangeListener(objectChangeListener);
          } 
          updateCameraFromHomeBounds(false);
        }
      };
    private CollectionListener<Label> labelsListener = new CollectionListener<Label>() {
        public void collectionChanged(CollectionEvent<Label> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            ev.getItem().addPropertyChangeListener(objectChangeListener);
          } else if (ev.getType() == CollectionEvent.Type.DELETE) {
            ev.getItem().removePropertyChangeListener(objectChangeListener);
          } 
          updateCameraFromHomeBounds(false);
        }
      };
    private SelectionListener selectionListener = new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          updateCameraFromHomeBounds(false);
        }
      };

    public TopCameraState(UserPreferences preferences) {
      this.aerialViewCenteredOnSelectionEnabled = preferences.isAerialViewCenteredOnSelectionEnabled();
      preferences.addPropertyChangeListener(UserPreferences.Property.AERIAL_VIEW_CENTERED_ON_SELECTION_ENABLED, 
          new UserPreferencesChangeListener(this));
    }

    @Override
    public void enter() {
      this.topCamera = home.getCamera();
      updateCameraFromHomeBounds(false);
      for (Level level : home.getLevels()) {
        level.addPropertyChangeListener(this.objectChangeListener);
      }
      home.addLevelsListener(this.levelsListener);
      for (Wall wall : home.getWalls()) {
        wall.addPropertyChangeListener(this.objectChangeListener);
      }
      home.addWallsListener(this.wallsListener);
      for (HomePieceOfFurniture piece : home.getFurniture()) {
        piece.addPropertyChangeListener(this.objectChangeListener);
      }
      home.addFurnitureListener(this.furnitureListener);
      for (Room room : home.getRooms()) {
        room.addPropertyChangeListener(this.objectChangeListener);
      }
      home.addRoomsListener(this.roomsListener);
      for (Label label : home.getLabels()) {
        label.addPropertyChangeListener(this.objectChangeListener);
      }
      home.addLabelsListener(this.labelsListener);
      home.addSelectionListener(this.selectionListener);
    }
    
    public void setAerialViewCenteredOnSelectionEnabled(boolean aerialViewCenteredOnSelectionEnabled) {
      this.aerialViewCenteredOnSelectionEnabled = aerialViewCenteredOnSelectionEnabled;
      updateCameraFromHomeBounds(false);
    }
    
    private void updateCameraFromHomeBounds(boolean firstPieceOfFurnitureAddedToEmptyHome) {
      if (this.aerialViewBoundsLowerPoint == null) {
        updateAerialViewBoundsFromHomeBounds(this.aerialViewCenteredOnSelectionEnabled);
      }
      float distanceToCenter = getCameraToAerialViewCenterDistance();
      updateAerialViewBoundsFromHomeBounds(this.aerialViewCenteredOnSelectionEnabled);
      updateCameraIntervalToAerialViewCenter();
      placeCameraAt(distanceToCenter, firstPieceOfFurnitureAddedToEmptyHome);
    }

    private float getCameraToAerialViewCenterDistance() {
      return (float)Math.sqrt(Math.pow((this.aerialViewBoundsLowerPoint [0] + this.aerialViewBoundsUpperPoint [0]) / 2 - this.topCamera.getX(), 2) 
          + Math.pow((this.aerialViewBoundsLowerPoint [1] + this.aerialViewBoundsUpperPoint [1]) / 2 - this.topCamera.getY(), 2) 
          + Math.pow((this.aerialViewBoundsLowerPoint [2] + this.aerialViewBoundsUpperPoint [2]) / 2 - this.topCamera.getZ(), 2));
    }

    
    private void updateAerialViewBoundsFromHomeBounds(boolean centerOnSelection) {
      this.aerialViewBoundsLowerPoint = 
      this.aerialViewBoundsUpperPoint = null;
      List<Selectable> selectedItems = Collections.emptyList();
      if (centerOnSelection) { 
        selectedItems = new ArrayList<Selectable>();
        for (Selectable item : home.getSelectedItems()) {
          if (item instanceof Elevatable 
              && isItemAtVisibleLevel((Elevatable)item)
              && (!(item instanceof HomePieceOfFurniture)
                  || ((HomePieceOfFurniture)item).isVisible())
              && (!(item instanceof Label)
                  || ((Label)item).getPitch() != null)) {
            selectedItems.add(item);
          }
        }
      }
      boolean selectionEmpty = selectedItems.size() == 0 || !centerOnSelection;

      boolean containsVisibleWalls = false;
      for (Wall wall : selectionEmpty
                           ? home.getWalls()
                           : Home.getWallsSubList(selectedItems)) {
        if (isItemAtVisibleLevel(wall)) {
          containsVisibleWalls = true;
          
          float wallElevation = wall.getLevel() != null 
              ? wall.getLevel().getElevation() 
              : 0;
          float minZ = selectionEmpty
              ? 0
              : wallElevation;
          
          Float height = wall.getHeight();
          float maxZ;
          if (height != null) {
            maxZ = wallElevation + height;
          } else {
            maxZ = wallElevation + home.getWallHeight();
          }
          Float heightAtEnd = wall.getHeightAtEnd();
          if (heightAtEnd != null) {
            maxZ = Math.max(maxZ, wallElevation + heightAtEnd);
          }
          for (float [] point : wall.getPoints()) {
            updateAerialViewBounds(point [0], point [1], minZ, maxZ);
          }
        }
      }

      for (HomePieceOfFurniture piece : selectionEmpty 
                                            ? home.getFurniture()
                                            : Home.getFurnitureSubList(selectedItems)) {
        if (piece.isVisible() && isItemAtVisibleLevel(piece)) {
          float minZ;
          float maxZ;
          if (selectionEmpty) {
            minZ = Math.max(0, piece.getGroundElevation());
            maxZ = Math.max(0, piece.getGroundElevation() + piece.getHeight());
          } else {
            minZ = piece.getGroundElevation();
            maxZ = piece.getGroundElevation() + piece.getHeight();
          }
          for (float [] point : piece.getPoints()) {
            updateAerialViewBounds(point [0], point [1], minZ, maxZ);
          }
        }
      }
      
      for (Room room : selectionEmpty 
                           ? home.getRooms()
                           : Home.getRoomsSubList(selectedItems)) {
        if (isItemAtVisibleLevel(room)) {
          float minZ = 0;
          float maxZ = MIN_HEIGHT;
          Level roomLevel = room.getLevel();
          if (roomLevel != null) {
            minZ = roomLevel.getElevation() - roomLevel.getFloorThickness();
            maxZ = roomLevel.getElevation();
            if (selectionEmpty) {
              minZ = Math.max(0, minZ);
              maxZ = Math.max(MIN_HEIGHT, roomLevel.getElevation());
            }
          }
          for (float [] point : room.getPoints()) {
            updateAerialViewBounds(point [0], point [1], minZ, maxZ);
          }
        }
      }
      
      for (Label label : selectionEmpty
                             ? home.getLabels()
                             : Home.getLabelsSubList(selectedItems)) {
        if (label.getPitch() != null && isItemAtVisibleLevel(label)) {
          float minZ;
          float maxZ;
          if (selectionEmpty) {
            minZ = Math.max(0, label.getGroundElevation());
            maxZ = Math.max(MIN_HEIGHT, label.getGroundElevation());
          } else {
            minZ = 
            maxZ = label.getGroundElevation();
          }
          for (float [] point : label.getPoints()) {
            updateAerialViewBounds(point [0], point [1], minZ, maxZ);
          }
        }
      }
      
      if (this.aerialViewBoundsLowerPoint == null) {
        this.aerialViewBoundsLowerPoint = new float [] {0, 0, 0};
        this.aerialViewBoundsUpperPoint = new float [] {MIN_WIDTH, MIN_DEPTH, MIN_HEIGHT};
      } else if (containsVisibleWalls && selectionEmpty) {
        if (MIN_WIDTH > this.aerialViewBoundsUpperPoint [0] - this.aerialViewBoundsLowerPoint [0]) {
          this.aerialViewBoundsLowerPoint [0] = (this.aerialViewBoundsLowerPoint [0] + this.aerialViewBoundsUpperPoint [0]) / 2 - MIN_WIDTH / 2;
          this.aerialViewBoundsUpperPoint [0] = this.aerialViewBoundsLowerPoint [0] + MIN_WIDTH;
        }
        if (MIN_DEPTH > this.aerialViewBoundsUpperPoint [1] - this.aerialViewBoundsLowerPoint [1]) {
          this.aerialViewBoundsLowerPoint [1] = (this.aerialViewBoundsLowerPoint [1] + this.aerialViewBoundsUpperPoint [1]) / 2 - MIN_DEPTH / 2;
          this.aerialViewBoundsUpperPoint [1] = this.aerialViewBoundsLowerPoint [1] + MIN_DEPTH;
        }
        if (MIN_HEIGHT > this.aerialViewBoundsUpperPoint [2] - this.aerialViewBoundsLowerPoint [2]) {
          this.aerialViewBoundsLowerPoint [2] = (this.aerialViewBoundsLowerPoint [2] + this.aerialViewBoundsUpperPoint [2]) / 2 - MIN_HEIGHT / 2;
          this.aerialViewBoundsUpperPoint [2] = this.aerialViewBoundsLowerPoint [2] + MIN_HEIGHT;
        }
      }
    }

   
    private void updateAerialViewBounds(float x, float y, float minZ, float maxZ) {
      if (this.aerialViewBoundsLowerPoint == null) {
        this.aerialViewBoundsLowerPoint = new float [] {x, y, minZ};
        this.aerialViewBoundsUpperPoint = new float [] {x, y, maxZ};
      } else {
        this.aerialViewBoundsLowerPoint [0] = Math.min(this.aerialViewBoundsLowerPoint [0], x); 
        this.aerialViewBoundsUpperPoint [0] = Math.max(this.aerialViewBoundsUpperPoint [0], x);
        this.aerialViewBoundsLowerPoint [1] = Math.min(this.aerialViewBoundsLowerPoint [1], y); 
        this.aerialViewBoundsUpperPoint [1] = Math.max(this.aerialViewBoundsUpperPoint [1], y);
        this.aerialViewBoundsLowerPoint [2] = Math.min(this.aerialViewBoundsLowerPoint [2], minZ); 
        this.aerialViewBoundsUpperPoint [2] = Math.max(this.aerialViewBoundsUpperPoint [2], maxZ);
      }
    }

   
    private boolean isItemAtVisibleLevel(Elevatable item) {
      return item.getLevel() == null || item.getLevel().isViewableAndVisible();
    }
    
    
    private void updateCameraIntervalToAerialViewCenter() {  
      float homeBoundsWidth = this.aerialViewBoundsUpperPoint [0] - this.aerialViewBoundsLowerPoint [0];
      float homeBoundsDepth = this.aerialViewBoundsUpperPoint [1] - this.aerialViewBoundsLowerPoint [1];
      float homeBoundsHeight = this.aerialViewBoundsUpperPoint [2] - this.aerialViewBoundsLowerPoint [2];
      float halfDiagonal = (float)Math.sqrt(homeBoundsWidth * homeBoundsWidth 
          + homeBoundsDepth * homeBoundsDepth 
          + homeBoundsHeight * homeBoundsHeight) / 2;
      this.minDistanceToAerialViewCenter = halfDiagonal * 1.05f;
      this.maxDistanceToAerialViewCenter = Math.max(5 * this.minDistanceToAerialViewCenter, 2500);
    }
       
    @Override
    public void moveCamera(float delta) {
      delta *= 5;
      float newDistanceToCenter = getCameraToAerialViewCenterDistance() - delta;
      placeCameraAt(newDistanceToCenter, false);
    }

    public void placeCameraAt(float distanceToCenter, boolean firstPieceOfFurnitureAddedToEmptyHome) {
      distanceToCenter = Math.max(distanceToCenter, this.minDistanceToAerialViewCenter);
      distanceToCenter = Math.min(distanceToCenter, this.maxDistanceToAerialViewCenter);
      if (firstPieceOfFurnitureAddedToEmptyHome) {
        distanceToCenter = Math.min(distanceToCenter, 3 * this.minDistanceToAerialViewCenter);
      }
      double distanceToCenterAtGroundLevel = distanceToCenter * Math.cos(this.topCamera.getPitch());
      this.topCamera.setX((this.aerialViewBoundsLowerPoint [0] + this.aerialViewBoundsUpperPoint [0]) / 2 
          + (float)(Math.sin(this.topCamera.getYaw()) * distanceToCenterAtGroundLevel));
      this.topCamera.setY((this.aerialViewBoundsLowerPoint [1] + this.aerialViewBoundsUpperPoint [1]) / 2 
          - (float)(Math.cos(this.topCamera.getYaw()) * distanceToCenterAtGroundLevel));
      this.topCamera.setZ((this.aerialViewBoundsLowerPoint [2] + this.aerialViewBoundsUpperPoint [2]) / 2 
          + (float)Math.sin(this.topCamera.getPitch()) * distanceToCenter);
    }

    @Override
    public void rotateCameraYaw(float delta) {
      float newYaw = this.topCamera.getYaw() + delta;
      double distanceToCenterAtGroundLevel = getCameraToAerialViewCenterDistance() * Math.cos(this.topCamera.getPitch());
      this.topCamera.setYaw(newYaw); 
      this.topCamera.setX((this.aerialViewBoundsLowerPoint [0] + this.aerialViewBoundsUpperPoint [0]) / 2 
          + (float)(Math.sin(newYaw) * distanceToCenterAtGroundLevel));
      this.topCamera.setY((this.aerialViewBoundsLowerPoint [1] + this.aerialViewBoundsUpperPoint [1]) / 2 
          - (float)(Math.cos(newYaw) * distanceToCenterAtGroundLevel));
    }
    
    @Override
    public void rotateCameraPitch(float delta) {
      float newPitch = this.topCamera.getPitch() + delta;
      newPitch = Math.max(newPitch, (float)0);
      newPitch = Math.min(newPitch, (float)Math.PI / 2);
      double distanceToCenter = getCameraToAerialViewCenterDistance();
      double distanceToCenterAtGroundLevel = distanceToCenter * Math.cos(newPitch);
      this.topCamera.setPitch(newPitch); 
      this.topCamera.setX((this.aerialViewBoundsLowerPoint [0] + this.aerialViewBoundsUpperPoint [0]) / 2 
          + (float)(Math.sin(this.topCamera.getYaw()) * distanceToCenterAtGroundLevel));
      this.topCamera.setY((this.aerialViewBoundsLowerPoint [1] + this.aerialViewBoundsUpperPoint [1]) / 2 
          - (float)(Math.cos(this.topCamera.getYaw()) * distanceToCenterAtGroundLevel));
      this.topCamera.setZ((this.aerialViewBoundsLowerPoint [2] + this.aerialViewBoundsUpperPoint [2]) / 2 
          + (float)(distanceToCenter * Math.sin(newPitch)));
    }
    
    @Override
    public void goToCamera(Camera camera) {
      this.topCamera.setCamera(camera);
      this.topCamera.setTime(camera.getTime());
      this.topCamera.setLens(camera.getLens());
      updateCameraFromHomeBounds(false);
    }
    
    @Override
    public void exit() {
      this.topCamera = null;
      for (Wall wall : home.getWalls()) {
        wall.removePropertyChangeListener(this.objectChangeListener);
      }
      home.removeWallsListener(wallsListener);
      for (HomePieceOfFurniture piece : home.getFurniture()) {
        piece.removePropertyChangeListener(this.objectChangeListener);
      }
      home.removeFurnitureListener(this.furnitureListener);
      for (Room room : home.getRooms()) {
        room.removePropertyChangeListener(this.objectChangeListener);
      }
      home.removeRoomsListener(this.roomsListener);
      for (Label label : home.getLabels()) {
        label.removePropertyChangeListener(this.objectChangeListener);
      }
      home.removeLabelsListener(this.labelsListener);
      for (Level level : home.getLevels()) {
        level.removePropertyChangeListener(this.objectChangeListener);
      }
      home.removeLevelsListener(this.levelsListener);
      home.removeSelectionListener(this.selectionListener);
    }
  }
  
 
  private static class UserPreferencesChangeListener implements PropertyChangeListener {
    private WeakReference<TopCameraState>  topCameraState;

    public UserPreferencesChangeListener(TopCameraState topCameraState) {
      this.topCameraState = new WeakReference<TopCameraState>(topCameraState);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      TopCameraState topCameraState = this.topCameraState.get();
      UserPreferences preferences = (UserPreferences)ev.getSource();
      if (topCameraState == null) {
        preferences.removePropertyChangeListener(UserPreferences.Property.valueOf(ev.getPropertyName()), this);
      } else {
        topCameraState.setAerialViewCenteredOnSelectionEnabled(preferences.isAerialViewCenteredOnSelectionEnabled());
      }
    }
  }

  private class ObserverCameraState extends CameraControllerState {
    private ObserverCamera observerCamera;
    private PropertyChangeListener levelElevationChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (Level.Property.ELEVATION.name().equals(ev.getPropertyName())) {
            updateCameraMinimumElevation();
          }
        }
      };
    private CollectionListener<Level> levelsListener = new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            ev.getItem().addPropertyChangeListener(levelElevationChangeListener);
          } else if (ev.getType() == CollectionEvent.Type.DELETE) {
            ev.getItem().removePropertyChangeListener(levelElevationChangeListener);
          } 
          updateCameraMinimumElevation();
        }
      };

    @Override
    public void enter() {
      this.observerCamera = (ObserverCamera)home.getCamera();
      for (Level level : home.getLevels()) {
        level.addPropertyChangeListener(this.levelElevationChangeListener);
      }
      home.addLevelsListener(this.levelsListener);
      home.setSelectedItems(Arrays.asList(new Selectable [] {this.observerCamera}));
    }
    
    @Override
    public void moveCamera(float delta) {
      this.observerCamera.setX(this.observerCamera.getX() - (float)Math.sin(this.observerCamera.getYaw()) * delta);
      this.observerCamera.setY(this.observerCamera.getY() + (float)Math.cos(this.observerCamera.getYaw()) * delta);
      home.setSelectedItems(Arrays.asList(new Selectable [] {this.observerCamera}));
    }
    
    @Override
    public void moveCameraSideways(float delta) {
      this.observerCamera.setX(this.observerCamera.getX() - (float)Math.cos(this.observerCamera.getYaw()) * delta);
      this.observerCamera.setY(this.observerCamera.getY() - (float)Math.sin(this.observerCamera.getYaw()) * delta);
      home.setSelectedItems(Arrays.asList(new Selectable [] {this.observerCamera}));
    }
    
    @Override
    public void elevateCamera(float delta) {
      float newElevation = this.observerCamera.getZ() + delta; 
      newElevation = Math.min(Math.max(newElevation, getMinimumElevation()), preferences.getLengthUnit().getMaximumElevation());
      this.observerCamera.setZ(newElevation);
      home.setSelectedItems(Arrays.asList(new Selectable [] {this.observerCamera}));
    }

    private void updateCameraMinimumElevation() {
      observerCamera.setZ(Math.max(observerCamera.getZ(), getMinimumElevation()));
    }

    public float getMinimumElevation() {
      List<Level> levels = home.getLevels();
      if (levels.size() > 0) {
        return 10 + levels.get(0).getElevation();
      } else {
        return 10;
      }
    }
    
    @Override
    public void rotateCameraYaw(float delta) {
      this.observerCamera.setYaw(this.observerCamera.getYaw() + delta); 
      home.setSelectedItems(Arrays.asList(new Selectable [] {this.observerCamera}));
    }
    
    @Override
    public void rotateCameraPitch(float delta) {
      float newPitch = this.observerCamera.getPitch() + delta; 
      newPitch = Math.max(newPitch, -(float)Math.PI / 2);
      newPitch = Math.min(newPitch, (float)Math.PI / 2);
      this.observerCamera.setPitch(newPitch); 
      home.setSelectedItems(Arrays.asList(new Selectable [] {this.observerCamera}));
    }
    
    @Override
    public void goToCamera(Camera camera) {
      this.observerCamera.setCamera(camera);
      this.observerCamera.setTime(camera.getTime());
      this.observerCamera.setLens(camera.getLens());
    }
    
    @Override
    public void exit() {
      List<Selectable> selectedItems = home.getSelectedItems();
      if (selectedItems.contains(this.observerCamera)) {
        selectedItems = new ArrayList<Selectable>(selectedItems);
        selectedItems.remove(this.observerCamera);
        home.setSelectedItems(selectedItems);
      }
      for (Level level : home.getLevels()) {
        level.removePropertyChangeListener(this.levelElevationChangeListener);
      }
      home.removeLevelsListener(this.levelsListener);
      this.observerCamera = null;
    }
  }
}
