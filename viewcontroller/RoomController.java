package com.eteks.homeview3d.viewcontroller;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Baseboard;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Room;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.model.Wall;

public class RoomController implements Controller {
  public enum Property {NAME, AREA_VISIBLE, FLOOR_VISIBLE, FLOOR_COLOR, FLOOR_PAINT, FLOOR_SHININESS,
      CEILING_VISIBLE, CEILING_COLOR, CEILING_PAINT, CEILING_SHININESS,
      SPLIT_SURROUNDING_WALLS, WALL_SIDES_COLOR, WALL_SIDES_PAINT, WALL_SIDES_SHININESS, WALL_SIDES_BASEBOARD}
  
  public enum RoomPaint {DEFAULT, COLORED, TEXTURED} 

  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;
  private final UndoableEditSupport   undoSupport;
  private TextureChoiceController     floorTextureController;
  private TextureChoiceController     ceilingTextureController;
  private TextureChoiceController     wallSidesTextureController;
  private BaseboardChoiceController   wallSidesBaseboardController;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  roomView;

  private String    name;
  private Boolean   areaVisible;
  private Boolean   floorVisible;
  private Integer   floorColor;
  private RoomPaint floorPaint;
  private Float     floorShininess;
  private Boolean   ceilingVisible;
  private Integer   ceilingColor;
  private RoomPaint ceilingPaint;
  private Float     ceilingShininess;
  private boolean   wallSidesEditable;
  private boolean   splitSurroundingWalls;
  private boolean   splitSurroundingWallsNeeded;
  private Integer   wallSidesColor;
  private RoomPaint wallSidesPaint;
  private Float     wallSidesShininess;

  public RoomController(final Home home, 
                        UserPreferences preferences,
                        ViewFactory viewFactory, 
                        ContentManager contentManager, 
                        UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.undoSupport = undoSupport;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }

  public TextureChoiceController getFloorTextureController() {
    if (this.floorTextureController == null) {
      this.floorTextureController = new TextureChoiceController(
          this.preferences.getLocalizedString(RoomController.class, "floorTextureTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
      this.floorTextureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setFloorPaint(RoomPaint.TEXTURED);
            }
          });
    }
    return this.floorTextureController;
  }

  public TextureChoiceController getCeilingTextureController() {
    if (this.ceilingTextureController == null) {
      this.ceilingTextureController = new TextureChoiceController(
          this.preferences.getLocalizedString(RoomController.class, "ceilingTextureTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
      this.ceilingTextureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setCeilingPaint(RoomPaint.TEXTURED);
            }
          });
    }
    return this.ceilingTextureController;
  }

  public TextureChoiceController getWallSidesTextureController() {
    if (this.wallSidesTextureController == null) {
      this.wallSidesTextureController = new TextureChoiceController(
          this.preferences.getLocalizedString(RoomController.class, "wallSidesTextureTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
      this.wallSidesTextureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setWallSidesPaint(RoomPaint.TEXTURED);
            }
          });
    }
    return this.wallSidesTextureController;
  }

  public BaseboardChoiceController getWallSidesBaseboardController() {
    if (this.wallSidesBaseboardController == null) {
      this.wallSidesBaseboardController = new BaseboardChoiceController(
          this.preferences, this.viewFactory, this.contentManager);
    }
    return this.wallSidesBaseboardController;
  }

  public DialogView getView() {
    if (this.roomView == null) {
      this.roomView = this.viewFactory.createRoomView(this.preferences, this); 
    }
    return this.roomView;
  }

  public void displayView(View parentView) {
    getView().displayView(parentView);
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  public boolean isPropertyEditable(Property property) {
    switch (property) {
      case SPLIT_SURROUNDING_WALLS :
      case WALL_SIDES_COLOR :
      case WALL_SIDES_PAINT :
      case WALL_SIDES_SHININESS :
      case WALL_SIDES_BASEBOARD :
        return this.wallSidesEditable;
      default :
        return true;
    }
  }
 
  protected void updateProperties() {
    List<Room> selectedRooms = Home.getRoomsSubList(this.home.getSelectedItems());
    if (selectedRooms.isEmpty()) {
      setAreaVisible(null); // Nothing to edit
      setFloorColor(null);
      getFloorTextureController().setTexture(null);
      setFloorPaint(null);
      setFloorShininess(null);
      setCeilingColor(null);
      getCeilingTextureController().setTexture(null);
      setCeilingPaint(null);
      setCeilingShininess(null);
    } else {
      Room firstRoom = selectedRooms.get(0);

      String name = firstRoom.getName();
      if (name != null) {
        for (int i = 1; i < selectedRooms.size(); i++) {
          if (!name.equals(selectedRooms.get(i).getName())) {
            name = null;
            break;
          }
        }
      }
      setName(name);
      
      Boolean areaVisible = firstRoom.isAreaVisible();
      for (int i = 1; i < selectedRooms.size(); i++) {
        if (areaVisible != selectedRooms.get(i).isAreaVisible()) {
          areaVisible = null;
          break;
        }
      }
      setAreaVisible(areaVisible);      
      
      Boolean floorVisible = firstRoom.isFloorVisible();
      for (int i = 1; i < selectedRooms.size(); i++) {
        if (floorVisible != selectedRooms.get(i).isFloorVisible()) {
          floorVisible = null;
          break;
        }
      }
      setFloorVisible(floorVisible);      
      
      Integer floorColor = firstRoom.getFloorColor();
      if (floorColor != null) {
        for (int i = 1; i < selectedRooms.size(); i++) {
          if (!floorColor.equals(selectedRooms.get(i).getFloorColor())) {
            floorColor = null;
            break;
          }
        }
      }
      setFloorColor(floorColor);
      
      HomeTexture floorTexture = firstRoom.getFloorTexture();
      if (floorTexture != null) {
        for (int i = 1; i < selectedRooms.size(); i++) {
          if (!floorTexture.equals(selectedRooms.get(i).getFloorTexture())) {
            floorTexture = null;
            break;
          }
        }
      }
      getFloorTextureController().setTexture(floorTexture);
      
      boolean defaultColorsAndTextures = true;
      for (int i = 0; i < selectedRooms.size(); i++) {
        Room room = selectedRooms.get(i);
        if (room.getFloorColor() != null
            || room.getFloorTexture() != null) {
          defaultColorsAndTextures = false;
          break;
        }
      }
      
      if (floorColor != null) {
        setFloorPaint(RoomPaint.COLORED);
      } else if (floorTexture != null) {
        setFloorPaint(RoomPaint.TEXTURED);
      } else if (defaultColorsAndTextures) {
        setFloorPaint(RoomPaint.DEFAULT);
      } else {
        setFloorPaint(null);
      }
      Float floorShininess = firstRoom.getFloorShininess();
      for (int i = 1; i < selectedRooms.size(); i++) {
        if (!floorShininess.equals(selectedRooms.get(i).getFloorShininess())) {
          floorShininess = null;
          break;
        }
      }
      setFloorShininess(floorShininess);
      
      Boolean ceilingVisible = firstRoom.isCeilingVisible();
      for (int i = 1; i < selectedRooms.size(); i++) {
        if (ceilingVisible != selectedRooms.get(i).isCeilingVisible()) {
          ceilingVisible = null;
          break;
        }
      }
      setCeilingVisible(ceilingVisible);      
      
      Integer ceilingColor = firstRoom.getCeilingColor();
      if (ceilingColor != null) {
        for (int i = 1; i < selectedRooms.size(); i++) {
          if (!ceilingColor.equals(selectedRooms.get(i).getCeilingColor())) {
            ceilingColor = null;
            break;
          }
        }
      }
      setCeilingColor(ceilingColor);
      
      HomeTexture ceilingTexture = firstRoom.getCeilingTexture();
      if (ceilingTexture != null) {
        for (int i = 1; i < selectedRooms.size(); i++) {
          if (!ceilingTexture.equals(selectedRooms.get(i).getCeilingTexture())) {
            ceilingTexture = null;
            break;
          }
        }
      }
      getCeilingTextureController().setTexture(ceilingTexture);
      
      defaultColorsAndTextures = true;
      for (int i = 0; i < selectedRooms.size(); i++) {
        Room room = selectedRooms.get(i);
        if (room.getCeilingColor() != null
            || room.getCeilingTexture() != null) {
          defaultColorsAndTextures = false;
          break;
        }
      }
      
      if (ceilingColor != null) {
        setCeilingPaint(RoomPaint.COLORED);
      } else if (ceilingTexture != null) {
        setCeilingPaint(RoomPaint.TEXTURED);
      } else if (defaultColorsAndTextures) {
        setCeilingPaint(RoomPaint.DEFAULT);
      } else {
        setCeilingPaint(null);
      }

      Float ceilingShininess = firstRoom.getCeilingShininess();
      for (int i = 1; i < selectedRooms.size(); i++) {
        if (!ceilingShininess.equals(selectedRooms.get(i).getCeilingShininess())) {
          ceilingShininess = null;
          break;
        }
      }
      setCeilingShininess(ceilingShininess);
    }

    List<WallSide> wallSides = getRoomsWallSides(selectedRooms, null);
    if (wallSides.isEmpty()) {
      this.wallSidesEditable =
      this.splitSurroundingWallsNeeded  =
      this.splitSurroundingWalls = false;
      setWallSidesColor(null);
      setWallSidesPaint(null);
      setWallSidesShininess(null);
      getWallSidesBaseboardController().setVisible(null);
      getWallSidesBaseboardController().setThickness(null);
      getWallSidesBaseboardController().setHeight(null);
      getWallSidesBaseboardController().setColor(null);
      getWallSidesBaseboardController().getTextureController().setTexture(null);
      getWallSidesBaseboardController().setPaint(null);
    } else {
      this.wallSidesEditable = true;
      this.splitSurroundingWallsNeeded = splitWalls(wallSides, null, null, null);
      this.splitSurroundingWalls = false;
      WallSide firstWallSide = wallSides.get(0);
      
      Integer wallSidesColor = firstWallSide.getSide() == WallSide.LEFT_SIDE
          ? firstWallSide.getWall().getLeftSideColor()
          : firstWallSide.getWall().getRightSideColor();
      if (wallSidesColor != null) {
        for (int i = 1; i < wallSides.size(); i++) {
          WallSide wallSide = wallSides.get(i);
          if (!wallSidesColor.equals(wallSide.getSide() == WallSide.LEFT_SIDE
                  ? wallSide.getWall().getLeftSideColor()
                  : wallSide.getWall().getRightSideColor())) {
            wallSidesColor = null;
            break;
          }
        }
      }
      setWallSidesColor(wallSidesColor);
      
      HomeTexture wallSidesTexture = firstWallSide.getSide() == WallSide.LEFT_SIDE
          ? firstWallSide.getWall().getLeftSideTexture()
          : firstWallSide.getWall().getRightSideTexture();
      if (wallSidesTexture != null) {
        for (int i = 1; i < wallSides.size(); i++) {
          WallSide wallSide = wallSides.get(i);
          if (!wallSidesTexture.equals(wallSide.getSide() == WallSide.LEFT_SIDE
                  ? wallSide.getWall().getLeftSideTexture()
                  : wallSide.getWall().getRightSideTexture())) {
            wallSidesTexture = null;
            break;
          }
        }
      }
      getWallSidesTextureController().setTexture(wallSidesTexture);
      
      boolean defaultColorsAndTextures = true;
      for (int i = 1; i < wallSides.size(); i++) {
        WallSide wallSide = wallSides.get(i);
        if ((wallSide.getSide() == WallSide.LEFT_SIDE
                ? wallSide.getWall().getLeftSideColor()
                : wallSide.getWall().getRightSideColor()) != null
            || (wallSide.getSide() == WallSide.LEFT_SIDE
                    ? wallSide.getWall().getLeftSideTexture()
                    : wallSide.getWall().getRightSideTexture()) != null) {
          defaultColorsAndTextures = false;
          break;
        }
      }
      
      if (wallSidesColor != null) {
        setWallSidesPaint(RoomPaint.COLORED);
      } else if (wallSidesTexture != null) {
        setWallSidesPaint(RoomPaint.TEXTURED);
      } else if (defaultColorsAndTextures) {
        setWallSidesPaint(RoomPaint.DEFAULT);
      } else {
        setWallSidesPaint(null);
      }
      
      Float wallSidesShininess = firstWallSide.getSide() == WallSide.LEFT_SIDE
          ? firstWallSide.getWall().getLeftSideShininess()
          : firstWallSide.getWall().getRightSideShininess();
      if (wallSidesShininess != null) {
        for (int i = 1; i < wallSides.size(); i++) {
          WallSide wallSide = wallSides.get(i);
          if (!wallSidesShininess.equals(wallSide.getSide() == WallSide.LEFT_SIDE
                  ? wallSide.getWall().getLeftSideShininess()
                  : wallSide.getWall().getRightSideShininess())) {
            wallSidesShininess = null;
            break;
          }
        }
      }
      setWallSidesShininess(wallSidesShininess);

      Baseboard firstWallSideBaseboard = firstWallSide.getSide() == WallSide.LEFT_SIDE
          ? firstWallSide.getWall().getLeftSideBaseboard()
          : firstWallSide.getWall().getRightSideBaseboard();
      Boolean wallSidesBaseboardVisible = firstWallSideBaseboard != null;
      for (int i = 1; i < wallSides.size(); i++) {
        WallSide wallSide = wallSides.get(i);
        if (wallSidesBaseboardVisible 
            != (wallSide.getSide() == WallSide.LEFT_SIDE
                  ? wallSide.getWall().getLeftSideBaseboard() != null
                  : wallSide.getWall().getRightSideBaseboard() != null)) {
          wallSidesBaseboardVisible = null;
          break;
        }
      }
      getWallSidesBaseboardController().setVisible(wallSidesBaseboardVisible);

      Float wallSidesBaseboardThickness = firstWallSideBaseboard != null
          ? firstWallSideBaseboard.getThickness()
          : this.preferences.getNewWallBaseboardThickness();
      for (int i = 1; i < wallSides.size(); i++) {
        WallSide wallSide = wallSides.get(i);
        Baseboard baseboard = wallSide.getSide() == WallSide.LEFT_SIDE
            ? wallSide.getWall().getLeftSideBaseboard()
            : wallSide.getWall().getRightSideBaseboard();
        if (!wallSidesBaseboardThickness.equals(baseboard != null
                ? baseboard.getThickness()
                : this.preferences.getNewWallBaseboardThickness())) {
          wallSidesBaseboardThickness = null;
          break;
        }
      }
      getWallSidesBaseboardController().setThickness(wallSidesBaseboardThickness);
      
      Float wallSidesBaseboardHeight = firstWallSideBaseboard != null
          ? firstWallSideBaseboard.getHeight()
          : this.preferences.getNewWallBaseboardHeight();
      for (int i = 1; i < wallSides.size(); i++) {
        WallSide wallSide = wallSides.get(i);
        Baseboard baseboard = wallSide.getSide() == WallSide.LEFT_SIDE
            ? wallSide.getWall().getLeftSideBaseboard()
            : wallSide.getWall().getRightSideBaseboard();
        if (!wallSidesBaseboardHeight.equals(baseboard != null
                ? baseboard.getHeight()
                : this.preferences.getNewWallBaseboardHeight())) {
          wallSidesBaseboardHeight = null;
          break;
        }
      }
      getWallSidesBaseboardController().setHeight(wallSidesBaseboardHeight);

      float maxBaseboardHeight = firstWallSide.getWall().isTrapezoidal()
          ? Math.max(firstWallSide.getWall().getHeight(), firstWallSide.getWall().getHeightAtEnd())
          : firstWallSide.getWall().getHeight();
      for (int i = 1; i < wallSides.size(); i++) {
        Wall wall = wallSides.get(i).getWall();
        maxBaseboardHeight = Math.max(maxBaseboardHeight, 
            wall.isTrapezoidal()
                ? Math.max(wall.getHeight(), wall.getHeightAtEnd())
                : wall.getHeight());
      }
      getWallSidesBaseboardController().setMaxHeight(maxBaseboardHeight);

      Integer wallSidesBaseboardColor = firstWallSideBaseboard != null
          ? firstWallSideBaseboard.getColor()
          : null;
      if (wallSidesBaseboardColor != null) {
        for (int i = 1; i < wallSides.size(); i++) {
          WallSide wallSide = wallSides.get(i);
          Baseboard baseboard = wallSide.getSide() == WallSide.LEFT_SIDE
              ? wallSide.getWall().getLeftSideBaseboard()
              : wallSide.getWall().getRightSideBaseboard();
          if (baseboard == null
              || !wallSidesBaseboardColor.equals(baseboard.getColor())) {
            wallSidesBaseboardColor = null;
            break;
          }
        }
      }
      getWallSidesBaseboardController().setColor(wallSidesBaseboardColor);
      
      HomeTexture wallSidesBaseboardTexture = firstWallSideBaseboard != null
          ? firstWallSideBaseboard.getTexture()
          : null;
      if (wallSidesBaseboardTexture != null) {
        for (int i = 1; i < wallSides.size(); i++) {
          WallSide wallSide = wallSides.get(i);
          Baseboard baseboard = wallSide.getSide() == WallSide.LEFT_SIDE
              ? wallSide.getWall().getLeftSideBaseboard()
              : wallSide.getWall().getRightSideBaseboard();
          if (baseboard == null
              || !wallSidesBaseboardTexture.equals(baseboard.getTexture())) {
            wallSidesBaseboardTexture = null;
            break;
          }
        }
      } 
      getWallSidesBaseboardController().getTextureController().setTexture(wallSidesBaseboardTexture);
      
      defaultColorsAndTextures = true;
      for (int i = 0; i < wallSides.size(); i++) {
        WallSide wallSide = wallSides.get(i);
        Baseboard baseboard = wallSide.getSide() == WallSide.LEFT_SIDE
            ? wallSide.getWall().getLeftSideBaseboard()
            : wallSide.getWall().getRightSideBaseboard();
        if (baseboard != null
            && (baseboard.getColor() != null
                || baseboard.getTexture() != null)) {
          defaultColorsAndTextures = false;
          break;
        }
      }
      
      if (wallSidesBaseboardColor != null) {
        getWallSidesBaseboardController().setPaint(BaseboardChoiceController.BaseboardPaint.COLORED);
      } else if (wallSidesBaseboardTexture != null) {
        getWallSidesBaseboardController().setPaint(BaseboardChoiceController.BaseboardPaint.TEXTURED);
      } else if (defaultColorsAndTextures) {
        getWallSidesBaseboardController().setPaint(BaseboardChoiceController.BaseboardPaint.DEFAULT);
      } else {
        getWallSidesBaseboardController().setPaint(null);
      }      
    }      
  }
  
  private List<WallSide> getRoomsWallSides(List<Room> rooms, List<WallSide> defaultWallSides) {
    List<WallSide> wallSides = new ArrayList<WallSide>();
    for (Room room : rooms) {
      Area roomArea = new Area(getPath(room.getPoints(), true));
      if (defaultWallSides != null) {
        for (WallSide wallSide : defaultWallSides) {
          if (isRoomItersectingWallSide(wallSide.getWall().getPoints(), wallSide.getSide(), roomArea)) {
            wallSides.add(wallSide);
          }
        }
      } else {
        for (Wall wall : this.home.getWalls()) {
          if ((wall.getLevel() == null || wall.getLevel().isViewable())
              && wall.isAtLevel(this.home.getSelectedLevel())) {
            float [][] wallPoints = wall.getPoints();
            if (isRoomItersectingWallSide(wallPoints, WallSide.LEFT_SIDE, roomArea)) {
              wallSides.add(new WallSide(wall, WallSide.LEFT_SIDE));
            }
            if (isRoomItersectingWallSide(wallPoints, WallSide.RIGHT_SIDE, roomArea)) {
              wallSides.add(new WallSide(wall, WallSide.RIGHT_SIDE));
            }
          }
        }
      }
    }
    return wallSides;
  }

  private boolean isRoomItersectingWallSide(float [][] wallPoints, int wallSide, Area roomArea) {
    BasicStroke lineStroke = new BasicStroke(2);
    Shape wallSideShape = getWallSideShape(wallPoints, wallSide);
    Area wallSideTestArea = new Area(lineStroke.createStrokedShape(wallSideShape));
    float wallSideTestAreaSurface = getSurface(wallSideTestArea);
    wallSideTestArea.intersect(roomArea);
    if (!wallSideTestArea.isEmpty()) {
      float wallSideIntersectionSurface = getSurface(wallSideTestArea);
      if (wallSideIntersectionSurface > wallSideTestAreaSurface * 0.02f) { 
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the shape of the side of the given <code>wall</code>. 
   */
  private Shape getWallSideShape(float [][] wallPoints, int wallSide) {
    if (wallPoints.length == 4) {
      if (wallSide == WallSide.LEFT_SIDE) {
        return new Line2D.Float(wallPoints [0][0], wallPoints [0][1], wallPoints [1][0], wallPoints [1][1]);
      } else {
        return new Line2D.Float(wallPoints [2][0], wallPoints [2][1], wallPoints [3][0], wallPoints [3][1]);
      }
    } else {
      float [][] wallSidePoints = new float [wallPoints.length / 2][];
      System.arraycopy(wallPoints, wallSide == WallSide.LEFT_SIDE ? 0 : wallSidePoints.length, 
          wallSidePoints, 0, wallSidePoints.length);
      return getPath(wallSidePoints, false);
    }
  }
  
  /**
   * Returns the shape matching the coordinates in <code>points</code> array.
   */
  private GeneralPath getPath(float [][] points, boolean closedPath) {
    GeneralPath path = new GeneralPath();
    path.moveTo(points [0][0], points [0][1]);
    for (int i = 1; i < points.length; i++) {
      path.lineTo(points [i][0], points [i][1]);
    }
    if (closedPath) {
      path.closePath();
    }
    return path;
  }

  private float getSurface(Area area) {
    float surface = 0;
    List<float []> currentPathPoints = new ArrayList<float[]>();
    for (PathIterator it = area.getPathIterator(null); !it.isDone(); ) {
      float [] roomPoint = new float[2];
      switch (it.currentSegment(roomPoint)) {
        case PathIterator.SEG_MOVETO : 
          currentPathPoints.add(roomPoint);
          break;
        case PathIterator.SEG_LINETO : 
          currentPathPoints.add(roomPoint);
          break;
        case PathIterator.SEG_CLOSE :
          float [][] pathPoints = 
              currentPathPoints.toArray(new float [currentPathPoints.size()][]);
          surface += Math.abs(getSignedSurface(pathPoints));
          currentPathPoints.clear();
          break;
      }
      it.next();        
    }
    return surface;
  }
  
  private float getSignedSurface(float areaPoints [][]) {
    float area = 0;
    for (int i = 1; i < areaPoints.length; i++) {
      area += areaPoints [i][0] * areaPoints [i - 1][1];
      area -= areaPoints [i][1] * areaPoints [i - 1][0];
    }
    area += areaPoints [0][0] * areaPoints [areaPoints.length - 1][1];
    area -= areaPoints [0][1] * areaPoints [areaPoints.length - 1][0];
    return area / 2;
  }

  public void setName(String name) {
    if (name != this.name) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  public String getName() {
    return this.name;
  }
  
  public void setAreaVisible(Boolean areaVisible) {
    if (areaVisible != this.areaVisible) {
      Boolean oldAreaVisible = this.areaVisible;
      this.areaVisible = areaVisible;
      this.propertyChangeSupport.firePropertyChange(Property.AREA_VISIBLE.name(), oldAreaVisible, areaVisible);
    }
  }

  public Boolean getAreaVisible() {
    return this.areaVisible;
  }

  public void setFloorVisible(Boolean floorVisible) {
    if (floorVisible != this.floorVisible) {
      Boolean oldFloorVisible = this.floorVisible;
      this.floorVisible = floorVisible;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_VISIBLE.name(), oldFloorVisible, floorVisible);
    }
  }

  public Boolean getFloorVisible() {
    return this.floorVisible;
  }

  public void setFloorColor(Integer floorColor) {
    if (floorColor != this.floorColor) {
      Integer oldFloorColor = this.floorColor;
      this.floorColor = floorColor;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_COLOR.name(), oldFloorColor, floorColor);
      
      setFloorPaint(RoomPaint.COLORED);
    }
  }
  
  public Integer getFloorColor() {
    return this.floorColor;
  }

  public void setFloorPaint(RoomPaint floorPaint) {
    if (floorPaint != this.floorPaint) {
      RoomPaint oldFloorPaint = this.floorPaint;
      this.floorPaint = floorPaint;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_PAINT.name(), oldFloorPaint, floorPaint);
    }
  }
  
  public RoomPaint getFloorPaint() {
    return this.floorPaint;
  }

  public void setFloorShininess(Float floorShininess) {
    if (floorShininess != this.floorShininess) {
      Float oldFloorShininess = this.floorShininess;
      this.floorShininess = floorShininess;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_SHININESS.name(), oldFloorShininess, floorShininess);
    }
  }
  
  public Float getFloorShininess() {
    return this.floorShininess;
  }

  public void setCeilingVisible(Boolean ceilingCeilingVisible) {
    if (ceilingCeilingVisible != this.ceilingVisible) {
      Boolean oldCeilingVisible = this.ceilingVisible;
      this.ceilingVisible = ceilingCeilingVisible;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_VISIBLE.name(), oldCeilingVisible, ceilingCeilingVisible);
    }
  }

  public Boolean getCeilingVisible() {
    return this.ceilingVisible;
  }

  public void setCeilingColor(Integer ceilingColor) {
    if (ceilingColor != this.ceilingColor) {
      Integer oldCeilingColor = this.ceilingColor;
      this.ceilingColor = ceilingColor;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_COLOR.name(), oldCeilingColor, ceilingColor);
      
      setCeilingPaint(RoomPaint.COLORED);
    }
  }
  
  public Integer getCeilingColor() {
    return this.ceilingColor;
  }

  public void setCeilingPaint(RoomPaint ceilingPaint) {
    if (ceilingPaint != this.ceilingPaint) {
      RoomPaint oldCeilingPaint = this.ceilingPaint;
      this.ceilingPaint = ceilingPaint;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_PAINT.name(), oldCeilingPaint, ceilingPaint);
    }
  }
  
  public RoomPaint getCeilingPaint() {
    return this.ceilingPaint;
  }

  public void setCeilingShininess(Float ceilingShininess) {
    if (ceilingShininess != this.ceilingShininess) {
      Float oldCeilingShininess = this.ceilingShininess;
      this.ceilingShininess = ceilingShininess;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_SHININESS.name(), oldCeilingShininess, ceilingShininess);
    }
  }
  
  public Float getCeilingShininess() {
    return this.ceilingShininess;
  }

  public boolean isSplitSurroundingWalls() {
    return this.splitSurroundingWalls;
  }
  
  public void setSplitSurroundingWalls(boolean splitSurroundingWalls) {
    if (splitSurroundingWalls != this.splitSurroundingWalls) {
      this.splitSurroundingWalls = splitSurroundingWalls;
      this.propertyChangeSupport.firePropertyChange(Property.SPLIT_SURROUNDING_WALLS.name(), !splitSurroundingWalls, splitSurroundingWalls);
    }
  }
  
  public boolean isSplitSurroundingWallsNeeded() {
    return this.splitSurroundingWallsNeeded;
  }
  
  public void setWallSidesColor(Integer wallSidesColor) {
    if (wallSidesColor != this.wallSidesColor) {
      Integer oldWallSidesColor = this.wallSidesColor;
      this.wallSidesColor = wallSidesColor;
      this.propertyChangeSupport.firePropertyChange(Property.WALL_SIDES_COLOR.name(), oldWallSidesColor, wallSidesColor);
      
      setWallSidesPaint(RoomPaint.COLORED);
    }
  }
  
  public Integer getWallSidesColor() {
    return this.wallSidesColor;
  }

  public void setWallSidesPaint(RoomPaint wallSidesPaint) {
    if (wallSidesPaint != this.wallSidesPaint) {
      RoomPaint oldWallSidesPaint = this.wallSidesPaint;
      this.wallSidesPaint = wallSidesPaint;
      this.propertyChangeSupport.firePropertyChange(Property.WALL_SIDES_PAINT.name(), oldWallSidesPaint, wallSidesPaint);
    }
  }
  
  public RoomPaint getWallSidesPaint() {
    return this.wallSidesPaint;
  }

  public void setWallSidesShininess(Float wallSidesShininess) {
    if (wallSidesShininess != this.wallSidesShininess) {
      Float oldWallSidesShininess = this.wallSidesShininess;
      this.wallSidesShininess = wallSidesShininess;
      this.propertyChangeSupport.firePropertyChange(Property.WALL_SIDES_SHININESS.name(), oldWallSidesShininess, wallSidesShininess);
    }
  }
  
  public Float getWallSidesShininess() {
    return this.wallSidesShininess;
  }

  public void modifyRooms() {
    List<Selectable> oldSelection = this.home.getSelectedItems(); 
    List<Room> selectedRooms = Home.getRoomsSubList(oldSelection);
    if (!selectedRooms.isEmpty()) {
      String name = getName();
      Boolean areaVisible = getAreaVisible();
      Boolean floorVisible = getFloorVisible();
      RoomPaint floorPaint = getFloorPaint();
      Integer floorColor = floorPaint == RoomPaint.COLORED 
          ? getFloorColor() : null;
      HomeTexture floorTexture = floorPaint == RoomPaint.TEXTURED
          ? getFloorTextureController().getTexture() : null;
      Float floorShininess = getFloorShininess();
      Boolean ceilingVisible = getCeilingVisible();
      RoomPaint ceilingPaint = getCeilingPaint();
      Integer ceilingColor = ceilingPaint == RoomPaint.COLORED
          ? getCeilingColor() : null;
      HomeTexture ceilingTexture = ceilingPaint == RoomPaint.TEXTURED
          ? getCeilingTextureController().getTexture() : null;
      Float ceilingShininess = getCeilingShininess();
      Integer wallSidesColor = getWallSidesPaint() == RoomPaint.COLORED 
          ? getWallSidesColor() : null;
      HomeTexture wallSidesTexture = getWallSidesPaint() == RoomPaint.TEXTURED
          ? getWallSidesTextureController().getTexture() : null;
      Float wallSidesShininess = getWallSidesShininess();
      Boolean wallSidesBaseboardVisible = getWallSidesBaseboardController().getVisible();
      Float wallSidesBaseboardThickness = getWallSidesBaseboardController().getThickness();
      Float wallSidesBaseboardHeight = getWallSidesBaseboardController().getHeight();
      BaseboardChoiceController.BaseboardPaint wallSidesBaseboardPaint = getWallSidesBaseboardController().getPaint();
      Integer wallSidesBaseboardColor = wallSidesBaseboardPaint == BaseboardChoiceController.BaseboardPaint.COLORED 
          ? getWallSidesBaseboardController().getColor() : null;
      HomeTexture wallSidesBaseboardTexture = wallSidesBaseboardPaint == BaseboardChoiceController.BaseboardPaint.TEXTURED
          ? getWallSidesBaseboardController().getTextureController().getTexture()
          : null;
      List<WallSide> selectedRoomsWallSides = getRoomsWallSides(selectedRooms, null);
      
      ModifiedRoom [] modifiedRooms = new ModifiedRoom [selectedRooms.size()]; 
      for (int i = 0; i < modifiedRooms.length; i++) {
        modifiedRooms [i] = new ModifiedRoom(selectedRooms.get(i));
      }
      
      List<ModifiedWall> deletedWalls = new ArrayList<ModifiedWall>();
      List<ModifiedWall> addedWalls = new ArrayList<ModifiedWall>();
      List<Selectable> newSelection = new ArrayList<Selectable>(oldSelection);
      if (this.splitSurroundingWalls) {
        if (splitWalls(selectedRoomsWallSides, deletedWalls, addedWalls, newSelection)) {
          this.home.setSelectedItems(newSelection);
          selectedRoomsWallSides = getRoomsWallSides(selectedRooms, selectedRoomsWallSides);
        }
      }
      
      ModifiedWallSide [] modifiedWallSides = new ModifiedWallSide [selectedRoomsWallSides.size()]; 
      for (int i = 0; i < modifiedWallSides.length; i++) {
        modifiedWallSides [i] = new ModifiedWallSide(selectedRoomsWallSides.get(i));
      }
      doModifyRoomsAndWallSides(home, modifiedRooms, name, areaVisible, 
          floorVisible, floorPaint, floorColor, floorTexture, floorShininess, 
          ceilingVisible, ceilingPaint, ceilingColor, ceilingTexture, ceilingShininess,
          modifiedWallSides, this.preferences.getNewWallBaseboardThickness(), this.preferences.getNewWallBaseboardHeight(),
          wallSidesColor, wallSidesTexture, wallSidesShininess, 
          wallSidesBaseboardVisible, wallSidesBaseboardThickness, wallSidesBaseboardHeight, 
          wallSidesBaseboardPaint, wallSidesBaseboardColor, wallSidesBaseboardTexture, null, null);
      if (this.undoSupport != null) {
        UndoableEdit undoableEdit = new RoomsAndWallSidesModificationUndoableEdit(
            this.home, this.preferences, oldSelection, newSelection, modifiedRooms, name, areaVisible, 
            floorVisible, floorPaint, floorColor, floorTexture, floorShininess,
            ceilingVisible, ceilingPaint, ceilingColor, ceilingTexture, ceilingShininess,
            modifiedWallSides, this.preferences.getNewWallBaseboardThickness(), this.preferences.getNewWallBaseboardHeight(),
            wallSidesColor, wallSidesTexture, wallSidesShininess,
            wallSidesBaseboardVisible, wallSidesBaseboardThickness, wallSidesBaseboardHeight, 
            wallSidesBaseboardPaint, wallSidesBaseboardColor, wallSidesBaseboardTexture,
            deletedWalls.toArray(new ModifiedWall [deletedWalls.size()]), 
            addedWalls.toArray(new ModifiedWall [addedWalls.size()]));
        this.undoSupport.postEdit(undoableEdit);
      }
      if (name != null) {
        this.preferences.addAutoCompletionString("RoomName", name);
      }
    }
  }

  private boolean splitWalls(List<WallSide> wallSides, 
                             List<ModifiedWall> deletedWalls,
                             List<ModifiedWall> addedWalls, 
                             List<Selectable> selectedItems) {
    Map<Wall, ModifiedWall> existingWalls = null;
    List<Wall> newWalls = new ArrayList<Wall>();
    WallSide splitWallSide;
    do {
      splitWallSide = null;
      Wall firstWall = null;
      Wall secondWall = null;
      ModifiedWall deletedWall = null;
      for (int i = 0; i < wallSides.size() && splitWallSide == null; i++) {
        WallSide wallSide = wallSides.get(i);
        Wall wall = wallSide.getWall();
        Float arcExtent = wall.getArcExtent();
        if (arcExtent == null || arcExtent.floatValue() == 0) { // Ignore round walls
          Area wallArea = new Area(getPath(wall.getPoints(), true));
          for (WallSide intersectedWallSide : wallSides) {
            Wall intersectedWall = intersectedWallSide.getWall();
            if (wall != intersectedWall) {
              Area intersectedWallArea = new Area(getPath(intersectedWall.getPoints(), true));
              intersectedWallArea.intersect(wallArea);
              if (!intersectedWallArea.isEmpty()
                  && intersectedWallArea.isSingular()) {
                float [] intersection = computeIntersection(
                    wall.getXStart(), wall.getYStart(), wall.getXEnd(), wall.getYEnd(), 
                    intersectedWall.getXStart(), intersectedWall.getYStart(), intersectedWall.getXEnd(), intersectedWall.getYEnd());
                if (intersection != null) {
                  firstWall = wall.clone();
                  secondWall = wall.clone();
                  firstWall.setLevel(wall.getLevel());
                  secondWall.setLevel(wall.getLevel());
                  
                  firstWall.setXEnd(intersection [0]);
                  firstWall.setYEnd(intersection [1]);
                  secondWall.setXStart(intersection [0]);
                  secondWall.setYStart(intersection [1]);
                  
                  if (firstWall.getLength() > intersectedWall.getThickness() / 2
                      && secondWall.getLength() > intersectedWall.getThickness() / 2) {
                    if (deletedWalls == null) { 
                      return true; 
                    }
                    
                    if (existingWalls == null) {
                      existingWalls = new HashMap<Wall, ModifiedWall>(wallSides.size());
                      for (WallSide side : wallSides) {
                        if (!existingWalls.containsKey(side.getWall())) {
                          existingWalls.put(side.getWall(), new ModifiedWall(side.getWall()));
                        }
                      }
                    }
                    
                    deletedWall = existingWalls.get(wall);
                    Wall wallAtStart = wall.getWallAtStart();            
                    if (wallAtStart != null) {
                      firstWall.setWallAtStart(wallAtStart);
                      if (wallAtStart.getWallAtEnd() == wall) {
                        wallAtStart.setWallAtEnd(firstWall);
                      } else {
                        wallAtStart.setWallAtStart(firstWall);
                      }
                    }
                    
                    Wall wallAtEnd = wall.getWallAtEnd();      
                    if (wallAtEnd != null) {
                      secondWall.setWallAtEnd(wallAtEnd);
                      if (wallAtEnd.getWallAtEnd() == wall) {
                        wallAtEnd.setWallAtEnd(secondWall);
                      } else {
                        wallAtEnd.setWallAtStart(secondWall);
                      }
                    }
                    
                    firstWall.setWallAtEnd(secondWall);
                    secondWall.setWallAtStart(firstWall);

                    if (wall.getHeightAtEnd() != null) {
                      Float heightAtIntersecion = wall.getHeight() 
                          + (wall.getHeightAtEnd() - wall.getHeight()) 
                            * (float)Point2D.distance(wall.getXStart(), wall.getYStart(), intersection [0], intersection [1])
                            / wall.getLength();
                      firstWall.setHeightAtEnd(heightAtIntersecion);
                      secondWall.setHeight(heightAtIntersecion);
                    }
                    
                    splitWallSide = wallSide;
                    break;
                  }
                }
              }
            }
          }
        }
      }
      
      if (splitWallSide != null) {    
        newWalls.add(firstWall);
        newWalls.add(secondWall);        
        Wall splitWall = splitWallSide.getWall();
        if (this.home.getWalls().contains(splitWall)) {
          deletedWalls.add(deletedWall);
        } else {
          for (int i = newWalls.size() - 1; i >= 0; i--) {
            if (newWalls.get(i) == splitWall) {
              newWalls.remove(i);
              break;
            }
          }
        }
        if (selectedItems.remove(splitWall)) {
          selectedItems.add(firstWall);
          selectedItems.add(secondWall);
        }
        
        wallSides.remove(splitWallSide);
        wallSides.add(new WallSide(firstWall, splitWallSide.getSide()));
        wallSides.add(new WallSide(secondWall, splitWallSide.getSide()));
        List<WallSide> sameWallSides = new ArrayList<WallSide>(); 
        for (int i = wallSides.size() - 1;  i >= 0; i--) {
          WallSide wallSide = wallSides.get(i);
          if (wallSide.getWall() == splitWall) {
            wallSides.remove(i);
            sameWallSides.add(new WallSide(firstWall, wallSide.getSide()));
            sameWallSides.add(new WallSide(secondWall, wallSide.getSide()));
          }
        }
        wallSides.addAll(sameWallSides);
      }
    } while (splitWallSide != null);

    if (deletedWalls == null) { 
      return false; 
    } else {
      for (Wall newWall : newWalls) {     
        ModifiedWall addedWall = new ModifiedWall(newWall);
        addedWalls.add(addedWall);
        this.home.addWall(newWall);
        newWall.setLevel(addedWall.getLevel());
      }
      for (ModifiedWall deletedWall : deletedWalls) {
        this.home.deleteWall(deletedWall.getWall());
      }
      return !deletedWalls.isEmpty();
    }
  }

  private float [] computeIntersection(float xPoint1, float yPoint1, float xPoint2, float yPoint2, 
                                       float xPoint3, float yPoint3, float xPoint4, float yPoint4) {    
    float [] point = PlanController.computeIntersection(xPoint1, yPoint1, xPoint2, yPoint2, 
        xPoint3, yPoint3, xPoint4, yPoint4);
    if (Line2D.ptSegDistSq(xPoint1, yPoint1, xPoint2, yPoint2, point [0], point [1]) < 1E-7
        && (Math.abs(xPoint1 - point [0]) > 1E-4
            || Math.abs(yPoint1 - point [1]) > 1E-4)
        && (Math.abs(xPoint2 - point [0]) > 1E-4
            || Math.abs(yPoint2 - point [1]) > 1E-4)) {
      return point;
    } else {
      return null;
    }
  }

  private static class RoomsAndWallSidesModificationUndoableEdit extends AbstractUndoableEdit {
    private final Home                home;
    private final UserPreferences     preferences;
    private final List<Selectable>    oldSelection;
    private final List<Selectable>    newSelection;
    private final ModifiedRoom []     modifiedRooms;
    private final String              name;
    private final Boolean             areaVisible;
    private final Boolean             floorVisible;
    private final RoomPaint           floorPaint;
    private final Integer             floorColor;
    private final HomeTexture         floorTexture;
    private final Float               floorShininess;
    private final Boolean             ceilingVisible;
    private final RoomPaint           ceilingPaint;
    private final Integer             ceilingColor;
    private final HomeTexture         ceilingTexture;
    private final Float               ceilingShininess;
    private final ModifiedWallSide [] modifiedWallSides;
    private final float               newWallBaseboardHeight;
    private final float               newWallBaseboardThickness;
    private final Integer             wallSidesColor;
    private final HomeTexture         wallSidesTexture;
    private final Float               wallSidesShininess;
    private final Boolean             wallSidesBaseboardVisible;
    private final Float               wallSidesBaseboardThickness;
    private final Float               wallSidesBaseboardHeight;
    private final BaseboardChoiceController.BaseboardPaint wallSidesBaseboardPaint;
    private final Integer             wallSidesBaseboardColor;
    private final HomeTexture         wallSidesBaseboardTexture;
    private final ModifiedWall []     deletedWalls;
    private final ModifiedWall []     addedWalls;

    private RoomsAndWallSidesModificationUndoableEdit(Home home,
                                          UserPreferences preferences,
                                          List<Selectable> oldSelection,
                                          List<Selectable> newSelection, 
                                          ModifiedRoom [] modifiedRooms,
                                          String name,
                                          Boolean areaVisible,
                                          Boolean floorVisible,
                                          RoomPaint floorPaint,
                                          Integer floorColor,
                                          HomeTexture floorTexture,
                                          Float floorShininess,
                                          Boolean ceilingVisible,
                                          RoomPaint ceilingPaint,
                                          Integer ceilingColor,
                                          HomeTexture ceilingTexture,
                                          Float ceilingShininess,
                                          ModifiedWallSide [] modifiedWallSides,
                                          float newWallBaseboardThickness, 
                                          float newWallBaseboardHeight,
                                          Integer wallSidesColor,
                                          HomeTexture wallSidesTexture,
                                          Float wallSidesShininess, 
                                          Boolean wallSidesBaseboardVisible, 
                                          Float wallSidesBaseboardThickness, 
                                          Float wallSidesBaseboardHeight, 
                                          BaseboardChoiceController.BaseboardPaint wallSidesBaseboardPaint, 
                                          Integer wallSidesBaseboardColor, 
                                          HomeTexture wallSidesBaseboardTexture,
                                          ModifiedWall [] deletedWalls, 
                                          ModifiedWall [] addedWalls) {
      this.home = home;
      this.preferences = preferences;
      this.oldSelection = oldSelection;
      this.newSelection = newSelection;
      this.modifiedRooms = modifiedRooms;
      this.name = name;
      this.areaVisible = areaVisible;
      this.floorVisible = floorVisible;
      this.floorPaint = floorPaint;
      this.floorColor = floorColor;
      this.floorTexture = floorTexture;
      this.floorShininess = floorShininess;
      this.ceilingVisible = ceilingVisible;
      this.ceilingPaint = ceilingPaint;
      this.ceilingColor = ceilingColor;
      this.ceilingTexture = ceilingTexture;
      this.ceilingShininess = ceilingShininess;
      this.modifiedWallSides = modifiedWallSides;
      this.newWallBaseboardThickness = newWallBaseboardThickness;
      this.newWallBaseboardHeight = newWallBaseboardHeight;
      this.wallSidesColor = wallSidesColor;
      this.wallSidesTexture = wallSidesTexture;
      this.wallSidesShininess = wallSidesShininess;
      this.wallSidesBaseboardVisible = wallSidesBaseboardVisible;
      this.wallSidesBaseboardThickness = wallSidesBaseboardThickness;
      this.wallSidesBaseboardHeight = wallSidesBaseboardHeight;
      this.wallSidesBaseboardPaint = wallSidesBaseboardPaint;
      this.wallSidesBaseboardColor = wallSidesBaseboardColor;
      this.wallSidesBaseboardTexture = wallSidesBaseboardTexture;
      this.deletedWalls = deletedWalls;
      this.addedWalls = addedWalls;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      undoModifyRoomsAndWallSides(this.home, this.modifiedRooms, this.modifiedWallSides, this.deletedWalls, this.addedWalls); 
      this.home.setSelectedItems(this.oldSelection); 
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      doModifyRoomsAndWallSides(this.home,
          this.modifiedRooms, this.name, this.areaVisible, 
          this.floorVisible, this.floorPaint, this.floorColor, this.floorTexture, this.floorShininess, 
          this.ceilingVisible, this.ceilingPaint, this.ceilingColor, this.ceilingTexture, this.ceilingShininess,
          this.modifiedWallSides, newWallBaseboardThickness, this.newWallBaseboardHeight, 
          this.wallSidesColor, this.wallSidesTexture, this.wallSidesShininess,
          this.wallSidesBaseboardVisible, this.wallSidesBaseboardThickness, this.wallSidesBaseboardHeight, 
          this.wallSidesBaseboardPaint, this.wallSidesBaseboardColor, this.wallSidesBaseboardTexture,
          this.deletedWalls, this.addedWalls); 
      this.home.setSelectedItems(this.newSelection); 
    }

    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(RoomController.class, "undoModifyRoomsName");
    }
  }

  private static void doModifyRoomsAndWallSides(Home home, ModifiedRoom [] modifiedRooms, 
                                                String name, Boolean areaVisible, 
                                                Boolean floorVisible, RoomPaint floorPaint, Integer floorColor, HomeTexture floorTexture, Float floorShininess,
                                                Boolean ceilingVisible, RoomPaint ceilingPaint, Integer ceilingColor, HomeTexture ceilingTexture, Float ceilingShininess,
                                                ModifiedWallSide [] modifiedWallSides, 
                                                float newWallBaseboardThickness, float newWallBaseboardHeight,
                                                Integer wallSidesColor, HomeTexture wallSidesTexture, Float wallSidesShininess, 
                                                Boolean wallSidesBaseboardVisible, Float wallSidesBaseboardThickness, Float wallSidesBaseboardHeight, 
                                                BaseboardChoiceController.BaseboardPaint wallSidesBaseboardPaint, Integer wallSidesBaseboardColor, HomeTexture wallSidesBaseboardTexture,
                                                ModifiedWall [] deletedWalls, 
                                                ModifiedWall [] addedWalls) {
    if (deletedWalls != null) {
      for (ModifiedWall newWall : addedWalls) {
        newWall.resetJoinedWalls();
        home.addWall(newWall.getWall());
        newWall.getWall().setLevel(newWall.getLevel());
      }
      for (ModifiedWall deletedWall : deletedWalls) {
        home.deleteWall(deletedWall.getWall());
      }
    }
    for (ModifiedRoom modifiedRoom : modifiedRooms) {
      Room room = modifiedRoom.getRoom();
      if (name != null) {
        room.setName(name);
      }
      if (areaVisible != null) {
        room.setAreaVisible(areaVisible);
      }
      if (floorVisible != null) {
        room.setFloorVisible(floorVisible);
      }
      if (floorPaint != null) {
        switch (floorPaint) {
          case DEFAULT :
            room.setFloorColor(null);
            room.setFloorTexture(null);
            break;
          case COLORED :
            room.setFloorColor(floorColor);
            room.setFloorTexture(null);
            break;
          case TEXTURED :
            room.setFloorColor(null);
            room.setFloorTexture(floorTexture);
            break;
        }
      }
      if (floorShininess != null) {
        room.setFloorShininess(floorShininess);
      }
      if (ceilingVisible != null) {
        room.setCeilingVisible(ceilingVisible);
      }
      if (ceilingPaint != null) {
        switch (ceilingPaint) {
          case DEFAULT :
            room.setCeilingColor(null);
            room.setCeilingTexture(null);
            break;
          case COLORED :
            room.setCeilingColor(ceilingColor);
            room.setCeilingTexture(null);
            break;
          case TEXTURED :
            room.setCeilingColor(null);
            room.setCeilingTexture(ceilingTexture);
            break;
        }
      }
      if (ceilingShininess != null) {
        room.setCeilingShininess(ceilingShininess);
      }
    }
    for (ModifiedWallSide modifiedWallSide : modifiedWallSides) {
      WallSide wallSide = modifiedWallSide.getWallSide();
      Wall wall = wallSide.getWall();
      if (wallSidesColor != null) {
        if (wallSide.getSide() == WallSide.LEFT_SIDE) {
          wall.setLeftSideColor(wallSidesColor);
        } else {
          wall.setRightSideColor(wallSidesColor);
        }
      }
      
      if (wallSidesTexture != null || wallSidesColor != null) {
        if (wallSide.getSide() == WallSide.LEFT_SIDE) {
          wall.setLeftSideTexture(wallSidesTexture);
        } else {
          wall.setRightSideTexture(wallSidesTexture);
        }
      }

      if (wallSidesShininess != null) {
        if (wallSide.getSide() == WallSide.LEFT_SIDE) {
          wall.setLeftSideShininess(wallSidesShininess);
        } else {
          wall.setRightSideShininess(wallSidesShininess);
        }
      }

      if (wallSidesBaseboardVisible == Boolean.FALSE) {
        if (wallSide.getSide() == WallSide.LEFT_SIDE) {
          wall.setLeftSideBaseboard(null);
        } else {
          wall.setRightSideBaseboard(null);
        }
      } else {
        Baseboard baseboard = wallSide.getSide() == WallSide.LEFT_SIDE
            ? wall.getLeftSideBaseboard()
            : wall.getRightSideBaseboard();
        if (wallSidesBaseboardVisible == Boolean.TRUE 
            || baseboard != null) {
          float baseboardThickness = baseboard != null
              ? baseboard.getThickness()
              : newWallBaseboardThickness;
          float baseboardHeight = baseboard != null
              ? baseboard.getHeight()
              : newWallBaseboardHeight;
          Integer baseboardColor = baseboard != null
              ? baseboard.getColor()
              : null;
          HomeTexture baseboardTexture = baseboard != null
              ? baseboard.getTexture()
              : null;
          if (wallSidesBaseboardPaint != null) {
            switch (wallSidesBaseboardPaint) {
              case DEFAULT :
                baseboardColor = null;
                baseboardTexture = null;
                break;
              case COLORED :
                if (wallSidesBaseboardColor != null) {
                  baseboardColor = wallSidesBaseboardColor;
                }
                baseboardTexture = null;
                break;
              case TEXTURED :
                baseboardColor = null;
                if (wallSidesBaseboardTexture != null) {
                  baseboardTexture = wallSidesBaseboardTexture;
                }
                break;
            }
          }
          baseboard = Baseboard.getInstance(
              wallSidesBaseboardThickness != null
                  ? wallSidesBaseboardThickness
                  : baseboardThickness, 
              wallSidesBaseboardHeight != null
                  ? wallSidesBaseboardHeight
                  : baseboardHeight, 
              baseboardColor, baseboardTexture);
          if (wallSide.getSide() == WallSide.LEFT_SIDE) {
            wall.setLeftSideBaseboard(baseboard);
          } else {
            wall.setRightSideBaseboard(baseboard);
          }
        } 
      }
    }
  }

  private static void undoModifyRoomsAndWallSides(Home home, 
                                                  ModifiedRoom [] modifiedRooms,
                                                  ModifiedWallSide [] modifiedWallSides, 
                                                  ModifiedWall [] deletedWalls, 
                                                  ModifiedWall [] addedWalls) {
    for (ModifiedRoom modifiedRoom : modifiedRooms) {
      modifiedRoom.reset();
    }
    for (ModifiedWallSide modifiedWallSide : modifiedWallSides) {
      modifiedWallSide.reset();
    }
    for (ModifiedWall newWall : addedWalls) {
      home.deleteWall(newWall.getWall());
    }
    for (ModifiedWall deletedWall : deletedWalls) {
      deletedWall.resetJoinedWalls();
      home.addWall(deletedWall.getWall());
      deletedWall.getWall().setLevel(deletedWall.getLevel());
    }
  }
  
  private static final class ModifiedRoom {
    private final Room        room;
    private final String      name;
    private final boolean     areaVisible;
    private final boolean     floorVisible;
    private final Integer     floorColor;
    private final HomeTexture floorTexture;
    private final float       floorShininess;
    private final boolean     ceilingVisible;
    private final Integer     ceilingColor;
    private final HomeTexture ceilingTexture;
    private final float       ceilingShininess;

    public ModifiedRoom(Room room) {
      this.room = room;
      this.name = room.getName();
      this.areaVisible = room.isAreaVisible();
      this.floorVisible = room.isFloorVisible();
      this.floorColor = room.getFloorColor();
      this.floorTexture = room.getFloorTexture();
      this.floorShininess = room.getFloorShininess();
      this.ceilingVisible = room.isCeilingVisible();
      this.ceilingColor = room.getCeilingColor();
      this.ceilingTexture = room.getCeilingTexture();
      this.ceilingShininess = room.getCeilingShininess();
    }

    public Room getRoom() {
      return this.room;
    }
    
    public void reset() {
      this.room.setName(this.name);
      this.room.setAreaVisible(this.areaVisible);
      this.room.setFloorVisible(this.floorVisible);
      this.room.setFloorColor(this.floorColor);
      this.room.setFloorTexture(this.floorTexture);
      this.room.setFloorShininess(this.floorShininess);
      this.room.setCeilingVisible(this.ceilingVisible);
      this.room.setCeilingColor(this.ceilingColor);
      this.room.setCeilingTexture(this.ceilingTexture);
      this.room.setCeilingShininess(this.ceilingShininess);
    }    
  }

  private class WallSide {
    public static final int LEFT_SIDE = 0;
    public static final int RIGHT_SIDE = 1;
    
    private Wall          wall;
    private int           side;
    private final Wall    wallAtStart;
    private final Wall    wallAtEnd;
    private final boolean joinedAtEndOfWallAtStart;
    private final boolean joinedAtStartOfWallAtEnd;
    
    public WallSide(Wall wall, int side) {
      this.wall = wall;
      this.side = side;
      this.wallAtStart = wall.getWallAtStart();
      this.joinedAtEndOfWallAtStart =
          this.wallAtStart != null
          && this.wallAtStart.getWallAtEnd() == wall;
      this.wallAtEnd = wall.getWallAtEnd();
      this.joinedAtStartOfWallAtEnd =
          this.wallAtEnd != null
          && wallAtEnd.getWallAtStart() == wall;
    }
    
    public Wall getWall() {
      return this.wall;
    }
    
    public int getSide() {
      return this.side;
    }
    
    public Wall getWallAtStart() {
      return this.wallAtStart;
    }
    
    public Wall getWallAtEnd() {
      return this.wallAtEnd;
    }

    public boolean isJoinedAtEndOfWallAtStart() {
      return this.joinedAtEndOfWallAtStart;
    }

    public boolean isJoinedAtStartOfWallAtEnd() {
      return this.joinedAtStartOfWallAtEnd;
    }
  }

  private class ModifiedWall {
    private Wall          wall;
    private final Level   level;
    private final Wall    wallAtStart;
    private final Wall    wallAtEnd;
    private final boolean joinedAtEndOfWallAtStart;
    private final boolean joinedAtStartOfWallAtEnd;
    
    public ModifiedWall(Wall wall) {
      this.wall = wall;
      this.level = wall.getLevel();
      this.wallAtStart = wall.getWallAtStart();
      this.joinedAtEndOfWallAtStart =
          this.wallAtStart != null
          && this.wallAtStart.getWallAtEnd() == wall;
      this.wallAtEnd = wall.getWallAtEnd();
      this.joinedAtStartOfWallAtEnd =
          this.wallAtEnd != null
          && wallAtEnd.getWallAtStart() == wall;
    }
    
    public Wall getWall() {
      return this.wall;
    }
    
    public Level getLevel() {
      return this.level;
    }
    
    public void resetJoinedWalls() {
      if (this.wallAtStart != null) {
        this.wall.setWallAtStart(this.wallAtStart);
        if (this.joinedAtEndOfWallAtStart) {
          this.wallAtStart.setWallAtEnd(this.wall);
        } else {
          this.wallAtStart.setWallAtStart(this.wall);
        }
      }
      if (this.wallAtEnd != null) {
        this.wall.setWallAtEnd(wallAtEnd);
        if (this.joinedAtStartOfWallAtEnd) {
          this.wallAtEnd.setWallAtStart(this.wall);
        } else {
          this.wallAtEnd.setWallAtEnd(this.wall);
        }
      }
    }    
  }

  private static final class ModifiedWallSide {
    private final WallSide    wallSide;
    private final Integer     wallColor;
    private final HomeTexture wallTexture;
    private final Float       wallShininess;
    private final Baseboard   wallBaseboard;

    public ModifiedWallSide(WallSide wallSide) {
      this.wallSide = wallSide;
      Wall wall = wallSide.getWall();
      if (wallSide.getSide() == WallSide.LEFT_SIDE) {
        this.wallColor = wall.getLeftSideColor();
        this.wallTexture = wall.getLeftSideTexture();
        this.wallShininess = wall.getLeftSideShininess();
        this.wallBaseboard = wall.getLeftSideBaseboard();
      } else {
        this.wallColor = wall.getRightSideColor();
        this.wallTexture = wall.getRightSideTexture();
        this.wallShininess = wall.getRightSideShininess();
        this.wallBaseboard = wall.getRightSideBaseboard();
      }
    }

    public WallSide getWallSide() {
      return this.wallSide;
    }
    
    public void reset() {
      Wall wall = this.wallSide.getWall();
      if (this.wallSide.getSide() == WallSide.LEFT_SIDE) {
        wall.setLeftSideColor(this.wallColor);
        wall.setLeftSideTexture(this.wallTexture);
        wall.setLeftSideShininess(this.wallShininess);
        wall.setLeftSideBaseboard(this.wallBaseboard);
      } else {
        wall.setRightSideColor(this.wallColor);
        wall.setRightSideTexture(this.wallTexture);
        wall.setRightSideShininess(this.wallShininess);
        wall.setRightSideBaseboard(this.wallBaseboard);
      }
      Wall wallAtStart = wallSide.getWallAtStart();
      if (wallAtStart != null) {
        wall.setWallAtStart(wallAtStart);
        if (wallSide.isJoinedAtEndOfWallAtStart()) {
          wallAtStart.setWallAtEnd(wall);
        } else {
          wallAtStart.setWallAtStart(wall);
        }
      }
      Wall wallAtEnd = wallSide.getWallAtEnd();
      if (wallAtEnd != null) {
        wall.setWallAtEnd(wallAtEnd);
        if (wallSide.isJoinedAtStartOfWallAtEnd()) {
          wallAtEnd.setWallAtStart(wall);
        } else {
          wallAtEnd.setWallAtEnd(wall);
        }
      }
    }    
  }
}
