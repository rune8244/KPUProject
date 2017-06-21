package com.eteks.homeview3d.io;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.eteks.homeview3d.model.AspectRatio;
import com.eteks.homeview3d.model.BackgroundImage;
import com.eteks.homeview3d.model.Baseboard;
import com.eteks.homeview3d.model.Camera;
import com.eteks.homeview3d.model.CatalogDoorOrWindow;
import com.eteks.homeview3d.model.CatalogLight;
import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.Compass;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.DimensionLine;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeDoorOrWindow;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.HomeFurnitureGroup;
import com.eteks.homeview3d.model.HomeLight;
import com.eteks.homeview3d.model.HomeMaterial;
import com.eteks.homeview3d.model.HomeObject;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomePrint;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.Label;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.LightSource;
import com.eteks.homeview3d.model.ObserverCamera;
import com.eteks.homeview3d.model.Polyline;
import com.eteks.homeview3d.model.Room;
import com.eteks.homeview3d.model.Sash;
import com.eteks.homeview3d.model.TextStyle;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.model.Wall;
import com.eteks.homeview3d.tools.ResourceURLContent;

public class HomeXMLHandler extends DefaultHandler {
  private HomeContentContext contentContext;
  private UserPreferences    preferences;
  private Home               home;
  
  private final StringBuilder     buffer  = new StringBuilder();
  private final Stack<String>     elements = new Stack<String>();
  private final Stack<Map<String, String>> attributes = new Stack<Map<String, String>>();
  private final Stack<List<HomePieceOfFurniture>> groupsFurniture = new Stack<List<HomePieceOfFurniture>>();
  private final Map<String, Level>      levels = new HashMap<String, Level>();
  private final Map<String, JoinedWall> joinedWalls  = new HashMap<String, JoinedWall>();
  
  private String homeElementName;
  private String labelText;
  private Baseboard leftSideBaseboard;
  private Baseboard rightSideBaseboard;
  private BackgroundImage homeBackgroundImage;
  private BackgroundImage backgroundImage;
  private final Map<String, String> homeProperties = new LinkedHashMap<String, String>();
  private final Map<String, String> properties = new LinkedHashMap<String, String>();
  private final Map<String, TextStyle>    textStyles = new HashMap<String, TextStyle>();
  private final Map<String, HomeTexture>  textures = new HashMap<String, HomeTexture>();
  private final List<HomeMaterial> materials = new ArrayList<HomeMaterial>();
  private HomeTexture materialTexture;
  private final List<Sash>         sashes = new ArrayList<Sash>();
  private final List<LightSource>  lightSources = new ArrayList<LightSource>();
  private final List<float[]>      points = new ArrayList<float[]>();
  private final List<HomePieceOfFurniture.SortableProperty> furnitureVisibleProperties = new ArrayList<HomePieceOfFurniture.SortableProperty>();
  
  public HomeXMLHandler() {
    this(null);
  }

  public HomeXMLHandler(UserPreferences preferences) {
    this.preferences = preferences != null ? preferences : new DefaultUserPreferences(false, null);
  }

  void setContentContext(HomeContentContext contentContext) {
    this.contentContext = contentContext;
  }
  
  @Override
  public void startDocument() throws SAXException {
    this.home = null;
    this.elements.clear();
    this.attributes.clear();
    this.groupsFurniture.clear();
    this.levels.clear();
    this.joinedWalls.clear();
  }

  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    this.buffer.setLength(0);
    this.elements.push(name);
    Map<String, String> attributesMap = new HashMap<String, String>();
    for (int i = 0; i < attributes.getLength(); i++) {
      attributesMap.put(attributes.getQName(i), attributes.getValue(i));
    }
    this.attributes.push(attributesMap);
    
    if ("home".equals(name)) {
      setHome(createHome(attributesMap));
      this.homeProperties.clear();
      this.furnitureVisibleProperties.clear();
      this.homeBackgroundImage = null;
    } else if ("environment".equals(name)) {
      this.textures.clear();
    } else if ("compass".equals(name)) {
      this.properties.clear();
    } else if ("level".equals(name)) {
      this.properties.clear();
      this.backgroundImage = null;
    } else if ("pieceOfFurniture".equals(name)
              || "doorOrWindow".equals(name)
              || "light".equals(name)
              || "furnitureGroup".equals(name)) {
      this.properties.clear();
      this.textStyles.clear();
      this.textures.clear();
      this.materials.clear();
      this.sashes.clear();
      this.lightSources.clear();
      if ("furnitureGroup".equals(name)) {
        this.groupsFurniture.push(new ArrayList<HomePieceOfFurniture>());
      }
    } else if ("camera".equals(name)
        || "observerCamera".equals(name)) {
      this.properties.clear();
    } else if ("room".equals(name)) {
      this.properties.clear();
      this.textStyles.clear();
      this.textures.clear();
      this.points.clear();
    } else if ("polyline".equals(name)) {
      this.properties.clear();
      this.points.clear();
    } else if ("dimensionLine".equals(name)) {
      this.properties.clear();
      this.textStyles.clear();
    } else if ("label".equals(name)) {
      this.properties.clear();
      this.textStyles.clear();
      this.labelText = null;
    } else if ("wall".equals(name)) {
      this.properties.clear();
      this.textures.clear();
      this.leftSideBaseboard = null;
      this.rightSideBaseboard = null;
    } else if ("baseboard".equals(name)) {
      this.textures.remove(null);
    } else if ("material".equals(name)) {
      this.materialTexture = null;
    }
  }

  @Override
  public void characters(char [] ch, int start, int length) throws SAXException {
    this.buffer.append(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    this.elements.pop();
    String parent = this.elements.isEmpty() ? null : this.elements.peek();
    Map<String, String> attributesMap = this.attributes.pop();
    if (this.homeElementName != null && this.homeElementName.equals(name)) {
      setHomeAttributes(home, name, attributesMap);
    } else if ("furnitureVisibleProperty".equals(name)) {
      try {
        if (attributesMap.get("name") == null) {
          throw new SAXException("Missing name attribute");
        }
        this.furnitureVisibleProperties.add(HomePieceOfFurniture.SortableProperty.valueOf(attributesMap.get("name")));
      } catch (IllegalArgumentException ex) {
        // Ignore malformed enum constant 
      }
    } else if ("environment".equals(name)) {
      setEnvironmentAttributes(this.home.getEnvironment(), name, attributesMap);
    } else if ("compass".equals(name)) {
      setCompassAttributes(this.home.getCompass(), name, attributesMap);
    } else if ("print".equals(name)) {
      this.home.setPrint(createPrint(attributesMap));
    } else if ("level".equals(name)) {
      Level level = createLevel(attributesMap);
      setLevelAttributes(level, name, attributesMap);
      this.levels.put(attributesMap.get("id"), level);
      this.home.addLevel(level);
    } else if ("camera".equals(name)
        || "observerCamera".equals(name)) {
      Camera camera = createCamera(name, attributesMap);
      setCameraAttributes(camera, name, attributesMap);
      String attribute = attributesMap.get("attribute");
      if ("cameraPath".equals(attribute)) {
        // Update camera path
        List<Camera> cameraPath = new ArrayList<Camera>(this.home.getEnvironment().getVideoCameraPath());
        cameraPath.add(camera);
        this.home.getEnvironment().setVideoCameraPath(cameraPath);
      } else if ("topCamera".equals(attribute)) {
        Camera topCamera = this.home.getTopCamera();
        topCamera.setCamera(camera);
        topCamera.setTime(camera.getTime());
        topCamera.setLens(camera.getLens());
      } else if ("observerCamera".equals(attribute)) {
        ObserverCamera observerCamera = this.home.getObserverCamera();
        observerCamera.setCamera(camera);
        observerCamera.setTime(camera.getTime());
        observerCamera.setLens(camera.getLens());
        observerCamera.setFixedSize(((ObserverCamera)camera).isFixedSize());
      } else if ("storedCamera".equals(attribute)) {
        List<Camera> storedCameras = new ArrayList<Camera>(this.home.getStoredCameras());
        storedCameras.add(camera);
        this.home.setStoredCameras(storedCameras);
      } 
    } else if ("pieceOfFurniture".equals(name)
        || "doorOrWindow".equals(name)
        || "light".equals(name)
        || "furnitureGroup".equals(name)) {
      HomePieceOfFurniture piece = "furnitureGroup".equals(name)
          ? createFurnitureGroup(attributesMap, this.groupsFurniture.pop())
          : createPieceOfFurniture(name, attributesMap);
      setPieceOfFurnitureAttributes(piece, name, attributesMap);
      if (this.homeElementName != null && this.homeElementName.equals(parent)) {
        this.home.addPieceOfFurniture(piece);
        String levelId = attributesMap.get("level");
        if (levelId != null) {
          piece.setLevel(this.levels.get(levelId));
        }
      } else if ("furnitureGroup".equals(parent)) {
        this.groupsFurniture.peek().add(piece);
        this.properties.clear();
        this.textStyles.clear();
      }
    } else if ("wall".equals(name)) {
      Wall wall = createWall(attributesMap);
      this.joinedWalls.put(attributesMap.get("id"), 
          new JoinedWall(wall, attributesMap.get("wallAtStart"), attributesMap.get("wallAtEnd")));
      setWallAttributes(wall, name, attributesMap);
      this.home.addWall(wall);
      String levelId = attributesMap.get("level");
      if (levelId != null) {
        wall.setLevel(this.levels.get(levelId));
      }
    } else if ("baseboard".equals(name)) {
      Baseboard baseboard = createBaseboard(attributesMap);
      if ("leftSideBaseboard".equals(attributesMap.get("attribute"))) {
        this.leftSideBaseboard = baseboard;
      } else {
        this.rightSideBaseboard = baseboard;
      }
    } else if ("room".equals(name)) {
      Room room = createRoom(attributesMap, this.points.toArray(new float [this.points.size()][]));
      setRoomAttributes(room, name, attributesMap);
      this.home.addRoom(room);
      String levelId = attributesMap.get("level");
      if (levelId != null) {
        room.setLevel(this.levels.get(levelId));
      }
    } else if ("polyline".equals(name)) {
      Polyline polyline = createPolyline(attributesMap, this.points.toArray(new float [this.points.size()][]));
      setPolylineAttributes(polyline, name, attributesMap);
      this.home.addPolyline(polyline);
      String levelId = attributesMap.get("level");
      if (levelId != null) {
        polyline.setLevel(this.levels.get(levelId));
      }
    } else if ("dimensionLine".equals(name)) {
      DimensionLine dimensionLine = createDimensionLine(attributesMap);
      setDimensionLineAttributes(dimensionLine, name, attributesMap);
      this.home.addDimensionLine(dimensionLine);
      String levelId = attributesMap.get("level");
      if (levelId != null) {
        dimensionLine.setLevel(this.levels.get(levelId));
      }
    } else if ("label".equals(name)) {
      Label label = createLabel(attributesMap, this.labelText);
      setLabelAttributes(label, name, attributesMap);
      this.home.addLabel(label);
      String levelId = attributesMap.get("level");
      if (levelId != null) {
        label.setLevel(this.levels.get(levelId));
      }      
    } else if ("text".equals(name)) {
      this.labelText = getCharacters();
    } else if ("textStyle".equals(name)) {
      this.textStyles.put(attributesMap.get("attribute"), createTextStyle(attributesMap));
    } else if ("texture".equals(name)) {
      if ("material".equals(parent)) {
        this.materialTexture = createTexture(attributesMap); 
      } else {
        this.textures.put(attributesMap.get("attribute"), createTexture(attributesMap));
      }
    } else if ("material".equals(name)) {
      this.materials.add(createMaterial(attributesMap));
    } else if ("point".equals(name)) {
      this.points.add(new float [] {
          parseFloat(attributesMap, "x"), 
          parseFloat(attributesMap, "y")});
    } else if ("sash".equals(name)) {
      this.sashes.add(new Sash(
          parseFloat(attributesMap, "xAxis"), 
          parseFloat(attributesMap, "yAxis"),
          parseFloat(attributesMap, "width"),
          parseFloat(attributesMap, "startAngle"),
          parseFloat(attributesMap, "endAngle")));
    } else if ("lightSource".equals(name)) {
      this.lightSources.add(new LightSource(
          parseFloat(attributesMap, "x"), 
          parseFloat(attributesMap, "y"),
          parseFloat(attributesMap, "z"),
          parseOptionalColor(attributesMap, "color"),
          parseOptionalFloat(attributesMap, "diameter")));
    } else if ("backgroundImage".equals(name)) {
      BackgroundImage backgroundImage = new BackgroundImage(
          parseContent(attributesMap.get("image")), 
          parseFloat(attributesMap, "scaleDistance"), 
          parseFloat(attributesMap, "scaleDistanceXStart"), 
          parseFloat(attributesMap, "scaleDistanceYStart"), 
          parseFloat(attributesMap, "scaleDistanceXEnd"), 
          parseFloat(attributesMap, "scaleDistanceYEnd"), 
          attributesMap.get("xOrigin") != null 
              ? parseFloat(attributesMap, "xOrigin") 
              : 0, 
          attributesMap.get("yOrigin") != null 
              ? parseFloat(attributesMap, "yOrigin") 
              : 0, 
          !"false".equals(attributesMap.get("visible")));
      if (this.homeElementName != null && this.homeElementName.equals(parent)) {
        this.homeBackgroundImage = backgroundImage;
      } else {
        this.backgroundImage = backgroundImage;
      }
    } else if ("property".equals(name)) {
      if (this.homeElementName != null) {
        if (this.homeElementName.equals(parent)) {
          this.homeProperties.put(attributesMap.get("name"), attributesMap.get("value"));
        } else {
          this.properties.put(attributesMap.get("name"), attributesMap.get("value"));
        }
      }
    } 
  }
  
  private String getCharacters() {
    return this.buffer.toString();
  }

  @Override
  public void endDocument() throws SAXException {
    for (JoinedWall joinedWall : this.joinedWalls.values()) {
      Wall wall = joinedWall.getWall();
      if (joinedWall.getWallAtStartId() != null) {
        JoinedWall joinedWallAtStart = this.joinedWalls.get(joinedWall.getWallAtStartId());
        if (joinedWallAtStart != null) {
          wall.setWallAtStart(joinedWallAtStart.getWall());
        }
      }
      if (joinedWall.getWallAtEndId() != null) {
        JoinedWall joinedWallAtEnd = this.joinedWalls.get(joinedWall.getWallAtEndId());
        if (joinedWallAtEnd != null) {
          wall.setWallAtEnd(joinedWallAtEnd.getWall());
        }
      }
    }
  }

  private Home createHome(Map<String, String> attributes) throws SAXException {
    if (attributes.get("wallHeight") != null) {
      return new Home(parseFloat(attributes, "wallHeight"));
    } else {
      return new Home();
    }
  }

  protected void setHomeAttributes(Home home, 
                                   String elementName, 
                                   Map<String, String> attributes) throws SAXException {
    for (Map.Entry<String, String> property : this.homeProperties.entrySet()) {
      home.setProperty(property.getKey(), property.getValue());
    }
    if (this.furnitureVisibleProperties.size() > 0) {
      this.home.setFurnitureVisibleProperties(this.furnitureVisibleProperties);
    }
    this.home.setBackgroundImage(this.homeBackgroundImage);
    String version = attributes.get("version");
    if (version != null) {
      try {
        home.setVersion(Integer.parseInt(version));
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for integer attribute version", ex);
      }
    }
    home.setName(attributes.get("name"));
    String selectedLevelId = attributes.get("selectedLevel");
    if (selectedLevelId != null) {
      this.home.setSelectedLevel(this.levels.get(selectedLevelId));
    }
    if ("observerCamera".equals(attributes.get("camera"))) {
      this.home.setCamera(this.home.getObserverCamera());
    }
    home.setBasePlanLocked("true".equals(attributes.get("basePlanLocked")));
    String furnitureSortedProperty = attributes.get("furnitureSortedProperty");
    if (furnitureSortedProperty != null) {
      try {
        home.setFurnitureSortedProperty(HomePieceOfFurniture.SortableProperty.valueOf(furnitureSortedProperty));
      } catch (IllegalArgumentException ex) {
      }
    }
    home.setFurnitureDescendingSorted("true".equals(attributes.get("furnitureDescendingSorted")));
  }

  private void setEnvironmentAttributes(HomeEnvironment environment, 
                                        String elementName, 
                                        Map<String, String> attributes) throws SAXException {
    Integer groundColor = parseOptionalColor(attributes, "groundColor");
    if (groundColor != null) {
      environment.setGroundColor(groundColor);
    }
    environment.setGroundTexture(this.textures.get("groundTexture"));
    Integer skyColor = parseOptionalColor(attributes, "skyColor");
    if (skyColor != null) {
      environment.setSkyColor(skyColor);
    }
    environment.setSkyTexture(this.textures.get("skyTexture"));
    Integer lightColor = parseOptionalColor(attributes, "lightColor");
    if (lightColor != null) {
      environment.setLightColor(lightColor);
    }
    Float wallsAlpha = parseOptionalFloat(attributes, "wallsAlpha");
    if (wallsAlpha != null) {
      environment.setWallsAlpha(wallsAlpha);
    }
    environment.setAllLevelsVisible("true".equals(attributes.get("allLevelsVisible")));
    environment.setObserverCameraElevationAdjusted(!"false".equals(attributes.get("observerCameraElevationAdjusted")));
    Integer ceillingLightColor = parseOptionalColor(attributes, "ceillingLightColor");
    if (ceillingLightColor != null) {
      environment.setCeillingLightColor(ceillingLightColor);
    }
    String drawingMode = attributes.get("drawingMode");
    if (drawingMode != null) {
      try {
        environment.setDrawingMode(HomeEnvironment.DrawingMode.valueOf(drawingMode));
      } catch (IllegalArgumentException ex) {
      }
    }
    Float subpartSizeUnderLight = parseOptionalFloat(attributes, "subpartSizeUnderLight");
    if (subpartSizeUnderLight != null) {
      environment.setSubpartSizeUnderLight(subpartSizeUnderLight);
    }
    Integer photoWidth = parseOptionalInteger(attributes, "photoWidth");
    if (photoWidth != null) {
      environment.setPhotoWidth(photoWidth);
    }
    Integer photoHeight = parseOptionalInteger(attributes, "photoHeight");
    if (photoHeight != null) {
      environment.setPhotoHeight(photoHeight);
    }
    String photoAspectRatio = attributes.get("photoAspectRatio");
    if (photoAspectRatio != null) {
      try {
        environment.setPhotoAspectRatio(AspectRatio.valueOf(photoAspectRatio));
      } catch (IllegalArgumentException ex) {
      }
    }
    Integer photoQuality = parseOptionalInteger(attributes, "photoQuality");
    if (photoQuality != null) {
      environment.setPhotoQuality(photoQuality);
    }
    Integer videoWidth = parseOptionalInteger(attributes, "videoWidth");
    if (videoWidth != null) {
      environment.setVideoWidth(videoWidth);
    }
    String videoAspectRatio = attributes.get("videoAspectRatio");
    if (videoAspectRatio != null) {
      try {
        environment.setVideoAspectRatio(AspectRatio.valueOf(videoAspectRatio));
      } catch (IllegalArgumentException ex) {
      }
    }
    Integer videoQuality = parseOptionalInteger(attributes, "videoQuality");
    if (videoQuality != null) {
      environment.setVideoQuality(videoQuality);
    }
    Integer videoFrameRate = parseOptionalInteger(attributes, "videoFrameRate");
    if (videoFrameRate != null) {
      environment.setVideoFrameRate(videoFrameRate);
    }
  }

  protected HomePrint createPrint(Map<String, String> attributes) throws SAXException {
    HomePrint.PaperOrientation paperOrientation = HomePrint.PaperOrientation.PORTRAIT;
    try {
      if (attributes.get("paperOrientation") == null) {
        throw new SAXException("Missing paperOrientation attribute");
      }
      paperOrientation = HomePrint.PaperOrientation.valueOf(attributes.get("paperOrientation"));
    } catch (IllegalArgumentException ex) {
    }
    return new HomePrint(paperOrientation, 
        parseFloat(attributes, "paperWidth"), 
        parseFloat(attributes, "paperHeight"), 
        parseFloat(attributes, "paperTopMargin"), 
        parseFloat(attributes, "paperLeftMargin"), 
        parseFloat(attributes, "paperBottomMargin"), 
        parseFloat(attributes, "paperRightMargin"), 
        !"false".equals(attributes.get("furniturePrinted")),
        !"false".equals(attributes.get("planPrinted")),
        !"false".equals(attributes.get("view3DPrinted")),
        parseOptionalFloat(attributes, "planScale"), 
        attributes.get("headerFormat"), 
        attributes.get("footerFormat"));
  }

  protected void setCompassAttributes(Compass compass, 
                                      String elementName, 
                                      Map<String, String> attributes) throws SAXException {
    setProperties(compass);
    compass.setX(parseOptionalFloat(attributes, "x"));
    compass.setY(parseOptionalFloat(attributes, "y"));
    compass.setDiameter(parseOptionalFloat(attributes, "diameter"));
    Float northDirection = parseOptionalFloat(attributes, "northDirection");
    if (northDirection != null) {
      compass.setNorthDirection(northDirection);
    }
    Float longitude = parseOptionalFloat(attributes, "longitude");
    if (longitude != null) {
      compass.setLongitude(longitude);
    }
    Float latitude = parseOptionalFloat(attributes, "latitude");
    if (latitude != null) {
      compass.setLatitude(latitude);
    }
    String timeZone = attributes.get("timeZone");
    if (timeZone != null) {
      compass.setTimeZone(timeZone);
    }
    compass.setVisible(!"false".equals(attributes.get("visible")));
  }

  private Camera createCamera(String elementName, Map<String, String> attributes) throws SAXException {
    if ("observerCamera".equals(elementName)) {
      return new ObserverCamera(parseFloat(attributes, "x"),
          parseFloat(attributes, "y"),
          parseFloat(attributes, "z"),
          parseFloat(attributes, "yaw"),
          parseFloat(attributes, "pitch"),
          parseFloat(attributes, "fieldOfView"));
    } else {
      return new Camera(parseFloat(attributes, "x"),
          parseFloat(attributes, "y"),
          parseFloat(attributes, "z"),
          parseFloat(attributes, "yaw"),
          parseFloat(attributes, "pitch"),
          parseFloat(attributes, "fieldOfView"));
    }
  }

  protected void setCameraAttributes(Camera camera, 
                                     String elementName, 
                                     Map<String, String> attributes) throws SAXException {
    setProperties(camera);
    if (camera instanceof ObserverCamera) {
      ((ObserverCamera)camera).setFixedSize("true".equals(attributes.get("fixedSize")));
    }
    String lens = attributes.get("lens");
    if (lens != null) {
      try {
        camera.setLens(Camera.Lens.valueOf(lens));
      } catch (IllegalArgumentException ex) {
      }
    }
    String time = attributes.get("time");
    if (time != null) {
      try {
        camera.setTime(Long.parseLong(time));
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for long attribute time", ex);
      }
    }
    
    camera.setName(attributes.get("name"));
  }

  private Level createLevel(Map<String, String> attributes) throws SAXException {
    return new Level(attributes.get("name"),
        parseFloat(attributes, "elevation"),
        parseFloat(attributes, "floorThickness"),
        parseFloat(attributes, "height"));
  }

  protected void setLevelAttributes(Level level, 
                                    String elementName, 
                                    Map<String, String> attributes) throws SAXException {
    setProperties(level);
    level.setBackgroundImage(this.backgroundImage);
    Integer elevationIndex = parseOptionalInteger(attributes, "elevationIndex");
    if (elevationIndex != null) {
      level.setElevationIndex(elevationIndex);
    }
    level.setVisible(!"false".equals(attributes.get("visible")));
    level.setViewable(!"false".equals(attributes.get("viewable")));
  }

  private HomePieceOfFurniture createPieceOfFurniture(String elementName, Map<String, String> attributes) throws SAXException {
    String [] tags = attributes.get("tags") != null 
        ? attributes.get("tags").split(" ")
        : null;
    float elevation = attributes.get("elevation") != null
        ? parseFloat(attributes, "elevation")
        : 0;
    float dropOnTopElevation = attributes.get("dropOnTopElevation") != null
        ? parseFloat(attributes, "dropOnTopElevation")
        : 1;
    float [][] modelRotation = null;
    if (attributes.get("modelRotation") != null) {
      String [] values = attributes.get("modelRotation").split(" ", 9);
      if (values.length < 9) {
        throw new SAXException("Missing values for attribute modelRotation");
      }
      try {
        modelRotation = new float [][] {
            {Float.parseFloat(values [0]), 
             Float.parseFloat(values [1]), 
             Float.parseFloat(values [2])}, 
            {Float.parseFloat(values [3]), 
             Float.parseFloat(values [4]), 
             Float.parseFloat(values [5])}, 
            {Float.parseFloat(values [6]), 
             Float.parseFloat(values [7]), 
             Float.parseFloat(values [8])}};
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for attribute modelRotation", ex);
      }
    }
    if ("doorOrWindow".equals(elementName)
        || "true".equals(attributes.get("doorOrWindow"))) {
      float wallThickness = attributes.get("wallThickness") != null
          ? parseFloat(attributes, "wallThickness")
          : 1;
      float wallDistance = attributes.get("wallDistance") != null
          ? parseFloat(attributes, "wallDistance")
          : 0;
      String cutOutShape = attributes.get("cutOutShape");
      if (cutOutShape == null
          && !"doorOrWindow".equals(elementName)) {
        cutOutShape = "M0,0 v1 h1 v-1 z";
      }
      return new HomeDoorOrWindow(new CatalogDoorOrWindow(
          attributes.get("catalogId"), 
          attributes.get("name"), 
          attributes.get("description"), 
          attributes.get("information"), 
          tags, 
          parseOptionalLong(attributes, "creationDate"), 
          parseOptionalFloat(attributes, "grade"), 
          parseContent(attributes.get("icon")),  
          parseContent(attributes.get("planIcon")), 
          parseContent(attributes.get("model")), 
          parseFloat(attributes, "width"), 
          parseFloat(attributes, "depth"), 
          parseFloat(attributes, "height"), 
          elevation, 
          dropOnTopElevation, 
          !"false".equals(attributes.get("movable")), 
          cutOutShape, 
          wallThickness,
          wallDistance,
          this.sashes.toArray(new Sash [this.sashes.size()]),
          modelRotation, 
          "true".equals(attributes.get("backFaceShown")),
          attributes.get("creator"), 
          !"false".equals(attributes.get("resizable")), 
          !"false".equals(attributes.get("deformable")), 
          !"false".equals(attributes.get("texturable")), 
          parseOptionalDecimal(attributes, "price"), 
          parseOptionalDecimal(attributes, "valueAddedTaxPercentage"), 
          attributes.get("currency")));
    } else if ("light".equals(elementName)) {
      return new HomeLight(new CatalogLight(
          attributes.get("catalogId"), 
          attributes.get("name"), 
          attributes.get("description"), 
          attributes.get("information"), 
          tags, 
          parseOptionalLong(attributes, "creationDate"), 
          parseOptionalFloat(attributes, "grade"), 
          parseContent(attributes.get("icon")),  
          parseContent(attributes.get("planIcon")), 
          parseContent(attributes.get("model")), 
          parseFloat(attributes, "width"), 
          parseFloat(attributes, "depth"), 
          parseFloat(attributes, "height"), 
          elevation, 
          dropOnTopElevation, 
          !"false".equals(attributes.get("movable")), 
          this.lightSources.toArray(new LightSource [this.lightSources.size()]),
          attributes.get("staircaseCutOutShape"), 
          modelRotation, 
          "true".equals(attributes.get("backFaceShown")),
          attributes.get("creator"), 
          !"false".equals(attributes.get("resizable")), 
          !"false".equals(attributes.get("deformable")), 
          !"false".equals(attributes.get("texturable")), 
          parseOptionalDecimal(attributes, "price"), 
          parseOptionalDecimal(attributes, "valueAddedTaxPercentage"), 
          attributes.get("currency")));
    } else {
      return new HomePieceOfFurniture(new CatalogPieceOfFurniture(
          attributes.get("catalogId"), 
          attributes.get("name"), 
          attributes.get("description"), 
          attributes.get("information"), 
          tags, 
          parseOptionalLong(attributes, "creationDate"), 
          parseOptionalFloat(attributes, "grade"), 
          parseContent(attributes.get("icon")),  
          parseContent(attributes.get("planIcon")), 
          parseContent(attributes.get("model")), 
          parseFloat(attributes, "width"), 
          parseFloat(attributes, "depth"), 
          parseFloat(attributes, "height"), 
          elevation, 
          dropOnTopElevation, 
          !"false".equals(attributes.get("movable")), 
          attributes.get("staircaseCutOutShape"), 
          modelRotation, 
          "true".equals(attributes.get("backFaceShown")),
          attributes.get("creator"), 
          !"false".equals(attributes.get("resizable")), 
          !"false".equals(attributes.get("deformable")), 
          !"false".equals(attributes.get("texturable")), 
          parseOptionalDecimal(attributes, "price"), 
          parseOptionalDecimal(attributes, "valueAddedTaxPercentage"), 
          attributes.get("currency")));
    }
  }

  private HomeFurnitureGroup createFurnitureGroup(Map<String, String> attributes, 
                                                  List<HomePieceOfFurniture> furniture) throws SAXException {
    return new HomeFurnitureGroup(furniture, 
        attributes.get("angle") != null ? parseFloat(attributes, "angle") : 0, 
        "true".equals(attributes.get("modelMirrored")), 
        attributes.get("name"));
  }

  protected void setPieceOfFurnitureAttributes(HomePieceOfFurniture piece, 
                                               String elementName, 
                                               Map<String, String> attributes) throws SAXException {
    setProperties(piece);
    piece.setNameStyle(this.textStyles.get("nameStyle"));
    piece.setNameVisible("true".equals(attributes.get("nameVisible")));
    Float nameAngle = parseOptionalFloat(attributes, "nameAngle");
    if (nameAngle != null) {
      piece.setNameAngle(nameAngle);
    }
    Float nameXOffset = parseOptionalFloat(attributes, "nameXOffset");
    if (nameXOffset != null) {
      piece.setNameXOffset(nameXOffset);
    }
    Float nameYOffset = parseOptionalFloat(attributes, "nameYOffset");
    if (nameYOffset != null) {
      piece.setNameYOffset(nameYOffset);
    }
    piece.setVisible(!"false".equals(attributes.get("visible")));
    
    if (!(piece instanceof HomeFurnitureGroup)) {
      Float x = parseOptionalFloat(attributes, "x");
      if (x != null) {
        piece.setX(x);
      }
      Float y = parseOptionalFloat(attributes, "y");
      if (y != null) {
        piece.setY(y);
      }
      Float angle = parseOptionalFloat(attributes, "angle");
      if (angle != null) {
        piece.setAngle(angle);
      }
      if (piece.isResizable()) {
         piece.setModelMirrored("true".equals(attributes.get("modelMirrored")));
      }
      if (piece.isTexturable()) {
        if (this.materials.size() > 0) {
          piece.setModelMaterials(this.materials.toArray(new HomeMaterial [this.materials.size()]));
        }
        Integer color = parseOptionalColor(attributes, "color");
        if (color != null) {
          piece.setColor(color);
        }
        HomeTexture texture = this.textures.get(null);
        if (texture != null) {
          piece.setTexture(texture);
        }
        Float shininess = parseOptionalFloat(attributes, "shininess");
        if (shininess != null) {
          piece.setShininess(shininess);
        }
      }
      
      if (piece instanceof HomeLight
          && attributes.get("power") != null) {
        ((HomeLight)piece).setPower(parseFloat(attributes, "power"));
      } else if (piece instanceof HomeDoorOrWindow
                 && "doorOrWindow".equals(elementName)) {
        ((HomeDoorOrWindow)piece).setBoundToWall(!"false".equals(attributes.get("boundToWall")));
      }
    }
  }

  @SuppressWarnings("deprecation")
  private Wall createWall(Map<String, String> attributes) throws SAXException {
    return new Wall(parseFloat(attributes, "xStart"),
        parseFloat(attributes, "yStart"),
        parseFloat(attributes, "xEnd"),
        parseFloat(attributes, "yEnd"),
        parseFloat(attributes, "thickness"));
  }

  protected void setWallAttributes(Wall wall, 
                                   String elementName, 
                                   Map<String, String> attributes) throws SAXException {
    setProperties(wall);
    wall.setLeftSideBaseboard(this.leftSideBaseboard);
    wall.setRightSideBaseboard(this.rightSideBaseboard);
    Float height = parseOptionalFloat(attributes, "height");
    if (height != null) {
      wall.setHeight(height);
    }
    wall.setHeightAtEnd(parseOptionalFloat(attributes, "heightAtEnd"));
    wall.setArcExtent(parseOptionalFloat(attributes, "arcExtent"));
    wall.setTopColor(parseOptionalColor(attributes, "topColor"));
    wall.setLeftSideColor(parseOptionalColor(attributes, "leftSideColor"));
    wall.setLeftSideTexture(this.textures.get("leftSideTexture"));
    Float leftSideShininess = parseOptionalFloat(attributes, "leftSideShininess");
    if (leftSideShininess != null) {
      wall.setLeftSideShininess(leftSideShininess);
    }
    wall.setRightSideColor(parseOptionalColor(attributes, "rightSideColor"));
    wall.setRightSideTexture(this.textures.get("rightSideTexture"));
    Float rightSideShininess = parseOptionalFloat(attributes, "rightSideShininess");
    if (rightSideShininess != null) {
      wall.setRightSideShininess(rightSideShininess);
    }
    String pattern = attributes.get("pattern");
    if (pattern != null) {
      try {
        wall.setPattern(this.preferences.getPatternsCatalog().getPattern(pattern));
      } catch (IllegalArgumentException ex) {
      }
    }
  }

  private Room createRoom(Map<String, String> attributes, float[][] points) {
    return new Room(points);
  }

  protected void setRoomAttributes(Room room, 
                                   String elementName, 
                                   Map<String, String> attributes) throws SAXException {
    setProperties(room);
    room.setNameStyle(this.textStyles.get("nameStyle"));
    room.setAreaStyle(this.textStyles.get("areaStyle"));
    room.setName(attributes.get("name"));
    Float nameAngle = parseOptionalFloat(attributes, "nameAngle");
    if (nameAngle != null) {
      room.setNameAngle(nameAngle);
    }
    Float nameXOffset = parseOptionalFloat(attributes, "nameXOffset");
    if (nameXOffset != null) {
      room.setNameXOffset(nameXOffset);
    }
    Float nameYOffset = parseOptionalFloat(attributes, "nameYOffset");
    if (nameYOffset != null) {
      room.setNameYOffset(nameYOffset);
    }
    room.setAreaVisible("true".equals(attributes.get("areaVisible")));
    Float areaAngle = parseOptionalFloat(attributes, "areaAngle");
    if (areaAngle != null) {
      room.setAreaAngle(areaAngle);
    }
    Float areaXOffset = parseOptionalFloat(attributes, "areaXOffset");
    if (areaXOffset != null) {
      room.setAreaXOffset(areaXOffset);
    }
    Float areaYOffset = parseOptionalFloat(attributes, "areaYOffset");
    if (areaYOffset != null) {
      room.setAreaYOffset(areaYOffset);
    }
    room.setFloorVisible(!"false".equals(attributes.get("floorVisible")));
    room.setFloorColor(parseOptionalColor(attributes, "floorColor"));
    room.setFloorTexture(this.textures.get("floorTexture"));
    Float floorShininess = parseOptionalFloat(attributes, "floorShininess");
    if (floorShininess != null) {
      room.setFloorShininess(floorShininess);
    }
    room.setCeilingVisible(!"false".equals(attributes.get("ceilingVisible")));
    room.setCeilingColor(parseOptionalColor(attributes, "ceilingColor"));
    room.setCeilingTexture(this.textures.get("ceilingTexture"));
    Float ceilingShininess = parseOptionalFloat(attributes, "ceilingShininess");
    if (ceilingShininess != null) {
      room.setCeilingShininess(ceilingShininess);
    }
  }
  
  private Polyline createPolyline(Map<String, String> attributes, float[][] points) {
    return new Polyline(points);
  }

  protected void setPolylineAttributes(Polyline polyline, 
                                       String elementName, 
                                       Map<String, String> attributes) throws SAXException {
    setProperties(polyline);
    Float thickness = parseOptionalFloat(attributes, "thickness");
    if (thickness != null) {
      polyline.setThickness(thickness);
    }
    String capStyle = attributes.get("capStyle");
    if (capStyle != null) {
      try {
        polyline.setCapStyle(Polyline.CapStyle.valueOf(capStyle));
      } catch (IllegalArgumentException ex) {
      }
    }
    String joinStyle = attributes.get("joinStyle");
    if (joinStyle != null) {
      try {
        polyline.setJoinStyle(Polyline.JoinStyle.valueOf(joinStyle));
      } catch (IllegalArgumentException ex) {
      }
    }
    String dashStyle = attributes.get("dashStyle");
    if (dashStyle != null) {
      try {
        polyline.setDashStyle(Polyline.DashStyle.valueOf(dashStyle));
      } catch (IllegalArgumentException ex) {
      }
    }
    String startArrowStyle = attributes.get("startArrowStyle");
    if (startArrowStyle != null) {
      try {
        polyline.setStartArrowStyle(Polyline.ArrowStyle.valueOf(startArrowStyle));
      } catch (IllegalArgumentException ex) {

      }
    }
    String endArrowStyle = attributes.get("endArrowStyle");
    if (endArrowStyle != null) {
      try {
        polyline.setEndArrowStyle(Polyline.ArrowStyle.valueOf(endArrowStyle));
      } catch (IllegalArgumentException ex) {

      }
    }
    Integer color = parseOptionalColor(attributes, "color");
    if (color != null) {
      polyline.setColor(color);
    }
    polyline.setClosedPath("true".equals(attributes.get("closedPath")));
  }
  private DimensionLine createDimensionLine(Map<String, String> attributes) throws SAXException {
    return new DimensionLine(parseFloat(attributes, "xStart"),
        parseFloat(attributes, "yStart"),
        parseFloat(attributes, "xEnd"),
        parseFloat(attributes, "yEnd"),
        parseFloat(attributes, "offset"));
  }

  protected void setDimensionLineAttributes(DimensionLine dimensionLine, 
                                            String elementName, 
                                            Map<String, String> attributes) throws SAXException {
    setProperties(dimensionLine);
    dimensionLine.setLengthStyle(this.textStyles.get("lengthStyle"));
  }

  private Label createLabel(Map<String, String> attributes, String text) throws SAXException {
    return new Label(text, 
        parseFloat(attributes, "x"),
        parseFloat(attributes, "y"));
  }

  protected void setLabelAttributes(Label label, 
                                    String elementName, 
                                    Map<String, String> attributes) throws SAXException {
    setProperties(label);
    label.setStyle(this.textStyles.get(null));
    Float angle = parseOptionalFloat(attributes, "angle");
    if (angle != null) {
      label.setAngle(angle);
    }
    Float elevation = parseOptionalFloat(attributes, "elevation");
    if (elevation != null) {
      label.setElevation(elevation);
    }
    Float pitch = parseOptionalFloat(attributes, "pitch");
    if (pitch != null) {
      label.setPitch(pitch);
    }
    label.setColor(parseOptionalColor(attributes, "color"));
    label.setOutlineColor(parseOptionalColor(attributes, "outlineColor"));
  }

  private Baseboard createBaseboard(Map<String, String> attributes) throws SAXException {
    return Baseboard.getInstance(parseFloat(attributes, "thickness"), 
        parseFloat(attributes, "height"), 
        parseOptionalColor(attributes, "color"),
        this.textures.get(null));
  }

  private TextStyle createTextStyle(Map<String, String> attributes) throws SAXException {
    return new TextStyle(attributes.get("fontName"),
        parseFloat(attributes, "fontSize"),
        "true".equals(attributes.get("bold")),
        "true".equals(attributes.get("italic")));
  }

  private HomeTexture createTexture(Map<String, String> attributes) throws SAXException {
    return new HomeTexture(new CatalogTexture(
            attributes.get("catalogId"), 
            attributes.get("name"), 
            parseContent(attributes.get("image")), 
            parseFloat(attributes, "width"), 
            parseFloat(attributes, "height"), 
            null),
        attributes.get("angle") != null
            ? parseFloat(attributes, "angle")
            : 0,
        !"false".equals(attributes.get("leftToRightOriented")));
  }

  private HomeMaterial createMaterial(Map<String, String> attributes) throws SAXException {
    return new HomeMaterial(
        attributes.get("name"), 
        attributes.get("key"), 
        parseOptionalColor(attributes, "color"), 
        this.materialTexture,
        parseOptionalFloat(attributes, "shininess"));
  }

  private void setProperties(HomeObject object) {
    for (Map.Entry<String, String> property : this.properties.entrySet()) {
      object.setProperty(property.getKey(), property.getValue());
    }
  }

  private Integer parseOptionalColor(Map<String, String> attributes, String name) throws SAXException {
    String color = attributes.get(name);
    if (color != null) {
      try {
        return new Integer((int)Long.parseLong(color, 16));
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for color attribute " + name, ex);
      }
    } else {
      return null;
    }
  }
  
  private Integer parseOptionalInteger(Map<String, String> attributes, String name) throws SAXException {
    String value = attributes.get(name);
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for integer attribute " + name, ex);
      }
    } else {
      return null;
    }
  }
  
  private Long parseOptionalLong(Map<String, String> attributes, String name) throws SAXException {
    String value = attributes.get(name);
    if (value != null) {
      try {
        return Long.parseLong(value);
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for long attribute " + name, ex);
      }
    } else {
      return null;
    }
  }
  
  private BigDecimal parseOptionalDecimal(Map<String, String> attributes, String name) throws SAXException {
    String value = attributes.get(name);
    if (value != null) {  
      try {
        return new BigDecimal(value);
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for decimal attribute " + name, ex);
      }
    } else {
      return null;
    }
  }
  
  private Float parseOptionalFloat(Map<String, String> attributes, String name) throws SAXException {
    String value = attributes.get(name);
    if (value != null) {  
      try {
        return Float.parseFloat(value);
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for float attribute " + name, ex);
      }
    } else {
      return null;
    }
  }
  
  private float parseFloat(Map<String, String> attributes, String name) throws SAXException {
    String value = attributes.get(name);
    if (value != null) {  
      try {
        return Float.parseFloat(value);
      } catch (NumberFormatException ex) {
        throw new SAXException("Invalid value for float attribute " + name, ex);
      }
    } else {
      throw new SAXException("Missing float attribute " + name);
    }
  }

  private Content parseContent(String content) throws SAXException {
    if (content != null) {
      try {
        return new ResourceURLContent(new URL(content), content.startsWith("jar:"));
      } catch (MalformedURLException ex1) {
        if (this.contentContext != null) {
          try {
            return this.contentContext.lookupContent(content);
          } catch (IOException ex2) {
            throw new SAXException("Invalid content " + content, ex2);
          }
        } else {
          throw new SAXException("Missing URL base", ex1);
        }
      }
    } else {
      return null;
    }
  }

  protected void setHome(Home home) {
    this.home = home;
    this.homeElementName = this.elements.peek();
  }

  public Home getHome() {
    return this.home;
  }

  private static final class JoinedWall {
    private final Wall    wall;
    private final String  wallAtStartId;
    private final String  wallAtEndId;
    
    public JoinedWall(Wall wall, String wallAtStartId, String wallAtEndId) {
      this.wall = wall;
      this.wallAtStartId = wallAtStartId;
      this.wallAtEndId = wallAtEndId;
    }
    
    public Wall getWall() {
      return this.wall;
    }
    
    public String getWallAtStartId() {
      return this.wallAtStartId;
    }
    
    public String getWallAtEndId() {
      return this.wallAtEndId;
    }
  }
}
