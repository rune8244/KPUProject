package com.eteks.homeview3d.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.DialogView;
import com.eteks.homeview3d.viewcontroller.LabelController;
import com.eteks.homeview3d.viewcontroller.View;

public class LabelPanel extends JPanel implements DialogView {
  private final boolean         labelModification;
  private final LabelController controller;
  private JLabel                textLabel;
  private JTextField            textTextField;
  private JLabel                fontNameLabel;
  private FontNameComboBox      fontNameComboBox;
  private JLabel                fontSizeLabel;
  private JSpinner              fontSizeSpinner;
  private JLabel                colorLabel;
  private ColorButton           colorButton;
  private NullableCheckBox      visibleIn3DViewCheckBox;
  private JLabel                pitchLabel;
  private JRadioButton          pitch0DegreeRadioButton;
  private JRadioButton          pitch90DegreeRadioButton;
  private JLabel                elevationLabel;
  private JSpinner              elevationSpinner;
  private String                dialogTitle;

  // 패널 생성
  public LabelPanel(boolean modification,
                    UserPreferences preferences,
                    LabelController controller) {
    super(new GridBagLayout());
    this.labelModification = modification;
    this.controller = controller;
    createComponents(modification, preferences, controller);
    setMnemonics(preferences);
    layoutComponents(controller, preferences);
  }

  // 요소 생성 및 초기화
  private void createComponents(boolean modification, 
                                UserPreferences preferences, 
                                final LabelController controller) {
    this.textLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "textLabel.text"));
    this.textTextField = new AutoCompleteTextField(controller.getText(), 20, preferences.getAutoCompletionStrings("LabelText"));
    if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
      SwingTools.addAutoSelectionOnFocusGain(this.textTextField);
    }
    final PropertyChangeListener textChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          textTextField.setText(controller.getText());
        }
      };
    controller.addPropertyChangeListener(LabelController.Property.TEXT, textChangeListener);
    this.textTextField.getDocument().addDocumentListener(new DocumentListener() {
        public void changedUpdate(DocumentEvent ev) {
          controller.removePropertyChangeListener(LabelController.Property.TEXT, textChangeListener);
          String text = textTextField.getText(); 
          if (text == null || text.trim().length() == 0) {
            controller.setText("");
          } else {
            controller.setText(text);
          }
          controller.addPropertyChangeListener(LabelController.Property.TEXT, textChangeListener);
        }
  
        public void insertUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }
  
        public void removeUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }
      });
    
    this.fontNameLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "fontNameLabel.text"));
    this.fontNameComboBox = new FontNameComboBox(preferences);
    this.fontNameComboBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ev) {
          String selectedItem = (String)fontNameComboBox.getSelectedItem();
          controller.setFontName(selectedItem == FontNameComboBox.DEFAULT_SYSTEM_FONT_NAME 
              ? null : selectedItem);
        }
      });
    PropertyChangeListener fontNameChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          if (controller.isFontNameSet()) {
            String fontName = controller.getFontName();
            fontNameComboBox.setSelectedItem(fontName == null 
                ? FontNameComboBox.DEFAULT_SYSTEM_FONT_NAME : fontName);
          } else {
            fontNameComboBox.setSelectedItem(null);
          }
        }
      };
    controller.addPropertyChangeListener(LabelController.Property.FONT_NAME, fontNameChangeListener);
    fontNameChangeListener.propertyChange(null);

    String unitName = preferences.getLengthUnit().getName();
    this.fontSizeLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, LabelPanel.class,
        "fontSizeLabel.text", unitName));
    final NullableSpinner.NullableSpinnerLengthModel fontSizeSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
        preferences, 5, 999);
    this.fontSizeSpinner = new NullableSpinner(fontSizeSpinnerModel);
    final PropertyChangeListener fontSizeChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          Float fontSize = controller.getFontSize();
          fontSizeSpinnerModel.setNullable(fontSize == null);
          fontSizeSpinnerModel.setLength(fontSize);
        }
      };
    fontSizeChangeListener.propertyChange(null);
    controller.addPropertyChangeListener(LabelController.Property.FONT_SIZE, fontSizeChangeListener);
    fontSizeSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(LabelController.Property.FONT_SIZE, fontSizeChangeListener);
          controller.setFontSize(fontSizeSpinnerModel.getLength());
          controller.addPropertyChangeListener(LabelController.Property.FONT_SIZE, fontSizeChangeListener);
        }
      });

    this.colorLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "colorLabel.text"));
    
    this.colorButton = new ColorButton(preferences);
    if (OperatingSystem.isMacOSX()) {
      this.colorButton.putClientProperty("JButton.buttonType", "segmented");
      this.colorButton.putClientProperty("JButton.segmentPosition", "only");
    }
    this.colorButton.setColorDialogTitle(preferences
        .getLocalizedString(LabelPanel.class, "colorDialog.title"));
    this.colorButton.setColor(controller.getColor());
    this.colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          controller.setColor(colorButton.getColor());
        }
      });
    controller.addPropertyChangeListener(LabelController.Property.COLOR, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          colorButton.setColor(controller.getColor());
        }
      });

    final PropertyChangeListener pitchChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          update3DViewComponents(controller);
        }
      };
    controller.addPropertyChangeListener(LabelController.Property.PITCH, pitchChangeListener);
    this.visibleIn3DViewCheckBox = new NullableCheckBox(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "visibleIn3DViewCheckBox.text"));
    if (controller.isPitchEnabled() != null) {
      this.visibleIn3DViewCheckBox.setValue(controller.isPitchEnabled());
    } else {
      this.visibleIn3DViewCheckBox.setNullable(true);
      this.visibleIn3DViewCheckBox.setValue(null);
    }
    this.visibleIn3DViewCheckBox.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(LabelController.Property.PITCH, pitchChangeListener);
          if (visibleIn3DViewCheckBox.isNullable()) {
            visibleIn3DViewCheckBox.setNullable(false);
          }
          if (Boolean.FALSE.equals(visibleIn3DViewCheckBox.getValue())) {
            controller.setPitch(null);
          } else if (pitch90DegreeRadioButton.isSelected()) {
            controller.setPitch((float)(Math.PI / 2));
          } else {
            controller.setPitch(0f);
          }
          update3DViewComponents(controller);
          controller.addPropertyChangeListener(LabelController.Property.PITCH, pitchChangeListener);
        }
      });
    this.pitchLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "pitchLabel.text"));
    this.pitch0DegreeRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "pitch0DegreeRadioButton.text"));
    ItemListener pitchRadioButtonsItemListener = new ItemListener() {
        public void itemStateChanged(ItemEvent ev) {
          if (pitch0DegreeRadioButton.isSelected()) {
            controller.setPitch(0f);
          } else if (pitch90DegreeRadioButton.isSelected()) {
            controller.setPitch((float)(Math.PI / 2));
          } 
        }
      };
    this.pitch0DegreeRadioButton.addItemListener(pitchRadioButtonsItemListener);
    this.pitch90DegreeRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "pitch90DegreeRadioButton.text"));
    this.pitch90DegreeRadioButton.addItemListener(pitchRadioButtonsItemListener);
    ButtonGroup pitchGroup = new ButtonGroup();
    pitchGroup.add(this.pitch0DegreeRadioButton);
    pitchGroup.add(this.pitch90DegreeRadioButton);
    
    this.elevationLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        LabelPanel.class, "elevationLabel.text", unitName));
    final NullableSpinner.NullableSpinnerLengthModel elevationSpinnerModel = new NullableSpinner.NullableSpinnerLengthModel(
        preferences, 0f, preferences.getLengthUnit().getMaximumElevation());
    this.elevationSpinner = new NullableSpinner(elevationSpinnerModel);
    elevationSpinnerModel.setNullable(controller.getElevation() == null);
    elevationSpinnerModel.setLength(controller.getElevation());
    final PropertyChangeListener elevationChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          elevationSpinnerModel.setNullable(ev.getNewValue() == null);
          elevationSpinnerModel.setLength((Float)ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(LabelController.Property.ELEVATION, elevationChangeListener);
    elevationSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(LabelController.Property.ELEVATION, elevationChangeListener);
          controller.setElevation(elevationSpinnerModel.getLength());
          controller.addPropertyChangeListener(LabelController.Property.ELEVATION, elevationChangeListener);
        }
      });

    update3DViewComponents(controller);
    
    this.dialogTitle = preferences.getLocalizedString(LabelPanel.class, 
        modification 
            ? "labelModification.title"
            : "labelCreation.title");
  }

  private void update3DViewComponents(LabelController controller) {
    boolean visibleIn3D = Boolean.TRUE.equals(controller.isPitchEnabled());
    this.pitch0DegreeRadioButton.setEnabled(visibleIn3D);
    this.pitch90DegreeRadioButton.setEnabled(visibleIn3D);
    this.elevationSpinner.setEnabled(visibleIn3D);
    if (controller.getPitch() != null) {
      if (controller.getPitch() == 0) {
        this.pitch0DegreeRadioButton.setSelected(true);
      } else if (controller.getPitch() == (float)(Math.PI / 2)) {
        this.pitch90DegreeRadioButton.setSelected(true);
      }
    }
  }
  
  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      this.textLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          LabelPanel.class, "textLabel.mnemonic")).getKeyCode());
      this.textLabel.setLabelFor(this.textTextField);
      this.fontNameLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          LabelPanel.class, "fontNameLabel.mnemonic")).getKeyCode());
      this.fontNameLabel.setLabelFor(this.fontNameComboBox);
      this.fontSizeLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(
          preferences.getLocalizedString(LabelPanel.class, "fontSizeLabel.mnemonic")).getKeyCode());
      this.fontSizeLabel.setLabelFor(this.fontSizeSpinner);
      this.visibleIn3DViewCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString( 
          LabelPanel.class, "visibleIn3DViewCheckBox.mnemonic")).getKeyCode());
      this.pitch0DegreeRadioButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString( 
          LabelPanel.class, "pitch0DegreeRadioButton.mnemonic")).getKeyCode());
      this.pitch90DegreeRadioButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString( 
          LabelPanel.class, "pitch90DegreeRadioButton.mnemonic")).getKeyCode());
      this.elevationLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          LabelPanel.class, "elevationLabel.mnemonic")).getKeyCode());
      this.elevationLabel.setLabelFor(this.elevationSpinner);
    }
  }
  
  private void layoutComponents(final LabelController controller, UserPreferences preferences) {
    int labelAlignment = OperatingSystem.isMacOSX() 
        ? GridBagConstraints.LINE_END
        : GridBagConstraints.LINE_START;
    
    JPanel nameAndStylePanel = SwingTools.createTitledPanel(
        preferences.getLocalizedString(LabelPanel.class, "textAndStylePanel.title"));
    nameAndStylePanel.add(this.textLabel, new GridBagConstraints(
        0, 0, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));
    nameAndStylePanel.add(this.textTextField, new GridBagConstraints(
        1, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 0, 0));
    nameAndStylePanel.add(this.fontNameLabel, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));
    Dimension preferredSize = this.fontNameComboBox.getPreferredSize();
    preferredSize.width = Math.min(preferredSize.width, this.textTextField.getPreferredSize().width);
    this.fontNameComboBox.setPreferredSize(preferredSize);
    nameAndStylePanel.add(this.fontNameComboBox, new GridBagConstraints(
        1, 1, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 0), 0, 0));
    nameAndStylePanel.add(this.fontSizeLabel, new GridBagConstraints(
        0, 2, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    nameAndStylePanel.add(this.fontSizeSpinner, new GridBagConstraints(
        1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 5, 0));
    nameAndStylePanel.add(this.colorLabel, new GridBagConstraints(
        2, 2, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 10, 0, 5), 0, 0));
    nameAndStylePanel.add(this.colorButton, new GridBagConstraints(
        3, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, OperatingSystem.isMacOSX()  ? 6  : 0), 0, 0));
    int rowGap = OperatingSystem.isMacOSXLeopardOrSuperior() ? 0 : 5;
    add(nameAndStylePanel, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.BOTH, new Insets(0, 0, rowGap, 0), 0, 0));

    JPanel rendering3DPanel = SwingTools.createTitledPanel(
        preferences.getLocalizedString(LabelPanel.class, "rendering3DPanel.title"));
    rendering3DPanel.add(this.visibleIn3DViewCheckBox, new GridBagConstraints(
        0, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, OperatingSystem.isMacOSX() ? -8 : 0, 5, 0), 0, 0));
    rendering3DPanel.add(this.pitchLabel, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));
    rendering3DPanel.add(this.pitch0DegreeRadioButton, new GridBagConstraints(
        1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));
    rendering3DPanel.add(this.pitch90DegreeRadioButton, new GridBagConstraints(
        2, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
    rendering3DPanel.add(this.elevationLabel, new GridBagConstraints(
        0, 3, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    rendering3DPanel.add(this.elevationSpinner, new GridBagConstraints(
        1, 3, 2, 1, 1, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    add(rendering3DPanel, new GridBagConstraints(
        0, 2, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  
  public void displayView(View parentView) {
    if (SwingTools.showConfirmDialog((JComponent)parentView, 
            this, this.dialogTitle, this.textTextField) == JOptionPane.OK_OPTION
        && this.controller != null) {
      if (this.labelModification) {
        this.controller.modifyLabels();
      } else {
        this.controller.createLabel();
      }
    }
  }
}
