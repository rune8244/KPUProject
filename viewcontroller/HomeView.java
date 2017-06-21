package com.eteks.homeview3d.viewcontroller;

import java.util.List;
import java.util.concurrent.Callable;

import com.eteks.homeview3d.model.Camera;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.InterruptedRecorderException;
import com.eteks.homeview3d.model.RecorderException;
import com.eteks.homeview3d.model.Selectable;

public interface HomeView extends View { 
  public enum ActionType {
      NEW_HOME, CLOSE, OPEN, DELETE_RECENT_HOMES, SAVE, SAVE_AS, SAVE_AND_COMPRESS,
      PAGE_SETUP, PRINT_PREVIEW, PRINT, PRINT_TO_PDF, PREFERENCES, EXIT, 
      UNDO, REDO, CUT, COPY, PASTE, PASTE_TO_GROUP, PASTE_STYLE, DELETE, SELECT_ALL, SELECT_ALL_AT_ALL_LEVELS,
      ADD_HOME_FURNITURE, ADD_FURNITURE_TO_GROUP, DELETE_HOME_FURNITURE, MODIFY_FURNITURE, 
      IMPORT_FURNITURE, IMPORT_FURNITURE_LIBRARY, IMPORT_TEXTURE, IMPORT_TEXTURES_LIBRARY,
      SORT_HOME_FURNITURE_BY_CATALOG_ID, SORT_HOME_FURNITURE_BY_NAME, 
      SORT_HOME_FURNITURE_BY_WIDTH, SORT_HOME_FURNITURE_BY_DEPTH, SORT_HOME_FURNITURE_BY_HEIGHT, 
      SORT_HOME_FURNITURE_BY_X, SORT_HOME_FURNITURE_BY_Y, SORT_HOME_FURNITURE_BY_ELEVATION, 
      SORT_HOME_FURNITURE_BY_ANGLE, SORT_HOME_FURNITURE_BY_LEVEL, SORT_HOME_FURNITURE_BY_COLOR, SORT_HOME_FURNITURE_BY_TEXTURE, 
      SORT_HOME_FURNITURE_BY_MOVABILITY, SORT_HOME_FURNITURE_BY_TYPE, SORT_HOME_FURNITURE_BY_VISIBILITY, 
      SORT_HOME_FURNITURE_BY_PRICE, SORT_HOME_FURNITURE_BY_VALUE_ADDED_TAX_PERCENTAGE, 
      SORT_HOME_FURNITURE_BY_VALUE_ADDED_TAX, SORT_HOME_FURNITURE_BY_PRICE_VALUE_ADDED_TAX_INCLUDED,
      SORT_HOME_FURNITURE_BY_DESCENDING_ORDER,
      DISPLAY_HOME_FURNITURE_CATALOG_ID, DISPLAY_HOME_FURNITURE_NAME, 
      DISPLAY_HOME_FURNITURE_WIDTH, DISPLAY_HOME_FURNITURE_DEPTH, DISPLAY_HOME_FURNITURE_HEIGHT, 
      DISPLAY_HOME_FURNITURE_X, DISPLAY_HOME_FURNITURE_Y, DISPLAY_HOME_FURNITURE_ELEVATION, 
      DISPLAY_HOME_FURNITURE_ANGLE, DISPLAY_HOME_FURNITURE_COLOR, DISPLAY_HOME_FURNITURE_LEVEL, DISPLAY_HOME_FURNITURE_TEXTURE, 
      DISPLAY_HOME_FURNITURE_MOVABLE, DISPLAY_HOME_FURNITURE_DOOR_OR_WINDOW, DISPLAY_HOME_FURNITURE_VISIBLE,
      DISPLAY_HOME_FURNITURE_PRICE, DISPLAY_HOME_FURNITURE_VALUE_ADDED_TAX_PERCENTAGE,
      DISPLAY_HOME_FURNITURE_VALUE_ADDED_TAX, DISPLAY_HOME_FURNITURE_PRICE_VALUE_ADDED_TAX_INCLUDED,
      ALIGN_FURNITURE_ON_TOP, ALIGN_FURNITURE_ON_BOTTOM, ALIGN_FURNITURE_ON_LEFT, ALIGN_FURNITURE_ON_RIGHT, 
      ALIGN_FURNITURE_ON_FRONT_SIDE, ALIGN_FURNITURE_ON_BACK_SIDE, ALIGN_FURNITURE_ON_LEFT_SIDE, ALIGN_FURNITURE_ON_RIGHT_SIDE, ALIGN_FURNITURE_SIDE_BY_SIDE,
      DISTRIBUTE_FURNITURE_HORIZONTALLY, DISTRIBUTE_FURNITURE_VERTICALLY, RESET_FURNITURE_ELEVATION,
      GROUP_FURNITURE, UNGROUP_FURNITURE, EXPORT_TO_CSV, 
      SELECT, PAN, CREATE_WALLS, CREATE_ROOMS, CREATE_DIMENSION_LINES, CREATE_POLYLINES, CREATE_LABELS, DELETE_SELECTION,
      LOCK_BASE_PLAN, UNLOCK_BASE_PLAN, MODIFY_COMPASS, MODIFY_WALL, REVERSE_WALL_DIRECTION, SPLIT_WALL, 
      MODIFY_ROOM, ADD_ROOM_POINT, DELETE_ROOM_POINT, MODIFY_POLYLINE, MODIFY_LABEL,
      INCREASE_TEXT_SIZE, DECREASE_TEXT_SIZE, TOGGLE_BOLD_STYLE, TOGGLE_ITALIC_STYLE,
      IMPORT_BACKGROUND_IMAGE, MODIFY_BACKGROUND_IMAGE, HIDE_BACKGROUND_IMAGE, SHOW_BACKGROUND_IMAGE, DELETE_BACKGROUND_IMAGE, 
      ADD_LEVEL, ADD_LEVEL_AT_SAME_ELEVATION, MAKE_LEVEL_VIEWABLE, MAKE_LEVEL_UNVIEWABLE, MODIFY_LEVEL, DELETE_LEVEL,
      ZOOM_OUT, ZOOM_IN, EXPORT_TO_SVG,
      VIEW_FROM_TOP, VIEW_FROM_OBSERVER, MODIFY_OBSERVER, STORE_POINT_OF_VIEW, DELETE_POINTS_OF_VIEW, CREATE_PHOTOS_AT_POINTS_OF_VIEW, DETACH_3D_VIEW, ATTACH_3D_VIEW,  
      DISPLAY_ALL_LEVELS, DISPLAY_SELECTED_LEVEL, MODIFY_3D_ATTRIBUTES, CREATE_PHOTO, CREATE_VIDEO, EXPORT_TO_OBJ,
      HELP, ABOUT}
  public enum SaveAnswer {SAVE, CANCEL, DO_NOT_SAVE}
  public enum OpenDamagedHomeAnswer {REMOVE_DAMAGED_ITEMS, REPLACE_DAMAGED_ITEMS, DO_NOT_OPEN_HOME}
  
  public abstract void setEnabled(ActionType actionType,
                                  boolean enabled);

 
  public abstract void setUndoRedoName(String undoText,
                                       String redoText);

  public abstract void setTransferEnabled(boolean enabled);

  public abstract void detachView(View view);
  
  public abstract void attachView(View view);
  
  public abstract String showOpenDialog();
  
  public abstract OpenDamagedHomeAnswer confirmOpenDamagedHome(String homeName, 
                                                               Home damagedHome, 
                                                               List<Content> invalidContent);
  
  public abstract String showImportLanguageLibraryDialog();
 
  public abstract boolean confirmReplaceLanguageLibrary(String languageLibraryName);

  public abstract String showImportFurnitureLibraryDialog();

  public abstract boolean confirmReplaceFurnitureLibrary(String furnitureLibraryName);

  public abstract String showImportTexturesLibraryDialog();

  public abstract boolean confirmReplaceTexturesLibrary(String texturesLibraryName);

  public abstract boolean confirmReplacePlugin(String pluginName);

  public abstract String showSaveDialog(String homeName);
 
  public abstract SaveAnswer confirmSave(String homeName);

  public abstract boolean confirmSaveNewerHome(String homeName);

  public abstract boolean confirmDeleteCatalogSelection();

  public abstract boolean confirmExit();
 
  public abstract void showError(String message);

  public abstract void showMessage(String message);

  public abstract boolean showActionTipMessage(String actionTipKey);

  public abstract void showAboutDialog();

  public abstract Callable<Void> showPrintDialog();

  public abstract String showPrintToPDFDialog(String homeName);

  public abstract void printToPDF(String pdfFile) throws RecorderException;

  public abstract String showExportToCSVDialog(String name);

  public abstract void exportToCSV(String csvName) throws RecorderException;

  public abstract String showExportToSVGDialog(String name);

  public abstract void exportToSVG(String svgName) throws RecorderException;

  public abstract String showExportToOBJDialog(String homeName);
  
  public abstract void exportToOBJ(String objFile) throws RecorderException;

  public abstract String showStoreCameraDialog(String cameraName);

  public abstract List<Camera> showDeletedCamerasDialog();
  
  public abstract boolean isClipboardEmpty();

  public abstract List<Selectable> getClipboardItems();

  public abstract boolean showUpdatesMessage(String updatesMessage, boolean showOnlyMessage);
  
  public abstract void invokeLater(Runnable runnable);
}