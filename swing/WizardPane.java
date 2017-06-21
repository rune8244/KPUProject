package com.eteks.homeview3d.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.DialogView;
import com.eteks.homeview3d.viewcontroller.View;
import com.eteks.homeview3d.viewcontroller.WizardController;

public class WizardPane extends JOptionPane implements DialogView {
  private final UserPreferences  preferences;
  private final WizardController controller;
  private JButton          backOptionButton;
  private JButton          nextFinishOptionButton;
  private String           defaultTitle;
  private JDialog          dialog;


  public WizardPane(UserPreferences preferences,
                    final WizardController controller) {
    this.preferences = preferences;
    this.controller = controller;
    this.defaultTitle = preferences.getLocalizedString(WizardPane.class, "wizard.title");
    
    setMessage(new JPanel(new BorderLayout(10, 0)));
    
    createOptionButtons(preferences, controller);    
    setOptionType(DEFAULT_OPTION);
    String cancelOption = preferences.getLocalizedString(WizardPane.class, "cancelOption");
    if (UIManager.getBoolean("OptionPane.isYesLast")
        || OperatingSystem.isMacOSX()) {
      setOptions(new Object [] {this.nextFinishOptionButton, this.backOptionButton, cancelOption});      
    } else {
      setOptions(new Object [] {this.backOptionButton, this.nextFinishOptionButton, cancelOption});      
    }
    setInitialValue(this.nextFinishOptionButton);
    
    updateStepView(controller);
    controller.addPropertyChangeListener(WizardController.Property.STEP_VIEW, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            updateStepView(controller);
          }
        });
    
    updateStepIcon(controller);
    controller.addPropertyChangeListener(WizardController.Property.STEP_ICON, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            updateStepIcon(controller);
          }
        });
  }

  private void createOptionButtons(UserPreferences preferences, 
                                   final WizardController controller) {
    this.backOptionButton = new JButton(SwingTools.getLocalizedLabelText(preferences, 
        WizardPane.class, "backOptionButton.text"));
    this.backOptionButton.setEnabled(controller.isBackStepEnabled());
    controller.addPropertyChangeListener(WizardController.Property.BACK_STEP_ENABLED, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            backOptionButton.setEnabled(controller.isBackStepEnabled());
          }
        });
    if (!OperatingSystem.isMacOSX()) {
      this.backOptionButton.setMnemonic(
          KeyStroke.getKeyStroke(preferences.getLocalizedString(
              WizardPane.class, "backOptionButton.mnemonic")).getKeyCode());
    }

    this.nextFinishOptionButton = new JButton();
    this.nextFinishOptionButton.setEnabled(controller.isNextStepEnabled());
    controller.addPropertyChangeListener(WizardController.Property.NEXT_STEP_ENABLED, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            nextFinishOptionButton.setEnabled(controller.isNextStepEnabled());
          }
        });
    
    updateNextFinishOptionButton(controller);
    controller.addPropertyChangeListener(WizardController.Property.LAST_STEP, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            updateNextFinishOptionButton(controller);
          }
        });
    
    this.backOptionButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          controller.goBackToPreviousStep();
        }
      });
    
    this.nextFinishOptionButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          if (controller.isLastStep()) {
            controller.finish();
            setValue(nextFinishOptionButton);
            if (dialog != null) {
              dialog.setVisible(false);
            }
          } else {
            controller.goToNextStep();
          }
        }
      });
  }

 
  private void updateNextFinishOptionButton(WizardController controller) {
    this.nextFinishOptionButton.setText(SwingTools.getLocalizedLabelText(this.preferences, WizardPane.class, 
        controller.isLastStep() 
            ? "finishOptionButton.text" 
            : "nextOptionButton.text"));
    if (!OperatingSystem.isMacOSX()) {
      this.nextFinishOptionButton.setMnemonic(KeyStroke.getKeyStroke(
          this.preferences.getLocalizedString(WizardPane.class, 
              controller.isLastStep() 
                  ? "finishOptionButton.mnemonic" 
                  : "nextOptionButton.mnemonic")).getKeyCode());
    }
  }
  
  
  private void updateStepView(WizardController controller) {
    JPanel messagePanel = (JPanel)getMessage();
    Component previousStepView = ((BorderLayout)messagePanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    if (previousStepView != null) {
      messagePanel.remove(previousStepView);
    }
    View stepView = controller.getStepView();
    if (stepView  != null) {
      messagePanel.add((JComponent)stepView, BorderLayout.CENTER);
    } 
    if (this.dialog != null && !this.controller.isResizable()) {
      this.dialog.pack();
    }
  }

  private void updateStepIcon(WizardController controller) {
    JPanel messagePanel = (JPanel)getMessage();
    Component previousStepIconLabel = ((BorderLayout)messagePanel.getLayout()).getLayoutComponent(BorderLayout.WEST);
    if (previousStepIconLabel != null) {
      messagePanel.remove(previousStepIconLabel);
    }
    URL stepIcon = controller.getStepIcon();
    if (stepIcon != null) {
      Color backgroundColor1 = new Color(163, 168, 226);
      Color backgroundColor2 = new Color(80, 86, 158);
      try {
        String [] stepIconBackgroundColors = this.preferences.getLocalizedString(
            WizardPane.class, "stepIconBackgroundColors").trim().split(" ");
        backgroundColor1 = new Color(Integer.decode(stepIconBackgroundColors [0]));
        if (stepIconBackgroundColors.length == 1) {
          backgroundColor2 = backgroundColor1;  
        } else if (stepIconBackgroundColors.length == 2) {
          backgroundColor2 = new Color(Integer.decode(stepIconBackgroundColors [1]));;
        }
      } catch (IllegalArgumentException ex) {
      }
         
      final Color gradientColor1 = backgroundColor1;
      final Color gradientColor2 = backgroundColor2;
      JLabel iconLabel = new JLabel(new ImageIcon(stepIcon)) {
          @Override
          protected void paintComponent(Graphics g) {
            Graphics2D g2D = (Graphics2D)g;
            g2D.setPaint(new GradientPaint(0, 0, gradientColor1, 
                                           0, getHeight(), gradientColor2));
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
          }
        };
      iconLabel.setBorder(new BevelBorder(BevelBorder.LOWERED) {
          @Override
          public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
          }
          
          @Override
          protected void paintLoweredBevel(Component c, Graphics g, int x, int y,
                                           int width, int height)  {
            Color oldColor = g.getColor();
            g.translate(x, y);

            g.setColor(getShadowInnerColor(c));
            g.drawLine(0, 0, 0, height - 1);
            g.drawLine(0, 0, width - 1, 0);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(0, height - 1, width - 1, height - 1);
            g.drawLine(width - 1, 1, width - 1, height - 2);

            g.translate(-x, -y);
            g.setColor(oldColor);
          }
        });
      messagePanel.add(iconLabel, BorderLayout.LINE_START);
    } 
  }
 
  public void displayView(View parentView) {
    this.dialog = createDialog(SwingUtilities.getRootPane((JComponent)parentView), 
        this.controller.getTitle() != null 
            ? this.controller.getTitle() 
            : this.defaultTitle);
    this.controller.addPropertyChangeListener(WizardController.Property.TITLE, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            dialog.setTitle(controller.getTitle() != null 
                                ? controller.getTitle() 
                                : defaultTitle);
          }
        });
    
    this.dialog.applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));    
    this.dialog.setResizable(this.controller.isResizable());
    this.controller.addPropertyChangeListener(WizardController.Property.RESIZABLE, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            dialog.setResizable(controller.isResizable());
          }
        });
    
    
    this.dialog.pack();
    this.dialog.setMinimumSize(getSize());
    this.dialog.setVisible(true);
    this.dialog.dispose();
  }
}
