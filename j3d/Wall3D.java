package com.eteks.homeview3d.j3d;

import java.awt.EventQueue;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;

import com.eteks.homeview3d.model.Baseboard;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.DoorOrWindow;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.HomeFurnitureGroup;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Wall;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

public class Wall3D extends Object3DBranch {
  private static final float LEVEL_ELEVATION_SHIFT = 0.1f;
  private static final Area  FULL_FACE_CUT_OUT_AREA = new Area(new Rectangle2D.Float(-0.5f, 0.5f, 1, 1));
  
  private static final int WALL_LEFT_SIDE  = 0;
  private static final int WALL_RIGHT_SIDE = 1;
  
  private static Map<HomePieceOfFurniture, ModelRotationTuple> doorOrWindowRotatedModels = new WeakHashMap<HomePieceOfFurniture, ModelRotationTuple>();
  private static Map<ModelRotationTuple, Area>                 rotatedModelsFrontAreas   = new WeakHashMap<ModelRotationTuple, Area>();
  
  private final Home home;

  public Wall3D(Wall wall, Home home) {
    this(wall, home, false, false);
  }

  public Wall3D(Wall wall, Home home, boolean ignoreDrawingMode, 
                boolean waitModelAndTextureLoadingEnd) {
    setUserData(wall);
    this.home = home;

    setCapability(BranchGroup.ALLOW_DETACH);
    setCapability(BranchGroup.ALLOW_CHILDREN_READ);

    for (int i = 0; i < 8; i++) {
      Group wallSideGroup = new Group();
      wallSideGroup.setCapability(Group.ALLOW_CHILDREN_READ);
      wallSideGroup.addChild(createWallPartShape(false));
      if (!ignoreDrawingMode) {
        wallSideGroup.addChild(createWallPartShape(true));
      }
      addChild(wallSideGroup);
    }
    updateWallGeometry(waitModelAndTextureLoadingEnd);
    updateWallAppearance(waitModelAndTextureLoadingEnd);
  }

  private Node createWallPartShape(boolean outline) {
    Shape3D wallShape = new Shape3D();
    wallShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
    wallShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    wallShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

    Appearance wallAppearance = new Appearance();
    wallShape.setAppearance(wallAppearance);
    wallAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
    TransparencyAttributes transparencyAttributes = new TransparencyAttributes();
    transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
    transparencyAttributes.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
    wallAppearance.setTransparencyAttributes(transparencyAttributes);
    wallAppearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
    RenderingAttributes renderingAttributes = new RenderingAttributes();
    renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
    wallAppearance.setRenderingAttributes(renderingAttributes);
    
    if (outline) {
      wallAppearance.setColoringAttributes(Object3DBranch.OUTLINE_COLORING_ATTRIBUTES);
      wallAppearance.setPolygonAttributes(Object3DBranch.OUTLINE_POLYGON_ATTRIBUTES);
      wallAppearance.setLineAttributes(Object3DBranch.OUTLINE_LINE_ATTRIBUTES);
    } else {
      wallAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
      wallAppearance.setMaterial(DEFAULT_MATERIAL);      
      wallAppearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
      wallAppearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
      wallAppearance.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
    }
    return wallShape;
  }

  @Override
  public void update() {
    updateWallGeometry(false);
    updateWallAppearance(false);
  }

  private void updateWallGeometry(boolean waitDoorOrWindowModelsLoadingEnd) {    
    updateWallSideGeometry(WALL_LEFT_SIDE, waitDoorOrWindowModelsLoadingEnd);
    updateWallSideGeometry(WALL_RIGHT_SIDE, waitDoorOrWindowModelsLoadingEnd);
  }
  
  private void updateWallSideGeometry(int wallSide, 
                                      boolean waitDoorOrWindowModelsLoadingEnd) {
    Wall wall = (Wall)getUserData();
    HomeTexture wallTexture;
    Baseboard baseboard;
    if (wallSide == WALL_LEFT_SIDE) {
      wallTexture = wall.getLeftSideTexture();
      baseboard = wall.getLeftSideBaseboard();
    } else {
      wallTexture = wall.getRightSideTexture();
      baseboard = wall.getRightSideBaseboard();
    }
    Group [] wallSideGroups = {(Group)getChild(wallSide),      
                               (Group)getChild(wallSide + 2),  
                               (Group)getChild(wallSide + 4), 
                               (Group)getChild(wallSide + 6)}; 
    Shape3D [] wallFilledShapes = new Shape3D [wallSideGroups.length];
    Shape3D [] wallOutlineShapes = new Shape3D [wallSideGroups.length];
    int [] currentGeometriesCounts = new int [wallSideGroups.length];
    for (int i = 0; i < wallSideGroups.length; i++) {
      wallFilledShapes  [i] = (Shape3D)wallSideGroups [i].getChild(0);
      wallOutlineShapes [i] = wallSideGroups [i].numChildren() > 1 
          ? (Shape3D)wallSideGroups [i].getChild(1)
          : null;
      currentGeometriesCounts [i] = wallFilledShapes [i].numGeometries();
    }
    if (wall.getLevel() == null || wall.getLevel().isViewableAndVisible()) {
      List [] wallGeometries = {new ArrayList<Geometry>(), 
                                new ArrayList<Geometry>(), 
                                new ArrayList<Geometry>(), 
                                new ArrayList<Geometry>()};
      createWallGeometries(wallGeometries [0], wallGeometries [2], wallGeometries [3], wallSide, 
          null, wallTexture, waitDoorOrWindowModelsLoadingEnd);
      if (baseboard != null) {
        HomeTexture baseboardTexture = baseboard.getTexture();
        if (baseboardTexture == null 
            && baseboard.getColor() == null) {
          baseboardTexture = wallTexture;
        }
        createWallGeometries(wallGeometries [1], wallGeometries [1], wallGeometries [1], wallSide, 
            baseboard, baseboardTexture, waitDoorOrWindowModelsLoadingEnd);
      }
      for (int i = 0; i < wallSideGroups.length; i++) {
        for (Geometry wallGeometry : (List<Geometry>)wallGeometries [i]) {
          if (wallGeometry != null) {
            wallFilledShapes [i].addGeometry(wallGeometry);
            if (wallOutlineShapes [i] != null) {
              wallOutlineShapes [i].addGeometry(wallGeometry);
            }
          }
        }
      }
    }
    for (int i = 0; i < wallSideGroups.length; i++) {
      for (int j = currentGeometriesCounts [i] - 1; j >= 0; j--) {
        wallFilledShapes [i].removeGeometry(j);
        if (wallOutlineShapes [i] != null) {
          wallOutlineShapes [i].removeGeometry(j);
        }
      }
    }
  }

  private void createWallGeometries(List<Geometry> bottomGeometries, 
                                    final List<Geometry> sideGeometries, 
                                    final List<Geometry> topGeometries, 
                                    final int wallSide, 
                                    final Baseboard baseboard,
                                    final HomeTexture texture, 
                                    final boolean waitDoorOrWindowModelsLoadingEnd) {
    final Wall wall = (Wall)getUserData();
    final float [][] wallSidePoints = getWallSidePoints(wallSide);
    Shape wallShape = getShape(wallSidePoints);
    final float [][] wallSideOrBaseboardPoints = baseboard == null 
        ? wallSidePoints
        : getWallBaseboardPoints(wallSide);
    Shape wallOrBaseboardShape = getShape(wallSideOrBaseboardPoints);
    Area wallOrBaseboardArea = new Area(wallOrBaseboardShape);
    final float [] textureReferencePoint = wallSide == WALL_LEFT_SIDE
        ? wallSideOrBaseboardPoints [0].clone()
        : wallSideOrBaseboardPoints [wallSideOrBaseboardPoints.length - 1].clone();
    final float wallElevation = getWallElevation(baseboard != null);
    float topElevationAtStart;
    float topElevationAtEnd;
    if (baseboard == null) {
      topElevationAtStart = getWallTopElevationAtStart();
      topElevationAtEnd = getWallTopElevationAtEnd();
    } else {
      topElevationAtStart = 
      topElevationAtEnd = getBaseboardTopElevation(baseboard);
    }
    float maxTopElevation = Math.max(topElevationAtStart, topElevationAtEnd);

    double wallYawAngle = Math.atan2(wall.getYEnd() - wall.getYStart(), wall.getXEnd() - wall.getXStart()); 
    final double cosWallYawAngle = Math.cos(wallYawAngle);
    final double sinWallYawAngle = Math.sin(wallYawAngle);
    double wallXStartWithZeroYaw = cosWallYawAngle * wall.getXStart() + sinWallYawAngle * wall.getYStart();
    double wallXEndWithZeroYaw = cosWallYawAngle * wall.getXEnd() + sinWallYawAngle * wall.getYEnd();
    Float arcExtent = wall.getArcExtent();
    boolean roundWall = arcExtent != null && arcExtent.floatValue() != 0; 
    final double topLineAlpha;
    final double topLineBeta;
    if (topElevationAtStart == topElevationAtEnd) {
      topLineAlpha = 0;
      topLineBeta = topElevationAtStart;
    } else {
      topLineAlpha = (topElevationAtEnd - topElevationAtStart) / (wallXEndWithZeroYaw - wallXStartWithZeroYaw);
      topLineBeta = topElevationAtStart - topLineAlpha * wallXStartWithZeroYaw;
    }

    List<DoorOrWindowArea> windowIntersections = new ArrayList<DoorOrWindowArea>();
    List<HomePieceOfFurniture> intersectingDoorOrWindows = new ArrayList<HomePieceOfFurniture>();
    for (HomePieceOfFurniture piece : getVisibleDoorsAndWindows(this.home.getFurniture())) {
      float pieceElevation = piece.getGroundElevation();
      if (pieceElevation + piece.getHeight() > wallElevation
          && pieceElevation < maxTopElevation) {
        Area pieceArea = new Area(getShape(piece.getPoints()));
        Area intersectionArea = new Area(wallShape);
        intersectionArea.intersect(pieceArea);
        if (!intersectionArea.isEmpty()) {
          if (baseboard != null) {
            double pieceWallAngle = Math.abs(wallYawAngle - piece.getAngle()) % Math.PI;
            if (pieceWallAngle < 1E-5 || (Math.PI - pieceWallAngle) < 1E-5) {
              HomePieceOfFurniture deeperPiece = piece.clone();
              deeperPiece.setDepth(deeperPiece.getDepth() + 2 * baseboard.getThickness());
              pieceArea = new Area(getShape(deeperPiece.getPoints()));
            } 
            intersectionArea = new Area(wallOrBaseboardShape);
            intersectionArea.intersect(pieceArea);
            if (intersectionArea.isEmpty()) {
              continue;
            }
          }
          windowIntersections.add(new DoorOrWindowArea(intersectionArea, Arrays.asList(new HomePieceOfFurniture [] {piece})));
          intersectingDoorOrWindows.add(piece);
          wallOrBaseboardArea.subtract(pieceArea);
        }
      }
    }
    if (windowIntersections.size() > 1) {
      for (int windowIndex = 0; windowIndex < windowIntersections.size(); windowIndex++) {
        DoorOrWindowArea windowIntersection = windowIntersections.get(windowIndex);
        List<DoorOrWindowArea> otherWindowIntersections = new ArrayList<DoorOrWindowArea>();
        int otherWindowIndex = 0;
        for (DoorOrWindowArea otherWindowIntersection : windowIntersections) {          
          if (windowIntersection.getArea().isEmpty()) {
            break;
          } else if (otherWindowIndex > windowIndex) {
            Area windowsIntersectionArea = new Area(otherWindowIntersection.getArea());
            windowsIntersectionArea.intersect(windowIntersection.getArea());
            if (!windowsIntersectionArea.isEmpty()) {
       
              otherWindowIntersection.getArea().subtract(windowsIntersectionArea);              
              windowIntersection.getArea().subtract(windowsIntersectionArea);

              List<HomePieceOfFurniture> doorsOrWindows = new ArrayList<HomePieceOfFurniture>(windowIntersection.getDoorsOrWindows());
              doorsOrWindows.addAll(otherWindowIntersection.getDoorsOrWindows());
              otherWindowIntersections.add(new DoorOrWindowArea(windowsIntersectionArea, doorsOrWindows));
            }
          }
          otherWindowIndex++;
        }
        windowIntersections.addAll(otherWindowIntersections);
      }
    }    
    List<float[]> points = new ArrayList<float[]>(4);
    float [] previousPoint = null;
    for (PathIterator it = wallOrBaseboardArea.getPathIterator(null); !it.isDone(); it.next()) {
      float [] wallPoint = new float[2];
      if (it.currentSegment(wallPoint) == PathIterator.SEG_CLOSE) {
        if (points.size() > 2) {
          if (Arrays.equals(points.get(0), points.get(points.size() - 1))) {
            points.remove(points.size() - 1);
          }
          if (points.size() > 2) {
            float [][] wallPartPoints = points.toArray(new float[points.size()][]);
            sideGeometries.add(createVerticalPartGeometry(wall, wallPartPoints, wallElevation, 
                cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta, baseboard, texture, 
                textureReferencePoint, wallSide));
            bottomGeometries.add(createHorizontalPartGeometry(wallPartPoints, wallElevation, true, roundWall));
            topGeometries.add(createTopPartGeometry(wallPartPoints, 
                cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta, roundWall));
          }
        }
        points.clear();
        previousPoint = null;
      } else if (previousPoint == null
                 || !Arrays.equals(wallPoint, previousPoint)) {
        points.add(wallPoint);
        previousPoint = wallPoint;
      }
    }

    Level level = wall.getLevel();
    previousPoint = null;
    for (DoorOrWindowArea windowIntersection : windowIntersections) {
      if (!windowIntersection.getArea().isEmpty()) {
        for (PathIterator it = windowIntersection.getArea().getPathIterator(null); !it.isDone(); it.next()) {
          float [] wallPoint = new float[2];
          if (it.currentSegment(wallPoint) == PathIterator.SEG_CLOSE) {
            if (Arrays.equals(points.get(0), points.get(points.size() - 1))) {
              points.remove(points.size() - 1);
            }

            if (points.size() > 2) {
              float [][] wallPartPoints = points.toArray(new float[points.size()][]);
              List<HomePieceOfFurniture> doorsOrWindows = windowIntersection.getDoorsOrWindows();
              if (doorsOrWindows.size() > 1) {
                Collections.sort(doorsOrWindows, 
                    new Comparator<HomePieceOfFurniture>() {
                      public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
                        float piece1Elevation = piece1.getGroundElevation();
                        float piece2Elevation = piece2.getGroundElevation();
                        if (piece1Elevation < piece2Elevation) {
                          return -1;
                        } else if (piece1Elevation > piece2Elevation) {
                          return 1;
                        } else {
                          return 0;
                        }
                      }
                    });
              }
              HomePieceOfFurniture lowestDoorOrWindow = doorsOrWindows.get(0);            
              float lowestDoorOrWindowElevation = lowestDoorOrWindow.getGroundElevation();
              if (lowestDoorOrWindowElevation > wallElevation) {
                if (level != null 
                    && level.getElevation() != wallElevation
                    && lowestDoorOrWindow.getElevation() < LEVEL_ELEVATION_SHIFT) {
                  lowestDoorOrWindowElevation -= LEVEL_ELEVATION_SHIFT;
                }
                sideGeometries.add(createVerticalPartGeometry(wall, wallPartPoints, wallElevation, 
                    cosWallYawAngle, sinWallYawAngle, 0, lowestDoorOrWindowElevation, baseboard, texture, 
                    textureReferencePoint, wallSide));
                bottomGeometries.add(createHorizontalPartGeometry(wallPartPoints, wallElevation, true, roundWall));
                sideGeometries.add(createHorizontalPartGeometry(wallPartPoints, 
                    lowestDoorOrWindowElevation, false, roundWall));
              }

              for (int i = 0; i < doorsOrWindows.size() - 1; ) {
                HomePieceOfFurniture lowerDoorOrWindow = doorsOrWindows.get(i);            
                float lowerDoorOrWindowElevation = lowerDoorOrWindow.getGroundElevation();
                HomePieceOfFurniture higherDoorOrWindow = doorsOrWindows.get(++i);
                float higherDoorOrWindowElevation = higherDoorOrWindow.getGroundElevation();
                while (lowerDoorOrWindowElevation + lowerDoorOrWindow.getHeight() >= higherDoorOrWindowElevation + higherDoorOrWindow.getHeight()
                    && ++i < doorsOrWindows.size()) {
                  higherDoorOrWindow = doorsOrWindows.get(i);
                }
                if (i < doorsOrWindows.size()
                    && lowerDoorOrWindowElevation + lowerDoorOrWindow.getHeight() < higherDoorOrWindowElevation) {
                  sideGeometries.add(createVerticalPartGeometry(wall, wallPartPoints, lowerDoorOrWindowElevation + lowerDoorOrWindow.getHeight(), 
                      cosWallYawAngle, sinWallYawAngle, 0, higherDoorOrWindowElevation, baseboard, texture, textureReferencePoint, wallSide));
                  sideGeometries.add(createHorizontalPartGeometry(wallPartPoints, 
                      lowerDoorOrWindowElevation + lowerDoorOrWindow.getHeight(), true, roundWall));
                  sideGeometries.add(createHorizontalPartGeometry(wallPartPoints, higherDoorOrWindowElevation, false, roundWall));
                }
              }
                
              HomePieceOfFurniture highestDoorOrWindow = doorsOrWindows.get(doorsOrWindows.size() - 1);            
              float highestDoorOrWindowElevation = highestDoorOrWindow.getGroundElevation();
              for (int i = doorsOrWindows.size() - 2; i >= 0; i--) {
                HomePieceOfFurniture doorOrWindow = doorsOrWindows.get(i);            
                if (doorOrWindow.getGroundElevation() + doorOrWindow.getHeight() > highestDoorOrWindowElevation + highestDoorOrWindow.getHeight()) {
                  highestDoorOrWindow = doorOrWindow;
                }
              }
              float doorOrWindowTop = highestDoorOrWindowElevation + highestDoorOrWindow.getHeight();
              boolean generateGeometry = true;
              for (int i = 0; i < wallPartPoints.length; i++) {
                double xTopPointWithZeroYaw = cosWallYawAngle * wallPartPoints[i][0] + sinWallYawAngle * wallPartPoints[i][1];
                double topPointWithZeroYawElevation = topLineAlpha * xTopPointWithZeroYaw + topLineBeta;
                if (doorOrWindowTop > topPointWithZeroYawElevation) {
                  if (topLineAlpha == 0 || roundWall) {
                    generateGeometry = false;
                    break;
                  }
                  double translation = (doorOrWindowTop - topPointWithZeroYawElevation) / topLineAlpha;
                  wallPartPoints [i][0] += (float)(translation * cosWallYawAngle);
                  wallPartPoints [i][1] += (float)(translation * sinWallYawAngle);
                }
              }
              if (generateGeometry) {
                sideGeometries.add(createVerticalPartGeometry(wall, wallPartPoints, doorOrWindowTop, 
                    cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta, baseboard, texture, textureReferencePoint, wallSide));
                sideGeometries.add(createHorizontalPartGeometry(
                    wallPartPoints, doorOrWindowTop, true, roundWall));
                topGeometries.add(createTopPartGeometry(wallPartPoints, 
                    cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta, roundWall));
              }
            }
            points.clear();
            previousPoint = null;
          } else if (previousPoint == null
                     || !Arrays.equals(wallPoint, previousPoint)) {
            points.add(wallPoint);
            previousPoint = wallPoint;
          }
        }
      } 
    }
    if (!roundWall && intersectingDoorOrWindows.size() > 0) {
      final double epsilon = Math.PI / 720; 
      final ArrayList<HomePieceOfFurniture> missingModels = new ArrayList<HomePieceOfFurniture>(intersectingDoorOrWindows.size());
      for (final HomePieceOfFurniture doorOrWindow : intersectingDoorOrWindows) {
        if (doorOrWindow instanceof DoorOrWindow
            && !"M0,0 v1 h1 v-1 z".equals(((DoorOrWindow)doorOrWindow).getCutOutShape())) {
          double angleDifference = Math.abs(wallYawAngle - doorOrWindow.getAngle()) % (2 * Math.PI);
          if (angleDifference < epsilon
              || angleDifference > 2 * Math.PI - epsilon
              || Math.abs(angleDifference - Math.PI) < epsilon) {
            final int frontOrBackSide = Math.abs(angleDifference - Math.PI) < epsilon ? 1 : -1;
            ModelRotationTuple rotatedModel = doorOrWindowRotatedModels.get(doorOrWindow);
            if (rotatedModel != null 
                && (missingModels.size() == 0 || !waitDoorOrWindowModelsLoadingEnd)) {
              createGeometriesSurroundingDoorOrWindow(doorOrWindow, rotatedModelsFrontAreas.get(rotatedModel), frontOrBackSide,
                  wall, sideGeometries, topGeometries, 
                  wallSideOrBaseboardPoints, wallElevation, cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta,
                  texture, textureReferencePoint, wallSide);
            } else {
              missingModels.add(doorOrWindow);
            }
          }
        }
      }
      if (missingModels.size() > 0) {
        final ModelManager modelManager = ModelManager.getInstance();
        for (final HomePieceOfFurniture doorOrWindow : (List<HomePieceOfFurniture>)missingModels.clone()) {
          double angleDifference = Math.abs(wallYawAngle - doorOrWindow.getAngle()) % (2 * Math.PI);
          final int frontOrBackSide = Math.abs(angleDifference - Math.PI) < epsilon ? 1 : -1;
          modelManager.loadModel(doorOrWindow.getModel(), waitDoorOrWindowModelsLoadingEnd,
              new ModelManager.ModelObserver() {
                public void modelUpdated(BranchGroup modelRoot) {
                  ModelRotationTuple rotatedModel = doorOrWindowRotatedModels.get(doorOrWindow);
                  Area frontArea;
                  if (rotatedModel == null) {
                    rotatedModel = new ModelRotationTuple(doorOrWindow.getModel(), doorOrWindow.getModelRotation());
                    frontArea = rotatedModelsFrontAreas.get(rotatedModel);
                    if (frontArea == null) {
                      TransformGroup rotation = new TransformGroup(modelManager.getRotationTransformation(doorOrWindow.getModelRotation()));
                      rotation.addChild(modelRoot);
                      frontArea = modelManager.getFrontArea(((DoorOrWindow)doorOrWindow).getCutOutShape(), rotation);
                      rotatedModelsFrontAreas.put(rotatedModel, frontArea);
                    }
                    doorOrWindowRotatedModels.put(doorOrWindow, rotatedModel);
                  } else {
                    frontArea = rotatedModelsFrontAreas.get(rotatedModel);
                  }
                  if (waitDoorOrWindowModelsLoadingEnd) {
                    createGeometriesSurroundingDoorOrWindow(doorOrWindow, frontArea, frontOrBackSide,
                        wall, sideGeometries, topGeometries, 
                        wallSideOrBaseboardPoints, wallElevation, cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta,
                        texture, textureReferencePoint, wallSide);
                  } else {
                    missingModels.remove(doorOrWindow);
                    if (missingModels.size() == 0 
                        && baseboard == null) {
                      EventQueue.invokeLater(new Runnable() {
                          public void run() {
                            updateWallSideGeometry(wallSide, waitDoorOrWindowModelsLoadingEnd);
                          }
                        });
                    }
                  }
                }
                
                public void modelError(Exception ex) {
                  ModelRotationTuple rotatedModel = new ModelRotationTuple(doorOrWindow.getModel(), doorOrWindow.getModelRotation());
                  doorOrWindowRotatedModels.put(doorOrWindow, rotatedModel);
                  if (rotatedModelsFrontAreas.get(rotatedModel) == null) {
                    rotatedModelsFrontAreas.put(rotatedModel, FULL_FACE_CUT_OUT_AREA);
                  }
                  if (!waitDoorOrWindowModelsLoadingEnd) {
                    missingModels.remove(doorOrWindow);
                  }
                }
              });
        }
      }
    }
  }

  private List<HomePieceOfFurniture> getVisibleDoorsAndWindows(List<HomePieceOfFurniture> furniture) {
    List<HomePieceOfFurniture> visibleDoorsAndWindows = new ArrayList<HomePieceOfFurniture>(furniture.size());
    for (HomePieceOfFurniture piece : furniture) {
      if (piece.isVisible()
          && (piece.getLevel() == null
          || piece.getLevel().isViewableAndVisible())) {
        if (piece instanceof HomeFurnitureGroup) {
          visibleDoorsAndWindows.addAll(getVisibleDoorsAndWindows(((HomeFurnitureGroup)piece).getFurniture()));
        } else if (piece.isDoorOrWindow()) {
          visibleDoorsAndWindows.add(piece);
        }
      }
    }
    return visibleDoorsAndWindows;
  }

  private float [][] getWallSidePoints(int wallSide) {
    Wall wall = (Wall)getUserData();
    float [][] wallPoints = wall.getPoints();
    
    if (wallSide == WALL_LEFT_SIDE) {
      for (int i = wallPoints.length / 2; i < wallPoints.length; i++) {
        wallPoints [i][0] = (wallPoints [i][0] + wallPoints [wallPoints.length - i - 1][0]) / 2;
        wallPoints [i][1] = (wallPoints [i][1] + wallPoints [wallPoints.length - i - 1][1]) / 2;
      }
    } else {
      for (int i = 0, n = wallPoints.length / 2; i < n; i++) {
        wallPoints [i][0] = (wallPoints [i][0] + wallPoints [wallPoints.length - i - 1][0]) / 2;
        wallPoints [i][1] = (wallPoints [i][1] + wallPoints [wallPoints.length - i - 1][1]) / 2;
      }
    }
    return wallPoints;
  }

  private float [][] getWallBaseboardPoints(int wallSide) {
    Wall wall = (Wall)getUserData();
    float [][] wallPointsIncludingBaseboards = wall.getPoints(true);
    float [][] wallPoints = wall.getPoints();
    
    if (wallSide == WALL_LEFT_SIDE) {
      for (int i = wallPointsIncludingBaseboards.length / 2; i < wallPointsIncludingBaseboards.length; i++) {
        wallPointsIncludingBaseboards [i] = wallPoints [wallPoints.length - i - 1];
      }
    } else { 
      for (int i = 0, n = wallPoints.length / 2; i < n; i++) {
        wallPointsIncludingBaseboards [i] = wallPoints [wallPoints.length - i - 1];
      }
    }
    return wallPointsIncludingBaseboards;
  }

  private Geometry createVerticalPartGeometry(Wall wall, 
                                              float [][] points, float minElevation, 
                                              double cosWallYawAngle, double sinWallYawAngle, 
                                              double topLineAlpha, double topLineBeta, 
                                              Baseboard baseboard, HomeTexture texture,
                                              float [] textureReferencePoint,
                                              int wallSide) {
    final float subpartSize = this.home.getEnvironment().getSubpartSizeUnderLight();
    Float arcExtent = wall.getArcExtent();
    if ((arcExtent == null || arcExtent == 0) 
        && subpartSize > 0) {
      List<float []> pointsList = new ArrayList<float[]>(points.length * 2);
      pointsList.add(points [0]);
      for (int i = 1; i < points.length; i++) {
        double distance = Point2D.distance(points [i - 1][0], points [i - 1][1], points [i][0], points [i][1]) - subpartSize / 2;
        double angle = Math.atan2(points [i][1] - points [i - 1][1], points [i][0] - points [i - 1][0]);
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        for (double d = 0; d < distance; d += subpartSize) {
          pointsList.add(new float [] {(float)(points [i - 1][0] + d * cosAngle), (float)(points [i - 1][1] + d * sinAngle)});
        }
        pointsList.add(points [i]);
      }
      points = pointsList.toArray(new float [pointsList.size()][]);
    }
    Point3f [] bottom = new Point3f [points.length];
    Point3f [] top    = new Point3f [points.length];
    Float   [] pointUCoordinates = new Float [points.length];
    float xStart = wall.getXStart();
    float yStart = wall.getYStart();
    float xEnd = wall.getXEnd();
    float yEnd = wall.getYEnd();
    float [] arcCircleCenter = null;
    float arcCircleRadius = 0;
    float referencePointAngle = 0;
    if (arcExtent != null && arcExtent != 0) {
      arcCircleCenter = new float [] {wall.getXArcCircleCenter(), wall.getYArcCircleCenter()};
      arcCircleRadius = (float)Point2D.distance(arcCircleCenter [0], arcCircleCenter [1], 
          xStart, yStart);
      referencePointAngle = (float)Math.atan2(textureReferencePoint [1] - arcCircleCenter [1], 
          textureReferencePoint [0] - arcCircleCenter [0]);
    }
    for (int i = 0; i < points.length; i++) {
      bottom [i] = new Point3f(points [i][0], minElevation, points [i][1]);
      float topY = getWallPointElevation(points [i][0], points [i][1], cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta);
      top [i] = new Point3f(points [i][0], topY, points [i][1]);
    }
    double  [] distanceSqToWallMiddle = new double [points.length];
    for (int i = 0; i < points.length; i++) {
      if (arcCircleCenter == null) {
        distanceSqToWallMiddle [i] = Line2D.ptLineDistSq(xStart, yStart, xEnd, yEnd, bottom [i].x, bottom [i].z);
      } else {
        distanceSqToWallMiddle [i] = arcCircleRadius 
            - Point2D.distance(arcCircleCenter [0], arcCircleCenter [1], bottom [i].x, bottom [i].z);
        distanceSqToWallMiddle [i] *= distanceSqToWallMiddle [i];
      }
    }
    int rectanglesCount = points.length;
    boolean [] usedRectangle = new boolean [points.length]; 
    if (baseboard == null) {
    	for (int i = 0; i < points.length - 1; i++) {
        usedRectangle [i] = distanceSqToWallMiddle [i] > 0.001f
            || distanceSqToWallMiddle [i + 1] > 0.001f;
        if (!usedRectangle [i]) {
          rectanglesCount--;
        } 
      }
      usedRectangle [usedRectangle.length - 1] =  distanceSqToWallMiddle [0] > 0.001f
          || distanceSqToWallMiddle [points.length - 1] > 0.001f;
      if (!usedRectangle [usedRectangle.length - 1]) {
        rectanglesCount--;
      }
      if (rectanglesCount == 0) {
        return null;
      }
    } else {
      Arrays.fill(usedRectangle, true);
    }
    
    List<Point3f> coords = new ArrayList<Point3f> (rectanglesCount * 4);
    for (int index = 0; index < points.length; index++) {
      if (usedRectangle [index]) {
        float y = minElevation;
        Point3f point1 = bottom [index];
        int nextIndex = (index + 1) % points.length;
        Point3f point2 = bottom [nextIndex];
        if (subpartSize > 0) {
          for (float yMax = Math.min(top [index].y, top [nextIndex].y) - subpartSize / 2; y < yMax; y += subpartSize) {
            coords.add(point1);
            coords.add(point2);
            point1 = new Point3f(bottom [index].x, y, bottom [index].z);
            point2 = new Point3f(bottom [nextIndex].x, y, bottom [nextIndex].z);
            coords.add(point2);
            coords.add(point1);
          }
        }
        coords.add(point1);
        coords.add(point2);
        coords.add(top [nextIndex]);
        coords.add(top [index]);
      }
    }    
    
    GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
    geometryInfo.setCoordinates(coords.toArray(new Point3f [coords.size()]));
    
    if (texture != null) {
      float halfThicknessSq;
      if (baseboard != null) {
        halfThicknessSq = wall.getThickness() / 2 + baseboard.getThickness();
        halfThicknessSq *= halfThicknessSq;
      } else {
        halfThicknessSq = (wall.getThickness() * wall.getThickness()) / 4;
      }
      TexCoord2f [] textureCoords = new TexCoord2f [coords.size()];
      TexCoord2f firstTextureCoords = new TexCoord2f(0, minElevation);
      int j = 0;
      float epsilon = arcCircleCenter == null 
          ? wall.getThickness() / 1E4f 
          : halfThicknessSq / 4;
      for (int index = 0; index < points.length; index++) {
        if (usedRectangle [index]) {
          int nextIndex = (index + 1) % points.length;
          TexCoord2f textureCoords1;
          TexCoord2f textureCoords2;
          if (Math.abs(distanceSqToWallMiddle [index] - halfThicknessSq) < epsilon
              && Math.abs(distanceSqToWallMiddle [nextIndex] - halfThicknessSq) < epsilon) {
            float firstHorizontalTextureCoords;
            float secondHorizontalTextureCoords;
            if (arcCircleCenter == null) {
              firstHorizontalTextureCoords = (float)Point2D.distance(textureReferencePoint [0], textureReferencePoint [1], 
                  points [index][0], points [index][1]);
              secondHorizontalTextureCoords = (float)Point2D.distance(textureReferencePoint [0], textureReferencePoint [1], 
                  points [nextIndex][0], points [nextIndex][1]);
            } else {
              if (pointUCoordinates [index] == null) {
                float pointAngle = (float)Math.atan2(points [index][1] - arcCircleCenter [1], points [index][0] - arcCircleCenter [0]);
                pointAngle = adjustAngleOnReferencePointAngle(pointAngle, referencePointAngle, arcExtent);
                pointUCoordinates [index] = (pointAngle - referencePointAngle) * arcCircleRadius;
              }
              if (pointUCoordinates [nextIndex] == null) {
                float pointAngle = (float)Math.atan2(points [nextIndex][1] - arcCircleCenter [1], points [nextIndex][0] - arcCircleCenter [0]);
                pointAngle = adjustAngleOnReferencePointAngle(pointAngle, referencePointAngle, arcExtent);
                pointUCoordinates [nextIndex] = (pointAngle - referencePointAngle) * arcCircleRadius;
              }
              
              firstHorizontalTextureCoords = pointUCoordinates [index];
              secondHorizontalTextureCoords = pointUCoordinates [nextIndex];
            }
            if (wallSide == WALL_LEFT_SIDE && texture.isLeftToRightOriented()) {
              firstHorizontalTextureCoords = -firstHorizontalTextureCoords;
              secondHorizontalTextureCoords = -secondHorizontalTextureCoords;
            }

            textureCoords1 = new TexCoord2f(firstHorizontalTextureCoords, minElevation);
            textureCoords2 = new TexCoord2f(secondHorizontalTextureCoords, minElevation);
          } else {
            textureCoords1 = firstTextureCoords;
            float horizontalTextureCoords = (float)Point2D.distance(points [index][0], points [index][1], 
                points [nextIndex][0], points [nextIndex][1]);
            textureCoords2 = new TexCoord2f(horizontalTextureCoords, minElevation);
          }
          
          if (subpartSize > 0) {
            float y = minElevation;
            for (float yMax = Math.min(top [index].y, top [nextIndex].y) - subpartSize / 2; y < yMax; y += subpartSize) {
              textureCoords [j++] = textureCoords1;
              textureCoords [j++] = textureCoords2;
              textureCoords1 = new TexCoord2f(textureCoords1.x, y);
              textureCoords2 = new TexCoord2f(textureCoords2.x, y);
              textureCoords [j++] = textureCoords2;
              textureCoords [j++] = textureCoords1;
            }
          }
          textureCoords [j++] = textureCoords1;
          textureCoords [j++] = textureCoords2;
          textureCoords [j++] = new TexCoord2f(textureCoords2.x, top [nextIndex].y);
          textureCoords [j++] = new TexCoord2f(textureCoords1.x, top [index].y);
        }
      }
      geometryInfo.setTextureCoordinateParams(1, 2);
      geometryInfo.setTextureCoordinates(0, textureCoords);
    }

    NormalGenerator normalGenerator = new NormalGenerator();
    if (arcCircleCenter == null) {
      normalGenerator.setCreaseAngle(0);
    }
    normalGenerator.generateNormals(geometryInfo);
    return geometryInfo.getIndexedGeometryArray();
  }

  private float getWallPointElevation(float xWallPoint, float yWallPoint, 
                                      double cosWallYawAngle, double sinWallYawAngle,
                                      double topLineAlpha, double topLineBeta) {
    double xTopPointWithZeroYaw = cosWallYawAngle * xWallPoint + sinWallYawAngle * yWallPoint;
    return (float)(topLineAlpha * xTopPointWithZeroYaw + topLineBeta);
  }

  private float adjustAngleOnReferencePointAngle(float pointAngle, float referencePointAngle, float arcExtent) {
    if (arcExtent > 0) {
      if ((referencePointAngle > 0 
          && (pointAngle < 0
              || referencePointAngle > pointAngle))
        || (referencePointAngle < 0 
            && pointAngle < 0 
            && referencePointAngle > pointAngle)) {
        pointAngle += 2 * (float)Math.PI;
      }
    } else {
      if ((referencePointAngle < 0 
            && (pointAngle > 0
                || referencePointAngle < pointAngle))
          || (referencePointAngle > 0 
              && pointAngle > 0 
              && referencePointAngle < pointAngle)) {
        pointAngle -= 2 * (float)Math.PI;
      }
    }
    return pointAngle;
  }

  private Geometry createHorizontalPartGeometry(float [][] points, float y, 
                                                boolean reverseOrder, boolean roundWall) {
    Point3f [] coords = new Point3f [points.length];
    for (int i = 0; i < points.length; i++) {
      coords [i] = new Point3f(points [i][0], y, points [i][1]);
    }
    GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
    geometryInfo.setCoordinates (coords);
    geometryInfo.setStripCounts(new int [] {coords.length});
    if (reverseOrder) {
      geometryInfo.reverse();
    }
    NormalGenerator normalGenerator = new NormalGenerator();
    if (roundWall) {
      normalGenerator.setCreaseAngle(0);
    }
    normalGenerator.generateNormals(geometryInfo);
    return geometryInfo.getIndexedGeometryArray ();
  }

  private Geometry createTopPartGeometry(float [][] points, 
                                         double cosWallYawAngle, double sinWallYawAngle, 
                                         double topLineAlpha, double topLineBeta, 
                                         boolean roundWall) {
    Point3f [] coords = new Point3f [points.length];
    for (int i = 0; i < points.length; i++) {
      double xTopPointWithZeroYaw = cosWallYawAngle * points [i][0] + sinWallYawAngle * points [i][1];
      float topY = (float)(topLineAlpha * xTopPointWithZeroYaw + topLineBeta);
      coords [i] = new Point3f(points [i][0], topY, points [i][1]);
    }
    GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
    geometryInfo.setCoordinates (coords);
    geometryInfo.setStripCounts(new int [] {coords.length});
    NormalGenerator normalGenerator = new NormalGenerator();
    if (roundWall) {
      normalGenerator.setCreaseAngle(0);
    }
    normalGenerator.generateNormals(geometryInfo);
    return geometryInfo.getIndexedGeometryArray ();
  }

  private void createGeometriesSurroundingDoorOrWindow(HomePieceOfFurniture doorOrWindow, 
                                                       Area doorOrWindowFrontArea,
                                                       float frontOrBackSide,
                                                       Wall wall, List<Geometry> wallGeometries, 
                                                       List<Geometry> wallTopGeometries,
                                                       float [][] wallSidePoints, 
                                                       float wallElevation, 
                                                       double cosWallYawAngle, double sinWallYawAngle, 
                                                       double topLineAlpha, double topLineBeta,
                                                       HomeTexture texture, float [] textureReferencePoint, 
                                                       int wallSide) {
    Area fullFaceArea = new Area(FULL_FACE_CUT_OUT_AREA);
    fullFaceArea.subtract(doorOrWindowFrontArea);
    if (!fullFaceArea.isEmpty()) {
      float doorOrWindowDepth = doorOrWindow.getDepth();
      float xPieceSide = (float)(doorOrWindow.getX() - frontOrBackSide * doorOrWindowDepth / 2 * Math.sin(doorOrWindow.getAngle()));
      float yPieceSide = (float)(doorOrWindow.getY() + frontOrBackSide * doorOrWindowDepth / 2 * Math.cos(doorOrWindow.getAngle()));
      float [] wallFirstPoint = wallSide == WALL_LEFT_SIDE 
          ? wallSidePoints [0]
          : wallSidePoints [wallSidePoints.length - 1];
      float [] wallSecondPoint = wallSide == WALL_LEFT_SIDE 
          ? wallSidePoints [wallSidePoints.length / 2 - 1]
          : wallSidePoints [wallSidePoints.length / 2];
      float frontSideToWallDistance = (float)Line2D.ptLineDist(wallFirstPoint [0], wallFirstPoint [1], 
              wallSecondPoint [0], wallSecondPoint [1], xPieceSide, yPieceSide);
      float position = (float)Line2D.relativeCCW(wallFirstPoint [0], wallFirstPoint [1], 
          wallSecondPoint [0], wallSecondPoint [1], xPieceSide, yPieceSide);
      float depthTranslation = frontOrBackSide * (0.5f - position * frontSideToWallDistance / doorOrWindowDepth);

      Transform3D frontAreaTransform = ModelManager.getInstance().getPieceOFFurnitureNormalizedModelTransformation(doorOrWindow);       
      Transform3D frontAreaTranslation = new Transform3D();
      frontAreaTranslation.setTranslation(new Vector3f(0, 0, depthTranslation));
      frontAreaTransform.mul(frontAreaTranslation);
  
      Transform3D invertedFrontAreaTransform = new Transform3D();
      invertedFrontAreaTransform.invert(frontAreaTransform);    
      GeneralPath wallPath = new GeneralPath();
      Point3f wallPoint = new Point3f(wallFirstPoint [0], wallElevation, wallFirstPoint [1]);
      invertedFrontAreaTransform.transform(wallPoint);
      wallPath.moveTo(wallPoint.x, wallPoint.y);
      wallPoint = new Point3f(wallSecondPoint [0], wallElevation, wallSecondPoint [1]);
      invertedFrontAreaTransform.transform(wallPoint);
      wallPath.lineTo(wallPoint.x, wallPoint.y);
      Point3f topWallPoint1 = new Point3f(wallSecondPoint [0], 
          getWallPointElevation(wallSecondPoint [0], wallSecondPoint [1], 
              cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta), wallSecondPoint [1]);
      invertedFrontAreaTransform.transform(topWallPoint1);
      wallPath.lineTo(topWallPoint1.x, topWallPoint1.y);
      Point3f topWallPoint2 = new Point3f(wallFirstPoint [0], 
          getWallPointElevation(wallFirstPoint [0], wallFirstPoint [1], 
              cosWallYawAngle, sinWallYawAngle, topLineAlpha, topLineBeta), wallFirstPoint [1]);
      invertedFrontAreaTransform.transform(topWallPoint2);
      wallPath.lineTo(topWallPoint2.x, topWallPoint2.y);
      wallPath.closePath();
  
      GeneralPath doorOrWindowSurroundingPath = new GeneralPath();
      doorOrWindowSurroundingPath.moveTo(-.5f, -.5f);
      doorOrWindowSurroundingPath.lineTo(-.5f,  .5f);
      doorOrWindowSurroundingPath.lineTo( .5f, .5f);
      doorOrWindowSurroundingPath.lineTo( .5f, -.5f);
      doorOrWindowSurroundingPath.closePath();

      Area doorOrWindowSurroundingArea = new Area(doorOrWindowSurroundingPath);
      doorOrWindowSurroundingArea.intersect(new Area(wallPath));
      doorOrWindowSurroundingArea.subtract(doorOrWindowFrontArea);
      float flatness = 0.5f / (Math.max(doorOrWindow.getWidth(), doorOrWindow.getHeight()));
      if (!doorOrWindowSurroundingArea.isEmpty()) {
        boolean reversed = frontOrBackSide > 0 ^ wallSide == WALL_RIGHT_SIDE ^ doorOrWindow.isModelMirrored();
        List<float [][]> doorOrWindowSurroundingAreasPoints = getAreaPoints(doorOrWindowSurroundingArea, flatness, reversed);     
        if (!doorOrWindowSurroundingAreasPoints.isEmpty()) {
          int [] stripCounts = new int [doorOrWindowSurroundingAreasPoints.size()];
          int vertexCount = 0;
          for (int i = 0; i < doorOrWindowSurroundingAreasPoints.size(); i++) {
            float [][] areaPoints = doorOrWindowSurroundingAreasPoints.get(i);
            stripCounts [i] = areaPoints.length + 1;
            vertexCount += stripCounts [i]; 
          }
          float halfWallThickness = wall.getThickness() / 2;
          float deltaXToWallMiddle = (float)(halfWallThickness * sinWallYawAngle);
          float deltaZToWallMiddle = -(float)(halfWallThickness * cosWallYawAngle);
          if (wallSide == WALL_LEFT_SIDE) {
            deltaXToWallMiddle *= -1;
            deltaZToWallMiddle *= -1;
          }
          Point3f [] coords = new Point3f [vertexCount];
          List<Point3f> borderCoords = new ArrayList<Point3f>(4 * vertexCount);
          List<Point3f> slopingTopCoords = new ArrayList<Point3f>();        
          TexCoord2f [] textureCoords;
          List<TexCoord2f> borderTextureCoords;
          if (texture != null) {
            textureCoords = new TexCoord2f [coords.length];
            borderTextureCoords = new ArrayList<TexCoord2f>(4 * vertexCount);
          } else {
            textureCoords = null;
            borderTextureCoords = null;
          }
          int i = 0;
          for (float [][] areaPoints : doorOrWindowSurroundingAreasPoints) {
            Point3f point = new Point3f(areaPoints [0][0], areaPoints [0][1], 0);
            frontAreaTransform.transform(point);
            TexCoord2f textureCoord = null;
            if (texture != null) {
              float horizontalTextureCoords = (float)Point2D.distance(textureReferencePoint [0], textureReferencePoint [1], 
                  point.x, point.z);
              if (wallSide == WALL_LEFT_SIDE && texture.isLeftToRightOriented()) {
                horizontalTextureCoords = -horizontalTextureCoords;
              }
              textureCoord = new TexCoord2f(horizontalTextureCoords, point.y);
            }
            double distanceToTop = Line2D.ptLineDistSq(topWallPoint1.x, topWallPoint1.y, topWallPoint2.x, topWallPoint2.y, 
                areaPoints [0][0], areaPoints [0][1]);
            
            for (int j = 0; j < areaPoints.length; j++, i++) {
              coords [i] = point;
              if (texture != null) {
                textureCoords [i] = textureCoord;
              }

              int nextPointIndex = j < areaPoints.length - 1  
                  ? j + 1
                  : 0;
              List<Point3f> coordsList;
              double nextDistanceToTop = Line2D.ptLineDistSq(topWallPoint1.x, topWallPoint1.y, topWallPoint2.x, topWallPoint2.y, 
                  areaPoints [nextPointIndex][0], areaPoints [nextPointIndex][1]);
              if (distanceToTop < 1E-10 && nextDistanceToTop < 1E-10) {
                coordsList = slopingTopCoords;
              } else {
                coordsList = borderCoords;
              }
              
              Point3f nextPoint = new Point3f(areaPoints [nextPointIndex][0], areaPoints [nextPointIndex][1], 0);
              frontAreaTransform.transform(nextPoint);            
              coordsList.add(point);
              coordsList.add(new Point3f(point.x + deltaXToWallMiddle, point.y, point.z + deltaZToWallMiddle));
              coordsList.add(new Point3f(nextPoint.x + deltaXToWallMiddle, nextPoint.y, nextPoint.z + deltaZToWallMiddle));
              coordsList.add(nextPoint);
              
              TexCoord2f nextTextureCoord = null;
              if (texture != null) {
                float horizontalTextureCoords = (float)Point2D.distance(textureReferencePoint [0], textureReferencePoint [1], 
                    nextPoint.x, nextPoint.z);
                if (wallSide == WALL_LEFT_SIDE && texture.isLeftToRightOriented()) {
                  horizontalTextureCoords = -horizontalTextureCoords;
                }
                nextTextureCoord = new TexCoord2f(horizontalTextureCoords, nextPoint.y);
                if (coordsList == borderCoords) {
                  borderTextureCoords.add(textureCoord);
                  borderTextureCoords.add(textureCoord);
                  borderTextureCoords.add(nextTextureCoord);
                  borderTextureCoords.add(nextTextureCoord);
                }
              }

              distanceToTop = nextDistanceToTop;
              point = nextPoint;
              textureCoord = nextTextureCoord;
            }

            coords [i] = point;
            if (texture != null) {
              textureCoords [i] = textureCoord;
            }
            i++;
          }

          GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
          geometryInfo.setStripCounts(stripCounts);
          geometryInfo.setCoordinates(coords);
          if (texture != null) {
            geometryInfo.setTextureCoordinateParams(1, 2);
            geometryInfo.setTextureCoordinates(0, textureCoords);
          }
          new NormalGenerator().generateNormals(geometryInfo);
          wallGeometries.add(geometryInfo.getIndexedGeometryArray());
        
          if (borderCoords.size() > 0) { 
            geometryInfo = new GeometryInfo(GeometryInfo.QUAD_ARRAY);        
            geometryInfo.setCoordinates(borderCoords.toArray(new Point3f [borderCoords.size()]));          
            if (texture != null) {
              geometryInfo.setTextureCoordinateParams(1, 2);
              geometryInfo.setTextureCoordinates(0, borderTextureCoords.toArray(new TexCoord2f [borderTextureCoords.size()]));
            }
            new NormalGenerator(Math.PI / 2).generateNormals(geometryInfo);
            wallGeometries.add(geometryInfo.getIndexedGeometryArray());
          }
          
          if (slopingTopCoords.size() > 0) { 
            geometryInfo = new GeometryInfo(GeometryInfo.QUAD_ARRAY);        
            geometryInfo.setCoordinates(slopingTopCoords.toArray(new Point3f [slopingTopCoords.size()]));
            new NormalGenerator().generateNormals(geometryInfo);
            wallTopGeometries.add(geometryInfo.getIndexedGeometryArray());
          }
        }
      }
    }
  }

    private float getWallElevation(boolean ignoreFloorThickness) {
    Wall wall = (Wall)getUserData();      
    Level level = wall.getLevel();
    if (level == null) {
      return 0;
    } else if (ignoreFloorThickness) {
      return level.getElevation();
    } else {
      float floorThicknessBottomWall = getFloorThicknessBottomWall();
      if (floorThicknessBottomWall > 0) {
        floorThicknessBottomWall -= LEVEL_ELEVATION_SHIFT;
      }
      return level.getElevation() - floorThicknessBottomWall;
    }
  }

  private float getFloorThicknessBottomWall() {
    Wall wall = (Wall)getUserData();      
    Level level = wall.getLevel();
    if (level == null) {
      return 0;
    } else {
      List<Level> levels = this.home.getLevels();
      if (!levels.isEmpty() && levels.get(0).getElevation() == level.getElevation()) {
        return 0;
      } else {
        return level.getFloorThickness();
      }
    }
  }

  private float getWallTopElevationAtStart() {
    Float wallHeight = ((Wall)getUserData()).getHeight();      
    float wallHeightAtStart;
    if (wallHeight != null) {
      wallHeightAtStart = wallHeight + getWallElevation(false) + getFloorThicknessBottomWall();
    } else {
      wallHeightAtStart = this.home.getWallHeight() + getWallElevation(false) + getFloorThicknessBottomWall();
    }
    return wallHeightAtStart + getTopElevationShift();
  }
  
  private float getTopElevationShift() {
    Level level = ((Wall)getUserData()).getLevel();
    if (level != null) {
      List<Level> levels = this.home.getLevels();
      if (levels.get(levels.size() - 1) != level) {
        return LEVEL_ELEVATION_SHIFT;
      }
    }
    return 0;
  }

  private float getWallTopElevationAtEnd() {
    Wall wall = (Wall)getUserData();      
    if (wall.isTrapezoidal()) {
      return wall.getHeightAtEnd() + getWallElevation(false) + getFloorThicknessBottomWall() + getTopElevationShift();
    } else {
      return getWallTopElevationAtStart();
    }
  }

  private float getBaseboardTopElevation(Baseboard baseboard) {
    return baseboard.getHeight() + getWallElevation(true);
  }

  private void updateWallAppearance(boolean waitTextureLoadingEnd) {
    Wall wall = (Wall)getUserData();
    Integer wallsTopColor = wall.getTopColor();
    Group [] wallLeftSideGroups  = {(Group)getChild(0),
                                    (Group)getChild(2), 
                                    (Group)getChild(4), 
                                    (Group)getChild(6)};
    Group [] wallRightSideGroups = {(Group)getChild(1), 
                                    (Group)getChild(3),
                                    (Group)getChild(5),
                                    (Group)getChild(7)};
    for (int i = 0; i < wallLeftSideGroups.length; i++) {
      if (i == 1) {
        Baseboard leftSideBaseboard = wall.getLeftSideBaseboard();
        if (leftSideBaseboard != null) {
          HomeTexture texture = leftSideBaseboard.getTexture();
          Integer color = leftSideBaseboard.getColor();
          if (color == null && texture == null) {
            texture = wall.getLeftSideTexture();
            color = wall.getLeftSideColor();
          }
          updateFilledWallSideAppearance(((Shape3D)wallLeftSideGroups [i].getChild(0)).getAppearance(), 
              texture, waitTextureLoadingEnd, color, wall.getLeftSideShininess());
        }
        Baseboard rightSideBaseboard = wall.getRightSideBaseboard();
        if (rightSideBaseboard != null) {
          HomeTexture texture = rightSideBaseboard.getTexture();
          Integer color = rightSideBaseboard.getColor();
          if (color == null && texture == null) {
            texture = wall.getRightSideTexture();
            color = wall.getRightSideColor();
          }
          updateFilledWallSideAppearance(((Shape3D)wallRightSideGroups [i].getChild(0)).getAppearance(), 
              texture, waitTextureLoadingEnd, color, wall.getRightSideShininess());
        }
      } else if (i != 3 || wallsTopColor == null) {
        updateFilledWallSideAppearance(((Shape3D)wallLeftSideGroups [i].getChild(0)).getAppearance(), 
            wall.getLeftSideTexture(), waitTextureLoadingEnd, wall.getLeftSideColor(), wall.getLeftSideShininess());
        updateFilledWallSideAppearance(((Shape3D)wallRightSideGroups [i].getChild(0)).getAppearance(), 
            wall.getRightSideTexture(), waitTextureLoadingEnd, wall.getRightSideColor(), wall.getRightSideShininess());
      } else {
        updateFilledWallSideAppearance(((Shape3D)wallLeftSideGroups [i].getChild(0)).getAppearance(), 
            null, waitTextureLoadingEnd, wallsTopColor, 0);
        updateFilledWallSideAppearance(((Shape3D)wallRightSideGroups [i].getChild(0)).getAppearance(), 
            null, waitTextureLoadingEnd, wallsTopColor, 0);
      }
      if (wallLeftSideGroups [i].numChildren() > 1) {
        updateOutlineWallSideAppearance(((Shape3D)wallLeftSideGroups [i].getChild(1)).getAppearance());
        updateOutlineWallSideAppearance(((Shape3D)wallRightSideGroups [i].getChild(1)).getAppearance());
      }
    }
  }

  private void updateFilledWallSideAppearance(final Appearance wallSideAppearance, 
                                              final HomeTexture wallSideTexture,
                                              boolean waitTextureLoadingEnd,
                                              Integer wallSideColor, 
                                              float shininess) {
    if (wallSideTexture == null) {
      wallSideAppearance.setMaterial(getMaterial(wallSideColor, wallSideColor, shininess));
      wallSideAppearance.setTexture(null);
    } else {
      wallSideAppearance.setMaterial(getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, shininess));
      wallSideAppearance.setTextureAttributes(getTextureAttributes(wallSideTexture, true));
      final TextureManager textureManager = TextureManager.getInstance();
      textureManager.loadTexture(wallSideTexture.getImage(), waitTextureLoadingEnd,
          new TextureManager.TextureObserver() {
              public void textureUpdated(Texture texture) {
                wallSideAppearance.setTexture(getHomeTextureClone(texture, home));
              }
            });
    }
    float wallsAlpha = this.home.getEnvironment().getWallsAlpha();
    TransparencyAttributes transparencyAttributes = wallSideAppearance.getTransparencyAttributes();
    transparencyAttributes.setTransparency(wallsAlpha);
    transparencyAttributes.setTransparencyMode(wallsAlpha == 0 
        ? TransparencyAttributes.NONE 
        : TransparencyAttributes.NICEST);      
    RenderingAttributes renderingAttributes = wallSideAppearance.getRenderingAttributes();
    HomeEnvironment.DrawingMode drawingMode = this.home.getEnvironment().getDrawingMode();
    renderingAttributes.setVisible(drawingMode == null
        || drawingMode == HomeEnvironment.DrawingMode.FILL 
        || drawingMode == HomeEnvironment.DrawingMode.FILL_AND_OUTLINE);
  }

  private void updateOutlineWallSideAppearance(final Appearance wallSideAppearance) {
    RenderingAttributes renderingAttributes = wallSideAppearance.getRenderingAttributes();
    HomeEnvironment.DrawingMode drawingMode = this.home.getEnvironment().getDrawingMode();
    renderingAttributes.setVisible(drawingMode == HomeEnvironment.DrawingMode.OUTLINE 
        || drawingMode == HomeEnvironment.DrawingMode.FILL_AND_OUTLINE);
  }

  private static class DoorOrWindowArea {
    private final Area area;
    private final List<HomePieceOfFurniture> doorsOrWindows;
    
    public DoorOrWindowArea(Area area, List<HomePieceOfFurniture> doorsOrWindows) {
      this.area = area;
      this.doorsOrWindows = doorsOrWindows;      
    }
    
    public Area getArea() {
      return this.area;
    }
    
    public List<HomePieceOfFurniture> getDoorsOrWindows() {
      return this.doorsOrWindows;
    }
  }

  private static class ModelRotationTuple {
    private Content    model;
    private float [][] rotation;

    public ModelRotationTuple(Content model, float [][] rotation) {
      this.model = model;
      this.rotation = rotation;
    }
    
    @Override
    public int hashCode() {
      int hashCode = 31 * this.model.hashCode();
      for (float [] table : this.rotation) {
        hashCode += Arrays.hashCode(table);
      }
      return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj instanceof ModelRotationTuple) {
        ModelRotationTuple tuple = (ModelRotationTuple)obj;
        if (this.model.equals(tuple.model)
            && this.rotation.length == tuple.rotation.length) {
          for (int i = 0; i < this.rotation.length; i++) {
            if (!Arrays.equals(this.rotation [i], tuple.rotation [i])) {
              return false;
            }
          }
          return true;
        }
      }
      return false;
    }
  }
}