package com.eteks.homeview3d.io;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeApplication;
import com.eteks.homeview3d.model.HomeRecorder;
import com.eteks.homeview3d.model.InterruptedRecorderException;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.model.UserPreferences.Property;
import com.eteks.homeview3d.tools.OperatingSystem;

public class AutoRecoveryManager {
  private static final int    MINIMUM_DELAY_BETWEEN_AUTO_SAVE_OPERATIONS = 30000;
  private static final String RECOVERY_SUB_FOLDER      = "recovery";
  private static final String RECOVERED_FILE_EXTENSION = ".recovered";
  private static final String UNRECOVERABLE_FILE_EXTENSION = ".unrecoverable";

  private final HomeApplication             application;
  private final List<Home>                  recoveredHomes      = new ArrayList<Home>();
  private final Map<Home, File>             autoSavedFiles      = new HashMap<Home, File>();
  private final Map<File, FileOutputStream> lockedOutputStreams = new HashMap<File, FileOutputStream>();
  private final ExecutorService             autoSaveForRecoveryExecutor;
  private Timer                             timer;
  private long                              lastAutoSaveTime;

  public AutoRecoveryManager(HomeApplication application) throws RecorderException {
    this.application = application;
    this.autoSaveForRecoveryExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable runnable) {
          Thread thread = new Thread(runnable);
          thread.setPriority(Thread.MIN_PRIORITY);
          return thread;
        }
      });
    
    readRecoveredHomes();
    
    // 자동저장 인터럽트
    Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          autoSaveForRecoveryExecutor.shutdownNow();
        }
      });
    
    // 프로그램 꺼지면 자동저장 삭제
    application.addHomesListener(new CollectionListener<Home>() {
        public void collectionChanged(CollectionEvent<Home> ev) {
          if (ev.getType() == CollectionEvent.Type.DELETE) {
            final Home home = ev.getItem();
            autoSaveForRecoveryExecutor.submit(new Runnable() {
                public void run() {
                  try {
                    final File homeFile = autoSavedFiles.get(home);
                    if (homeFile != null) {
                      freeLockedFile(homeFile);
                      homeFile.delete();
                      autoSavedFiles.remove(home);
                    }
                  } catch (RecorderException ex) {
                  }
                }
              });
          }
        }
      });
    
    application.getUserPreferences().addPropertyChangeListener(Property.AUTO_SAVE_DELAY_FOR_RECOVERY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          restartTimer();
        }
      });
    restartTimer();
  }

  /**
   * 홈 복구 읽기.
   */
  private void readRecoveredHomes() throws RecorderException {
    File recoveryFolder = getRecoveryFolder();
    File [] recoveredFiles = recoveryFolder.listFiles(new FileFilter() {
        public boolean accept(File file) {
          return file.isFile()
              && file.getName().endsWith(RECOVERED_FILE_EXTENSION);
        }
      });
    if (recoveredFiles != null) {
      Arrays.sort(recoveredFiles, new Comparator<File>() {
          public int compare(File f1, File f2) {
            if (f1.lastModified() < f2.lastModified()) {
              return 1;
            } else {
              return -1;
            }
          }
        });
      for (final File file : recoveredFiles) {
        if (!isFileLocked(file)) {
          try {
            final Home home = this.application.getHomeRecorder().readHome(file.getPath());
            if (home.getName() == null 
                || !file.equals(new File(home.getName()))) {
              home.setRecovered(true);
              home.addPropertyChangeListener(Home.Property.RECOVERED, new PropertyChangeListener() {
                  public void propertyChange(PropertyChangeEvent evt) {
                    if (!home.isRecovered()) {
                      file.delete();
                    }
                  }
                });
              this.recoveredHomes.add(home);
            }
          } catch (RecorderException ex) {
            ex.printStackTrace();
            // 재 명명
            file.renameTo(new File(recoveryFolder, 
                file.getName().replace(RECOVERED_FILE_EXTENSION, UNRECOVERABLE_FILE_EXTENSION)));
          }
        }
      }
    }
  }

  private boolean isFileLocked(final File file) {
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(file, true); 
      return out.getChannel().tryLock() == null;
    } catch (IOException ex) {
      return true;
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ex) {
          return true;
        }
      }
    }
  }

  /**
   * 복구된 프로그램 열기. 
   */
  public void openRecoveredHomes() {
    for (Home recoveredHome : this.recoveredHomes) {
      boolean recoveredHomeOpen = false;
      for (Home home : this.application.getHomes()) {
        if (home.getName() != null
            && home.getName().equals(recoveredHome.getName())) {
          recoveredHome.setName(null);
          this.application.addHome(recoveredHome);
          recoveredHomeOpen = true;
          break;
        }
      }
      if (!recoveredHomeOpen) {
        this.application.addHome(recoveredHome);
      }
    }
    this.recoveredHomes.clear();
  }
  
  /**
   * 타이머 재시작. 
   */
  private void restartTimer() {
    if (this.timer != null) {
      this.timer.cancel();
      this.timer = null;
    }
    int autoSaveDelayForRecovery = this.application.getUserPreferences().getAutoSaveDelayForRecovery();
    if (autoSaveDelayForRecovery > 0) {
      this.timer = new Timer("autoSaveTimer", true);
      TimerTask task = new TimerTask() {
        @Override
        public void run() {
          if (System.currentTimeMillis() - lastAutoSaveTime > MINIMUM_DELAY_BETWEEN_AUTO_SAVE_OPERATIONS) {
            cloneAndSaveHomes();
          }
        }
      };
      this.timer.scheduleAtFixedRate(task, autoSaveDelayForRecovery, autoSaveDelayForRecovery);
    }
  }

  private void cloneAndSaveHomes() {
    try {
      EventQueue.invokeAndWait(new Runnable() {
          public void run() {
            for (final Home home : application.getHomes()) {
              final Home autoSavedHome = home.clone();
              final HomeRecorder homeRecorder = application.getHomeRecorder();
              autoSaveForRecoveryExecutor.submit(new Runnable() {
                public void run() {
                  try {
                    saveHome(home, autoSavedHome, homeRecorder);
                  } catch (RecorderException ex) {
                    ex.printStackTrace();
                  }
                }
              });
            }
          }
        });
    } catch (InvocationTargetException ex) {
      throw new RuntimeException(ex);
    } catch (InterruptedException ex) {
    }
  }

  private void saveHome(Home home, Home autoSavedHome, HomeRecorder homeRecorder) throws RecorderException {
    File autoSavedHomeFile = this.autoSavedFiles.get(home);
    if (autoSavedHomeFile == null) {
      File recoveredFilesFolder = getRecoveryFolder();
      if (!recoveredFilesFolder.exists()) {
        if (!recoveredFilesFolder.mkdirs()) {
          throw new RecorderException("Can't create folder " + recoveredFilesFolder + " to store recovered files");
        }
      }
      if (autoSavedHome.getName() != null) {
        String homeFile = new File(autoSavedHome.getName()).getName();
        autoSavedHomeFile = new File(recoveredFilesFolder, homeFile + RECOVERED_FILE_EXTENSION);
        if (autoSavedHomeFile.exists()) {
          autoSavedHomeFile = new File(recoveredFilesFolder, 
              UUID.randomUUID() + "-" + homeFile + RECOVERED_FILE_EXTENSION);
        }
      } else {
        autoSavedHomeFile = new File(recoveredFilesFolder,
            UUID.randomUUID() + RECOVERED_FILE_EXTENSION);
      }
    }
    freeLockedFile(autoSavedHomeFile);        
    if (autoSavedHome.isModified()) {
      this.autoSavedFiles.put(home, autoSavedHomeFile);
      try {
        homeRecorder.writeHome(autoSavedHome, autoSavedHomeFile.getPath());
        
        FileOutputStream lockedOutputStream = null;
        try {
          lockedOutputStream = new FileOutputStream(autoSavedHomeFile, true);
          lockedOutputStream.getChannel().lock();
          this.lockedOutputStreams.put(autoSavedHomeFile, lockedOutputStream);
        } catch (OverlappingFileLockException ex) {
        } catch (IOException ex) {
          if (lockedOutputStream != null) {
            try {
              lockedOutputStream.close();
            } catch (IOException ex1) {
            }
          }
          throw new RecorderException("Can't lock saved home", ex);            
        }
      } catch (InterruptedRecorderException ex) {
        // 익셉션 지우기
      } 
    } else {
      autoSavedHomeFile.delete();
      this.autoSavedFiles.remove(home);
    }
    this.lastAutoSaveTime = Math.max(this.lastAutoSaveTime, System.currentTimeMillis());
  }

  private void freeLockedFile(File file) throws RecorderException {
    FileOutputStream lockedOutputStream = this.lockedOutputStreams.get(file);
    if (lockedOutputStream != null) {
      // 스트림 닫고 프리
      try {
        lockedOutputStream.close();
        this.lockedOutputStreams.remove(file);
      } catch (IOException ex) {
        throw new RecorderException("Can't close locked stream", ex);
      }
    }
  }

  /**
   *복구파일 저장소로 돌아가기.
   */
  private File getRecoveryFolder() throws RecorderException {
    try {
      UserPreferences userPreferences = this.application.getUserPreferences();
      return new File(userPreferences instanceof FileUserPreferences
          ? ((FileUserPreferences)userPreferences).getApplicationFolder()
          : OperatingSystem.getDefaultApplicationFolder(), RECOVERY_SUB_FOLDER);
    } catch (IOException ex) {
      throw new RecorderException("Can't retrieve recovered files folder", ex);
    }
  }
}