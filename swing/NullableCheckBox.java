package com.eteks.homeview3d.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class NullableCheckBox extends JComponent {    
  public static final String TEXT_CHANGED_PROPERTY = "text";
  public static final String MNEMONIC_CHANGED_PROPERTY = "mnemonic";
  private JCheckBox    checkBox;
  private Boolean      value = Boolean.FALSE;
  private boolean      nullable;
  private List<ChangeListener> changeListeners = new ArrayList<ChangeListener>(1);
  public NullableCheckBox(String text) {
    final Dimension checkBoxSize = new JCheckBox().getPreferredSize();
    this.checkBox = new JCheckBox(text) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (value == null) {
          g.drawRect(checkBoxSize.width / 2 - 3, checkBoxSize.height / 2, 6, 1);
        }
      }
    };
    ItemListener checkBoxListener = new ItemListener() {
      public void itemStateChanged(ItemEvent ev) {
        ev.getItemSelectable().removeItemListener(this);
        if (nullable) {
          if (getValue() == Boolean.FALSE) {
            setValue(null);
          } else if (getValue() == null) {
            setValue(Boolean.TRUE);
          } else {
            setValue(Boolean.FALSE);
          }
        } else {
          setValue(checkBox.isSelected());
        }
        ev.getItemSelectable().addItemListener(this);
      }
    };
    this.checkBox.addItemListener(checkBoxListener);
    
    setLayout(new GridLayout());
    add(this.checkBox);
  }
  
  public Boolean getValue() {
    return this.value;
  }

  public void setValue(Boolean value) {
    if (value != this.value) {
      this.value = value;
      if (value != null) {
        this.checkBox.setSelected(value);
      } else if (isNullable()) {
        this.checkBox.setSelected(false);
      } else {
        throw new IllegalArgumentException("Check box isn't nullable");
      }
      this.checkBox.repaint();
      fireStateChanged();
    }
  }
  
  public boolean isNullable() {
    return this.nullable;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
    if (!nullable && getValue() == null) {
      setValue(Boolean.FALSE);
    }
  }
  
  public void setMnemonic(int mnemonic) {
    int oldMnemonic = this.checkBox.getMnemonic();
    if (oldMnemonic != mnemonic) {
      this.checkBox.setMnemonic(mnemonic);
      firePropertyChange(MNEMONIC_CHANGED_PROPERTY, oldMnemonic, mnemonic);
    }
  }
  
  public int getMnemonic() {
    return this.checkBox.getMnemonic();
  }

  public void setText(String text) {
    String oldText = this.checkBox.getText();
    if (oldText != text) {
      this.checkBox.setText(text);
      firePropertyChange(TEXT_CHANGED_PROPERTY, oldText, text);
    }
  }
  
  public String getText() {
    return this.checkBox.getText();
  }
 
  public void setToolTipText(String text) {
    this.checkBox.setToolTipText(text);
  }

  @Override
  public void setEnabled(boolean enabled) {
    if (this.checkBox.isEnabled() != enabled) {
      this.checkBox.setEnabled(enabled);
      firePropertyChange("enabled", !enabled, enabled);
    }
  }

  @Override
  public boolean isEnabled() {
    return this.checkBox.isEnabled();
  }
  
  public void addChangeListener(final ChangeListener l) {
    this.changeListeners.add(l);
  }


  public void removeChangeListener(final ChangeListener l) {
    this.changeListeners.remove(l);
  }

  private void fireStateChanged() {
    if (!this.changeListeners.isEmpty()) {
      ChangeEvent changeEvent = new ChangeEvent(this);
      ChangeListener [] listeners = this.changeListeners.
        toArray(new ChangeListener [this.changeListeners.size()]);
      for (ChangeListener listener : listeners) {
        listener.stateChanged(changeEvent);
      }
    }
  }
}