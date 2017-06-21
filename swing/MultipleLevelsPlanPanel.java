package com.eteks.homeview3d.swing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.DimensionLine;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.TextStyle;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.PlanController;
import com.eteks.homeview3d.viewcontroller.PlanController.EditableProperty;
import com.eteks.homeview3d.viewcontroller.PlanView;
import com.eteks.homeview3d.viewcontroller.View;

public class MultipleLevelsPlanPanel extends JPanel implements PlanView, Printable {
  private static final String ONE_LEVEL_PANEL_NAME = "oneLevelPanel";
  private static final String MULTIPLE_LEVELS_PANEL_NAME = "multipleLevelsPanel";
  
  private static final ImageIcon sameElevationIcon = SwingTools.getScaledImageIcon(FurnitureTable.class.getResource("resources/sameElevation.png"));
  
  private PlanComponent planComponent;
  private JScrollPane   planScrollPane;
  private JTabbedPane   multipleLevelsTabbedPane;
  private JPanel        oneLevelPanel;

  public MultipleLevelsPlanPanel(Home home, 
                                 UserPreferences preferences, 
                                 PlanController controller) {
    super(new CardLayout());
    createComponents(home, preferences, controller);
    layoutComponents();
    updateSelectedTab(home);
  }


  private void createComponents(final Home home, 
                                final UserPreferences preferences, final PlanController controller) {
    this.planComponent = createPlanComponent(home, preferences, controller);
    
    UIManager.getDefaults().put("TabbedPane.contentBorderInsets", OperatingSystem.isMacOSX()
        ? new Insets(2, 2, 2, 2)
        : new Insets(-1, 0, 2, 2));
    this.multipleLevelsTabbedPane = new JTabbedPane();
    if (OperatingSystem.isMacOSX()) {
      this.multipleLevelsTabbedPane.setBorder(new EmptyBorder(-2, -6, -7, -6));
    }
    List<Level> levels = home.getLevels();
    this.planScrollPane = new JScrollPane(this.planComponent);
    this.planScrollPane.setMinimumSize(new Dimension());
    if (OperatingSystem.isMacOSX()) {
      this.planScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      this.planScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    }
    
    createTabs(home, preferences);
    final ChangeListener changeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          Component selectedComponent = multipleLevelsTabbedPane.getSelectedComponent();
          if (selectedComponent instanceof LevelLabel) {
            controller.setSelectedLevel(((LevelLabel)selectedComponent).getLevel());
          }
        }
      };
    this.multipleLevelsTabbedPane.addChangeListener(changeListener);
    this.multipleLevelsTabbedPane.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          int indexAtLocation = multipleLevelsTabbedPane.indexAtLocation(ev.getX(), ev.getY());
          if (ev.getClickCount() == 1) {
            if (indexAtLocation == multipleLevelsTabbedPane.getTabCount() - 1) {
              controller.addLevel();
            }
            final Level oldSelectedLevel = home.getSelectedLevel();
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                  if (oldSelectedLevel == home.getSelectedLevel()) {
                    planComponent.requestFocusInWindow();
                  }
                }
              });
          } else if (indexAtLocation != -1) {
            if (multipleLevelsTabbedPane.getSelectedIndex() == multipleLevelsTabbedPane.getTabCount() - 1) {
              multipleLevelsTabbedPane.setSelectedIndex(multipleLevelsTabbedPane.getTabCount() - 2);
            } 
            controller.modifySelectedLevel();
          }
        }
      });

    final PropertyChangeListener levelChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (Level.Property.NAME.name().equals(ev.getPropertyName())) {
            int index = home.getLevels().indexOf(ev.getSource());
            multipleLevelsTabbedPane.setTitleAt(index, (String)ev.getNewValue());
            updateTabComponent(home, index);
          } else if (Level.Property.VIEWABLE.name().equals(ev.getPropertyName())) {
            updateTabComponent(home, home.getLevels().indexOf(ev.getSource()));
          } else if (Level.Property.ELEVATION.name().equals(ev.getPropertyName())
              || Level.Property.ELEVATION_INDEX.name().equals(ev.getPropertyName())) {
            multipleLevelsTabbedPane.removeChangeListener(changeListener);
            multipleLevelsTabbedPane.removeAll();
            createTabs(home, preferences);
            updateSelectedTab(home);
            multipleLevelsTabbedPane.addChangeListener(changeListener);
          }
        }
      };
    for (Level level : levels) {
      level.addPropertyChangeListener(levelChangeListener);
    }
    home.addLevelsListener(new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          multipleLevelsTabbedPane.removeChangeListener(changeListener);
          switch (ev.getType()) {
            case ADD:
              multipleLevelsTabbedPane.insertTab(ev.getItem().getName(), null, new LevelLabel(ev.getItem()), null, ev.getIndex());
              updateTabComponent(home, ev.getIndex());
              ev.getItem().addPropertyChangeListener(levelChangeListener);
              break;
            case DELETE:
              ev.getItem().removePropertyChangeListener(levelChangeListener);
              multipleLevelsTabbedPane.remove(ev.getIndex());
              break;
          }
          updateLayout(home);
          multipleLevelsTabbedPane.addChangeListener(changeListener);
        }
      });
    
    home.addPropertyChangeListener(Home.Property.SELECTED_LEVEL, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent ev) {
        multipleLevelsTabbedPane.removeChangeListener(changeListener);
        updateSelectedTab(home);
        multipleLevelsTabbedPane.addChangeListener(changeListener);
      }
    });
    
    this.oneLevelPanel = new JPanel(new BorderLayout());
    
    if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
      home.addPropertyChangeListener(Home.Property.ALL_LEVELS_SELECTION, new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            multipleLevelsTabbedPane.repaint();
          }
        });
    }
    
    preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE, 
        new LanguageChangeListener(this));
  }

  protected PlanComponent createPlanComponent(final Home home, final UserPreferences preferences,
                                              final PlanController controller) {
    return new PlanComponent(home, preferences, controller);
  }

  private void updateTabComponent(final Home home, int i) {
    if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
      JLabel tabLabel = new JLabel(this.multipleLevelsTabbedPane.getTitleAt(i)) {
          @Override
          protected void paintComponent(Graphics g) {
            if (home.isAllLevelsSelection() && isEnabled()) {
              Graphics2D g2D = (Graphics2D)g;
              g2D.setPaint(planComponent.getSelectionColor());
              Composite oldComposite = g2D.getComposite();
              g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
              g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
              Font font = getFont();
              FontMetrics fontMetrics = getFontMetrics(font);
              float strokeWidth = fontMetrics.getHeight() * 0.125f;
              g2D.setStroke(new BasicStroke(strokeWidth));
              FontRenderContext fontRenderContext = g2D.getFontRenderContext();
              TextLayout textLayout = new TextLayout(getText(), font, fontRenderContext);
              AffineTransform oldTransform = g2D.getTransform();
              if (getIcon() != null) {
                g2D.translate(getIcon().getIconWidth() + getIconTextGap(), 0);
              }
              g2D.draw(textLayout.getOutline(AffineTransform.getTranslateInstance(-strokeWidth / 5, 
                  (getHeight() - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent() - strokeWidth / 5)));
              g2D.setComposite(oldComposite);
              g2D.setTransform(oldTransform);
            }
            super.paintComponent(g);
          }
        };
      List<Level> levels = home.getLevels();
      tabLabel.setEnabled(levels.get(i).isViewable());
      if (i > 0 
          && levels.get(i - 1).getElevation() == levels.get(i).getElevation()) {
        tabLabel.setIcon(sameElevationIcon);
      }
        
      try {
        this.multipleLevelsTabbedPane.getClass().getMethod("setTabComponentAt", int.class, Component.class)
            .invoke(this.multipleLevelsTabbedPane, i, tabLabel);
      } catch (InvocationTargetException ex) {
        throw new RuntimeException(ex);
      } catch (IllegalAccessException ex) {
        throw new IllegalAccessError(ex.getMessage());
      } catch (NoSuchMethodException ex) {
        throw new NoSuchMethodError(ex.getMessage());
      }
    }
  }

   private static class LanguageChangeListener implements PropertyChangeListener {
    private WeakReference<MultipleLevelsPlanPanel> planPanel;

    public LanguageChangeListener(MultipleLevelsPlanPanel planPanel) {
      this.planPanel = new WeakReference<MultipleLevelsPlanPanel>(planPanel);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      MultipleLevelsPlanPanel planPanel = this.planPanel.get();
      UserPreferences preferences = (UserPreferences)ev.getSource();
      if (planPanel == null) {
        preferences.removePropertyChangeListener(UserPreferences.Property.LANGUAGE, this);
      } else {
        String createNewLevelTooltip = preferences.getLocalizedString(MultipleLevelsPlanPanel.class, "ADD_LEVEL.ShortDescription");
        planPanel.multipleLevelsTabbedPane.setToolTipTextAt(planPanel.multipleLevelsTabbedPane.getTabCount() - 1, createNewLevelTooltip);
      }
    }
  }

  private void createTabs(Home home, UserPreferences preferences) {
    List<Level> levels = home.getLevels();
    for (int i = 0; i < levels.size(); i++) {
      Level level = levels.get(i);
      this.multipleLevelsTabbedPane.addTab(level.getName(), new LevelLabel(level));
      updateTabComponent(home, i);
    }
    String createNewLevelIcon = preferences.getLocalizedString(MultipleLevelsPlanPanel.class, "ADD_LEVEL.SmallIcon");
    String createNewLevelTooltip = preferences.getLocalizedString(MultipleLevelsPlanPanel.class, "ADD_LEVEL.ShortDescription");
    ImageIcon newLevelIcon = SwingTools.getScaledImageIcon(MultipleLevelsPlanPanel.class.getResource(createNewLevelIcon));
    this.multipleLevelsTabbedPane.addTab("", newLevelIcon, new JLabel(), createNewLevelTooltip);
    this.multipleLevelsTabbedPane.setEnabledAt(this.multipleLevelsTabbedPane.getTabCount() - 1, false);
    this.multipleLevelsTabbedPane.setDisabledIconAt(this.multipleLevelsTabbedPane.getTabCount() - 1, newLevelIcon);
  }
  
  private void updateSelectedTab(Home home) {
    List<Level> levels = home.getLevels();
    Level selectedLevel = home.getSelectedLevel();
    if (levels.size() >= 2 && selectedLevel != null) {
      this.multipleLevelsTabbedPane.setSelectedIndex(levels.indexOf(selectedLevel));
      displayPlanComponentAtSelectedIndex(home);
    }
    updateLayout(home);
  }

  private void displayPlanComponentAtSelectedIndex(Home home) {
    int planIndex = this.multipleLevelsTabbedPane.indexOfComponent(this.planScrollPane);
    if (planIndex != -1) {
      this.multipleLevelsTabbedPane.setComponentAt(planIndex, new LevelLabel(home.getLevels().get(planIndex)));
    }
    this.multipleLevelsTabbedPane.setComponentAt(this.multipleLevelsTabbedPane.getSelectedIndex(), this.planScrollPane);
  }

  private void updateLayout(Home home) {
    CardLayout layout = (CardLayout)getLayout();
    List<Level> levels = home.getLevels();
    boolean focus = this.planComponent.hasFocus();
    if (levels.size() < 2 || home.getSelectedLevel() == null) {
      int planIndex = this.multipleLevelsTabbedPane.indexOfComponent(this.planScrollPane);
      if (planIndex != -1) {
        this.multipleLevelsTabbedPane.setComponentAt(planIndex, new LevelLabel(home.getLevels().get(planIndex)));
      }
      this.oneLevelPanel.add(this.planScrollPane);      
      layout.show(this, ONE_LEVEL_PANEL_NAME);
    } else {
      layout.show(this, MULTIPLE_LEVELS_PANEL_NAME);
    }
    if (focus) {
      this.planComponent.requestFocusInWindow();
    }
  }

  private void layoutComponents() {
    add(this.multipleLevelsTabbedPane, MULTIPLE_LEVELS_PANEL_NAME);
    add(this.oneLevelPanel, ONE_LEVEL_PANEL_NAME);
    
    SwingTools.installFocusBorder(this.planComponent);
    setFocusTraversalPolicyProvider(false);
    setMinimumSize(new Dimension());
  }

  @Override
  public void setTransferHandler(TransferHandler newHandler) {
    this.planComponent.setTransferHandler(newHandler);
  }
  
  @Override
  public void setComponentPopupMenu(JPopupMenu popup) {
    this.planComponent.setComponentPopupMenu(popup);
  }
  
  @Override
  public void addMouseMotionListener(final MouseMotionListener l) {
    this.planComponent.addMouseMotionListener(new MouseMotionListener() {
        public void mouseMoved(MouseEvent ev) {
          l.mouseMoved(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
        
        public void mouseDragged(MouseEvent ev) {
          l.mouseDragged(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
      });
  }
  
  @Override
  public void addMouseListener(final MouseListener l) {
    this.planComponent.addMouseListener(new MouseListener() {
        public void mouseReleased(MouseEvent ev) {
          l.mouseReleased(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
        
        public void mousePressed(MouseEvent ev) {
          l.mousePressed(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
        
        public void mouseExited(MouseEvent ev) {
          l.mouseExited(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
        
        public void mouseEntered(MouseEvent ev) {
          l.mouseEntered(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
        
        public void mouseClicked(MouseEvent ev) {
          l.mouseClicked(SwingUtilities.convertMouseEvent(planComponent, ev, MultipleLevelsPlanPanel.this));
        }
      });
  }
  
  @Override
  public void addFocusListener(final FocusListener l) {
    FocusListener componentFocusListener = new FocusListener() {
        public void focusGained(FocusEvent ev) {
          l.focusGained(new FocusEvent(MultipleLevelsPlanPanel.this, FocusEvent.FOCUS_GAINED, ev.isTemporary(), ev.getOppositeComponent()));
        }
        
        public void focusLost(FocusEvent ev) {
          l.focusLost(new FocusEvent(MultipleLevelsPlanPanel.this, FocusEvent.FOCUS_LOST, ev.isTemporary(), ev.getOppositeComponent()));
        }
      };
    this.planComponent.addFocusListener(componentFocusListener);
    this.multipleLevelsTabbedPane.addFocusListener(componentFocusListener);
  }
  
  public void setRectangleFeedback(float x0, float y0, float x1, float y1) {
    this.planComponent.setRectangleFeedback(x0, y0, x1, y1);
  }

  
  public void makeSelectionVisible() {
    this.planComponent.makeSelectionVisible();
  }

  
  public void makePointVisible(float x, float y) {
    this.planComponent.makePointVisible(x, y);
  }

  public float getScale() {
    return this.planComponent.getScale();
  }

  
  public void setScale(float scale) {
    this.planComponent.setScale(scale);
  }

  
  public void moveView(float dx, float dy) {
    this.planComponent.moveView(dx, dy);
  }

 
  public float convertXPixelToModel(int x) {
    return this.planComponent.convertXPixelToModel(SwingUtilities.convertPoint(this, x, 0, this.planComponent).x);
  }


  public float convertYPixelToModel(int y) {
    return this.planComponent.convertYPixelToModel(SwingUtilities.convertPoint(this, 0, y, this.planComponent).y);
  }

 
  public int convertXModelToScreen(float x) {
    return this.planComponent.convertXModelToScreen(x);
  }

 
  public int convertYModelToScreen(float y) {
    return this.planComponent.convertYModelToScreen(y);
  }


  public float getPixelLength() {
    return this.planComponent.getPixelLength();
  }

  
  public float [][] getTextBounds(String text, TextStyle style, float x, float y, float angle) {
    return this.planComponent.getTextBounds(text, style, x, y, angle);
  }


  public void setCursor(CursorType cursorType) {
    this.planComponent.setCursor(cursorType);
  }

  
  @Override
  public void setCursor(Cursor cursor) {
    this.planComponent.setCursor(cursor);
  }
  
  
  @Override
  public Cursor getCursor() {
    return this.planComponent.getCursor();
  }
  

  public void setToolTipFeedback(String toolTipFeedback, float x, float y) {
    this.planComponent.setToolTipFeedback(toolTipFeedback, x, y);
  }


  public void setToolTipEditedProperties(EditableProperty [] toolTipEditedProperties, Object [] toolTipPropertyValues,
                                         float x, float y) {
    this.planComponent.setToolTipEditedProperties(toolTipEditedProperties, toolTipPropertyValues, x, y);
  }


  public void deleteToolTipFeedback() {
    this.planComponent.deleteToolTipFeedback();
  }


  public void setResizeIndicatorVisible(boolean visible) {
    this.planComponent.setResizeIndicatorVisible(visible);
  }


  public void setAlignmentFeedback(Class<? extends Selectable> alignedObjectClass, Selectable alignedObject, float x,
                                   float y, boolean showPoint) {
    this.planComponent.setAlignmentFeedback(alignedObjectClass, alignedObject, x, y, showPoint);
  }



  public void setAngleFeedback(float xCenter, float yCenter, float x1, float y1, float x2, float y2) {
    this.planComponent.setAngleFeedback(xCenter, yCenter, x1, y1, x2, y2);
  }


  public void setDraggedItemsFeedback(List<Selectable> draggedItems) {
    this.planComponent.setDraggedItemsFeedback(draggedItems);
  }


  public void setDimensionLinesFeedback(List<DimensionLine> dimensionLines) {
    this.planComponent.setDimensionLinesFeedback(dimensionLines);
  }


  public void deleteFeedback() {
    this.planComponent.deleteFeedback();
  }
  


  public boolean canImportDraggedItems(List<Selectable> items, int x, int y) {
    JViewport viewport = this.planScrollPane.getViewport();
    Point point = SwingUtilities.convertPoint(this, x, y, viewport);
    return viewport.contains(point);
  }


  public View getHorizontalRuler() {
    return this.planComponent.getHorizontalRuler();
  }


  public View getVerticalRuler() {
    return this.planComponent.getVerticalRuler();
  }


  public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
    return this.planComponent.print(graphics, pageFormat, pageIndex);
  }


  public float getPrintPreferredScale(Graphics graphics, PageFormat pageFormat) {
    return this.planComponent.getPrintPreferredScale(graphics, pageFormat);
  }
  

  private static class LevelLabel extends JLabel {
    private final Level level;

    public LevelLabel(Level level) {
      this.level = level;
      
    }
    
    public Level getLevel() {
      return this.level;
    }
  }
}
