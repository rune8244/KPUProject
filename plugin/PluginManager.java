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
   * ������ �÷����� ������ �ڿ����� ���� ���α׷� �÷����� �б�.
   */
  public PluginManager(File pluginFolder) {
    this(new File [] {pluginFolder});
  }
  

  public PluginManager(File [] pluginFolders) {
    this.pluginFolders = pluginFolders;
    if (pluginFolders != null) {
      for (File pluginFolder : pluginFolders) {
        // �÷����� �������� �÷����� ���� �ҷ�����
        File [] pluginFiles = pluginFolder.listFiles(new FileFilter () {
          public boolean accept(File pathname) {
            return pathname.isFile();
          }
        });
        
        if (pluginFiles != null) {
          // �÷����� ������ ���� ��ȣ �������� ó��
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
   * ������ URL ���ҽ����� ���� ���α׷� �÷����� �б�.
   */
  public PluginManager(URL [] pluginUrls) {
    this.pluginFolders = null;
    for (URL pluginUrl : pluginUrls) {
      loadPlugins(pluginUrl, pluginUrl.toExternalForm());
    }
  }

  /**
   * ������ URL�� ��� ������ �÷����� �ε�.
   */
  private void loadPlugins(URL pluginUrl, String pluginLocation) {
    ZipInputStream zipIn = null;
    try {
      // �÷����� URL���� zip ���� ����
      zipIn = new ZipInputStream(pluginUrl.openStream());
      // zip���� �÷����� �Ӽ� ���� ã��
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
            // ���۵� �÷����� ����
          }
        }
      }
    } catch (IOException ex) {
      // ���� �÷����� ����
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

      // �ڹٿ� ���ø����̼� ���� Ȯ��
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
      
      // �÷����� �Ӽ� ���� ��� ����
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
        // ù��° �κ� ���� ��
        int applicationVersionFirstPart = (int)(Home.CURRENT_VERSION / 1000);
        int applicationMinimumVersionFirstPart = Integer.parseInt(applicationMinimumVersionParts [0]);        
        if (applicationVersionFirstPart > applicationMinimumVersionFirstPart) {
          return true;
        } else if (applicationVersionFirstPart == applicationMinimumVersionFirstPart 
                   && applicationMinimumVersionParts.length >= 2) { 
          // �ι��� �κ� ���� ��
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
   * ��밡���� �÷����� ���̺귯�� ��ȯ.
   */
  public List<Library> getPluginLibraries() {
    return Collections.unmodifiableList(new ArrayList<Library>(this.pluginLibraries.values()));
  }
  
  /**
   * �÷����� �ν��Ͻ��� ���� �Ұ����� ����Ʈ ��ȯ.
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
        // �� �÷����� Ŭ���� �ν��Ͻ�ȭ
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
        
        // ���� ���� �� �� ��� �÷����� �ı�
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

      // furnitureCatalogFile�� ���� �÷����� ������ ����
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
   * �÷����� �ν��Ͻ�ȭ�� �ʿ��� �Ӽ�
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
     * �Ű� ������ �÷����� �Ӽ� �ۼ�. 
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
