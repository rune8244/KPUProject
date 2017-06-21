package com.eteks.homeview3d.applet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JApplet;
import javax.swing.JLabel;

import com.eteks.homeview3d.tools.ExtensionsClassLoader;

public class homeview3DApplet extends JApplet {
  private Object appletApplication;
   
  public void init() {
    if (!isJava5OrSuperior()) {
      showText(getLocalizedString("requirementsMessage"));
    } else if (getCodeBase() != null
               && !getDocumentBase().getHost().equals(getCodeBase().getHost())) {
      showText(getLocalizedString("unauthorizedHostError"));
    } else {
      createAppletApplication();
    }
  }

  public void destroy() {
    if (this.appletApplication != null) {
      try {
        Method destroyMethod = this.appletApplication.getClass().getMethod("destroy", new Class [0]);
        destroyMethod.invoke(this.appletApplication, new Object [0]);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    this.appletApplication = null;
    // 삭제된 오브젝트 모으기
    System.gc();
  }
  

  public boolean isModified() {
    if (this.appletApplication != null) {
      try {
        Method destroyMethod = this.appletApplication.getClass().getMethod("isModified", new Class [0]);
        return ((Boolean)destroyMethod.invoke(this.appletApplication, new Object [0])).booleanValue();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return false;
  }
  
  /**
   * Returns <code>true</code> if current JVM version is 5+. 
   */
  private boolean isJava5OrSuperior() {
    String javaVersion = System.getProperty("java.version");
    String [] javaVersionParts = javaVersion.split("\\.|_");
    if (javaVersionParts.length >= 1) {
      try {
        if (Integer.parseInt(javaVersionParts [1]) >= 5) {
          return true;
        }
      } catch (NumberFormatException ex) {
      }
    }
    return false;
  }

  private String getLocalizedString(String key) {
    Class HomeView3DAppletClass = HomeView3DApplet.class;
    return ResourceBundle.getBundle(HomeView3DAppletClass.getPackage().getName().replace('.', '/') + "/package").
        getString(HomeView3DAppletClass.getName().substring(HomeView3DAppletClass.getName().lastIndexOf('.') + 1) + "." + key);
  }
  
  /**
   * 텍스트 라벨 보여줌
   */
  private void showText(String text) {
    JLabel label = new JLabel(text, JLabel.CENTER);
    setContentPane(label);
  }
  
  /**
   * 주어진 익셉션 보고.
   */
  private void showError(Throwable ex) {
    showText("<html>" + getLocalizedString("startError") 
        + "<br>Exception " + ex.getClass().getName() 
        + (ex.getMessage() != null ? " " + ex.getMessage() : ""));
    ex.printStackTrace();
  }


  private void createAppletApplication() {
    String applicationClassName = null;
    try {
      applicationClassName = getApplicationClassName();
      Class HomeView3DAppletClass = HomeView3DApplet.class;
      List java3DFiles = new ArrayList();
      if (System.getProperty("os.name").startsWith("Mac OS X")
          && System.getProperty("java.version").startsWith("1.5")) {
        java3DFiles.addAll(Arrays.asList(new String [] {
            "j3dcore.jar", // Main Java 3D jars
            "vecmath.jar",
            "j3dutils.jar",
            "macosx/gluegen-rt.jar", // Mac OS X jars and DLLs
            "macosx/jogl.jar",
            "macosx/libgluegen-rt.jnilib",
            "macosx/libjogl.jnilib",
            "macosx/libjogl_awt.jnilib",
            "macosx/libjogl_cg.jnilib"}));
      } else {
        java3DFiles.addAll(Arrays.asList(new String [] {
            "java3d-1.6/j3dcore.jar", // Mac OS X Java 3D 1.6 jars and DLLs
            "java3d-1.6/vecmath.jar",
            "java3d-1.6/j3dutils.jar",
            "java3d-1.6/gluegen-rt.jar", 
            "java3d-1.6/jogl-java3d.jar",
            "java3d-1.6/macosx/libgluegen-rt.jnilib",
            "java3d-1.6/macosx/libjogl_desktop.jnilib",
            "java3d-1.6/macosx/libnativewindow_awt.jnilib",
            "java3d-1.6/macosx/libnativewindow_macosx.jnilib"}));
        try {
          System.setProperty("jogamp.gluegen.UseTempJarCache", "false");
          System.setProperty("com.eteks.homeview3d.j3d.useOffScreen3DView", "true");
        } catch (AccessControlException ex) {
        }
      }
      if ("64".equals(System.getProperty("sun.arch.data.model"))) {
        java3DFiles.addAll(Arrays.asList(new String [] {
          "java3d-1.6/linux/amd64/libgluegen-rt.so", // Linux 64 bits DLLs for Java 3D 1.6
          "java3d-1.6/linux/amd64/libjogl_desktop.so",
          "java3d-1.6/linux/amd64/libnativewindow_awt.so",
          "java3d-1.6/linux/amd64/libnativewindow_x11.so",
          "java3d-1.6/windows/amd64/gluegen-rt.dll", // Windows 64 bits DLLs for Java 3D 1.6
          "java3d-1.6/windows/amd64/jogl_desktop.dll",
          "java3d-1.6/windows/amd64/nativewindow_awt.dll",
          "java3d-1.6/windows/amd64/nativewindow_win32.dll"}));
      } else {
        java3DFiles.addAll(Arrays.asList(new String [] {
          "java3d-1.6/linux/i586/libgluegen-rt.so", // Linux 32 bits DLLs for Java 3D 1.6
          "java3d-1.6/linux/i586/libjogl_desktop.so",
          "java3d-1.6/linux/i586/libnativewindow_awt.so",
          "java3d-1.6/linux/i586/libnativewindow_x11.so",
          "java3d-1.6/windows/i586/gluegen-rt.dll", // Windows 32 bits DLLs for Java 3D 1.6
          "java3d-1.6/windows/i586/jogl_desktop.dll",
          "java3d-1.6/windows/i586/nativewindow_awt.dll",
          "java3d-1.6/windows/i586/nativewindow_win32.dll"}));
      }
      
      List applicationPackages = new ArrayList(Arrays.asList(new String [] {
          "com.eteks.homeview3d",
          "javax.media",
          "javax.vecmath",
          "com.sun.j3d",
          "com.sun.opengl",
          "com.sun.gluegen.runtime",
          "com.jogamp",
          "jogamp",
          "javax.media.opengl",
          "javax.media.nativewindow",
          "com.sun.media",
          "com.ibm.media",
          "jmpapps.util",
          "com.microcrowd.loader.java3d",
          "org.sunflow",
          "org.apache.batik"}));
      applicationPackages.addAll(getPluginsPackages());
      
      if (!applicationClassName.startsWith((String)applicationPackages.get(0))) {
        String [] applicationClassParts = applicationClassName.split("\\.");
        String applicationClassPackageBase = ""; 
        for (int i = 0, n = Math.min(applicationClassParts.length - 1, 2); i < n; i++) {
          if (i > 0) {
            applicationClassPackageBase += ".";
          }
          applicationClassPackageBase += applicationClassParts [i];
        }
        applicationPackages.add(applicationClassPackageBase);
      }
      
      ClassLoader extensionsClassLoader = System.getProperty("os.name").startsWith("Windows")
          ? new ExtensionsClassLoader(
              homeview3DAppletClass.getClassLoader(), homeView3DAppletClass.getProtectionDomain(),
              (String [])java3DFiles.toArray(new String [java3DFiles.size()]), null, 
              new File(System.getProperty("java.io.tmpdir")), applicationClassName + "-cache-", true)  
          : new ExtensionsClassLoader(
              homeview3DAppletClass.getClassLoader(), homeview3DAppletClass.getProtectionDomain(),
              (String [])java3DFiles.toArray(new String [java3DFiles.size()]), 
              (String [])applicationPackages.toArray(new String [applicationPackages.size()]));
      startApplication(applicationClassName, extensionsClassLoader);
    } catch (AccessControlException ex) {
      String runWithoutSignature = getParameter("runWithoutSignature");
      if (runWithoutSignature != null && Boolean.parseBoolean(runWithoutSignature)) {
        // 3D 어플리케이션 없이 돌리기
        startApplication(applicationClassName, getClass().getClassLoader());
      } else {
        showText(getLocalizedString("signatureError"));
      }
    } catch (Throwable ex) {
      showError(ex);
    }
  }

  private void startApplication(String applicationClassName, ClassLoader extensionsClassLoader) {
    try {
      Class applicationClass = extensionsClassLoader.loadClass(applicationClassName);
      Constructor applicationConstructor = applicationClass.getConstructor(new Class [] {JApplet.class});
      this.appletApplication = applicationConstructor.newInstance(new Object [] {this});
    } catch (Exception ex) {
      showError(ex);
    }
  }


  protected String getApplicationClassName() {
    return "com.eteks.homeview3d.applet.AppletApplication";
  }  
  
  /**
   * 어플리케이션 인스턴스 값으로 돌아감. 
   */
  protected Object getApplication() {
    return this.appletApplication;
  }  
  
  /**
   * 패키지 콜렉션으로 돌아감. 
   */
  private Collection getPluginsPackages() {
    String pluginURLs = getParameter("pluginURLs");
    if (pluginURLs != null) {        
      Set pluginPackages = new HashSet();
      String [] urlStrings = pluginURLs.split("\\s|,");
      for (int i = 0; i < urlStrings.length; i++) {
        try {
          URL pluginUrl = new URL(getCodeBase(), urlStrings [i]);
          ZipInputStream zipIn = null;
          try {
            // zip입력 열기
            zipIn = new ZipInputStream(pluginUrl.openStream());
            for (ZipEntry entry; (entry = zipIn.getNextEntry()) != null; ) {
              String zipEntryName = entry.getName();
              int lastIndex = zipEntryName.lastIndexOf('/');
              if (zipEntryName.endsWith(".class")) {
                if (lastIndex == -1) {
                  pluginPackages.add(""); 
                } else {
                  pluginPackages.add(zipEntryName.substring(0, lastIndex).replace('/', '.'));
                }
              }
            }
          } catch (IOException ex) {
          } finally {
            if (zipIn != null) {
              try {
                zipIn.close();
              } catch (IOException ex) {
              }
            }
          }
        } catch (MalformedURLException ex) {
        }
      }
      return pluginPackages;
    }
    return Collections.EMPTY_SET;
  }
}
