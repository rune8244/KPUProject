package com.eteks.homeview3d.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.URLContent;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.View;

public class FileContentManager implements ContentManager {
  private static final String OBJ_EXTENSION = ".obj";
  
  private static final FileFilter [] OBJ_FILTER = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          // 디렉토리, obj 파일 수락
          return file.isDirectory()
              || file.getName().toLowerCase().endsWith(OBJ_EXTENSION);
        }
        
        @Override
        public String getDescription() {
          return "OBJ - Wavefront";
        }
      }};
  // 3D 모델링파일 지원
  private static final String LWS_EXTENSION = ".lws";
  private static final String THREEDS_EXTENSION = ".3ds";
  private static final String DAE_EXTENSION = ".dae";
  private static final String KMZ_EXTENSION = ".kmz";
  private static final String ZIP_EXTENSION = ".zip";
  private static final FileFilter [] MODEL_FILTERS = {
     OBJ_FILTER [0],
     new FileFilter() {
       @Override
       public boolean accept(File file) {
         // Accept directories and LWS files
         return file.isDirectory()
                || file.getName().toLowerCase().endsWith(LWS_EXTENSION);
       }
   
       @Override
       public String getDescription() {
         return "LWS - LightWave Scene";
       }
     },
     new FileFilter() {
       @Override
       public boolean accept(File file) {
         // 파일 지원
         return file.isDirectory()
                || file.getName().toLowerCase().endsWith(THREEDS_EXTENSION);
       }
   
       @Override
       public String getDescription() {
         return "3DS - 3D Studio";
       }
     },
     new FileFilter() {
       @Override
       public boolean accept(File file) {
         // 파일 지원
         return file.isDirectory()
                || file.getName().toLowerCase().endsWith(DAE_EXTENSION);
       }
   
       @Override
       public String getDescription() {
         return "DAE - Collada";
       }
     },
     new FileFilter() {
       @Override
       public boolean accept(File file) {
         // Zip 파일 지원
         return file.isDirectory()
                || file.getName().toLowerCase().endsWith(KMZ_EXTENSION);
       }
   
       @Override
       public String getDescription() {
         return "KMZ";
       }
     },
     new FileFilter() {
       @Override
       public boolean accept(File file) {
         return file.isDirectory()
                || file.getName().toLowerCase().endsWith(ZIP_EXTENSION);
       }
   
       @Override
       public String getDescription() {
         return "ZIP";
       }
     }};
  private static final String PNG_EXTENSION = ".png";
  // png파일 지원
  private static final FileFilter [] PNG_FILTER = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          return file.isDirectory()
              || file.getName().toLowerCase().endsWith(PNG_EXTENSION);
        }
        
        @Override
        public String getDescription() {
          return "PNG";
        }
      }};
  private static final String JPEG_EXTENSION = ".jpg";
  // jpeg 필터 지원
  private static final FileFilter [] JPEG_FILTER = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          return file.isDirectory()
              || file.getName().toLowerCase().endsWith(JPEG_EXTENSION)
              || file.getName().toLowerCase().endsWith("jpeg");
        }
        
        @Override
        public String getDescription() {
          return "JPEG";
        }
      }};
  
  private static final String BMP_EXTENSION = ".bmp";
  private static final String WBMP_EXTENSION = ".wbmp";
  private static final String GIF_EXTENSION = ".gif";
  private static final FileFilter [] IMAGE_FILTERS = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          // 비트맵 지원
          return file.isDirectory()
                 || file.getName().toLowerCase().endsWith(BMP_EXTENSION)
                 || file.getName().toLowerCase().endsWith(WBMP_EXTENSION);
        }
    
        @Override
        public String getDescription() {
          return "BMP";
        }
      },
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          // Gif 지원
          return file.isDirectory()
                 || file.getName().toLowerCase().endsWith(GIF_EXTENSION);
        }
    
        @Override
        public String getDescription() {
          return "GIF";
        }
      },
      JPEG_FILTER [0], 
      PNG_FILTER [0]};
  private static final String MOV_EXTENSION = ".mov";
  // mov 파일 지원
  private static final FileFilter [] MOV_FILTER = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          return file.isDirectory()
              || file.getName().toLowerCase().endsWith(MOV_EXTENSION);
        }
        
        @Override
        public String getDescription() {
          return "MOV";
        }
      }};
  private static final String PDF_EXTENSION = ".pdf";
  // PDF지원
  private static final FileFilter [] PDF_FILTER = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          return file.isDirectory()
              || file.getName().toLowerCase().endsWith(PDF_EXTENSION);
        }
        
        @Override
        public String getDescription() {
          return "PDF";
        }
      }};
  private static final String CSV_EXTENSION = ".csv";
  
  private static final FileFilter [] CSV_FILTER = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          // csv 파일 지원
          return file.isDirectory()
              || file.getName().toLowerCase().endsWith(CSV_EXTENSION);
        }
        
        @Override
        public String getDescription() {
          return "CSV - Tab Separated Values";
        }
      }};
  private static final String SVG_EXTENSION = ".svg";
  
  private static final FileFilter [] SVG_FILTER = {
      new FileFilter() {
        @Override
        public boolean accept(File file) {
          return file.isDirectory()
              || file.getName().toLowerCase().endsWith(SVG_EXTENSION);
        }
        
        @Override
        public String getDescription() {
          return "SVG - Scalable Vector Graphics";
        }
      }};
  
  private final UserPreferences           preferences;
  private final String                    homeview3dFileExtension;
  private final String                    homeview3dFileExtension2;
  private final String                    languageLibraryFileExtension;
  private final String                    furnitureLibraryFileExtension;
  private final String                    texturesLibraryFileExtension;
  private final String                    pluginFileExtension;
  private Map<ContentType, File>          lastDirectories;
  private Map<ContentType, FileFilter []> fileFilters;
  private Map<ContentType, String []>     fileExtensions;

  public FileContentManager(final UserPreferences preferences) {  
    this.preferences = preferences;
    this.homeview3dFileExtension = preferences.getLocalizedString(FileContentManager.class, "homeExtension");
    String homeExtension2;
    try {
      homeExtension2 = preferences.getLocalizedString(FileContentManager.class, "homeExtension2");
    } catch (IllegalArgumentException ex) {
      homeExtension2 = null;
    }
    this.homeview3dFileExtension2 = homeExtension2;
    this.languageLibraryFileExtension = preferences.getLocalizedString(FileContentManager.class, "languageLibraryExtension");
    this.furnitureLibraryFileExtension = preferences.getLocalizedString(FileContentManager.class, "furnitureLibraryExtension");
    this.texturesLibraryFileExtension = preferences.getLocalizedString(FileContentManager.class, "texturesLibraryExtension");
    this.pluginFileExtension = preferences.getLocalizedString(FileContentManager.class, "pluginExtension");
    this.lastDirectories = new HashMap<ContentManager.ContentType, File>();
    
    // 각 파일 필터 맵 채우기
    this.fileFilters = new HashMap<ContentType, FileFilter[]>();
    this.fileFilters.put(ContentType.MODEL, MODEL_FILTERS);
    this.fileFilters.put(ContentType.IMAGE, IMAGE_FILTERS);
    this.fileFilters.put(ContentType.MOV, MOV_FILTER);
    this.fileFilters.put(ContentType.PNG, PNG_FILTER);
    this.fileFilters.put(ContentType.JPEG, JPEG_FILTER);
    this.fileFilters.put(ContentType.PDF, PDF_FILTER);
    this.fileFilters.put(ContentType.CSV, CSV_FILTER);
    this.fileFilters.put(ContentType.SVG, SVG_FILTER);
    this.fileFilters.put(ContentType.OBJ, OBJ_FILTER);
    this.fileFilters.put(ContentType.SWEET_HOME_3D, new FileFilter [] {
        new FileFilter() {
          @Override
          public boolean accept(File file) {
            return file.isDirectory()
                || file.getName().toLowerCase().endsWith(homeview3dFileExtension)
                || (homeview3dFileExtension2 != null 
                     && file.getName().toLowerCase().endsWith(homeview3dFileExtension2));
          }
          
          @Override
          public String getDescription() {
            return preferences.getLocalizedString(FileContentManager.class, "homeDescription");
          }
        }
      });
    this.fileFilters.put(ContentType.LANGUAGE_LIBRARY, new FileFilter [] {
        new FileFilter() {
          @Override
          public boolean accept(File file) {
            return file.isDirectory()
                || file.getName().toLowerCase().endsWith(languageLibraryFileExtension);
          }
         
          @Override
          public String getDescription() {
            return preferences.getLocalizedString(FileContentManager.class, "languageLibraryDescription");
          }
        }
      });
    this.fileFilters.put(ContentType.FURNITURE_LIBRARY, new FileFilter [] {
        new FileFilter() {
          @Override
          public boolean accept(File file) {
            return file.isDirectory()
                || file.getName().toLowerCase().endsWith(furnitureLibraryFileExtension);
          }
          
          @Override
          public String getDescription() {
            return preferences.getLocalizedString(FileContentManager.class, "furnitureLibraryDescription");
          }
        }
      });
    this.fileFilters.put(ContentType.TEXTURES_LIBRARY, new FileFilter [] {
        new FileFilter() {
          @Override
          public boolean accept(File file) {
            return file.isDirectory()
                || file.getName().toLowerCase().endsWith(texturesLibraryFileExtension);
          }
         
          @Override
          public String getDescription() {
            return preferences.getLocalizedString(FileContentManager.class, "texturesLibraryDescription");
          }
        }
      });
    this.fileFilters.put(ContentType.PLUGIN, new FileFilter [] {
        new FileFilter() {
          @Override
          public boolean accept(File file) {
            return file.isDirectory()
                || file.getName().toLowerCase().endsWith(pluginFileExtension);
          }
         
          @Override
          public String getDescription() {
            return preferences.getLocalizedString(FileContentManager.class, "pluginDescription");
          }
        }
      });
    this.fileFilters.put(ContentType.PHOTOS_DIRECTORY, new FileFilter [] {
        new FileFilter() {
          @Override
          public boolean accept(File file) {
            // 디렉터리만 수락
            return file.isDirectory();
          }
         
          @Override
          public String getDescription() {
            return "Photos";
          }
        }
      });

    this.fileExtensions = new HashMap<ContentType, String []>();
    String [] homeview3dFileExtensions = this.homeview3dFileExtension2 != null
        ? new String [] {this.homeview3dFileExtension, this.homeview3dFileExtension2}
        : new String [] {this.homeview3dFileExtension};
    this.fileExtensions.put(ContentType.SWEET_HOME_3D,     homeview3dFileExtensions);
    this.fileExtensions.put(ContentType.LANGUAGE_LIBRARY,  new String [] {this.languageLibraryFileExtension});
    this.fileExtensions.put(ContentType.FURNITURE_LIBRARY, new String [] {this.furnitureLibraryFileExtension});
    this.fileExtensions.put(ContentType.TEXTURES_LIBRARY,  new String [] {this.texturesLibraryFileExtension});
    this.fileExtensions.put(ContentType.PLUGIN,            new String [] {this.pluginFileExtension});
    this.fileExtensions.put(ContentType.PNG,               new String [] {PNG_EXTENSION});
    this.fileExtensions.put(ContentType.JPEG,              new String [] {JPEG_EXTENSION});
    this.fileExtensions.put(ContentType.MOV,               new String [] {MOV_EXTENSION});
    this.fileExtensions.put(ContentType.PDF,               new String [] {PDF_EXTENSION});
    this.fileExtensions.put(ContentType.CSV,               new String [] {CSV_EXTENSION});
    this.fileExtensions.put(ContentType.SVG,               new String [] {SVG_EXTENSION});
    this.fileExtensions.put(ContentType.OBJ,               new String [] {OBJ_EXTENSION});
    this.fileExtensions.put(ContentType.MODEL,             
        new String [] {OBJ_EXTENSION, LWS_EXTENSION, THREEDS_EXTENSION, DAE_EXTENSION, ZIP_EXTENSION, KMZ_EXTENSION});
    this.fileExtensions.put(ContentType.IMAGE,             
        new String [] {PNG_EXTENSION, JPEG_EXTENSION, BMP_EXTENSION, WBMP_EXTENSION, GIF_EXTENSION} );
  }
  
 public Content getContent(String contentPath) throws RecorderException {
    try {
      return new URLContent(new File(contentPath).toURI().toURL());
    } catch (IOException ex) {
      throw new RecorderException("Couldn't access to content " + contentPath);
    }
  }
  
 public String getPresentationName(String contentPath, 
                                    ContentType contentType) {
    switch (contentType) {
      case SWEET_HOME_3D :
      case FURNITURE_LIBRARY :
      case TEXTURES_LIBRARY :
      case LANGUAGE_LIBRARY :
      case PLUGIN :
        return new File(contentPath).getName();
      default :
        String fileName = new File(contentPath).getName();
        int pointIndex = fileName.lastIndexOf('.');
        if (pointIndex != -1) {
          fileName = fileName.substring(0, pointIndex);
        }
        return fileName;
    }    
  }
  
 protected FileFilter [] getFileFilter(ContentType contentType) {
    if (contentType == ContentType.USER_DEFINED) {
      throw new IllegalArgumentException("Unknown user defined content type");
    } else {
      return this.fileFilters.get(contentType);
    }
  }
  
  
  public String getDefaultFileExtension(ContentType contentType) {
    String [] fileExtensions = this.fileExtensions.get(contentType);
    if (fileExtensions != null) {
      return fileExtensions [0];
    }
    return null;
  }
  
  
  protected String [] getFileExtensions(ContentType contentType) {
    return this.fileExtensions.get(contentType);
  }
  
  
  public boolean isAcceptable(String contentPath, 
                              ContentType contentType) {
    for (FileFilter filter : getFileFilter(contentType)) {
      if (filter.accept(new File(contentPath))) {
        return true;
      }
    }
    return false;
  }
  
  
  protected boolean isDirectory(ContentType contentType) {
    return contentType == ContentType.PHOTOS_DIRECTORY;
  }
  

  public String showOpenDialog(View        parentView,
                               String      dialogTitle,
                               ContentType contentType) {
    if (OperatingSystem.isMacOSX()
        && !isDirectory(contentType)) {
      return showFileDialog(parentView, dialogTitle, contentType, null, false);
    } else {
      return showFileChooser(parentView, dialogTitle, contentType, null, false);
    }
  }
  
  public String showSaveDialog(View        parentView,
                               String      dialogTitle,
                               ContentType contentType,
                               String      path) {
    String defaultExtension = getDefaultFileExtension(contentType);
    if (path != null) {
      int extensionIndex = path.lastIndexOf('.');
      if (extensionIndex != -1) {
        path = path.substring(0, extensionIndex);
        if (defaultExtension != null) {
          path += defaultExtension;
        }
      }
    }
    
    String savedPath;
    if (OperatingSystem.isMacOSX()
        && !isDirectory(contentType)) {
      savedPath = showFileDialog(parentView, dialogTitle, contentType, path, true);
    } else {
      savedPath = showFileChooser(parentView, dialogTitle, contentType, path, true);
    }
    
    boolean addedExtension = false;
    if (savedPath != null) {
      if (defaultExtension != null) {
        if (!savedPath.toLowerCase().endsWith(defaultExtension)) {
          savedPath += defaultExtension;
          addedExtension = true;
        }
      }

     if (OperatingSystem.isMacOSX()
          && !addedExtension) {
        return savedPath;
      }
      if (!isDirectory(contentType)) {
        // 파일 존재할 경우 덮어쓰기
        File savedFile = new File(savedPath);
        if (savedFile.exists()
            && !confirmOverwrite(parentView, savedFile.getName())) {
          return showSaveDialog(parentView, dialogTitle, contentType, savedPath);
        }
      }
    }
    return savedPath;
  }
  
 
  private String showFileDialog(View               parentView,
                                String             dialogTitle,
                                final ContentType  contentType,
                                String             path, 
                                boolean            save) {
    FileDialog fileDialog = new FileDialog(
        JOptionPane.getFrameForComponent((JComponent)parentView));

    // 선택된 파일 셋팅
    if (save && path != null) {
      fileDialog.setFile(new File(path).getName());
    }
    // 지원되는 파일 필터 셋팅
    fileDialog.setFilenameFilter(new FilenameFilter() {
        public boolean accept(File dir, String name) {          
          return isAcceptable(new File(dir, name).toString(), contentType);
        }
      });

    // 디렉토리 업데이트
    File directory = getLastDirectory(contentType);
    if (directory != null && directory.exists()) {
      if (isDirectory(contentType)) {
        fileDialog.setDirectory(directory.getParent());
        fileDialog.setFile(directory.getName());
      } else {
        fileDialog.setDirectory(directory.toString());
      }
    }
    if (save) {
      fileDialog.setMode(FileDialog.SAVE);
    } else {
      fileDialog.setMode(FileDialog.LOAD);
    }

    if (dialogTitle == null) {
      dialogTitle = getFileDialogTitle(save);
    }
    fileDialog.setTitle(dialogTitle);
    
    fileDialog.setVisible(true);

    String selectedFile = fileDialog.getFile();
    // 파일 선택할 경우
    if (selectedFile != null) {
      selectedFile = new File(fileDialog.getDirectory(), selectedFile).toString();
      // Retrieve directory for future calls
      if (isDirectory(contentType)) {
        directory = new File(selectedFile);
      } else {
        directory = new File(fileDialog.getDirectory());
      }
      setLastDirectory(contentType, directory);
      return selectedFile;
    } else {
      return null;
    }
  }

  // 가장 최근 디렉토리로
  protected File getLastDirectory(ContentType contentType) {
    File directory = this.lastDirectories.get(contentType);
    if (directory == null) {
      directory = this.lastDirectories.get(null);
    }
    return directory;
  }

  protected void setLastDirectory(ContentType contentType, File directory) {
    this.lastDirectories.put(contentType, directory);
    // Store default last directory in null content 
    this.lastDirectories.put(null, directory);
  }

  
  private String showFileChooser(View          parentView,
                                 String        dialogTitle,
                                 ContentType   contentType,
                                 String        path,
                                 boolean       save) {
    final JFileChooser fileChooser;
    if (isDirectory(contentType)) {
      fileChooser = new DirectoryChooser(this.preferences);
    } else {
      fileChooser = new JFileChooser();
    }
    if (dialogTitle == null) {
      dialogTitle = getFileDialogTitle(save);
    }
    fileChooser.setDialogTitle(dialogTitle);


    File directory = getLastDirectory(contentType);
    if (directory != null && directory.exists()) {
      if (isDirectory(contentType)) {
        fileChooser.setCurrentDirectory(directory.getParentFile());
        fileChooser.setSelectedFile(directory);
      } else {
        fileChooser.setCurrentDirectory(directory);
      }
    }    
    if (save 
        && path != null
        && (directory == null || !isDirectory(contentType))) {
      fileChooser.setSelectedFile(new File(path));
    }    

    FileFilter acceptAllFileFilter = fileChooser.getAcceptAllFileFilter();
    fileChooser.addChoosableFileFilter(acceptAllFileFilter);
    FileFilter [] contentFileFilters = getFileFilter(contentType);
    for (FileFilter filter : contentFileFilters) {
      fileChooser.addChoosableFileFilter(filter);
    }

    if (contentFileFilters.length == 1) {
      fileChooser.setFileFilter(contentFileFilters [0]);
    } else {
      fileChooser.setFileFilter(acceptAllFileFilter);
    }
    int option;
    if (isDirectory(contentType)) {
      option = fileChooser.showDialog((JComponent)parentView,
          this.preferences.getLocalizedString(FileContentManager.class, "selectFolderButton.text"));
    } else if (save) {
      option = fileChooser.showSaveDialog((JComponent)parentView);
    } else {
      option = fileChooser.showOpenDialog((JComponent)parentView);
    }    
    if (option == JFileChooser.APPROVE_OPTION) {
      if (isDirectory(contentType)) {
        directory = fileChooser.getSelectedFile();
      } else {
        directory = fileChooser.getCurrentDirectory();
      }
      setLastDirectory(contentType, directory);
      return fileChooser.getSelectedFile().toString();
    } else {
      return null;
    }
  }


  protected String getFileDialogTitle(boolean save) {
    if (save) {
      return this.preferences.getLocalizedString(FileContentManager.class, "saveDialog.title");
    } else {
      return this.preferences.getLocalizedString(FileContentManager.class, "openDialog.title");
    }
  }
    
  protected boolean confirmOverwrite(View parentView, String path) {
    String message = this.preferences.getLocalizedString(FileContentManager.class, "confirmOverwrite.message", path);
    String title = this.preferences.getLocalizedString(FileContentManager.class, "confirmOverwrite.title");
    String replace = this.preferences.getLocalizedString(FileContentManager.class, "confirmOverwrite.overwrite");
    String cancel = this.preferences.getLocalizedString(FileContentManager.class, "confirmOverwrite.cancel");
    
    return JOptionPane.showOptionDialog(SwingUtilities.getRootPane((JComponent)parentView), 
        message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, new Object [] {replace, cancel}, cancel) == JOptionPane.OK_OPTION;
  }

  private static class DirectoryChooser extends JFileChooser {
    private Executor               fileSystemViewExecutor;
    private JTree                  directoriesTree;
    private TreeSelectionListener  treeSelectionListener;
    private PropertyChangeListener selectedFileListener;
    private Action                 createDirectoryAction;

    public DirectoryChooser(final UserPreferences preferences) {
      setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      this.fileSystemViewExecutor = Executors.newSingleThreadExecutor();
      this.directoriesTree = new JTree(new DefaultTreeModel(new DirectoryNode()));
      this.directoriesTree.setRootVisible(false);
      this.directoriesTree.setEditable(false);
      this.directoriesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      this.directoriesTree.setCellRenderer(new DefaultTreeCellRenderer() {
          @Override
          public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                        boolean leaf, int row, boolean hasFocus) {
            DirectoryNode node = (DirectoryNode)value;
            File file = (File)node.getUserObject();
            super.getTreeCellRendererComponent(tree, DirectoryChooser.this.getName(file), 
                selected, expanded, leaf, row, hasFocus);
            setIcon(DirectoryChooser.this.getIcon(file));
            if (!node.isWritable()) {
              setForeground(Color.GRAY);
            } 
            return this;
          }
        });
      this.treeSelectionListener = new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent ev) {
            TreePath selectionPath = directoriesTree.getSelectionPath();
            if (selectionPath != null) {
              DirectoryNode selectedNode = (DirectoryNode)selectionPath.getLastPathComponent();
              setSelectedFile((File)selectedNode.getUserObject());
            }
          }
        };
      this.directoriesTree.addTreeSelectionListener(this.treeSelectionListener);
      
      this.selectedFileListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            showSelectedFile();
          }
        };
      addPropertyChangeListener(SELECTED_FILE_CHANGED_PROPERTY, this.selectedFileListener);
      
      this.directoriesTree.addTreeExpansionListener(new TreeExpansionListener() {
          public void treeCollapsed(TreeExpansionEvent ev) {
            if (ev.getPath().isDescendant(directoriesTree.getSelectionPath())) {
              // If selected node becomes hidden select not hidden parent
              removePropertyChangeListener(SELECTED_FILE_CHANGED_PROPERTY, selectedFileListener);
              directoriesTree.setSelectionPath(ev.getPath());
              addPropertyChangeListener(SELECTED_FILE_CHANGED_PROPERTY, selectedFileListener);
            }
          }
          
          public void treeExpanded(TreeExpansionEvent ev) {
          }
        });
      
      final String newDirectoryText = UIManager.getString("FileChooser.win32.newFolder");
      this.createDirectoryAction = new AbstractAction(newDirectoryText) {
          public void actionPerformed(ActionEvent ev) {
            String newDirectoryNameBase = OperatingSystem.isWindows() || OperatingSystem.isMacOSX()
                ? newDirectoryText
                : UIManager.getString("FileChooser.other.newFolder");
            String newDirectoryName = newDirectoryNameBase;
            // 해당 파일 없을 경우 새로운 경로
            DirectoryNode parentNode = (DirectoryNode)directoriesTree.getLastSelectedPathComponent();
            File parentDirectory = (File)parentNode.getUserObject();
            for (int i = 2; new File(parentDirectory, newDirectoryName).exists(); i++) {
              newDirectoryName = newDirectoryNameBase;
              if (OperatingSystem.isWindows() || OperatingSystem.isMacOSX()) {
                newDirectoryName += " ";
              }
              newDirectoryName += i;
            }
            newDirectoryName = (String)JOptionPane.showInputDialog(DirectoryChooser.this, 
                preferences.getLocalizedString(FileContentManager.class, "createFolder.message"),                
                newDirectoryText, JOptionPane.QUESTION_MESSAGE, null, null, newDirectoryName);
            if (newDirectoryName != null) {
              File newDirectory = new File(parentDirectory, newDirectoryName);
              if (!newDirectory.mkdir()) {
                String newDirectoryErrorText = UIManager.getString("FileChooser.newFolderErrorText");
                JOptionPane.showMessageDialog(DirectoryChooser.this, 
                    newDirectoryErrorText, newDirectoryErrorText, JOptionPane.ERROR_MESSAGE);
              } else {
                parentNode.updateChildren(parentNode.getChildDirectories());
                ((DefaultTreeModel)directoriesTree.getModel()).nodeStructureChanged(parentNode);
                setSelectedFile(newDirectory);
              }
            }
          }
        };
      
      setSelectedFile(getFileSystemView().getHomeDirectory());
    }
    
    
    @Override
    public void setSelectedFile(File file) {
      if (file != null && file.isFile()) {
        file = file.getParentFile();
      }
      super.setSelectedFile(file);
    }
    

    private void showSelectedFile() {
      final File selectedFile = getSelectedFile();
      if (selectedFile != null) {
        final DirectoryNode rootNode = (DirectoryNode)this.directoriesTree.getModel().getRoot();
        this.fileSystemViewExecutor.execute(new Runnable() {
            public void run() {
              try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                      createDirectoryAction.setEnabled(false);
                      if (directoriesTree.isShowing()) {
                        directoriesTree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                      }
                    }
                  });
                File cononicalFile = selectedFile.getCanonicalFile();
                List<File> parentsAndFile = new ArrayList<File>();
                for (File file = cononicalFile; 
                    file != null;
                    file = getFileSystemView().getParentDirectory(file)) {
                  parentsAndFile.add(0, file);
                }
                final List<DirectoryNode> pathToFileNode = new ArrayList<DirectoryNode>();
                DirectoryNode node = rootNode;
                pathToFileNode.add(node);
                for (final File file : parentsAndFile) {
                  final File [] childDirectories = node.isLoaded() 
                      ? null 
                      : node.getChildDirectories();
                  final DirectoryNode currentNode = node;
                  EventQueue.invokeAndWait(new Runnable() {
                      public void run() {
                        if (!currentNode.isLoaded()) {
                          currentNode.updateChildren(childDirectories);
                          ((DefaultTreeModel)directoriesTree.getModel()).nodeStructureChanged(currentNode);
                        }
                        for (int i = 0, n = currentNode.getChildCount(); i < n; i++) {
                          DirectoryNode child = (DirectoryNode)currentNode.getChildAt(i);
                          if (file.equals(child.getUserObject())) {
                            pathToFileNode.add(child);
                            break;
                          }
                        }
                      }
                    });
                  node = pathToFileNode.get(pathToFileNode.size() - 1);
                  if (currentNode == node) { // 파일 없을 경우
                    break;
                  }
                }
                  
                if (pathToFileNode.size() > 1) {
                  final TreePath path = new TreePath(pathToFileNode.toArray(new TreeNode [pathToFileNode.size()]));
                  EventQueue.invokeAndWait(new Runnable() {
                      public void run() {
                        directoriesTree.removeTreeSelectionListener(treeSelectionListener);
                        directoriesTree.expandPath(path);
                        directoriesTree.setSelectionPath(path);
                        directoriesTree.scrollRowToVisible(directoriesTree.getRowForPath(path));
                        directoriesTree.addTreeSelectionListener(treeSelectionListener);
                      }
                    });                    
                }
                
              } catch (IOException ex) {
                // 찾을 수 없을 경우 경로 무시
              } catch (InterruptedException ex) {
                // 인터럽트 시 포기
              } catch (InvocationTargetException ex) {
                ex.printStackTrace();
              } finally {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      createDirectoryAction.setEnabled(directoriesTree.getSelectionCount() > 0 
                          && ((DirectoryNode)directoriesTree.getSelectionPath().getLastPathComponent()).isWritable());
                      directoriesTree.setCursor(Cursor.getDefaultCursor());
                    }
                  });
              }
            }
          });
      }
    }

    @Override
    public int showDialog(Component parent, final String approveButtonText) {
      final JButton createDirectoryButton = new JButton(this.createDirectoryAction);
      final JButton approveButton = new JButton(approveButtonText);
      Object cancelOption = UIManager.get("FileChooser.cancelButtonText");
      Object [] options;
      if (OperatingSystem.isMacOSX()) {
        options = new Object [] {approveButton, cancelOption, createDirectoryButton};
      } else {
        options = new Object [] {createDirectoryButton, approveButton, cancelOption};
      }
      final JOptionPane optionPane = new JOptionPane(SwingTools.createScrollPane(this.directoriesTree), 
          JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, approveButton); 
      final JDialog dialog = optionPane.createDialog(SwingUtilities.getRootPane(parent), getDialogTitle());
      dialog.setResizable(true);
      dialog.pack();
      if (this.directoriesTree.getSelectionCount() > 0) {
        this.directoriesTree.scrollPathToVisible(this.directoriesTree.getSelectionPath());
        boolean validDirectory = ((DirectoryNode)this.directoriesTree.getSelectionPath().getLastPathComponent()).isWritable();
        approveButton.setEnabled(validDirectory);
        createDirectoryAction.setEnabled(validDirectory);
      }
      this.directoriesTree.addTreeSelectionListener(new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent ev) {
            TreePath selectedPath = ev.getPath();
            boolean validDirectory = selectedPath != null 
                && ((DirectoryNode)ev.getPath().getLastPathComponent()).isWritable();
            approveButton.setEnabled(validDirectory);
            createDirectoryAction.setEnabled(validDirectory); 
          }
        });
      approveButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            optionPane.setValue(approveButtonText);
            dialog.setVisible(false);
          }
        });
      dialog.setMinimumSize(dialog.getPreferredSize());
      dialog.setVisible(true);
      dialog.dispose();
      if (approveButtonText.equals(optionPane.getValue())) {
        return JFileChooser.APPROVE_OPTION;
      } else {
        return JFileChooser.CANCEL_OPTION;
      }
    }


    private class DirectoryNode extends DefaultMutableTreeNode {
      private boolean loaded;
      private boolean writable;

      private DirectoryNode() {
        super(null);
      }

      private DirectoryNode(File file) {
        super(file);
        this.writable = file.canWrite();
      }

      public boolean isWritable() {
        return this.writable;
      }
      
      @Override
      public int getChildCount() {
        if (!this.loaded) {
          this.loaded = true;
          return updateChildren(getChildDirectories());
        } else {
          return super.getChildCount();
        }
      }

      public File [] getChildDirectories() {
        File [] childFiles = getUserObject() == null
            ? getFileSystemView().getRoots()
            : getFileSystemView().getFiles((File)getUserObject(), true);
        if (childFiles != null) {
          List<File> childDirectories = new ArrayList<File>(childFiles.length);
          for (File childFile : childFiles) {
            if (isTraversable(childFile)) {
              childDirectories.add(childFile);
            }
          }
          return childDirectories.toArray(new File [childDirectories.size()]);
        } else {
          return new File [0];
        }
      }
      
      public boolean isLoaded() {
        return this.loaded;
      }

      public int updateChildren(File [] childDirectories) {
        if (this.children == null) {
          this.children = new Vector<File>(childDirectories.length); 
        }          
        synchronized (this.children) {
          removeAllChildren();
          for (File childFile : childDirectories) {
            add(new DirectoryNode(childFile));
          }
          return childDirectories.length;
        }
      }
    }
  }
}
