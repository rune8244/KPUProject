package com.eteks.homeview3d.j3d;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingLeaf;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.CapabilityNotSetException;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.Link;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PointLight;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.eteks.homeview3d.j3d.TextureManager.TextureObserver;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.HomeFurnitureGroup;
import com.eteks.homeview3d.model.HomeLight;
import com.eteks.homeview3d.model.HomeMaterial;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Light;
import com.eteks.homeview3d.model.LightSource;
import com.eteks.homeview3d.model.Room;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.SelectionEvent;
import com.eteks.homeview3d.model.SelectionListener;
import com.sun.j3d.utils.geometry.Box;

/**
 * 가구 루트피스.
 */
public class HomePieceOfFurniture3D extends Object3DBranch {
  private static final TransparencyAttributes DEFAULT_TEXTURED_SHAPE_TRANSPARENCY_ATTRIBUTES = 
      new TransparencyAttributes(TransparencyAttributes.NICEST, 0);
  private static final PolygonAttributes      DEFAULT_TEXTURED_SHAPE_POLYGON_ATTRIBUTES = 
      new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0, false);
  private static final PolygonAttributes      NORMAL_FLIPPED_TEXTURED_SHAPE_POLYGON_ATTRIBUTES = 
      new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0, true);
  private static final Bounds                 DEFAULT_INFLUENCING_BOUNDS = new BoundingSphere(new Point3d(), 1E7);

  private final Home home;
  
  static {
    DEFAULT_TEXTURED_SHAPE_POLYGON_ATTRIBUTES.setCapability(PolygonAttributes.ALLOW_CULL_FACE_READ);
    NORMAL_FLIPPED_TEXTURED_SHAPE_POLYGON_ATTRIBUTES.setCapability(PolygonAttributes.ALLOW_CULL_FACE_READ);
  }
  
  /**
   * 3d 조각들 생성.
   */
  public HomePieceOfFurniture3D(HomePieceOfFurniture piece, Home home) {
    this(piece, home, false, false);
  }

  /**
   * 주어진 집에 3d 조각들 매칭.
   */
  public HomePieceOfFurniture3D(HomePieceOfFurniture piece, 
                                Home home, 
                                boolean ignoreDrawingMode, 
                                boolean waitModelAndTextureLoadingEnd) {
    setUserData(piece);      
    this.home = home;

    setCapability(BranchGroup.ALLOW_DETACH);
    setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
    
    createPieceOfFurnitureNode(piece, ignoreDrawingMode, waitModelAndTextureLoadingEnd);
  }

  private void createPieceOfFurnitureNode(final HomePieceOfFurniture piece, 
                                          final boolean ignoreDrawingMode, 
                                          final boolean waitModelAndTextureLoadingEnd) {
    final TransformGroup pieceTransformGroup = new TransformGroup();

    pieceTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    pieceTransformGroup.setCapability(Group.ALLOW_CHILDREN_READ);
    pieceTransformGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
    pieceTransformGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    addChild(pieceTransformGroup);
    
    if (piece instanceof HomeLight) {
      BoundingLeaf bounds = new BoundingLeaf();
      bounds.setCapability(BoundingLeaf.ALLOW_REGION_WRITE);
      addChild(bounds);
    }

    // 모델 로딩동안 화이트 박스 출력
    final BranchGroup waitBranch = new BranchGroup();
    waitBranch.setCapability(BranchGroup.ALLOW_DETACH);
    waitBranch.addChild(getModelBox(Color.WHITE));      
    setModelCapabilities(waitBranch);
    
    pieceTransformGroup.addChild(waitBranch);
    
  
    updatePieceOfFurnitureTransform();
    
    // 진짜 3d 모델 로드
    Content model = piece.getModel();
    ModelManager.getInstance().loadModel(model, waitModelAndTextureLoadingEnd,
        new ModelManager.ModelObserver() {
          public void modelUpdated(BranchGroup modelRoot) {
            float [][] modelRotation = piece.getModelRotation();
            TransformGroup modelTransformGroup = 
                ModelManager.getInstance().getNormalizedTransformGroup(modelRoot, modelRotation, 1);
            
            cloneHomeTextures(modelRoot);
            updatePieceOfFurnitureModelNode(modelRoot, modelTransformGroup, ignoreDrawingMode, waitModelAndTextureLoadingEnd);            
          }
          
          public void modelError(Exception ex) {
            updatePieceOfFurnitureModelNode(getModelBox(Color.RED), new TransformGroup(), ignoreDrawingMode, waitModelAndTextureLoadingEnd);            
          }
          
          /**
           * 텍스쳐 세팅 교체.
           */
          private void cloneHomeTextures(Node node) {
            if (node instanceof Group) {
              Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
              while (enumeration.hasMoreElements()) {
                cloneHomeTextures((Node)enumeration.nextElement());
              }
            } else if (node instanceof Link) {
              cloneHomeTextures(((Link)node).getSharedGroup());
            } else if (node instanceof Shape3D) {
              Appearance appearance = ((Shape3D)node).getAppearance();
              if (appearance != null) {
                Texture texture = appearance.getTexture();
                if (texture != null) {
                  appearance.setTexture(getHomeTextureClone(texture, home));
                }
              }
            } 
          }
        });
  }

  /**
   * 이 브랜치 업데이트.
   */
  @Override
  public void update() {
    updatePieceOfFurnitureTransform();
    updatePieceOfFurnitureModelMirrored();
    updatePieceOfFurnitureColorAndTexture(false);      
    updateLight();
    updatePieceOfFurnitureVisibility();      
  }

  private void updatePieceOfFurnitureTransform() {
    Transform3D pieceTransform = ModelManager.getInstance().
        getPieceOFFurnitureNormalizedModelTransformation((HomePieceOfFurniture)getUserData());
    
    ((TransformGroup)getChild(0)).setTransform(pieceTransform);
  }

  /**
   * 컬러 및 텍스쳐 세팅.
   */
  private void updatePieceOfFurnitureColorAndTexture(boolean waitTextureLoadingEnd) {
    HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
    Node filledModelNode = getFilledModelNode();
    if (piece.getColor() != null) {
      setColorAndTexture(filledModelNode, piece.getColor(), null, piece.getShininess(), null, false, 
          null, null, new HashSet<Appearance>());
    } else if (piece.getTexture() != null) {
      setColorAndTexture(filledModelNode, null, piece.getTexture(), piece.getShininess(), null, waitTextureLoadingEnd,
          new Vector3f(piece.getWidth(), piece.getHeight(), piece.getDepth()), ModelManager.getInstance().getBounds(((Group)filledModelNode).getChild(0)),
          new HashSet<Appearance>());
    } else if (piece.getModelMaterials() != null) {
      setColorAndTexture(filledModelNode, null, null, null, piece.getModelMaterials(), waitTextureLoadingEnd,
          new Vector3f(piece.getWidth(), piece.getHeight(), piece.getDepth()), ModelManager.getInstance().getBounds(((Group)filledModelNode).getChild(0)), 
          new HashSet<Appearance>());
    } else {
      setColorAndTexture(filledModelNode, null, null, piece.getShininess(), null, false, 
          null, null, new HashSet<Appearance>());
    }
  }

  /**
   * 빛 색상 설정. 
   */
  private void updateLight() {
    HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
    if (piece instanceof HomeLight
        && this.home != null) {
      boolean enabled = this.home.getEnvironment().getSubpartSizeUnderLight() > 0 
          && piece.isVisible() 
          && (piece.getLevel() == null
            || piece.getLevel().isViewableAndVisible());
      HomeLight light = (HomeLight)piece;
      LightSource [] lightSources = light.getLightSources();
      if (numChildren() > 2) {
        Color homeLightColor = new Color(this.home.getEnvironment().getLightColor());
        float homeLightColorRed   = homeLightColor.getRed()   / 3072f; 
        float homeLightColorGreen = homeLightColor.getGreen() / 3072f; 
        float homeLightColorBlue  = homeLightColor.getBlue()  / 3072f; 
        float angle = light.getAngle();
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        Group lightsBranch = (Group)getChild(2); 
        for (int i = 0; i < lightSources.length; i++) {
          LightSource lightSource = lightSources [i];
          Color lightColor = new Color(lightSource.getColor());
          float power = light.getPower();
          PointLight pointLight = (PointLight)lightsBranch.getChild(i);
          pointLight.setColor(new Color3f(
              lightColor.getRed()   / 255f * power + (power > 0 ? homeLightColorRed : 0), 
              lightColor.getGreen() / 255f * power + (power > 0 ? homeLightColorGreen : 0), 
              lightColor.getBlue()  / 255f * power + (power > 0 ? homeLightColorBlue : 0)));
          float xLightSourceInLight = -light.getWidth() / 2 + (lightSource.getX() * light.getWidth());
          float yLightSourceInLight = light.getDepth() / 2 - (lightSource.getY() * light.getDepth());
          float lightElevation = light.getGroundElevation();
          pointLight.setPosition(
              light.getX() + xLightSourceInLight * cos - yLightSourceInLight * sin,
              lightElevation + (lightSource.getZ() * light.getHeight()),
              light.getY() + xLightSourceInLight * sin + yLightSourceInLight * cos);
          pointLight.setEnable(enabled);
        }
  
        if (enabled) {
          Bounds bounds = DEFAULT_INFLUENCING_BOUNDS;
          for (Room room : this.home.getRooms()) {
            Level roomLevel = room.getLevel();
            if (light.isAtLevel(roomLevel)) {
              Shape roomShape = getShape(room.getPoints());
              if (roomShape.contains(light.getX(), light.getY())) {
                Rectangle roomBounds = roomShape.getBounds();
                float minElevation = roomLevel != null 
                    ? roomLevel.getElevation() 
                    : 0;
                float maxElevation =  roomLevel != null 
                    ? minElevation + roomLevel.getHeight() 
                    : 1E7f;
                float epsilon = 0.1f;
                bounds = new BoundingBox(
                    new Point3d(roomBounds.getMinX() - epsilon, minElevation - epsilon, roomBounds.getMinY() - epsilon),
                    new Point3d(roomBounds.getMaxX() + epsilon, maxElevation + epsilon, roomBounds.getMaxY() + epsilon));
                break;
              }
            }
          }
          ((BoundingLeaf)getChild(1)).setRegion(bounds);
        }
      }
    }
  }

  private Node getFilledModelNode() {
    TransformGroup transformGroup = (TransformGroup)getChild(0);
    BranchGroup branchGroup = (BranchGroup)transformGroup.getChild(0);
    return branchGroup.getChild(0);
  }

  private Node getOutlineModelNode() {
    TransformGroup transformGroup = (TransformGroup)getChild(0);
    BranchGroup branchGroup = (BranchGroup)transformGroup.getChild(0);
    if (branchGroup.numChildren() > 1) {
      return branchGroup.getChild(1);
    } else {
      return null;
    }
  }

  private void updatePieceOfFurnitureVisibility() {
    HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
    Node outlineModelNode = getOutlineModelNode();
    HomeEnvironment.DrawingMode drawingMode;
    if (this.home != null && outlineModelNode != null) {
      drawingMode = this.home.getEnvironment().getDrawingMode(); 
    } else {
      drawingMode = null; 
    }
    boolean visible = piece.isVisible() 
        && (piece.getLevel() == null
            || piece.getLevel().isViewableAndVisible()); 
    HomeMaterial [] materials = piece.getColor() == null && piece.getTexture() == null
        ? piece.getModelMaterials()
        : null;
    setVisible(getFilledModelNode(), visible
        && (drawingMode == null
            || drawingMode == HomeEnvironment.DrawingMode.FILL 
            || drawingMode == HomeEnvironment.DrawingMode.FILL_AND_OUTLINE),
        materials);
    if (outlineModelNode != null) {
      setVisible(outlineModelNode, visible
          && (drawingMode == HomeEnvironment.DrawingMode.OUTLINE
              || drawingMode == HomeEnvironment.DrawingMode.FILL_AND_OUTLINE),
          materials);
    }
  }

  private void updatePieceOfFurnitureModelMirrored() {
    HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
    setCullFace(getFilledModelNode(), piece.isModelMirrored(), piece.isBackFaceShown());
  }

  private void updatePieceOfFurnitureModelNode(Node modelNode,
                                               TransformGroup normalization,
                                               boolean ignoreDrawingMode,
                                               boolean waitTextureLoadingEnd) {    
    normalization.addChild(modelNode);
    setModelCapabilities(normalization);
    BranchGroup modelBranch = new BranchGroup();
    modelBranch.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    modelBranch.addChild(normalization);
    if (!ignoreDrawingMode) {
      // 윤곽 모델 노드 추가
      modelBranch.addChild(createOutlineModelNode(normalization));
    }

    TransformGroup transformGroup = (TransformGroup)getChild(0);
    // 이전 노드 삭제  
    transformGroup.removeAllChildren();
    transformGroup.addChild(modelBranch);

    HomePieceOfFurniture piece = (HomePieceOfFurniture)getUserData();
    if (piece instanceof HomeLight) {
      BranchGroup lightBranch = new BranchGroup();
      lightBranch.setCapability(ALLOW_CHILDREN_READ);
      HomeLight light = (HomeLight)piece;
      for (int i = light.getLightSources().length; i > 0 ; i--) {
        PointLight pointLight = new PointLight(new Color3f(), new Point3f(), new Point3f(0.25f, 0, 0.0000025f)); 
        pointLight.setCapability(PointLight.ALLOW_POSITION_WRITE);
        pointLight.setCapability(PointLight.ALLOW_COLOR_WRITE);
        pointLight.setCapability(PointLight.ALLOW_STATE_WRITE);
        BoundingLeaf bounds = (BoundingLeaf)getChild(1);
        pointLight.setInfluencingBoundingLeaf(bounds);
        lightBranch.addChild(pointLight);
      }
      addChild(lightBranch);
    }

    if (piece.isBackFaceShown()) {
      setBackFaceNormalFlip(getFilledModelNode(), true);
    }
    updatePieceOfFurnitureModelMirrored();
    updatePieceOfFurnitureColorAndTexture(waitTextureLoadingEnd);      
    updateLight();
    updatePieceOfFurnitureVisibility();

    if (this.home != null 
        && getUserData() instanceof Light) {
      this.home.addSelectionListener(new LightSelectionListener(this));
    }
  }

  private static class LightSelectionListener implements SelectionListener {
    private WeakReference<HomePieceOfFurniture3D>  piece;

    public LightSelectionListener(HomePieceOfFurniture3D piece) {
      this.piece = new WeakReference<HomePieceOfFurniture3D>(piece);
    }
    
    public void selectionChanged(SelectionEvent ev) {
      HomePieceOfFurniture3D piece3D = this.piece.get();
      Home home = (Home)ev.getSource();
      if (piece3D == null) {
        home.removeSelectionListener(this);
      } else {
        piece3D.updatePieceOfFurnitureVisibility();
      }
    }
  }
  
  private Node getModelBox(Color color) {
    Material material = new Material();
    material.setDiffuseColor(new Color3f(color));
    material.setAmbientColor(new Color3f(color.darker()));
    
    Appearance boxAppearance = new Appearance();
    boxAppearance.setMaterial(material);
    return new Box(0.5f, 0.5f, 0.5f, boxAppearance);
  }

  private Node createOutlineModelNode(Node modelNode) {
    Node node = ModelManager.getInstance().cloneNode(modelNode);
    setOutlineAppearance(node);
    return node;
  }
  
  private void setOutlineAppearance(Node node) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        setOutlineAppearance((Node)enumeration.nextElement());
      }
    } else if (node instanceof Link) {
      setOutlineAppearance(((Link)node).getSharedGroup());
    } else if (node instanceof Shape3D) {        
      Appearance outlineAppearance = new Appearance();
      ((Shape3D)node).setAppearance(outlineAppearance);
      outlineAppearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
      RenderingAttributes renderingAttributes = new RenderingAttributes();
      renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
      outlineAppearance.setRenderingAttributes(renderingAttributes);
      outlineAppearance.setColoringAttributes(Object3DBranch.OUTLINE_COLORING_ATTRIBUTES);
      outlineAppearance.setPolygonAttributes(Object3DBranch.OUTLINE_POLYGON_ATTRIBUTES);
      outlineAppearance.setLineAttributes(Object3DBranch.OUTLINE_LINE_ATTRIBUTES);
    }
  }

  private void setModelCapabilities(Node node) {
    if (node instanceof Group) {
      node.setCapability(Group.ALLOW_CHILDREN_READ);
      if (node instanceof TransformGroup) {
        node.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
      }
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        setModelCapabilities((Node)enumeration.nextElement());
      }
    } else if (node instanceof Link) {
      node.setCapability(Link.ALLOW_SHARED_GROUP_READ);
      setModelCapabilities(((Link)node).getSharedGroup());
    } else if (node instanceof Shape3D) {        
      Shape3D shape = (Shape3D)node;
      Appearance appearance = shape.getAppearance();
      if (appearance != null) {
        setAppearanceCapabilities(appearance);
      }
      Enumeration<?> enumeration = shape.getAllGeometries();
      while (enumeration.hasMoreElements()) {
        setGeometryCapabilities((Geometry)enumeration.nextElement());
      }
      node.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      node.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
      node.setCapability(Shape3D.ALLOW_BOUNDS_READ);
    }
  }

  private void setColorAndTexture(Node node, Integer color, HomeTexture texture, Float shininess, 
                                  HomeMaterial [] materials, boolean waitTextureLoadingEnd, 
                                  Vector3f pieceSize, BoundingBox modelBounds, 
                                  Set<Appearance> modifiedAppearances) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        setColorAndTexture((Node)enumeration.nextElement(), color, 
            texture, shininess, materials, waitTextureLoadingEnd, pieceSize,
            modelBounds, modifiedAppearances);
      }
    } else if (node instanceof Link) {
      setColorAndTexture(((Link)node).getSharedGroup(), color,
          texture, shininess, materials, waitTextureLoadingEnd, pieceSize,
          modelBounds, modifiedAppearances);
    } else if (node instanceof Shape3D) {
      final Shape3D shape = (Shape3D)node;
      String shapeName = (String)shape.getUserData();
      Appearance appearance = shape.getAppearance();
      if (appearance == null) {
        appearance = createAppearanceWithChangeCapabilities();
        ((Shape3D)node).setAppearance(appearance);
      }
      
      // 외관 체크
      if (!modifiedAppearances.contains(appearance)) {
        DefaultMaterialAndTexture defaultMaterialAndTexture = null;
        boolean colorModified = color != null;
        boolean textureModified = !colorModified 
            && texture != null;
        boolean materialModified = !colorModified
            && !textureModified
            && materials != null && materials.length > 0;
        boolean appearanceModified = colorModified            
            || textureModified
            || materialModified
            || shininess != null;
        boolean windowPane = shapeName != null
            && shapeName.startsWith(ModelManager.WINDOW_PANE_SHAPE_PREFIX);
        float materialShininess = 0;
        if (appearanceModified) {
          defaultMaterialAndTexture = (DefaultMaterialAndTexture)appearance.getUserData();
          if (defaultMaterialAndTexture == null) {
            defaultMaterialAndTexture = new DefaultMaterialAndTexture(appearance);
            appearance.setUserData(defaultMaterialAndTexture);
          }
          
          materialShininess = shininess != null
              ? shininess.floatValue()
              : (defaultMaterialAndTexture.getMaterial() != null
                  ? defaultMaterialAndTexture.getMaterial().getShininess() / 128f
                  : 0);
        }
        if (colorModified) {
          if (windowPane) {
            restoreDefaultMaterialAndTexture(appearance, materialShininess);
          } else {
            appearance.setMaterial(getMaterial(color, color, materialShininess));
            appearance.setTransparencyAttributes(defaultMaterialAndTexture.getTransparencyAttributes());
            appearance.setPolygonAttributes(defaultMaterialAndTexture.getPolygonAttributes());
            appearance.setTexCoordGeneration(defaultMaterialAndTexture.getTexCoordGeneration());
            appearance.setTextureAttributes(defaultMaterialAndTexture.getTextureAttributes());
            appearance.setTexture(null);
          }
        } else if (textureModified) {            
          if (windowPane) {
            restoreDefaultMaterialAndTexture(appearance, materialShininess);
          } else {
            appearance.setTexCoordGeneration(getTextureCoordinates(texture, pieceSize, modelBounds));
            appearance.setTextureAttributes(getTextureAttributes(texture, true));
            appearance.setMaterial(getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, materialShininess));
            TextureManager.getInstance().loadTexture(texture.getImage(),  
                waitTextureLoadingEnd, getTextureObserver(appearance));
          }
        } else if (materialModified) {
          String appearanceName = null;
          try {
            appearanceName = appearance.getName();
          } catch (NoSuchMethodError ex) {
          }
          boolean materialFound = false;
          if (appearanceName != null) {
            for (HomeMaterial material : materials) {
              if (material != null
                  && (material.getKey() != null
                          && material.getKey().equals(appearanceName)
                      || material.getKey() == null
                          && material.getName().equals(appearanceName))) {
                if (material.getShininess() != null) {
                  materialShininess = material.getShininess();
                }
                color = material.getColor();                
                if (color != null 
                    && (color.intValue() & 0xFF000000) != 0) {
                  appearance.setMaterial(getMaterial(color, color, materialShininess));
                  appearance.setTexture(null);
                  appearance.setTransparencyAttributes(defaultMaterialAndTexture.getTransparencyAttributes());
                  appearance.setPolygonAttributes(defaultMaterialAndTexture.getPolygonAttributes());
                } else if (color == null && material.getTexture() != null) {
                  HomeTexture materialTexture = material.getTexture();
                  if (isTexturesCoordinatesDefined(shape)) {
                    restoreDefaultTextureCoordinatesGeneration(appearance);
                    appearance.setTextureAttributes(getTextureAttributes(materialTexture));
                  } else {
                    appearance.setTexCoordGeneration(getTextureCoordinates(material.getTexture(), pieceSize, modelBounds));
                    appearance.setTextureAttributes(getTextureAttributes(materialTexture, true));
                  }
                  appearance.setMaterial(getMaterial(DEFAULT_COLOR, DEFAULT_AMBIENT_COLOR, materialShininess));
                  TextureManager.getInstance().loadTexture(materialTexture.getImage(),  
                      waitTextureLoadingEnd, getTextureObserver(appearance));
                } else {
                  restoreDefaultMaterialAndTexture(appearance, material.getShininess());
                }
                materialFound = true;
                break;
              }
            }
          }
          if (!materialFound) {
            restoreDefaultMaterialAndTexture(appearance, null);
          }
        } else {
          restoreDefaultMaterialAndTexture(appearance, shininess);
        }
        modifiedAppearances.add(appearance);
      }
    }
  }

  private TextureObserver getTextureObserver(final Appearance appearance) {
    return new TextureManager.TextureObserver() {
        public void textureUpdated(Texture texture) {
          if (TextureManager.getInstance().isTextureTransparent(texture)) {
            appearance.setTransparencyAttributes(DEFAULT_TEXTURED_SHAPE_TRANSPARENCY_ATTRIBUTES);
            DefaultMaterialAndTexture defaultMaterialAndTexture = (DefaultMaterialAndTexture)appearance.getUserData();
            if (defaultMaterialAndTexture != null
                && defaultMaterialAndTexture.getPolygonAttributes() != null
                && defaultMaterialAndTexture.getPolygonAttributes().getBackFaceNormalFlip()) {
              appearance.setPolygonAttributes(NORMAL_FLIPPED_TEXTURED_SHAPE_POLYGON_ATTRIBUTES);
            } else {
              appearance.setPolygonAttributes(DEFAULT_TEXTURED_SHAPE_POLYGON_ATTRIBUTES);
            }
          } else {
            DefaultMaterialAndTexture defaultMaterialAndTexture = (DefaultMaterialAndTexture)appearance.getUserData();
            if (defaultMaterialAndTexture != null) {
              appearance.setTransparencyAttributes(defaultMaterialAndTexture.getTransparencyAttributes());
              appearance.setPolygonAttributes(defaultMaterialAndTexture.getPolygonAttributes());
            }
          }
          Texture homeTexture = getHomeTextureClone(texture, home);
          if (appearance.getTexture() != homeTexture) {
            appearance.setTexture(homeTexture);
          }
        }
      };
  }

  private TexCoordGeneration getTextureCoordinates(HomeTexture texture, Vector3f pieceSize, 
                                                   BoundingBox modelBounds) {
    Point3d lower = new Point3d();
    modelBounds.getLower(lower);
    Point3d upper = new Point3d();
    modelBounds.getUpper(upper);
    float minimumSize = ModelManager.getInstance().getMinimumSize();
    float sx = pieceSize.x / (float)Math.max(upper.x - lower.x, minimumSize);
    float sw = texture.isLeftToRightOriented()  
        ? (float)-lower.x * sx  
        : 0;
    float ty = pieceSize.y / (float)Math.max(upper.y - lower.y, minimumSize);
    float tz = pieceSize.z / (float)Math.max(upper.z - lower.z, minimumSize);
    float tw = texture.isLeftToRightOriented()  
        ? (float)(-lower.y * ty + upper.z * tz)
        : 0;
    return new TexCoordGeneration(TexCoordGeneration.OBJECT_LINEAR,
        TexCoordGeneration.TEXTURE_COORDINATE_2, new Vector4f(sx, 0, 0, sw), new Vector4f(0, ty, -tz, tw));
  }


  private boolean isTexturesCoordinatesDefined(Shape3D shape) {
    for (int i = 0, n = shape.numGeometries(); i < n; i++) {
      Geometry geometry = shape.getGeometry(i);
      if (geometry instanceof GeometryArray 
          && (((GeometryArray)geometry).getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) == 0) {
        return false;
      }
    }
    return true;
  }

  private void restoreDefaultMaterialAndTexture(Appearance appearance,
                                                Float shininess) {
    DefaultMaterialAndTexture defaultMaterialAndTexture = (DefaultMaterialAndTexture)appearance.getUserData();
    if (defaultMaterialAndTexture != null) {
      Material defaultMaterial = defaultMaterialAndTexture.getMaterial();
      if (defaultMaterial != null && shininess != null) {
        defaultMaterial = (Material)defaultMaterial.cloneNodeComponent(true);
        defaultMaterial.setSpecularColor(new Color3f(shininess, shininess, shininess));
        defaultMaterial.setShininess(shininess * 128);
      }
      appearance.setMaterial(defaultMaterial);
      appearance.setTransparencyAttributes(defaultMaterialAndTexture.getTransparencyAttributes());
      appearance.setPolygonAttributes(defaultMaterialAndTexture.getPolygonAttributes());
      appearance.setTexCoordGeneration(defaultMaterialAndTexture.getTexCoordGeneration());
      appearance.setTexture(getHomeTextureClone(defaultMaterialAndTexture.getTexture(), home));
      appearance.setTextureAttributes(defaultMaterialAndTexture.getTextureAttributes());
    }
  }

  private void restoreDefaultTextureCoordinatesGeneration(Appearance appearance) {
    DefaultMaterialAndTexture defaultMaterialAndTexture = (DefaultMaterialAndTexture)appearance.getUserData();
    if (defaultMaterialAndTexture != null) {
      appearance.setTexCoordGeneration(defaultMaterialAndTexture.getTexCoordGeneration());
    }
  }
  

  private void setVisible(Node node, boolean visible, HomeMaterial [] materials) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        setVisible((Node)enumeration.nextElement(), visible, materials);
      }
    } else if (node instanceof Link) {
      setVisible(((Link)node).getSharedGroup(), visible, materials);
    } else if (node instanceof Shape3D) {
      final Shape3D shape = (Shape3D)node;
      Appearance appearance = shape.getAppearance();
      if (appearance == null) {
        appearance = createAppearanceWithChangeCapabilities();
        ((Shape3D)node).setAppearance(appearance);
      }
      RenderingAttributes renderingAttributes = appearance.getRenderingAttributes();
      if (renderingAttributes == null) {
        renderingAttributes = new RenderingAttributes();
        renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
        appearance.setRenderingAttributes(renderingAttributes);
      }
      
      String shapeName = (String)shape.getUserData();
      if (visible 
          && shapeName != null
          && (getUserData() instanceof Light)
          && shapeName.startsWith(ModelManager.LIGHT_SHAPE_PREFIX)
          && this.home != null
          && !isSelected(this.home.getSelectedItems())) {
        visible = false;
      }
      
      if (visible
          && materials != null) {
        String appearanceName = null;
        try {
          appearanceName = appearance.getName();
        } catch (NoSuchMethodError ex) {
        }
        if (appearanceName != null) {
          for (HomeMaterial material : materials) {
            if (material != null 
                && material.getName().equals(appearanceName)) {
              Integer color = material.getColor();                
              visible = color == null
                  || (color.intValue() & 0xFF000000) != 0;
              break;
            }
          }
        }
      }  
      renderingAttributes.setVisible(visible);
    } 
  } 

  private boolean isSelected(List<? extends Selectable> selectedItems) {
    Object piece = getUserData();
    for (Selectable item : selectedItems) {
      if (item == piece
          || (item instanceof HomeFurnitureGroup
              && isSelected(((HomeFurnitureGroup)item).getFurniture()))) {
        return true;
      }
    }
    return false;
  }

  private void setCullFace(Node node, boolean mirrored, boolean backFaceShown) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        setCullFace((Node)enumeration.nextElement(), mirrored, backFaceShown);
      }
    } else if (node instanceof Link) {
      setCullFace(((Link)node).getSharedGroup(), mirrored, backFaceShown);
    } else if (node instanceof Shape3D) {
      Appearance appearance = ((Shape3D)node).getAppearance();
      if (appearance == null) {
        appearance = createAppearanceWithChangeCapabilities();
        ((Shape3D)node).setAppearance(appearance);
      }
      PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
      if (polygonAttributes == null) {
        polygonAttributes = createPolygonAttributesWithChangeCapabilities();
        appearance.setPolygonAttributes(polygonAttributes);
      }

      try {
        int cullFace = polygonAttributes.getCullFace();
        if (cullFace != PolygonAttributes.CULL_NONE) {
          Integer defaultCullFace = (Integer)polygonAttributes.getUserData();
          if (defaultCullFace == null) {
            polygonAttributes.setUserData(defaultCullFace = cullFace);
          }
          polygonAttributes.setCullFace((mirrored ^ backFaceShown ^ defaultCullFace == PolygonAttributes.CULL_FRONT)
              ? PolygonAttributes.CULL_FRONT 
              : PolygonAttributes.CULL_BACK);
        }
      } catch (CapabilityNotSetException ex) {
        ex.printStackTrace();
      }
    }
  }
  
  private void setBackFaceNormalFlip(Node node, boolean backFaceNormalFlip) {
    if (node instanceof Group) {
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        setBackFaceNormalFlip((Node)enumeration.nextElement(), backFaceNormalFlip);
      }
    } else if (node instanceof Link) {
      setBackFaceNormalFlip(((Link)node).getSharedGroup(), backFaceNormalFlip);
    } else if (node instanceof Shape3D) {
      Appearance appearance = ((Shape3D)node).getAppearance();
      if (appearance == null) {
        appearance = createAppearanceWithChangeCapabilities();
        ((Shape3D)node).setAppearance(appearance);
      }
      PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
      if (polygonAttributes == null) {
        polygonAttributes = createPolygonAttributesWithChangeCapabilities();
        appearance.setPolygonAttributes(polygonAttributes);
      }
      
      polygonAttributes.setBackFaceNormalFlip(
          backFaceNormalFlip ^ polygonAttributes.getCullFace() == PolygonAttributes.CULL_FRONT);
    }
  }

  private PolygonAttributes createPolygonAttributesWithChangeCapabilities() {
    PolygonAttributes polygonAttributes = new PolygonAttributes();
    polygonAttributes.setCapability(PolygonAttributes.ALLOW_CULL_FACE_READ);
    polygonAttributes.setCapability(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
    polygonAttributes.setCapability(PolygonAttributes.ALLOW_NORMAL_FLIP_READ);
    polygonAttributes.setCapability(PolygonAttributes.ALLOW_NORMAL_FLIP_WRITE);
    return polygonAttributes;
  }

  private Appearance createAppearanceWithChangeCapabilities() {
    Appearance appearance = new Appearance();
    setAppearanceCapabilities(appearance);
    return appearance;
  }

  private void setAppearanceCapabilities(Appearance appearance) {
    appearance.setCapability(Appearance.ALLOW_MATERIAL_READ);
    appearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
    Material material = appearance.getMaterial();
    if (material != null) {
      material.setCapability(Material.ALLOW_COMPONENT_READ);
    }
    appearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
    appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE);
    appearance.setCapability(Appearance.ALLOW_TEXGEN_READ);
    appearance.setCapability(Appearance.ALLOW_TEXGEN_WRITE);
    appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
    appearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
    appearance.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_TEXTURE_ATTRIBUTES_WRITE);
    appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
    appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
    PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
    if (polygonAttributes != null) {
      polygonAttributes.setCapability(PolygonAttributes.ALLOW_CULL_FACE_READ);
      polygonAttributes.setCapability(PolygonAttributes.ALLOW_CULL_FACE_WRITE);
      polygonAttributes.setCapability(PolygonAttributes.ALLOW_NORMAL_FLIP_READ);
      polygonAttributes.setCapability(PolygonAttributes.ALLOW_NORMAL_FLIP_WRITE);
    }
  }
  
  private void setGeometryCapabilities(Geometry geometry) {
    if (!geometry.isLive()
        && geometry instanceof GeometryArray) {
      geometry.setCapability(GeometryArray.ALLOW_FORMAT_READ);
      geometry.setCapability(GeometryArray.ALLOW_COUNT_READ);
      geometry.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
      geometry.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
      geometry.setCapability(GeometryArray.ALLOW_NORMAL_READ);
      geometry.setCapability(GeometryArray.ALLOW_TEXCOORD_READ);
    }
  }

  private static class DefaultMaterialAndTexture {
    private final Material               material;
    private final TransparencyAttributes transparencyAttributes;
    private final PolygonAttributes      polygonAttributes;
    private final TexCoordGeneration     texCoordGeneration;
    private final Texture                texture;
    private final TextureAttributes      textureAttributes;

    public DefaultMaterialAndTexture(Appearance appearance) {
      this.material = appearance.getMaterial();
      this.transparencyAttributes = appearance.getTransparencyAttributes();
      this.polygonAttributes = appearance.getPolygonAttributes();
      this.texCoordGeneration = appearance.getTexCoordGeneration();
      this.texture = appearance.getTexture();
      this.textureAttributes = appearance.getTextureAttributes();
    }
    
    public Material getMaterial() {
      return this.material;
    }

    public TransparencyAttributes getTransparencyAttributes() {
      return this.transparencyAttributes;
    }
    
    public PolygonAttributes getPolygonAttributes() {
      return this.polygonAttributes;
    }
    
    public TexCoordGeneration getTexCoordGeneration() {
      return this.texCoordGeneration;
    }
    
    public Texture getTexture() {
      return this.texture;
    }
    
    public TextureAttributes getTextureAttributes() {
      return this.textureAttributes;
    }
  }
}