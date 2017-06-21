package com.eteks.homeview3d.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ExtensionsClassLoader extends ClassLoader {
  private final ProtectionDomain protectionDomain;
  private final String []        applicationPackages;

  private final Map    extensionDlls = new HashMap();
  private JarFile []   extensionJars = null;

  /**
   * 클래스 로더 작성
   */
  public ExtensionsClassLoader(ClassLoader parent, 
                               ProtectionDomain protectionDomain, 
                               String [] extensionJarsAndDlls,
                               String [] applicationPackages) {
    this(parent, protectionDomain, extensionJarsAndDlls, new URL [0], applicationPackages, null, null);
  }
  
  public ExtensionsClassLoader(ClassLoader parent, 
                               ProtectionDomain protectionDomain, 
                               String [] extensionJarAndDllResources,
                               URL [] extensionJarAndDllUrls,
                               String [] applicationPackages,
                               File cacheFolder,
                               String cachedFilesPrefix) {
    this(parent, protectionDomain, extensionJarAndDllResources, extensionJarAndDllUrls, applicationPackages, 
        cacheFolder, cachedFilesPrefix, false);
  }

  public ExtensionsClassLoader(ClassLoader parent, 
                               ProtectionDomain protectionDomain, 
                               String [] extensionJarAndDllResources,
                               URL [] extensionJarAndDllUrls,
                               String [] applicationPackages,
                               File cacheFolder,
                               String cachedFilesPrefix,
                               boolean cacheOnlyJars) {
    super(parent);
    this.protectionDomain = protectionDomain;
    this.applicationPackages = applicationPackages;
    String extensionPrefix = cachedFilesPrefix == null ? "" : cachedFilesPrefix;

    String dllSuffix;
    String dllPrefix;
    
    String osName = System.getProperty("os.name");
    if (osName.startsWith("Windows")) {
      dllSuffix = ".dll";
      dllPrefix = "";
    } else if (osName.startsWith("Mac OS X")) {
      dllSuffix = ".jnilib";
      dllPrefix = "lib";
    } else {
      dllSuffix = ".so";
      dllPrefix = "lib";
    }
    
    // URL만 포함하는 목록 생성
    ArrayList extensionJarsAndDlls = new ArrayList();
    for (int i = 0; i < extensionJarAndDllResources.length; i++) {
      URL extensionJarOrDllUrl = getResource(extensionJarAndDllResources [i]);
      if (extensionJarOrDllUrl != null) {
        extensionJarsAndDlls.add(extensionJarOrDllUrl);
      }
    }
    if (extensionJarAndDllUrls != null) {
      extensionJarsAndDlls.addAll(Arrays.asList(extensionJarAndDllUrls));
    }
    
    ArrayList extensionJars = new ArrayList();
    for (int i = 0; i < extensionJarsAndDlls.size(); i++) {
      URL extensionJarOrDllUrl = (URL)extensionJarsAndDlls.get(i);
      try {
        String extensionJarOrDllUrlFile = extensionJarOrDllUrl.getFile();
        URLConnection connection = null;
        long extensionJarOrDllFileDate;
        int  extensionJarOrDllFileLength;
        String extensionJarOrDllFile;
        if (extensionJarOrDllUrl.getProtocol().equals("jar")) {
          URL jarEntryUrl = new URL(extensionJarOrDllUrlFile.substring(0, extensionJarOrDllUrlFile.indexOf('!')));
          URLConnection jarEntryUrlConnection = jarEntryUrl.openConnection(); 
          extensionJarOrDllFileDate = jarEntryUrlConnection.getLastModified();
          extensionJarOrDllFileLength = jarEntryUrlConnection.getContentLength();
          extensionJarOrDllFile = extensionJarOrDllUrlFile.substring(extensionJarOrDllUrlFile.indexOf('!') + 2);
        } else {
          connection = extensionJarOrDllUrl.openConnection();
          extensionJarOrDllFileDate = connection.getLastModified();
          extensionJarOrDllFileLength = connection.getContentLength();
          extensionJarOrDllFile = extensionJarOrDllUrlFile;
        }
        int lastSlashIndex = extensionJarOrDllFile.lastIndexOf('/');
        String libraryName;
        boolean extensionJarFile = extensionJarOrDllFile.endsWith(".jar");
        if (extensionJarFile) {
          libraryName = null;
        } else if (extensionJarOrDllFile.endsWith(dllSuffix)) {
          libraryName = extensionJarOrDllFile.substring(lastSlashIndex + 1 + dllPrefix.length(),
              extensionJarOrDllFile.length() - dllSuffix.length()); 
        } else {
          // 다른 플랫폼의 DLL 무시
          continue;
        }
        
        if (cacheFolder != null 
            && (!cacheOnlyJars || extensionJarFile)
            && extensionJarOrDllFileDate != 0
            && extensionJarOrDllFileLength != -1
            && ((cacheFolder.exists()
                  && cacheFolder.isDirectory())
                || cacheFolder.mkdirs())) {
          try {
            String extensionJarOrDllFileName = extensionPrefix
                + extensionJarOrDllFileLength + "-"
                + (extensionJarOrDllFileDate / 1000L) + "-"
                + extensionJarOrDllFile.replace('/', '-');
            File cachedFile = new File(cacheFolder, extensionJarOrDllFileName);            
            if (!cachedFile.exists() 
                || cachedFile.lastModified() < extensionJarOrDllFileDate) {
              // jar을 캐시에 복사
              if (connection == null) {
                connection = extensionJarOrDllUrl.openConnection();
              }
              copyInputStreamToFile(connection.getInputStream(), cachedFile);
            }
            if (extensionJarFile) {
              // jars 목록에 tmp 파일 추가
              extensionJars.add(new JarFile(cachedFile.toString(), false));
            } else if (extensionJarOrDllFile.endsWith(dllSuffix)) {
              // DLL 맵에 tmp 파일 추가
              this.extensionDlls.put(libraryName, cachedFile.toString());
            }
            continue;
          } catch (IOException ex) {
          }          
        } 
        
        if (connection == null) {
          connection = extensionJarOrDllUrl.openConnection();
        }
        InputStream input = connection.getInputStream();          
        if (extensionJarFile) {
          // jar을 tmp 파일에 복사
          String extensionJar = copyInputStreamToTmpFile(input, ".jar");
          // jars 목록에 tmp 파일 추가
          extensionJars.add(new JarFile(extensionJar, false));
        } else if (extensionJarOrDllFile.endsWith(dllSuffix)) {
          // DLL을 tmp 파일에 복사
          String extensionDll = copyInputStreamToTmpFile(input, dllSuffix);
          // DLL 맵에 tmp 파일 추가
          this.extensionDlls.put(libraryName, extensionDll);
        }          
      } catch (IOException ex) {
        throw new RuntimeException("Couldn't extract extension " + extensionJarOrDllUrl, ex);
      }
    }
    
    // extensionJars 배열 생성
    if (extensionJars.size() > 0) {
      this.extensionJars = (JarFile [])extensionJars.toArray(new JarFile [extensionJars.size()]);                    
    }
  }

  private String copyInputStreamToTmpFile(InputStream input, 
                                          String suffix) throws IOException {
    File tmpFile = File.createTempFile("extension", suffix);
    tmpFile.deleteOnExit();
    copyInputStreamToFile(input, tmpFile);
    return tmpFile.toString();
  }

  public void copyInputStreamToFile(InputStream input, File file) throws FileNotFoundException, IOException {
    OutputStream output = null;
    try {
      output = new BufferedOutputStream(new FileOutputStream(file));
      byte [] buffer = new byte [8192];
      int size; 
      while ((size = input.read(buffer)) != -1) {
        output.write(buffer, 0, size);
      }
    } finally {
      if (input != null) {
        input.close();
      }
      if (output != null) {
        output.close();
      }
    }
  }
  
  protected Class findClass(String name) throws ClassNotFoundException {
    // 클래스 파일 생성
    String classFile = name.replace('.', '/') + ".class";
    InputStream classInputStream = null;
    if (this.extensionJars != null) {
      // 검색된 클래스가 확장 클래스인지 확인
      for (int i = 0; i < this.extensionJars.length; i++) {
        JarFile extensionJar = this.extensionJars [i];
        JarEntry jarEntry = extensionJar.getJarEntry(classFile);
        if (jarEntry != null) {
          try {
            classInputStream = extensionJar.getInputStream(jarEntry);
          } catch (IOException ex) {
            throw new ClassNotFoundException("Couldn't read class " + name, ex);
          }
        }
      }
    }
    if (classInputStream == null) {
      URL url = getResource(classFile);
      if (url == null) {
        throw new ClassNotFoundException("Class " + name);
      }
      try {
        classInputStream = url.openStream();
      } catch (IOException ex) {
        throw new ClassNotFoundException("Couldn't read class " + name, ex);
      }
    } 
    
    try {
      // 클래스 입력 내용 바이트로 읽음
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      BufferedInputStream in = new BufferedInputStream(classInputStream);
      byte [] buffer = new byte [8192];
      int size; 
      while ((size = in.read(buffer)) != -1) {
        out.write(buffer, 0, size);
      }
      in.close();
      // 클래스 정의
      return defineClass(name, out.toByteArray(), 0, out.size(), 
          this.protectionDomain);
    } catch (IOException ex) {
      throw new ClassNotFoundException("Class " + name, ex);
    }
  }
  
  protected String findLibrary(String libname) {
    return (String)this.extensionDlls.get(libname);
  }
 
  protected URL findResource(String name) {
    if (this.extensionJars != null) {
      // 추출된 jars 중 하나에 자원이 속했는지 확인
      for (int i = 0; i < this.extensionJars.length; i++) {
        JarFile extensionJar = this.extensionJars [i];
        JarEntry jarEntry = extensionJar.getJarEntry(name);        
        if (jarEntry != null) {
          try {
            return new URL("jar:file:" + extensionJar.getName() + "!/" + jarEntry.getName());
          } catch (MalformedURLException ex) {
          }
        }
      }
    }
    return super.findResource(name);
  }

  protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // 확장 jar을 찾을 수 없는 경우
    if (this.extensionJars == null) {
      // 기본 클래스 로더가 작업 수행하도록 허용
      return super.loadClass(name, resolve);
    }
    // 클래스가 이미 로드 되었는지 확인
    Class loadedClass = findLoadedClass(name);
    if (loadedClass == null) {
      try {
        // 클래스가 응용 프로그램 피키지 중 하나에 속하는지 확인
        for (int i = 0; i < this.applicationPackages.length; i++) {
          String applicationPackage = this.applicationPackages [i];
          int applicationPackageLength = applicationPackage.length();
          if (   (applicationPackageLength == 0 
                 && name.indexOf('.') == 0)
              || (applicationPackageLength > 0
                 && name.startsWith(applicationPackage))) {
            loadedClass = findClass(name);
            break;
          }
        }
      } catch (ClassNotFoundException ex) {
      }
      if (loadedClass == null) {
        loadedClass = super.loadClass(name, resolve);
      }
    }
    if (resolve) {
      resolveClass(loadedClass);
    }
    return loadedClass;
  }
}
