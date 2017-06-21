package com.eteks.homeview3d.swing;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.viewcontroller.ThreadedTaskController;
import com.eteks.homeview3d.viewcontroller.ThreadedTaskView;
import com.eteks.homeview3d.viewcontroller.View;


public class ThreadedTaskPanel extends JPanel implements ThreadedTaskView {
  private final UserPreferences        preferences;
  private final ThreadedTaskController controller;
  private JLabel                       taskLabel;
  private JProgressBar                 taskProgressBar;
  private JDialog                      dialog;
  private boolean                      taskRunning;

  public ThreadedTaskPanel(String taskMessage, 
                           UserPreferences preferences, 
                           ThreadedTaskController controller) {
    super(new BorderLayout(5, 5));
    this.preferences = preferences;
    this.controller = controller;
    createComponents(taskMessage);
    layoutComponents();
  }

  private void createComponents(String taskMessage) {
    this.taskLabel = new JLabel(taskMessage);
    this.taskProgressBar = new JProgressBar();
    this.taskProgressBar.setIndeterminate(true);
  }
    
 
  private void layoutComponents() {
    add(this.taskLabel, BorderLayout.NORTH);
    add(this.taskProgressBar, BorderLayout.SOUTH);
  }
  
  
  public void setIndeterminateProgress() {
    if (EventQueue.isDispatchThread()) {
      this.taskProgressBar.setIndeterminate(true);
    } else {
      invokeLater(new Runnable() {
          public void run() {
            setIndeterminateProgress();
          }
        });
    }
  }
  
  public void setProgress(final int value, 
                          final int minimum, 
                          final int maximum) {
    if (EventQueue.isDispatchThread()) {
      this.taskProgressBar.setIndeterminate(false);
      this.taskProgressBar.setValue(value);
      this.taskProgressBar.setMinimum(minimum);
      this.taskProgressBar.setMaximum(maximum);
    } else {
      invokeLater(new Runnable() {
          public void run() {
            setProgress(value, minimum, maximum);
          }
        });
    }
  }
  
  
  public void invokeLater(Runnable runnable) {
    EventQueue.invokeLater(runnable);
  }

  public void setTaskRunning(boolean taskRunning, View executingView) {
    this.taskRunning = taskRunning;
    if (taskRunning && this.dialog == null) {
      String dialogTitle = this.preferences.getLocalizedString(
          ThreadedTaskPanel.class, "threadedTask.title");
      final JButton cancelButton = new JButton(this.preferences.getLocalizedString(
          ThreadedTaskPanel.class, "cancelButton.text"));
      
      final JOptionPane optionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, 
          JOptionPane.DEFAULT_OPTION, null, new Object [] {cancelButton});
      cancelButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            optionPane.setValue(cancelButton);
          }
        });
      this.dialog = optionPane.createDialog(SwingUtilities.getRootPane((JComponent)executingView), dialogTitle);
      
      new Timer(200, new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            ((Timer)ev.getSource()).stop();
            if (controller.isTaskRunning()) {
              dialog.setVisible(true);
              
              dialog.dispose();
              if (ThreadedTaskPanel.this.taskRunning 
                  && (cancelButton == optionPane.getValue() 
                      || new Integer(JOptionPane.CLOSED_OPTION).equals(optionPane.getValue()))) {
                dialog = null;
                controller.cancelTask();
              }
            }
          }
        }).start();
    } else if (!taskRunning && this.dialog != null) {
      this.dialog.setVisible(false);
    }
  }
}
