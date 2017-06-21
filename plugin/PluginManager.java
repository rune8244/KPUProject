package com.eteks.homeview3d.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.model.Library;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.HomeController;


public class PluginManager {
  public static final String PLUGIN_LIBRARY_TYPE = "Plugin"; 
  
  private static final String ID                          = "id";
  private static final String NAME                        = "name";
  private static final String CLASS                       = "class";
  private static final String DESCRIPTION                 = "description";
  private static final String VERSION                     = "version";
  private static final String LICENSE                     = "license";
  private static final String PROVIDER                    = "provider";
  private static final String APPLICATION_MINIMUM_VERSION = "applicationMinimumVersion";
  private static final String JAVA_MINIMUM_VERSION        = "javaMinimumVersion";

  private static final String APPLICATION_PLUGIN_FAMILY   = "ApplicationPlugin";

  private static final String DEFAULT_APPLICATION_PLUGIN_PROPERTIES_FILE = 
      APPLICATION_PLUGIN_FAMILY + ".properties";

  private final File [] pluginFolders;
  private final Map<String, PluginLibrary> pluginLibraries = 
      new TreeMap<String, PluginLibrary>();
  private final Map<Home, List<Plugin>> homePlugins = new LinkedHashMap<Home, List<Plugin>>();
  
  /**
   * 지정된 플러그인 폴더의 자원에서 응용 프로그램 플러그인 읽기.
   */
  public PluginManager(File pluginFolder) {
    this(new File [] {pluginFolder});
  }
  

  public PluginManager(File [] pluginFolders) {
    this.pluginFolders = pluginFolders;
    if (pluginFolders != null) {
      for (File pluginFolder : pluginFolders) {
        // 플러그인 폴더에서 플러그인 파일 불러오기
        File [] pluginFiles = pluginFolder.listFiles(new FileFilter () {
          public boolean accept(File pathname) {
            return pathname.isFile();
          }
        });
        
        if (pluginFiles != null) {
          // 플러그인 파일을 버전 번호 역순으로 처리
          Arrays.sort(pluginFiles, Collections.reverseOrder(OperatingSystem.getFileVersionComparator()));
          for (File pluginFile : pluginFiles) {
            try {
              loadPlugins(pluginFile.toURI().toURL(), pluginFile.getAbsolutePath());
            } catch (MalformedURLException ex) {
            }
          }
        }
      }
    }
  }

  /**
   * 지정된 URL 리소스에서 응용 프로그램 플러그인 읽기.
   */
  public PluginManager(URL [] pluginUrls) {
    this.pluginFolders = null;
    for (URL pluginUrl : pluginUrls) {
      loadPlugins(pluginUrl, pluginUrl.toExternalForm());
    }
  }

  /**
   * 지정된 URL로 사용 가능한 플러그인 로드.
   */
  private void loadPlugins(URL pluginUrl, String pluginLocation) {
    ZipInputStream zipIn = null;
    try {
      // 플러그인 URL에서 zip 파일 열기
      zipIn = new ZipInputStream(pluginUrl.openStream());
      // zip에서 플러그인 속성 파일 찾기
      for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
        String zipEntryName = entry.getName();
        int lastIndex = zipEntryName.lastIndexOf(DEFAULT_APPLICATION_PLUGIN_PROPERTIES_FILE);
        if (lastIndex != -1
            && (lastIndex == 0
                || zipEntryName.charAt(lastIndex - 1) == '/')) {
          try { 
            String applicationPluginFamily = zipEntryName.substring(0, lastIndex);
            applicationPluginFamily += APPLICATION_PLUGIN_FAMILY;
            ClassLoader classLoader = new URLClassLoader(new URL [] {pluginUrl}, getClass().getClassLoader());
            readPlugin(ResourceBundle.getBundle(applicationPluginFamily, Locale.getDefault(), classLoader), 
                pluginLocation, 
                "jar:" + pluginUrl.toString() + "!/" + URLEncoder.encode(zipEntryName, "UTF-8").replace("+", "%20"),
                classLoader);
          } catch (MissingResourceException ex) {
            // 조작된 플러그인 무시
          }
        }
      }
    } catch (IOException ex) {
      // 가구 플러그인 무시
    } finally {
      if (zipIn != null) {
        try {
          zipIn.close();
        } catch (IOException ex) {
        }
      }
    }
  }
  

  private void readPlugin(ResourceBundle resource,
                          String         pluginLocation,
                          String         pluginEntry,
                          ClassLoader    pluginClassLoader) {
    try {
      String name = resource.getString(NAME);

      // 자바와 애플리케이션 버전 확인
      String javaMinimumVersion = resource.getString(JAVA_MINIMUM_VERSION);
      if (!OperatingSystem.isJavaVersionGreaterOrEqual(javaMinimumVersion)) {
        System.err.println("Invalid plug-in " + pluginEntry + ":\n" 
            + "Not compatible Java version " + System.getProperty("java.version"));
        return;
      }
      
      String applicationMinimumVersion = resource.getString(APPLICATION_MINIMUM_VERSION);
      if (!isApplicationVersionSuperiorTo(applicationMinimumVersion)) {
        System.err.println("Invalid plug-in " + pluginEntry + ":\n" 
            + "Not compatible application version");
        return;
      }
      
      String pluginClassName = resource.getString(CLASS);
      Class<? extends Plugin> pluginClass = getPluginClass(pluginClassLoader, pluginClassName);
      
      String id = getOptionalString(resource, ID, null);
      String description = resource.getString(DESCRIPTION);
      String version = resource.getString(VERSION);
      String license = resource.getString(LICENSE);
      String provider = resource.getString(PROVIDER);
      
      // 플러그인 속성 없을 경우 저장
      if (this.pluginLibraries.get(name) == null) {
        this.pluginLibraries.put(name, new PluginLibrary(
            pluginLocation, id, name, description, version, license, provider, pluginClass, pluginClassLoader));
      }      
    } catch (MissingResourceException ex) {
      System.err.println("Invalid plug-in " + pluginEntry + ":\n" + ex.getMessage());
    } catch (IllegalArgumentException ex) {
      System.err.println("Invalid plug-in " + pluginEntry + ":\n" + ex.getMessage());
    } 
  }


  private String getOptionalString(ResourceBundle resource, String key, String defaultValue) {
    try {
      return resource.getString(key);
    } catch (MissingResourceException ex) {
      return defaultValue;
    }
  }
  

  private boolean isApplicationVersionSuperiorTo(String applicationMinimumVersion) {
    String [] applicationMinimumVersionParts = applicationMinimumVersion.split("\\.|_|\\s");
    if (applicationMinimumVersionParts.length >= 1) {
      try {
        // 첫번째 부분 숫자 비교
        int applicationVersionFirstPart = (int)(Home.CURRENT_VERSION / 1000);
        int applicationMinimumVersionFirstPart = Integer.parseInt(applicationMinimumVersionParts [0]);        
        if (applicationVersionFirstPart > applicationMinimumVersionFirstPart) {
          return true;
        } else if (applicationVersionFirstPart == applicationMinimumVersionFirstPart 
                   && applicationMinimumVersionParts.length >= 2) { 
          // 두번쨰 부분 숫자 비교
          return ((Home.CURRENT_VERSION / 100) % 10) >= Integer.parseInt(applicationMinimumVersionParts [1]);
        }
      } catch (NumberFormatException ex) {
      }
    }
    return false;
  }
  

  @SuppressWarnings("unchecked")
  private Class<? extends Plugin> getPluginClass(ClassLoader pluginClassLoader,
                                                 String pluginClassName) {
    try {
      Class<? extends Plugin> pluginClass = 
          (Class<? extends Plugin>)pluginClassLoader.loadClass(pluginClassName);
      if (!Plugin.class.isAssignableFrom(pluginClass)) {
        throw new IllegalArgumentException(
            pluginClassName + " not a subclass of " + Plugin.class.getName());
      } else if (Modifier.isAbstract(pluginClass.getModifiers())
                 || !Modifier.isPublic(pluginClass.getModifiers())) {
        throw new IllegalArgumentException( 
            pluginClassName + " not a public static class");
      }
      Constructor<? extends Plugin> constructor = pluginClass.getConstructor(new Class [0]);
      if (!Modifier.isPublic(constructor.getModifiers())) {
        throw new IllegalArgumentException( 
            pluginClassName + " constructor not accessible");
      }
      return pluginClass;
    } catch (NoClassDefFoundError ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    } catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }

  /**
   * 사용가능한 플러그인 라이브러리 반환.
   */
  public List<Library> getPluginLibraries() {
    return Collections.unmodifiableList(new ArrayList<Library>(this.pluginLibraries.values()));
  }
  
  /**
   * 플러그인 인스턴스의 변경 불가능한 리스트 반환.
   */
  public List<Plugin> getPlugins(final HomeApplication application, 
                                 final Home home, 
                                 UserPreferences preferences,                                 
                                 UndoableEditSupport undoSupport) {
    return getPlugins(application, home, preferences, null, undoSupport);
  }
    

  List<Plugin> getPlugins(final HomeApplication application, 
                          final Home home, 
                          UserPreferences preferences,
                          HomeController homeController,
                          UndoableEditSupport undoSupport) {
    if (application.getHomes().contains(home)) {
      List<Plugin> plugins = this.homePlugins.get(home);
      if (plugins == null) {
        plugins = new ArrayList<Plugin>();
        // 각 플러그인 클래스 인스턴스화
        for (PluginLibrary pluginLibrary : this.pluginLibraries.values()) {
          try {
            Plugin plugin = pluginLibrary.getPluginClass().newInstance();                      
            plugin.setPluginClassLoader(pluginLibrary.getPluginClassLoader());
            plugin.setName(pluginLibrary.getName());
            plugin.setDescription(pluginLibrary.getDescription());
            plugin.setVersion(pluginLibrary.getVersion());
            plugin.setLicense(pluginLibrary.getLicense());
            plugin.setProvider(pluginLibrary.getProvider());
            plugin.setUserPreferences(preferences);
            plugin.setHome(home);
            plugin.setHomeController(homeController);
            plugin.setUndoableEditSupport(undoSupport);
            plugins.add(plugin);
          } catch (InstantiationException ex) {            
            throw new RuntimeException(ex);
          } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
          } 
        }
        
        plugins = Collections.unmodifiableList(plugins);
        this.homePlugins.put(home, plugins);
        
        // 집이 삭제 될 때 모든 플러그인 파괴
        application.addHomesListener(new CollectionListener<Home>() {
            public void collectionChanged(CollectionEvent<Home> ev) {
              if (ev.getType() == CollectionEvent.Type.DELETE
                  && ev.getItem() == home) {
                for (Plugin plugin : homePlugins.get(home)) {
                  plugin.destroy();
                }                
                homePlugins.remove(home);
                application.removeHomesListener(this);
              }
            }
          });
      }
      return plugins;
    } else {
      return Collections.emptyList();
    }
  }
  
  public boolean pluginExists(String pluginLocation) throws RecorderException {
    if (this.pluginFolders == null
        || this.pluginFolders.length == 0) {
      throw new RecorderException("Can't access to plugins folder");
    } else {
      String pluginFileName = new File(pluginLocation).getName();
      return new File(this.pluginFolders [0], pluginFileName).exists();
    }
  }

  public void deletePlugins(List<Library> libraries) throws RecorderException {
    for (Library library : libraries) {
      for (Iterator<Map.Entry<String, PluginLibrary>> it = this.pluginLibraries.entrySet().iterator(); it.hasNext(); ) {
        String pluginLocation = it.next().getValue().getLocation();
        if (pluginLocation.equals(library.getLocation())) {
          if (new File(pluginLocation).exists()
              && !new File(pluginLocation).delete()) {
            throw new RecorderException("Couldn't delete file " + library.getLocation());
          }
          it.remove();
        }
      }
    }
  }
  
  public void addPlugin(String pluginPath) throws RecorderException {
    try {
      if (this.pluginFolders == null
          || this.pluginFolders.length == 0) {
        throw new RecorderException("Can't access to plugins folder");
      }
      String pluginFileName = new File(pluginPath).getName();
      File destinationFile = new File(this.pluginFolders [0], pluginFileName);

      // furnitureCatalogFile을 가구 플러그인 폴더에 복사
      InputStream tempIn = null;
      OutputStream tempOut = null;
      try {
        tempIn = new BufferedInputStream(new FileInputStream(pluginPath));
        this.pluginFolders [0].mkdirs();
        tempOut = new FileOutputStream(destinationFile);          
        byte [] buffer = new byte [8192];
        int size; 
        while ((size = tempIn.read(buffer)) != -1) {
          tempOut.write(buffer, 0, size);
        }
      } finally {
        if (tempIn != null) {
          tempIn.close();
        }
        if (tempOut != null) {
          tempOut.close();
        }
      }
    } catch (IOException ex) {
      throw new RecorderException(
          "Can't write " + pluginPath +  " in plugins folder", ex);
    }
  }

  /**
   * 플러그인 인스턴스화에 필요한 속성
   */
  private static class PluginLibrary implements Library {
    private final String                  location;
    private final String                  name;
    private final String                  id;
    private final String                  description;
    private final String                  version;
    private final String                  license;
    private final String                  provider;
    private final Class<? extends Plugin> pluginClass;
    private final ClassLoader             pluginClassLoader;
    
    /**
     * 매개 변수로 플러그인 속성 작성. 
     */
    public PluginLibrary(String location,
                         String id,
                         String name, String description, String version, 
                         String license, String provider,
                         Class<? extends Plugin> pluginClass, ClassLoader pluginClassLoader) {
      this.location = location;
      this.id = id;
      this.name = name;
      this.description = description;
      this.version = version;
      this.license = license;
      this.provider = provider;
      this.pluginClass = pluginClass;
      this.pluginClassLoader = pluginClassLoader;
    }

    public Class<? extends Plugin> getPluginClass() {
      return this.pluginClass;
    }

    public ClassLoader getPluginClassLoader() {
      return this.pluginClassLoader;
    }
    
    public String getType() {
      return PluginManager.PLUGIN_LIBRARY_TYPE;
    }
    
    public String getLocation() {
      return this.location;
    }
    
    public String getId() {
      return this.id;
    }

    public String getName() {
      return this.name;
    }
    
    public String getDescription() {
      return this.description;
    }

    public String getVersion() {
      return this.version;
    }

    public String getLicense() {
      return this.license;
    }

    public String getProvider() {
      return this.provider;
    }
  }
}
