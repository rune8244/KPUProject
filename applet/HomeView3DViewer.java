package com.eteks.sweethome3d.applet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JApplet;
import javax.swing.JLabel;

import com.eteks.sweethome3d.tools.ExtensionsClassLoader;

public class HomeView3DViewer extends JApplet {
  private Object appletApplication;
  
  public void init() {
    if (!isJava5OrSuperior()) {
      showError(getLocalizedString("requirementsMessage"));
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
    System.gc();
  }

  private boolean isJava5OrSuperior() {
    String javaVersion = System.getProperty("java.version");
    String [] javaVersionParts = javaVersion.split("\\.|_");
    if (javaVersionParts.length >= 1) {
      try {
        // Return true for Java SE 5 and superior
        if (Integer.parseInt(javaVersionParts [1]) >= 5) {
          return true;
        }
      } catch (NumberFormatException ex) {
      }
    }
    return false;
  }

  private String getLocalizedString(String key) {
    Class HomeView3DViewerClass = HomeView3DViewer.class;
    return ResourceBundle.getBundle(HomeView3DViewerClass.getPackage().getName().replace('.', '/') + "/package").
        getString(HomeView3DViewerClass.getName().substring(HomeView3DViewerClass.getName().lastIndexOf('.') + 1) + "." + key);
  }
  
  /**
   * 주어진 텍스트 라벨 보여줌.
   */
  private void showError(String text) {
    JLabel label = new JLabel(text, JLabel.CENTER);
    setContentPane(label);
  }
  
  private void createAppletApplication() {
    try {
      Class HomeView3DViewerClass = HomeView3DViewer.class;
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
        System.setProperty("jogamp.gluegen.UseTempJarCache", "false");
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
          "javax.media.j3d",
          "javax.vecmath",
          "com.sun.j3d",
          "com.sun.opengl",
          "com.sun.gluegen.runtime",
          "com.jogamp",
          "jogamp",
          "javax.media.opengl",
          "javax.media.nativewindow",
          "com.microcrowd.loader.java3d",
          "org.apache.batik"}));
      
      ClassLoader extensionsClassLoader = new ExtensionsClassLoader(
          homeview3DViewerClass.getClassLoader(), homeView3DViewerClass.getProtectionDomain(),
          (String [])java3DFiles.toArray(new String [java3DFiles.size()]), 
          (String [])applicationPackages.toArray(new String [applicationPackages.size()]));
      
      String applicationClassName = "com.eteks.homeview3d.applet.ViewerHelper";
      Class applicationClass = extensionsClassLoader.loadClass(applicationClassName);
      Constructor applicationConstructor = 
          applicationClass.getConstructor(new Class [] {JApplet.class});
      this.appletApplication = applicationConstructor.newInstance(new Object [] {this});
    } catch (Throwable ex) {
      if (ex instanceof InvocationTargetException) {
        ex = ((InvocationTargetException)ex).getCause();
      }
      if (ex instanceof AccessControlException) {
        showError(getLocalizedString("signatureError"));
      } else {
        showError("<html>" + getLocalizedString("startError") 
            + "<br>Exception" + ex.getClass().getName() + " " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }  
}
