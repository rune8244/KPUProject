package com.eteks.homeview3d.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.DialogView;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.PrintPreviewController;
import com.eteks.homeview3d.viewcontroller.View;

public class PrintPreviewPanel extends JPanel implements DialogView {
  private enum ActionType {SHOW_PREVIOUS_PAGE, SHOW_NEXT_PAGE}

  private final UserPreferences  preferences;
  private JToolBar               toolBar;
  private HomePrintableComponent printableComponent;
  private JLabel                 pageLabel;

 
  public PrintPreviewPanel(Home home,
                           UserPreferences preferences, 
                           HomeController homeController,
                           PrintPreviewController printPreviewController) {
    super(new ProportionalLayout());
    this.preferences = preferences;
    createActions(preferences);
    installKeyboardActions();
    createComponents(home, homeController);
    layoutComponents();
    updateComponents();
  }

  private void createActions(UserPreferences preferences) {
    Action showPreviousPageAction = new ResourceAction(
          preferences, PrintPreviewPanel.class, ActionType.SHOW_PREVIOUS_PAGE.name()) {
        @Override
        public void actionPerformed(ActionEvent e) {
          printableComponent.setPage(printableComponent.getPage() - 1);
          updateComponents();
        }
      };
    Action showNextPageAction = new ResourceAction(
          preferences, PrintPreviewPanel.class, ActionType.SHOW_NEXT_PAGE.name()) {
        @Override
        public void actionPerformed(ActionEvent e) {
          printableComponent.setPage(printableComponent.getPage() + 1);
          updateComponents();
        }
      };
    ActionMap actionMap = getActionMap();
    actionMap.put(ActionType.SHOW_PREVIOUS_PAGE, showPreviousPageAction);
    actionMap.put(ActionType.SHOW_NEXT_PAGE, showNextPageAction);
  }

  private void installKeyboardActions() {
    InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
    inputMap.put(KeyStroke.getKeyStroke("LEFT"), ActionType.SHOW_PREVIOUS_PAGE);
    inputMap.put(KeyStroke.getKeyStroke("UP"), ActionType.SHOW_PREVIOUS_PAGE);
    inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), ActionType.SHOW_PREVIOUS_PAGE);
    inputMap.put(KeyStroke.getKeyStroke("RIGHT"), ActionType.SHOW_NEXT_PAGE);
    inputMap.put(KeyStroke.getKeyStroke("DOWN"), ActionType.SHOW_NEXT_PAGE);
    inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), ActionType.SHOW_NEXT_PAGE);
  }

  private void createComponents(Home home, HomeController homeController) {
    this.printableComponent = new HomePrintableComponent(home, homeController, getFont());
    this.printableComponent.setBorder(BorderFactory.createCompoundBorder(
        new AbstractBorder() {
          @Override
          public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 5, 5);
          }

          @Override
          public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2D = (Graphics2D)g;
            Color oldColor = g2D.getColor();
            for (int i = 0; i < 5; i++) {
              g2D.setColor(new Color(128, 128, 128, 200 - i * 45));
              g2D.drawLine(x + width - 5 + i, y + i, x + width - 5 + i, y + height - 5 + i);
              g2D.drawLine(x + i, y + height - 5 + i, x + width - 5 + i - 1, y + height - 5 + i);
            }
            g2D.setColor(oldColor);
          }
        },
        BorderFactory.createLineBorder(Color.BLACK)));
    
    this.pageLabel = new JLabel();
    
    this.toolBar = new JToolBar() {
        public void applyComponentOrientation(ComponentOrientation orientation) {
        }
      };
    this.toolBar.setFloatable(false);
    ActionMap actions = getActionMap();    
    if (OperatingSystem.isMacOSXLeopardOrSuperior() && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
      class HigherInsetsButton extends JButton {
        public HigherInsetsButton(Action action) {
          super(action);
        }

        @Override
        public Insets getInsets() {
          Insets insets = super.getInsets();
          insets.top += 3;
          insets.bottom += 3;
          return insets;
        }
      }
      toolBar.add(new HigherInsetsButton(actions.get(ActionType.SHOW_PREVIOUS_PAGE)));
      toolBar.add(new HigherInsetsButton(actions.get(ActionType.SHOW_NEXT_PAGE)));
    } else {
      this.toolBar.add(actions.get(ActionType.SHOW_PREVIOUS_PAGE));
      this.toolBar.add(actions.get(ActionType.SHOW_NEXT_PAGE));
    }
    updateToolBarButtonsStyle(this.toolBar);
    
    this.toolBar.add(Box.createHorizontalStrut(20));
    this.toolBar.add(this.pageLabel);
    
    for (int i = 0, n = toolBar.getComponentCount(); i < n; i++) {
      toolBar.getComponentAtIndex(i).setFocusable(false);      
    }
  }
  
  private void updateToolBarButtonsStyle(JToolBar toolBar) {
    if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
      JComponent previousButton = (JComponent)toolBar.getComponentAtIndex(0);
      previousButton.putClientProperty("JButton.buttonType", "segmentedTextured");
      previousButton.putClientProperty("JButton.segmentPosition", "first");
      JComponent nextButton = (JComponent)toolBar.getComponentAtIndex(1);
      nextButton.putClientProperty("JButton.buttonType", "segmentedTextured");
      nextButton.putClientProperty("JButton.segmentPosition", "last");
    }
  }
    
  private void layoutComponents() {
    JPanel panel = new JPanel();
    panel.add(this.toolBar);
    add(panel, ProportionalLayout.Constraints.TOP);
    add(this.printableComponent, ProportionalLayout.Constraints.BOTTOM);
  }

  
  private void updateComponents() {
    ActionMap actions = getActionMap();    
    actions.get(ActionType.SHOW_PREVIOUS_PAGE).setEnabled(this.printableComponent.getPage() > 0);
    actions.get(ActionType.SHOW_NEXT_PAGE).setEnabled(
        this.printableComponent.getPage() < this.printableComponent.getPageCount() - 1);
    this.pageLabel.setText(preferences.getLocalizedString(
        PrintPreviewPanel.class, "pageLabel.text", 
        this.printableComponent.getPage() + 1, this.printableComponent.getPageCount()));
  }


  public void displayView(View parentView) {
    String dialogTitle = preferences.getLocalizedString(PrintPreviewPanel.class, "printPreview.title");
    JOptionPane optionPane = new JOptionPane(this, 
        JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION); 
    if (parentView != null) {
      optionPane.setComponentOrientation(((JComponent)parentView).getComponentOrientation());
    }
    JDialog dialog = optionPane.createDialog(SwingUtilities.getRootPane((JComponent)parentView), dialogTitle);
    dialog.applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));    
    dialog.setResizable(true);
    dialog.pack();
    dialog.setMinimumSize(dialog.getPreferredSize());
    dialog.setVisible(true);
    dialog.dispose();
  }
}
