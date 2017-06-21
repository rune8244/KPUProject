package com.eteks.homeview3d.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.eteks.homeview3d.model.CatalogTexture;
import com.eteks.homeview3d.model.CollectionEvent;
import com.eteks.homeview3d.model.CollectionListener;
import com.eteks.homeview3d.model.HomeTexture;
import com.eteks.homeview3d.model.TextureImage;
import com.eteks.homeview3d.model.TexturesCatalog;
import com.eteks.homeview3d.model.TexturesCategory;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.TextureChoiceController;
import com.eteks.homeview3d.viewcontroller.TextureChoiceView;
import com.eteks.homeview3d.viewcontroller.View;

public class TextureChoiceComponent extends JButton implements TextureChoiceView {
  private final UserPreferences preferences;

  
  public TextureChoiceComponent(final UserPreferences preferences,
                                final TextureChoiceController controller) {
    this.preferences = preferences;
    JLabel dummyLabel = new JLabel("Text");
    Dimension iconDimension = dummyLabel.getPreferredSize();
    final int iconHeight = iconDimension.height;

    controller.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            repaint();
          }
        });
    
    setIcon(new Icon() {
        public int getIconWidth() {
          return iconHeight;
        }
  
        public int getIconHeight() {
          return iconHeight;
        }
  
        public void paintIcon(Component c, Graphics g, int x, int y) {
          g.setColor(Color.BLACK);
          g.drawRect(x + 2, y + 2, iconHeight - 5, iconHeight - 5);
          HomeTexture texture = controller.getTexture();
          if (texture != null) {
            Icon icon = IconManager.getInstance().getIcon(
                texture.getImage(), iconHeight - 6, TextureChoiceComponent.this);
            if (icon.getIconWidth() != icon.getIconHeight()) {
              Graphics2D g2D = (Graphics2D)g;
              AffineTransform previousTransform = g2D.getTransform();
              g2D.translate(x + 3, y + 3);
              g2D.scale((float)icon.getIconHeight() / icon.getIconWidth(), 1);
              icon.paintIcon(c, g2D, 0, 0);
              g2D.setTransform(previousTransform);
            } else {
              icon.paintIcon(c, g, x + 3, y + 3);
            }
          }
        }
      });
    
    addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        final TexturePanel texturePanel = new TexturePanel(preferences, controller);
        texturePanel.displayView(TextureChoiceComponent.this);
      }
    });
  }

  public boolean confirmDeleteSelectedCatalogTexture() {
    String message = this.preferences.getLocalizedString(
        TextureChoiceComponent.class, "confirmDeleteSelectedCatalogTexture.message");
    String title = this.preferences.getLocalizedString(
        TextureChoiceComponent.class, "confirmDeleteSelectedCatalogTexture.title");
    String delete = this.preferences.getLocalizedString(
        TextureChoiceComponent.class, "confirmDeleteSelectedCatalogTexture.delete");
    String cancel = this.preferences.getLocalizedString(
        TextureChoiceComponent.class, "confirmDeleteSelectedCatalogTexture.cancel");
    
    return JOptionPane.showOptionDialog(
        KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(), message, title, 
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
        null, new Object [] {delete, cancel}, cancel) == JOptionPane.OK_OPTION;
  }

  private static class TexturePanel extends JPanel {
    private static final int PREVIEW_ICON_SIZE = Math.round(128 * SwingTools.getResolutionScale());; 
    private static String    searchFilterText = "";
    private static Dimension dialogPreferredSize; 
    
    private TextureChoiceController controller;
    
    private TextureImage            previewTexture;
    private JLabel                  searchLabel;
    private JTextField              searchTextField;
    private JLabel                  chosenTextureLabel;
    private ScaledImageComponent    texturePreviewComponent;
    private JLabel                  availableTexturesLabel;
    private JList                   availableTexturesList;
    private JLabel                  angleLabel;
    private JRadioButton            angle0DegreeRadioButton;
    private JRadioButton            angle45DegreeRadioButton;
    private JRadioButton            angle90DegreeRadioButton;
    private JButton                 importTextureButton;
    private JButton                 modifyTextureButton;
    private JButton                 deleteTextureButton;
    private JPanel                  recentTexturesPanel;
    private CatalogItemToolTip      toolTip;

    public TexturePanel(UserPreferences preferences, 
                        TextureChoiceController controller) {
      super(new GridBagLayout());
      this.controller = controller;
      createComponents(preferences, controller);
      setMnemonics(preferences);
      layoutComponents();
    }

    private void createComponents(final UserPreferences preferences, 
                                  final TextureChoiceController controller) {
      this.availableTexturesLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
          TextureChoiceComponent.class, "availableTexturesLabel.text"));
      final TexturesCatalogListModel texturesListModel = new TexturesCatalogListModel(preferences.getTexturesCatalog());
      this.availableTexturesList = new JList(texturesListModel) {
          @Override
          public JToolTip createToolTip() {    
            if (toolTip.isTipTextComplete()) {
              return super.createToolTip();
            } else {
              toolTip.setComponent(this);
              return toolTip;
            }
          }
  
          @Override
          public String getToolTipText(MouseEvent ev) {
            int index = locationToIndex(ev.getPoint());
            if (index >= 0) {
              CatalogTexture texture = (CatalogTexture)getModel().getElementAt(index);
              IconManager iconManager = IconManager.getInstance();
              if (!iconManager.isWaitIcon(iconManager.getIcon(texture.getImage(), TextureIcon.SIZE, this))) {
                toolTip.setCatalogItem(texture);
                return toolTip.getTipText();
              }
            }
            return null;
          }
        };
      this.toolTip = new CatalogItemToolTip(true, preferences);
      this.availableTexturesList.setVisibleRowCount(12);
      this.availableTexturesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      this.availableTexturesList.setCellRenderer(new TextureListCellRenderer());
      this.availableTexturesList.getSelectionModel().addListSelectionListener(
          new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
              CatalogTexture selectedTexture = (CatalogTexture)availableTexturesList.getSelectedValue();
              if (selectedTexture != null) {
                setPreviewTexture(selectedTexture);
                angle45DegreeRadioButton.setEnabled(Math.abs(selectedTexture.getWidth() - selectedTexture.getHeight()) < 1E-4);
                if (angle45DegreeRadioButton.isSelected() && !angle45DegreeRadioButton.isEnabled()) {
                  angle0DegreeRadioButton.setSelected(true);
                }
              }
              if (modifyTextureButton != null) {
                modifyTextureButton.setEnabled(selectedTexture != null && selectedTexture.isModifiable());
              }
              if (deleteTextureButton != null) {
                deleteTextureButton.setEnabled(selectedTexture != null && selectedTexture.isModifiable());
              }
            }
          });

      this.searchLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
          TextureChoiceComponent.class, "searchLabel.text"));
      this.searchTextField = new JTextField(5);
      this.searchTextField.getDocument().addDocumentListener(new DocumentListener() {  
          public void changedUpdate(DocumentEvent ev) {
            Object selectedValue = availableTexturesList.getSelectedValue();
            texturesListModel.setFilterText(searchTextField.getText());
            if (selectedValue != null) {
              availableTexturesList.clearSelection();
              availableTexturesList.setSelectedValue(selectedValue, true);
              
              if (texturesListModel.getSize() == 1) {
                availableTexturesList.setSelectedIndex(0);
              }
            }
          }
    
          public void insertUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
    
          public void removeUpdate(DocumentEvent ev) {
            changedUpdate(ev);
          }
        });
      this.searchTextField.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "deleteContent");
      this.searchTextField.getActionMap().put("deleteContent", new AbstractAction() {
          public void actionPerformed(ActionEvent ev) {
            searchTextField.setText("");
          }
        });
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        this.searchTextField.putClientProperty("JTextField.variant", "search");
      } 
      this.searchTextField.getInputMap(JComponent.WHEN_FOCUSED).remove(KeyStroke.getKeyStroke("ESCAPE"));

      this.chosenTextureLabel = new JLabel(preferences.getLocalizedString(
          TextureChoiceComponent.class, "chosenTextureLabel.text"));
      this.texturePreviewComponent = new ScaledImageComponent(null, true) {
          protected void paintComponent(Graphics g) {
            Graphics2D g2D = (Graphics2D)g;
            Shape oldClip = g2D.getClip();
            AffineTransform oldTransform = g2D.getTransform();
            Insets borderInsets = getBorder().getBorderInsets(this);
            g2D.setClip(new Rectangle(borderInsets.left, borderInsets.top, 
                getWidth() - borderInsets.right - borderInsets.left, getHeight() - borderInsets.bottom - borderInsets.top));
            if (angle45DegreeRadioButton.isSelected()) {
              g2D.rotate(Math.PI / 4, getWidth() / 2, getHeight() / 2);
            } else if (angle90DegreeRadioButton.isSelected()) {
              g2D.rotate(Math.PI / 2, getWidth() / 2, getHeight() / 2);
            } 
            super.paintComponent(g);
            g2D.setTransform(oldTransform);
            g2D.setClip(oldClip);
          }
        };

      try {
        String importTextureButtonText = SwingTools.getLocalizedLabelText(
            preferences, TextureChoiceComponent.class, "importTextureButton.text");
        this.texturePreviewComponent.setBorder(SwingTools.getDropableComponentBorder());
        this.texturePreviewComponent.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(JComponent comp, DataFlavor [] flavors) {
              return Arrays.asList(flavors).contains(DataFlavor.javaFileListFlavor);
            }
            
            @Override
            public boolean importData(JComponent comp, Transferable transferedFiles) {
              try {
                List<File> files = (List<File>)transferedFiles.getTransferData(DataFlavor.javaFileListFlavor);
                final String textureName = files.get(0).getAbsolutePath();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      controller.importTexture(textureName);
                    }
                  });
                return true;
              } catch (UnsupportedFlavorException ex) {
                return false;
              } catch (IOException ex) {
                return false;
              }
            }
          });
      
        this.angleLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, TextureChoiceComponent.class,
            "angleLabel.text"));
        this.angle0DegreeRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, TextureChoiceComponent.class,
            "angle0DegreeRadioButton.text"), true);
        this.angle45DegreeRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, TextureChoiceComponent.class,
            "angle45DegreeRadioButton.text"));
        this.angle90DegreeRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, TextureChoiceComponent.class,
            "angle90DegreeRadioButton.text"));
        ChangeListener angleChangeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
              texturePreviewComponent.repaint();
            }
          };
        this.angle0DegreeRadioButton.addChangeListener(angleChangeListener);
        this.angle45DegreeRadioButton.addChangeListener(angleChangeListener);
        this.angle90DegreeRadioButton.addChangeListener(angleChangeListener);
        ButtonGroup angleGroup = new ButtonGroup();
        angleGroup.add(this.angle0DegreeRadioButton);
        angleGroup.add(this.angle45DegreeRadioButton);
        angleGroup.add(this.angle90DegreeRadioButton);
        
        this.importTextureButton = new JButton(importTextureButtonText);
        this.importTextureButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              controller.importTexture();
            }
          });
        this.modifyTextureButton = new JButton(SwingTools.getLocalizedLabelText(preferences, 
            TextureChoiceComponent.class, "modifyTextureButton.text"));
        this.modifyTextureButton.setEnabled(false);
        this.modifyTextureButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              controller.modifyTexture((CatalogTexture)availableTexturesList.getSelectedValue());
            }
          });    
        this.deleteTextureButton = new JButton(SwingTools.getLocalizedLabelText(preferences, 
            TextureChoiceComponent.class, "deleteTextureButton.text"));
        this.deleteTextureButton.setEnabled(false);
        this.deleteTextureButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              controller.deleteTexture((CatalogTexture)availableTexturesList.getSelectedValue());
            }
          });
        
        preferences.getTexturesCatalog().addTexturesListener(new TexturesCatalogListener(this));
      } catch (IllegalArgumentException ex) {
        this.texturePreviewComponent.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
      }
      
      this.recentTexturesPanel = SwingTools.createTitledPanel(preferences.getLocalizedString(
          TextureChoiceComponent.class, "recentPanel.title"));
      int reducedBorderWidth = OperatingSystem.isMacOSXLeopardOrSuperior() ? -8 : -2;
      this.recentTexturesPanel.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createEmptyBorder(0, reducedBorderWidth, 0, reducedBorderWidth), this.recentTexturesPanel.getBorder())); 
      preferences.addPropertyChangeListener(UserPreferences.Property.RECENT_TEXTURES, 
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              updateRecentTextures(preferences);
            }
          });
      updateRecentTextures(preferences);
      this.recentTexturesPanel.setOpaque(false);

      Border border = this.texturePreviewComponent.getBorder();
      HomeTexture texture = controller.getTexture();
      setPreviewTexture(texture);
      if (texture instanceof HomeTexture) {
        this.angle45DegreeRadioButton.setEnabled(Math.abs(texture.getWidth() - texture.getHeight()) < 1E-4);
        if (((HomeTexture)texture).getAngle() == (float)(Math.PI / 4)) {
          this.angle45DegreeRadioButton.setSelected(true);
        } else if (((HomeTexture)texture).getAngle() == (float)(Math.PI / 2)) {
          this.angle90DegreeRadioButton.setSelected(true);
        }
      }
      Insets insets = border.getBorderInsets(this.texturePreviewComponent);
      this.texturePreviewComponent.setPreferredSize(
          new Dimension(PREVIEW_ICON_SIZE + insets.left + insets.right, PREVIEW_ICON_SIZE + insets.top + insets.bottom));
    }

    private static class TextureListCellRenderer extends DefaultListCellRenderer {
      private Font defaultFont;
      private Font modifiablePieceFont;

      @Override
      public Component getListCellRendererComponent(final JList list, Object value, 
          int index, boolean isSelected, boolean cellHasFocus) {
        if (this.defaultFont == null) {
          this.defaultFont = getFont();
          this.modifiablePieceFont = 
              new Font(this.defaultFont.getFontName(), Font.ITALIC, this.defaultFont.getSize());
          
        }
        
        final CatalogTexture texture = (CatalogTexture)value;
        value = texture.getName();
        value = texture.getCategory().getName() + " - " + value;
        Component component = super.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus);
        setIcon(new TextureIcon(texture, list));
        setFont(texture.isModifiable() ? this.modifiablePieceFont : this.defaultFont);
        return component;
      }
    }

    private static class TextureIcon implements Icon {
      static final int SIZE = 16;
      
      private TextureImage texture;
      private JComponent   component;

      public TextureIcon(TextureImage texture,
                         JComponent component) {
        this.texture = texture;
        this.component = component;
      }
      
      public int getIconWidth() {
        return SIZE;
      }

      public int getIconHeight() {
        return SIZE;
      }

      public void paintIcon(Component c, Graphics g, int x, int y) {
        Icon icon = IconManager.getInstance().getIcon(
            this.texture.getImage(), getIconHeight(), this.component);
        if (icon.getIconWidth() != icon.getIconHeight()) {
          Graphics2D g2D = (Graphics2D)g;
          AffineTransform previousTransform = g2D.getTransform();
          g2D.translate(x, y);
          g2D.scale((float)icon.getIconHeight() / icon.getIconWidth(), 1);
          icon.paintIcon(c, g2D, 0, 0);
          g2D.setTransform(previousTransform);
        } else {
          icon.paintIcon(c, g, x, y);
        }
      }
    }
    

    private static class TexturesCatalogListener implements CollectionListener<CatalogTexture> {
      private WeakReference<TexturePanel> texturePanel;
      
      public TexturesCatalogListener(TexturePanel texturePanel) {
        this.texturePanel = new WeakReference<TexturePanel>(texturePanel);
      }
      
      public void collectionChanged(final CollectionEvent<CatalogTexture> ev) {
        if (texturePanel == null) {
          ((TexturesCatalog)ev.getSource()).removeTexturesListener(this);
        } else {
          switch (ev.getType()) {
            case ADD:
              texturePanel.searchTextField.setText("");
              if (texturePanel.availableTexturesList.isShowing()) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                      texturePanel.availableTexturesList.setSelectedValue(ev.getItem(), true);
                    }
                  });
              } else {
                texturePanel.availableTexturesList.clearSelection();
              }
              break;
            case DELETE:
              texturePanel.availableTexturesList.clearSelection();
              break;
          }       
        }
      }
    }

    public void updateRecentTextures(UserPreferences preferences) {
      this.recentTexturesPanel.removeAll();
      List<TextureImage> recentTextures = preferences.getRecentTextures();
      Border labelBorder = BorderFactory.createLineBorder(Color.GRAY);
      for (int i = 0; i < recentTextures.size(); i++) {
        final TextureImage recentTexture = recentTextures.get(i);
        JLabel textureLabel = new JLabel();
        textureLabel.setIcon(new TextureIcon(recentTexture, textureLabel));
        textureLabel.setBorder(labelBorder);
        textureLabel.setToolTipText(recentTexture.getName());
        textureLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent ev) {
              setPreviewTexture(recentTexture);
              if (ev.getClickCount() == 2) {
                JRootPane rootPane = (JRootPane)SwingUtilities.getAncestorOfClass(JRootPane.class, recentTexturesPanel);
                if (rootPane != null) {
                  for (JButton button : SwingTools.findChildren(rootPane, JButton.class)) {
                    if ("OK".equals(button.getActionCommand())) {
                      button.doClick();
                    }
                  }
                }
              }
            }
          });
        this.recentTexturesPanel.add(textureLabel, new GridBagConstraints(
            i, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
            GridBagConstraints.NONE, new Insets(0, 0, 0, 2), 0, 0));
      }
      this.recentTexturesPanel.setVisible(!recentTextures.isEmpty());
    }
    
    private void setMnemonics(UserPreferences preferences) {
      if (!OperatingSystem.isMacOSX()) {
        this.availableTexturesLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TextureChoiceComponent.class, "availableTexturesLabel.mnemonic")).getKeyCode());
        this.availableTexturesLabel.setLabelFor(this.availableTexturesList);
        this.searchLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            TextureChoiceComponent.class, "searchLabel.mnemonic")).getKeyCode());
        this.searchLabel.setLabelFor(this.searchTextField);
        this.angle0DegreeRadioButton.setMnemonic(KeyStroke.getKeyStroke(
            preferences.getLocalizedString(TextureChoiceComponent.class, "angle0DegreeRadioButton.mnemonic")).getKeyCode());
        this.angle45DegreeRadioButton.setMnemonic(KeyStroke.getKeyStroke(
            preferences.getLocalizedString(TextureChoiceComponent.class, "angle45DegreeRadioButton.mnemonic")).getKeyCode());
        this.angle90DegreeRadioButton.setMnemonic(KeyStroke.getKeyStroke(
            preferences.getLocalizedString(TextureChoiceComponent.class, "angle90DegreeRadioButton.mnemonic")).getKeyCode());
        if (this.importTextureButton != null) {
          this.importTextureButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
              TextureChoiceComponent.class, "importTextureButton.mnemonic")).getKeyCode());
          this.modifyTextureButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
              TextureChoiceComponent.class, "modifyTextureButton.mnemonic")).getKeyCode());
          this.deleteTextureButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
              TextureChoiceComponent.class, "deleteTextureButton.mnemonic")).getKeyCode());
        }
      }
    }
    

    private void layoutComponents() {
      int labelAlignment = OperatingSystem.isMacOSX() 
          ? GridBagConstraints.LINE_END
          : GridBagConstraints.LINE_START;
      JPanel leftPanel = new JPanel(new GridBagLayout());
      leftPanel.add(this.availableTexturesLabel, new GridBagConstraints(
          0, 0, 2, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
      JScrollPane scrollPane = new JScrollPane(this.availableTexturesList);
      scrollPane.getVerticalScrollBar().addAdjustmentListener(
          SwingTools.createAdjustmentListenerUpdatingScrollPaneViewToolTip(scrollPane));
      scrollPane.setPreferredSize(new Dimension(250, scrollPane.getPreferredSize().height));
      leftPanel.add(scrollPane, new GridBagConstraints(
          0, 1, 2, 1, 1, 1, GridBagConstraints.CENTER,
          GridBagConstraints.BOTH, new Insets(0, 0, 3, 0), 0, 0));
      SwingTools.installFocusBorder(this.availableTexturesList);
      if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
        leftPanel.add(this.searchTextField, new GridBagConstraints(
            0, 5, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
      } else { 
        leftPanel.add(this.searchLabel, new GridBagConstraints(
            0, 5, 1, 1, 0, 0, labelAlignment, 
            GridBagConstraints.NONE, new Insets(2, 0, 0, 3), 0, 0));
        leftPanel.add(this.searchTextField, new GridBagConstraints(
            1, 5, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
            GridBagConstraints.HORIZONTAL, new Insets(2, 0, 0, 0), 0, 0));
      }
      
      JPanel rightPanel = new JPanel(new GridBagLayout());
      rightPanel.add(this.chosenTextureLabel, new GridBagConstraints(
          2, 0, 4, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
      rightPanel.add(this.texturePreviewComponent, new GridBagConstraints(
          2, 1, 4, 1, 0, 0, GridBagConstraints.NORTH,
          GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
      if (this.controller.isRotationSupported()) {
        rightPanel.add(this.angleLabel, new GridBagConstraints(
            2, 2, 1, 1, 0.1, 0, labelAlignment,
            GridBagConstraints.NONE, new Insets(0, 0, 5, 2), 0, 0));
        rightPanel.add(this.angle0DegreeRadioButton, new GridBagConstraints(
            3, 2, 1, 1, 0.1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.NONE, new Insets(0, 0, 5, 2), 0, 0));
        rightPanel.add(this.angle45DegreeRadioButton, new GridBagConstraints(
            4, 2, 1, 1, 0.1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.NONE, new Insets(0, 0, 5, 2), 0, 0));
        rightPanel.add(this.angle90DegreeRadioButton, new GridBagConstraints(
            5, 2, 1, 1, 0.1, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
        rightPanel.add(new JSeparator(), new GridBagConstraints(
            2, 3, 4, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 0, 0));
      }
      if (this.importTextureButton != null) {
        JPanel buttonsPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        buttonsPanel.add(this.importTextureButton);
        buttonsPanel.add(this.modifyTextureButton);
        buttonsPanel.add(this.deleteTextureButton);
        rightPanel.add(buttonsPanel, new GridBagConstraints(
            2, 4, 4, 1, 0, 1, GridBagConstraints.NORTH,
            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      }
      
      add(leftPanel, new GridBagConstraints(
          0, 0, 1, 1, 1, 1, GridBagConstraints.NORTH,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 15), 0, 0));
      add(rightPanel, new GridBagConstraints(
          1, 0, 1, 1, 0, 0, GridBagConstraints.NORTH,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      add(this.recentTexturesPanel, new GridBagConstraints(
          0, 1, 2, 1, 0, 0, GridBagConstraints.NORTH,
          GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
      
      final List<JComponent> components = new ArrayList<JComponent>();
      components.add(this.availableTexturesList);
      components.add(this.searchTextField);
      if (this.controller.isRotationSupported()) {
        components.add(this.angle0DegreeRadioButton);
        components.add(this.angle45DegreeRadioButton);
        components.add(this.angle90DegreeRadioButton);
      }
      if (this.importTextureButton != null) {
        components.add(this.importTextureButton);
        components.add(this.modifyTextureButton);
        components.add(this.deleteTextureButton);
      }
      setFocusTraversalPolicy(new FocusTraversalPolicy() {
          @Override
          public Component getComponentAfter(Container container, Component component) {
            int index = components.indexOf(component);
            if (index == components.size() - 1) {
              return null;
            } else {
              JComponent nextComponent = components.get(index + 1);
              if (nextComponent.isEnabled()) {
                return nextComponent;
              } else {
                return getComponentAfter(container, nextComponent);
              }
            }
          }
          
          @Override
          public Component getComponentBefore(Container container, Component component) {
            int index = components.indexOf(component);
            if (index == 0) {
              return null;
            } else {
              JComponent previousComponent = components.get(index - 1);
              if (previousComponent.isEnabled()) {
                return previousComponent;
              } else {
                return getComponentBefore(container, previousComponent);
              }
            }
          }

          @Override
          public Component getFirstComponent(Container container) {
            return components.get(0);
          }

          @Override
          public Component getLastComponent(Container container) {
            return components.get(components.size() - 1);
          }

          @Override
          public Component getDefaultComponent(Container container) {
            return getFirstComponent(container);
          }
        });
      setFocusTraversalPolicyProvider(true);
    }
    
    private TextureImage getPreviewTexture() {
      return this.previewTexture;
    }

    private void setPreviewTexture(TextureImage previewTexture) {
      this.previewTexture = previewTexture;
      if (previewTexture != null) {
        this.texturePreviewComponent.setToolTipText(previewTexture.getName());
        InputStream iconStream = null;
        try {
          iconStream = previewTexture.getImage().openStream();
          this.texturePreviewComponent.setImage(ImageIO.read(iconStream));
        } catch (IOException ex) {
        } finally {
          if (iconStream != null) {
            try {
              iconStream.close();
            } catch (IOException ex) {
            }
          }
        }        
      } else {
        this.texturePreviewComponent.setToolTipText(null);
        this.texturePreviewComponent.setImage(null);
        this.angle0DegreeRadioButton.setSelected(true);
      }
      this.availableTexturesList.setSelectedValue(previewTexture, true);
      if (this.availableTexturesList.getSelectedValue() != previewTexture) {
        int selectedIndex = this.availableTexturesList.getSelectedIndex();
        this.availableTexturesList.removeSelectionInterval(selectedIndex, selectedIndex);
      }
    }

    public void displayView(View textureChoiceComponent) {
      final JOptionPane optionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, 
          JOptionPane.OK_CANCEL_OPTION);
      JComponent parentComponent = SwingUtilities.getRootPane((JComponent)textureChoiceComponent);
      if (parentComponent != null) {
        optionPane.setComponentOrientation(parentComponent.getComponentOrientation());
      }
      final JDialog dialog = optionPane.createDialog(parentComponent, controller.getDialogTitle());
      dialog.applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
      dialog.setResizable(true);
      dialog.pack();
      dialog.setMinimumSize(dialog.getPreferredSize());
      if (dialogPreferredSize != null
          && dialogPreferredSize.width >= dialog.getWidth()
          && dialogPreferredSize.height >= dialog.getHeight()) {
        dialog.setSize(dialogPreferredSize);
      }
      searchTextField.setText(searchFilterText);
      dialog.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentShown(ComponentEvent ev) {
            SwingTools.requestFocusInWindow(searchTextField);
            dialog.removeComponentListener(this);
          }
        });
      this.availableTexturesList.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent ev) {
            if (ev.getClickCount() == 2) {
              optionPane.setValue(JOptionPane.OK_OPTION);
              availableTexturesList.removeMouseListener(this);
            }
          }
        });
      
      ToolTipManager.sharedInstance().registerComponent(this.availableTexturesList);
      dialog.setVisible(true);
      dialog.dispose();
      ToolTipManager.sharedInstance().unregisterComponent(this.availableTexturesList);
      dialogPreferredSize = dialog.getSize();
      searchFilterText = this.searchTextField.getText();
      if (Integer.valueOf(JOptionPane.OK_OPTION).equals(optionPane.getValue())) {
        HomeTexture selectedTexture = getSelectedTexture();
        this.controller.setTexture(selectedTexture);
        if (selectedTexture != null) {
          this.controller.addRecentTexture(selectedTexture);
        }
      }
    }

    
    private HomeTexture getSelectedTexture() {
      TextureImage previewTexture = getPreviewTexture();
      if (previewTexture == null) {
        return null;
      } else {
        float angleInRadians;
        if (this.angle45DegreeRadioButton.isSelected()) {
          angleInRadians = (float)(Math.PI / 4);
        } else if (this.angle90DegreeRadioButton.isSelected()) {
          angleInRadians = (float)(Math.PI / 2);
        } else {
          angleInRadians = 0;
        }
        return new HomeTexture(previewTexture, angleInRadians);
      }
    }
    
    
    private static class TexturesCatalogListModel extends AbstractListModel {
      private TexturesCatalog        catalog;
      private List<CatalogTexture>   textures;
      private String                 filterText;
      
      public TexturesCatalogListModel(TexturesCatalog catalog) {
        this.catalog = catalog;
        this.filterText = "";
        catalog.addTexturesListener(new TexturesCatalogListener(this));
      }

      public void setFilterText(String filterText) {
        this.filterText = filterText;
        resetFurnitureList();
      }

      public Object getElementAt(int index) {
        checkFurnitureList();
        return this.textures.get(index);
      }

      public int getSize() {
        checkFurnitureList();
        return this.textures.size();
      }
      
      private void resetFurnitureList() {
        if (this.textures != null) {
          this.textures = null;
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                fireContentsChanged(this, -1, -1);
              }
            });
        }
      }

      private void checkFurnitureList() {
        if (this.textures == null) {
          this.textures = new ArrayList<CatalogTexture>();
          this.textures.clear();
          for (TexturesCategory category : this.catalog.getCategories()) {
            for (CatalogTexture texture : category.getTextures()) {
              if (texture.matchesFilter(this.filterText)) {
                textures.add(texture);
              }
            }
          }
        }
      }

    
      private static class TexturesCatalogListener implements CollectionListener<CatalogTexture> {
        private WeakReference<TexturesCatalogListModel>  listModel;

        public TexturesCatalogListener(TexturesCatalogListModel catalogListModel) {
          this.listModel = new WeakReference<TexturesCatalogListModel>(catalogListModel);
        }
        
        public void collectionChanged(CollectionEvent<CatalogTexture> ev) {
          TexturesCatalogListModel listModel = this.listModel.get();
          TexturesCatalog catalog = (TexturesCatalog)ev.getSource();
          if (listModel == null) {
            catalog.removeTexturesListener(this);
          } else {
            listModel.resetFurnitureList();
          }
        }
      }
    }
  }
}