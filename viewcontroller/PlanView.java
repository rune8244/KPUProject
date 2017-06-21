package com.eteks.homeview3d.viewcontroller;

import java.util.List;

import com.eteks.homeview3d.model.DimensionLine;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.TextStyle;

public interface PlanView extends View {
  public enum CursorType {SELECTION, PANNING, DRAW, ROTATION, ELEVATION, HEIGHT, POWER, RESIZE, DUPLICATION, MOVE}
  
  public abstract void setRectangleFeedback(float x0, float y0,
                                            float x1, float y1);

 
  public abstract void makeSelectionVisible();

 
  public abstract void makePointVisible(float x, float y);

 
  public abstract float getScale();


  public abstract void setScale(float scale);

  
  public abstract void moveView(float dx, float dy);


  public abstract float convertXPixelToModel(int x);

  
  public abstract float convertYPixelToModel(int y);

  
  public abstract int convertXModelToScreen(float x);

  
  public abstract int convertYModelToScreen(float y);

 
  public abstract float getPixelLength();

  
  public abstract float [][] getTextBounds(String text, TextStyle style, 
                                           float x, float y, float angle);

  
  public abstract void setCursor(CursorType cursorType);

  
  public abstract void setToolTipFeedback(String toolTipFeedback,
                                          float x, float y);

  
  public abstract void setToolTipEditedProperties(PlanController.EditableProperty [] toolTipEditedProperties, 
                                                  Object [] toolTipPropertyValues,
                                                  float x, float y);
  
 
  public abstract void deleteToolTipFeedback();

  
  public abstract void setResizeIndicatorVisible(boolean resizeIndicatorVisible);

  
  public abstract void setAlignmentFeedback(Class<? extends Selectable> alignedObjectClass,
                                            Selectable alignedObject,
                                            float x, 
                                            float y, 
                                            boolean showPoint);
  

  
  public abstract void setAngleFeedback(float xCenter, float yCenter, 
                                        float x1, float y1, 
                                        float x2, float y2);

  
  public abstract void setDraggedItemsFeedback(List<Selectable> draggedItems);

  
  public abstract void setDimensionLinesFeedback(List<DimensionLine> dimensionLines);

  
  public abstract void deleteFeedback();


  
  public abstract View getHorizontalRuler();

 
  public abstract View getVerticalRuler();

  
  public abstract boolean canImportDraggedItems(List<Selectable> items, int x, int y);
}