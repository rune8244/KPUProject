package com.eteks.homeview3d.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Home implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;

  public static final long CURRENT_VERSION = 5300;
  
  private static final boolean KEEP_BACKWARD_COMPATIBLITY = true;

  private static final Comparator<Level> LEVEL_ELEVATION_COMPARATOR = new Comparator<Level>() {
      public int compare(Level level1, Level level2) {
        int elevationComparison = Float.compare(level1.getElevation(), level2.getElevation());
        if (elevationComparison != 0) {
          return elevationComparison;
        } else {
          return level1.getElevationIndex() - level2.getElevationIndex();
        }
      }
    };

  public enum Property {NAME, MODIFIED,
    FURNITURE_SORTED_PROPERTY, FURNITURE_DESCENDING_SORTED, FURNITURE_VISIBLE_PROPERTIES,    
    BACKGROUND_IMAGE, CAMERA, PRINT, BASE_PLAN_LOCKED, STORED_CAMERAS, RECOVERED, REPAIRED, 
    SELECTED_LEVEL, ALL_LEVELS_SELECTION};
  
  private List<HomePieceOfFurniture>                  furniture;
  private transient CollectionChangeSupport<HomePieceOfFurniture> furnitureChangeSupport;
  private transient List<Selectable>                  selectedItems;
  private transient List<SelectionListener>           selectionListeners;
  private transient boolean                           allLevelsSelection;
  private List<Level>                                 levels;
  private Level                                       selectedLevel;
  private transient CollectionChangeSupport<Level>    levelsChangeSupport;
  private List<Wall>                                  walls;
  private transient CollectionChangeSupport<Wall>     wallsChangeSupport;
  private List<Room>                                  rooms;
  private transient CollectionChangeSupport<Room>     roomsChangeSupport;
  private List<Polyline>                              polylines;
  private transient CollectionChangeSupport<Polyline> polylinesChangeSupport;
  private List<DimensionLine>                         dimensionLines;
  private transient CollectionChangeSupport<DimensionLine> dimensionLinesChangeSupport;
  private List<Label>                                 labels;
  private transient CollectionChangeSupport<Label>    labelsChangeSupport;
  private Camera                                      camera;
  private String                                      name;
  private final float                                 wallHeight;
  private transient boolean                           modified;
  private transient boolean                           recovered;
  private transient boolean                           repaired;
  private BackgroundImage                             backgroundImage;
  private ObserverCamera                              observerCamera;
  private Camera                                      topCamera;
  private List<Camera>                                storedCameras;  
  private HomeEnvironment                             environment;
  private HomePrint                                   print;
  private String                                      furnitureSortedPropertyName;
  private List<String>                                furnitureVisiblePropertyNames;
  private boolean                                     furnitureDescendingSorted;
  private Map<String, Object>                         visualProperties;
  private Map<String, String>                         properties;
  private transient PropertyChangeSupport             propertyChangeSupport;
  private long                                        version;
  private boolean                                     basePlanLocked; 
  private Compass                                     compass;
  private int                                         skyColor;
  private int                                         groundColor;
  private HomeTexture                                 groundTexture;
  private int                                         lightColor;
  private float                                       wallsAlpha;
  private HomePieceOfFurniture.SortableProperty       furnitureSortedProperty;
  private List<HomePieceOfFurniture.SortableProperty> furnitureVisibleProperties;
  private List<HomePieceOfFurniture>                  furnitureWithDoorsAndWindows;
  private List<HomePieceOfFurniture>                  furnitureWithGroups;

  public Home() {
    this(250);
  }

  public Home(float wallHeight) {
    this(new ArrayList<HomePieceOfFurniture>(), wallHeight);
  }

  public Home(List<HomePieceOfFurniture> furniture) {
    this(furniture, 250);
  }

  private Home(List<HomePieceOfFurniture> furniture, float wallHeight) {
    this.furniture = new ArrayList<HomePieceOfFurniture>(furniture);
    this.walls = new ArrayList<Wall>();
    this.wallHeight = wallHeight;
    this.furnitureVisibleProperties = Arrays.asList(new HomePieceOfFurniture.SortableProperty [] {
        HomePieceOfFurniture.SortableProperty.NAME,
        HomePieceOfFurniture.SortableProperty.WIDTH,
        HomePieceOfFurniture.SortableProperty.DEPTH,
        HomePieceOfFurniture.SortableProperty.HEIGHT,
        HomePieceOfFurniture.SortableProperty.VISIBLE});
    // 초기화 임시 목록 및 기타 필드
    init(true);
    addModelListeners();
  }

  protected Home(Home home) {
    this.wallHeight = home.getWallHeight();
    copyHomeData(home, this);
    initListenersSupport(this);
    addModelListeners();
  }    

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    init(false);
    in.defaultReadObject();
    
    if (KEEP_BACKWARD_COMPATIBLITY) {
      if (this.furnitureSortedPropertyName != null) {
        try {
          this.furnitureSortedProperty = 
              HomePieceOfFurniture.SortableProperty.valueOf(this.furnitureSortedPropertyName);
        } catch (IllegalArgumentException ex) {
        }
        this.furnitureSortedPropertyName = null;
      }
      if (this.furnitureVisiblePropertyNames != null) {
        this.furnitureVisibleProperties = new ArrayList<HomePieceOfFurniture.SortableProperty>();
        for (String furnitureVisiblePropertyName : this.furnitureVisiblePropertyNames) {
          try {
            this.furnitureVisibleProperties.add(
                HomePieceOfFurniture.SortableProperty.valueOf(furnitureVisiblePropertyName));
          } catch (IllegalArgumentException ex) {
          }
        }
        this.furnitureVisiblePropertyNames = null;
      }

      for (Wall wall : this.walls) {
        if (wall.getHeight() == null) {
          wall.setHeight(this.wallHeight);
        }
      }

      if (this.furnitureWithDoorsAndWindows != null) {
        this.furniture = this.furnitureWithDoorsAndWindows;
        this.furnitureWithDoorsAndWindows = null;
      }

      if (this.furnitureWithGroups != null) {
        this.furniture = this.furnitureWithGroups;
        this.furnitureWithGroups = null;
      }

      this.environment.setGroundColor(this.groundColor);
      this.environment.setGroundTexture(this.groundTexture);
      this.environment.setSkyColor(this.skyColor);
      this.environment.setLightColor(this.lightColor);
      this.environment.setWallsAlpha(this.wallsAlpha);
      
      if (this.version <= 3400) {
        int groundColor = this.environment.getGroundColor();
        this.environment.setGroundColor(  
              ((((groundColor >> 16) & 0xFF) * 3 / 4) << 16)
            | ((((groundColor >> 8) & 0xFF) * 3 / 4) << 8)
            | ((groundColor & 0xFF) * 3 / 4));
      }

      if (this.levels.size() > 0) {
        Level previousLevel = this.levels.get(0);
        if (previousLevel.getElevationIndex() == -1) {
          previousLevel.setElevationIndex(0);
        }
        for (int i = 1; i < this.levels.size(); i++) {
          Level level = this.levels.get(i);
          if (level.getElevationIndex() == -1) {
            if (previousLevel.getElevation() == level.getElevation()) {
              level.setElevationIndex(previousLevel.getElevationIndex() + 1);
            } else {
              level.setElevationIndex(0);
            }
          }
          previousLevel = level;
        }
      }

      moveVisualProperty("com.eteks.homeview3d.swing.PhotoPanel.PhotoDialogX");
      moveVisualProperty("com.eteks.homeview3d.swing.PhotoPanel.PhotoDialogY");
      moveVisualProperty("com.eteks.homeview3d.swing.PhotosPanel.PhotoDialogX");
      moveVisualProperty("com.eteks.homeview3d.swing.PhotosPanel.PhotoDialogY");
      moveVisualProperty("com.eteks.homeview3d.swing.VideoPanel.VideoDialogX");
      moveVisualProperty("com.eteks.homeview3d.swing.VideoPanel.VideoDialogY");
      moveVisualProperty("com.eteks.homeview3d.swing.HomeComponent3D.detachedViewX");
      moveVisualProperty("com.eteks.homeview3d.swing.HomeComponent3D.detachedViewY");
      moveVisualProperty("com.eteks.homeview3d.swing.HomeComponent3D.detachedViewWidth");
      moveVisualProperty("com.eteks.homeview3d.swing.HomeComponent3D.detachedViewHeight");
      moveVisualProperty("com.eteks.homeview3d.swing.HomeComponent3D.detachedView");
      moveVisualProperty("com.eteks.homeview3d.swing.HomeComponent3D.detachedViewDividerLocation");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.MainPaneDividerLocation");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.CatalogPaneDividerLocation");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.PlanPaneDividerLocation");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.PlanViewportX");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.PlanViewportY");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.FurnitureViewportY");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.PlanScale");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.ExpandedGroups");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.FrameX");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.FrameY");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.FrameWidth");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.FrameHeight");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.FrameMaximized");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.ScreenWidth");
      moveVisualProperty("com.eteks.homeview3d.homeview3d.ScreenHeight");
    }

    addModelListeners();
  }

  private void moveVisualProperty(String visualPropertyName) {
    if (this.visualProperties.containsKey(visualPropertyName)) {
      Object value = this.visualProperties.get(visualPropertyName);
      this.properties.put(visualPropertyName, value != null  ? String.valueOf(value)  : null);
      this.visualProperties.remove(visualPropertyName);
    }
  }
  
  private void init(boolean newHome) {
    this.selectedItems = new ArrayList<Selectable>();
    initListenersSupport(this);

    if (this.furnitureVisibleProperties == null) {
      this.furnitureVisibleProperties = Arrays.asList(new HomePieceOfFurniture.SortableProperty [] {
          HomePieceOfFurniture.SortableProperty.NAME,
          HomePieceOfFurniture.SortableProperty.WIDTH,
          HomePieceOfFurniture.SortableProperty.DEPTH,
          HomePieceOfFurniture.SortableProperty.HEIGHT,
          HomePieceOfFurniture.SortableProperty.COLOR,
          HomePieceOfFurniture.SortableProperty.MOVABLE,
          HomePieceOfFurniture.SortableProperty.DOOR_OR_WINDOW,
          HomePieceOfFurniture.SortableProperty.VISIBLE});
    }
    // 기본 시점과 일치하는 상단 카메라 생성
    this.topCamera = new Camera(50, 1050, 1010, 
        (float)Math.PI, (float)Math.PI / 4, (float)Math.PI * 63 / 180);
    // 기본 관찰자 카메라 생성
    this.observerCamera = new ObserverCamera(50, 50, 170, 
        7 * (float)Math.PI / 4, (float)Math.PI / 16, (float)Math.PI * 63 / 180);
    this.storedCameras = Collections.emptyList();
    // 새 필드 초기화
    this.environment = new HomeEnvironment();
    this.rooms = new ArrayList<Room>();
    this.polylines = new ArrayList<Polyline>();
    this.dimensionLines = new ArrayList<DimensionLine>();
    this.labels = new ArrayList<Label>();
    this.compass = new Compass(-100, 50, 100);
    this.levels = new ArrayList<Level>();
    this.compass.setVisible(newHome);
    this.visualProperties = new HashMap<String, Object>();
    this.properties = new HashMap<String, String>();
    
    this.version = CURRENT_VERSION;
  }

  private static void initListenersSupport(Home home) {
    home.furnitureChangeSupport = new CollectionChangeSupport<HomePieceOfFurniture>(home);
    home.selectionListeners = new ArrayList<SelectionListener>();
    home.levelsChangeSupport = new CollectionChangeSupport<Level>(home);
    home.wallsChangeSupport = new CollectionChangeSupport<Wall>(home);
    home.roomsChangeSupport = new CollectionChangeSupport<Room>(home);
    home.polylinesChangeSupport = new CollectionChangeSupport<Polyline>(home);
    home.dimensionLinesChangeSupport = new CollectionChangeSupport<DimensionLine>(home);
    home.labelsChangeSupport = new CollectionChangeSupport<Label>(home);
    home.propertyChangeSupport = new PropertyChangeSupport(home);
  }

  private void addModelListeners() {
    final PropertyChangeListener levelElevationChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (Level.Property.ELEVATION.name().equals(ev.getPropertyName())
              || Level.Property.ELEVATION_INDEX.name().equals(ev.getPropertyName())) {
            levels = new ArrayList<Level>(levels);
            Collections.sort(levels, LEVEL_ELEVATION_COMPARATOR);
          }
        }
      };
    for (Level level : this.levels) {
      level.addPropertyChangeListener(levelElevationChangeListener);
    }
    addLevelsListener(new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          switch (ev.getType()) {
            case ADD :
              ev.getItem().addPropertyChangeListener(levelElevationChangeListener);
              break;
            case DELETE :
              ev.getItem().removePropertyChangeListener(levelElevationChangeListener);
              break;
          }
        }
      });
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    this.version = CURRENT_VERSION;
        
    if (KEEP_BACKWARD_COMPATIBLITY) {
      HomePieceOfFurniture.SortableProperty homeFurnitureSortedProperty = this.furnitureSortedProperty;
      List<HomePieceOfFurniture.SortableProperty> homeFurnitureVisibleProperties = this.furnitureVisibleProperties;
      List<HomePieceOfFurniture> homeFurniture = this.furniture;
      try {
        if (this.furnitureSortedProperty != null) {
          this.furnitureSortedPropertyName = this.furnitureSortedProperty.name();
          if (!isFurnitureSortedPropertyBackwardCompatible(this.furnitureSortedProperty)) {
            this.furnitureSortedProperty = null;
          }
        }
        
        this.furnitureVisiblePropertyNames = new ArrayList<String>();
        this.furnitureVisibleProperties = new ArrayList<HomePieceOfFurniture.SortableProperty>();
        for (HomePieceOfFurniture.SortableProperty visibleProperty : homeFurnitureVisibleProperties) {
          this.furnitureVisiblePropertyNames.add(visibleProperty.name());
          if (isFurnitureSortedPropertyBackwardCompatible(visibleProperty)) {
            this.furnitureVisibleProperties.add(visibleProperty);
          }
        }

        this.furnitureWithGroups = this.furniture;
        this.furnitureWithDoorsAndWindows = new ArrayList<HomePieceOfFurniture>(this.furniture.size());
        this.furniture = new ArrayList<HomePieceOfFurniture>(this.furniture.size());
        for (HomePieceOfFurniture piece : this.furnitureWithGroups) {
          if (piece.getClass() == HomePieceOfFurniture.class) {
            this.furnitureWithDoorsAndWindows.add(piece);
            this.furniture.add(piece);
          } else {
            if (piece.getClass() == HomeFurnitureGroup.class) {
              for (HomePieceOfFurniture groupPiece : getGroupFurniture((HomeFurnitureGroup)piece)) {
                this.furnitureWithDoorsAndWindows.add(groupPiece);
                if (groupPiece.getClass() == HomePieceOfFurniture.class) {
                  this.furniture.add(groupPiece);
                } else {
                  // 이전 버전과 호환되는 인스턴스 생성
                  this.furniture.add(new HomePieceOfFurniture(groupPiece));
                }
              }            
            } else {
              this.furnitureWithDoorsAndWindows.add(piece);
              this.furniture.add(new HomePieceOfFurniture(piece));
            }
          }
        }

        this.groundColor = this.environment.getGroundColor();
        this.groundTexture = this.environment.getGroundTexture();
        this.skyColor = this.environment.getSkyColor();
        this.lightColor = this.environment.getLightColor();
        this.wallsAlpha = this.environment.getWallsAlpha();
  
        out.defaultWriteObject();
      } finally {
        this.furniture = homeFurniture;
        this.furnitureWithDoorsAndWindows = null;
        this.furnitureWithGroups = null;
      
        this.furnitureSortedProperty = homeFurnitureSortedProperty;
        this.furnitureVisibleProperties = homeFurnitureVisibleProperties;
        this.furnitureSortedPropertyName = null;
        this.furnitureVisiblePropertyNames = null;
      }
    } else {
      out.defaultWriteObject();
    }
  }

  private boolean isFurnitureSortedPropertyBackwardCompatible(HomePieceOfFurniture.SortableProperty property) {
    switch (property) {
      case NAME : 
      case WIDTH : 
      case DEPTH :
      case HEIGHT :
      case MOVABLE :
      case DOOR_OR_WINDOW :
      case COLOR :
      case VISIBLE :
      case X :
      case Y :
      case ELEVATION :
      case ANGLE :
        return true;
      default :
        return false;
    }
  }

  private List<HomePieceOfFurniture> getGroupFurniture(HomeFurnitureGroup furnitureGroup) {
    List<HomePieceOfFurniture> groupFurniture = new ArrayList<HomePieceOfFurniture>();
    for (HomePieceOfFurniture piece : furnitureGroup.getFurniture()) {
      if (piece instanceof HomeFurnitureGroup) {
        groupFurniture.addAll(getGroupFurniture((HomeFurnitureGroup)piece));
      } else {
        groupFurniture.add(piece);
      }
    }
    return groupFurniture;
  }

  public void addLevelsListener(CollectionListener<Level> listener) {
    this.levelsChangeSupport.addCollectionListener(listener);
  }

  public void removeLevelsListener(CollectionListener<Level> listener) {
    this.levelsChangeSupport.removeCollectionListener(listener);
  } 

  public List<Level> getLevels() {
    return Collections.unmodifiableList(this.levels);
  }

  public void addLevel(Level level) {
    if (level.getElevationIndex() < 0) {
      int elevationIndex = 0;
      for (Level homeLevel : this.levels) {
        if (homeLevel.getElevation() == level.getElevation()) {
          elevationIndex = homeLevel.getElevationIndex() + 1;
        } else if (homeLevel.getElevation() > level.getElevation()) {
          break;
        }
      }
      level.setElevationIndex(elevationIndex);
    }
    this.levels = new ArrayList<Level>(this.levels);
    int index = Collections.binarySearch(this.levels, level, LEVEL_ELEVATION_COMPARATOR);
    int levelIndex;
    if (index >= 0) {
      levelIndex = index; 
    } else {
      levelIndex = -(index + 1);
    }
    this.levels.add(levelIndex, level);
    this.levelsChangeSupport.fireCollectionChanged(level, levelIndex, CollectionEvent.Type.ADD);
  }

  public void deleteLevel(Level level) {
    int index = this.levels.indexOf(level);
    if (index != -1) {
      for (HomePieceOfFurniture piece : this.furniture) {
        if (piece.getLevel() == level) {
          deletePieceOfFurniture(piece);
        }
      }
      for (Room room : this.rooms) {
        if (room.getLevel() == level) {
          deleteRoom(room);
        }
      }
      for (Wall wall : this.walls) {
        if (wall.getLevel() == level) {
          deleteWall(wall);
        }
      }
      for (Polyline polyline : this.polylines) {
        if (polyline.getLevel() == level) {
          deletePolyline(polyline);
        }
      }
      for (DimensionLine dimensionLine : this.dimensionLines) {
        if (dimensionLine.getLevel() == level) {
          deleteDimensionLine(dimensionLine);
        }
      }
      for (Label label : this.labels) {
        if (label.getLevel() == level) {
          deleteLabel(label);
        }
      }
      if (this.selectedLevel == level) {
        if (this.levels.size() == 1) {
          setSelectedLevel(null);
          setAllLevelsSelection(false);
        } else {
          setSelectedLevel(this.levels.get(index >= 1 ? index - 1 : index + 1));
        }
      }
      this.levels = new ArrayList<Level>(this.levels);
      this.levels.remove(index);
      this.levelsChangeSupport.fireCollectionChanged(level, index, CollectionEvent.Type.DELETE);
    }
  }

  public Level getSelectedLevel() {
    return this.selectedLevel;
  }

  public void setSelectedLevel(Level selectedLevel) {
    if (selectedLevel != this.selectedLevel) {
      Level oldSelectedLevel = this.selectedLevel;
      this.selectedLevel = selectedLevel;
      this.propertyChangeSupport.firePropertyChange(Property.SELECTED_LEVEL.name(), oldSelectedLevel, selectedLevel);
    }
  }

  public boolean isAllLevelsSelection() {
    return this.allLevelsSelection;
  }

  public void setAllLevelsSelection(boolean selectionAtAllLevels) {
    if (selectionAtAllLevels != this.allLevelsSelection) {
      this.allLevelsSelection = selectionAtAllLevels;
      this.propertyChangeSupport.firePropertyChange(Property.ALL_LEVELS_SELECTION.name(), !selectionAtAllLevels, selectionAtAllLevels);
    }
  }

  public void addFurnitureListener(CollectionListener<HomePieceOfFurniture> listener) {
    this.furnitureChangeSupport.addCollectionListener(listener);
  }

  public void removeFurnitureListener(CollectionListener<HomePieceOfFurniture> listener) {
    this.furnitureChangeSupport.removeCollectionListener(listener);
  }

  public List<HomePieceOfFurniture> getFurniture() {
    return Collections.unmodifiableList(this.furniture);
  }

  public void addPieceOfFurniture(HomePieceOfFurniture piece) {
    addPieceOfFurniture(piece, this.furniture.size());
  }

  public void addPieceOfFurniture(HomePieceOfFurniture piece, int index) {
    this.furniture = new ArrayList<HomePieceOfFurniture>(this.furniture);
    piece.setLevel(this.selectedLevel);
    this.furniture.add(index, piece);
    this.furnitureChangeSupport.fireCollectionChanged(piece, index, CollectionEvent.Type.ADD);
  }

  public void addPieceOfFurnitureToGroup(HomePieceOfFurniture piece, HomeFurnitureGroup group, int index) {
    piece.setLevel(this.selectedLevel);
    group.addPieceOfFurniture(piece, index);
    this.furnitureChangeSupport.fireCollectionChanged(piece, CollectionEvent.Type.ADD);
  }

  public void deletePieceOfFurniture(HomePieceOfFurniture piece) {
    deselectItem(piece);
    int index = this.furniture.indexOf(piece);
    HomeFurnitureGroup group = index == -1
        ? getPieceOfFurnitureGroup(piece, null, this.furniture)
        : null;
    if (index != -1
        || group != null) {
      piece.setLevel(null);
      this.furniture = new ArrayList<HomePieceOfFurniture>(this.furniture);
      if (group != null) {
        group.deletePieceOfFurniture(piece);
        this.furnitureChangeSupport.fireCollectionChanged(piece, CollectionEvent.Type.DELETE);
      } else {
        this.furniture.remove(index);
        this.furnitureChangeSupport.fireCollectionChanged(piece, index, CollectionEvent.Type.DELETE);
      }
    }
  }

  private HomeFurnitureGroup getPieceOfFurnitureGroup(HomePieceOfFurniture piece, 
                                                      HomeFurnitureGroup furnitureGroup, 
                                                      List<HomePieceOfFurniture> furniture) {
    for (HomePieceOfFurniture homePiece : furniture) {
      if (homePiece.equals(piece)) {
        return furnitureGroup;
      } else if (homePiece instanceof HomeFurnitureGroup) {
        HomeFurnitureGroup group = getPieceOfFurnitureGroup(piece, 
            (HomeFurnitureGroup)homePiece, ((HomeFurnitureGroup)homePiece).getFurniture());
        if (group != null) {
          return group;
        }
      }
    }
    return null;
  }

  public void addSelectionListener(SelectionListener listener) {
    this.selectionListeners.add(listener);
  }

  public void removeSelectionListener(SelectionListener listener) {
    this.selectionListeners.remove(listener);
  }

  public List<Selectable> getSelectedItems() {
    return Collections.unmodifiableList(this.selectedItems);
  }

  public void setSelectedItems(List<? extends Selectable> selectedItems) {
    this.selectedItems = new ArrayList<Selectable>(selectedItems);
    if (!this.selectionListeners.isEmpty()) {
      SelectionEvent selectionEvent = new SelectionEvent(this, getSelectedItems());
      SelectionListener [] listeners = this.selectionListeners.
        toArray(new SelectionListener [this.selectionListeners.size()]);
      for (SelectionListener listener : listeners) {
        listener.selectionChanged(selectionEvent);
      }
    }
  }

  public void deselectItem(Selectable item) {
    int pieceSelectionIndex = this.selectedItems.indexOf(item);
    if (pieceSelectionIndex != -1) {
      List<Selectable> selectedItems = new ArrayList<Selectable>(getSelectedItems());
      selectedItems.remove(pieceSelectionIndex);
      setSelectedItems(selectedItems);
    }
  }

  public void addRoomsListener(CollectionListener<Room> listener) {
    this.roomsChangeSupport.addCollectionListener(listener);
  }

  public void removeRoomsListener(CollectionListener<Room> listener) {
    this.roomsChangeSupport.removeCollectionListener(listener);
  } 

  public List<Room> getRooms() {
    return Collections.unmodifiableList(this.rooms);
  }

  public void addRoom(Room room) {
    addRoom(room, this.rooms.size());
  }

  public void addRoom(Room room, int index) {
    this.rooms = new ArrayList<Room>(this.rooms);
    this.rooms.add(index, room);
    room.setLevel(this.selectedLevel);
    this.roomsChangeSupport.fireCollectionChanged(room, index, CollectionEvent.Type.ADD);
  }

  public void deleteRoom(Room room) {
    deselectItem(room);
    int index = this.rooms.indexOf(room);
    if (index != -1) {
      room.setLevel(null);
      this.rooms = new ArrayList<Room>(this.rooms);
      this.rooms.remove(index);
      this.roomsChangeSupport.fireCollectionChanged(room, index, CollectionEvent.Type.DELETE);
    }
  }

  public void addWallsListener(CollectionListener<Wall> listener) {
    this.wallsChangeSupport.addCollectionListener(listener);
  }

  public void removeWallsListener(CollectionListener<Wall> listener) {
    this.wallsChangeSupport.removeCollectionListener(listener);
  } 

  public Collection<Wall> getWalls() {
    return Collections.unmodifiableCollection(this.walls);
  }

  public void addWall(Wall wall) {
    this.walls = new ArrayList<Wall>(this.walls);
    this.walls.add(wall);
    wall.setLevel(this.selectedLevel);
    this.wallsChangeSupport.fireCollectionChanged(wall, CollectionEvent.Type.ADD);
  }

  public void deleteWall(Wall wall) {
    deselectItem(wall);
    for (Wall otherWall : getWalls()) {
      if (wall.equals(otherWall.getWallAtStart())) {
        otherWall.setWallAtStart(null);
      } else if (wall.equals(otherWall.getWallAtEnd())) {
        otherWall.setWallAtEnd(null);
      }
    }
    int index = this.walls.indexOf(wall);
    if (index != -1) {
      wall.setLevel(null);
      this.walls = new ArrayList<Wall>(this.walls);
      this.walls.remove(index);
      this.wallsChangeSupport.fireCollectionChanged(wall, CollectionEvent.Type.DELETE);
    }
  }

  public void addPolylinesListener(CollectionListener<Polyline> listener) {
    this.polylinesChangeSupport.addCollectionListener(listener);
  }

  public void removePolylinesListener(CollectionListener<Polyline> listener) {
    this.polylinesChangeSupport.removeCollectionListener(listener);
  } 

  public List<Polyline> getPolylines() {
    return Collections.unmodifiableList(this.polylines);
  }

  public void addPolyline(Polyline polyline) {
    addPolyline(polyline, this.polylines.size());
  }

  public void addPolyline(Polyline polyline, int index) {
    this.polylines = new ArrayList<Polyline>(this.polylines);
    this.polylines.add(index, polyline);
    polyline.setLevel(this.selectedLevel);
    this.polylinesChangeSupport.fireCollectionChanged(polyline, CollectionEvent.Type.ADD);
  }

  public void deletePolyline(Polyline polyline) {
    deselectItem(polyline);
    int index = this.polylines.indexOf(polyline);
    if (index != -1) {
      polyline.setLevel(null);
      this.polylines = new ArrayList<Polyline>(this.polylines);
      this.polylines.remove(index);
      this.polylinesChangeSupport.fireCollectionChanged(polyline, CollectionEvent.Type.DELETE);
    }
  }

  public void addDimensionLinesListener(CollectionListener<DimensionLine> listener) {
    this.dimensionLinesChangeSupport.addCollectionListener(listener);
  }

  public void removeDimensionLinesListener(CollectionListener<DimensionLine> listener) {
    this.dimensionLinesChangeSupport.removeCollectionListener(listener);
  } 

  public Collection<DimensionLine> getDimensionLines() {
    return Collections.unmodifiableCollection(this.dimensionLines);
  }

  public void addDimensionLine(DimensionLine dimensionLine) {
    this.dimensionLines = new ArrayList<DimensionLine>(this.dimensionLines);
    this.dimensionLines.add(dimensionLine);
    dimensionLine.setLevel(this.selectedLevel);
    this.dimensionLinesChangeSupport.fireCollectionChanged(dimensionLine, CollectionEvent.Type.ADD);
  }

  public void deleteDimensionLine(DimensionLine dimensionLine) {
    deselectItem(dimensionLine);
    int index = this.dimensionLines.indexOf(dimensionLine);
    if (index != -1) {
      dimensionLine.setLevel(null);
      this.dimensionLines = new ArrayList<DimensionLine>(this.dimensionLines);
      this.dimensionLines.remove(index);
      this.dimensionLinesChangeSupport.fireCollectionChanged(dimensionLine, CollectionEvent.Type.DELETE);
    }
  }

  public void addLabelsListener(CollectionListener<Label> listener) {
    this.labelsChangeSupport.addCollectionListener(listener);
  }

  public void removeLabelsListener(CollectionListener<Label> listener) {
    this.labelsChangeSupport.removeCollectionListener(listener);
  } 

  public Collection<Label> getLabels() {
    return Collections.unmodifiableCollection(this.labels);
  }

  public void addLabel(Label label) {
    this.labels = new ArrayList<Label>(this.labels);
    this.labels.add(label);
    label.setLevel(this.selectedLevel);
    this.labelsChangeSupport.fireCollectionChanged(label, CollectionEvent.Type.ADD);
  }

  public void deleteLabel(Label label) {
    deselectItem(label);
    int index = this.labels.indexOf(label);
    if (index != -1) {
      label.setLevel(null);
      this.labels = new ArrayList<Label>(this.labels);
      this.labels.remove(index);
      this.labelsChangeSupport.fireCollectionChanged(label, CollectionEvent.Type.DELETE);
    }
  }

  public List<Selectable> getSelectableViewableItems() {
    List<Selectable> homeItems = new ArrayList<Selectable>();
    addViewableItems(this.walls, homeItems);
    addViewableItems(this.rooms, homeItems);
    addViewableItems(this.dimensionLines, homeItems);
    addViewableItems(this.polylines, homeItems);
    addViewableItems(this.labels, homeItems);
    for (HomePieceOfFurniture piece : getFurniture()) {
      if (piece.isVisible()
          && (piece.getLevel() == null
              || piece.getLevel().isViewable())) {
        homeItems.add(piece);
      }
    }
    if (getCompass().isVisible()) {
      homeItems.add(getCompass());
    }
    return homeItems;
  }

  private <T extends Selectable> void addViewableItems(Collection<T> items, 
                                                       List<Selectable> selectableViewableItems) {
    for (T item : items) {
      if (item instanceof Elevatable) {
        Elevatable elevatableItem = (Elevatable)item;
        if (elevatableItem.getLevel() == null
            || elevatableItem.getLevel().isViewable()) {
          selectableViewableItems.add(item);
        }
      }
    }
  }

  public boolean isEmpty() {
    return this.furniture.isEmpty()
        && this.walls.isEmpty()
        && this.rooms.isEmpty()
        && this.dimensionLines.isEmpty()
        && this.polylines.isEmpty()
        && this.labels.isEmpty();
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  public float getWallHeight() {
    return this.wallHeight;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    if (name != this.name
        && (name == null || !name.equals(this.name))) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  public boolean isModified() {
    return this.modified;
  }

  public void setModified(boolean modified) {
    if (modified != this.modified) {
      this.modified = modified;
      this.propertyChangeSupport.firePropertyChange(
          Property.MODIFIED.name(), !modified, modified);
    }
  }

  public boolean isRecovered() {
    return this.recovered;
  }

  public void setRecovered(boolean recovered) {
    if (recovered != this.recovered) {
      this.recovered = recovered;
      this.propertyChangeSupport.firePropertyChange(
          Property.RECOVERED.name(), !recovered, recovered);
    }
  }

  public boolean isRepaired() {
    return this.repaired;
  }

  public void setRepaired(boolean repaired) {
    if (repaired != this.repaired) {
      this.repaired = repaired;
      this.propertyChangeSupport.firePropertyChange(
          Property.REPAIRED.name(), !repaired, repaired);
    }
  }

  public HomePieceOfFurniture.SortableProperty getFurnitureSortedProperty() {
    return this.furnitureSortedProperty;
  }

  public void setFurnitureSortedProperty(HomePieceOfFurniture.SortableProperty furnitureSortedProperty) {
    if (furnitureSortedProperty != this.furnitureSortedProperty
        && (furnitureSortedProperty == null || !furnitureSortedProperty.equals(this.furnitureSortedProperty))) {
      HomePieceOfFurniture.SortableProperty oldFurnitureSortedProperty = this.furnitureSortedProperty;
      this.furnitureSortedProperty = furnitureSortedProperty;
      this.propertyChangeSupport.firePropertyChange(
          Property.FURNITURE_SORTED_PROPERTY.name(), 
          oldFurnitureSortedProperty, furnitureSortedProperty);
    }
  }

  public boolean isFurnitureDescendingSorted() {
    return this.furnitureDescendingSorted;
  }
  
  public void setFurnitureDescendingSorted(boolean furnitureDescendingSorted) {
    if (furnitureDescendingSorted != this.furnitureDescendingSorted) {
      this.furnitureDescendingSorted = furnitureDescendingSorted;
      this.propertyChangeSupport.firePropertyChange(
          Property.FURNITURE_DESCENDING_SORTED.name(), 
          !furnitureDescendingSorted, furnitureDescendingSorted);
    }
  }

  public List<HomePieceOfFurniture.SortableProperty> getFurnitureVisibleProperties() {
    if (this.furnitureVisibleProperties == null) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(this.furnitureVisibleProperties);
    }
  }

  public void setFurnitureVisibleProperties(List<HomePieceOfFurniture.SortableProperty> furnitureVisibleProperties) {
    if (furnitureVisibleProperties != this.furnitureVisibleProperties
        && (furnitureVisibleProperties == null || !furnitureVisibleProperties.equals(this.furnitureVisibleProperties))) {
      List<HomePieceOfFurniture.SortableProperty> oldFurnitureVisibleProperties = this.furnitureVisibleProperties;
      this.furnitureVisibleProperties = new ArrayList<HomePieceOfFurniture.SortableProperty>(furnitureVisibleProperties);
      this.propertyChangeSupport.firePropertyChange(
          Property.FURNITURE_VISIBLE_PROPERTIES.name(), 
          Collections.unmodifiableList(oldFurnitureVisibleProperties), 
          Collections.unmodifiableList(furnitureVisibleProperties));
    }
  }

  public BackgroundImage getBackgroundImage() {
    return this.backgroundImage;
  }

  public void setBackgroundImage(BackgroundImage backgroundImage) {
    if (backgroundImage != this.backgroundImage) {
      BackgroundImage oldBackgroundImage = this.backgroundImage;
      this.backgroundImage = backgroundImage;
      this.propertyChangeSupport.firePropertyChange(
          Property.BACKGROUND_IMAGE.name(), oldBackgroundImage, backgroundImage);
    }
  }

  public Camera getTopCamera() {
    return this.topCamera;
  }

  public ObserverCamera getObserverCamera() {
    return this.observerCamera;
  }

  public void setCamera(Camera camera) {
    if (camera != this.camera) {
      Camera oldCamera = this.camera;
      this.camera = camera;
      this.propertyChangeSupport.firePropertyChange(
          Property.CAMERA.name(), oldCamera, camera);
    }
  }

  public Camera getCamera() {
    if (this.camera == null) {
      this.camera = getTopCamera();
    }
    return this.camera;
  }

  public void setStoredCameras(List<Camera> storedCameras) {
    if (!this.storedCameras.equals(storedCameras)) {
      List<Camera> oldStoredCameras = this.storedCameras;
      if (storedCameras == null) {
        this.storedCameras = Collections.emptyList();
      } else {
        this.storedCameras = new ArrayList<Camera>(storedCameras);
      }
      this.propertyChangeSupport.firePropertyChange(
          Property.STORED_CAMERAS.name(), Collections.unmodifiableList(oldStoredCameras), Collections.unmodifiableList(storedCameras));
    }
  }

  public List<Camera> getStoredCameras() {
    return Collections.unmodifiableList(this.storedCameras);
  }

  public HomeEnvironment getEnvironment() {
    return this.environment;
  }

  public Compass getCompass() {
    return this.compass;
  }

  public HomePrint getPrint() {
    return this.print;
  }

  public void setPrint(HomePrint print) {
    if (print != this.print) {
      HomePrint oldPrint = this.print;
      this.print = print;
      this.propertyChangeSupport.firePropertyChange(Property.PRINT.name(), oldPrint, print);
    }
    this.print = print;
  }
  
  public Object getVisualProperty(String name) {
    return this.visualProperties.get(name);
  }
  
  public void setVisualProperty(String name, Object value) {
    this.visualProperties.put(name, value);
  }

  public String getProperty(String name) {
    return this.properties.get(name);
  }

  public Number getNumericProperty(String name) {
    String value = this.properties.get(name);
    if (value != null) {
      try {
        return new Long (value);
      } catch (NumberFormatException ex) {
        try {
          return new Double (value);
        } catch (NumberFormatException ex1) {
        }
      }
    }
    return null;
  }

  public void setProperty(String name, String value) {
    if (value == null) {
      if (this.properties.containsKey(name)) {
        this.properties.remove(name);
      }
    } else {
      this.properties.put(name, value);
    }
  }

  public Collection<String> getPropertyNames() {
    return this.properties.keySet();
  }
  
  public boolean isBasePlanLocked() {
    return this.basePlanLocked;
  }

  public void setBasePlanLocked(boolean basePlanLocked) {
    if (basePlanLocked != this.basePlanLocked) {
      this.basePlanLocked = basePlanLocked;
      this.propertyChangeSupport.firePropertyChange(
          Property.BASE_PLAN_LOCKED.name(), !basePlanLocked, basePlanLocked);
    }
  }

  public long getVersion() {
    return this.version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public Home clone() {
    try {
      Home clone = (Home)super.clone();
      copyHomeData(this, clone);
      initListenersSupport(clone);
      clone.addModelListeners();
      return clone;
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException("Super class isn't cloneable"); 
    }
  }

  private static void copyHomeData(Home source, Home destination) {
    destination.allLevelsSelection = source.allLevelsSelection;
    destination.name = source.name;
    destination.modified = source.modified;
    destination.recovered = source.recovered;
    destination.repaired = source.repaired;
    destination.backgroundImage = source.backgroundImage;
    destination.print = source.print;
    destination.furnitureDescendingSorted = source.furnitureDescendingSorted;
    destination.version = source.version;
    destination.basePlanLocked = source.basePlanLocked;
    destination.skyColor = source.skyColor;
    destination.groundColor = source.groundColor;
    destination.lightColor = source.lightColor;
    destination.wallsAlpha = source.wallsAlpha;
    destination.furnitureSortedProperty = source.furnitureSortedProperty;
    
    destination.selectedItems = new ArrayList<Selectable>(source.selectedItems.size());
    destination.furniture = cloneSelectableItems(
        source.furniture, source.selectedItems, destination.selectedItems);
    for (int i = 0; i < source.furniture.size(); i++) {
      HomePieceOfFurniture piece = source.furniture.get(i);
      if (piece instanceof HomeDoorOrWindow
          && ((HomeDoorOrWindow)piece).isBoundToWall()) {
        ((HomeDoorOrWindow)destination.furniture.get(i)).setBoundToWall(true);
      }
    }
    destination.rooms = cloneSelectableItems(source.rooms, source.selectedItems, destination.selectedItems);
    destination.dimensionLines = cloneSelectableItems(
        source.dimensionLines, source.selectedItems, destination.selectedItems);
    destination.polylines = cloneSelectableItems(
        source.polylines, source.selectedItems, destination.selectedItems);
    destination.labels = cloneSelectableItems(source.labels, source.selectedItems, destination.selectedItems);
    
    destination.walls = Wall.clone(source.walls);
    for (int i = 0; i < source.walls.size(); i++) {
      Wall wall = source.walls.get(i);
      if (source.selectedItems.contains(wall)) {
        destination.selectedItems.add(destination.walls.get(i));
      }
    }
    destination.levels = new ArrayList<Level>();
    if (source.levels.size() > 0) {
      for (Level level : source.levels) {
        destination.levels.add(level.clone());
      }
      for (int i = 0; i < source.furniture.size(); i++) {
        Level pieceLevel = source.furniture.get(i).getLevel();
        if (pieceLevel != null) {
          destination.furniture.get(i).setLevel(destination.levels.get(source.levels.indexOf(pieceLevel)));
        }
      }
      for (int i = 0; i < source.rooms.size(); i++) {
        Level roomLevel = source.rooms.get(i).getLevel();
        if (roomLevel != null) {
          destination.rooms.get(i).setLevel(destination.levels.get(source.levels.indexOf(roomLevel)));
        }
      }
      for (int i = 0; i < source.dimensionLines.size(); i++) {
        Level dimensionLineLevel = source.dimensionLines.get(i).getLevel();
        if (dimensionLineLevel != null) {
          destination.dimensionLines.get(i).setLevel(destination.levels.get(source.levels.indexOf(dimensionLineLevel)));
        }
      }
      for (int i = 0; i < source.polylines.size(); i++) {
        Level polylineLevel = source.polylines.get(i).getLevel();
        if (polylineLevel != null) {
          destination.polylines.get(i).setLevel(destination.levels.get(source.levels.indexOf(polylineLevel)));
        }
      }
      for (int i = 0; i < source.labels.size(); i++) {
        Level labelLevel = source.labels.get(i).getLevel();
        if (labelLevel != null) {
          destination.labels.get(i).setLevel(destination.levels.get(source.levels.indexOf(labelLevel)));
        }
      }
      for (int i = 0; i < source.walls.size(); i++) {
        Level wallLevel = source.walls.get(i).getLevel();
        if (wallLevel != null) {
          destination.walls.get(i).setLevel(destination.levels.get(source.levels.indexOf(wallLevel)));
        }
      }
      if (source.selectedLevel != null) {
        destination.selectedLevel = destination.levels.get(source.levels.indexOf(source.selectedLevel));
      }
    }
    destination.observerCamera = source.observerCamera.clone();
    destination.topCamera = source.topCamera.clone();
    if (source.camera == source.observerCamera) {
      destination.camera = destination.observerCamera;
      if (source.selectedItems.contains(source.observerCamera)) {
        destination.selectedItems.add(destination.observerCamera);
      }
    } else {
      destination.camera = destination.topCamera;
    }
    destination.storedCameras = new ArrayList<Camera>(source.storedCameras.size());
    for (Camera camera : source.storedCameras) {
      destination.storedCameras.add(camera.clone());
    }
    destination.environment = source.environment.clone();
    destination.compass = source.compass.clone();
    destination.furnitureVisibleProperties = new ArrayList<HomePieceOfFurniture.SortableProperty>(
        source.furnitureVisibleProperties);
    destination.visualProperties = new HashMap<String, Object>(source.visualProperties);
    destination.properties = new HashMap<String, String>(source.properties);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Selectable> List<T> cloneSelectableItems(List<T> source,
                                                                     List<Selectable> sourceSelectedItems,
                                                                     List<Selectable> destinationSelectedItems) {
    List<T> destination = new ArrayList<T>(source.size());
    for (T item : source) {
      T clone = (T)item.clone();
      destination.add(clone);
      if (sourceSelectedItems.contains(item)) {
        destinationSelectedItems.add(clone);
      } else if (item instanceof HomeFurnitureGroup) {
        List<HomePieceOfFurniture> sourceFurnitureGroup = ((HomeFurnitureGroup)item).getAllFurniture();
        List<HomePieceOfFurniture> destinationFurnitureGroup = null;
        for (int i = 0, n = sourceFurnitureGroup.size(); i < n; i++) {
          HomePieceOfFurniture piece = sourceFurnitureGroup.get(i);
          if (sourceSelectedItems.contains(piece)) {
            if (destinationFurnitureGroup == null) {
              destinationFurnitureGroup = ((HomeFurnitureGroup)clone).getAllFurniture();
            }
            destinationSelectedItems.add(destinationFurnitureGroup.get(i));
          }
        }
      }
    }
    return destination;
  }

  public static List<Selectable> duplicate(List<? extends Selectable> items) {
    List<Selectable> list = new ArrayList<Selectable>();
    for (Selectable item : items) {
      if (!(item instanceof Wall)        
          && !(item instanceof Camera)    
          && !(item instanceof Compass)) { 
        list.add(item.clone());
      }
    }
    list.addAll(Wall.clone(getWallsSubList(items)));
    return list;
  }

  public static List<HomePieceOfFurniture> getFurnitureSubList(List<? extends Selectable> items) {
    return getSubList(items, HomePieceOfFurniture.class);
  }

  public static List<Wall> getWallsSubList(List<? extends Selectable> items) {
    return getSubList(items, Wall.class);
  }

  public static List<Room> getRoomsSubList(List<? extends Selectable> items) {
    return getSubList(items, Room.class);
  }

  public static List<Polyline> getPolylinesSubList(List<? extends Selectable> items) {
    return getSubList(items, Polyline.class);
  }
  

  public static List<DimensionLine> getDimensionLinesSubList(List<? extends Selectable> items) {
    return getSubList(items, DimensionLine.class);
  }

  public static List<Label> getLabelsSubList(List<? extends Selectable> items) {
    return getSubList(items, Label.class);
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> getSubList(List<? extends Selectable> items, 
                                       Class<T> subListClass) {
    List<T> subList = new ArrayList<T>();
    for (Selectable item : items) {
      if (subListClass.isInstance(item)) {
        subList.add((T)item);
      }
    }
    return subList;
  }
}