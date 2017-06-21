package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.Polyline;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.UserPreferences;

public class PolylineController implements Controller {
  public enum Property {THICKNESS, CAP_STYLE, JOIN_STYLE, DASH_STYLE, START_ARROW_STYLE, END_ARROW_STYLE, COLOR}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final UndoableEditSupport   undoSupport;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  polylineView;

  private Float               thickness;
  private boolean             capStyleEditable;
  private Polyline.CapStyle   capStyle;
  private Polyline.JoinStyle  joinStyle;
  private boolean             joinStyleEditable;
  private Polyline.DashStyle  dashStyle;
  private boolean             arrowsStyleEditable;
  private Polyline.ArrowStyle startArrowStyle;
  private Polyline.ArrowStyle endArrowStyle;
  private Integer             color;

  public PolylineController(final Home home, 
                            UserPreferences preferences,
                            ViewFactory viewFactory, 
                            ContentManager contentManager, 
                            UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.undoSupport = undoSupport;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }


  public DialogView getView() {
    if (this.polylineView == null) {
      this.polylineView = this.viewFactory.createPolylineView(this.preferences, this); 
    }
    return this.polylineView;
  }

  public void displayView(View parentView) {
    getView().displayView(parentView);
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }


  protected void updateProperties() {
    List<Polyline> selectedPolylines = Home.getPolylinesSubList(this.home.getSelectedItems());
    if (selectedPolylines.isEmpty()) {
      setThickness(null); 
      this.capStyleEditable = false;
      setCapStyle(null);
      this.joinStyleEditable = false;
      setJoinStyle(null);
      setDashStyle(null);
      this.arrowsStyleEditable = false;
      setStartArrowStyle(null);
      setEndArrowStyle(null);
      setColor(null);
    } else {
      Polyline firstPolyline = selectedPolylines.get(0);

      Float thickness = firstPolyline.getThickness();
      if (thickness != null) {
        for (int i = 1; i < selectedPolylines.size(); i++) {
          if (thickness != selectedPolylines.get(i).getThickness()) {
            thickness = null;
            break;
          }
        }
      }
      setThickness(thickness);
      
      this.capStyleEditable = false;
      for (int i = 0; i < selectedPolylines.size(); i++) {
        if (!selectedPolylines.get(i).isClosedPath()) {
          this.capStyleEditable = true;
          break;
        }
      }

      if (this.capStyleEditable) {
        Polyline.CapStyle capStyle = firstPolyline.getCapStyle();
        if (capStyle != null) {
          for (int i = 1; i < selectedPolylines.size(); i++) {
            if (capStyle != selectedPolylines.get(i).getCapStyle()) {
              capStyle = null;
              break;
            }
          }
        }
        setCapStyle(capStyle);
      } else {
        setCapStyle(null);
      }      
      
      this.joinStyleEditable = false;
      for (int i = 0; i < selectedPolylines.size(); i++) {
        if (selectedPolylines.get(i).getPointCount() > 2) {
          this.joinStyleEditable = true;
          break;
        }
      }

      if (this.joinStyleEditable) {
        Polyline.JoinStyle joinStyle = firstPolyline.getJoinStyle();
        if (joinStyle != null) {
          for (int i = 1; i < selectedPolylines.size(); i++) {
            if (joinStyle != selectedPolylines.get(i).getJoinStyle()) {
              joinStyle = null;
              break;
            }
          }
        }
        setJoinStyle(joinStyle);
      } else {
        setJoinStyle(null);
      }
      
      Polyline.DashStyle dashStyle = firstPolyline.getDashStyle();
      if (dashStyle != null) {
        for (int i = 1; i < selectedPolylines.size(); i++) {
          if (dashStyle != selectedPolylines.get(i).getDashStyle()) {
            dashStyle = null;
            break;
          }
        }
      }
      setDashStyle(dashStyle);

      this.arrowsStyleEditable = this.capStyleEditable;
      if (this.arrowsStyleEditable) {
        Polyline.ArrowStyle startArrowStyle = firstPolyline.getStartArrowStyle();
        if (startArrowStyle != null) {
          for (int i = 1; i < selectedPolylines.size(); i++) {
            if (startArrowStyle != selectedPolylines.get(i).getStartArrowStyle()) {
              startArrowStyle = null;
              break;
            }
          }
        }
        setStartArrowStyle(startArrowStyle);
  
        Polyline.ArrowStyle endArrowStyle = firstPolyline.getEndArrowStyle();
        if (endArrowStyle != null) {
          for (int i = 1; i < selectedPolylines.size(); i++) {
            if (endArrowStyle != selectedPolylines.get(i).getEndArrowStyle()) {
              endArrowStyle = null;
              break;
            }
          }
        }
        setEndArrowStyle(endArrowStyle);
      } else {
        setStartArrowStyle(null);
        setEndArrowStyle(null);
      }

      Integer color = firstPolyline.getColor();
      if (color != null) {
        for (int i = 1; i < selectedPolylines.size(); i++) {
          if (color != selectedPolylines.get(i).getColor()) {
            color = null;
            break;
          }
        }
      }
      setColor(color);
    }
  }
  
  public void setThickness(Float thickness) {
    if (thickness != this.thickness) {
      Float oldThickness = this.thickness;
      this.thickness = thickness;
      this.propertyChangeSupport.firePropertyChange(Property.THICKNESS.name(), oldThickness, thickness);
    }
  }

  public Float getThickness() {
    return this.thickness;
  }
  
  public void setCapStyle(Polyline.CapStyle capStyle) {
    if (capStyle != this.capStyle) {
      Polyline.CapStyle oldCapStyle = this.capStyle;
      this.capStyle = capStyle;
      this.propertyChangeSupport.firePropertyChange(Property.CAP_STYLE.name(), oldCapStyle, capStyle);
    }
  }

  public Polyline.CapStyle getCapStyle() {
    return this.capStyle;
  }
  
  public boolean isCapStyleEditable() {
    return this.capStyleEditable;
  }
  
  public void setJoinStyle(Polyline.JoinStyle joinStyle) {
    if (joinStyle != this.joinStyle) {
      Polyline.JoinStyle oldJoinStyle = this.joinStyle;
      this.joinStyle = joinStyle;
      this.propertyChangeSupport.firePropertyChange(Property.JOIN_STYLE.name(), oldJoinStyle, joinStyle);
    }
  }

  public Polyline.JoinStyle getJoinStyle() {
    return this.joinStyle;
  }
  
  public boolean isJoinStyleEditable() {
    return this.joinStyleEditable;
  }
  
  public void setDashStyle(Polyline.DashStyle dashStyle) {
    if (dashStyle != this.dashStyle) {
      Polyline.DashStyle oldDashStyle = this.dashStyle;
      this.dashStyle = dashStyle;
      this.propertyChangeSupport.firePropertyChange(Property.DASH_STYLE.name(), oldDashStyle, dashStyle);
    }
  }

  public Polyline.DashStyle getDashStyle() {
    return this.dashStyle;
  }
  
  public void setStartArrowStyle(Polyline.ArrowStyle startArrowStyle) {
    if (startArrowStyle != this.startArrowStyle) {
      Polyline.ArrowStyle oldStartArrowStyle = this.startArrowStyle;
      this.startArrowStyle = startArrowStyle;
      this.propertyChangeSupport.firePropertyChange(Property.START_ARROW_STYLE.name(), oldStartArrowStyle, startArrowStyle);
    }
  }

  public Polyline.ArrowStyle getStartArrowStyle() {
    return this.startArrowStyle;
  }
  
  public void setEndArrowStyle(Polyline.ArrowStyle endArrowStyle) {
    if (endArrowStyle != this.endArrowStyle) {
      Polyline.ArrowStyle oldEndArrowStyle = this.endArrowStyle;
      this.endArrowStyle = endArrowStyle;
      this.propertyChangeSupport.firePropertyChange(Property.END_ARROW_STYLE.name(), oldEndArrowStyle, endArrowStyle);
    }
  }
  
  public Polyline.ArrowStyle getEndArrowStyle() {
    return this.endArrowStyle;
  }
  
  public boolean isArrowsStyleEditable() {
    return this.arrowsStyleEditable;
  }

  public void setColor(Integer color) {
    if (color != this.color) {
      Integer oldColor = this.color;
      this.color = color;
      this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, color);
    }
  }

  public Integer getColor() {
    return this.color;
  }

  public void modifyPolylines() {
    List<Selectable> oldSelection = this.home.getSelectedItems(); 
    List<Polyline> selectedPolylines = Home.getPolylinesSubList(oldSelection);
    if (!selectedPolylines.isEmpty()) {
      Float thickness = getThickness(); 
      Polyline.CapStyle capStyle = getCapStyle();
      Polyline.JoinStyle joinStyle = getJoinStyle();
      Polyline.DashStyle dashStyle = getDashStyle();
      Polyline.ArrowStyle startArrowStyle = getStartArrowStyle();  
      Polyline.ArrowStyle endArrowStyle = getEndArrowStyle();
      Integer color = getColor();
      
      ModifiedPolyline [] modifiedPolylines = new ModifiedPolyline [selectedPolylines.size()]; 
      for (int i = 0; i < modifiedPolylines.length; i++) {
        modifiedPolylines [i] = new ModifiedPolyline(selectedPolylines.get(i));
      }
      doModifyPolylines(modifiedPolylines, thickness, capStyle, joinStyle, dashStyle, startArrowStyle, endArrowStyle, color);       
      if (this.undoSupport != null) {
        UndoableEdit undoableEdit = new PolylinesModificationUndoableEdit(
            this.home, this.preferences, oldSelection,
            modifiedPolylines, thickness, capStyle, joinStyle, dashStyle, 
            startArrowStyle, endArrowStyle, color);
        this.undoSupport.postEdit(undoableEdit);
      }
    }
  }

  private static class PolylinesModificationUndoableEdit extends AbstractUndoableEdit {
    private final Home                home;
    private final UserPreferences     preferences;
    private final List<Selectable>    oldSelection;
    private final ModifiedPolyline [] modifiedPolylines;
    private Float                     thickness;
    private Polyline.CapStyle         capStyle;
    private Polyline.JoinStyle        joinStyle;
    private Polyline.DashStyle        dashStyle;
    private Polyline.ArrowStyle       startArrowStyle;  
    private Polyline.ArrowStyle       endArrowStyle;
    private Integer                   color;

    private PolylinesModificationUndoableEdit(Home home,
                                          UserPreferences preferences,
                                          List<Selectable> oldSelection,
                                          ModifiedPolyline [] modifiedPolylines, 
                                          Float thickness, 
                                          Polyline.CapStyle capStyle, 
                                          Polyline.JoinStyle joinStyle, 
                                          Polyline.DashStyle dashStyle, 
                                          Polyline.ArrowStyle startArrowStyle,  
                                          Polyline.ArrowStyle endArrowStyle,
                                          Integer color) {
      this.home = home;
      this.preferences = preferences;
      this.oldSelection = oldSelection;
      this.modifiedPolylines = modifiedPolylines;
      this.thickness = thickness;
      this.capStyle = capStyle;
      this.joinStyle = joinStyle;
      this.dashStyle = dashStyle;
      this.startArrowStyle = startArrowStyle;
      this.endArrowStyle = endArrowStyle;
      this.color = color;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      undoModifyPolylines(this.modifiedPolylines); 
      this.home.setSelectedItems(this.oldSelection); 
    }

    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      doModifyPolylines(this.modifiedPolylines, this.thickness, 
          this.capStyle, this.joinStyle, this.dashStyle, this.startArrowStyle, this.endArrowStyle, this.color); 
      this.home.setSelectedItems(this.oldSelection); 
    }

    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(PolylineController.class, "undoModifyPolylinesName");
    }
  }

  private static void doModifyPolylines(ModifiedPolyline [] modifiedPolylines, 
                                        Float thickness, Polyline.CapStyle capStyle, 
                                        Polyline.JoinStyle joinStyle, Polyline.DashStyle dashStyle, 
                                        Polyline.ArrowStyle startArrowStyle, Polyline.ArrowStyle endArrowStyle,
                                        Integer color) {
    for (ModifiedPolyline modifiedPolyline : modifiedPolylines) {
      Polyline polyline = modifiedPolyline.getPolyline();
      if (thickness != null) {
        polyline.setThickness(thickness);
      }
      if (capStyle != null) {
        polyline.setCapStyle(capStyle);
      }
      if (joinStyle != null) {
        polyline.setJoinStyle(joinStyle);
      }
      if (dashStyle != null) {
        polyline.setDashStyle(dashStyle);
      }
      if (startArrowStyle != null) {
        polyline.setStartArrowStyle(startArrowStyle);
      }
      if (endArrowStyle != null) {
        polyline.setEndArrowStyle(endArrowStyle);
      }
      if (color != null) {
        polyline.setColor(color);
      }
    }
  }

  private static void undoModifyPolylines(ModifiedPolyline [] modifiedPolylines) {
    for (ModifiedPolyline modifiedPolyline : modifiedPolylines) {
      modifiedPolyline.reset();
    }
  }
  
  private static final class ModifiedPolyline {
    private final Polyline            polyline;
    private final float               thickness;
    private final Polyline.CapStyle   capStyle;
    private final Polyline.JoinStyle  joinStyle;
    private final Polyline.DashStyle  dashStyle;
    private final Polyline.ArrowStyle startArrowStyle;
    private final Polyline.ArrowStyle endArrowStyle;
    private final int                 color;

    public ModifiedPolyline(Polyline polyline) {
      this.polyline = polyline;
      this.thickness = polyline.getThickness();
      this.capStyle = polyline.getCapStyle();
      this.joinStyle = polyline.getJoinStyle();
      this.dashStyle = polyline.getDashStyle();
      this.startArrowStyle = polyline.getStartArrowStyle();
      this.endArrowStyle = polyline.getEndArrowStyle();
      this.color = polyline.getColor();
    }

    public Polyline getPolyline() {
      return this.polyline;
    }
    
    public void reset() {
      this.polyline.setThickness(this.thickness);
      this.polyline.setCapStyle(this.capStyle);
      this.polyline.setJoinStyle(this.joinStyle);
      this.polyline.setDashStyle(this.dashStyle);
      this.polyline.setStartArrowStyle(this.startArrowStyle);
      this.polyline.setEndArrowStyle(this.endArrowStyle);
      this.polyline.setColor(this.color);
    }    
  }
}
