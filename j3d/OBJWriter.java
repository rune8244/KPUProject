package com.eteks.homeview3d.j3d;

import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryStripArray;
import javax.media.j3d.Group;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedGeometryStripArray;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.IndexedLineStripArray;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.IndexedTriangleFanArray;
import javax.media.j3d.IndexedTriangleStripArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Link;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
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
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class OBJWriter extends FilterWriter {
  private final NumberFormat defaultNumberFormat = 
      new DecimalFormat("0.#######", new DecimalFormatSymbols(Locale.US));
  private final NumberFormat numberFormat;  
  private final String  header;
  
  private boolean firstNode = true;
  private String  mtlFileName;

  private int shapeIndex = 1;
  private Map<Point3f, Integer>    vertexIndices = new HashMap<Point3f, Integer>();
  private Map<Vector3f, Integer>   normalIndices = new HashMap<Vector3f, Integer>();
  private Map<TexCoord2f, Integer> textureCoordinatesIndices = new HashMap<TexCoord2f, Integer>();  
  private Map<ComparableAppearance, String> appearances = 
      new LinkedHashMap<ComparableAppearance, String>();
  private Map<Texture, File> textures = new HashMap<Texture, File>();
  private List<URL>          copiedTextures = new ArrayList<URL>();
  

  public OBJWriter(File objFile) throws FileNotFoundException, IOException {
    this(objFile, null, -1);
  }
  

  public OBJWriter(File objFile, String header, 
                   int maximumFractionDigits) throws FileNotFoundException, IOException {
    this(objFile.toString(), header, maximumFractionDigits);
  }
  

  public OBJWriter(String objFileName) throws FileNotFoundException, IOException {
    this(objFileName, null, -1);
  }
  


  public OBJWriter(String objFileName, String header, 
                   int maximumFractionDigits) throws FileNotFoundException, IOException {
    this(new FileOutputStream(objFileName), header, maximumFractionDigits);
    if (objFileName.toLowerCase().endsWith(".obj")) {
      this.mtlFileName = objFileName.substring(0, objFileName.length() - 4) + ".mtl";
    } else {
      this.mtlFileName = objFileName + ".mtl";
    }
    // MTL파일에서 공간 삭제
    this.mtlFileName = new File(new File(this.mtlFileName).getParent(), 
        new File(this.mtlFileName).getName().replace(' ', '_')).toString();
    String name = new File(this.mtlFileName).getName();
    for (int i = 0; i < name.length(); i++) {
      if (name.charAt(i) >= 128) {
        this.mtlFileName = new File(new File(this.mtlFileName).getParent(),
            "materials.mtl").toString();
        break;
      }
    }
  }

  public OBJWriter(OutputStream out) throws IOException {
    this(out, null, -1);
  }


  public OBJWriter(OutputStream out, String header, 
                   int maximumFractionDigits) throws IOException {
    this(new OutputStreamWriter(new BufferedOutputStream(out), "US-ASCII"), header, maximumFractionDigits);
  }


  public OBJWriter(Writer out) throws IOException {
    this(out, null, -1);
  }
  

  public OBJWriter(Writer out, String header, 
                   int maximumFractionDigits) throws IOException {
    super(out);
    if (maximumFractionDigits >= 0) {
      this.numberFormat = NumberFormat.getNumberInstance(Locale.US);
      this.numberFormat.setMinimumFractionDigits(0);
      this.numberFormat.setMaximumFractionDigits(maximumFractionDigits);
    } else {
      this.numberFormat = null;
    }
    this.header = header;
    writeHeader(this.out);
  }
  
  /**
   * writer코드에서 헤더 쓰기
   */
  private void writeHeader(Writer writer) throws IOException {
    if (this.header != null) {
      if (!this.header.startsWith("#")) {
        writer.write("# ");
      }
      writer.write(this.header.replace("\n", "\n# "));
      writer.write("\n");
    }
  }


  @Override
  public void write(int c) throws IOException {
    this.out.write("# ");
    this.out.write(c);
    this.out.write("\n");
  }


  @Override
  public void write(char cbuf[], int off, int len) throws IOException {
    this.out.write("# ");
    this.out.write(cbuf, off, len);
    this.out.write("\n");
  }

  @Override
  public void write(String str, int off, int len) throws IOException {
    this.out.write("# ");
    this.out.write(str, off, len);
    this.out.write("\n");
  }
  

  @Override
  public void write(String str) throws IOException {
    this.out.write("# ");
    this.out.write(str, 0, str.length());
    this.out.write("\n");
  }


  private void checkCurrentThreadIsntInterrupted() throws InterruptedIOException {
    if (Thread.interrupted()) {
      this.mtlFileName = null;
      throw new InterruptedIOException("Current thread interrupted");
    }
  }
  

  public void writeNode(Node node) throws IOException, InterruptedIOException {
    writeNode(node, null);
  }
  

  public void writeNode(Node node, String nodeName) throws IOException, InterruptedIOException {
    if (this.firstNode) {
      if (this.mtlFileName != null) {
        this.out.write("mtllib " + new File(this.mtlFileName).getName() + "\n");
      }
      this.firstNode = false;
    }
    
    writeNode(node, nodeName, new Transform3D());
  }

 
  private void writeNode(Node node, String nodeName, Transform3D parentTransformations) throws IOException {
    if (node instanceof Group) {
      if (node instanceof TransformGroup) {
        parentTransformations = new Transform3D(parentTransformations);
        Transform3D transform = new Transform3D();
        ((TransformGroup)node).getTransform(transform);
        parentTransformations.mul(transform);
      }
      // 모든 자식들 쓰기
      Enumeration<?> enumeration = ((Group)node).getAllChildren(); 
      while (enumeration.hasMoreElements()) {
        writeNode((Node)enumeration.nextElement(), nodeName, parentTransformations);
      }
    } else if (node instanceof Link) {
      writeNode(((Link)node).getSharedGroup(), nodeName, parentTransformations);
    } else if (node instanceof Shape3D) {
      Shape3D shape = (Shape3D)node;
      Appearance appearance = shape.getAppearance();
      RenderingAttributes renderingAttributes = appearance != null 
          ? appearance.getRenderingAttributes() : null;
      if (shape.numGeometries() >= 1 
          && (renderingAttributes == null
              || renderingAttributes.getVisible())) {
        String objectName = "";
        if (accept(nodeName)) {
          objectName = nodeName + "_";
        }
          
        String shapeName = null;
        if (shape.getUserData() instanceof String) {
          shapeName = (String)shape.getUserData(); 
        }
        if (accept(shapeName)) {
          objectName += shapeName + "_";
        } 
        
        objectName += String.valueOf(this.shapeIndex++);
        
        // OBJ포맷에서 새로운 오브젝트 시작
        this.out.write("g " + objectName + "\n");
        
        TexCoordGeneration texCoordGeneration = null;
        Transform3D textureTransform = new Transform3D();
        if (this.mtlFileName != null) {
          if (appearance != null) {
            texCoordGeneration = appearance.getTexCoordGeneration();
            TextureAttributes textureAttributes = appearance.getTextureAttributes();
            if (textureAttributes != null) {
              textureAttributes.getTextureTransform(textureTransform);
            }
            ComparableAppearance comparableAppearance = new ComparableAppearance(appearance);
            String appearanceName = this.appearances.get(comparableAppearance);
            if (appearanceName == null) {
              // 외관 저장
              try {
                appearanceName = appearance.getName();
              } catch (NoSuchMethodError ex) {
              }
              if (appearanceName == null || !accept(appearanceName)) {
                appearanceName = objectName;
              } else {
                Collection<String> appearanceNames = this.appearances.values();
                String baseName = appearanceName + "_" + objectName;
                for (int i = 0; appearanceNames.contains(appearanceName); i++) {
                  if (i == 0) {
                    appearanceName = baseName;
                  } else {
                    appearanceName = baseName + "_" + i;
                  }
                }
              }
              this.appearances.put(comparableAppearance, appearanceName);
              
              Texture texture = appearance.getTexture();
              if (texture != null) {
                File textureFile = this.textures.get(texture);
                if (textureFile == null) {
                  String fileExtension = "png";
                  URL textureUrl = (URL)texture.getUserData();
                  if (textureUrl instanceof URL) {
                    InputStream in = null;
                    try {
                      in = openStream(textureUrl);
                      ImageInputStream imageIn = ImageIO.createImageInputStream(in);
                      Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageIn);
                      if (imageReaders.hasNext()) {
                        ImageReader reader = (ImageReader)imageReaders.next();
                        fileExtension = reader.getFormatName().toLowerCase();
                        this.copiedTextures.add(textureUrl);
                      }
                    } catch (IOException ex) {
                      if (in != null) {
                        in.close();
                      }
                    }
                  }
                  // 텍스쳐 저장
                  textureFile = new File(this.mtlFileName.substring(0, this.mtlFileName.length() - 4) 
                      + "_" + appearanceName + "." + fileExtension);
                  this.textures.put(texture, textureFile);
                }
              }
            } 
            this.out.write("usemtl " + appearanceName + "\n");
          }
        }
        
        int cullFace = PolygonAttributes.CULL_BACK;
        boolean backFaceNormalFlip = false;
        if (appearance != null) {
          PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
          if (polygonAttributes != null) {
            cullFace = polygonAttributes.getCullFace();
            backFaceNormalFlip = polygonAttributes.getBackFaceNormalFlip();
          }
        }

        for (int i = 0, n = shape.numGeometries(); i < n; i++) {
          writeNodeGeometry(shape.getGeometry(i), parentTransformations, texCoordGeneration, 
              textureTransform, cullFace, backFaceNormalFlip);
        }
      }
    }    
  }
  
  private InputStream openStream(URL url) throws IOException {
    URLConnection connection = url.openConnection();
    if (System.getProperty("os.name").startsWith("Windows")
        && (connection instanceof JarURLConnection)) {
      JarURLConnection urlConnection = (JarURLConnection)connection;
      URL jarFileUrl = urlConnection.getJarFileURL();
      if (jarFileUrl.getProtocol().equalsIgnoreCase("file")) {
        try {
          if (new File(jarFileUrl.toURI()).canWrite()) {

            connection.setUseCaches(false);
          }
        } catch (URISyntaxException ex) {
          IOException ex2 = new IOException();
          ex2.initCause(ex);
          throw ex2;
        }
      }
    }
    return connection.getInputStream();
  }
  

  private boolean accept(String name) {
    if (name == null) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char car = name.charAt(i);
      if (!(car >= 'a' && car <= 'z'
            || car >= 'A' && car <= 'Z'
            || car >= '0' && car <= '9'
            || car == '_')) {
        return false;
      }
    }
    return true;
  }


  private void writeNodeGeometry(Geometry geometry, 
                                 Transform3D parentTransformations, 
                                 TexCoordGeneration texCoordGeneration, 
                                 Transform3D textureTransform, 
                                 int cullFace, 
                                 boolean backFaceNormalFlip) throws IOException {
    if (geometry instanceof GeometryArray) {
      GeometryArray geometryArray = (GeometryArray)geometry;      
      
      int [] vertexIndexSubstitutes = new int [geometryArray.getVertexCount()];
      
      boolean normalsDefined = (geometryArray.getVertexFormat() & GeometryArray.NORMALS) != 0;
      StringBuilder normalsBuffer;
      List<Vector3f> addedNormals;
      if (normalsDefined) {
        normalsBuffer = new StringBuilder(geometryArray.getVertexCount() * 3 * 10);
        addedNormals = new ArrayList<Vector3f>();
      } else {
        normalsBuffer = null;
        addedNormals = null;
      }
      int [] normalIndexSubstitutes = new int [geometryArray.getVertexCount()];
      int [] oppositeSideNormalIndexSubstitutes;
      if (cullFace == PolygonAttributes.CULL_NONE) {
        oppositeSideNormalIndexSubstitutes = new int [geometryArray.getVertexCount()];
      } else {
        oppositeSideNormalIndexSubstitutes = null;
      }

      boolean textureCoordinatesDefined = (geometryArray.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) != 0;
      int [] textureCoordinatesIndexSubstitutes = new int [geometryArray.getVertexCount()];

      boolean textureCoordinatesGenerated = false;
      Vector4f planeS = null;
      Vector4f planeT = null;
      if (texCoordGeneration != null) {
        textureCoordinatesGenerated = texCoordGeneration.getGenMode() == TexCoordGeneration.OBJECT_LINEAR
            && texCoordGeneration.getEnable()
            && !(geometryArray instanceof IndexedLineArray)
            && !(geometryArray instanceof IndexedLineStripArray)
            && !(geometryArray instanceof LineArray)
            && !(geometryArray instanceof LineStripArray);
        if (textureCoordinatesGenerated) {
          planeS = new Vector4f();
          planeT = new Vector4f();
          texCoordGeneration.getPlaneS(planeS);
          texCoordGeneration.getPlaneT(planeT);
        }
      }
      
      checkCurrentThreadIsntInterrupted();

      if ((geometryArray.getVertexFormat() & GeometryArray.BY_REFERENCE) != 0) {
        if ((geometryArray.getVertexFormat() & GeometryArray.INTERLEAVED) != 0) {
          float [] vertexData = geometryArray.getInterleavedVertices();
          int vertexSize = vertexData.length / geometryArray.getVertexCount();
          for (int index = 0, i = vertexSize - 3, n = geometryArray.getVertexCount(); 
               index < n; index++, i += vertexSize) {
            Point3f vertex = new Point3f(vertexData [i], vertexData [i + 1], vertexData [i + 2]);
            writeVertex(parentTransformations, vertex, index, vertexIndexSubstitutes);
          }
          if (texCoordGeneration != null) {
            if (textureCoordinatesGenerated) {
              for (int index = 0, i = vertexSize - 3, n = geometryArray.getVertexCount(); 
                    index < n; index++, i += vertexSize) {
                TexCoord2f textureCoordinates = generateTextureCoordinates(
                    vertexData [i], vertexData [i + 1], vertexData [i + 2], planeS, planeT);
                writeTextureCoordinates(textureCoordinates, textureTransform, index, textureCoordinatesIndexSubstitutes);
              }
            }
          } else if (textureCoordinatesDefined) {
            for (int index = 0, i = 0, n = geometryArray.getVertexCount(); 
                  index < n; index++, i += vertexSize) {
              TexCoord2f textureCoordinates = new TexCoord2f(vertexData [i], vertexData [i + 1]);
              writeTextureCoordinates(textureCoordinates, textureTransform, index, textureCoordinatesIndexSubstitutes);
            }
          }
          if (normalsDefined) {
            for (int index = 0, i = vertexSize - 6, n = geometryArray.getVertexCount(); 
                 normalsDefined && index < n; index++, i += vertexSize) {
              Vector3f normal = new Vector3f(vertexData [i], vertexData [i + 1], vertexData [i + 2]);
              normalsDefined = writeNormal(normalsBuffer, parentTransformations, normal, index, normalIndexSubstitutes, 
                  oppositeSideNormalIndexSubstitutes, addedNormals, cullFace, backFaceNormalFlip);
            }
          }
        } else {
          float [] vertexCoordinates = geometryArray.getCoordRefFloat();
          for (int index = 0, i = 0, n = geometryArray.getVertexCount(); index < n; index++, i += 3) {
            Point3f vertex = new Point3f(vertexCoordinates [i], vertexCoordinates [i + 1], vertexCoordinates [i + 2]);
            writeVertex(parentTransformations, vertex, index,
                vertexIndexSubstitutes);
          }
          if (texCoordGeneration != null) {
            if (textureCoordinatesGenerated) {
              for (int index = 0, i = 0, n = geometryArray.getVertexCount(); index < n; index++, i += 3) {
                TexCoord2f textureCoordinates = generateTextureCoordinates(
                    vertexCoordinates [i], vertexCoordinates [i + 1], vertexCoordinates [i + 2], planeS, planeT);
                writeTextureCoordinates(textureCoordinates, textureTransform, index, textureCoordinatesIndexSubstitutes);
              }
            }
          } else if (textureCoordinatesDefined) {
            float [] textureCoordinatesArray = geometryArray.getTexCoordRefFloat(0);
            for (int index = 0, i = 0, n = geometryArray.getVertexCount(); index < n; index++, i += 2) {
              TexCoord2f textureCoordinates = new TexCoord2f(textureCoordinatesArray [i], textureCoordinatesArray [i + 1]);
              writeTextureCoordinates(textureCoordinates, textureTransform, index, textureCoordinatesIndexSubstitutes);
            }
          }
          if (normalsDefined) {
            float [] normalCoordinates = geometryArray.getNormalRefFloat();
            for (int index = 0, i = 0, n = geometryArray.getVertexCount(); normalsDefined && index < n; index++, i += 3) {
              Vector3f normal = new Vector3f(normalCoordinates [i], normalCoordinates [i + 1], normalCoordinates [i + 2]);
              normalsDefined = writeNormal(normalsBuffer, parentTransformations, normal, index, normalIndexSubstitutes, 
                  oppositeSideNormalIndexSubstitutes, addedNormals, cullFace, backFaceNormalFlip);
            }
          }
        }
      } else {
        for (int index = 0, n = geometryArray.getVertexCount(); index < n; index++) {
          Point3f vertex = new Point3f();
          geometryArray.getCoordinate(index, vertex);
          writeVertex(parentTransformations, vertex, index,
              vertexIndexSubstitutes);
        }
        if (texCoordGeneration != null) {
          if (textureCoordinatesGenerated) {
            for (int index = 0, n = geometryArray.getVertexCount(); index < n; index++) {
              Point3f vertex = new Point3f();
              geometryArray.getCoordinate(index, vertex);
              TexCoord2f textureCoordinates = generateTextureCoordinates(
                  vertex.x, vertex.y, vertex.z, planeS, planeT);
              writeTextureCoordinates(textureCoordinates, textureTransform, index, textureCoordinatesIndexSubstitutes);
            }
          }
        } else if (textureCoordinatesDefined) {
          for (int index = 0, n = geometryArray.getVertexCount(); index < n; index++) {
            TexCoord2f textureCoordinates = new TexCoord2f();
            geometryArray.getTextureCoordinate(0, index, textureCoordinates);
            writeTextureCoordinates(textureCoordinates, textureTransform, index, textureCoordinatesIndexSubstitutes);
          }
        }
        if (normalsDefined) {
          for (int index = 0, n = geometryArray.getVertexCount(); normalsDefined && index < n; index++) {
            Vector3f normal = new Vector3f();
            geometryArray.getNormal(index, normal);
            normalsDefined = writeNormal(normalsBuffer, parentTransformations, normal, index, normalIndexSubstitutes, 
                oppositeSideNormalIndexSubstitutes, addedNormals, cullFace, backFaceNormalFlip);
          }
        }
      }

      if (normalsDefined) {
        out.write(normalsBuffer.toString());
      } else if (addedNormals != null) {
        for (Vector3f normal : addedNormals) {
          this.normalIndices.remove(normal);
        }
      }

      checkCurrentThreadIsntInterrupted();
      
      if (geometryArray instanceof IndexedGeometryArray) {
        if (geometryArray instanceof IndexedLineArray) {
          IndexedLineArray lineArray = (IndexedLineArray)geometryArray;
          for (int i = 0, n = lineArray.getIndexCount(); i < n; i += 2) {
            writeIndexedLine(lineArray, i, i + 1, vertexIndexSubstitutes, textureCoordinatesIndexSubstitutes);
          }
        } else if (geometryArray instanceof IndexedTriangleArray) {
          IndexedTriangleArray triangleArray = (IndexedTriangleArray)geometryArray;
          for (int i = 0, n = triangleArray.getIndexCount(); i < n; i += 3) {
            writeIndexedTriangle(triangleArray, i, i + 1, i + 2, 
                vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,  
                normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
          }
        } else if (geometryArray instanceof IndexedQuadArray) {
          IndexedQuadArray quadArray = (IndexedQuadArray)geometryArray;
          for (int i = 0, n = quadArray.getIndexCount(); i < n; i += 4) {
            writeIndexedQuadrilateral(quadArray, i, i + 1, i + 2, i + 3, 
                vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,  
                normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
          }
        } else if (geometryArray instanceof IndexedGeometryStripArray) {
          IndexedGeometryStripArray geometryStripArray = (IndexedGeometryStripArray)geometryArray;
          int [] stripIndexCounts = new int [geometryStripArray.getNumStrips()];
          geometryStripArray.getStripIndexCounts(stripIndexCounts);
          int initialIndex = 0; 
          
          if (geometryStripArray instanceof IndexedLineStripArray) {
            for (int strip = 0; strip < stripIndexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripIndexCounts [strip] - 1; i < n; i++) {
                writeIndexedLine(geometryStripArray, i, i + 1, 
                    vertexIndexSubstitutes, textureCoordinatesIndexSubstitutes);
              }
              initialIndex += stripIndexCounts [strip];
            }
          } else if (geometryStripArray instanceof IndexedTriangleStripArray) {
            for (int strip = 0; strip < stripIndexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripIndexCounts [strip] - 2, j = 0; i < n; i++, j++) {
                if (j % 2 == 0) {
                  writeIndexedTriangle(geometryStripArray, i, i + 1, i + 2, 
                      vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,   
                      normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
                } else {             
                  writeIndexedTriangle(geometryStripArray, i, i + 2, i + 1, 
                      vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,  
                      normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
                }
              }
              initialIndex += stripIndexCounts [strip];
            }
          } else if (geometryStripArray instanceof IndexedTriangleFanArray) {
            for (int strip = 0; strip < stripIndexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripIndexCounts [strip] - 2; i < n; i++) {
                writeIndexedTriangle(geometryStripArray, initialIndex, i + 1, i + 2, 
                    vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,   
                    normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
              }
              initialIndex += stripIndexCounts [strip];
            }
          }
        } 
      } else {
        if (geometryArray instanceof LineArray) {
          LineArray lineArray = (LineArray)geometryArray;
          for (int i = 0, n = lineArray.getVertexCount(); i < n; i += 2) {
            writeLine(lineArray, i, i + 1, vertexIndexSubstitutes, textureCoordinatesIndexSubstitutes);
          }
        } else if (geometryArray instanceof TriangleArray) {
          TriangleArray triangleArray = (TriangleArray)geometryArray;
          for (int i = 0, n = triangleArray.getVertexCount(); i < n; i += 3) {
            writeTriangle(triangleArray, i, i + 1, i + 2, 
                vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,   
                normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
          }
        } else if (geometryArray instanceof QuadArray) {
          QuadArray quadArray = (QuadArray)geometryArray;
          for (int i = 0, n = quadArray.getVertexCount(); i < n; i += 4) {
            writeQuadrilateral(quadArray, i, i + 1, i + 2, i + 3, 
                vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,   
                normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
          }
        } else if (geometryArray instanceof GeometryStripArray) {
          GeometryStripArray geometryStripArray = (GeometryStripArray)geometryArray;
          int [] stripVertexCounts = new int [geometryStripArray.getNumStrips()];
          geometryStripArray.getStripVertexCounts(stripVertexCounts);
          int initialIndex = 0;
          
          if (geometryStripArray instanceof LineStripArray) {
            for (int strip = 0; strip < stripVertexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripVertexCounts [strip] - 1; i < n; i++) {
                writeLine(geometryStripArray, i, i + 1, vertexIndexSubstitutes, textureCoordinatesIndexSubstitutes);
              }
              initialIndex += stripVertexCounts [strip];
            }
          } else if (geometryStripArray instanceof TriangleStripArray) {
            for (int strip = 0; strip < stripVertexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripVertexCounts [strip] - 2, j = 0; i < n; i++, j++) {
                if (j % 2 == 0) {
                  writeTriangle(geometryStripArray, i, i + 1, i + 2, 
                      vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,  
                      normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
                } else {                
                  writeTriangle(geometryStripArray, i, i + 2, i + 1, 
                      vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,  
                      normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
                }
              }
              initialIndex += stripVertexCounts [strip];
            }
          } else if (geometryStripArray instanceof TriangleFanArray) {
            for (int strip = 0; strip < stripVertexCounts.length; strip++) {
              for (int i = initialIndex, n = initialIndex + stripVertexCounts [strip] - 2; i < n; i++) {
                writeTriangle(geometryStripArray, initialIndex, i + 1, i + 2, 
                    vertexIndexSubstitutes, normalIndexSubstitutes, oppositeSideNormalIndexSubstitutes,  
                    normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, cullFace);
              }
              initialIndex += stripVertexCounts [strip];
            }
          }
        }
      }
    } 
  }


  private TexCoord2f generateTextureCoordinates(float x, float y, float z, 
                                                Vector4f planeS, 
                                                Vector4f planeT) {
    return new TexCoord2f(x * planeS.x + y * planeS.y + z * planeS.z + planeS.w, 
        x * planeT.x + y * planeT.y + z * planeT.z + planeT.w);
  }


  private void writeVertex(Transform3D transformationToParent,
                           Point3f vertex, int index,
                           int [] vertexIndexSubstitutes) throws IOException {
    transformationToParent.transform(vertex);
    Integer vertexIndex = this.vertexIndices.get(vertex);
    if (vertexIndex == null) {
      vertexIndexSubstitutes [index] = this.vertexIndices.size() + 1;
      this.vertexIndices.put(vertex, vertexIndexSubstitutes [index]);

      this.out.write("v " + format(vertex.x) 
          + " " + format(vertex.y) 
          + " " + format(vertex.z) + "\n");
    } else {
      vertexIndexSubstitutes [index] = vertexIndex;
    }
  }
  

  private String format(float number) {
    if (this.numberFormat != null) {
      return this.numberFormat.format(number);
    } else {
      String numberString = String.valueOf((float)number);
      if (numberString.indexOf('E') != -1) {
        return this.defaultNumberFormat.format(number);
      } else {
        return numberString;
      }
    }      
  }


  private boolean writeNormal(StringBuilder normalsBuffer, 
                              Transform3D transformationToParent, 
                              Vector3f normal, int index,
                              int [] normalIndexSubstitutes,
                              int [] oppositeSideNormalIndexSubstitutes, 
                              List<Vector3f> addedNormals, 
                              int cullFace, boolean backFaceNormalFlip) throws IOException {
    if (Float.isNaN(normal.x) || Float.isNaN(normal.y) || Float.isNaN(normal.z)) {
      return false;
    }
    if (backFaceNormalFlip) {
      normal.negate();
    }
    if (normal.x != 0 || normal.y != 0 || normal.z != 0) {
      transformationToParent.transform(normal);
      normal.normalize();
    }
    Integer normalIndex = this.normalIndices.get(normal);
    if (normalIndex == null) {
      normalIndexSubstitutes [index] = this.normalIndices.size() + 1;
      this.normalIndices.put(normal, normalIndexSubstitutes [index]);
      addedNormals.add(normal);

      normalsBuffer.append("vn " + format(normal.x) 
          + " " + format(normal.y) 
          + " " + format(normal.z) + "\n");
    } else {
      normalIndexSubstitutes [index] = normalIndex;
    }
    
    if (cullFace == PolygonAttributes.CULL_NONE) {
      Vector3f oppositeNormal = new Vector3f(); 
      oppositeNormal.negate(normal);
      return writeNormal(normalsBuffer, transformationToParent, oppositeNormal, index, oppositeSideNormalIndexSubstitutes, 
          null, addedNormals, PolygonAttributes.CULL_FRONT, false);
    } else {
      return true;
    }
  }


  private void writeTextureCoordinates(TexCoord2f textureCoordinates, Transform3D textureTransform,
                                       int index, int [] textureCoordinatesIndexSubstitutes) throws IOException {
    if (textureTransform.getBestType() != Transform3D.IDENTITY) {
      Point3f transformedCoordinates = new Point3f(textureCoordinates.x, textureCoordinates.y, 0);
      textureTransform.transform(transformedCoordinates);
      textureCoordinates = new TexCoord2f(transformedCoordinates.x, transformedCoordinates.y);
    }
    Integer textureCoordinatesIndex = this.textureCoordinatesIndices.get(textureCoordinates);
    if (textureCoordinatesIndex == null) {
      textureCoordinatesIndexSubstitutes [index] = this.textureCoordinatesIndices.size() + 1;
      this.textureCoordinatesIndices.put(textureCoordinates, textureCoordinatesIndexSubstitutes [index]);
      this.out.write("vt " + format(textureCoordinates.x) 
          + " " + format(textureCoordinates.y) + " 0\n");
    } else {
      textureCoordinatesIndexSubstitutes [index] = textureCoordinatesIndex;
    }
  }


  private void writeIndexedLine(IndexedGeometryArray geometryArray, 
                                int vertexIndex1, int vertexIndex2, 
                                int [] vertexIndexSubstitutes, 
                                int [] textureCoordinatesIndexSubstitutes) throws IOException {
    if ((geometryArray.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) != 0) {
      this.out.write("l " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
          + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex1)]) 
          + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
          + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex2)]) + "\n");
    } else {
      this.out.write("l " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
          + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) + "\n");
    }
  }
  

  private void writeIndexedTriangle(IndexedGeometryArray geometryArray, 
                                    int vertexIndex1, int vertexIndex2, int vertexIndex3, 
                                    int [] vertexIndexSubstitutes, 
                                    int [] normalIndexSubstitutes, 
                                    int [] oppositeSideNormalIndexSubstitutes,                                     
                                    boolean normalsDefined,
                                    int [] textureCoordinatesIndexSubstitutes, 
                                    boolean textureCoordinatesGenerated, int cullFace) throws IOException {
    if (cullFace == PolygonAttributes.CULL_FRONT) {
      int tmp = vertexIndex1;
      vertexIndex1 = vertexIndex3;
      vertexIndex3 = tmp;
    }
    
    if (textureCoordinatesGenerated) {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex3)]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) + "\n");
      }
    } else if ((geometryArray.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) != 0) {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex1)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex2)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex3)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex3)]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex3)]) + "\n");
      }
    } else {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "//" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex1)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "//" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex2)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "//" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex3)]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) + "\n");
      }
    }

    if (cullFace == PolygonAttributes.CULL_NONE) {
      writeIndexedTriangle(geometryArray, vertexIndex1, vertexIndex2, vertexIndex3, 
          vertexIndexSubstitutes, oppositeSideNormalIndexSubstitutes, null,  
          normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, PolygonAttributes.CULL_FRONT);
    }
  }
  

  private void writeIndexedQuadrilateral(IndexedGeometryArray geometryArray, 
                                         int vertexIndex1, int vertexIndex2, int vertexIndex3, int vertexIndex4, 
                                         int [] vertexIndexSubstitutes, 
                                         int [] normalIndexSubstitutes, 
                                         int [] oppositeSideNormalIndexSubstitutes,                                      
                                         boolean normalsDefined,
                                         int [] textureCoordinatesIndexSubstitutes, 
                                         boolean textureCoordinatesGenerated, int cullFace) throws IOException {
    if (cullFace == PolygonAttributes.CULL_FRONT) {
      int tmp = vertexIndex2;
      vertexIndex2 = vertexIndex3;
      vertexIndex3 = tmp;
      tmp = vertexIndex1;
      vertexIndex1 = vertexIndex4;
      vertexIndex4 = tmp;
    }
    
    if (textureCoordinatesGenerated) {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex3)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex4)]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) + "\n");
      }
    } else if ((geometryArray.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) != 0) {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex1)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex2)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex3)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex3)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex4)]) 
            + "/" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex4)]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex1)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex2)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex3)]) 
            + " " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) 
            + "/" + (textureCoordinatesIndexSubstitutes [geometryArray.getTextureCoordinateIndex(0, vertexIndex4)]) + "\n");
      }
    } else {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + "//" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex1)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + "//" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex2)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + "//" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex3)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) 
            + "//" + (normalIndexSubstitutes [geometryArray.getNormalIndex(vertexIndex4)]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex1)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex2)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex3)]) 
            + " "  + (vertexIndexSubstitutes [geometryArray.getCoordinateIndex(vertexIndex4)]) + "\n");
      }
    }

    if (cullFace == PolygonAttributes.CULL_NONE) {      

      writeIndexedQuadrilateral(geometryArray, vertexIndex1, vertexIndex2, vertexIndex3, vertexIndex4, 
          vertexIndexSubstitutes, oppositeSideNormalIndexSubstitutes, null,  
          normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, PolygonAttributes.CULL_FRONT);
    }
  }
  

  private void writeLine(GeometryArray geometryArray, 
                         int vertexIndex1, int vertexIndex2, 
                         int [] vertexIndexSubstitutes,  
                         int [] textureCoordinatesIndexSubstitutes) throws IOException {
    if ((geometryArray.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) != 0) {
      this.out.write("l " + (vertexIndexSubstitutes [vertexIndex1]) 
          + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex1]) 
          + " " + (vertexIndexSubstitutes [vertexIndex2]) 
          + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex2]) + "\n");
    } else {
      this.out.write("l " + (vertexIndexSubstitutes [vertexIndex1]) 
          + " "  + (vertexIndexSubstitutes [vertexIndex2]) + "\n");
    }
  }
  

  private void writeTriangle(GeometryArray geometryArray, 
                             int vertexIndex1, int vertexIndex2, int vertexIndex3, 
                             int [] vertexIndexSubstitutes,  
                             int [] normalIndexSubstitutes, 
                             int [] oppositeSideNormalIndexSubstitutes,                                       
                             boolean normalsDefined,
                             int [] textureCoordinatesIndexSubstitutes, 
                             boolean textureCoordinatesGenerated, int cullFace) throws IOException {
    if (cullFace == PolygonAttributes.CULL_FRONT) {
      int tmp = vertexIndex1;
      vertexIndex1 = vertexIndex3;
      vertexIndex3 = tmp;
    }
    
    if (textureCoordinatesGenerated
        || (geometryArray.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) != 0) {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex1]) 
            + "/" + (normalIndexSubstitutes [vertexIndex1]) 
            + " " + (vertexIndexSubstitutes [vertexIndex2]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex2]) 
            + "/" + (normalIndexSubstitutes [vertexIndex2]) 
            + " " + (vertexIndexSubstitutes [vertexIndex3]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex3]) 
            + "/" + (normalIndexSubstitutes [vertexIndex3]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex1]) 
            + " " + (vertexIndexSubstitutes [vertexIndex2]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex2]) 
            + " " + (vertexIndexSubstitutes [vertexIndex3]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex3]) + "\n");
      }
    } else {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + "//" + (normalIndexSubstitutes [vertexIndex1]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex2]) 
            + "//" + (normalIndexSubstitutes [vertexIndex2]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex3]) 
            + "//" + (normalIndexSubstitutes [vertexIndex3]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex2]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex3]) + "\n");
      }
    }

    if (cullFace == PolygonAttributes.CULL_NONE) {
      writeTriangle(geometryArray, vertexIndex1, vertexIndex2, vertexIndex3, 
          vertexIndexSubstitutes, oppositeSideNormalIndexSubstitutes, null,  
          normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, PolygonAttributes.CULL_FRONT);
    }
  }
  

  private void writeQuadrilateral(GeometryArray geometryArray, 
                                  int vertexIndex1, int vertexIndex2, int vertexIndex3, int vertexIndex4, 
                                  int [] vertexIndexSubstitutes, 
                                  int [] normalIndexSubstitutes, 
                                  int [] oppositeSideNormalIndexSubstitutes,                                       
                                  boolean normalsDefined,
                                  int [] textureCoordinatesIndexSubstitutes, 
                                  boolean textureCoordinatesGenerated, int cullFace) throws IOException {
    if (cullFace == PolygonAttributes.CULL_FRONT) {
      int tmp = vertexIndex2;
      vertexIndex2 = vertexIndex3;
      vertexIndex3 = tmp;
      tmp = vertexIndex1;
      vertexIndex1 = vertexIndex4;
      vertexIndex4 = tmp;
    }
    
    if (textureCoordinatesGenerated
        || (geometryArray.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) != 0) {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex1]) 
            + "/" + (normalIndexSubstitutes [vertexIndex1]) 
            + " " + (vertexIndexSubstitutes [vertexIndex2]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex2]) 
            + "/" + (normalIndexSubstitutes [vertexIndex2]) 
            + " " + (vertexIndexSubstitutes [vertexIndex3]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex3]) 
            + "/" + (normalIndexSubstitutes [vertexIndex3]) 
            + " " + (vertexIndexSubstitutes [vertexIndex4]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex4]) 
            + "/" + (normalIndexSubstitutes [vertexIndex4]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex1]) 
            + " " + (vertexIndexSubstitutes [vertexIndex2]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex2]) 
            + " " + (vertexIndexSubstitutes [vertexIndex3]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex3]) 
            + " " + (vertexIndexSubstitutes [vertexIndex4]) 
            + "/" + (textureCoordinatesIndexSubstitutes [vertexIndex4]) + "\n");
      }
    } else {
      if (normalsDefined) {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + "//" + (normalIndexSubstitutes [vertexIndex1]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex2]) 
            + "//" + (normalIndexSubstitutes [vertexIndex2]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex3]) 
            + "//" + (normalIndexSubstitutes [vertexIndex3]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex4]) 
            + "//" + (normalIndexSubstitutes [vertexIndex4]) + "\n");
      } else {
        this.out.write("f " + (vertexIndexSubstitutes [vertexIndex1]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex2]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex3]) 
            + " "  + (vertexIndexSubstitutes [vertexIndex4]) + "\n");
      }
    }

    if (cullFace == PolygonAttributes.CULL_NONE) {      
      writeQuadrilateral(geometryArray, vertexIndex1, vertexIndex2, vertexIndex3, vertexIndex4, 
          vertexIndexSubstitutes, oppositeSideNormalIndexSubstitutes, null, 
          normalsDefined, textureCoordinatesIndexSubstitutes, textureCoordinatesGenerated, PolygonAttributes.CULL_FRONT);
    }
  }
  

  @Override
  public void close() throws IOException, InterruptedIOException {
    super.close();
    if (this.mtlFileName != null) {
      writeAppearancesToMTLFile();
    }
  }

  private void writeAppearancesToMTLFile() throws IOException {
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(
          new BufferedOutputStream(new FileOutputStream(this.mtlFileName)), "ISO-8859-1");
      writeHeader(writer);      
      for (Map.Entry<ComparableAppearance, String> appearanceEntry : this.appearances.entrySet()) {
        checkCurrentThreadIsntInterrupted();
        
        Appearance appearance = appearanceEntry.getKey().getAppearance();        
        String appearanceName = appearanceEntry.getValue();
        writer.write("\nnewmtl " + appearanceName + "\n");
        Material material = appearance.getMaterial();
        if (material != null) {
          if (material instanceof OBJMaterial
              && ((OBJMaterial)material).isIlluminationModelSet()) {
            writer.write("illum " + ((OBJMaterial)material).getIlluminationModel() + "\n");
          } else if (material.getShininess() > 1) {
            writer.write("illum 2\n");
          } else if (material.getLightingEnable()) {  
            writer.write("illum 1\n");
          } else {
            writer.write("illum 0\n");
          }
          Color3f color = new Color3f();
          material.getAmbientColor(color);          
          writer.write("Ka " + format(color.x) + " " + format(color.y) + " " + format(color.z) + "\n");
          material.getDiffuseColor(color);          
          writer.write("Kd " + format(color.x) + " " + format(color.y) + " " + format(color.z) + "\n");
          material.getSpecularColor(color);          
          writer.write("Ks " + format(color.x) + " " + format(color.y) + " " + format(color.z) + "\n");
          writer.write("Ns " + format(material.getShininess()) + "\n");
          if (material instanceof OBJMaterial) {
            OBJMaterial objMaterial = (OBJMaterial)material;
            if (objMaterial.isOpticalDensitySet()) {
              writer.write("Ni " + format(objMaterial.getOpticalDensity()) + "\n");
            }
            if (objMaterial.isSharpnessSet()) {
              writer.write("sharpness " + format(objMaterial.getSharpness()) + "\n");
            }
          }
        } else {
          ColoringAttributes coloringAttributes = appearance.getColoringAttributes();
          if (coloringAttributes != null) {
            writer.write("illum 0\n");
            Color3f color = new Color3f();
            coloringAttributes.getColor(color);          
            writer.write("Ka " + format(color.x) + " " + format(color.y) + " " + format(color.z) + "\n");
            writer.write("Kd " + format(color.x) + " " + format(color.y) + " " + format(color.z) + "\n");
            writer.write("Ks " + format(color.x) + " " + format(color.y) + " " + format(color.z) + "\n");
          }
        }
        TransparencyAttributes transparency = appearance.getTransparencyAttributes();
        if (transparency != null) {
          if (!(material instanceof OBJMaterial)) {
            writer.write("Ni 1\n");
          }
          writer.write("d " + format(1f - transparency.getTransparency()) + "\n");
        }
        Texture texture = appearance.getTexture();
        if (texture != null) {
          writer.write("map_Kd " + this.textures.get(texture).getName() + "\n");
        }
      }
      
      for (Map.Entry<Texture, File> textureEntry : this.textures.entrySet()) {
        Texture texture = textureEntry.getKey();
        Object textureUrl = texture.getUserData();
        if (this.copiedTextures.contains(textureUrl)) {
          InputStream in = null;
          OutputStream out = null;
          try {
            in = openStream((URL)textureUrl);
            out = new FileOutputStream(textureEntry.getValue());
            byte [] buffer = new byte [8192];
            int size; 
            while ((size = in.read(buffer)) != -1) {
              out.write(buffer, 0, size);
            }
          } finally {
            if (in != null) {
              in.close();
            }
            if (out != null) {
              out.close();
            }
          }
        } else {
          ImageComponent2D imageComponent = (ImageComponent2D)texture.getImage(0);
          RenderedImage image = imageComponent.getRenderedImage();
          ImageIO.write(image, "png", textureEntry.getValue());
        }
      }
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
  }

  public static void writeNodeInZIPFile(Node node, 
                                        File zipFile,    
                                        int compressionLevel,
                                        String entryName, 
                                        String header) throws IOException {
    writeNodeInZIPFile(node, null, zipFile, compressionLevel, entryName, header);
  }
  

  public static void writeNodeInZIPFile(Node node, 
                                        Map<String, Appearance> materialAppearances,
                                        File zipFile,    
                                        int compressionLevel,
                                        String entryName, 
                                        String header) throws IOException {
    File tempFolder = null;
    for (int i = 0; i < 10 && tempFolder == null; i++) { 
      tempFolder = File.createTempFile("obj", "tmp");
      tempFolder.delete();
      if (!tempFolder.mkdirs()) {
        tempFolder = null;
      }
    }
    if (tempFolder == null) {
      throw new IOException("Couldn't create a temporary folder");
    }
            
    ZipOutputStream zipOut = null;
    try {
      OBJWriter writer = new OBJWriter(new File(tempFolder, entryName), header, -1);
      writer.writeNode(node);
      writer.close();
      zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
      zipOut.setLevel(compressionLevel);
      for (File tempFile : tempFolder.listFiles()) {
        if (tempFile.isFile()) {
          InputStream tempIn = null;
          try {
            zipOut.putNextEntry(new ZipEntry(tempFile.getName()));
            tempIn = new FileInputStream(tempFile);
            byte [] buffer = new byte [8096];
            int size; 
            while ((size = tempIn.read(buffer)) != -1) {
              zipOut.write(buffer, 0, size);
            }
            zipOut.closeEntry();
          } finally {
            if (tempIn != null) {
              tempIn.close();
            }
          }          
        }
      }

      if (materialAppearances != null) {
        for (Map.Entry<ComparableAppearance, String> appearanceEntry : writer.appearances.entrySet()) {
          materialAppearances.put(appearanceEntry.getValue(), appearanceEntry.getKey().getAppearance());
        }
      }
    } finally {
      if (zipOut != null) {
        zipOut.close();
      }
      for (File tempFile : tempFolder.listFiles()) {
        if (tempFile.isFile()) {
          tempFile.delete();
        }
      }
      tempFolder.delete();
    }
  }
  
  

  private static class ComparableAppearance {
    private Appearance appearance;
    
    public ComparableAppearance(Appearance appearance) {
      this.appearance = appearance;
    }
    
    public Appearance getAppearance() {
      return this.appearance;
    }
    

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ComparableAppearance) {
        Appearance appearance2 = ((ComparableAppearance)obj).appearance;
        ColoringAttributes coloringAttributes1 = this.appearance.getColoringAttributes();
        ColoringAttributes coloringAttributes2 = appearance2.getColoringAttributes();
        if ((coloringAttributes1 == null) ^ (coloringAttributes2 == null)) {
          return false;
        } else if (coloringAttributes1 != coloringAttributes2) {
          Color3f color1 = new Color3f();
          Color3f color2 = new Color3f();
          coloringAttributes1.getColor(color1);
          coloringAttributes2.getColor(color2);
          if (!color1.equals(color2)) {
            return false;
          } 
        }
        Material material1 = this.appearance.getMaterial();
        Material material2 = appearance2.getMaterial();
        if ((material1 == null) ^ (material2 == null)) {
          return false;
        } else if (material1 != material2) {
          Color3f color1 = new Color3f();
          Color3f color2 = new Color3f();
          material1.getAmbientColor(color1);
          material2.getAmbientColor(color2);
          if (!color1.equals(color2)) {
            return false;
          } else {
            material1.getDiffuseColor(color1);
            material2.getDiffuseColor(color2);
            if (!color1.equals(color2)) {
              return false;
            } else {
              material1.getEmissiveColor(color1);
              material2.getEmissiveColor(color2);
              if (!color1.equals(color2)) {
                return false;
              } else {
                material1.getSpecularColor(color1);
                material2.getSpecularColor(color2);
                if (!color1.equals(color2)) {
                  return false;
                } else if (material1.getShininess() != material2.getShininess()) {
                  return false;
                } else if (material1.getClass() != material2.getClass()) {
                  return false;
                } else if (material1.getClass() == OBJMaterial.class) {
                  OBJMaterial objMaterial1 = (OBJMaterial)material1;
                  OBJMaterial objMaterial2 = (OBJMaterial)material2;
                  if (objMaterial1.isOpticalDensitySet() ^ objMaterial2.isOpticalDensitySet()) {
                    return false;
                  } else if (objMaterial1.isOpticalDensitySet() && objMaterial2.isOpticalDensitySet()
                            && objMaterial1.getOpticalDensity() != objMaterial2.getOpticalDensity()) {
                    return false;
                  } else if (objMaterial1.isIlluminationModelSet() ^ objMaterial2.isIlluminationModelSet()) {
                    return false;
                  } else if (objMaterial1.isIlluminationModelSet() && objMaterial2.isIlluminationModelSet()
                            && objMaterial1.getIlluminationModel() != objMaterial2.getIlluminationModel()) {
                    return false;
                  } else if (objMaterial1.isSharpnessSet() ^ objMaterial2.isSharpnessSet()) {
                    return false;
                  } else if (objMaterial1.isSharpnessSet() && objMaterial2.isSharpnessSet()
                            && objMaterial1.getSharpness() != objMaterial2.getSharpness()) {
                    return false;
                  }
                }
              }
            }
          }
        }
        TransparencyAttributes transparency1 = this.appearance.getTransparencyAttributes();
        TransparencyAttributes transparency2 = appearance2.getTransparencyAttributes();
        if ((transparency1 == null) ^ (transparency2 == null)) {
          return false;
        } else if (transparency1 != transparency2) {
          if (transparency1.getTransparency() != transparency2.getTransparency()) {
            return false;
          }
        }
        Texture texture1 = this.appearance.getTexture();
        Texture texture2 = appearance2.getTexture();
        if ((texture1 == null) ^ (texture2 == null)) {
          return false;
        } else if (texture1 != texture2) {
          if (texture1.getImage(0) != texture2.getImage(0)) {
            return false;
          }
        }
        try {
          String name1 = this.appearance.getName();
          String name2 = appearance2.getName();
          if ((name1 == null) ^ (name2 == null)) {
            return false;
          } else if (name1 != name2 
                     && !name1.equals(name2)) {
            return false;
          }
        } catch (NoSuchMethodError ex) {
          
        }

        return true;
      }
      return false;
    }
    
    @Override
    public int hashCode() {
      int code = 0;
      ColoringAttributes coloringAttributes = appearance.getColoringAttributes();
      if (coloringAttributes != null) {
        Color3f color = new Color3f();
        coloringAttributes.getColor(color);
        code += color.hashCode();
      }
      Material material = this.appearance.getMaterial();
      if (material != null) {
        Color3f color = new Color3f();
        material.getAmbientColor(color);
        code += color.hashCode();
        material.getDiffuseColor(color);
        code += color.hashCode();
        material.getEmissiveColor(color);
        code += color.hashCode();
        material.getSpecularColor(color);
        code += color.hashCode();
        code += Float.floatToIntBits(material.getShininess());
      }
      TransparencyAttributes transparency = this.appearance.getTransparencyAttributes();
      if (transparency != null) {
        code += Float.floatToIntBits(transparency.getTransparency());
      }
      Texture texture = this.appearance.getTexture();
      if (texture != null) {
        code += texture.getImage(0).hashCode();
      }
      try {
        String name = this.appearance.getName();
        if (name != null) {
          code += name.hashCode();
        }
      } catch (NoSuchMethodError ex) {
      }
      return code;
    }
  }
}
