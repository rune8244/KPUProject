package com.eteks.homeview3d.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.dnd.DnDConstants;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.eteks.homeview3d.model.CatalogPieceOfFurniture;
import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.FurnitureCatalog;
import com.eteks.homeview3d.model.FurnitureCategory;
import com.eteks.homeview3d.model.SelectionEvent;
import com.eteks.homeview3d.model.SelectionListener;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.viewcontroller.FurnitureCatalogController;
import com.eteks.homeview3d.viewcontroller.View;

// 가구카탈로그 트리
public class FurnitureCatalogTree extends JTree implements View {
  private final UserPreferences preferences;
  private TreeSelectionListener treeSelectionListener;
  private CatalogItemToolTip    toolTip;

  // 카탈로그 트리 생성
  public FurnitureCatalogTree(FurnitureCatalog catalog) {
    this(catalog, null);
  }

  // 카탈로그 트리 컨트롤러 생성
  public FurnitureCatalogTree(FurnitureCatalog catalog, 
                              FurnitureCatalogController controller) {
    this(catalog, null, controller);
  }
  
  public FurnitureCatalogTree(FurnitureCatalog catalog, 
                              UserPreferences preferences, 
                              FurnitureCatalogController controller) {
    this.preferences = preferences;
    float resolutionScale = SwingTools.getResolutionScale();
    if (resolutionScale != 1) {
      setRowHeight(Math.round(getRowHeight() * resolutionScale));
    }
    this.toolTip = new CatalogItemToolTip(true, preferences);
    setModel(new CatalogTreeModel(catalog));
    setRootVisible(false);
    setShowsRootHandles(true);
    setCellRenderer(new CatalogCellRenderer());
    addDragListener();
    if (controller != null) {
      updateTreeSelectedFurniture(catalog, controller);
      addSelectionListeners(catalog, controller);
      addMouseListeners(controller);
    }
    ToolTipManager.sharedInstance().registerComponent(this);
    addVerticalScrollBarAdjustmentListener();
    // 선택된 액션 전부 제거
    getActionMap().getParent().remove("selectAll");
  }

  // 드래고했을 때 마우스 액션
  private void addDragListener() {
    MouseInputAdapter mouseListener = new MouseInputAdapter() {
        private boolean canExport;

        @Override
        public void mousePressed(MouseEvent ev) {
          this.canExport = SwingUtilities.isLeftMouseButton(ev)
              && getPathForLocation(ev.getX(), ev.getY()) != null;
        }
        
        public void mouseDragged(MouseEvent ev) {
          if (this.canExport 
              && getTransferHandler() != null) {
            getTransferHandler().exportAsDrag(FurnitureCatalogTree.this, ev, DnDConstants.ACTION_COPY);
          }
          this.canExport = false;
        }
      };
      
    addMouseListener(mouseListener);
    addMouseMotionListener(mouseListener);
  }
  
  // 트리 선택 동기화 관리자 추가
  private void addSelectionListeners(final FurnitureCatalog catalog, 
                                     final FurnitureCatalogController controller) {
    final SelectionListener modelSelectionListener = new SelectionListener() {
        public void selectionChanged(SelectionEvent selectionEvent) {
          updateTreeSelectedFurniture(catalog, controller);        
        }
      };
    this.treeSelectionListener = new TreeSelectionListener () {
        public void valueChanged(TreeSelectionEvent ev) {
          // Updates selected furniture in catalog from selected nodes in tree. 
          controller.removeSelectionListener(modelSelectionListener);
          controller.setSelectedFurniture(getSelectedFurniture());
          controller.addSelectionListener(modelSelectionListener);
        }
      };
      
    controller.addSelectionListener(modelSelectionListener);
    getSelectionModel().addTreeSelectionListener(this.treeSelectionListener);
  }
  
  // 선택된 카탈로그 업데이트
  private void updateTreeSelectedFurniture(FurnitureCatalog catalog,
                                           FurnitureCatalogController controller) {
    if (this.treeSelectionListener != null) {
      getSelectionModel().removeTreeSelectionListener(this.treeSelectionListener);
    }
    
    clearSelection();
    for (CatalogPieceOfFurniture piece : controller.getSelectedFurniture()) {
      TreePath path = new TreePath(new Object [] {catalog, piece.getCategory(), piece});
      addSelectionPath(path);
      scrollRowToVisible(getRowForPath(path));
    }
    
    if (this.treeSelectionListener != null) {
      getSelectionModel().addTreeSelectionListener(this.treeSelectionListener);
    }
  }

  
  private List<CatalogPieceOfFurniture> getSelectedFurniture() {
    // Build the list of selected furniture
    List<CatalogPieceOfFurniture> selectedFurniture = new ArrayList<CatalogPieceOfFurniture>();
    TreePath [] selectionPaths = getSelectionPaths(); 
    if (selectionPaths != null) {
      for (TreePath path : selectionPaths) {
        if (path.getPathCount() == 3) {
          selectedFurniture.add((CatalogPieceOfFurniture)path.getLastPathComponent());
        }
      }
    }   
    return selectedFurniture;
  }
  
  
  private void addMouseListeners(final FurnitureCatalogController controller) {
    final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
    MouseInputAdapter mouseListener = new MouseInputAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          if (SwingUtilities.isLeftMouseButton(ev)) {
            if (ev.getClickCount() == 2) {
              TreePath clickedPath = getPathForLocation(ev.getX(), ev.getY());
              if (clickedPath != null
                  && clickedPath.getLastPathComponent() instanceof CatalogPieceOfFurniture) {
                controller.modifySelectedFurniture();
              }
            } else {
              URL url = getURLAt(ev.getPoint(), (JTree)ev.getSource());
              if (url != null) {
                SwingTools.showDocumentInBrowser(url);
              }
            }
          }
        }
        
        @Override
        public void mouseMoved(MouseEvent ev) {
          final URL url = getURLAt(ev.getPoint(), (JTree)ev.getSource());
          EventQueue.invokeLater(new Runnable() {                  
              public void run() {
                if (url != null) {
                  setCursor(handCursor);
                } else {
                  setCursor(Cursor.getDefaultCursor());
                }
              }
            });
        }

        private URL getURLAt(Point point, JTree tree) {
          TreePath path = tree.getPathForLocation(point.x, point.y);
          if (path != null
              && path.getLastPathComponent() instanceof CatalogPieceOfFurniture) {
            CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)path.getLastPathComponent();
            String information = piece.getInformation();
            if (information != null) {
              int row = tree.getRowForPath(path);
              JComponent rendererComponent = (JComponent)tree.getCellRenderer().
                  getTreeCellRendererComponent(tree, piece, false, false, false, row, false);
              rendererComponent.doLayout();
              for (JEditorPane pane : SwingTools.findChildren(rendererComponent, JEditorPane.class)) {
                Rectangle rowBounds = tree.getRowBounds(row);
                point.x -= rowBounds.x + pane.getX(); 
                point.y -= rowBounds.y + pane.getY(); 
                if (point.x > 0 && point.y > 0) {
                  // Search in information pane if point is over a HTML link
                  int position = pane.viewToModel(point);
                  if (position > 0) {
                    HTMLDocument hdoc = (HTMLDocument)pane.getDocument();
                    Element element = hdoc.getCharacterElement(position);
                    AttributeSet a = element.getAttributes();
                    AttributeSet anchor = (AttributeSet)a.getAttribute(HTML.Tag.A);
                    if (anchor != null) {
                      String href = (String)anchor.getAttribute(HTML.Attribute.HREF);
                      if (href != null) {
                        try {
                          return new URL(href);
                        } catch (MalformedURLException ex) {
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          return null;
        }
      };
    addMouseListener(mouseListener);
    addMouseMotionListener(mouseListener);
  }

 
  private void addVerticalScrollBarAdjustmentListener() {
    addAncestorListener(new AncestorListener() {
        private AdjustmentListener adjustmentListener;

        public void ancestorAdded(AncestorEvent ev) {
          Container parent = getParent();
          if (parent instanceof JViewport) {
            JScrollPane scrollPane = (JScrollPane)((JViewport)parent).getParent();
            this.adjustmentListener = SwingTools.createAdjustmentListenerUpdatingScrollPaneViewToolTip(scrollPane);
            scrollPane.getVerticalScrollBar().addAdjustmentListener(this.adjustmentListener);
          }
        }
        
        public void ancestorRemoved(AncestorEvent event) {
          if (this.adjustmentListener != null) {
            ((JScrollPane)((JViewport)getParent()).getParent()).getVerticalScrollBar().
                removeAdjustmentListener(this.adjustmentListener);
            this.adjustmentListener = null;
          }
        }
        
        public void ancestorMoved(AncestorEvent event) {
        }
      });
  }

 @Override
  public JToolTip createToolTip() {    
    if (this.toolTip.isTipTextComplete()) {
      return super.createToolTip();
    } else {
      this.toolTip.setComponent(this);
      return this.toolTip;
    }
  }

  
  @Override
  public String getToolTipText(MouseEvent ev) {
    TreePath path = getPathForLocation(ev.getX(), ev.getY());
    if (this.preferences != null
        && path != null
        && path.getPathCount() == 3) {
      this.toolTip.setCatalogItem((CatalogPieceOfFurniture)path.getLastPathComponent());
      return this.toolTip.getTipText();
    } else {
      return null;
    }
  }
  
private class CatalogCellRenderer extends JComponent implements TreeCellRenderer {
    private static final int        DEFAULT_ICON_HEIGHT = 32;
    private Font                    defaultFont;
    private Font                    modifiablePieceFont;
    private DefaultTreeCellRenderer nameLabel;
    private JEditorPane             informationPane;
    
    public CatalogCellRenderer() {
      setLayout(null);
      this.nameLabel = new DefaultTreeCellRenderer();
      this.informationPane = new JEditorPane("text/html", null);
      this.informationPane.setOpaque(false);
      this.informationPane.setEditable(false);
      add(this.nameLabel);
      add(this.informationPane);
    }
    
    public Component getTreeCellRendererComponent(JTree tree, 
        Object value, boolean selected, boolean expanded, 
        boolean leaf, int row, boolean hasFocus) {
      this.nameLabel.getTreeCellRendererComponent( 
          tree, value, selected, expanded, leaf, row, hasFocus);
      if (this.defaultFont == null) {
        this.defaultFont = this.nameLabel.getFont();
        String bodyRule = "body { font-family: " + this.defaultFont.getFamily() + "; " 
            + "font-size: " + this.defaultFont.getSize() + "pt; " 
            + "margin: 0; }";
        ((HTMLDocument)this.informationPane.getDocument()).getStyleSheet().addRule(bodyRule);
        this.modifiablePieceFont = 
            new Font(this.defaultFont.getFontName(), Font.ITALIC, this.defaultFont.getSize());        
      }
      if (value instanceof FurnitureCategory) {
        this.nameLabel.setText(((FurnitureCategory)value).getName());
        this.nameLabel.setFont(this.defaultFont);
        this.informationPane.setVisible(false);
      } 
      else if (value instanceof CatalogPieceOfFurniture) {
        CatalogPieceOfFurniture piece = (CatalogPieceOfFurniture)value;
        this.nameLabel.setText(piece.getName());
        this.nameLabel.setIcon(getLabelIcon(tree, piece.getIcon()));
        this.nameLabel.setFont(piece.isModifiable() 
            ? this.modifiablePieceFont : this.defaultFont);
        
        this.informationPane.setVisible(true);
        this.informationPane.setText(piece.getInformation());
      }
      return this;
    }
    
    @Override
    public void doLayout() {
      Dimension namePreferredSize = this.nameLabel.getPreferredSize();
      this.nameLabel.setSize(namePreferredSize);
      if (this.informationPane.isVisible()) {
        Dimension informationPreferredSize = this.informationPane.getPreferredSize();
        this.informationPane.setBounds(namePreferredSize.width + 2, 
            Math.max(0, (namePreferredSize.height - informationPreferredSize.height) / 2),
            informationPreferredSize.width, namePreferredSize.height);
      }
    }
    
    @Override
    public Dimension getPreferredSize() {
      Dimension preferredSize = this.nameLabel.getPreferredSize();
      if (this.informationPane.isVisible()) {
        preferredSize.width += 2 + this.informationPane.getPreferredSize().width;
      }
      return preferredSize;
    }
    
    
    @Override
    public void revalidate() {      
    }
    
    @Override
    public void repaint(long tm, int x, int y, int width, int height) {      
    }

    @Override
    public void repaint(Rectangle r) {      
    }

    @Override
    public void repaint() {      
    }

    
    private Icon getLabelIcon(JTree tree, Content content) {
      return IconManager.getInstance().getIcon(content, getRowHeight(tree), tree);
    }

    
    private int getRowHeight(JTree tree) {
      return tree.isFixedRowHeight()
          ? tree.getRowHeight()
          : DEFAULT_ICON_HEIGHT;
    }
    
    @Override
    protected void paintChildren(Graphics g) {
      // Force text anti aliasing on texts
      ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      super.paintChildren(g);
    }
  }
  
  
  private static class CatalogTreeModel implements TreeModel {
    private FurnitureCatalog        catalog;
    private List<TreeModelListener> listeners;
    
    public CatalogTreeModel(FurnitureCatalog catalog) {
      this.catalog = catalog;
      this.listeners = new ArrayList<TreeModelListener>(2);
      catalog.addFurnitureListener(new CatalogFurnitureListener(this));
    }

    public Object getRoot() {
      return this.catalog;
    }

    public Object getChild(Object parent, int index) {
      if (parent instanceof FurnitureCatalog) {
        return ((FurnitureCatalog)parent).getCategory(index);
      } else {
        return ((FurnitureCategory)parent).getPieceOfFurniture(index);
      }
    }

    public int getChildCount(Object parent) {
      if (parent instanceof FurnitureCatalog) {
        return ((FurnitureCatalog)parent).getCategoriesCount();
      } else if (parent instanceof FurnitureCategory) {
        return ((FurnitureCategory)parent).getFurnitureCount();
      } else {
        javax.swing.plaf.basic.BasicTreeUI$Actions.traverse 
        return 0;
      }
    }

    public int getIndexOfChild(Object parent, Object child) {
      if (parent instanceof FurnitureCatalog) {
        return Collections.binarySearch(((FurnitureCatalog)parent).getCategories(), (FurnitureCategory)child);
      } else {
        return ((FurnitureCategory)parent).getIndexOfPieceOfFurniture((CatalogPieceOfFurniture)child);
      }
    }

    public boolean isLeaf(Object node) {
      return node instanceof CatalogPieceOfFurniture;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
      // 편집불가상태
    }

    public void addTreeModelListener(TreeModelListener l) {
      this.listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
      this.listeners.remove(l);
    }
    
    private void fireTreeNodesInserted(TreeModelEvent treeModelEvent) {
      // 수정 가능
      TreeModelListener [] listeners = this.listeners.
          toArray(new TreeModelListener [this.listeners.size()]);
      for (TreeModelListener listener : listeners) {
        listener.treeNodesInserted(treeModelEvent);
      }
    }

    private void fireTreeNodesRemoved(TreeModelEvent treeModelEvent) {
      TreeModelListener [] listeners = this.listeners.
          toArray(new TreeModelListener [this.listeners.size()]);
      for (TreeModelListener listener : listeners) {
        listener.treeNodesRemoved(treeModelEvent);
      }
    }
    
    
    private static class CatalogFurnitureListener implements CollectionListener<CatalogPieceOfFurniture> {
      private WeakReference<CatalogTreeModel>  catalogTreeModel;

      public CatalogFurnitureListener(CatalogTreeModel catalogTreeModel) {
        this.catalogTreeModel = new WeakReference<CatalogTreeModel>(catalogTreeModel);
      }
      
      public void collectionChanged(CollectionEvent<CatalogPieceOfFurniture> ev) {
        CatalogTreeModel catalogTreeModel = this.catalogTreeModel.get();
        FurnitureCatalog catalog = (FurnitureCatalog)ev.getSource();
        if (catalogTreeModel == null) {
          catalog.removeFurnitureListener(this);
        } else {
          CatalogPieceOfFurniture piece = ev.getItem();
          switch (ev.getType()) {
            case ADD :
              if (piece.getCategory().getFurnitureCount() == 1) {
                catalogTreeModel.fireTreeNodesInserted(new TreeModelEvent(catalogTreeModel,
                    new Object [] {catalog}, 
                    new int [] {Collections.binarySearch(catalog.getCategories(), piece.getCategory())}, 
                    new Object [] {piece.getCategory()}));
              } else {
                catalogTreeModel.fireTreeNodesInserted(new TreeModelEvent(catalogTreeModel,
                    new Object [] {catalog, piece.getCategory()},
                    new int [] {ev.getIndex()},
                    new Object [] {piece}));
              }
              break;
            case DELETE :
              if (piece.getCategory().getFurnitureCount() == 0) {
                catalogTreeModel.fireTreeNodesRemoved(new TreeModelEvent(catalogTreeModel,
                    new Object [] {catalog},
                    new int [] {-(Collections.binarySearch(catalog.getCategories(), piece.getCategory()) + 1)},
                    new Object [] {piece.getCategory()}));
              } else {
                // Fire nodes removed for deleted piece
                catalogTreeModel.fireTreeNodesRemoved(new TreeModelEvent(catalogTreeModel, 
                    new Object [] {catalog, piece.getCategory()},
                    new int [] {ev.getIndex()},
                    new Object [] {piece}));
              }
              break;
          }
        }
      }
    }
  }
}