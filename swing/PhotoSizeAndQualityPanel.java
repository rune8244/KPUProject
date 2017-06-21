package com.eteks.homeview3d.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.ToolTipManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.eteks.homeview3d.j3d.Component3DManager;
import com.eteks.homeview3d.model.AspectRatio;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.tools.ResourceURLContent;
import com.eteks.homeview3d.viewcontroller.AbstractPhotoController;

public class PhotoSizeAndQualityPanel extends JPanel {
  private JLabel                        widthLabel;
  private JSpinner                      widthSpinner;
  private JLabel                        heightLabel;
  private JSpinner                      heightSpinner;
  private JCheckBox                     applyProportionsCheckBox;
  private JComboBox                     aspectRatioComboBox;
  private JLabel                        qualityLabel;
  private JSlider                       qualitySlider;
  private JLabel                        fastQualityLabel;
  private JLabel                        bestQualityLabel;

  public PhotoSizeAndQualityPanel(Home home, 
                    UserPreferences preferences, 
                    AbstractPhotoController controller) {
    super(new GridBagLayout());
    createComponents(home, preferences, controller);
    setMnemonics(preferences);
    layoutComponents();    

    preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE, new LanguageChangeListener(this));
  }
  
  private void createComponents(final Home home, 
                                final UserPreferences preferences,
                                final AbstractPhotoController controller) {
    this.widthLabel = new JLabel();
    final SpinnerNumberModel widthSpinnerModel = new SpinnerNumberModel(480, 10, 10000, 10);
    this.widthSpinner = new AutoCommitSpinner(widthSpinnerModel);
    widthSpinnerModel.setValue(controller.getWidth());
    widthSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.setWidth(((Number)widthSpinnerModel.getValue()).intValue());
        }
      });
    controller.addPropertyChangeListener(AbstractPhotoController.Property.WIDTH, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            widthSpinnerModel.setValue(controller.getWidth());
          }
        });

    
    this.heightLabel = new JLabel();
    final SpinnerNumberModel heightSpinnerModel = new SpinnerNumberModel(480, 10, 10000, 10);
    this.heightSpinner = new AutoCommitSpinner(heightSpinnerModel);
    heightSpinnerModel.setValue(controller.getHeight());
    heightSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.setHeight(((Number)heightSpinnerModel.getValue()).intValue());
        }
      });
    controller.addPropertyChangeListener(AbstractPhotoController.Property.HEIGHT, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            heightSpinnerModel.setValue(controller.getHeight());
          }
        });

    boolean notFreeAspectRatio = controller.getAspectRatio() != AspectRatio.FREE_RATIO;
    this.applyProportionsCheckBox = new JCheckBox();
    this.applyProportionsCheckBox.setSelected(notFreeAspectRatio);
    this.applyProportionsCheckBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ev) {
          controller.setAspectRatio(applyProportionsCheckBox.isSelected()
              ? (AspectRatio)aspectRatioComboBox.getSelectedItem()
              : AspectRatio.FREE_RATIO);
        }
      });
    this.aspectRatioComboBox = new JComboBox(new Object [] {
        AspectRatio.VIEW_3D_RATIO,
        AspectRatio.SQUARE_RATIO,
        AspectRatio.RATIO_4_3,
        AspectRatio.RATIO_3_2,
        AspectRatio.RATIO_16_9,
        AspectRatio.RATIO_2_1});
    this.aspectRatioComboBox.setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                                                      int index, boolean isSelected, boolean cellHasFocus) {
          AspectRatio aspectRatio = (AspectRatio)value;
          String displayedValue = "";
          if (aspectRatio != AspectRatio.FREE_RATIO) {
            switch (aspectRatio) {
              case VIEW_3D_RATIO :
                displayedValue = preferences.getLocalizedString(
                    PhotoSizeAndQualityPanel.class, "aspectRatioComboBox.view3DRatio.text");
                break;
              case SQUARE_RATIO :
                displayedValue = preferences.getLocalizedString(
                    PhotoSizeAndQualityPanel.class, "aspectRatioComboBox.squareRatio.text");
                break;
              case RATIO_4_3 :
                displayedValue = "4/3";
                break;
              case RATIO_3_2 :
                displayedValue = "3/2";
                break;
              case RATIO_16_9 :
                displayedValue = "16/9";
                break;
              case RATIO_2_1 :
                displayedValue = "2/1";
                break;
            }
          } 
          return super.getListCellRendererComponent(list, displayedValue, index, isSelected,
              cellHasFocus);
        }
      });
    this.aspectRatioComboBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ev) {
          controller.setAspectRatio((AspectRatio)aspectRatioComboBox.getSelectedItem());
        }
      });
    this.aspectRatioComboBox.setEnabled(notFreeAspectRatio);
    this.aspectRatioComboBox.setSelectedItem(controller.getAspectRatio());
    controller.addPropertyChangeListener(AbstractPhotoController.Property.ASPECT_RATIO,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            boolean notFreeAspectRatio = controller.getAspectRatio() != AspectRatio.FREE_RATIO;
            applyProportionsCheckBox.setSelected(notFreeAspectRatio);
            aspectRatioComboBox.setEnabled(notFreeAspectRatio);
            aspectRatioComboBox.setSelectedItem(controller.getAspectRatio());
          }
        });

    this.qualityLabel = new JLabel();
    this.fastQualityLabel = new JLabel();
    this.bestQualityLabel = new JLabel();
    this.qualitySlider = new JSlider(1, controller.getQualityLevelCount()) {
        @Override
        public String getToolTipText(MouseEvent ev) {
          float valueUnderMouse = getSliderValueAt(this, ev.getX(), preferences);
          float valueToTick = valueUnderMouse - (float)Math.floor(valueUnderMouse);
          if (valueToTick < 0.25f || valueToTick > 0.75f) {
            return "<html><table><tr valign='middle'>"
                + "<td><img border='1' src='" 
                + new ResourceURLContent(PhotoSizeAndQualityPanel.class, "resources/quality" + Math.round(valueUnderMouse - qualitySlider.getMinimum()) + ".jpg").getURL() + "'></td>"
                + "<td>" + preferences.getLocalizedString(PhotoSizeAndQualityPanel.class, "quality" + Math.round(valueUnderMouse - qualitySlider.getMinimum()) + "DescriptionLabel.text") + "</td>"
                + "</tr></table>";
          } else {
            return null;
          }
        }
      };
    this.qualitySlider.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent ev) {
          EventQueue.invokeLater(new Runnable() {
              public void run() {
                float valueUnderMouse = getSliderValueAt(qualitySlider, ev.getX(), preferences);
                if (qualitySlider.getValue() == Math.round(valueUnderMouse)) {
                  ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
                  int initialDelay = toolTipManager.getInitialDelay();
                  toolTipManager.setInitialDelay(Math.min(initialDelay, 150));
                  toolTipManager.mouseMoved(ev);
                  toolTipManager.setInitialDelay(initialDelay);
                }
              }
            });
        }
      });
    this.qualitySlider.setPaintTicks(true);    
    this.qualitySlider.setMajorTickSpacing(1);
    this.qualitySlider.setSnapToTicks(true);
    final boolean offScreenImageSupported = Component3DManager.getInstance().isOffScreenImageSupported();
    this.qualitySlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          if (!offScreenImageSupported) {
            qualitySlider.setValue(Math.max(qualitySlider.getMinimum() + 2, qualitySlider.getValue()));
          }
          controller.setQuality(qualitySlider.getValue() - qualitySlider.getMinimum());
        }
      });
    controller.addPropertyChangeListener(AbstractPhotoController.Property.QUALITY, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            qualitySlider.setValue(qualitySlider.getMinimum() + controller.getQuality());
          }
        });
    this.qualitySlider.setValue(this.qualitySlider.getMinimum() + controller.getQuality());
   
    final JComponent view3D = (JComponent)controller.get3DView();
    final ComponentAdapter view3DSizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent ev) {
          controller.set3DViewAspectRatio((float)view3D.getWidth() / view3D.getHeight());
        }
      };
    addAncestorListener(new AncestorListener() {        
      public void ancestorAdded(AncestorEvent ev) {
        view3D.addComponentListener(view3DSizeListener);
        ToolTipManager.sharedInstance().registerComponent(qualitySlider);
      }
      
      public void ancestorRemoved(AncestorEvent ev) {
        ToolTipManager.sharedInstance().unregisterComponent(qualitySlider);
        view3D.removeComponentListener(view3DSizeListener);
      }
      
      public void ancestorMoved(AncestorEvent ev) {
      }        
    });

    setComponentTexts(preferences);
  }

  private float getSliderValueAt(JSlider qualitySlider, int x, UserPreferences preferences) {
    int fastLabelOffset = 0;
    int bestLabelOffset = 0;
    int sliderWidth = qualitySlider.getWidth() - fastLabelOffset - bestLabelOffset;
    return qualitySlider.getMinimum()
        + (float)(x - (qualitySlider.getComponentOrientation().isLeftToRight() 
                          ? fastLabelOffset 
                          : bestLabelOffset))
        / sliderWidth * (qualitySlider.getMaximum() - qualitySlider.getMinimum());
  }
  
  private void setComponentTexts(UserPreferences preferences) {
    this.widthLabel.setText(SwingTools.getLocalizedLabelText(preferences, 
        PhotoSizeAndQualityPanel.class, "widthLabel.text"));
    this.heightLabel.setText(SwingTools.getLocalizedLabelText(preferences, 
        PhotoSizeAndQualityPanel.class, "heightLabel.text"));
    this.applyProportionsCheckBox.setText(SwingTools.getLocalizedLabelText(preferences, 
        PhotoSizeAndQualityPanel.class, "applyProportionsCheckBox.text"));
    this.qualityLabel.setText(SwingTools.getLocalizedLabelText(preferences, 
        PhotoSizeAndQualityPanel.class, "qualityLabel.text"));
    this.fastQualityLabel.setText(SwingTools.getLocalizedLabelText(preferences,
        PhotoSizeAndQualityPanel.class, "fastLabel.text"));
    if (!Component3DManager.getInstance().isOffScreenImageSupported()) {
      this.fastQualityLabel.setEnabled(false);
    }
    this.bestQualityLabel.setText(SwingTools.getLocalizedLabelText(preferences,
        PhotoSizeAndQualityPanel.class, "bestLabel.text"));
  }

  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      this.widthLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          PhotoSizeAndQualityPanel.class, "widthLabel.mnemonic")).getKeyCode());
      this.widthLabel.setLabelFor(this.widthSpinner);
      this.heightLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          PhotoSizeAndQualityPanel.class, "heightLabel.mnemonic")).getKeyCode());
      this.heightLabel.setLabelFor(this.heightSpinner);
      this.applyProportionsCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          PhotoSizeAndQualityPanel.class, "applyProportionsCheckBox.mnemonic")).getKeyCode());
      this.qualityLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          PhotoSizeAndQualityPanel.class, "qualityLabel.mnemonic")).getKeyCode());
      this.qualityLabel.setLabelFor(this.qualitySlider);
    }
  }

  public static class LanguageChangeListener implements PropertyChangeListener {
    private final WeakReference<PhotoSizeAndQualityPanel> photoPanel;

    public LanguageChangeListener(PhotoSizeAndQualityPanel photoPanel) {
      this.photoPanel = new WeakReference<PhotoSizeAndQualityPanel>(photoPanel);
    }

    public void propertyChange(PropertyChangeEvent ev) {
      PhotoSizeAndQualityPanel photoPanel = this.photoPanel.get();
      UserPreferences preferences = (UserPreferences)ev.getSource();
      if (photoPanel == null) {
        preferences.removePropertyChangeListener(UserPreferences.Property.LANGUAGE, this);
      } else {
        photoPanel.setComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        photoPanel.setComponentTexts(preferences);
        photoPanel.setMnemonics(preferences);
      }
    }
  }

  private void layoutComponents() {
    int labelAlignment = OperatingSystem.isMacOSX()
        ? JLabel.TRAILING
        : JLabel.LEADING;
    Insets labelInsets = new Insets(0, 0, 0, 5);
    add(this.widthLabel, new GridBagConstraints(
        0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, labelInsets, 0, 0));
    this.widthLabel.setHorizontalAlignment(labelAlignment);
    Insets componentInsets = new Insets(0, 0, 0, 10);
    add(this.widthSpinner, new GridBagConstraints(
        1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, componentInsets, 0, 0));
    add(this.heightLabel, new GridBagConstraints(
        2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, labelInsets, 0, 0));
    this.heightLabel.setHorizontalAlignment(labelAlignment);
    add(this.heightSpinner, new GridBagConstraints(
        3, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    JPanel proportionsPanel = new JPanel();
    proportionsPanel.add(this.applyProportionsCheckBox);
    proportionsPanel.add(this.aspectRatioComboBox);
    add(proportionsPanel, new GridBagConstraints(
        0, 1, 4, 1, 0, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(this.qualityLabel, new GridBagConstraints(
        0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 5), 0, 0));
    this.qualityLabel.setHorizontalAlignment(labelAlignment);
    add(this.qualitySlider, new GridBagConstraints(
        1, 3, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    JPanel qualityLabelsPanel = new JPanel(new BorderLayout(20, 0));
    qualityLabelsPanel.add(this.fastQualityLabel, BorderLayout.WEST);
    qualityLabelsPanel.add(this.bestQualityLabel, BorderLayout.EAST);
    add(qualityLabelsPanel, new GridBagConstraints(
        1, 4, 3, 1, 0, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, new Insets(OperatingSystem.isWindows() ? 0 : -3, 0, 0, 0), 0, 0));
  }
  
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    this.widthSpinner.setEnabled(enabled);
    this.heightSpinner.setEnabled(enabled);
    this.applyProportionsCheckBox.setEnabled(enabled);
    this.aspectRatioComboBox.setEnabled(enabled);
    this.qualitySlider.setEnabled(enabled);
  }
  
  public void setProportionsChoiceEnabled(boolean enabled) {
    this.applyProportionsCheckBox.setEnabled(enabled); 
    this.aspectRatioComboBox.setEnabled(enabled && this.applyProportionsCheckBox.isSelected()); 
  }
}
