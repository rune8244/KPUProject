package com.eteks.homeview3d.swing;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.CompassController;
import com.eteks.homeview3d.viewcontroller.DialogView;
import com.eteks.homeview3d.viewcontroller.View;

// 도면 패널
public class CompassPanel extends JPanel implements DialogView {
  private final CompassController controller;
  private JLabel                  xLabel;
  private JSpinner                xSpinner;
  private JLabel                  yLabel;
  private JSpinner                ySpinner;
  private JLabel                  diameterLabel;
  private JSpinner                diameterSpinner;
  private JCheckBox               visibleCheckBox;
  private JComponent              northDirectionComponent;
  private JLabel                  longitudeLabel;
  private JSpinner                longitudeSpinner;
  private JLabel                  latitudeLabel;
  private JSpinner                latitudeSpinner;
  private JLabel                  timeZoneLabel;
  private JComboBox               timeZoneComboBox;
  private JLabel                  northDirectionLabel;
  private JSpinner                northDirectionSpinner;
  private String                  dialogTitle;

  // 패널 생성
  public CompassPanel(UserPreferences preferences,
                      CompassController controller) {
    super(new GridBagLayout());
    this.controller = controller;
    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents(preferences);
  }

  // 요소 생성 및 초기화
  private void createComponents(UserPreferences preferences, 
                                final CompassController controller) {
    // 현재 유닛과 맞는 유닛 이름 가져오기
    String unitName = preferences.getLengthUnit().getName();

    // X축 좌표 관련 정보
    this.xLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        CompassPanel.class, "xLabel.text", unitName));
    float maximumLength = preferences.getLengthUnit().getMaximumLength();
    final NullableSpinner.NullableSpinnerLengthModel xSpinnerModel = 
        new NullableSpinner.NullableSpinnerLengthModel(preferences, -maximumLength, maximumLength);
    this.xSpinner = new NullableSpinner(xSpinnerModel);
    xSpinnerModel.setLength(controller.getX());
    final PropertyChangeListener xChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          xSpinnerModel.setLength((Float)ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(CompassController.Property.X, xChangeListener);
    xSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.X, xChangeListener);
          controller.setX(xSpinnerModel.getLength());
          controller.addPropertyChangeListener(CompassController.Property.X, xChangeListener);
        }
      });
    
    // Y축 좌표 관련 정보
    this.yLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        CompassPanel.class, "yLabel.text", unitName));
    final NullableSpinner.NullableSpinnerLengthModel ySpinnerModel = 
        new NullableSpinner.NullableSpinnerLengthModel(preferences, -maximumLength, maximumLength);
    this.ySpinner = new NullableSpinner(ySpinnerModel);
    ySpinnerModel.setLength(controller.getY());
    final PropertyChangeListener yChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          ySpinnerModel.setLength((Float)ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(CompassController.Property.Y, yChangeListener);
    ySpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.Y, yChangeListener);
          controller.setY(ySpinnerModel.getLength());
          controller.addPropertyChangeListener(CompassController.Property.Y, yChangeListener);
        }
      });
    
    // 직경라벨 생성 및 속성
    this.diameterLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, 
        CompassPanel.class, "diameterLabel.text", unitName));
    final NullableSpinner.NullableSpinnerLengthModel diameterSpinnerModel = 
        new NullableSpinner.NullableSpinnerLengthModel(preferences, 
            preferences.getLengthUnit().getMinimumLength(), preferences.getLengthUnit().getMaximumLength()  / 10);
    this.diameterSpinner = new NullableSpinner(diameterSpinnerModel);
    diameterSpinnerModel.setLength(controller.getDiameter());
    final PropertyChangeListener diameterChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          diameterSpinnerModel.setLength((Float)ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(CompassController.Property.DIAMETER, 
        diameterChangeListener);
    diameterSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.DIAMETER, 
              diameterChangeListener);
          controller.setDiameter(diameterSpinnerModel.getLength());
          controller.addPropertyChangeListener(CompassController.Property.DIAMETER, 
              diameterChangeListener);
        }
      });
    
    // 체크박스 생성
    this.visibleCheckBox = new JCheckBox(SwingTools.getLocalizedLabelText(preferences, 
        CompassPanel.class, "visibleCheckBox.text"));
    this.visibleCheckBox.setSelected(controller.isVisible());
    final PropertyChangeListener visibleChangeListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent ev) {
        visibleCheckBox.setSelected((Boolean)ev.getNewValue());
      }
    };
    controller.addPropertyChangeListener(CompassController.Property.VISIBLE, visibleChangeListener);
    this.visibleCheckBox.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.VISIBLE, visibleChangeListener);
          controller.setVisible(visibleCheckBox.isSelected());
          controller.addPropertyChangeListener(CompassController.Property.VISIBLE, visibleChangeListener);
        }
      });

    this.latitudeLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, CompassPanel.class, "latitudeLabel.text"));
    final SpinnerNumberModel latitudeSpinnerModel = new SpinnerNumberModel(new Float(0), new Float(-90), new Float(90), new Float(5));
    this.latitudeSpinner = new AutoCommitSpinner(latitudeSpinnerModel);
    // 북쪽/남쪽 변경
    JFormattedTextField textField = ((DefaultEditor)this.latitudeSpinner.getEditor()).getTextField();
    NumberFormatter numberFormatter = (NumberFormatter)((DefaultFormatterFactory)textField.getFormatterFactory()).getDefaultFormatter();
    numberFormatter.setFormat(new DecimalFormat("N ##0.000;S ##0.000"));
    textField.setFormatterFactory(new DefaultFormatterFactory(numberFormatter));
    SwingTools.addAutoSelectionOnFocusGain(textField);
    latitudeSpinnerModel.setValue(controller.getLatitudeInDegrees());
    final PropertyChangeListener latitudeChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          latitudeSpinnerModel.setValue((Float)ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(CompassController.Property.LATITUDE_IN_DEGREES, latitudeChangeListener);
    latitudeSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.LATITUDE_IN_DEGREES, latitudeChangeListener);
          controller.setLatitudeInDegrees(((Number)latitudeSpinnerModel.getValue()).floatValue());
          controller.addPropertyChangeListener(CompassController.Property.LATITUDE_IN_DEGREES, latitudeChangeListener);
        }
      });
    
    this.longitudeLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, CompassPanel.class, "longitudeLabel.text"));
    final SpinnerNumberModel longitudeSpinnerModel = new SpinnerNumberModel(new Float(0), new Float(-180), new Float(180), new Float(5));
    this.longitudeSpinner = new AutoCommitSpinner(longitudeSpinnerModel);
    // 동쪽/서쪽 변경
    textField = ((DefaultEditor)this.longitudeSpinner.getEditor()).getTextField();
    numberFormatter = (NumberFormatter)((DefaultFormatterFactory)textField.getFormatterFactory()).getDefaultFormatter();
    numberFormatter.setFormat(new DecimalFormat("E ##0.000;W ##0.000"));
    textField.setFormatterFactory(new DefaultFormatterFactory(numberFormatter));
    SwingTools.addAutoSelectionOnFocusGain(textField);
    longitudeSpinnerModel.setValue(controller.getLongitudeInDegrees());
    final PropertyChangeListener longitudeChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          longitudeSpinnerModel.setValue((Float)ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(CompassController.Property.LONGITUDE_IN_DEGREES, longitudeChangeListener);
    longitudeSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.LONGITUDE_IN_DEGREES, longitudeChangeListener);
          controller.setLongitudeInDegrees(((Number)longitudeSpinnerModel.getValue()).floatValue());
          controller.addPropertyChangeListener(CompassController.Property.LONGITUDE_IN_DEGREES, longitudeChangeListener);
        }
      });

    this.timeZoneLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, CompassPanel.class, "timeZoneLabel.text"));
    List<String> timeZoneIds = new ArrayList<String>(Arrays.asList(TimeZone.getAvailableIDs()));
    // Remove synonymous time zones
    timeZoneIds.remove("GMT");
    timeZoneIds.remove("GMT0");
    timeZoneIds.remove("Etc/GMT0");
    timeZoneIds.remove("Etc/GMT-0");
    timeZoneIds.remove("Etc/GMT+0");
    // Replace Etc/GMT... ids by their English value that are less misleading
    for (int i = 0; i < timeZoneIds.size(); i++) {
      String timeZoneId = timeZoneIds.get(i);
      if (timeZoneId.startsWith("Etc/GMT")) {
        timeZoneIds.set(i, TimeZone.getTimeZone(timeZoneId).getDisplayName(Locale.ENGLISH));
      }
    }
    String [] timeZoneIdsArray = timeZoneIds.toArray(new String [timeZoneIds.size()]);
    Arrays.sort(timeZoneIdsArray);
    this.timeZoneComboBox = new JComboBox(timeZoneIdsArray);
    this.timeZoneComboBox.setSelectedItem(controller.getTimeZone());
    final PropertyChangeListener timeZoneChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          timeZoneComboBox.setSelectedItem(ev.getNewValue());
        }
      };
    controller.addPropertyChangeListener(CompassController.Property.TIME_ZONE, timeZoneChangeListener);
    this.timeZoneComboBox.setRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
          String timeZoneId = (String)value;
          if (timeZoneId.startsWith("GMT")) {
            if (!OperatingSystem.isMacOSX()) {
              setToolTipText(timeZoneId);
            }
          } else {
            String timeZoneDisplayName = TimeZone.getTimeZone(timeZoneId).getDisplayName();
            if (OperatingSystem.isMacOSX()) {
              value = timeZoneId + " - " + timeZoneDisplayName;
            } else {
              // Use tool tip do display the complete time zone information
              setToolTipText(timeZoneId + " - " + timeZoneDisplayName);
            }
          }
          return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
      });

    this.timeZoneComboBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.TIME_ZONE, timeZoneChangeListener);
          controller.setTimeZone((String)timeZoneComboBox.getSelectedItem());
          controller.addPropertyChangeListener(CompassController.Property.TIME_ZONE, timeZoneChangeListener);
        }
      });
    this.timeZoneComboBox.setPrototypeDisplayValue("GMT");
    
    this.northDirectionLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, CompassPanel.class, "northDirectionLabel.text"));
    // 360도 회전 스피너 생성
    final SpinnerNumberModel northDirectionSpinnerModel = new AutoCommitSpinner.SpinnerModuloNumberModel(0, 0, 360, 5);
    this.northDirectionSpinner = new AutoCommitSpinner(northDirectionSpinnerModel);
    northDirectionSpinnerModel.setValue(new Integer(Math.round(controller.getNorthDirectionInDegrees())));
    this.northDirectionComponent = new JComponent() {
        @Override
        public Dimension getPreferredSize() {
          return new Dimension(35, 35);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
          Graphics2D g2D = (Graphics2D) g;
          g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          g2D.translate(getWidth() / 2, getHeight() / 2);
          g2D.scale(getWidth() / 2, getWidth() / 2);
          g2D.rotate(Math.toRadians(controller.getNorthDirectionInDegrees()));
          // Draw a round arc
          g2D.setStroke(new BasicStroke(0.5f / getWidth()));
          g2D.draw(new Ellipse2D.Float(-0.7f, -0.7f, 1.4f, 1.4f));
          g2D.draw(new Line2D.Float(-0.85f, 0, -0.7f, 0));
          g2D.draw(new Line2D.Float(0.85f, 0, 0.7f, 0));
          g2D.draw(new Line2D.Float(0, -0.8f, 0, -0.7f));
          g2D.draw(new Line2D.Float(0, 0.85f, 0, 0.7f));
          // Draw a N
          GeneralPath path = new GeneralPath();
          path.moveTo(-0.1f, -0.8f);
          path.lineTo(-0.1f, -1f);
          path.lineTo(0.1f, -0.8f);
          path.lineTo(0.1f, -1f);
          g2D.setStroke(new BasicStroke(1.5f / getWidth()));
          g2D.draw(path);
          // Draw the needle
          GeneralPath needlePath = new GeneralPath();
          needlePath.moveTo(0, -0.75f);
          needlePath.lineTo(0.2f, 0.7f);
          needlePath.lineTo(0, 0.5f);
          needlePath.lineTo(-0.2f, 0.7f);
          needlePath.closePath();
          needlePath.moveTo(-0.02f, 0);
          needlePath.lineTo(0.02f, 0);
          g2D.setStroke(new BasicStroke(4 / getWidth()));
          g2D.draw(needlePath);
        }
      };
    this.northDirectionComponent.setOpaque(false);
    final PropertyChangeListener northDirectionChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          northDirectionSpinnerModel.setValue(((Number)ev.getNewValue()).intValue());
          northDirectionComponent.repaint();
        }
      };
    controller.addPropertyChangeListener(CompassController.Property.NORTH_DIRECTION_IN_DEGREES, northDirectionChangeListener);
    northDirectionSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          controller.removePropertyChangeListener(CompassController.Property.NORTH_DIRECTION_IN_DEGREES, northDirectionChangeListener);
          controller.setNorthDirectionInDegrees(((Number)northDirectionSpinnerModel.getValue()).floatValue());
          northDirectionComponent.repaint();
          controller.addPropertyChangeListener(CompassController.Property.NORTH_DIRECTION_IN_DEGREES, northDirectionChangeListener);
        }
      });

    this.dialogTitle = preferences.getLocalizedString(CompassPanel.class, "compass.title");
  }

  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      this.xLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "xLabel.mnemonic")).getKeyCode());
      this.xLabel.setLabelFor(this.xSpinner);
      this.yLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "yLabel.mnemonic")).getKeyCode());
      this.yLabel.setLabelFor(this.ySpinner);
      this.diameterLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "diameterLabel.mnemonic")).getKeyCode());
      this.diameterLabel.setLabelFor(this.diameterSpinner);
      this.visibleCheckBox.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "visibleCheckBox.mnemonic")).getKeyCode());
      this.latitudeLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "latitudeLabel.mnemonic")).getKeyCode());
      this.latitudeLabel.setLabelFor(this.latitudeSpinner);
      this.longitudeLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "longitudeLabel.mnemonic")).getKeyCode());
      this.longitudeLabel.setLabelFor(this.longitudeSpinner);
      this.timeZoneLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "timeZoneLabel.mnemonic")).getKeyCode());
      this.timeZoneLabel.setLabelFor(this.timeZoneComboBox);
      this.northDirectionLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          CompassPanel.class, "northDirectionLabel.mnemonic")).getKeyCode());
      this.northDirectionLabel.setLabelFor(this.northDirectionSpinner);
    }
  }
  
  // 레이아웃 패널 요소
  private void layoutComponents(UserPreferences preferences) {
    int labelAlignment = OperatingSystem.isMacOSX() 
        ? GridBagConstraints.LINE_END
        : GridBagConstraints.LINE_START;
    // First row
    JPanel compassRosePanel = SwingTools.createTitledPanel(preferences.getLocalizedString(
        CompassPanel.class, "compassRosePanel.title"));
    Insets labelInsets = new Insets(0, 0, 5, 5);
    Insets componentInsets = new Insets(0, 0, 5, 10);
    Insets lastComponentInsets = new Insets(0, 0, 5, 0);
    int rowGap = OperatingSystem.isMacOSXLeopardOrSuperior() ? 0 : 5;
    compassRosePanel.add(this.xLabel, new GridBagConstraints(
        0, 0, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, labelInsets, 0, 0));
    compassRosePanel.add(this.xSpinner, new GridBagConstraints(
        1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 20), -10, 0));
    compassRosePanel.add(this.visibleCheckBox, new GridBagConstraints(
        2, 0, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, lastComponentInsets, 0, 0));
    compassRosePanel.add(this.yLabel, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    compassRosePanel.add(this.ySpinner, new GridBagConstraints(
        1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 20), -10, 0));
    compassRosePanel.add(this.diameterLabel, new GridBagConstraints(
        2, 1, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    compassRosePanel.add(this.diameterSpinner, new GridBagConstraints(
        3, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    add(compassRosePanel, new GridBagConstraints(
        0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, rowGap, 0), 0, 0));
    JPanel geographicLocationPanel = SwingTools.createTitledPanel(preferences.getLocalizedString(
        CompassPanel.class, "geographicLocationPanel.title"));
    geographicLocationPanel.add(this.latitudeLabel, new GridBagConstraints(
        0, 0, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, labelInsets, 0, 0));
    geographicLocationPanel.add(this.latitudeSpinner, new GridBagConstraints(
        1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, componentInsets, 20, 0));
    geographicLocationPanel.add(this.northDirectionLabel, new GridBagConstraints(
        2, 0, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, labelInsets, 0, 0));
    geographicLocationPanel.add(this.northDirectionSpinner, new GridBagConstraints(
        3, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0));
    geographicLocationPanel.add(this.northDirectionComponent, new GridBagConstraints(
        4, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
    geographicLocationPanel.add(this.longitudeLabel, new GridBagConstraints(
        0, 1, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    geographicLocationPanel.add(this.longitudeSpinner, new GridBagConstraints(
        1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 20, 0));
    geographicLocationPanel.add(this.timeZoneLabel, new GridBagConstraints(
        2, 1, 1, 1, 0, 0, labelAlignment, 
        GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    geographicLocationPanel.add(this.timeZoneComboBox, new GridBagConstraints(
        3, 1, 2, 1, 0, 0, GridBagConstraints.LINE_START, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    this.timeZoneComboBox.setPreferredSize(new Dimension(this.latitudeSpinner.getPreferredSize().width + 60, 
        this.timeZoneComboBox.getPreferredSize().height));
    add(geographicLocationPanel, new GridBagConstraints(
        0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }

public void displayView(View parentView) {
    JFormattedTextField northDirectionTextField = 
        ((JSpinner.DefaultEditor)this.northDirectionSpinner.getEditor()).getTextField();
    if (SwingTools.showConfirmDialog((JComponent)parentView, 
            this, this.dialogTitle, northDirectionTextField) == JOptionPane.OK_OPTION
        && this.controller != null) {
      this.controller.modifyCompass();
    }
  }
}
