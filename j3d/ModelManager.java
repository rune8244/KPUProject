package com.eteks.homeview3d.j3d;

import java.awt.EventQueue;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryStripArray;
import javax.media.j3d.Group;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedGeometryStripArray;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.IndexedTriangleFanArray;
import javax.media.j3d.IndexedTriangleStripArray;
import javax.media.j3d.Light;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Link;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.SharedGroup;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.TriangleFanArray;
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathParser;

import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.HomeMaterial;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.Room;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.SimpleURLContent;
import com.eteks.homeview3d.tools.TemporaryURLContent;
import com.eteks.homeview3d.tools.URLContent;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.lw3d.Lw3dLoader;

public class ModelManager {

  public static final String WINDOW_PANE_SHAPE_PREFIX = "homeview3d_window_pane";

  public static final String MIRROR_SHAPE_PREFIX = "homeview3d_window_mirror";

  public static final String LIGHT_SHAPE_PREFIX = "homeview3d_light";
  
  private static final TransparencyAttributes WINDOW_PANE_TRANSPARENCY_ATTRIBUTES = 
      new TransparencyAttributes(TransparencyAttributes.NICEST, 0.5f);

  private static final Material               DEFAULT_MATERIAL = new Material();
  
  private static final float MINIMUM_SIZE = 0.001f;

  private static final String ADDITIONAL_LOADER_CLASSES = "com.eteks.homeview3d.j3d.additionalLoaderClasses";
  
  private static ModelManager instance;
  

  private Map<Content, BranchGroup> loadedModelNodes;

  private Map<Content, List<ModelObserver>> loadingModelObservers;

  private ExecutorService           modelsLoader;

  private Class<Loader> []          additionalLoaderClasses;

  private final Map<String, Shape>  parsedShapes;

  private ModelManager() {    

    this.loadedModelNodes = new WeakHashMap<Content, BranchGroup>();
    this.loadingModelObservers = new HashMap<Content, List<ModelObserver>>();
    this.parsedShapes = new WeakHashMap<String, Shape>();

    List<Class<Loader>> loaderClasses = new ArrayList<Class<Loader>>();
    String loaderClassNames = System.getProperty(ADDITIONAL_LOADER_CLASSES);
    if (loaderClassNames != null) {
      for (String loaderClassName : loaderClassNames.split("\\s|:")) {
        try {
          loaderClasses.add(getLoaderClass(loaderClassName));
        } catch (IllegalArgumentException ex) {
          System.err.println("Invalid loader class " + loaderClassName + ":\n" + ex.getMessage());
        }
      }
    }
    this.additionalLoaderClasses = loaderClasses.toArray(new Class [loaderClasses.size()]);
  }


  @SuppressWarnings("unchecked")
  private Class<Loader> getLoaderClass(String loaderClassName) {
    try {
      Class<Loader> loaderClass = (Class<Loader>)getClass().getClassLoader().loadClass(loaderClassName);
      if (!Loader.class.isAssignableFrom(loaderClass)) {
        throw new IllegalArgumentException(loaderClassName + " not a subclass of " + Loader.class.getName());
      } else if (Modifier.isAbstract(loaderClass.getModifiers()) || !Modifier.isPublic(loaderClass.getModifiers())) {
        throw new IllegalArgumentException(loaderClassName + " not a public static class");
      }
      Constructor<Loader> constructor = loaderClass.getConstructor(new Class [0]);

      constructor.newInstance(new Object [0]);
      return loaderClass;
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    } catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    } catch (InvocationTargetException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    } catch (IllegalAccessException ex) {
      throw new IllegalArgumentException(loaderClassName + " constructor not accessible");
    } catch (InstantiationException ex) {
      throw new IllegalArgumentException(loaderClassName + " not a public static class");
    }
  }
  

  public static ModelManager getInstance() {
    if (instance == null) {
      instance = new ModelManager();
    }
    return instance;
  }


  public void clear() {
    if (this.modelsLoader != null) {
      this.modelsLoader.shutdownNow();
      this.modelsLoader = null;
    }
    synchronized (this.loadedModelNodes) {
      this.loadedModelNodes.clear();
    }
    this.loadingModelObservers.clear();
  }
  

  float getMinimumSize() {
    return MINIMUM_SIZE;
  }
  

  public Vector3f getSize(Node node) {
    return getSize(node, new Transform3D());
  }
  

  public Vector3f getSize(Node node, Transform3D transformation) {
    BoundingBox bounds = getBounds(node, transformation);
    Point3d lower = new Point3d();
    bounds.getLower(lower);
    Point3d upper = new Point3d();
    bounds.getUpper(upper);
    return new Vector3f(Math.max(getMinimumSize(), (float)(upper.x - lower.x)), 
        Math.max(getMinimumSize(), (float)(upper.y - lower.y)), 
        Math.max(getMinimumSize(), (float)(upper.z - lower.z)));
  }
  

  public BoundingBox getBounds(Node node) {
    return getBounds(node, new Transform3D());
  }
  

  public BoundingBox getBounds(Node node, Transform3D transformation) {
    BoundingBox objectBounds = new BoundingBox(
        new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
        new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
    computeBounds(node, objectBounds, transformation, !isOrthogonalRotation(transformation));
    Point3d lower = new Point3d();
    objectBounds.getLower(lower);
    if (lower.x == Double.POSITIVE_INFINITY) {
      throw new IllegalArgumentException("Node has no bounds");
    }
    return objectBounds;
  }
  

  private boolean isOrthogonalRotation(Transform3D transformation) {
    Matrix3f matrix = new Matrix3f();
    transformation.get(matrix);
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {

        if (Math.abs(matrix.getElement(i, j)) > 1E-6
            && Math.abs(matrix.getElement(i, j) - 1) > 1E-6
            && Math.abs(matrix.getElement(i, j) + 1) > 1E-6) {
          return false;
        }
      }
    }
    return true;
  }

  private void computeBounds(Node node, BoundingBox bounds, 
                             Transform3D parentTransformations, boolean transformShapeGeometry) {
    if (node instanceof Group) {
      if (node instanceof TransformGroup) {
        parentTransformations = new Transform3D(parentTransformations);
        Transform3D transform = new Transform3D();
        ((TransformGroup)node).getTransform(transform);
        parentTransformations.mul(transform);
      }

      Enumeration<?> enumeration = ((Group)node).getAllChildren();
      while (enumeration.hasMoreElements ()) {
        computeBounds((Node)enumeration.nextElement(), bounds, parentTransformations, transformShapeGeometry);
      }
    } else if (node instanceof Link) {
      computeBounds(((Link)node).getSharedGroup(), bounds, parentTransformations, transformShapeGeometry);
    } else if (node instanceof Shape3D) {
      Shape3D shape = (Shape3D)node;
      Bounds shapeBounds;
      if (transformShapeGeometry) {
        shapeBounds = computeTransformedGeometryBounds(shape, parentTransformations);
      } else {
        shapeBounds = shape.getBounds();
        shapeBounds.transform(parentTransformations);
      }
      bounds.combine(shapeBounds);
    }
  }

  private Bounds computeTransformedGeometryBounds(Shape3D shape, Transform3D transformation) {
    Point3d lower = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    Point3d upper = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);    
    for (int i = 0, n = shape.numGeometries(); i < n; i++) {
      Geometry geometry = shape.getGeometry(i);
      if (geometry instanceof GeometryArray) {
        GeometryArray geometryArray = (GeometryArray)geometry;      
        int vertexCount = geometryArray.getVertexCount();
        Point3f vertex = new Point3f();
        if ((geometryArray.getVertexFormat() & GeometryArray.BY_REFERENCE) != 0) {
          if ((geometryArray.getVertexFormat() & GeometryArray.INTERLEAVED) != 0) {
            float [] vertexData = geometryArray.getInterleavedVertices();
            int vertexSize = vertexData.length / vertexCount;
            for (int index = 0, j = vertexSize - 3; index < vertexCount; j += vertexSize, index++) {
              vertex.x = vertexData [j];
              vertex.y = vertexData [j + 1];
              vertex.z = vertexData [j + 2];
              updateBounds(vertex, transformation, lower, upper);
            }
          } else {
            float [] vertexCoordinates = geometryArray.getCoordRefFloat();
            for (int index = 0, j = 0; index < vertexCount; j += 3, index++) {
              vertex.x = vertexCoordinates [j];
              vertex.y = vertexCoordinates [j + 1];
              vertex.z = vertexCoordinates [j + 2];
              updateBounds(vertex, transformation, lower, upper);
            }
          }
        } else {
          for (int index = 0; index < vertexCount; index++) {
            geometryArray.getCoordinate(index, vertex);
            updateBounds(vertex, transformation, lower, upper);
          }
        }
      } else {
        Bounds shapeBounds = shape.getBounds();
        shapeBounds.transform(transformation);
        return shapeBounds;
      }
    }
    Bounds shapeBounds = new BoundingBox(lower, upper);
    return shapeBounds;
  }

  private void updateBounds(Point3f vertex, Transform3D transformation, Point3d lower, Point3d upper) {
    transformation.transform(vertex);
    if (lower.x > vertex.x) {
      lower.x = vertex.x;
    }
    if (lower.y > vertex.y) {
      lower.y = vertex.y;
    }
    if (lower.z > vertex.z) {
      lower.z = vertex.z;
    }
    if (upper.x < vertex.x) {
      upper.x = vertex.x;
    }
    if (upper.y < vertex.y) {
      upper.y = vertex.y;
    }
    if (upper.z < vertex.z) {
      upper.z = vertex.z;
    }
  }

  public TransformGroup getNormalizedTransformGroup(Node node, float [][] modelRotation, float width) {
    return new TransformGroup(getNormalizedTransform(node, modelRotation, width));
  }
  

  public Transform3D getNormalizedTransform(Node node, float [][] modelRotation, float width) {

    BoundingBox modelBounds = getBounds(node);
    Point3d lower = new Point3d();
    modelBounds.getLower(lower);
    Point3d upper = new Point3d();
    modelBounds.getUpper(upper);

    Transform3D translation = new Transform3D();
    translation.setTranslation(
        new Vector3d(-lower.x - (upper.x - lower.x) / 2, 
            -lower.y - (upper.y - lower.y) / 2, 
            -lower.z - (upper.z - lower.z) / 2));
    
    Transform3D modelTransform;
    if (modelRotation != null) {

      modelTransform = getRotationTransformation(modelRotation);
      modelTransform.mul(translation);
      BoundingBox rotatedModelBounds = getBounds(node, modelTransform);
      rotatedModelBounds.getLower(lower);
      rotatedModelBounds.getUpper(upper);
    } else {
      modelTransform = translation;
    }


    Transform3D scaleOneTransform = new Transform3D();
    scaleOneTransform.setScale (
        new Vector3d(width / Math.max(getMinimumSize(), upper.x -lower.x), 
            width / Math.max(getMinimumSize(), upper.y - lower.y), 
            width / Math.max(getMinimumSize(), upper.z - lower.z)));
    scaleOneTransform.mul(modelTransform);
    return scaleOneTransform;
  }

  Transform3D getRotationTransformation(float [][] modelRotation) {
    Matrix3f modelRotationMatrix = new Matrix3f(modelRotation [0][0], modelRotation [0][1], modelRotation [0][2],
        modelRotation [1][0], modelRotation [1][1], modelRotation [1][2],
        modelRotation [2][0], modelRotation [2][1], modelRotation [2][2]);
    Transform3D modelTransform = new Transform3D();
    modelTransform.setRotation(modelRotationMatrix);
    return modelTransform;
  }

  Transform3D getPieceOFFurnitureNormalizedModelTransformation(HomePieceOfFurniture piece) {

    Transform3D scale = new Transform3D();
    float pieceWidth = piece.getWidth();

    if (piece.isModelMirrored()) {
      pieceWidth *= -1;
    }
    scale.setScale(new Vector3d(pieceWidth, piece.getHeight(), piece.getDepth()));

    Transform3D orientation = new Transform3D();
    orientation.rotY(-piece.getAngle());
    orientation.mul(scale);

    Transform3D pieceTransform = new Transform3D();
    float z = piece.getElevation() + piece.getHeight() / 2;
    if (piece.getLevel() != null) {
      z += piece.getLevel().getElevation();
    }
    pieceTransform.setTranslation(new Vector3f(piece.getX(), z, piece.getY()));      
    pieceTransform.mul(orientation);
    return pieceTransform;
  }


  public void loadModel(Content content,
                        ModelObserver modelObserver) {
    loadModel(content, false, modelObserver);
  }
  

  public void loadModel(final Content content,
                        boolean synchronous,
                        ModelObserver modelObserver) {
    BranchGroup modelRoot;
    synchronized (this.loadedModelNodes) {
      modelRoot = this.loadedModelNodes.get(content);
    }
    if (modelRoot != null) {

      modelObserver.modelUpdated((BranchGroup)cloneNode(modelRoot));
    } else if (synchronous) {
      try {
        modelRoot = loadModel(content);
        synchronized (this.loadedModelNodes) {

          this.loadedModelNodes.put(content, (BranchGroup)modelRoot);
        }
        modelObserver.modelUpdated((BranchGroup)cloneNode(modelRoot));
      } catch (IOException ex) {
        modelObserver.modelError(ex);
      }
    } else if (!EventQueue.isDispatchThread()) {
      throw new IllegalStateException("Asynchronous call out of Event Dispatch Thread");
    } else {  
      if (this.modelsLoader == null) {
        this.modelsLoader = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
      }
      List<ModelObserver> observers = this.loadingModelObservers.get(content);
      if (observers != null) {

        observers.add(modelObserver);
      } else {

        observers = new ArrayList<ModelObserver>();
        observers.add(modelObserver);
        this.loadingModelObservers.put(content, observers);
        

        this.modelsLoader.execute(new Runnable() {
          public void run() {
            try {
              final BranchGroup loadedModel = loadModel(content);
              synchronized (loadedModelNodes) {

                loadedModelNodes.put(content, loadedModel);
              }
              EventQueue.invokeLater(new Runnable() {
                  public void run() {
                    List<ModelObserver> observers = loadingModelObservers.remove(content);
                    if (observers != null) {
                      for (final ModelObserver observer : observers) {
                        observer.modelUpdated((BranchGroup)cloneNode(loadedModel));
                      }
                    }
                  }
                });
            } catch (final IOException ex) {
              EventQueue.invokeLater(new Runnable() {
                  public void run() {
                    List<ModelObserver> observers = loadingModelObservers.remove(content);
                    if (observers != null) {
                      for (final ModelObserver observer : observers) {
                        observer.modelError(ex);
                      }
                    }
                  }
                });
            }
          }
        });
      }
    }
  }
  

  public Node cloneNode(Node node) {

    synchronized (this.loadedModelNodes) {  
      return cloneNode(node, new HashMap<SharedGroup, SharedGroup>());
    }
  }
    
  private Node cloneNode(Node node, Map<SharedGroup, SharedGroup> clonedSharedGroups) {
    if (node instanceof Shape3D) {
      Shape3D shape = (Shape3D)node;
      Shape3D clonedShape = (Shape3D)shape.cloneNode(false);
      Appearance appearance = shape.getAppearance();
      if (appearance != null) {
        Appearance clonedAppearance = (Appearance)appearance.cloneNodeComponent(false);
        Material material = appearance.getMaterial();
        if (material != null) {
          clonedAppearance.setMaterial((Material)material.cloneNodeComponent(true));
        }
        ColoringAttributes coloringAttributes = appearance.getColoringAttributes();
        if (coloringAttributes != null) {
          clonedAppearance.setColoringAttributes((ColoringAttributes)coloringAttributes.cloneNodeComponent(true));
        }
        TransparencyAttributes transparencyAttributes = appearance.getTransparencyAttributes();
        if (transparencyAttributes != null) {
          clonedAppearance.setTransparencyAttributes((TransparencyAttributes)transparencyAttributes.cloneNodeComponent(true));
        }
        RenderingAttributes renderingAttributes = appearance.getRenderingAttributes();
        if (renderingAttributes != null) {
          clonedAppearance.setRenderingAttributes((RenderingAttributes)renderingAttributes.cloneNodeComponent(true));
        }
        PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
        if (polygonAttributes != null) {
          clonedAppearance.setPolygonAttributes((PolygonAttributes)polygonAttributes.cloneNodeComponent(true));
        }
        LineAttributes lineAttributes = appearance.getLineAttributes();
        if (lineAttributes != null) {
          clonedAppearance.setLineAttributes((LineAttributes)lineAttributes.cloneNodeComponent(true));
        }
        PointAttributes pointAttributes = appearance.getPointAttributes();
        if (pointAttributes != null) {
          clonedAppearance.setPointAttributes((PointAttributes)pointAttributes.cloneNodeComponent(true));
        }
        TextureAttributes textureAttributes = appearance.getTextureAttributes();
        if (textureAttributes != null) {
          clonedAppearance.setTextureAttributes((TextureAttributes)textureAttributes.cloneNodeComponent(true));
        }
        TexCoordGeneration texCoordGeneration = appearance.getTexCoordGeneration();
        if (texCoordGeneration != null) {
          clonedAppearance.setTexCoordGeneration((TexCoordGeneration)texCoordGeneration.cloneNodeComponent(true));
        }
        
        clonedShape.setAppearance(clonedAppearance);
      }
      return clonedShape;
    } else if (node instanceof Link) {
      Link clonedLink = (Link)node.cloneNode(true);
      SharedGroup sharedGroup = clonedLink.getSharedGroup();
      if (sharedGroup != null) {
        SharedGroup clonedSharedGroup = clonedSharedGroups.get(sharedGroup);
        if (clonedSharedGroup == null) {
          clonedSharedGroup = (SharedGroup)cloneNode(sharedGroup, clonedSharedGroups);
          clonedSharedGroups.put(sharedGroup, clonedSharedGroup);          
        }
        clonedLink.setSharedGroup(clonedSharedGroup);
      }
      return clonedLink;
    } else {
      Node clonedNode = node.cloneNode(true);
      if (node instanceof Group) {
        Group group = (Group)node;
        Group clonedGroup = (Group)clonedNode;
        for (int i = 0, n = group.numChildren(); i < n; i++) {
          Node clonedChild = cloneNode(group.getChild(i), clonedSharedGroups);
          clonedGroup.addChild(clonedChild);
        }
      }
      return clonedNode;
    }
  }
  

  public BranchGroup loadModel(Content content) throws IOException {

    URLContent urlContent;
    if (content instanceof URLContent) {
      urlContent = (URLContent)content;
    } else {
      urlContent = TemporaryURLContent.copyToTemporaryURLContent(content);
    }
    Loader []  defaultLoaders = new Loader [] {new OBJLoader(),
                                               new DAELoader(),
                                               new Max3DSLoader(),
                                               new Lw3dLoader()};
    Loader [] loaders = new Loader [defaultLoaders.length + this.additionalLoaderClasses.length];
    System.arraycopy(defaultLoaders, 0, loaders, 0, defaultLoaders.length);
    for (int i = 0; i < this.additionalLoaderClasses.length; i++) {
      try {
        loaders [defaultLoaders.length + i] = this.additionalLoaderClasses [i].newInstance();
      } catch (InstantiationException ex) {

        throw new InternalError(ex.getMessage());
      } catch (IllegalAccessException ex) {

        throw new InternalError(ex.getMessage());
      } 
    }
    
    Exception lastException = null;
    Boolean useCaches = shouldUseCaches(urlContent);
    for (Loader loader : loaders) {
      boolean loadSynchronously = false;
      try {

        loader.getClass().getMethod("setUseCaches", Boolean.class).invoke(loader, useCaches);
      } catch (NoSuchMethodException ex) {

        URLConnection connection = urlContent.getURL().openConnection();
        loadSynchronously = connection.getDefaultUseCaches() != useCaches;        
      } catch (InvocationTargetException ex) {
        if (ex instanceof Exception) {
          lastException = (Exception)ex.getTargetException();
          continue;
        } else {
          ex.printStackTrace();
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
      
      try {     

        loader.setFlags(loader.getFlags() 
            & ~(Loader.LOAD_LIGHT_NODES | Loader.LOAD_FOG_NODES 
                | Loader.LOAD_BACKGROUND_NODES | Loader.LOAD_VIEW_GROUPS));

        Scene scene;
        if (loadSynchronously) {
          synchronized (this.modelsLoader) {
            URLConnection connection = urlContent.getURL().openConnection();
            try {
              connection.setDefaultUseCaches(useCaches);
              scene = loader.load(urlContent.getURL());
            } finally {
              if (connection.getDefaultUseCaches() == useCaches) {

                connection.setDefaultUseCaches(!useCaches);
              }
            }
          }
        } else {
          scene = loader.load(urlContent.getURL());
        }

        BranchGroup modelNode = scene.getSceneGroup();
        if (modelNode.numChildren() == 0) {
          throw new IllegalArgumentException("Empty model");
        }
        

        updateShapeNamesAndWindowPanesTransparency(scene);        
        turnOffLightsShareAndModulateTextures(modelNode, new IdentityHashMap<Texture, Texture>());        
        checkAppearancesName(modelNode);
        return modelNode;
      } catch (IllegalArgumentException ex) {
        lastException = ex;
      } catch (IncorrectFormatException ex) {
        lastException = ex;
      } catch (ParsingErrorException ex) {
        lastException = ex;
      } catch (IOException ex) {
        lastException = ex;
      } catch (RuntimeException ex) {

        if (ex.getClass().getName().equals("com.sun.j3d.utils.image.ImageException")) {
          lastException = ex;
        } else {
          throw ex;
        }
      }
    }
    
    if (lastException instanceof IOException) {
      throw (IOException)lastException;
    } else if (lastException instanceof IncorrectFormatException) {
      IOException incorrectFormatException = new IOException("Incorrect format");
      incorrectFormatException.initCause(lastException);
      throw incorrectFormatException;
    } else if (lastException instanceof ParsingErrorException) {
      IOException incorrectFormatException = new IOException("Parsing error");
      incorrectFormatException.initCause(lastException);
      throw incorrectFormatException;
    } else {
      IOException otherException = new IOException();
      otherException.initCause(lastException);
      throw otherException;
    } 
  }  
  

  private boolean shouldUseCaches(URLContent urlContent) throws IOException {
    URLConnection connection = urlContent.getURL().openConnection();
    if (OperatingSystem.isWindows() 
        && (connection instanceof JarURLConnection)) {
      JarURLConnection urlConnection = (JarURLConnection)connection;
      URL jarFileUrl = urlConnection.getJarFileURL();
      if (jarFileUrl.getProtocol().equalsIgnoreCase("file")) {
        try {
          if (new File(jarFileUrl.toURI()).canWrite()) {

            return false;
          }
        } catch (URISyntaxException ex) {
          IOException ex2 = new IOException();
          ex2.initCause(ex);
          throw ex2;
        }
      }
    }
    return connection.getDefaultUseCaches();
  }
  

  @SuppressWarnings("unchecked")
  private void updateShapeNamesAndWindowPanesTransparency(Scene scene) {
    Map<String, Object> namedObjects = scene.getNamedObjects();
    for (Map.Entry<String, Object> entry : namedObjects.entrySet()) {
      if (entry.getValue() instanceof Shape3D) {
        String shapeName = entry.getKey();
        Shape3D shape = (Shape3D)entry.getValue();
        shape.setUserData(shapeName);
        if (shapeName.startsWith(WINDOW_PANE_SHAPE_PREFIX)) {
          Appearance appearance = shape.getAppearance();
          if (appearance == null) {
            appearance = new Appearance();
            shape.setAppearance(appearance);
          }
          if (appearance.getTransparencyAttributes() == null) {
            appearance.setTransparencyAttributes(WINDOW_PANE_TRANSPARENCY_ATTRIBUTES);
          }
        }
      }
    }
  }
  

  private void turnOffLightsShareAndModulateTextures(Node node, 
                                                     Map<Texture, Texture> replacedTextures) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        turnOffLightsShareAndModulateTextures((Node)enumeration.nextElement(), replacedTextures);
      }
    } else if (node instanceof Link) {
      turnOffLightsShareAndModulateTextures(((Link)node).getSharedGroup(), replacedTextures);
    } else if (node instanceof Light) {
      ((Light)node).setEnable(false);
    } else if (node instanceof Shape3D) {
      Appearance appearance = ((Shape3D)node).getAppearance();
      if (appearance != null) {
        Texture texture = appearance.getTexture();
        if (texture != null) {
          Texture sharedTexture = replacedTextures.get(texture);
          if (sharedTexture == null) {
            sharedTexture = TextureManager.getInstance().shareTexture(texture);
            replacedTextures.put(texture, sharedTexture);
          }
          if (sharedTexture != texture) {
            appearance.setTexture(sharedTexture);
          }
          TextureAttributes textureAttributes = appearance.getTextureAttributes();
          if (textureAttributes == null) {
            textureAttributes = new TextureAttributes();
            textureAttributes.setTextureMode(TextureAttributes.MODULATE);
            appearance.setTextureAttributes(textureAttributes);
            Material material = appearance.getMaterial();
            if (material == null) {
              appearance.setMaterial((Material)DEFAULT_MATERIAL.cloneNodeComponent(true));
            } else {
              Color3f color = new Color3f();
              DEFAULT_MATERIAL.getDiffuseColor(color);
              material.setDiffuseColor(color);
              DEFAULT_MATERIAL.getAmbientColor(color);
              material.setAmbientColor(color);
            }
          }
          
          if (TextureManager.getInstance().isTextureTransparent(sharedTexture)) {
            if (appearance.getTransparencyAttributes() == null) {
              appearance.setTransparencyAttributes(
                  new TransparencyAttributes(TransparencyAttributes.NICEST, 0));
            }             
          }
        }
      }
    } 
  }


  public void checkAppearancesName(Node node) {
    Set<Appearance> appearances = new LinkedHashSet<Appearance>(); 
    searchAppearances(node, appearances);
    int i = 0;
    for (Appearance appearance : appearances) {
      try {
        if (appearance.getName() == null) {
          appearance.setName("Texture_" + ++i);
        }
      } catch (NoSuchMethodError ex) {
        break;
      }
    }
  }


  public HomeMaterial [] getMaterials(Node node) {
    Set<Appearance> appearances = new HashSet<Appearance>(); 
    searchAppearances(node, appearances);
    Set<HomeMaterial> materials = new TreeSet<HomeMaterial>(new Comparator<HomeMaterial>() {
        public int compare(HomeMaterial m1, HomeMaterial m2) {
          String name1 = m1.getName();
          String name2 = m2.getName();
          if (name1 != null) {
            if (name2 != null) {
              return name1.compareTo(name2);
            } else {
              return 1;
            }
          } else if (name2 != null) {
            return -1;
          } else {
            return 0;
          }
        }
      });
    for (Appearance appearance : appearances) {
      Integer color = null;
      Float   shininess = null;
      Material material = appearance.getMaterial();
      if (material != null) {
        Color3f diffuseColor = new Color3f();
        material.getDiffuseColor(diffuseColor);
        color = 0xFF000000 
            | ((int)(diffuseColor.x * 255) << 16) 
            | ((int)(diffuseColor.y * 255) << 8)
            | (int)(diffuseColor.z * 255); 
        shininess = material.getShininess() / 128;          
      }
      Texture appearanceTexture = appearance.getTexture();
      HomeTexture texture = null;
      if (appearanceTexture != null) {
        URL textureImageUrl = (URL)appearanceTexture.getUserData();
        if (textureImageUrl != null) {
          Content textureImage = new SimpleURLContent(textureImageUrl);
          String textureImageName = textureImageUrl.getFile();
          textureImageName = textureImageName.substring(textureImageName.lastIndexOf('/') + 1);
          int lastPoint = textureImageName.lastIndexOf('.');
          if (lastPoint != -1) {
            textureImageName = textureImageName.substring(0, lastPoint);
          }
          texture = new HomeTexture(new CatalogTexture(textureImageName, textureImage, -1, -1));
        }
      }
      try {
        materials.add(new HomeMaterial(appearance.getName(), color, texture, shininess));
      } catch (NoSuchMethodError ex) {
        return new HomeMaterial [0];
      }
    }
    return materials.toArray(new HomeMaterial [materials.size()]);
  }

  private void searchAppearances(Node node, Set<Appearance> appearances) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        searchAppearances((Node)enumeration.nextElement(), appearances);
      }
    } else if (node instanceof Link) {
      searchAppearances(((Link)node).getSharedGroup(), appearances);
    } else if (node instanceof Shape3D) {
      Appearance appearance = ((Shape3D)node).getAppearance();
      if (appearance != null) {
        appearances.add(appearance);
      }
    }
  }


  Area getFrontArea(String cutOutShape, Node node) {
    Area frontArea;
    if (cutOutShape != null) {
      frontArea = new Area(getShape(cutOutShape));
      frontArea.transform(AffineTransform.getScaleInstance(1, -1));
      frontArea.transform(AffineTransform.getTranslateInstance(-.5, .5));
    } else {
      int vertexCount = getVertexCount(node);
      if (vertexCount < 1000000) {
        Area frontAreaWithHoles = new Area();
        computeBottomOrFrontArea(node, frontAreaWithHoles, new Transform3D(), false, false);
        frontArea = new Area();
        List<float []> currentPathPoints = new ArrayList<float[]>();
        float [] previousRoomPoint = null;
        for (PathIterator it = frontAreaWithHoles.getPathIterator(null, 1); !it.isDone(); it.next()) {
          float [] areaPoint = new float[2];
          switch (it.currentSegment(areaPoint)) {
            case PathIterator.SEG_MOVETO :
            case PathIterator.SEG_LINETO : 
              if (previousRoomPoint == null
                  || areaPoint [0] != previousRoomPoint [0] 
                  || areaPoint [1] != previousRoomPoint [1]) {
                currentPathPoints.add(areaPoint);
              }
              previousRoomPoint = areaPoint;
              break;
            case PathIterator.SEG_CLOSE :
              if (currentPathPoints.get(0) [0] == previousRoomPoint [0] 
                  && currentPathPoints.get(0) [1] == previousRoomPoint [1]) {
                currentPathPoints.remove(currentPathPoints.size() - 1);
              }
              if (currentPathPoints.size() > 2) {
                float [][] pathPoints = 
                    currentPathPoints.toArray(new float [currentPathPoints.size()][]);
                Room subRoom = new Room(pathPoints);
                if (subRoom.getArea() > 0) {
                  if (!subRoom.isClockwise()) {
                    GeneralPath currentPath = new GeneralPath();
                    currentPath.moveTo(pathPoints [0][0], pathPoints [0][1]);
                    for (int i = 1; i < pathPoints.length; i++) {
                      currentPath.lineTo(pathPoints [i][0], pathPoints [i][1]);
                    }
                    currentPath.closePath();
                    frontArea.add(new Area(currentPath));
                  }
                }
              }
              currentPathPoints.clear();
              previousRoomPoint = null;
              break;
          }
        }
        Rectangle2D bounds = frontAreaWithHoles.getBounds2D();
        frontArea.transform(AffineTransform.getTranslateInstance(-bounds.getCenterX(), -bounds.getCenterY()));
        frontArea.transform(AffineTransform.getScaleInstance(1 / bounds.getWidth(), 1 / bounds.getHeight()));        
      } else {
        frontArea = new Area(new Rectangle2D.Float(-.5f, -.5f, 1, 1));
      }    
    }
    return frontArea;
  }
  

  public Area getAreaOnFloor(Node node) {
    Area modelAreaOnFloor;
    int vertexCount = getVertexCount(node);
    if (vertexCount < 10000) {
      modelAreaOnFloor = new Area();
      computeBottomOrFrontArea(node, modelAreaOnFloor, new Transform3D(), true, true);
    } else {
      List<float []> vertices = new ArrayList<float[]>(vertexCount); 
      computeVerticesOnFloor(node, vertices, new Transform3D());
      float [][] surroundingPolygon = getSurroundingPolygon(vertices.toArray(new float [vertices.size()][]));
      GeneralPath generalPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, surroundingPolygon.length);
      generalPath.moveTo(surroundingPolygon [0][0], surroundingPolygon [0][1]);
      for (int i = 0; i < surroundingPolygon.length; i++) {
        generalPath.lineTo(surroundingPolygon [i][0], surroundingPolygon [i][1]);
      }
      generalPath.closePath();
      modelAreaOnFloor = new Area(generalPath);
    }
    return modelAreaOnFloor;
  }

  private int getVertexCount(Node node) {
    int count = 0;
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren();
      while (enumeration.hasMoreElements()) {
        count += getVertexCount((Node)enumeration.nextElement());
      }
    } else if (node instanceof Link) {
      count = getVertexCount(((Link)node).getSharedGroup());
    } else if (node instanceof Shape3D) {
      Shape3D shape = (Shape3D)node;
      Appearance appearance = shape.getAppearance();
      RenderingAttributes renderingAttributes = appearance != null 
          ? appearance.getRenderingAttributes() : null;
      if (renderingAttributes == null
          || renderingAttributes.getVisible()) {
        for (int i = 0, n = shape.numGeometries(); i < n; i++) {
          Geometry geometry = shape.getGeometry(i);
          if (geometry instanceof GeometryArray) {
            count += ((GeometryArray)geometry).getVertexCount();
          }
        }
      }
    }    
    return count;
  }
  
  private void computeBottomOrFrontArea(Node node, 
                                        Area nodeArea, 
                                        Transform3D parentTransformations,
                                        boolean ignoreTransparentShapes, 
                                        boolean bottom) {
    if (node instanceof Group) {
      if (node instanceof TransformGroup) {
        parentTransformations = new Transform3D(parentTransformations);
        Transform3D transform = new Transform3D();
        ((TransformGroup)node).getTransform(transform);
        parentTransformations.mul(transform);
      }
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        computeBottomOrFrontArea((Node)enumeration.nextElement(), nodeArea, parentTransformations, ignoreTransparentShapes, bottom);
      }
    } else if (node instanceof Link) {
      computeBottomOrFrontArea(((Link)node).getSharedGroup(), nodeArea, parentTransformations, ignoreTransparentShapes, bottom);
    } else if (node instanceof Shape3D) {
      Shape3D shape = (Shape3D)node;
      Appearance appearance = shape.getAppearance();
      RenderingAttributes renderingAttributes = appearance != null 
          ? appearance.getRenderingAttributes() : null;
      TransparencyAttributes transparencyAttributes = appearance != null 
          ? appearance.getTransparencyAttributes() : null;
      if ((renderingAttributes == null
            || renderingAttributes.getVisible())
          && (!ignoreTransparentShapes
              || transparencyAttributes == null
              || transparencyAttributes.getTransparency() < 1)) {
        for (int i = 0, n = shape.numGeometries(); i < n; i++) {
          computeBottomOrFrontGeometryArea(shape.getGeometry(i), nodeArea, parentTransformations, bottom);
        }
      }
    }    
  }
  

  private void computeBottomOrFrontGeometryArea(Geometry geometry, 
                                                Area nodeArea, 
                                                Transform3D parentTransformations,
                                                boolean bottom) {
    if (geometry instanceof GeometryArray) {
      GeometryArray geometryArray = (GeometryArray)geometry;      

      int vertexCount = geometryArray.getVertexCount();
      float [] vertices = new float [vertexCount * 2]; 
      Point3f vertex = new Point3f();
      if ((geometryArray.getVertexFormat() & GeometryArray.BY_REFERENCE) != 0) {
        if ((geometryArray.getVertexFormat() & GeometryArray.INTERLEAVED) != 0) {
          float [] vertexData = geometryArray.getInterleavedVertices();
          int vertexSize = vertexData.length / vertexCount;
          for (int index = 0, i = vertexSize - 3; index < vertices.length; i += vertexSize) {
            vertex.x = vertexData [i];
            vertex.y = vertexData [i + 1];
            vertex.z = vertexData [i + 2];
            parentTransformations.transform(vertex);
            vertices [index++] = vertex.x;
            if (bottom) {
              vertices [index++] = vertex.z;
            } else {
              vertices [index++] = vertex.y;
            }
          }
        } else {
          float [] vertexCoordinates = geometryArray.getCoordRefFloat();
          for (int index = 0, i = 0; index < vertices.length; i += 3) {
            vertex.x = vertexCoordinates [i];
            vertex.y = vertexCoordinates [i + 1];
            vertex.z = vertexCoordinates [i + 2];
            parentTransformations.transform(vertex);
            vertices [index++] = vertex.x;
            if (bottom) {
              vertices [index++] = vertex.z;
            } else {
              vertices [index++] = vertex.y;
            }
          }
        }
      } else {
        for (int index = 0, i = 0; index < vertices.length; i++) {
          geometryArray.getCoordinate(i, vertex);
          parentTransformations.transform(vertex);
          vertices [index++] = vertex.x;
          if (bottom) {
            vertices [index++] = vertex.z;
          } else {
            vertices [index++] = vertex.y;
          }
        }
      }

      GeneralPath geometryPath = null;
      if (geometryArray instanceof IndexedGeometryArray) {
        if (geometryArray instanceof IndexedTriangleArray) {
          IndexedTriangleArray triangleArray = (IndexedTriangleArray)geometryArray;
          geometryPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1000);
          for (int i = 0, triangleIndex = 0, n = triangleArray.getIndexCount(); i < n; i += 3) {
            addIndexedTriangleToPath(triangleArray, i, i + 1, i + 2, vertices, 
                geometryPath, triangleIndex++, nodeArea);
          }
        } else if (geometryArray instanceof IndexedQuadArray) {
          IndexedQuadArray quadArray = (IndexedQuadArray)geometryArray;
          geometryPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1000);
          for (int i = 0, quadrilateralIndex = 0, n = quadArray.getIndexCount(); i < n; i += 4) {
            addIndexedQuadrilateralToPath(quadArray, i, i + 1, i + 2, i + 3, vertices, 
                geometryPath, quadrilateralIndex++, nodeArea); 
          }
        } else if (geometryArray instanceof IndexedGeometryStripArray) {
          IndexedGeometryStripArray geometryStripArray = (IndexedGeometryStripArray)geometryArray;
          int [] stripIndexCounts = new int [geometryStripArray.getNumStrips()];
          geometryStripArray.getStripIndexCounts(stripIndexCounts);
          geometryPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1000);
          int initialIndex = 0; 
          
          if (geometryStripArray instanceof IndexedTriangleStripArray) {
            for (int strip = 0, triangleIndex = 0; strip < stripIndexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripIndexCounts [strip] - 2, j = 0; i < n; i++, j++) {
                if (j % 2 == 0) {
                  addIndexedTriangleToPath(geometryStripArray, i, i + 1, i + 2, vertices, 
                      geometryPath, triangleIndex++, nodeArea); 
                } else {          
                  addIndexedTriangleToPath(geometryStripArray, i, i + 2, i + 1, vertices, 
                      geometryPath, triangleIndex++, nodeArea);
                }
              }
              initialIndex += stripIndexCounts [strip];
            }
          } else if (geometryStripArray instanceof IndexedTriangleFanArray) {
            for (int strip = 0, triangleIndex = 0; strip < stripIndexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripIndexCounts [strip] - 2; i < n; i++) {
                addIndexedTriangleToPath(geometryStripArray, initialIndex, i + 1, i + 2, vertices, 
                    geometryPath, triangleIndex++, nodeArea); 
              }
              initialIndex += stripIndexCounts [strip];
            }
          }
        }
      } else {
        if (geometryArray instanceof TriangleArray) {
          TriangleArray triangleArray = (TriangleArray)geometryArray;
          geometryPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1000);
          for (int i = 0, triangleIndex = 0; i < vertexCount; i += 3) {
            addTriangleToPath(triangleArray, i, i + 1, i + 2, vertices, 
                geometryPath, triangleIndex++, nodeArea);
          }
        } else if (geometryArray instanceof QuadArray) {
          QuadArray quadArray = (QuadArray)geometryArray;
          geometryPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1000);
          for (int i = 0, quadrilateralIndex = 0; i < vertexCount; i += 4) {
            addQuadrilateralToPath(quadArray, i, i + 1, i + 2, i + 3, vertices, 
                geometryPath, quadrilateralIndex++, nodeArea);
          }
        } else if (geometryArray instanceof GeometryStripArray) {
          GeometryStripArray geometryStripArray = (GeometryStripArray)geometryArray;
          int [] stripVertexCounts = new int [geometryStripArray.getNumStrips()];
          geometryStripArray.getStripVertexCounts(stripVertexCounts);
          geometryPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 1000);
          int initialIndex = 0;
          
          if (geometryStripArray instanceof TriangleStripArray) {
            for (int strip = 0, triangleIndex = 0; strip < stripVertexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripVertexCounts [strip] - 2, j = 0; i < n; i++, j++) {
                if (j % 2 == 0) {
                  addTriangleToPath(geometryStripArray, i, i + 1, i + 2, vertices, 
                      geometryPath, triangleIndex++, nodeArea);
                } else { // Vertices of odd triangles are in reverse order               
                  addTriangleToPath(geometryStripArray, i, i + 2, i + 1, vertices, 
                      geometryPath, triangleIndex++, nodeArea);
                }
              }
              initialIndex += stripVertexCounts [strip];
            }
          } else if (geometryStripArray instanceof TriangleFanArray) {
            for (int strip = 0, triangleIndex = 0; strip < stripVertexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripVertexCounts [strip] - 2; i < n; i++) {
                addTriangleToPath(geometryStripArray, initialIndex, i + 1, i + 2, vertices, 
                    geometryPath, triangleIndex++, nodeArea);
              }
              initialIndex += stripVertexCounts [strip];
            }
          }
        }
      }
      
      if (geometryPath != null) {
        nodeArea.add(new Area(geometryPath));
      }
    } 
  }

  private void addIndexedTriangleToPath(IndexedGeometryArray geometryArray, 
                                    int vertexIndex1, int vertexIndex2, int vertexIndex3, 
                                    float [] vertices, 
                                    GeneralPath geometryPath, int triangleIndex, Area nodeArea) {
    addTriangleToPath(geometryArray, geometryArray.getCoordinateIndex(vertexIndex1), 
        geometryArray.getCoordinateIndex(vertexIndex2), 
        geometryArray.getCoordinateIndex(vertexIndex3), vertices, geometryPath, triangleIndex, nodeArea);
  }

  private void addIndexedQuadrilateralToPath(IndexedGeometryArray geometryArray, 
                                         int vertexIndex1, int vertexIndex2, int vertexIndex3, int vertexIndex4, 
                                         float [] vertices, 
                                         GeneralPath geometryPath, int quadrilateralIndex, Area nodeArea) {
    addQuadrilateralToPath(geometryArray, geometryArray.getCoordinateIndex(vertexIndex1), 
        geometryArray.getCoordinateIndex(vertexIndex2), 
        geometryArray.getCoordinateIndex(vertexIndex3), 
        geometryArray.getCoordinateIndex(vertexIndex4), vertices, geometryPath, quadrilateralIndex, nodeArea);
  }
  

  private void addTriangleToPath(GeometryArray geometryArray, 
                             int vertexIndex1, int vertexIndex2, int vertexIndex3, 
                             float [] vertices, 
                             GeneralPath geometryPath, int triangleIndex, Area nodeArea) {
    float xVertex1 = vertices [2 * vertexIndex1];
    float yVertex1 = vertices [2 * vertexIndex1 + 1];
    float xVertex2 = vertices [2 * vertexIndex2];
    float yVertex2 = vertices [2 * vertexIndex2 + 1];
    float xVertex3 = vertices [2 * vertexIndex3];
    float yVertex3 = vertices [2 * vertexIndex3 + 1];
    if ((xVertex2 - xVertex1) * (yVertex3 - yVertex2) - (yVertex2 - yVertex1) * (xVertex3 - xVertex2) > 0) {
      if (triangleIndex > 0 && triangleIndex % 1000 == 0) {

        nodeArea.add(new Area(geometryPath));
        geometryPath.reset();
      }
      geometryPath.moveTo(xVertex1, yVertex1);      
      geometryPath.lineTo(xVertex2, yVertex2);      
      geometryPath.lineTo(xVertex3, yVertex3);
      geometryPath.closePath();
    }
  }

  private void addQuadrilateralToPath(GeometryArray geometryArray, 
                                      int vertexIndex1, int vertexIndex2, int vertexIndex3, int vertexIndex4, 
                                      float [] vertices, 
                                      GeneralPath geometryPath, int quadrilateralIndex, Area nodeArea) {
    float xVertex1 = vertices [2 * vertexIndex1];
    float yVertex1 = vertices [2 * vertexIndex1 + 1];
    float xVertex2 = vertices [2 * vertexIndex2];
    float yVertex2 = vertices [2 * vertexIndex2 + 1];
    float xVertex3 = vertices [2 * vertexIndex3];
    float yVertex3 = vertices [2 * vertexIndex3 + 1];
    if ((xVertex2 - xVertex1) * (yVertex3 - yVertex2) - (yVertex2 - yVertex1) * (xVertex3 - xVertex2) > 0) {
      if (quadrilateralIndex > 0 && quadrilateralIndex % 1000 == 0) {
        nodeArea.add(new Area(geometryPath));
        geometryPath.reset();
      }
      geometryPath.moveTo(xVertex1, yVertex1);      
      geometryPath.lineTo(xVertex2, yVertex2);      
      geometryPath.lineTo(xVertex3, yVertex3);
      geometryPath.lineTo(vertices [2 * vertexIndex4], vertices [2 * vertexIndex4 + 1]);
      geometryPath.closePath();
    }
  }

  private void computeVerticesOnFloor(Node node, List<float []> vertices, Transform3D parentTransformations) {
    if (node instanceof Group) {
      if (node instanceof TransformGroup) {
        parentTransformations = new Transform3D(parentTransformations);
        Transform3D transform = new Transform3D();
        ((TransformGroup)node).getTransform(transform);
        parentTransformations.mul(transform);
      }
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        computeVerticesOnFloor((Node)enumeration.nextElement(), vertices, parentTransformations);
      }
    } else if (node instanceof Link) {
      computeVerticesOnFloor(((Link)node).getSharedGroup(), vertices, parentTransformations);
    } else if (node instanceof Shape3D) {
      Shape3D shape = (Shape3D)node;
      Appearance appearance = shape.getAppearance();
      RenderingAttributes renderingAttributes = appearance != null 
          ? appearance.getRenderingAttributes() : null;
      TransparencyAttributes transparencyAttributes = appearance != null 
          ? appearance.getTransparencyAttributes() : null;
      if ((renderingAttributes == null
            || renderingAttributes.getVisible())
          && (transparencyAttributes == null
              || transparencyAttributes.getTransparency() < 1)) {

        for (int i = 0, n = shape.numGeometries(); i < n; i++) {
          Geometry geometry = shape.getGeometry(i);
          if (geometry instanceof GeometryArray) {
            GeometryArray geometryArray = (GeometryArray)geometry;      

            int vertexCount = geometryArray.getVertexCount();
            Point3f vertex = new Point3f();
            if ((geometryArray.getVertexFormat() & GeometryArray.BY_REFERENCE) != 0) {
              if ((geometryArray.getVertexFormat() & GeometryArray.INTERLEAVED) != 0) {
                float [] vertexData = geometryArray.getInterleavedVertices();
                int vertexSize = vertexData.length / vertexCount;

                for (int index = 0, j = vertexSize - 3; index < vertexCount; j += vertexSize, index++) {
                  vertex.x = vertexData [j];
                  vertex.y = vertexData [j + 1];
                  vertex.z = vertexData [j + 2];
                  parentTransformations.transform(vertex);
                  vertices.add(new float [] {vertex.x, vertex.z});
                }
              } else {

                float [] vertexCoordinates = geometryArray.getCoordRefFloat();
                for (int index = 0, j = 0; index < vertexCount; j += 3, index++) {
                  vertex.x = vertexCoordinates [j];
                  vertex.y = vertexCoordinates [j + 1];
                  vertex.z = vertexCoordinates [j + 2];
                  parentTransformations.transform(vertex);
                  vertices.add(new float [] {vertex.x, vertex.z});
                }
              }
            } else {
              for (int index = 0, j = 0; index < vertexCount; j++, index++) {
                geometryArray.getCoordinate(j, vertex);
                parentTransformations.transform(vertex);
                vertices.add(new float [] {vertex.x, vertex.z});
              }
            }
          }
        }
      }
    }    
  }
  

  private float [][] getSurroundingPolygon(float [][] vertices) {
    Arrays.sort(vertices, new Comparator<float []> () {
        public int compare(float [] vertex1, float [] vertex2) {
          if (vertex1 [0] == vertex2 [0]) {
            return (int)Math.signum(vertex2 [1] - vertex1 [1]);
          } else {
            return (int)Math.signum(vertex2 [0] - vertex1 [0]);
          }
        }
      });
    float [][] polygon = new float [vertices.length][];

    int bottom = 0, top = -1; 
    int i; 

    int minMin = 0, minMax;
    float xmin = vertices [0][0];
    for (i = 1; i < vertices.length; i++) {
      if (vertices [i][0] != xmin) {
        break;
      }
    }
    minMax = i - 1;
    if (minMax == vertices.length - 1) { 

      polygon [++top] = vertices [minMin];
      if (vertices [minMax][1] != vertices [minMin][1]) { 

        polygon [++top] = vertices [minMax];
      }

      polygon [++top] = vertices [minMin];
      float [][] surroundingPolygon = new float [top + 1][];
      System.arraycopy(polygon, 0, surroundingPolygon, 0, surroundingPolygon.length);
    }


    int maxMin, maxMax = vertices.length - 1;
    float xMax = vertices [vertices.length - 1][0];
    for (i = vertices.length - 2; i >= 0; i--) {
      if (vertices [i][0] != xMax) {
        break;
      }
    }
    maxMin = i + 1;

    polygon [++top] = vertices [minMin];
    i = minMax;
    while (++i <= maxMin) {

      if (isLeft(vertices [minMin], vertices [maxMin], vertices [i]) >= 0 && i < maxMin) {

        continue; 
      }

      while (top > 0) 
      {

        if (isLeft(polygon [top - 1], polygon [top], vertices [i]) > 0)
          break; 
        else
          top--; 
      }
      polygon [++top] = vertices [i]; // push points [i] onto stack
    }


    if (maxMax != maxMin) { 

      polygon [++top] = vertices [maxMax]; 
    }

    bottom = top; 
    i = maxMin;
    while (--i >= minMax) {

      if (isLeft(vertices [maxMax], vertices [minMax], vertices [i]) >= 0 && i > minMax) {

        continue; 
      }


      while (top > bottom) 
      {

        if (isLeft(polygon [top - 1], polygon [top], vertices [i]) > 0) {

          break; 
        } else {

          top--; 
        }
      }

      polygon [++top] = vertices [i]; 
    }
    if (minMax != minMin) {

      polygon [++top] = vertices [minMin]; 
    }

    float [][] surroundingPolygon = new float [top + 1][];
    System.arraycopy(polygon, 0, surroundingPolygon, 0, surroundingPolygon.length);
    return surroundingPolygon;
  }

  private float isLeft(float [] vertex0, float [] vertex1, float [] vertex2) {
    return (vertex1 [0] - vertex0 [0]) * (vertex2 [1] - vertex0 [1]) 
         - (vertex2 [0] - vertex0 [0]) * (vertex1 [1] - vertex0 [1]);
  }


  public Area getAreaOnFloor(HomePieceOfFurniture staircase) {
    if (staircase.getStaircaseCutOutShape() == null) {
      throw new IllegalArgumentException("No cut out shape associated to piece");
    }
    Shape shape = getShape(staircase.getStaircaseCutOutShape());
    Area staircaseArea = new Area(shape);
    if (staircase.isModelMirrored()) {
      staircaseArea = getMirroredArea(staircaseArea);
    }
    AffineTransform staircaseTransform = AffineTransform.getTranslateInstance(
        staircase.getX() - staircase.getWidth() / 2, 
        staircase.getY() - staircase.getDepth() / 2);
    staircaseTransform.concatenate(AffineTransform.getRotateInstance(staircase.getAngle(),
        staircase.getWidth() / 2, staircase.getDepth() / 2));
    staircaseTransform.concatenate(AffineTransform.getScaleInstance(staircase.getWidth(), staircase.getDepth()));
    staircaseArea.transform(staircaseTransform);
    return staircaseArea;
  }


  private Area getMirroredArea(Area area) {

    GeneralPath mirrorPath = new GeneralPath();
    float [] point = new float[6];
    for (PathIterator it = area.getPathIterator(null); !it.isDone(); it.next()) {
      switch (it.currentSegment(point)) {
        case PathIterator.SEG_MOVETO :
          mirrorPath.moveTo(1 - point[0], point[1]);
          break;
        case PathIterator.SEG_LINETO : 
          mirrorPath.lineTo(1 - point[0], point[1]);
          break;
        case PathIterator.SEG_QUADTO : 
          mirrorPath.quadTo(1 - point[0], point[1], 1 - point[2], point[3]);
          break;
        case PathIterator.SEG_CUBICTO : 
          mirrorPath.curveTo(1 - point[0], point[1], 1 - point[2], point[3], 1 - point[4], point[5]);
          break;
        case PathIterator.SEG_CLOSE :
          mirrorPath.closePath();
          break;
      }
    }
    return new Area(mirrorPath);
  }

  public Shape getShape(String svgPathShape) {
    Shape shape = this.parsedShapes.get(svgPathShape);
    if (shape == null) {
      try {
        shape = SVGPathSupport.parsePathShape(svgPathShape);
      } catch (LinkageError ex) {

        shape = new Rectangle2D.Float(0, 0, 1, 1);
      }
      this.parsedShapes.put(svgPathShape, shape);
    }
    return shape;
  }

  private static class SVGPathSupport {
    public static Shape parsePathShape(String svgPathShape) {
      try {
        AWTPathProducer pathProducer = new AWTPathProducer();
        PathParser pathParser = new PathParser();
        pathParser.setPathHandler(pathProducer);
        pathParser.parse(svgPathShape);
        return pathProducer.getShape();
      } catch (ParseException ex) {

        return new Rectangle2D.Float(0, 0, 1, 1);
      }
    }
  }

  public static interface ModelObserver {
    public void modelUpdated(BranchGroup modelRoot); 
    
    public void modelError(Exception ex);
  }
}
