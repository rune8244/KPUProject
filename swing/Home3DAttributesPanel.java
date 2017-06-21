package com.eteks.homeview3d.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.DialogView;
import com.eteks.homeview3d.viewcontroller.Home3DAttributesController;
import com.eteks.homeview3d.viewcontroller.View;

public class Home3DAttributesPanel extends JPanel implements DialogView {
  private final Home3DAttributesController controller;
  private JRadioButton  groundColorRadioButton;
  private ColorButton   groundColorButton;
  private JRadioButton  groundTextureRadioButton;
  private JComponent    groundTextureComponent;
  private JRadioButton  skyColorRadioButton;
  private ColorButton   skyColorButton;
  private JRadioButton  skyTextureRadioButton;
  private JComponent    skyTextureComponent;
  private JLabel        brightnessLabel;
  private JSlider       brightnessSlider;
  private JLabel        darkBrightnessLabel;
  private JLabel        brightBrightnessLabel;
  private JLabel        wallsTransparencyLabel;
  private JLabel        opaqueWallsTransparencyLabel;
  private JLabel        invisibleWallsTransparencyLabel;
  private JSlider       wallsTransparencySlider;
  private String        dialogTitle;

  public Home3DAttributesPanel(UserPreferences preferences,
                               Home3DAttributesController controller) {
    super(new GridBagLayout());
    this.controller = controller;
    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents(preferences);
  }

  // 요소 및 스피너 모델 생성 및 초기화
  private void createComponents(UserPreferences preferences,
                                final Home3DAttributesController controller) {
    this.groundColorRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, 
        Home3DAttributesPanel.class, "groundColorRadioButton.text"));
    this.groundColorRadioButton.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          if (groundColorRadioButton.isSelected()) {
            controller.setGroundPaint(Home3DAttributesController.EnvironmentPaint.COLORED);
          }
        }
      });
    controller.addPropertyChangeListener(Home3DAttributesController.Property.GROUND_PAINT, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            updateGroundRadioButtons(controller);
          }
        });
  
    this.groundColorButton = new ColorButton(preferences);
    this.groundColorButton.setColorDialogTitle(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "groundColorDialog.title"));
    this.groundColorButton.setColor(controller.getGroundColor());
    this.groundColorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            controller.setGroundColor(groundColorButton.getColor());
          }
        });
    controller.addPropertyChangeListener(Home3DAttributesController.Property.GROUND_COLOR, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            groundColorButton.setColor(controller.getGroundColor());
          }
        });
    
    this.groundTextureRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, 
        Home3DAttributesPanel.class, "groundTextureRadioButton.text"));
    this.groundTextureRadioButton.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ev) {
        if (groundTextureRadioButton.isSelected()) {
          controller.setGroundPaint(Home3DAttributesController.EnvironmentPaint.TEXTURED);
        }
      }
    });
    
    this.groundTextureComponent = (JComponent)controller.getGroundTextureController().getView();

    ButtonGroup groundGroup = new ButtonGroup();
    groundGroup.add(this.groundColorRadioButton);
    groundGroup.add(this.groundTextureRadioButton);
    updateGroundRadioButtons(controller);
    
    this.skyColorRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, 
        Home3DAttributesPanel.class, "skyColorRadioButton.text"));
    this.skyColorRadioButton.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          if (skyColorRadioButton.isSelected()) {
            controller.setSkyPaint(Home3DAttributesController.EnvironmentPaint.COLORED);
          }
        }
      });
    controller.addPropertyChangeListener(Home3DAttributesController.Property.SKY_PAINT, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            updateSkyRadioButtons(controller);
          }
        });
  
    this.skyColorButton = new ColorButton(preferences);
    this.skyColorButton.setColorDialogTitle(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "skyColorDialog.title"));
    this.skyColorButton.setColor(controller.getSkyColor());
    this.skyColorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            controller.setSkyColor(skyColorButton.getColor());
          }
        });
    controller.addPropertyChangeListener(Home3DAttributesController.Property.SKY_COLOR, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            skyColorButton.setColor(controller.getSkyColor());
          }
        });
    
    this.skyTextureRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, 
        Home3DAttributesPanel.class, "skyTextureRadioButton.text"));
    this.skyTextureRadioButton.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent ev) {
        if (skyTextureRadioButton.isSelected()) {
          controller.setSkyPaint(Home3DAttributesController.EnvironmentPaint.TEXTURED);
        }
      }
    });
    
    this.skyTextureComponent = (JComponent)controller.getSkyTextureController().getView();

    ButtonGroup skyGroup = new ButtonGroup();
    skyGroup.add(this.skyColorRadioButton);
    skyGroup.add(this.skyTextureRadioButton);
    updateSkyRadioButtons(controller);
    
    // 밝기 라벨 속성
    this.brightnessLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        Home3DAttributesPanel.class, "brightnessLabel.text"));
    this.brightnessSlider = new JSlider(0, 255);
    this.darkBrightnessLabel = new JLabel(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "darkLabel.text"));
    this.brightBrightnessLabel = new JLabel(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "brightLabel.text"));
    this.brightnessSlider.setPaintTicks(true);
    this.brightnessSlider.setMajorTickSpacing(17);
    this.brightnessSlider.setValue(controller.getLightColor() & 0xFF);
    this.brightnessSlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          int brightness = brightnessSlider.getValue();
          controller.setLightColor((brightness << 16) + (brightness << 8) + brightness);
        }
      });
    controller.addPropertyChangeListener(Home3DAttributesController.Property.LIGHT_COLOR, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            brightnessSlider.setValue(controller.getLightColor() & 0xFF);
          }
        });
    
   this.wallsTransparencyLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        Home3DAttributesPanel.class, "wallsTransparencyLabel.text"));
    this.wallsTransparencySlider = new JSlider(0, 255);
    this.opaqueWallsTransparencyLabel = new JLabel(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "opaqueLabel.text"));
    this.invisibleWallsTransparencyLabel = new JLabel(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "invisibleLabel.text"));
    this.wallsTransparencySlider.setPaintTicks(true);
    this.wallsTransparencySlider.setMajorTickSpacing(17);
    this.wallsTransparencySlider.setValue((int)(controller.getWallsAlpha() * 255));
    this.wallsTransparencySlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.setWallsAlpha(wallsTransparencySlider.getValue() / 255f);
        }
      });
    controller.addPropertyChangeListener(Home3DAttributesController.Property.WALLS_ALPHA, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            wallsTransparencySlider.setValue((int)(controller.getWallsAlpha() * 255));
          }
        });
    
    this.dialogTitle = preferences.getLocalizedString(
        Home3DAttributesPanel.class, "home3DAttributes.title");
  }

  // 버튼 업데이트
  private void updateGroundRadioButtons(Home3DAttributesController controller) {
    if (controller.getGroundPaint() == Home3DAttributesController.EnvironmentPaint.COLORED) {
      this.groundColorRadioButton.setSelected(true);
    } else {
      this.groundTextureRadioButton.setSelected(true);
    } 
  }

  // 버튼 업데이트2
  private void updateSkyRadioButtons(Home3DAttributesController controller) {
    if (controller.getSkyPaint() == Home3DAttributesController.EnvironmentPaint.COLORED) {
      this.skyColorRadioButton.setSelected(true);
    } else {
      this.skyTextureRadioButton.setSelected(true);
    } 
  }
  
  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      this.groundColorRadioButton.setMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              Home3DAttributesPanel.class,"groundColorRadioButton.mnemonic")).getKeyCode());
      this.groundTextureRadioButton.setMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              Home3DAttributesPanel.class,"groundTextureRadioButton.mnemonic")).getKeyCode());
      this.skyColorRadioButton.setMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              Home3DAttributesPanel.class,"skyColorRadioButton.mnemonic")).getKeyCode());
      this.skyTextureRadioButton.setMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              Home3DAttributesPanel.class,"skyTextureRadioButton.mnemonic")).getKeyCode());
      this.brightnessLabel.setDisplayedMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              Home3DAttributesPanel.class,"brightnessLabel.mnemonic")).getKeyCode());
      this.brightnessLabel.setLabelFor(this.brightnessSlider);
      this.wallsTransparencyLabel.setDisplayedMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              Home3DAttributesPanel.class,"wallsTransparencyLabel.mnemonic")).getKeyCode());
      this.wallsTransparencyLabel.setLabelFor(this.wallsTransparencySlider);
    }
  }
  

  private void layoutComponents(UserPreferences preferences) {
    int labelAlignment = OperatingSystem.isMacOSX() 
        ? GridBagConstraints.LINE_END
        : GridBagConstraints.LINE_START;
    JPanel groundPanel = SwingTools.createTitledPanel(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "groundPanel.title"));
    Insets labelInsets = new Insets(0, 0, 2, 5);
    groundPanel.add(this.groundColorRadioButton, new GridBagConstraints(
        0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, labelInsets, 0, 0));
    groundPanel.add(this.groundColorButton, new GridBagConstraints(
        1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));
    groundPanel.add(this.groundTextureRadioButton, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    groundPanel.add(this.groundTextureComponent, new GridBagConstraints(
        1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    Insets rowInsets;
    if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
      rowInsets = new Insets(0, 0, 0, 0);
    } else {
      rowInsets = new Insets(0, 0, 5, 0);
    }
    add(groundPanel, new GridBagConstraints(
        0, 1, 1, 1, 0.5, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, rowInsets, 0, 0));
    
    JPanel skyPanel = SwingTools.createTitledPanel(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "skyPanel.title"));
    skyPanel.add(this.skyColorRadioButton, new GridBagConstraints(
        0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, labelInsets, 0, 0));
    skyPanel.add(this.skyColorButton, new GridBagConstraints(
        1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));
    skyPanel.add(this.skyTextureRadioButton, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    skyPanel.add(this.skyTextureComponent, new GridBagConstraints(
        1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(skyPanel, new GridBagConstraints(
        1, 1, 1, 1, 0.5, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, rowInsets, 0, 0));
    
    JPanel renderingPanel = SwingTools.createTitledPanel(preferences.getLocalizedString(
        Home3DAttributesPanel.class, "renderingPanel.title"));
    renderingPanel.add(this.brightnessLabel, new GridBagConstraints(
        0, 0, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    renderingPanel.add(this.brightnessSlider, new GridBagConstraints(
        1, 0, 3, 1, 1, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    JPanel brightnessLabelsPanel = new JPanel(new BorderLayout(20, 0));
    brightnessLabelsPanel.setOpaque(false);
    brightnessLabelsPanel.add(this.darkBrightnessLabel, BorderLayout.WEST);
    brightnessLabelsPanel.add(this.brightBrightnessLabel, BorderLayout.EAST);
    renderingPanel.add(brightnessLabelsPanel, new GridBagConstraints(
        1, 1, 3, 1, 1, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, new Insets(OperatingSystem.isWindows() ? 0 : -3, 0, 3, 0), 0, 0));
    renderingPanel.add(this.wallsTransparencyLabel, new GridBagConstraints(
        0, 2, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    renderingPanel.add(this.wallsTransparencySlider, new GridBagConstraints(
        1, 2, 3, 1, 1, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    JPanel wallsTransparencyLabelsPanel = new JPanel(new BorderLayout(20, 0));
    wallsTransparencyLabelsPanel.setOpaque(false);
    wallsTransparencyLabelsPanel.add(this.opaqueWallsTransparencyLabel, BorderLayout.WEST);
    wallsTransparencyLabelsPanel.add(this.invisibleWallsTransparencyLabel, BorderLayout.EAST);
    renderingPanel.add(wallsTransparencyLabelsPanel, new GridBagConstraints(
        1, 3, 3, 1, 1, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, new Insets(OperatingSystem.isWindows() ? 0 : -3, 0, 10, 0), 0, 0));
    add(renderingPanel, new GridBagConstraints(
        0, 2, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }
  public void displayView(View parentView) {
    if (SwingTools.showConfirmDialog((JComponent)parentView, 
            this, this.dialogTitle, this.wallsTransparencySlider) == JOptionPane.OK_OPTION
        && this.controller != null) {
      this.controller.modify3DAttributes();
    }
  }
}
