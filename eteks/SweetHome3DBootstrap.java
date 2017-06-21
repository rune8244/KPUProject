package com.eteks.homeview3d;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.JOptionPane;

import com.eteks.homeview3d.tools.ExtensionsClassLoader;

public class HomeView3DBootstrap {
  public static void main(String [] args) throws MalformedURLException, IllegalAccessException, 
        InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
    Class homeview3DBootstrapClass = HomeView3DBootstrap.class;
    List<String> extensionJarsAndDlls = new ArrayList<String>(Arrays.asList(new String [] {
        "iText-2.1.7.jar", 
        "freehep-vectorgraphics-svg-2.1.1b.jar",
        "sunflow-0.07.3i.jar",
        "jmf.jar",
        "batik-svgpathparser-1.7.jar",
        "jnlp.jar"}));
    
    String operatingSystemName = System.getProperty("os.name");
    String javaVersion = System.getProperty("java.version");
    String java7Prefix = "1.7.0_";
    if (operatingSystemName.startsWith("Mac OS X")) {
      if (javaVersion.startsWith("1.6")
          && System.getProperty("com.eteks.homeview3d.deploymentInformation", "").startsWith("Java Web Start")) {
        String message = Locale.getDefault().getLanguage().equals(Locale.FRENCH.getLanguage())
            ? "Home View 3D ne peut pas fonctionner avec Java\n"
            + "Web Start 6 sous Mac OS X de fa�on fiable.\n" 
            + "Merci de t�l�charger le programme d'installation depuis\n" 
            + "http://www.homeview3d.com/fr/download.jsp"
            : "Home View 3D can't reliably run with Java Web Start 6\n" 
            + "under Mac OS X.\n" 
            + "Please download the installer version from\n" 
            + "http://www.homeview3d.com/download.jsp";
        JOptionPane.showMessageDialog(null, message);
        System.exit(1);
      } else if (javaVersion.startsWith("1.5")
          || javaVersion.startsWith("1.6")) {
        extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
            "j3dcore.jar", 
            "vecmath.jar",
            "j3dutils.jar",
            "macosx/gluegen-rt.jar",
            "macosx/jogl.jar",
            "macosx/libgluegen-rt.jnilib",
            "macosx/libjogl.jnilib",
            "macosx/libjogl_awt.jnilib",
            "macosx/libjogl_cg.jnilib"}));
      } else if (javaVersion.startsWith(java7Prefix)
          && javaVersion.length() >= java7Prefix.length() + 1
          && Character.isDigit(javaVersion.charAt(java7Prefix.length()))
          && (javaVersion.length() >= java7Prefix.length() + 2
          && Character.isDigit(javaVersion.charAt(java7Prefix.length() + 1))
          && Integer.parseInt(javaVersion.substring(java7Prefix.length(), java7Prefix.length() + 2)) < 40
          || javaVersion.length() == java7Prefix.length() + 1 
          || !Character.isDigit(javaVersion.charAt(java7Prefix.length() + 1)))) {
        String message = Locale.getDefault().getLanguage().equals(Locale.FRENCH.getLanguage())
            ? "Sous Mac OS X, Home View 3D ne peut fonctionner avec Java 7\n" 
            + "qu'� partir de la version Java 7u40. Merci de mettre � jour\n" 
            + "votre version de Java ou de lancer Home View 3D sous Java 6."
            : "Under Mac OS X, Home View 3D can run with Java 7 only\n" 
            + "from version Java 7u40. Please, update you Java version\n" 
            + "or run Home View 3D under Java 6.";
        JOptionPane.showMessageDialog(null, message);
        System.exit(1);
      } else { 
        extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
            "java3d-1.6/j3dcore.jar", 
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
    } else {
      if ("1.5.2".equals(System.getProperty("com.eteks.homeview3d.j3d.version", "1.6"))
          || "d3d".equals(System.getProperty("j3d.rend", "jogl"))) {
        extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
            "j3dcore.jar",
            "vecmath.jar",
            "j3dutils.jar"}));
        if ("64".equals(System.getProperty("sun.arch.data.model"))) {
          extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
              "linux/x64/libj3dcore-ogl.so",    
              "windows/x64/j3dcore-ogl.dll"})); 
        } else {
          extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
              "linux/i386/libj3dcore-ogl.so", 
              "linux/i386/libj3dcore-ogl-cg.so", 
              "windows/i386/j3dcore-d3d.dll", 
              "windows/i386/j3dcore-ogl.dll",
              "windows/i386/j3dcore-ogl-cg.dll",
              "windows/i386/j3dcore-ogl-chk.dll"}));
        }
      } else {
        extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
            "java3d-1.6/j3dcore.jar",
            "java3d-1.6/vecmath.jar",
            "java3d-1.6/j3dutils.jar",
            "java3d-1.6/gluegen-rt.jar", 
            "java3d-1.6/jogl-java3d.jar"}));
        System.setProperty("jogamp.gluegen.UseTempJarCache", "false");
        if ("64".equals(System.getProperty("sun.arch.data.model"))) {
          extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
              "java3d-1.6/linux/amd64/libgluegen-rt.so",
              "java3d-1.6/linux/amd64/libjogl_desktop.so",
              "java3d-1.6/linux/amd64/libnativewindow_awt.so",
              "java3d-1.6/linux/amd64/libnativewindow_x11.so",
              "java3d-1.6/windows/amd64/gluegen-rt.dll", 
              "java3d-1.6/windows/amd64/jogl_desktop.dll",
              "java3d-1.6/windows/amd64/nativewindow_awt.dll",
              "java3d-1.6/windows/amd64/nativewindow_win32.dll"}));
        } else {
          extensionJarsAndDlls.addAll(Arrays.asList(new String [] {
              "java3d-1.6/linux/i586/libgluegen-rt.so",
              "java3d-1.6/linux/i586/libjogl_desktop.so",
              "java3d-1.6/linux/i586/libnativewindow_awt.so",
              "java3d-1.6/linux/i586/libnativewindow_x11.so",
              "java3d-1.6/windows/i586/gluegen-rt.dll", 
              "java3d-1.6/windows/i586/jogl_desktop.dll",
              "java3d-1.6/windows/i586/nativewindow_awt.dll",
              "java3d-1.6/windows/i586/nativewindow_win32.dll"}));
        }
      }
    }
    
    String [] applicationPackages = {
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
        "org.apache.batik"};
    String applicationClassName = "com.eteks.homeview3d.HomeView3D";
    ClassLoader java3DClassLoader = operatingSystemName.startsWith("Windows")
        ? new ExtensionsClassLoader(
            homeview3DBootstrapClass.getClassLoader(), 
            homeview3DBootstrapClass.getProtectionDomain(),
            extensionJarsAndDlls.toArray(new String [extensionJarsAndDlls.size()]), null, applicationPackages,
            new File(System.getProperty("java.io.tmpdir")), applicationClassName + "-cache-")  
        : new ExtensionsClassLoader(
            homeview3DBootstrapClass.getClassLoader(), 
            homeview3DBootstrapClass.getProtectionDomain(),
            extensionJarsAndDlls.toArray(new String [extensionJarsAndDlls.size()]), applicationPackages);      
    Class applicationClass = java3DClassLoader.loadClass(applicationClassName);
    Method applicationClassMain = 
        applicationClass.getMethod("main", Array.newInstance(String.class, 0).getClass());
    applicationClassMain.invoke(null, new Object [] {args});
  }
}