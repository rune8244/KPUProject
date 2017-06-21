package com.eteks.homeview3d.swing;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeFurnitureGroup;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.LengthUnit;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.SelectionEvent;
import com.eteks.homeview3d.model.SelectionListener;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.ResourceURLContent;
import com.eteks.homeview3d.viewcontroller.FurnitureController;
import com.eteks.homeview3d.viewcontroller.View;

public class FurnitureTable extends JTable implements View, Printable {
  private static final String EXPANDED_ROWS_VISUAL_PROPERTY = "com.eteks.homeview3d.homeview3d.ExpandedGroups";

  private UserPreferences        preferences;
  private ListSelectionListener  tableSelectionListener;
  private boolean                selectionByUser;
  private int                    furnitureInformationRow;
  private Popup                  furnitureInformationPopup;
  private AWTEventListener       informationPopupRemovalListener;

  // 가구테이블
  public FurnitureTable(Home home, UserPreferences preferences) {
    this(home, preferences, null);
  }

  // 가구 테이블 생성
  public FurnitureTable(Home home, UserPreferences preferences, 
                       FurnitureController controller) {
    this.preferences = preferences;
    float resolutionScale = SwingTools.getResolutionScale();
    if (resolutionScale != 1) {
      setRowHeight(Math.round(getRowHeight() * resolutionScale));
    }
    setModel(new FurnitureTreeTableModel(home));
    setColumnModel(new FurnitureTableColumnModel(home, preferences));
    updateTableColumnsWidth(0);
    updateExpandedRows(home);
    updateTableSelectedFurniture(home);
    // Add listeners to model
    if (controller != null) {
      addSelectionListeners(home, controller);
      // Enable sort in table with click in header
      addTableHeaderListener(controller);
      addTableColumnModelListener(controller);
      addMouseListener(home, controller);
    }
    addHomeListener(home, controller);
    addUserPreferencesListener(preferences);
    
    if (OperatingSystem.isJavaVersionGreaterOrEqual("1.6")) {
      try {
        Class<?> dropModeEnum = Class.forName("javax.swing.DropMode");
        Object insertRowsDropMode = dropModeEnum.getMethod("valueOf", String.class).invoke(null, "INSERT_ROWS");
        getClass().getMethod("setDropMode", dropModeEnum).invoke(this, insertRowsDropMode);
        UIManager.getDefaults().remove("Table.dropLineColor");
        UIManager.getDefaults().remove("Table.dropLineShortColor");
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
  
  // 테이블 관리 리스너 추가
  private void addSelectionListeners(final Home home,
                                     final FurnitureController controller) {   
    final SelectionListener homeSelectionListener = new SelectionListener() {
        public void selectionChanged(SelectionEvent ev) {
          updateTableSelectedFurniture(home);     
          storeExpandedRows(home, controller);
        }
      };
    this.tableSelectionListener = new ListSelectionListener () {
        public void valueChanged(ListSelectionEvent ev) {
          selectionByUser = true;
          int [] selectedRows = getSelectedRows();
          // 선택된 가구 리스트 생성
          List<HomePieceOfFurniture> selectedFurniture = new ArrayList<HomePieceOfFurniture>(selectedRows.length);
          List<HomePieceOfFurniture> ignoredGroupsFurniture = new ArrayList<HomePieceOfFurniture>();
          TableModel tableModel = getModel();
          for (int index : selectedRows) {
            HomePieceOfFurniture piece = (HomePieceOfFurniture)tableModel.getValueAt(index, 0);
            if (!ignoredGroupsFurniture.contains(piece)) {
              selectedFurniture.add(piece);
              if (piece instanceof HomeFurnitureGroup) {
                ignoredGroupsFurniture.addAll(((HomeFurnitureGroup)piece).getAllFurniture());
              }
            }
          }
          controller.setSelectedFurniture(new ArrayList<HomePieceOfFurniture>(selectedFurniture));
          selectionByUser = false;
        }
      };
    getSelectionModel().addListSelectionListener(this.tableSelectionListener);
    home.addSelectionListener(homeSelectionListener);
  }

  // 테이블 내 선택된 가구들 업데이트
  private void updateTableSelectedFurniture(Home home) {
    ListSelectionModel selectionModel = getSelectionModel();
    selectionModel.removeListSelectionListener(this.tableSelectionListener);

    FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)getModel();
    List<Selectable> selectedItems = home.getSelectedItems();
    for (Selectable item : selectedItems) {
      if (item instanceof HomePieceOfFurniture) {
        tableModel.expandPathToPieceOfFurniture((HomePieceOfFurniture)item);
      }
    }
    
    int minIndex = Integer.MAX_VALUE;
    int maxIndex = Integer.MIN_VALUE;
    int [] furnitureIndices = new int [tableModel.getRowCount()];
    int selectedFurnitureCount = 0;
    for (Selectable item : selectedItems) {
      if (item instanceof HomePieceOfFurniture) {
        HomePieceOfFurniture piece = (HomePieceOfFurniture)item;
        int rowIndex = tableModel.getPieceOfFurnitureIndex(piece);
        if (rowIndex != -1) {         
          furnitureIndices [selectedFurnitureCount++] = rowIndex;
          minIndex = Math.min(minIndex, rowIndex);
          maxIndex = Math.max(maxIndex, rowIndex);
          if (piece instanceof HomeFurnitureGroup
             && tableModel.isRowExpanded(rowIndex)) {
            List<HomePieceOfFurniture> groupFurniture = ((HomeFurnitureGroup)piece).getAllFurniture();
            for (rowIndex++; 
                 rowIndex < tableModel.getRowCount() 
                 && groupFurniture.contains((HomePieceOfFurniture)tableModel.getValueAt(rowIndex, 0)); 
                 rowIndex++) {
              furnitureIndices [selectedFurnitureCount++] = rowIndex;
              minIndex = Math.min(minIndex, rowIndex);
              maxIndex = Math.max(maxIndex, rowIndex);
            }
          }
        }
      }
    }

    if (selectedFurnitureCount < furnitureIndices.length) {
      int [] tmp = new int [selectedFurnitureCount];
      System.arraycopy(furnitureIndices, 0, tmp, 0, selectedFurnitureCount);
      furnitureIndices = tmp;
    }
    Arrays.sort(furnitureIndices);
    
    if (getSelectedRowCount() != selectedFurnitureCount
        || !Arrays.equals(getSelectedRows(), furnitureIndices)) {
      deleteInformationPopup();
      clearSelection();
      for (int min = 0; min < furnitureIndices.length; ) {
        int max = min;
        while (max + 1 < furnitureIndices.length
            && furnitureIndices [max] + 1 == furnitureIndices [max + 1]) {
          max++;
        }
        addRowSelectionInterval(furnitureIndices [max], furnitureIndices [min]);
        min = max + 1;
      }
    }

    if (!this.selectionByUser && minIndex != Integer.MIN_VALUE) {
      makeRowsVisible(minIndex, maxIndex);
    }
    getSelectionModel().addListSelectionListener(this.tableSelectionListener);
  }

  // 출력 속성 업데이트
  private void updateExpandedRows(Home home) {
    if (home.getVersion() >= 5000) {
      final String expandedRows = home.getProperty(EXPANDED_ROWS_VISUAL_PROPERTY);
      if (expandedRows != null && expandedRows.length() > 0) {        
        addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent event) {
              FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)getModel();
              for (String row : expandedRows.split(",")) {
                int rowIndex = Integer.parseInt(row);
                if (rowIndex < tableModel.getRowCount() && !tableModel.isRowExpanded(rowIndex)) {
                  tableModel.toggleRowExpandedState(rowIndex);
                }
              }
              removeAncestorListener(this);
            }
  
            public void ancestorRemoved(AncestorEvent event) {
            }
  
            public void ancestorMoved(AncestorEvent event) {
            }
          });
      }
    }
  }

  
  private void storeExpandedRows(Home home, FurnitureController controller) {
    FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)getModel();
    StringBuilder rows = new StringBuilder();
    for (int row = 0, n = tableModel.getRowCount(); row < n; row++) {
      if (tableModel.isRowExpanded(row)) {
        if (rows.length() != 0) {
          rows.append(',');
        }
        rows.append(row);
      }
    }
    if (home.getProperty(EXPANDED_ROWS_VISUAL_PROPERTY) != null
        || rows.length() > 0) {
      controller.setHomeProperty(EXPANDED_ROWS_VISUAL_PROPERTY, rows.toString());
    }
  }

  private void updateTableColumnsWidth(int additionalSpacing) {
    int intercellWidth = getIntercellSpacing().width + additionalSpacing;
    TableColumnModel columnModel = getColumnModel();
    TableModel tableModel = getModel();
    for (int columnIndex = 0, n = columnModel.getColumnCount(); columnIndex < n; columnIndex++) {
      TableColumn column = columnModel.getColumn(columnIndex);
      int modelColumnIndex = convertColumnIndexToModel(columnIndex);
      int preferredWidth = column.getHeaderRenderer().getTableCellRendererComponent(
          this, column.getHeaderValue(), false, false, -1, columnIndex).getPreferredSize().width;
      int rowCount = tableModel.getRowCount();
      if (rowCount > 0) {
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
          preferredWidth = Math.max(preferredWidth, 
              column.getCellRenderer().getTableCellRendererComponent(
                  this, tableModel.getValueAt(rowIndex, modelColumnIndex), false, false, -1, columnIndex).
                      getPreferredSize().width);
        }
      } else {
        preferredWidth = Math.max(preferredWidth, column.getPreferredWidth());
      }
      column.setPreferredWidth(preferredWidth + intercellWidth);
      column.setWidth(preferredWidth + intercellWidth);
    }
  }
  
  // 마우스 더블클릭 시 동작
  private void addMouseListener(final Home home, final FurnitureController controller) {
    addMouseListener(new MouseAdapter () {
        @Override
        public void mouseClicked(MouseEvent ev) {
          final int column = columnAtPoint(ev.getPoint());
          final int row = rowAtPoint(ev.getPoint());
          boolean isVisibleColumn = false;
          boolean isGroupExpandIcon = false;
          boolean isInformationIcon = false;
          if (column >= 0
              && row >= 0) {
            Object columnId = getColumnModel().getColumn(column).getIdentifier();
            if (columnId == HomePieceOfFurniture.SortableProperty.VISIBLE) {
              Component visibilityComponent = getCellRenderer(row, column).
                  getTableCellRendererComponent(FurnitureTable.this, getValueAt(row, column), false, false, row, column);
              if (visibilityComponent.isEnabled()) {
                Rectangle cellRect = getCellRect(row, column, false);
                visibilityComponent.setSize(visibilityComponent.getPreferredSize());
                visibilityComponent.setLocation(cellRect.x + (cellRect.width - visibilityComponent.getWidth()) / 2, 
                        cellRect.y + (cellRect.height - visibilityComponent.getHeight()) / 2);
                isVisibleColumn = visibilityComponent.getBounds().contains(ev.getPoint());
              }
            } else if (columnId == HomePieceOfFurniture.SortableProperty.NAME) {
              TableCellRenderer cellRenderer = getCellRenderer(row, column);
              if (cellRenderer instanceof TreeTableNameCellRenderer) {
                Rectangle informationIconBounds = ((TreeTableNameCellRenderer)cellRenderer).
                    getInformationIconBounds(FurnitureTable.this, row, column);
                isInformationIcon = ev.getClickCount() == 1
                    && informationIconBounds != null
                    && informationIconBounds.contains(ev.getPoint());
                if (!isInformationIcon 
                    && getValueAt(row, column) instanceof HomeFurnitureGroup) {
                  Rectangle expandedStateBounds = ((TreeTableNameCellRenderer)cellRenderer).
                      getExpandedStateBounds(FurnitureTable.this, row, column);
                  isGroupExpandIcon = expandedStateBounds.contains(ev.getPoint());
                }
              }
            }
            if (isVisibleColumn) {
              controller.toggleSelectedFurnitureVisibility();
            } else if (isInformationIcon) {
              FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)getModel();
              String information = ((HomePieceOfFurniture)tableModel.getValueAt(row, 0)).getInformation();
              if (furnitureInformationPopup != null
                  && furnitureInformationRow == row) {
                // 클릭 다시 반복하면 정보 삭제
                deleteInformationPopup();
              } else {
                showInformationPopup(information, column, row);
              }
            } else if (isGroupExpandIcon) {
              FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)getModel();
              tableModel.toggleRowExpandedState(row);
              controller.setSelectedFurniture(Arrays.asList(new HomePieceOfFurniture [] {
                  (HomePieceOfFurniture)tableModel.getValueAt(row, 0)}));
            } else if (ev.getClickCount() == 2) {
              deleteInformationPopup();
              controller.modifySelectedFurniture();
            }
          }
          
          if (!isInformationIcon) {
            deleteInformationPopup();
          }
        }
      });
  }

  private void showInformationPopup(String information, int column, int row) {
    if (this.furnitureInformationPopup == null
        || this.furnitureInformationRow != row) {
      deleteInformationPopup();
      
      final JEditorPane informationPane = new JEditorPane("text/html", information);
      informationPane.setEditable(false);
      informationPane.setFocusable(false);
      Font font = getFont();
      String bodyRule = "body { font-family: " + font.getFamily() + "; " 
          + "font-size: " + font.getSize() + "pt; " 
          + "text-align: center; }";
      ((HTMLDocument)informationPane.getDocument()).getStyleSheet().addRule(bodyRule);
      informationPane.addHyperlinkListener(new HyperlinkListener() {
          public void hyperlinkUpdate(HyperlinkEvent ev) {
            if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
              deleteInformationPopup();
              SwingTools.showDocumentInBrowser(ev.getURL()); 
            }
          }
        });
      
      // Reuse tool tip look
      Border border = UIManager.getBorder("ToolTip.border");
      if (!OperatingSystem.isMacOSX()
          || OperatingSystem.isMacOSXLeopardOrSuperior()) {
        border = BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 3, 0, 2));
      }
      informationPane.setBorder(border);
      JToolTip toolTip = new JToolTip();
      toolTip.setComponent(this);
      informationPane.setBackground(toolTip.getBackground().getRGB() == 0 && getBackground().getRGB() != 0
          ? Color.WHITE
          : toolTip.getBackground());
      informationPane.setForeground(toolTip.getForeground());
      informationPane.setSize(informationPane.getPreferredSize());

      // 팝업 정보 보이기
      Rectangle cellRectangle = getCellRect(row, column, true);
      Point p = new Point(cellRectangle.x + cellRectangle.width, cellRectangle.y);
      SwingUtilities.convertPointToScreen(p, this);
      try {
        this.informationPopupRemovalListener = new AWTEventListener() {
            public void eventDispatched(AWTEvent ev) {
              if (ev instanceof KeyEvent) {
                if (((KeyEvent)ev).getKeyCode() == KeyEvent.VK_ESCAPE) {
                  deleteInformationPopup();
                  ((KeyEvent)ev).consume();
                }
              } else if (ev.getID() != WindowEvent.WINDOW_OPENED  // Fired at first popup instantiation
                         && (!(ev instanceof MouseEvent)
                             || (ev.getSource() != FurnitureTable.this
                                 && ev.getSource() != informationPane))) {
                deleteInformationPopup();                
              }
            }
          };
        getToolkit().addAWTEventListener(this.informationPopupRemovalListener, 
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK 
            | AWTEvent.KEY_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK 
            | AWTEvent.WINDOW_EVENT_MASK | AWTEvent.WINDOW_FOCUS_EVENT_MASK | AWTEvent.WINDOW_STATE_EVENT_MASK);
        this.furnitureInformationPopup = 
            PopupFactory.getSharedInstance().getPopup(this, informationPane,  p.x, p.y);
        this.furnitureInformationPopup.show();
        this.furnitureInformationRow = row;
      } catch (SecurityException ex) {
      }
    }
  }

  // 팝업 정보 삭제
  public void deleteInformationPopup() {
    if (this.furnitureInformationPopup != null) {
      getToolkit().removeAWTEventListener(this.informationPopupRemovalListener);
      this.furnitureInformationPopup.hide();
      this.furnitureInformationPopup = null;
    }
  }

 
  private void addUserPreferencesListener(UserPreferences preferences) {
    preferences.addPropertyChangeListener(
        UserPreferences.Property.UNIT, new UserPreferencesChangeListener(this));
    preferences.addPropertyChangeListener(
        UserPreferences.Property.LANGUAGE, new UserPreferencesChangeListener(this));
  }

 
  private static class UserPreferencesChangeListener implements PropertyChangeListener {
    private WeakReference<FurnitureTable>  furnitureTable;

    public UserPreferencesChangeListener(FurnitureTable furnitureTable) {
      this.furnitureTable = new WeakReference<FurnitureTable>(furnitureTable);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      FurnitureTable furnitureTable = this.furnitureTable.get();
      if (furnitureTable == null) {
        ((UserPreferences)ev.getSource()).removePropertyChangeListener(
            UserPreferences.Property.valueOf(ev.getPropertyName()), this);
      } else {
        furnitureTable.repaint();
        furnitureTable.getTableHeader().repaint();
      }
    }
  }

  private void addHomeListener(final Home home, 
                               final FurnitureController controller) {
    PropertyChangeListener sortListener = 
      new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent ev) {
          ((FurnitureTreeTableModel)getModel()).filterAndSortFurniture();
          updateTableSelectedFurniture(home);
          storeExpandedRows(home, controller);
          getTableHeader().repaint();
        }
      };
    home.addPropertyChangeListener(Home.Property.FURNITURE_SORTED_PROPERTY, sortListener);
    home.addPropertyChangeListener(Home.Property.FURNITURE_DESCENDING_SORTED, sortListener);
    
    final PropertyChangeListener changeListener = 
      new PropertyChangeListener () {
        public void propertyChange(PropertyChangeEvent ev) {
          ((FurnitureTreeTableModel)getModel()).filterAndSortFurniture();
          updateTableSelectedFurniture(home);
          storeExpandedRows(home, controller);
        }
      };
    for (HomePieceOfFurniture piece : home.getFurniture()) {
      piece.addPropertyChangeListener(changeListener);
      if (piece instanceof HomeFurnitureGroup) {
        for (HomePieceOfFurniture childPiece : ((HomeFurnitureGroup)piece).getAllFurniture()) {
          childPiece.addPropertyChangeListener(changeListener);
        }
      }
    }
    home.addFurnitureListener(new CollectionListener<HomePieceOfFurniture>() {
      public void collectionChanged(CollectionEvent<HomePieceOfFurniture> ev) {
          HomePieceOfFurniture piece = ev.getItem();
          if (ev.getType() == CollectionEvent.Type.ADD) {
            piece.addPropertyChangeListener(changeListener);
            if (piece instanceof HomeFurnitureGroup) {
              for (HomePieceOfFurniture childPiece : ((HomeFurnitureGroup)piece).getAllFurniture()) {
                childPiece.addPropertyChangeListener(changeListener);
              }
            }
          } else {
            piece.removePropertyChangeListener(changeListener);
            if (piece instanceof HomeFurnitureGroup) {
              for (HomePieceOfFurniture childPiece : ((HomeFurnitureGroup)piece).getAllFurniture()) {
                childPiece.removePropertyChangeListener(changeListener);
              }
            }
          }
        }
      });
    for (Level level : home.getLevels()) {
      level.addPropertyChangeListener(changeListener);
    }
    home.addLevelsListener(new CollectionListener<Level>() {
        public void collectionChanged(CollectionEvent<Level> ev) {
          if (ev.getType() == CollectionEvent.Type.ADD) {
            ev.getItem().addPropertyChangeListener(changeListener);
          } else {
            ev.getItem().removePropertyChangeListener(changeListener);
          }
        }
      });
  }

  private void makeRowsVisible(int minRow, int maxRow) {
    Rectangle includingRectangle = getCellRect(minRow, 0, true);
    if (minRow != maxRow) {
      includingRectangle = includingRectangle.
          union(getCellRect(maxRow, 0, true));      
    }
    if (getAutoResizeMode() == AUTO_RESIZE_OFF) {
      int lastColumn = getColumnCount() - 1;
      includingRectangle = includingRectangle.
          union(getCellRect(minRow, lastColumn, true));
      if (minRow != maxRow) {
        includingRectangle = includingRectangle.
            union(getCellRect(maxRow, lastColumn, true));      
      }
    }
    scrollRectToVisible(includingRectangle);
  }

  private void addTableHeaderListener(final FurnitureController controller) {
    getTableHeader().addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          int columnIndex = getTableHeader().columnAtPoint(ev.getPoint());
          Object columnIdentifier = getColumnModel().getColumn(columnIndex).getIdentifier();
          if (columnIdentifier instanceof HomePieceOfFurniture.SortableProperty) {
            controller.sortFurniture((HomePieceOfFurniture.SortableProperty)columnIdentifier);
          }
        }
      });
  }


  private void addTableColumnModelListener(final FurnitureController controller) {
    // 보이는 속성 업데이트
    getColumnModel().addColumnModelListener(new TableColumnModelListener() {
        public void columnAdded(TableColumnModelEvent ev) {
        }
  
        public void columnMarginChanged(ChangeEvent ev) {
        }
  
        public void columnMoved(TableColumnModelEvent ev) {
          getColumnModel().removeColumnModelListener(this);
          // 가구 보이는 속성 리스트 생성
          List<HomePieceOfFurniture.SortableProperty> furnitureVisibleProperties = 
              new ArrayList<HomePieceOfFurniture.SortableProperty>();
          for (Enumeration<TableColumn> it = getColumnModel().getColumns(); it.hasMoreElements(); ) {
            Object columnIdentifier = it.nextElement().getIdentifier();
            if (columnIdentifier instanceof HomePieceOfFurniture.SortableProperty) {
              furnitureVisibleProperties.add((HomePieceOfFurniture.SortableProperty)columnIdentifier);
            }
          }
          controller.setFurnitureVisibleProperties(furnitureVisibleProperties);
          getColumnModel().addColumnModelListener(this);
        }
  
        public void columnRemoved(TableColumnModelEvent ev) {
        }
  
        public void columnSelectionChanged(ListSelectionEvent ev) {
        }
      });
  }


  public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
    DefaultTableColumnModel printableColumnModel = new DefaultTableColumnModel();
    TableColumnModel columnModel = getColumnModel();
    final DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
    defaultRenderer.setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
    TableCellRenderer printableHeaderRenderer = new TableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value, 
                                   boolean isSelected, boolean hasFocus, int row, int column) {
          JLabel headerRendererLabel = (JLabel)defaultRenderer.getTableCellRendererComponent(table, value, 
              isSelected, hasFocus, row, column);
              headerRendererLabel.setIcon(null);
          headerRendererLabel.setBackground(Color.LIGHT_GRAY);
          headerRendererLabel.setForeground(Color.BLACK);
          headerRendererLabel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(Color.BLACK),
              headerRendererLabel.getBorder()));
          return headerRendererLabel;
        }
      };
    for (int columnIndex = 0, n = columnModel.getColumnCount(); columnIndex < n; columnIndex++) {
      final TableColumn tableColumn = columnModel.getColumn(columnIndex);
      TableColumn printableColumn = new TableColumn();
      printableColumn.setIdentifier(tableColumn.getIdentifier());
      printableColumn.setHeaderValue(tableColumn.getHeaderValue());
      TableCellRenderer printableCellRenderer = new TableCellRenderer() {
          public Component getTableCellRendererComponent(JTable table, Object value, 
                                 boolean isSelected, boolean hasFocus, int row, int column) {
            TableCellRenderer cellRenderer = tableColumn.getCellRenderer();
            Component rendererComponent = cellRenderer.getTableCellRendererComponent(table, value, 
                isSelected, hasFocus, row, column);
            if (rendererComponent instanceof JCheckBox) {
              rendererComponent = defaultRenderer.getTableCellRendererComponent(table, 
                  ((JCheckBox)rendererComponent).isSelected() ? "x" : "", false, false, row, column);
            }
            rendererComponent.setBackground(Color.WHITE);
            rendererComponent.setForeground(Color.BLACK);
            return rendererComponent;
          }
        };
      printableColumn.setCellRenderer(printableCellRenderer);
      printableColumn.setHeaderRenderer(printableHeaderRenderer);
      printableColumnModel.addColumn(printableColumn);
    }    
    return print(g, pageFormat, pageIndex, printableColumnModel, Color.BLACK);
  }

 
  private int print(final Graphics g, 
                    final PageFormat pageFormat, 
                    final int pageIndex, 
                    final TableColumnModel printableColumnModel,
                    final Color gridColor) throws PrinterException {
    if (EventQueue.isDispatchThread()) {
      TableColumnModel oldColumnModel = getColumnModel();
      Color oldGridColor = getGridColor();
      setColumnModel(printableColumnModel);   
      if (OperatingSystem.isWindows()) {
        updateTableColumnsWidth(3);
      } else {
        updateTableColumnsWidth(0);
      }
      setGridColor(gridColor);
      Printable printable = getPrintable(PrintMode.FIT_WIDTH, null, null);
      int pageExists = printable.print(g, pageFormat, pageIndex);
      // Restore column model and grid color to their previous values
      setColumnModel(oldColumnModel);
      setGridColor(oldGridColor);
      return pageExists;
    } else {
      class RunnableContext {
        int pageExists;
        PrinterException exception;
      }
      
      final RunnableContext context = new RunnableContext();
      try {
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
              try {
                context.pageExists = print(g, pageFormat, pageIndex, printableColumnModel, gridColor);
              } catch (PrinterException ex) {
                context.exception = ex;
              }
            }
          });
        if (context.exception != null) {
          throw context.exception;
        }
        return context.pageExists;
      } catch (InterruptedException ex) {
        throw new InterruptedPrinterException("Print interrupted");
      } catch (InvocationTargetException ex) {
        if (ex.getCause() instanceof RuntimeException) {
          throw (RuntimeException)ex.getCause();
        } else {
          throw (Error)ex.getCause();
        }
      }
    }
  }
  
  public void exportToCSV(Writer writer, char fieldSeparator) throws IOException {
    exportHeaderToCSV(writer, fieldSeparator);
    for (int row = 0, n = getRowCount(); row < n; row++) {
      exportRowToCSV(writer, fieldSeparator, row);
    }
  }
  
  private void exportHeaderToCSV(Writer writer, char fieldSeparator) throws IOException {
    TableColumnModel columnModel = getColumnModel();
    for (int columnIndex = 0, n = columnModel.getColumnCount(); columnIndex < n; columnIndex++) {
      if (columnIndex > 0) {
        writer.write(fieldSeparator);
      }
      writer.write(String.valueOf(columnModel.getColumn(columnIndex).getHeaderValue()));
    }
    writer.write(System.getProperty("line.separator"));
  }

  private void exportRowToCSV(Writer writer, char fieldSeparator, int rowIndex)
      throws IOException {
    TableModel model = getModel();
    HomePieceOfFurniture copiedPiece = (HomePieceOfFurniture)model.getValueAt(rowIndex, 0);
    Format sizeFormat;
    if (this.preferences.getLengthUnit() == LengthUnit.INCH) {
      sizeFormat = LengthUnit.INCH_DECIMALS.getFormat();
    } else {
      sizeFormat = this.preferences.getLengthUnit().getFormat();
    }
    
    TableColumnModel columnModel = getColumnModel();
    for (int columnIndex = 0, n = columnModel.getColumnCount(); columnIndex < n; columnIndex++) {
      if (columnIndex > 0) {
        writer.write(fieldSeparator);
      }
      TableColumn column = columnModel.getColumn(columnIndex);
      Object columnIdentifier = column.getIdentifier();
      if (columnIdentifier instanceof HomePieceOfFurniture.SortableProperty) {
        switch ((HomePieceOfFurniture.SortableProperty)columnIdentifier) {
          case CATALOG_ID :
            String catalogId = copiedPiece.getCatalogId();
            writer.write(catalogId != null ? catalogId : "");
            break;
          case NAME :
            writer.write(copiedPiece.getName());
            break;
          case LEVEL :
            writer.write(copiedPiece.getLevel() != null 
                ? copiedPiece.getLevel().getName() 
                : "");
            break;
          case COLOR :
            if (copiedPiece.getColor() != null) {
              writer.write("#" + Integer.toHexString(copiedPiece.getColor()).substring(2));
            }
            break;
          case TEXTURE :
            if (copiedPiece.getTexture() != null) {
              writer.write(copiedPiece.getTexture().getName());
            }
            break;
          case WIDTH :
            writer.write(sizeFormat.format(copiedPiece.getWidth()));
            break;
          case DEPTH :
            writer.write(sizeFormat.format(copiedPiece.getDepth()));
            break;
          case HEIGHT : 
            writer.write(sizeFormat.format(copiedPiece.getHeight()));
            break;
          case X : 
            writer.write(sizeFormat.format(copiedPiece.getX()));
            break;
          case Y :
            writer.write(sizeFormat.format(copiedPiece.getY()));
            break;
          case ELEVATION : 
            writer.write(sizeFormat.format(copiedPiece.getElevation()));
            break;
          case ANGLE :
          case PRICE : 
          case VALUE_ADDED_TAX_PERCENTAGE : 
          case VALUE_ADDED_TAX :
          case PRICE_VALUE_ADDED_TAX_INCLUDED : 
            String text = ((JLabel)column.getCellRenderer().getTableCellRendererComponent(
                this, copiedPiece, false, false, rowIndex, columnIndex)).getText();
            if (text != null) {
              writer.write(text);
            }
            break;
          case MOVABLE :
            writer.write(String.valueOf(copiedPiece.isMovable()));
            break;
          case DOOR_OR_WINDOW : 
            writer.write(String.valueOf(copiedPiece.isDoorOrWindow()));
            break;
          case VISIBLE :
            writer.write(String.valueOf(copiedPiece.isVisible()));
            break;
        }
      } else {
        Component rendererComponent = column.getCellRenderer().getTableCellRendererComponent(
            this, copiedPiece, false, false, rowIndex, columnIndex);
        if (rendererComponent instanceof JLabel) {
          String text = ((JLabel)rendererComponent).getText();
          if (text != null) {
            writer.write(text);
          }
        } else {
          writer.write(String.valueOf(model.getValueAt(rowIndex, columnIndex)));
        }
      }  
    }
    writer.write(System.getProperty("line.separator"));
  }
  
   public String getClipboardCSV() {
    StringWriter writer = new StringWriter();
    try {
      exportHeaderToCSV(writer, '\t');
      for (int row : getSelectedRows()) {
        exportRowToCSV(writer, '\t', row);
      }
    } catch (IOException ex) {
    }
    return writer.toString();
  }
  
 
  public void setFurnitureFilter(FurnitureTable.FurnitureFilter filter) {
    FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)getModel();
    tableModel.setFurnitureFilter(filter);
  }
  
  public FurnitureTable.FurnitureFilter getFurnitureFilter() {
    FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)getModel();
    return tableModel.getFurnitureFilter();
  }
  
  private static class FurnitureTableColumnModel extends DefaultTableColumnModel {
    private Map<HomePieceOfFurniture.SortableProperty, TableColumn> availableColumns;

    public FurnitureTableColumnModel(Home home, UserPreferences preferences) {
      createAvailableColumns(home, preferences);
      addHomeListener(home);
      addLanguageListener(preferences);
      updateModelColumns(home.getFurnitureVisibleProperties());
    }
    private void createAvailableColumns(Home home, UserPreferences preferences) {
      this.availableColumns = new HashMap<HomePieceOfFurniture.SortableProperty, TableColumn>();
      TableCellRenderer headerRenderer = getHeaderRenderer(home);
      for (HomePieceOfFurniture.SortableProperty columnProperty : HomePieceOfFurniture.SortableProperty.values()) {
        TableColumn tableColumn = new TableColumn();
        tableColumn.setIdentifier(columnProperty);
        tableColumn.setHeaderValue(getColumnName(columnProperty, preferences));
        tableColumn.setCellRenderer(getColumnRenderer(columnProperty, preferences));
        tableColumn.setPreferredWidth(getColumnPreferredWidth(columnProperty));
        tableColumn.setHeaderRenderer(headerRenderer);
        this.availableColumns.put(columnProperty, tableColumn);
      }
    }

    private void addHomeListener(final Home home) {
      home.addPropertyChangeListener(Home.Property.FURNITURE_VISIBLE_PROPERTIES, 
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              updateModelColumns(home.getFurnitureVisibleProperties());
            }
          });
    }
   
    private void addLanguageListener(UserPreferences preferences) {
      preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE, 
          new LanguageChangeListener(this));
    }

    private static class LanguageChangeListener implements PropertyChangeListener {
      private WeakReference<FurnitureTableColumnModel> furnitureTableColumnModel;

      public LanguageChangeListener(FurnitureTableColumnModel furnitureTable) {
        this.furnitureTableColumnModel = new WeakReference<FurnitureTableColumnModel>(furnitureTable);
      }
      
      public void propertyChange(PropertyChangeEvent ev) {
        FurnitureTableColumnModel furnitureTableColumnModel = this.furnitureTableColumnModel.get();
        UserPreferences preferences = (UserPreferences)ev.getSource();
        if (furnitureTableColumnModel == null) {
          preferences.removePropertyChangeListener(
              UserPreferences.Property.LANGUAGE, this);
        } else {
          for (TableColumn tableColumn : furnitureTableColumnModel.availableColumns.values()) {
            HomePieceOfFurniture.SortableProperty columnIdentifier = 
                (HomePieceOfFurniture.SortableProperty)tableColumn.getIdentifier();
            tableColumn.setHeaderValue(furnitureTableColumnModel.getColumnName(columnIdentifier, preferences));
            tableColumn.setCellRenderer(furnitureTableColumnModel.getColumnRenderer(columnIdentifier, preferences));
          }
        }
      }
    }
    
    // 업데이트 가구 리스트
    private void updateModelColumns(List<HomePieceOfFurniture.SortableProperty> furnitureVisibleProperties) {
      for (int i = this.tableColumns.size() - 1; i >= 0; i--) {
        TableColumn tableColumn = this.tableColumns.get(i);
        Object columnIdentifier = tableColumn.getIdentifier();
        if ((columnIdentifier instanceof HomePieceOfFurniture.SortableProperty)
            && !furnitureVisibleProperties.contains(columnIdentifier)) {
          removeColumn(tableColumn);
        } 
      }
      for (HomePieceOfFurniture.SortableProperty visibleProperty : furnitureVisibleProperties) {
        TableColumn tableColumn = this.availableColumns.get(visibleProperty);
        if (!this.tableColumns.contains(tableColumn)) {
          addColumn(tableColumn);
        }
      }
      for (int i = 0, n = furnitureVisibleProperties.size(); i < n; i++) {
        TableColumn tableColumn = this.availableColumns.get(furnitureVisibleProperties.get(i));
        int tableColumnIndex = this.tableColumns.indexOf(tableColumn);
        if (tableColumnIndex != i) {
          moveColumn(tableColumnIndex, i);
        }
      }
    }
    private String getColumnName(HomePieceOfFurniture.SortableProperty property, 
                                 UserPreferences preferences) {
      switch (property) {
        case CATALOG_ID :
          return preferences.getLocalizedString(FurnitureTable.class, "catalogIdColumn");
        case NAME :
          return preferences.getLocalizedString(FurnitureTable.class, "nameColumn");
        case WIDTH :
          return preferences.getLocalizedString(FurnitureTable.class, "widthColumn");
        case DEPTH :
          return preferences.getLocalizedString(FurnitureTable.class, "depthColumn");
        case HEIGHT : 
          return preferences.getLocalizedString(FurnitureTable.class, "heightColumn");
        case X : 
          return preferences.getLocalizedString(FurnitureTable.class, "xColumn");
        case Y :
          return preferences.getLocalizedString(FurnitureTable.class, "yColumn");
        case ELEVATION : 
          return preferences.getLocalizedString(FurnitureTable.class, "elevationColumn");
        case ANGLE :
          return preferences.getLocalizedString(FurnitureTable.class, "angleColumn");
        case LEVEL :
          return preferences.getLocalizedString(FurnitureTable.class, "levelColumn");
        case COLOR :
          return preferences.getLocalizedString(FurnitureTable.class, "colorColumn");
        case TEXTURE :
          return preferences.getLocalizedString(FurnitureTable.class, "textureColumn");
        case MOVABLE :
          return preferences.getLocalizedString(FurnitureTable.class, "movableColumn");
        case DOOR_OR_WINDOW : 
          return preferences.getLocalizedString(FurnitureTable.class, "doorOrWindowColumn");
        case VISIBLE :
          return preferences.getLocalizedString(FurnitureTable.class, "visibleColumn");
        case PRICE :
          return preferences.getLocalizedString(FurnitureTable.class, "priceColumn");          
        case VALUE_ADDED_TAX_PERCENTAGE :
          return preferences.getLocalizedString(FurnitureTable.class, "valueAddedTaxPercentageColumn");          
        case VALUE_ADDED_TAX :
          return preferences.getLocalizedString(FurnitureTable.class, "valueAddedTaxColumn");          
        case PRICE_VALUE_ADDED_TAX_INCLUDED :
          return preferences.getLocalizedString(FurnitureTable.class, "priceValueAddedTaxIncludedColumn");          
        default :
          throw new IllegalArgumentException("Unknown column name " + property);
      }
    }

    private int getColumnPreferredWidth(HomePieceOfFurniture.SortableProperty property) {
      switch (property) {
        case CATALOG_ID :
        case NAME :
          return 120; 
        case WIDTH :
        case DEPTH :
        case HEIGHT : 
        case X : 
        case Y :
        case ELEVATION : 
          return 50;
        case ANGLE :
          return 35;        
        case LEVEL :
          return 70;        
        case COLOR :
        case TEXTURE :
          return 30;        
        case MOVABLE :
        case DOOR_OR_WINDOW : 
        case VISIBLE :
          return 20;
        case PRICE :
        case VALUE_ADDED_TAX_PERCENTAGE :
        case VALUE_ADDED_TAX :
        case PRICE_VALUE_ADDED_TAX_INCLUDED :
          return 70;          
        default :
          throw new IllegalArgumentException("Unknown column name " + property);
      }
    }

    private TableCellRenderer getColumnRenderer(HomePieceOfFurniture.SortableProperty property, 
                                                UserPreferences preferences) {
      switch (property) {
        case CATALOG_ID :
          return getCatalogIdRenderer(); 
        case NAME :
          return getNameWithIconRenderer(); 
        case WIDTH :
          return getSizeRenderer(HomePieceOfFurniture.SortableProperty.WIDTH, preferences);
        case DEPTH :
          return getSizeRenderer(HomePieceOfFurniture.SortableProperty.DEPTH, preferences);
        case HEIGHT : 
          return getSizeRenderer(HomePieceOfFurniture.SortableProperty.HEIGHT, preferences);
        case X : 
          return getSizeRenderer(HomePieceOfFurniture.SortableProperty.X, preferences);
        case Y :
          return getSizeRenderer(HomePieceOfFurniture.SortableProperty.Y, preferences);
        case ELEVATION : 
          return getSizeRenderer(HomePieceOfFurniture.SortableProperty.ELEVATION, preferences);
        case ANGLE :
          return getAngleRenderer();        
        case LEVEL :
          return getLevelRenderer();        
        case COLOR :
          return getColorRenderer();        
        case TEXTURE :
          return getTextureRenderer();        
        case MOVABLE :
          return getBooleanRenderer(HomePieceOfFurniture.SortableProperty.MOVABLE);
        case DOOR_OR_WINDOW : 
          return getBooleanRenderer(HomePieceOfFurniture.SortableProperty.DOOR_OR_WINDOW);
        case VISIBLE :
          return getBooleanRenderer(HomePieceOfFurniture.SortableProperty.VISIBLE);
        case PRICE :
          return getPriceRenderer(HomePieceOfFurniture.SortableProperty.PRICE, preferences);          
        case VALUE_ADDED_TAX_PERCENTAGE :
          return getValueAddedTaxPercentageRenderer();          
        case VALUE_ADDED_TAX :
          return getPriceRenderer(HomePieceOfFurniture.SortableProperty.VALUE_ADDED_TAX, preferences);          
        case PRICE_VALUE_ADDED_TAX_INCLUDED :
          return getPriceRenderer(HomePieceOfFurniture.SortableProperty.PRICE_VALUE_ADDED_TAX_INCLUDED, preferences);          
        default :
          throw new IllegalArgumentException("Unknown column name " + property);
      }
    }

    private TableCellRenderer getCatalogIdRenderer() {
      return new DefaultTableCellRenderer() { 
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          return super.getTableCellRendererComponent(table, 
              value != null  ? ((HomePieceOfFurniture)value).getCatalogId()  : null, 
              isSelected, hasFocus, row, column); 
        }
      };
    }

   private TableCellRenderer getLevelRenderer() {
      return new DefaultTableCellRenderer() { 
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          HomePieceOfFurniture piece = (HomePieceOfFurniture)value; 
          Level level = value != null 
              ? piece.getLevel()
              : null;
          return super.getTableCellRendererComponent(
              table, level != null  ? level.getName()  : null, isSelected, hasFocus, row, column); 
        }
      };
    }
    
    private TableCellRenderer getNameWithIconRenderer() {
      return new TreeTableNameCellRenderer();
    }
    
    private TableCellRenderer getSizeRenderer(HomePieceOfFurniture.SortableProperty property,
                                              final UserPreferences preferences) {
      class SizeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          if (value != null) {
            value = preferences.getLengthUnit().getFormat().format((Float)value);
          }
          setHorizontalAlignment(JLabel.RIGHT);
          return super.getTableCellRendererComponent(
              table, value, isSelected, hasFocus, row, column);
        }
      };
      
      switch (property) {
        case WIDTH :
          return new SizeRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).getWidth()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        case DEPTH :
          return new SizeRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).getDepth()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        case HEIGHT :
          return new SizeRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).getHeight()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        case X :
          return new SizeRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).getX()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        case Y :
          return new SizeRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).getY()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        case ELEVATION :
          return new SizeRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).getElevation()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        default :
          throw new IllegalArgumentException(property + " column not a size column");
      }
    }
    
    private TableCellRenderer getPriceRenderer(HomePieceOfFurniture.SortableProperty property,
                                               final UserPreferences preferences) {
      class PriceRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, 
             BigDecimal price, String currency, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          String defaultCurrency = preferences.getCurrency();
          String value;
          if (price != null && defaultCurrency != null) {
            NumberFormat currencyFormat = DecimalFormat.getCurrencyInstance();
            currencyFormat.setCurrency(Currency.getInstance(currency != null ? currency : defaultCurrency));
            value = currencyFormat.format(price);
          } else {
            value = null;
          }
          setHorizontalAlignment(JLabel.RIGHT);
          return super.getTableCellRendererComponent(
              table, value, isSelected, hasFocus, row, column);
        }
      };
      
      switch (property) {
        case PRICE :
          return new PriceRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                HomePieceOfFurniture piece = (HomePieceOfFurniture)value;
                BigDecimal price;
                String currency;
                if (value != null) {
                  price = piece.getPrice();
                  currency = piece.getCurrency();
                } else {
                  price = null;
                  currency = null;
                }
                return super.getTableCellRendererComponent(table, 
                    price, currency, isSelected, hasFocus, row, column);
              }
            };
        case VALUE_ADDED_TAX :
          return new PriceRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                HomePieceOfFurniture piece = (HomePieceOfFurniture)value;
                BigDecimal valueAddedTax;
                String currency;
                if (value != null) {
                  valueAddedTax = piece.getValueAddedTax();
                  currency = piece.getCurrency();
                } else {
                  valueAddedTax = null;
                  currency = null;
                }
                return super.getTableCellRendererComponent(table, 
                    valueAddedTax, currency, isSelected, hasFocus, row, column);
              }
            };
        case PRICE_VALUE_ADDED_TAX_INCLUDED :
          return new PriceRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                HomePieceOfFurniture piece = (HomePieceOfFurniture)value;
                BigDecimal priceValueAddedTaxIncluded;
                String currency;
                if (value != null) {
                  priceValueAddedTaxIncluded = piece.getPriceValueAddedTaxIncluded();
                  currency = piece.getCurrency();
                } else {
                  priceValueAddedTaxIncluded = null;
                  currency = null;
                }
                return super.getTableCellRendererComponent(table, 
                    priceValueAddedTaxIncluded, currency, isSelected, hasFocus, row, column);
              }
            };
        default :
          throw new IllegalArgumentException(property + " column not a price column");
      }
    }
   
    private TableCellRenderer getAngleRenderer() {
      return new DefaultTableCellRenderer() { 
        private TableCellRenderer integerRenderer;
        
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          if (this.integerRenderer == null) {
            this.integerRenderer = table.getDefaultRenderer(Integer.class);
          }
          Integer angle = value != null  
              ? (int)(Math.round(Math.toDegrees(((HomePieceOfFurniture)value).getAngle()) + 360) % 360)
              : null;
          return this.integerRenderer.getTableCellRendererComponent(
              table, angle, isSelected, hasFocus, row, column); 
        }
      };
    }

    private TableCellRenderer getValueAddedTaxPercentageRenderer() {
      return new DefaultTableCellRenderer() { 
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          BigDecimal valueAddedTaxPercentage = value != null  
              ? ((HomePieceOfFurniture)value).getValueAddedTaxPercentage()
              : null;
          if (valueAddedTaxPercentage != null) {
            NumberFormat percentInstance = DecimalFormat.getPercentInstance();
            percentInstance.setMinimumFractionDigits(valueAddedTaxPercentage.scale() - 2);
            value = percentInstance.format(valueAddedTaxPercentage);
          } else {
            value = null;
          }
          setHorizontalAlignment(JLabel.RIGHT);
          return super.getTableCellRendererComponent(
              table, value, isSelected, hasFocus, row, column);
        }
      };
    }

    private TableCellRenderer getColorRenderer() {
      return new DefaultTableCellRenderer() {
        private Icon squareIcon = new Icon () {
          public int getIconHeight() {
            return getFont().getSize();
          }

          public int getIconWidth() {
            return getIconHeight();
          }

          public void paintIcon(Component c, Graphics g, int x, int y) {
            int squareSize = getIconHeight();
            g.setColor(c.getForeground());          
            g.fillRect(x + 2, y + 2, squareSize - 3, squareSize - 3);
            g.setColor(c.getParent().getParent().getForeground());
            g.drawRect(x + 1, y + 1, squareSize - 2, squareSize - 2);
          }
        };
          
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          Integer color = value != null  
              ? ((HomePieceOfFurniture)value).getColor()
              : null;
          JLabel label = (JLabel)super.getTableCellRendererComponent(
              table, color, isSelected, hasFocus, row, column);
          if (color != null) {
            label.setText(null);
            label.setIcon(squareIcon);
            label.setForeground(new Color(color));
          } else {
            if (value != null) {
              label.setText("-");
            }
            label.setIcon(null);
            label.setForeground(table.getForeground());
          }
          label.setHorizontalAlignment(JLabel.CENTER);
          return label;
        } 
      };
    }
   
    private TableCellRenderer getTextureRenderer() {
      return new DefaultTableCellRenderer() { 
        {
          setHorizontalAlignment(CENTER);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, 
             int row, int column) {
          HomePieceOfFurniture piece = (HomePieceOfFurniture)value; 
          JLabel label = (JLabel)super.getTableCellRendererComponent(
              table, null, isSelected, hasFocus, row, column); 
          HomeTexture texture = piece != null  
              ? piece.getTexture()
              : null;
          if (texture != null) {
            Content textureContent = texture.getImage();
            label.setIcon(IconManager.getInstance().getIcon(
                textureContent, table.getRowHeight() - 2, table));
          } else {
            label.setIcon(null);
          }
          return label;
        }
      };
    }

    private TableCellRenderer getBooleanRenderer(HomePieceOfFurniture.SortableProperty property) {
      // Renderer super class used to display booleans
      class BooleanRenderer implements TableCellRenderer {
        private TableCellRenderer booleanRenderer;
        private final boolean enabled;

        public BooleanRenderer(boolean enabled) {
          this.enabled = enabled;
        }

        public Component getTableCellRendererComponent(JTable table, 
             Object value, boolean isSelected, boolean hasFocus, int row, int column) {
          if (this.booleanRenderer == null) {
            this.booleanRenderer = table.getDefaultRenderer(Boolean.class);
          }
          Component component = this.booleanRenderer.getTableCellRendererComponent(
              table, value, isSelected, hasFocus, row, column);
          component.setEnabled(this.enabled);
          return component;
        }
      };
      
      switch (property) {
        case MOVABLE :
          return new BooleanRenderer(false) {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).isMovable()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        case DOOR_OR_WINDOW :
          return new BooleanRenderer(false) {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).isDoorOrWindow()  : null, 
                    isSelected, hasFocus, row, column);
              }
            };
        case VISIBLE :
          return new BooleanRenderer(true) {
              @Override
              public Component getTableCellRendererComponent(JTable table, 
                  Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, 
                    value != null  ? ((HomePieceOfFurniture)value).isVisible()  : null, 
                    isSelected, hasFocus, row, column);
                if (value != null) {
                  FurnitureTreeTableModel tableModel = (FurnitureTreeTableModel)table.getModel();
                  component.setEnabled(tableModel.getIndexOfChild(tableModel.getRoot(), value) != -1);
                }
                return component;
              }
            };
        default :
          throw new IllegalArgumentException(property + " column not a boolean column");
      }
    }
       
    private TableCellRenderer getHeaderRenderer(final Home home) {
      return new TableCellRenderer() {
          private TableCellRenderer headerRenderer;        
          private ImageIcon ascendingSortIcon = new ImageIcon(FurnitureTable.class.getResource("resources/ascending.png"));
          private ImageIcon descendingSortIcon = new ImageIcon(FurnitureTable.class.getResource("resources/descending.png"));
          
          public Component getTableCellRendererComponent(JTable table, 
               Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (this.headerRenderer == null) {
              this.headerRenderer = table.getTableHeader().getDefaultRenderer();
            }
            JLabel label = (JLabel)this.headerRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            if (getColumn(column).getIdentifier().equals(home.getFurnitureSortedProperty())) {
              label.setHorizontalTextPosition(JLabel.LEADING);
              if (home.isFurnitureDescendingSorted()) {
                label.setIcon(descendingSortIcon);
              } else {
                label.setIcon(ascendingSortIcon);
              }
            } else {
              label.setIcon(null);
            }
            return label;
          }
        };
    }
  }
 
  private static class TreeTableNameCellRenderer implements TableCellRenderer {
    private static final ResourceURLContent GROUP_ICON_CONTENT = 
        new ResourceURLContent(FurnitureTable.class, "resources/groupIcon.png");
    private PanelWithInformationIcon groupRendererComponent;
    private JTree                    nameRendererTree;
    private int                      renderedRow;
    private PanelWithInformationIcon noGroupRendererComponent;
    private DefaultTableCellRenderer nameRendererLabel;
    private Font                     defaultFont;
    
    public Component getTableCellRendererComponent(JTable table, 
         Object value, boolean isSelected, boolean hasFocus, 
         int row, int column) {
      if (this.defaultFont == null) {
        this.defaultFont = table.getFont();
      }
      HomePieceOfFurniture piece = (HomePieceOfFurniture)value; 
      boolean containsGroup = false;
      if (piece != null) {
        for (int i = 0; i < table.getRowCount(); i++) {
          if (table.getValueAt(i, 0) instanceof HomeFurnitureGroup) {
            containsGroup = true;
            break;
          }
        }
      }
      if (containsGroup) {
        prepareTree(table);   
        if (this.groupRendererComponent == null) {
          this.groupRendererComponent = new PanelWithInformationIcon();
          this.groupRendererComponent.add(this.nameRendererTree, BorderLayout.CENTER);
        }
        
        this.groupRendererComponent.setInformationIconVisible(piece.getInformation() != null);
        this.groupRendererComponent.setFont(this.defaultFont);
        if (isSelected) {
          this.nameRendererTree.setSelectionRow(row);
          this.groupRendererComponent.setBackground(table.getSelectionBackground());
        } else {
          this.nameRendererTree.clearSelection();
          this.groupRendererComponent.setBackground(table.getBackground());
        }
        this.renderedRow = row;
        
        return this.groupRendererComponent;
      } else {
        if (this.noGroupRendererComponent == null) {
          this.nameRendererLabel = new DefaultTableCellRenderer();
          this.noGroupRendererComponent = new PanelWithInformationIcon();
          this.noGroupRendererComponent.add(this.nameRendererLabel, BorderLayout.CENTER);
        }

        String pieceName = piece != null  ? piece.getName()  : null;
        this.nameRendererLabel.getTableCellRendererComponent(table,
              pieceName, isSelected, hasFocus, row, column);
        if (piece != null) {
          Content iconContent;
          if (piece instanceof HomeFurnitureGroup) {
            iconContent = GROUP_ICON_CONTENT;
          } else {
            iconContent = piece.getIcon();
          }
          this.nameRendererLabel.setIcon(IconManager.getInstance().getIcon(
              iconContent, table.getRowHeight() - table.getRowMargin(), table));
  
          this.noGroupRendererComponent.setInformationIconVisible(piece.getInformation() != null);
        } else {
          this.nameRendererLabel.setIcon(null);
          this.noGroupRendererComponent.setInformationIconVisible(false);
        }
        this.noGroupRendererComponent.setBackground(this.nameRendererLabel.getBackground());
        this.noGroupRendererComponent.setBorder(this.nameRendererLabel.getBorder());
        this.nameRendererLabel.setBorder(null);
        return this.noGroupRendererComponent;
      }
    }

    private void prepareTree(final JTable table) {
      if (this.nameRendererTree == null) {
        UIManager.put("Tree.rendererFillBackground", Boolean.TRUE);
        final DefaultTreeCellRenderer treeCellRenderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, 
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
              if (value instanceof HomePieceOfFurniture) {
                HomePieceOfFurniture piece = (HomePieceOfFurniture)value; 
                super.getTreeCellRendererComponent(tree, piece.getName(), isSelected, expanded, leaf, row, false); 
                Content iconContent;
                if (piece instanceof HomeFurnitureGroup) {
                  iconContent = GROUP_ICON_CONTENT;
                } else {
                  iconContent = piece.getIcon();
                }
                setIcon(IconManager.getInstance().getIcon(iconContent, table.getRowHeight() - table.getRowMargin(), table)); 
                setBackgroundSelectionColor(table.getSelectionBackground());
                setBackgroundNonSelectionColor(table.getBackground());
                setTextSelectionColor(table.getSelectionForeground());
                setTextNonSelectionColor(table.getForeground());
              }
              return this;
            }
            
            @Override
            public void setBounds(int x, int y, int width, int height) {
              super.setBounds(x, y, nameRendererTree.getWidth() - x, height); 
            }
          };
          
        final FurnitureTreeTableModel tableTreeModel = (FurnitureTreeTableModel)table.getModel();
        this.nameRendererTree = new JTree(tableTreeModel) {
            boolean drawing = false;
            
            public void setBounds(int x, int y, int width, int height) {
              super.setBounds(x, 0, width, table.getHeight());
            }

            public void paint(Graphics g) {
              if (table.getRowMargin() > 0) {
                Rectangle clipBounds = g.getClipBounds();
                g.clipRect(clipBounds.x, clipBounds.y, clipBounds.width, getRowHeight() - table.getRowMargin());
              }
              g.translate(0, -renderedRow * getRowHeight());
              this.drawing = true;
              super.paint(g);
              this.drawing = false;
            }

            @Override
            public TreeCellRenderer getCellRenderer() {
              return treeCellRenderer;
            }
            
            @Override
            public boolean hasFocus() {
              if (this.drawing 
                  && UIManager.getLookAndFeel() instanceof SynthLookAndFeel) {
                return  true;
              } else {
                return super.hasFocus();
              }
            }
          };
        this.nameRendererTree.setOpaque(false);
        this.nameRendererTree.setRowHeight(table.getRowHeight());
        this.nameRendererTree.setRootVisible(false);
        this.nameRendererTree.setShowsRootHandles(true);
        updateExpandedRows(tableTreeModel);
        tableTreeModel.addTreeModelListener(new TreeModelListener() {
            public void treeStructureChanged(TreeModelEvent ev) {
              updateExpandedRows(tableTreeModel);
            }
            
            public void treeNodesRemoved(TreeModelEvent ev) {
            }
            
            public void treeNodesInserted(TreeModelEvent ev) {
            }
            
            public void treeNodesChanged(TreeModelEvent ev) {
            }
          });
      }
    }

    private void updateExpandedRows(FurnitureTreeTableModel tableTreeModel) {
      for (int row = 0; row < tableTreeModel.getRowCount(); row++) {
        if (tableTreeModel.getValueAt(row, 0) instanceof HomeFurnitureGroup) {
          if (tableTreeModel.isRowExpanded(row)) {
            TreePath pathForRow = this.nameRendererTree.getPathForRow(row);
            if (this.nameRendererTree.isCollapsed(pathForRow)) {
              this.nameRendererTree.expandPath(pathForRow); 
            }
          } else {
            TreePath pathForRow = this.nameRendererTree.getPathForRow(row);
            if (this.nameRendererTree.isExpanded(pathForRow)) {
              this.nameRendererTree.collapsePath(pathForRow);
            }
          }
        }
      }
    }    
    
    public Rectangle getExpandedStateBounds(JTable table, int row, int column) {
      prepareTree(table);
      Rectangle cellBounds = table.getCellRect(row, column, true);
      Rectangle pathBounds = this.nameRendererTree.getPathBounds(this.nameRendererTree.getPathForRow(row));
      cellBounds.width = pathBounds.x;
      return cellBounds;
    }
        
    public Rectangle getInformationIconBounds(JTable table, int row, int column) {
      Component component = getTableCellRendererComponent(table, table.getValueAt(row, column), false, false, row, column);
      if (component instanceof PanelWithInformationIcon) {
        Rectangle informationIconBounds = ((PanelWithInformationIcon)component).getInformationIconBounds();
        if (informationIconBounds != null) {
          Rectangle rectangle = table.getCellRect(row, column, false);
          informationIconBounds.translate(rectangle.x, rectangle.y);
          return informationIconBounds;
        }
      }
      return null;
    }
        
    private static class PanelWithInformationIcon extends JPanel {
      private static final ImageIcon INFORMATION_ICON = 
          SwingTools.getScaledImageIcon(FurnitureTable.class.getResource("resources/furnitureInformation.png"));
      private JLabel informationLabel;
      
      public PanelWithInformationIcon() {
        super(new BorderLayout());
        this.informationLabel = new JLabel(INFORMATION_ICON) {
          @Override
          public void print(Graphics g) {
          }
        };
        add(this.informationLabel, BorderLayout.LINE_END);
      }
      
      @Override
      public void revalidate() {      
      }
      
      @Override
      public void repaint(long tm, int x, int y, int width, int height) {
      }
      
      @Override
      public void repaint() {      
      }
      
      public void setInformationIconVisible(boolean visible) {
        this.informationLabel.setVisible(visible);
      }
      
      public Rectangle getInformationIconBounds() {
        if (this.informationLabel.isVisible()) {
          return this.informationLabel.getBounds();
        } else {
          return null;
        }
      }
      
      @Override
      public void setFont(Font font) {
        super.setFont(font);
        for (int i = 0, n = getComponentCount(); i < n; i++) {
          getComponent(i).setFont(font);
        }
      }
    }
  }
  
  // 모델 리스트
  private static class FurnitureTreeTableModel extends AbstractTableModel implements TreeModel {
    private Home                                    home;
    private List<HomePieceOfFurniture>              filteredAndSortedFurniture;
    private FurnitureFilter                         furnitureFilter;
    private Set<HomeFurnitureGroup>                 expandedGroups;
    private List<TreeModelListener>                 treeModelListeners;
    private Map<Object, List<HomePieceOfFurniture>> childFurnitureCache;  
    private boolean                                 containsNotViewableFurniture;
    
    public FurnitureTreeTableModel(Home home) {
      this.home = home;
      this.expandedGroups = new HashSet<HomeFurnitureGroup>();
      this.treeModelListeners = new ArrayList<TreeModelListener>();
      this.childFurnitureCache = new HashMap<Object, List<HomePieceOfFurniture>>();
      addHomeListener(home);
      filterAndSortFurniture();
    }

    private void addHomeListener(final Home home) {
      home.addFurnitureListener(new CollectionListener<HomePieceOfFurniture>() {
          public void collectionChanged(CollectionEvent<HomePieceOfFurniture> ev) {
            HomePieceOfFurniture piece = ev.getItem();
            int pieceIndex = ev.getIndex();
            switch (ev.getType()) {
              case ADD :
                if (!expandedGroups.isEmpty()
                    || containsNotViewableFurniture
                    || pieceIndex < 0) {
                  filterAndSortFurniture();
                } else { 
                  int insertionIndex = getPieceOfFurnitureInsertionIndex(piece, home, pieceIndex);
                  if (insertionIndex != -1) {
                    filteredAndSortedFurniture.add(insertionIndex, piece);
                    fireTableRowsInserted(insertionIndex, insertionIndex);
                    fireTreeModelChanged();
                  }
                }
                break;
              case DELETE :
                if (furnitureFilter != null
                    || pieceIndex < 0) {
                  filterAndSortFurniture();
                } else {
                  int deletionIndex = getPieceOfFurnitureDeletionIndex(piece, home, pieceIndex);
                  if (deletionIndex != -1) {
                    if (expandedGroups.contains(piece)) { 
                      filterAndSortFurniture();
                    } else {
                      filteredAndSortedFurniture.remove(deletionIndex);
                      fireTableRowsDeleted(deletionIndex, deletionIndex);
                      fireTreeModelChanged();
                    }
                  }
                }
                if (piece instanceof HomeFurnitureGroup) {
                  expandedGroups.remove(piece);
                }
                break;
            }
          }
  
          private int getPieceOfFurnitureInsertionIndex(HomePieceOfFurniture piece, Home home, int homePieceIndex) {
            if (furnitureFilter == null) {
              if (home.getFurnitureSortedProperty() == null) {
                return homePieceIndex;
              } 
            } else if (!furnitureFilter.include(home, piece)) {
              return -1;
            } else if (home.getFurnitureSortedProperty() == null) {
              if (homePieceIndex == 0
                  || filteredAndSortedFurniture.size() == 0) {
                return 0;
              } else {
                List<HomePieceOfFurniture> homeFurniture = home.getFurniture();
                int previousIncludedPieceIndex = homePieceIndex - 1;
                while (previousIncludedPieceIndex > 0 
                    && !furnitureFilter.include(home, homeFurniture.get(previousIncludedPieceIndex))) {
                  previousIncludedPieceIndex--;
                }
                return getPieceOfFurnitureIndex(homeFurniture.get(previousIncludedPieceIndex)) + 1;
              }
            }
            
            int sortedIndex = Collections.binarySearch(filteredAndSortedFurniture, piece, getFurnitureComparator(home));
            if (sortedIndex >= 0) {
              return sortedIndex;
            } else {
              return -(sortedIndex + 1);
            }              
          }
            
          private int getPieceOfFurnitureDeletionIndex(HomePieceOfFurniture piece, Home home, int homePieceIndex) {
            if (furnitureFilter == null
                && home.getFurnitureSortedProperty() == null
                && expandedGroups.isEmpty()
                && !containsNotViewableFurniture) {
              return homePieceIndex;
            } 
            return getPieceOfFurnitureIndex(piece);              
          }
        });
      home.addPropertyChangeListener(Home.Property.FURNITURE_VISIBLE_PROPERTIES, new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            if (!home.getFurnitureVisibleProperties().contains(HomePieceOfFurniture.SortableProperty.NAME)) {
              expandedGroups.clear();
              filterAndSortFurniture();
            }
          }
        });
    }

    @Override
    public String getColumnName(int columnIndex) {
      return null;
    }

    public int getColumnCount() {
      return 0;
    }

    public int getRowCount() {
      return this.filteredAndSortedFurniture.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      return this.filteredAndSortedFurniture.get(rowIndex);
    }
    
    public int getPieceOfFurnitureIndex(HomePieceOfFurniture piece) {
      return this.filteredAndSortedFurniture.indexOf(piece);
    }

    public void filterAndSortFurniture() {
      int previousRowCount = this.filteredAndSortedFurniture != null 
          ? this.filteredAndSortedFurniture.size()
          : 0;
      List<HomePieceOfFurniture> furniture = this.home.getFurniture();     
      boolean containsNotViewableFurniture = false;
      for (HomePieceOfFurniture homePiece : furniture) {
        Level level = homePiece.getLevel();
        if (level != null && !level.isViewable()) {
          containsNotViewableFurniture = true;
          break;
        }
      }
      this.containsNotViewableFurniture = containsNotViewableFurniture;      
      this.filteredAndSortedFurniture = getFilteredAndSortedFurniture(furniture, true);      
      if (previousRowCount != this.filteredAndSortedFurniture.size()) {
        fireTableDataChanged();
      } else {
        fireTableRowsUpdated(0, getRowCount() - 1);
      }
      fireTreeModelChanged();
    }

    private List<HomePieceOfFurniture> getFilteredAndSortedFurniture(List<HomePieceOfFurniture> furniture, 
                                                                     boolean includeExpandedGroups) {
      List<HomePieceOfFurniture> viewableFurniture = new ArrayList<HomePieceOfFurniture>(furniture.size());
      for (HomePieceOfFurniture homePiece : furniture) {
        if (homePiece.getLevel() == null
            || homePiece.getLevel().isViewable()) {
          viewableFurniture.add(homePiece);
        }
      }
      
      List<HomePieceOfFurniture> filteredAndSortedFurniture;
      if (this.furnitureFilter == null) {
        filteredAndSortedFurniture = viewableFurniture;
      } else {
        filteredAndSortedFurniture = new ArrayList<HomePieceOfFurniture>(viewableFurniture.size());
        for (HomePieceOfFurniture homePiece : viewableFurniture) {
          if (this.furnitureFilter.include(this.home, homePiece)) {
            filteredAndSortedFurniture.add(homePiece);
          }
        }
      }
      if (this.home.getFurnitureSortedProperty() != null) {
        Comparator<HomePieceOfFurniture> furnitureComparator = getFurnitureComparator(this.home);
        Collections.sort(filteredAndSortedFurniture, furnitureComparator);         
      }
      if (includeExpandedGroups) {
        for (int i = filteredAndSortedFurniture.size() - 1; i >= 0; i--) {
          HomePieceOfFurniture piece = filteredAndSortedFurniture.get(i);
          if (piece instanceof HomeFurnitureGroup 
              && this.expandedGroups.contains(piece)) {
            filteredAndSortedFurniture.addAll(i + 1, 
                getFilteredAndSortedFurniture(((HomeFurnitureGroup)piece).getFurniture(), true));
          }
        }
      }
      return filteredAndSortedFurniture;
    }

    private Comparator<HomePieceOfFurniture> getFurnitureComparator(Home home) {
      Comparator<HomePieceOfFurniture> furnitureComparator = 
        HomePieceOfFurniture.getFurnitureComparator(home.getFurnitureSortedProperty());
      if (home.isFurnitureDescendingSorted()) {
        furnitureComparator = Collections.reverseOrder(furnitureComparator);
      }
      return furnitureComparator;
    }

    public void setFurnitureFilter(FurnitureFilter furnitureFilter) {
      this.furnitureFilter = furnitureFilter;      
      filterAndSortFurniture();
    }
    

    public FurnitureFilter getFurnitureFilter() {
      return this.furnitureFilter;
    }

   
    public Object getRoot() {
      return this.home;
    }

    public Object getChild(Object parent, int index) {      
      return getChildFurniture(parent).get(index);
    }

    
    public int getChildCount(Object parent) {
      return getChildFurniture(parent).size();
    }

    public int getIndexOfChild(Object parent, Object child) {
      return getChildFurniture(parent).indexOf(child);
    }

    private List<HomePieceOfFurniture> getChildFurniture(Object parent) {
      List<HomePieceOfFurniture> furniture = this.childFurnitureCache.get(parent);
      if (furniture == null) {
        if (parent instanceof HomeFurnitureGroup) {
          furniture = ((HomeFurnitureGroup)parent).getFurniture();
        } else {
          furniture = this.home.getFurniture();
        }      
        furniture = getFilteredAndSortedFurniture(furniture, false);
        this.childFurnitureCache.put(parent, furniture);
      }
      return furniture;
    }
    
    public boolean isLeaf(Object node) {
      return node instanceof HomePieceOfFurniture 
          && !(node instanceof HomeFurnitureGroup);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public void addTreeModelListener(TreeModelListener listener) {
      this.treeModelListeners.add(listener);
    }

    public void removeTreeModelListener(TreeModelListener listener) {
      this.treeModelListeners.remove(listener);
    }
    
    private void fireTreeModelChanged() {
      this.childFurnitureCache.clear();
      for (TreeModelListener listener : this.treeModelListeners) {
        listener.treeStructureChanged(new TreeModelEvent(this, new TreePath(this.home)));
      }
    }
    
    public boolean isRowExpanded(int rowIndex) {
      return this.expandedGroups.contains(this.filteredAndSortedFurniture.get(rowIndex));
    }    
    
    public void toggleRowExpandedState(int rowIndex) {
      HomePieceOfFurniture piece = this.filteredAndSortedFurniture.get(rowIndex);
      if (piece instanceof HomeFurnitureGroup) {
        if (this.expandedGroups.contains(piece)) {
          this.expandedGroups.remove((HomeFurnitureGroup)piece);
        } else {
          this.expandedGroups.add((HomeFurnitureGroup)piece);        
        }
        filterAndSortFurniture();
      }
    }
    
    public void expandPathToPieceOfFurniture(HomePieceOfFurniture piece) {
      List<HomePieceOfFurniture> furniture = this.home.getFurniture();
      if (furniture.contains(piece)) {
        return;
      }
      for (HomeFurnitureGroup group : this.expandedGroups) {
        if (group.getFurniture().contains(piece)) {
          return;
        }
      }
      for (HomePieceOfFurniture homePiece : furniture) {
        if (homePiece instanceof HomeFurnitureGroup
            && expandPathToPieceOfFurniture(piece, (HomeFurnitureGroup)homePiece)) {
          filterAndSortFurniture();
          return;
        }
      }
    }

    private boolean expandPathToPieceOfFurniture(HomePieceOfFurniture piece, 
                                                 HomeFurnitureGroup group) {
      for (HomePieceOfFurniture groupPiece : group.getFurniture()) {
        if (groupPiece == piece
            || (groupPiece instanceof HomeFurnitureGroup
                && expandPathToPieceOfFurniture(piece, (HomeFurnitureGroup)groupPiece))) {
          this.expandedGroups.add(group);
          return true;
        } 
      }
      return false;
    }
  }
    
  public static interface FurnitureFilter {    
    public abstract boolean include(Home home, HomePieceOfFurniture piece);
  }
}
