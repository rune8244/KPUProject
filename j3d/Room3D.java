package com.eteks.homeview3d.j3d;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.Node;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeFurnitureGroup;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Room;
import com.eteks.homeview3d.model.Wall;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

/**
 * 룸 브랜치 루트.
 */
public class Room3D extends Object3DBranch {
  private static final int FLOOR_PART  = 0;
  private static final int CEILING_PART = 1;
  
  private final Home home;

  public Room3D(Room room, Home home) {
    this(room, home, false, false);
  }


  public Room3D(Room room, Home home,
                boolean ignoreCeilingPart,
                boolean waitTextureLoadingEnd) {
    setUserData(room);
    this.home = home;

    setCapability(BranchGroup.ALLOW_DETACH);
    setCapability(BranchGroup.ALLOW_CHILDREN_READ);

    addChild(createRoomPartShape());
    addChild(createRoomPartShape());
    updateRoomGeometry();
    updateRoomAppearance(waitTextureLoadingEnd);
    
    if (ignoreCeilingPart) {
      removeChild(CEILING_PART);
    }
  }

  private Node createRoomPartShape() {
    Shape3D roomShape = new Shape3D();
    roomShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
    roomShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    roomShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

    Appearance roomAppearance = new Appearance();
    roomShape.setAppearance(roomAppearance);
    roomAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
    TransparencyAttributes transparencyAttributes = new TransparencyAttributes();
    transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
    transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
    roomAppearance.setTransparencyAttributes(transparencyAttributes);
    roomAppearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
    RenderingAttributes renderingAttributes = new RenderingAttributes();
    renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
    roomAppearance.setRenderingAttributes(renderingAttributes);
    roomAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
    roomAppearance.setMaterial(DEFAULT_MATERIAL);      
    roomAppearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
    roomAppearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
    roomAppearance.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
    
    return roomShape;
  }

  @Override
  public void update() {
    updateRoomGeometry();
    updateRoomAppearance(false);
  }

  private void updateRoomGeometry() {
    updateRoomPartGeometry(FLOOR_PART, ((Room)getUserData()).getFloorTexture());
    updateRoomPartGeometry(CEILING_PART, ((Room)getUserData()).getCeilingTexture());
  }
  
  private void updateRoomPartGeometry(int roomPart, HomeTexture texture) {
    Shape3D roomShape = (Shape3D)getChild(roomPart);
    int currentGeometriesCount = roomShape.numGeometries();
    Room room = (Room)getUserData();
    if (room.getLevel() == null || room.getLevel().isViewableAndVisible()) {
      for (Geometry roomGeometry : createRoomGeometries(roomPart, texture)) {
        roomShape.addGeometry(roomGeometry);
      }
    }
    for (int i = currentGeometriesCount - 1; i >= 0; i--) {
      roomShape.removeGeometry(i);
    }
  }

  private Geometry [] createRoomGeometries(int roomPart, HomeTexture texture) {
    Room room = (Room)getUserData();
    float [][] points = room.getPoints();
    if ((roomPart == FLOOR_PART && room.isFloorVisible()
         || roomPart == CEILING_PART && room.isCeilingVisible())
        && points.length > 2) {
      Level roomLevel = room.getLevel();
      List<Level> levels = this.home.getLevels();
      boolean lastLevel = isLastLevel(roomLevel, levels);
      float floorBottomElevation;
      float roomElevation;
      if (roomLevel != null) {
        roomElevation = roomLevel.getElevation();
        floorBottomElevation = roomElevation - roomLevel.getFloorThickness();
      } else {
        roomElevation = 0;
        floorBottomElevation = 0;
      }

      float firstLevelElevation;
      if (levels.size() == 0) {
        firstLevelElevation = 0;
      } else {
        firstLevelElevation = levels.get(0).getElevation();
      }
      boolean floorBottomVisible = roomPart == FLOOR_PART 
          && roomLevel != null 
          && roomElevation != firstLevelElevation;
 
      final List<Room> roomsAtSameElevation = new ArrayList<Room>();
      List<Room> ceilingsAtSameFloorBottomElevation = new ArrayList<Room>();
      for (Room homeRoom : this.home.getRooms()) {
        Level homeRoomLevel = homeRoom.getLevel();
        if (homeRoomLevel == null || homeRoomLevel.isViewableAndVisible()) {
          if (room == homeRoom // Store also the room itself to know its order among rooms at same elevation
              || roomLevel == homeRoomLevel
                  && (roomPart == FLOOR_PART && homeRoom.isFloorVisible()
                      || roomPart == CEILING_PART && homeRoom.isCeilingVisible()) 
              || roomLevel != null 
                  && homeRoomLevel != null 
                  && (roomPart == FLOOR_PART 
                          && homeRoom.isFloorVisible()
                          && Math.abs(roomElevation - homeRoomLevel.getElevation()) < 1E-4
                      || roomPart == CEILING_PART 
                          && homeRoom.isCeilingVisible()
                          && !lastLevel
                          && !isLastLevel(homeRoomLevel, levels)
                          && Math.abs(roomElevation + roomLevel.getHeight() - (homeRoomLevel.getElevation() + homeRoomLevel.getHeight())) < 1E-4)) {         
            roomsAtSameElevation.add(homeRoom);
          } else if (floorBottomVisible 
                      && homeRoomLevel != null 
                      && homeRoom.isCeilingVisible() 
                      && !isLastLevel(homeRoomLevel, levels) 
                      && Math.abs(floorBottomElevation - (homeRoomLevel.getElevation() + homeRoomLevel.getHeight())) < 1E-4) {
            ceilingsAtSameFloorBottomElevation.add(homeRoom);
          }
        }
      }
      if (roomLevel != null) {
        Collections.sort(roomsAtSameElevation, new Comparator<Room>() {
            public int compare(Room room1, Room room2) {
              int comparison = Float.compare(room1.getLevel().getElevation(), room2.getLevel().getElevation());
              if (comparison != 0) {
                return comparison;
              } else {
                return room1.getLevel().getElevationIndex() - room2.getLevel().getElevationIndex();
              }
            }
          });
      }
      
      List<HomePieceOfFurniture> visibleStaircases;
      if (roomLevel == null
          || roomPart == CEILING_PART
              && lastLevel) {
        visibleStaircases = Collections.emptyList();
      } else {
        visibleStaircases = getVisibleStaircases(this.home.getFurniture(), roomPart, roomLevel, 
            roomLevel.getElevation() == firstLevelElevation);
      }
      boolean sameElevation = true;
      if (roomPart == CEILING_PART
          && (roomLevel == null || lastLevel)) {
        float firstPointElevation = getRoomHeightAt(points [0][0], points [0][1]);
        for (int i = 1; i < points.length && sameElevation; i++) {
          sameElevation = getRoomHeightAt(points [i][0], points [i][1]) == firstPointElevation;
        }
      }

      List<float [][]> roomPoints;
      List<float [][]> roomHoles;
      List<float [][]> roomPointsWithoutHoles;
      Area roomVisibleArea;
      if (!room.isSingular() 
          || sameElevation
              && (roomsAtSameElevation.get(roomsAtSameElevation.size() - 1) != room
                  || visibleStaircases.size() > 0)) {        
        roomVisibleArea = new Area(getShape(points));
        if (roomsAtSameElevation.contains(room)) {
          for (int i = roomsAtSameElevation.size() - 1; i > 0 && roomsAtSameElevation.get(i) != room; i--) {
            Room otherRoom = roomsAtSameElevation.get(i);
            roomVisibleArea.subtract(new Area(getShape(otherRoom.getPoints())));
          }
        }        
        removeStaircasesFromArea(visibleStaircases, roomVisibleArea);
        roomPoints = new ArrayList<float[][]>();
        roomHoles = new ArrayList<float[][]>();
        roomPointsWithoutHoles = getAreaPoints(roomVisibleArea, roomPoints, roomHoles, 1, roomPart == CEILING_PART);
      } else {
        boolean clockwise = room.isClockwise();
        if (clockwise && roomPart == FLOOR_PART
            || !clockwise && roomPart == CEILING_PART) {
          points = getReversedArray(points);
        }
        roomPointsWithoutHoles = 
        roomPoints = Arrays.asList(new float [][][] {points});
        roomHoles = Collections.emptyList();
        roomVisibleArea = null;
      }
      
      List<Geometry> geometries = new ArrayList<Geometry> (3);      
      final float subpartSize = this.home.getEnvironment().getSubpartSizeUnderLight();
      
      if (!roomPointsWithoutHoles.isEmpty()) {
        List<float []> roomPointElevations = new ArrayList<float[]>();
        boolean roomAtSameElevation = true;
        for (int i = 0; i < roomPointsWithoutHoles.size(); i++) {
          float [][] roomPartPoints = roomPointsWithoutHoles.get(i);
          float [] roomPartPointElevations = new float [roomPartPoints.length];
          for (int j = 0; j < roomPartPoints.length; j++) {
            roomPartPointElevations [j] = roomPart == FLOOR_PART 
                ? roomElevation 
                : getRoomHeightAt(roomPartPoints [j][0], roomPartPoints [j][1]);
            if (roomAtSameElevation && j > 0) {
              roomAtSameElevation = roomPartPointElevations [j] == roomPartPointElevations [j - 1];
            }
          }
          roomPointElevations.add(roomPartPointElevations);
        }

        if (roomAtSameElevation && subpartSize > 0) {
          for (int i = 0; i < roomPointsWithoutHoles.size(); i++) {
            float [][] roomPartPoints = roomPointsWithoutHoles.get(i);
   
            float xMin = Float.MAX_VALUE;
            float xMax = Float.MIN_VALUE;
            float zMin = Float.MAX_VALUE;
            float zMax = Float.MIN_VALUE;
            for (float [] point : roomPartPoints) {
              xMin = Math.min(xMin, point [0]);
              xMax = Math.max(xMax, point [0]);
              zMin = Math.min(zMin, point [1]);
              zMax = Math.max(zMax, point [1]);
            }
            
            Area roomPartArea = new Area(getShape(roomPartPoints));        
            for (float xSquare = xMin; xSquare < xMax; xSquare += subpartSize) {
              for (float zSquare = zMin; zSquare < zMax; zSquare += subpartSize) {
                Area roomPartSquare = new Area(new Rectangle2D.Float(xSquare, zSquare, subpartSize, subpartSize));
                roomPartSquare.intersect(roomPartArea);
                if (!roomPartSquare.isEmpty()) {
                  List<float [][]> geometryPartPointsWithoutHoles = 
                      getAreaPoints(roomPartSquare, 1, roomPart == CEILING_PART);
                  if (!geometryPartPointsWithoutHoles.isEmpty()) {
                    geometries.add(computeRoomPartGeometry(geometryPartPointsWithoutHoles, 
                        null, roomLevel, roomPointElevations.get(i) [0], floorBottomElevation, 
                        roomPart == FLOOR_PART, false, texture));
                  }
                }
              }
            }
          }
        } else {
          geometries.add(computeRoomPartGeometry(roomPointsWithoutHoles, roomPointElevations, roomLevel,
              roomElevation, floorBottomElevation, roomPart == FLOOR_PART, false, texture));
        }
          

        if (roomLevel != null
            && roomPart == FLOOR_PART 
            && roomLevel.getElevation() != firstLevelElevation) {
          geometries.add(computeRoomBorderGeometry(roomPoints, roomHoles, roomLevel, roomElevation, texture));
        }
      }

      if (floorBottomVisible) {
        List<float [][]> floorBottomPointsWithoutHoles;
        if (roomVisibleArea != null 
            || ceilingsAtSameFloorBottomElevation.size() > 0) {        
          Area floorBottomVisibleArea = roomVisibleArea != null ? roomVisibleArea : new Area(getShape(points));
          for (Room otherRoom : ceilingsAtSameFloorBottomElevation) {
           floorBottomVisibleArea.subtract(new Area(getShape(otherRoom.getPoints())));
          }          
          floorBottomPointsWithoutHoles = getAreaPoints(floorBottomVisibleArea, 1, true);
        } else {
          floorBottomPointsWithoutHoles = Arrays.asList(new float [][][] {getReversedArray(points)});
        }
        
        if (!floorBottomPointsWithoutHoles.isEmpty()) {
          if (subpartSize > 0) {
            for (int i = 0 ; i < floorBottomPointsWithoutHoles.size(); i++) {
              float [][] floorBottomPartPoints = floorBottomPointsWithoutHoles.get(i);
              float xMin = Float.MAX_VALUE;
              float xMax = Float.MIN_VALUE;
              float zMin = Float.MAX_VALUE;
              float zMax = Float.MIN_VALUE;
              for (float [] point : floorBottomPartPoints) {
                xMin = Math.min(xMin, point [0]);
                xMax = Math.max(xMax, point [0]);
                zMin = Math.min(zMin, point [1]);
                zMax = Math.max(zMax, point [1]);
              }
              
              Area floorBottomPartArea = new Area(getShape(floorBottomPartPoints));
              for (float xSquare = xMin; xSquare < xMax; xSquare += subpartSize) {
                for (float zSquare = zMin; zSquare < zMax; zSquare += subpartSize) {
                  Area floorBottomPartSquare = new Area(new Rectangle2D.Float(xSquare, zSquare, subpartSize, subpartSize));
                  floorBottomPartSquare.intersect(floorBottomPartArea);
                  if (!floorBottomPartSquare.isEmpty()) {
                    List<float [][]> geometryPartPointsWithoutHoles = getAreaPoints(floorBottomPartSquare, 1, true);
                    if (!geometryPartPointsWithoutHoles.isEmpty()) {
                      geometries.add(computeRoomPartGeometry(geometryPartPointsWithoutHoles, 
                          null, roomLevel, roomElevation, floorBottomElevation, 
                          true, true, texture));
                    }
                  }
                }
              }
            }
          } else {
            geometries.add(computeRoomPartGeometry(floorBottomPointsWithoutHoles, null, roomLevel,
                roomElevation, floorBottomElevation, true, true, texture));
          }
        }
      }

      return geometries.toArray(new Geometry [geometries.size()]);
    } else {
      return new Geometry [0];
    }
  }

  private Geometry computeRoomPartGeometry(List<float [][]> geometryPoints, 
                                           List<float []> roomPointElevations,
                                           Level roomLevel,
                                           float roomPartElevation, float floorBottomElevation,
                                           boolean floorPart, boolean floorBottomPart, 
                                           HomeTexture texture) {
    int [] stripCounts = new int [geometryPoints.size()];
    int vertexCount = 0;
    for (int i = 0; i < geometryPoints.size(); i++) {
      float [][] areaPoints = geometryPoints.get(i);
      stripCounts [i] = areaPoints.length;
      vertexCount += stripCounts [i]; 
    }
    Point3f [] coords = new Point3f [vertexCount];
    int i = 0;
    for (int j = 0; j < geometryPoints.size(); j++) {
      float [][] areaPoints = geometryPoints.get(j);
      float [] roomPartPointElevations = roomPointElevations != null
          ? roomPointElevations.get(j)
          : null;
      for (int k = 0; k < areaPoints.length; k++) {
        float y = floorBottomPart 
            ? floorBottomElevation 
            : (roomPartPointElevations != null
                  ? roomPartPointElevations [k]
                  : roomPartElevation);
        coords [i++] = new Point3f(areaPoints [k][0], y, areaPoints [k][1]);
      }
    }
    GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
    geometryInfo.setCoordinates(coords);
    geometryInfo.setStripCounts(stripCounts);
    
    if (texture != null) {
      TexCoord2f [] textureCoords = new TexCoord2f [vertexCount];
      i = 0;
      for (float [][] areaPoints : geometryPoints) {
        for (int k = 0; k < areaPoints.length; k++) {
          textureCoords [i++] = new TexCoord2f(areaPoints [k][0], 
              floorPart 
                  ? -areaPoints [k][1]
                  : areaPoints [k][1]);
        }
      }
      geometryInfo.setTextureCoordinateParams(1, 2);
      geometryInfo.setTextureCoordinates(0, textureCoords);
    }

    new NormalGenerator().generateNormals(geometryInfo);
    return geometryInfo.getIndexedGeometryArray();
  }

  private Geometry computeRoomBorderGeometry(List<float [][]> geometryRooms, 
                                             List<float [][]> geometryHoles,
                                             Level roomLevel, float roomElevation, 
                                             HomeTexture texture) {
    int vertexCount = 0;
    for (float [][] geometryPoints : geometryRooms) {
      vertexCount += geometryPoints.length;
    }
    for (float [][] geometryHole : geometryHoles) {
      vertexCount += geometryHole.length;
    }
    vertexCount = vertexCount * 4;

    int i = 0;
    Point3f [] coords = new Point3f [vertexCount];
    float floorBottomElevation = roomElevation - roomLevel.getFloorThickness();
    for (float [][] geometryPoints : geometryRooms) {
      for (int j = 0; j < geometryPoints.length; j++) {
        coords [i++] = new Point3f(geometryPoints [j][0], roomElevation, geometryPoints [j][1]);
        coords [i++] = new Point3f(geometryPoints [j][0], floorBottomElevation, geometryPoints [j][1]);
        int nextPoint = j < geometryPoints.length - 1  
            ? j + 1
            : 0;
        coords [i++] = new Point3f(geometryPoints [nextPoint][0], floorBottomElevation, geometryPoints [nextPoint][1]);
        coords [i++] = new Point3f(geometryPoints [nextPoint][0], roomElevation, geometryPoints [nextPoint][1]);
      }
    }
    for (float [][] geometryHole : geometryHoles) {
      for (int j = 0; j < geometryHole.length; j++) {
        coords [i++] = new Point3f(geometryHole [j][0], roomElevation, geometryHole [j][1]);
        int nextPoint = j < geometryHole.length - 1  
            ? j + 1
            : 0;
        coords [i++] = new Point3f(geometryHole [nextPoint][0], roomElevation, geometryHole [nextPoint][1]);
        coords [i++] = new Point3f(geometryHole [nextPoint][0], floorBottomElevation, geometryHole [nextPoint][1]);
        coords [i++] = new Point3f(geometryHole [j][0], floorBottomElevation, geometryHole [j][1]);
      }
    }

    GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
    geometryInfo.setCoordinates(coords);
    
    if (texture != null) {
      TexCoord2f [] textureCoords = new TexCoord2f [vertexCount];
      i = 0;
      for (float [][] geometryPoints : geometryRooms) {
        for (int j = 0; j < geometryPoints.length; j++) {
          textureCoords [i++] = new TexCoord2f(0, roomLevel.getFloorThickness());
          textureCoords [i++] = new TexCoord2f(0, 0);
          int nextPoint = j < geometryPoints.length - 1  
              ? j + 1
              : 0;
          float textureCoord = (float)(Point2D.distance(geometryPoints [j][0], geometryPoints [j][1], 
              geometryPoints [nextPoint][0], geometryPoints [nextPoint][1]));
          textureCoords [i++] = new TexCoord2f(textureCoord, 0);
          textureCoords [i++] = new TexCoord2f(textureCoord, roomLevel.getFloorThickness());
        }
      }
      for (float [][] geometryHole : geometryHoles) {
        for (int j = 0; j < geometryHole.length; j++) {
          textureCoords [i++] = new TexCoord2f(0, 0);
          int nextPoint = j < geometryHole.length - 1  
              ? j + 1
              : 0;
          float textureCoord = (float)(Point2D.distance(geometryHole [j][0], geometryHole [j][1], 
              geometryHole [nextPoint][0], geometryHole [nextPoint][1]));
          textureCoords [i++] = new TexCoord2f(textureCoord, 0);
          textureCoords [i++] = new TexCoord2f(textureCoord, roomLevel.getFloorThickness());
          textureCoords [i++] = new TexCoord2f(0, roomLevel.getFloorThickness());
        }
      }
      geometryInfo.setTextureCoordinateParams(1, 2);
      geometryInfo.setTextureCoordinates(0, textureCoords);
    }

    new NormalGenerator(Math.PI / 8).generateNormals(geometryInfo);
    return geometryInfo.getIndexedGeometryArray();
  }

  private void removeStaircasesFromArea(List<HomePieceOfFurniture> visibleStaircases, Area area) {
    ModelManager modelManager = ModelManager.getInstance();
    for (HomePieceOfFurniture staircase : visibleStaircases) {
      area.subtract(modelManager.getAreaOnFloor(staircase));
    }
  }

  private List<HomePieceOfFurniture> getVisibleStaircases(List<HomePieceOfFurniture> furniture, 
                                                          int roomPart, Level roomLevel,
                                                          boolean firstLevel) {
    List<HomePieceOfFurniture> visibleStaircases = new ArrayList<HomePieceOfFurniture>(furniture.size());
    for (HomePieceOfFurniture piece : furniture) {
      if (piece.isVisible()
          && (piece.getLevel() == null
              || piece.getLevel().isViewableAndVisible())) {
        if (piece instanceof HomeFurnitureGroup) {
          visibleStaircases.addAll(getVisibleStaircases(((HomeFurnitureGroup)piece).getFurniture(), roomPart, roomLevel, firstLevel));
        } else if (piece.getStaircaseCutOutShape() != null
            && !"false".equalsIgnoreCase(piece.getStaircaseCutOutShape())
            && ((roomPart == FLOOR_PART 
                    && piece.getGroundElevation() < roomLevel.getElevation()
                    && piece.getGroundElevation() + piece.getHeight() >= roomLevel.getElevation() - (firstLevel ? 0 : roomLevel.getFloorThickness())
                || roomPart == CEILING_PART
                    && piece.getGroundElevation() < roomLevel.getElevation() + roomLevel.getHeight()
                    && piece.getGroundElevation() + piece.getHeight() >= roomLevel.getElevation() + roomLevel.getHeight()))) {
          visibleStaircases.add(piece);
        }
      }
    }
    return visibleStaircases;
  }

  private float [][] getReversedArray(float [][] points) {
    points = points.clone();
    List<float []> pointList = Arrays.asList(points);
    Collections.reverse(pointList);
    return pointList.toArray(points);
  }

  private float getRoomHeightAt(float x, float y) {
    double smallestDistance = Float.POSITIVE_INFINITY;
    Room room = (Room)getUserData();
    Level roomLevel = room.getLevel();
    float roomElevation = roomLevel != null
        ? roomLevel.getElevation()
        : 0;
    float roomHeight = roomElevation + 
        (roomLevel == null ? this.home.getWallHeight() : roomLevel.getHeight());
    List<Level> levels = this.home.getLevels();
    if (roomLevel == null || isLastLevel(roomLevel, levels)) {
      Wall closestWall = null;
      float [][] closestWallPoints = null;
      int closestIndex = -1;
      for (Wall wall : this.home.getWalls()) {
        if ((wall.getLevel() == null || wall.getLevel().isViewable())
            && wall.isAtLevel(roomLevel)) {
          float [][] points = wall.getPoints();
          for (int i = 0; i < points.length; i++) {
            double distanceToWallPoint = Point2D.distanceSq(points [i][0], points [i][1], x, y);
            if (distanceToWallPoint < smallestDistance) {
              closestWall = wall;
              closestWallPoints = points;
              closestIndex = i;
              smallestDistance = distanceToWallPoint;
            }
          }
        }
      }
      
      if (closestWall != null) {
        roomHeight = closestWall.getLevel() == null ? 0 : closestWall.getLevel().getElevation();
        Float wallHeightAtStart = closestWall.getHeight();
        if (closestIndex == 0 || closestIndex == closestWallPoints.length - 1) { // Wall start
          roomHeight += wallHeightAtStart != null 
              ? wallHeightAtStart 
              : this.home.getWallHeight();
        } else {
          if (closestWall.isTrapezoidal()) {
            Float arcExtent = closestWall.getArcExtent();
            if (arcExtent == null
                || arcExtent.floatValue() == 0
                || closestIndex == closestWallPoints.length / 2 
                || closestIndex == closestWallPoints.length / 2 - 1) {
              roomHeight += closestWall.getHeightAtEnd();
            } else {
              float xArcCircleCenter = closestWall.getXArcCircleCenter();
              float yArcCircleCenter = closestWall.getYArcCircleCenter();
              float xClosestPoint = closestWallPoints [closestIndex][0];
              float yClosestPoint = closestWallPoints [closestIndex][1];
              double centerToClosestPointDistance = Point2D.distance(xArcCircleCenter, yArcCircleCenter, xClosestPoint, yClosestPoint);
              float xStart = closestWall.getXStart();
              float yStart = closestWall.getYStart();
              double centerToStartPointDistance = Point2D.distance(xArcCircleCenter, yArcCircleCenter, xStart, yStart);
              double scalarProduct = (xClosestPoint - xArcCircleCenter) * (xStart - xArcCircleCenter) 
                  + (yClosestPoint - yArcCircleCenter) * (yStart - yArcCircleCenter);
              scalarProduct /= (centerToClosestPointDistance * centerToStartPointDistance);
              double arcExtentToClosestWallPoint = Math.acos(scalarProduct) * Math.signum(arcExtent);
              roomHeight += (float)(wallHeightAtStart 
                  + (closestWall.getHeightAtEnd() - wallHeightAtStart) * arcExtentToClosestWallPoint / arcExtent);
            }
          } else {
            roomHeight += (wallHeightAtStart != null ? wallHeightAtStart : this.home.getWallHeight());
          }
        }
      }
    }
    return roomHeight;
  }

  private boolean isLastLevel(Level level, List<Level> levels) {
    return levels.indexOf(level) == levels.size() - 1;
  }

  private void updateRoomAppearance(boolean waitTextureLoadingEnd) {
    Room room = (Room)getUserData();
    boolean ignoreFloorTransparency = room.getLevel() == null || room.getLevel().getElevation() <= 0;
    updateRoomPartAppearance(((Shape3D)getChild(FLOOR_PART)).getAppearance(), 
        room.getFloorTexture(), waitTextureLoadingEnd, room.getFloorColor(), room.getFloorShininess(), room.isFloorVisible(), ignoreFloorTransparency);
    boolean ignoreCeillingTransparency = room.getLevel() == null; 
    updateRoomPartAppearance(((Shape3D)getChild(CEILING_PART)).getAppearance(), 
        room.getCeilingTexture(), waitTextureLoadingEnd, room.getCeilingColor(), room.getCeilingShininess(), room.isCeilingVisible(), ignoreCeillingTransparency);
  }

  private void updateRoomPartAppearance(final Appearance roomPartAppearance, 
                                        final HomeTexture roomPartTexture,
                                        boolean waitTextureLoadingEnd,
                                        Integer roomPartColor,
                                        float shininess,
                                        boolean visible,
                                        boolean ignoreTransparency) {
    if (roomPartTexture == null) {
      roomPartAppearance.setMaterial(getMaterial(roomPartColor, roomPartColor, shininess));
      roomPartAppearance.setTexture(null);
    } else {
      roomPartAppearance.setMaterial(getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, shininess));
      roomPartAppearance.setTextureAttributes(getTextureAttributes(roomPartTexture, true));
      final TextureManager textureManager = TextureManager.getInstance();
      textureManager.loadTexture(roomPartTexture.getImage(), waitTextureLoadingEnd,
          new TextureManager.TextureObserver() {
              public void textureUpdated(Texture texture) {
                texture = getHomeTextureClone(texture, home);
                if (roomPartAppearance.getTexture() != texture) {
                  roomPartAppearance.setTexture(texture);
                }
              }
            });
    }
    if (!ignoreTransparency) { 
      float upperRoomsAlpha = this.home.getEnvironment().getWallsAlpha();
      TransparencyAttributes transparencyAttributes = roomPartAppearance.getTransparencyAttributes();
      transparencyAttributes.setTransparency(upperRoomsAlpha);
      transparencyAttributes.setTransparencyMode(upperRoomsAlpha == 0 
          ? TransparencyAttributes.NONE 
          : TransparencyAttributes.NICEST);
    }
    RenderingAttributes renderingAttributes = roomPartAppearance.getRenderingAttributes();
    renderingAttributes.setVisible(visible);
  }
}