package com.eteks.homeview3d.swing;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import com.eteks.homeview3d.model.UserPreferences;

public class FontNameComboBox extends JComboBox {
  public static final String DEFAULT_SYSTEM_FONT_NAME = "DEFAULT_SYSTEM_FONT_NAME";
  
  private static final String [] availableFontNames;
  
  static {
    availableFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    Arrays.sort(availableFontNames);
  }
  
  private UserPreferences preferences;
  private String          unavailableFontName;
  
  public FontNameComboBox(UserPreferences preferences) {
    this.preferences = preferences;
    DefaultComboBoxModel fontNamesModel = new DefaultComboBoxModel(availableFontNames);
    fontNamesModel.insertElementAt(DEFAULT_SYSTEM_FONT_NAME, 0);
    setModel(fontNamesModel);
    final String systemFontName = preferences.getLocalizedString(FontNameComboBox.class, "systemFontName");
    setRenderer(new DefaultListCellRenderer() {
        private Font rendererDefaultFont;
        private Font rendererSpecialFont;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
          if (value == DEFAULT_SYSTEM_FONT_NAME) {
            value = systemFontName;
          } else if (value != null && Arrays.binarySearch(availableFontNames, value) < 0) {
            value = unavailableFontName;
          }
            
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          
          if (this.rendererDefaultFont == null) {
            this.rendererDefaultFont = this.getFont();
            this.rendererSpecialFont = 
                new Font(this.rendererDefaultFont.getFontName(), Font.ITALIC, this.rendererDefaultFont.getSize());        
          }
          if (value == null
              || value == systemFontName
              || value == unavailableFontName) {
            setFont(this.rendererSpecialFont);
          } else {
            setFont(this.rendererDefaultFont);
          }
          return this;
        }
      });
  }
  
  @Override
  public void setSelectedItem(final Object item) {
    final DefaultComboBoxModel model = (DefaultComboBoxModel)getModel();
    Object firstItem = model.getElementAt(0);
    // 선택한 아이템이 NULL값이면 첫 아이템으로 추가
    if (firstItem != null && item == null) {
      if (firstItem != DEFAULT_SYSTEM_FONT_NAME) {
        model.removeElementAt(0);
      }
      model.insertElementAt(null, 0);
    // 선택한 아이템이 NULL값 아니면 첫 아이템 삭제
    } else if (firstItem == null && item != null) {
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            model.removeElementAt(0);
            FontNameComboBox.super.setSelectedItem(item);
          }
        });
    } else if (firstItem != item 
               && Arrays.binarySearch(availableFontNames, item) < 0 
               && item != DEFAULT_SYSTEM_FONT_NAME) {
      if (firstItem != DEFAULT_SYSTEM_FONT_NAME) {
        model.removeElementAt(0);
      }
      model.insertElementAt(item, 0);
      this.unavailableFontName = preferences.getLocalizedString(FontNameComboBox.class, "unavailableFontName", item);
    }
    super.setSelectedItem(item);
  }
}
