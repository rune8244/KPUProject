package com.eteks.homeview3d.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.apple.eio.FileManager;
import com.eteks.homeview3d.model.Home;

public class OperatingSystem {
  private static final String EDITOR_SUB_FOLDER; 
  private static final String APPLICATION_SUB_FOLDER;
  private static final String TEMPORARY_SUB_FOLDER;
  private static final String TEMPORARY_SESSION_SUB_FOLDER;
  
  static {
    // 응용 프로그램 데이터 저장된 하위 폴더 검색
    ResourceBundle resource = ResourceBundle.getBundle(OperatingSystem.class.getName());
    if (OperatingSystem.isMacOSX()) {
      EDITOR_SUB_FOLDER = resource.getString("editorSubFolder.Mac OS X");
      APPLICATION_SUB_FOLDER = resource.getString("applicationSubFolder.Mac OS X");
    } else if (OperatingSystem.isWindows()) {
      EDITOR_SUB_FOLDER = resource.getString("editorSubFolder.Windows");
      APPLICATION_SUB_FOLDER = resource.getString("applicationSubFolder.Windows");
    } else {
      EDITOR_SUB_FOLDER = resource.getString("editorSubFolder");
      APPLICATION_SUB_FOLDER = resource.getString("applicationSubFolder");
    }
    
    String temporarySubFolder;
    try {
      temporarySubFolder = resource.getString("temporarySubFolder");
      if (temporarySubFolder.trim().length() == 0) {
        temporarySubFolder = null;
      }
    } catch (MissingResourceException ex) {
      temporarySubFolder = "work";
    }
    try {
      temporarySubFolder = System.getProperty(
          "com.eteks.homeview3d.tools.temporarySubFolder", temporarySubFolder);
    } catch (AccessControlException ex) {
    }
    TEMPORARY_SUB_FOLDER = temporarySubFolder;
    TEMPORARY_SESSION_SUB_FOLDER = UUID.randomUUID().toString();
  }
 
  // 이 클래스는 정적 메소드만 포함
  private OperatingSystem() {    
  }

  public static boolean isLinux() {
    return System.getProperty("os.name").startsWith("Linux");
  }

  public static boolean isWindows() {
    return System.getProperty("os.name").startsWith("Windows");
  }

  public static boolean isMacOSX() {
    return System.getProperty("os.name").startsWith("Mac OS X");
  }

  public static boolean isMacOSXLeopardOrSuperior() {
    return isMacOSX()
        && !System.getProperty("os.version").startsWith("10.4");
  }

  public static boolean isMacOSXLionOrSuperior() {
    return isMacOSX()
        && compareVersions(System.getProperty("os.version"), "10.7") >= 0;
  }

  public static boolean isMacOSXYosemiteOrSuperior() {
    return isMacOSX()
        && compareVersions(System.getProperty("os.version"), "10.10") >= 0;
  }

  public static boolean isJavaVersionGreaterOrEqual(String javaMinimumVersion) {
    return compareVersions(javaMinimumVersion, getComparableJavaVersion()) <= 0;
  }

  public static boolean isJavaVersionBetween(String javaMinimumVersion, String javaMaximumVersion) {
    String javaVersion = getComparableJavaVersion();
    return compareVersions(javaMinimumVersion, javaVersion) <= 0 
        && compareVersions(javaVersion, javaMaximumVersion) < 0;
  }

  private static String getComparableJavaVersion() {
    String javaVersion = System.getProperty("java.version");
    try {
      if ("OpenJDK Runtime Environment".equals(System.getProperty("java.runtime.name"))) {
        javaVersion = javaVersion.replace("-u", "_");
      }
    } catch (AccessControlException ex) {

    }
    return javaVersion;
  }

  public static int compareVersions(String version1, String version2) {
    List<Object> version1Parts = splitVersion(version1);
    List<Object> version2Parts = splitVersion(version2);
    int i = 0;
    for ( ; i < version1Parts.size() || i < version2Parts.size(); i++) {
      Object version1Part = i < version1Parts.size() 
          ? convertPreReleaseVersion(version1Parts.get(i))
          : BigInteger.ZERO; // 누락된 부분은 0으로 간주
      Object version2Part = i < version2Parts.size() 
          ? convertPreReleaseVersion(version2Parts.get(i))
          : BigInteger.ZERO;
      if (version1Part.getClass() == version2Part.getClass()) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        int comparison = ((Comparable)version1Part).compareTo(version2Part);
        if (comparison != 0) {
          return comparison;
        }
      } else if (version1Part instanceof String) {
        // 정수 > 문자열
        return 1;
      } else {
        // 문자열 > 정수
        return -1;
      }
    }
    return 0;
  }

  private static List<Object> splitVersion(String version) {
    List<Object> versionParts = new ArrayList<Object>();
    StringBuilder subPart = new StringBuilder();
    // 첫 번째 분할 버전
    for (String part : version.split("\\p{Punct}|\\s")) {
      for (int i = 0; i < part.length(); ) {
        subPart.setLength(0);
        char c = part.charAt(i);
        if (Character.isDigit(c)) {
          for ( ; i < part.length() && Character.isDigit(c = part.charAt(i)); i++) {
            subPart.append(c);
          }
          versionParts.add(new BigInteger(subPart.toString()));
        } else {
          for ( ; i < part.length() && !Character.isDigit(c = part.charAt(i)); i++) {
            subPart.append(c);
          }
          versionParts.add(subPart.toString());
        }  
      }
    }    
    return versionParts;
  }

  private static Object convertPreReleaseVersion(Object versionPart) {
    if (versionPart instanceof String) {
      String versionPartString = (String)versionPart;
      if ("alpha".equalsIgnoreCase(versionPartString)) {
        return new BigInteger("-3");
      } else if ("beta".equalsIgnoreCase(versionPartString)) {
        return new BigInteger("-2");
      } else if ("rc".equalsIgnoreCase(versionPartString)) {
        return new BigInteger("-1");
      }
    }
    return versionPart;
  }

  public static File createTemporaryFile(String prefix, String suffix) throws IOException {
    File temporaryFolder;
    try {
      temporaryFolder = getDefaultTemporaryFolder(true);
    } catch (IOException ex) {
      // 기본 임시 폴더 생성에 실패하면 기본 임시 파일 폴더 사용
      temporaryFolder = null;
    }
    File temporaryFile = File.createTempFile(prefix, suffix, temporaryFolder);
    temporaryFile.deleteOnExit();
    return temporaryFile;
  }
  

  public static Comparator<File> getFileVersionComparator() {
    return new Comparator<File>() {
        public int compare(File file1, File file2) {
          String fileName1 = file1.getName();
          String fileName2 = file2.getName();
          int extension1Index = fileName1.lastIndexOf('.');
          String extension1 = extension1Index != -1  ? fileName1.substring(extension1Index)  : null;
          int extension2Index = fileName2.lastIndexOf('.');
          String extension2 = extension2Index != -1  ? fileName2.substring(extension2Index)  : null;
          // If the files have the same extension, remove it 
          if (extension1 != null && extension1.equals(extension2)) {
            fileName1 = fileName1.substring(0, extension1Index);
            fileName2 = fileName2.substring(0, extension2Index);
          }
          return OperatingSystem.compareVersions(fileName1, fileName2);
        }
      };
  }

  public static void deleteTemporaryFiles() {
    try {
      File temporaryFolder = getDefaultTemporaryFolder(false);
      if (temporaryFolder != null) {
        for (File temporaryFile : temporaryFolder.listFiles()) {
          temporaryFile.delete();
        }
        temporaryFolder.delete();
      }
    } catch (IOException ex) {
      // 찾을 수 없는 임시 폴더 무시
    } catch (AccessControlException ex) {
    }
  }

  private synchronized static File getDefaultTemporaryFolder(boolean create) throws IOException {
    if (TEMPORARY_SUB_FOLDER != null) {
      File temporaryFolder;
      if (new File(TEMPORARY_SUB_FOLDER).isAbsolute()) {
        temporaryFolder = new File(TEMPORARY_SUB_FOLDER);
      } else {
        temporaryFolder = new File(getDefaultApplicationFolder(), TEMPORARY_SUB_FOLDER);
      }
      final String versionPrefix = Home.CURRENT_VERSION + "-";
      final File sessionTemporaryFolder = new File(temporaryFolder, 
          versionPrefix + TEMPORARY_SESSION_SUB_FOLDER);      
      if (!sessionTemporaryFolder.exists()) {
        // 동일한 버전으로 작업하는 기존 폴더를 임시 폴더에서 검색
        final File [] siblingTemporaryFolders = temporaryFolder.listFiles(new FileFilter() {
            public boolean accept(File file) {
              return file.isDirectory() 
                  && file.getName().startsWith(versionPrefix);
            }
          });
        
        // 임시 폴더 생성 
        if (!createTemporaryFolders(sessionTemporaryFolder)) {
          throw new IOException("Can't create temporary folder " + sessionTemporaryFolder);
        }
        
        // 임시 폴더의 수정 날짜 업데이트하는 타이머 
        final long updateDelay = 60000;
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
              sessionTemporaryFolder.setLastModified(Math.max(System.currentTimeMillis(),
                  sessionTemporaryFolder.lastModified() + updateDelay));
            }
          }, updateDelay, updateDelay);
        
        if (siblingTemporaryFolders != null
            && siblingTemporaryFolders.length > 0) {
          // 임시 폴더에서 일주일 이상 지난 것 삭제하는 타이머 실행
          final long deleteDelay = 10 * 60000;
          final long age = 7 * 24 * 3600000;
          new Timer(true).schedule(new TimerTask() {
              @Override
              public void run() {
                long now = System.currentTimeMillis();
                for (File siblingTemporaryFolder : siblingTemporaryFolders) {
                  if (siblingTemporaryFolder.exists()
                      && now - siblingTemporaryFolder.lastModified() > age) {
                    File [] temporaryFiles = siblingTemporaryFolder.listFiles();
                    for (File temporaryFile : temporaryFiles) {
                      temporaryFile.delete();
                    }
                    siblingTemporaryFolder.delete();
                  }
                }
              }
            }, deleteDelay);
        }
      }
      return sessionTemporaryFolder;
    } else {
      return null;
    }
  }

  private static boolean createTemporaryFolders(File temporaryFolder) {
    if (temporaryFolder.exists()) {
      return false;
    }
    if (temporaryFolder.mkdir()) {
      temporaryFolder.deleteOnExit();
      return true;
    }
    File canonicalFile = null;
    try {
      canonicalFile = temporaryFolder.getCanonicalFile();
    } catch (IOException e) {
      return false;
    }
    File parent = canonicalFile.getParentFile();
    if (parent != null 
        && (createTemporaryFolders(parent) || parent.exists()) 
        && canonicalFile.mkdir()) {
      temporaryFolder.deleteOnExit();
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * 기본 응용 프로그램 폴더 반환. 
   */
  public static File getDefaultApplicationFolder() throws IOException {
    File userApplicationFolder; 
    if (isMacOSX()) {
      userApplicationFolder = new File(MacOSXFileManager.getApplicationSupportFolder());
    } else if (isWindows()) {
      userApplicationFolder = new File(System.getProperty("user.home"), "Application Data");
      if (!userApplicationFolder.exists()) {
        userApplicationFolder = new File(System.getProperty("user.home"));
      }
    } else { 
      userApplicationFolder = new File(System.getProperty("user.home"));
    }
    return new File(userApplicationFolder, 
        EDITOR_SUB_FOLDER + File.separator + APPLICATION_SUB_FOLDER);
  }

  private static class MacOSXFileManager {
    public static String getApplicationSupportFolder() throws IOException {
      return FileManager.findFolder((short)-32763, 0x61737570);
    }
  }
}
