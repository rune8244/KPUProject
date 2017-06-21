package com.eteks.homeview3d.swing;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

public class AutoCommitSpinner extends JSpinner {
  // 스피너 생성
  public AutoCommitSpinner(SpinnerModel model) {
    this(model, null);
  }
  
public AutoCommitSpinner(SpinnerModel model, 
                           Format format) {
    super(model);
    JComponent editor = getEditor();
    if (editor instanceof JSpinner.DefaultEditor) {
      final JFormattedTextField textField = ((JSpinner.DefaultEditor)editor).getTextField();      
      SwingTools.addAutoSelectionOnFocusGain(textField);
      // 편집 도중 텍스트 확인
      if (textField.getFormatterFactory() instanceof DefaultFormatterFactory) {
        DefaultFormatterFactory formatterFactory = (DefaultFormatterFactory)textField.getFormatterFactory();
        JFormattedTextField.AbstractFormatter defaultFormatter = formatterFactory.getDefaultFormatter();
        if (defaultFormatter instanceof DefaultFormatter) {
          ((DefaultFormatter)defaultFormatter).setCommitsOnValidEdit(true);
        }
        if (defaultFormatter instanceof NumberFormatter) {
          final NumberFormatter numberFormatter = (NumberFormatter)defaultFormatter;
          // 기본 함수값 대신할 것 생성
          NumberFormatter editFormatter = new NumberFormatter() {
              private boolean keepFocusedTextUnchanged;

              {
                // Listener 추가
                final KeyAdapter keyListener = new KeyAdapter() {
                    public void keyTyped(KeyEvent ev) {
                       keepFocusedTextUnchanged = true;
                    };
                  };
                textField.addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent ev) {
                      textField.addKeyListener(keyListener);
                    }
                    
                    public void focusLost(FocusEvent ev) {
                      textField.removeKeyListener(keyListener);
                    };
                  });
              }
              
              @Override
              public Format getFormat() {
                Format format = super.getFormat();
               
                if (textField.hasFocus() && format instanceof DecimalFormat) {
                  // 텍스트 잡혀 있을땐 그룹 묶기 안됨
                  DecimalFormat noGroupingFormat = (DecimalFormat)format.clone();
                  noGroupingFormat.setGroupingUsed(false);
                  return noGroupingFormat;
                } else {
                  return format;
                }
              }
            
              @SuppressWarnings({"rawtypes"})
              @Override
              public Comparable getMaximum() {
                return numberFormatter.getMaximum();
              }
              
              @SuppressWarnings({"rawtypes"})
              @Override
              public Comparable getMinimum() {
                return numberFormatter.getMinimum();
              }
              
              @SuppressWarnings({"rawtypes"})
              @Override
              public void setMaximum(Comparable maximum) {
                numberFormatter.setMaximum(maximum);
              }
              
              @SuppressWarnings({"rawtypes"})
              @Override
              public void setMinimum(Comparable minimum) {
                numberFormatter.setMinimum(minimum);
              }
              
              @Override
              public Class<?> getValueClass() {
                return numberFormatter.getValueClass();
              }
              
              @Override
              public String valueToString(Object value) throws ParseException {
                if (textField.hasFocus()
                    && this.keepFocusedTextUnchanged) {
                  this.keepFocusedTextUnchanged = false;
                  return textField.getText();
                } else {
                  return super.valueToString(value);
                }
              }
            };
          editFormatter.setCommitsOnValidEdit(true);
          textField.setFormatterFactory(new DefaultFormatterFactory(editFormatter));
        }
      }
    }
    
    if (format != null) {
      setFormat(format);
    }
  }
  
  // 스피너 값 표시할 때 사용할 포맷 설정
  public void setFormat(Format format) {
    JComponent editor = getEditor();
    if (editor instanceof JSpinner.DefaultEditor) {
      JFormattedTextField textField = ((JSpinner.DefaultEditor)editor).getTextField();
      AbstractFormatter formatter = textField.getFormatter();
      if (formatter instanceof NumberFormatter) {
        ((NumberFormatter)formatter).setFormat(format);
        fireStateChanged();
      }
    }
  }

  // 스피너 값 최대값->최소값으로 리셋
  public static class SpinnerModuloNumberModel extends SpinnerNumberModel {
    public SpinnerModuloNumberModel(int value, int minimum, int maximum, int stepSize) {
      super(value, minimum, maximum, stepSize);
    }
    
    @Override
    public Object getNextValue() {
      if (getNumber().intValue() + getStepSize().intValue() < ((Number)getMaximum()).intValue()) {
        return ((Number)super.getNextValue()).intValue();
      } else {
        return getNumber().intValue() + getStepSize().intValue() - ((Number)getMaximum()).intValue() + ((Number)getMinimum()).intValue();
      }
    }
    
    @Override
    public Object getPreviousValue() {
      if (getNumber().intValue() - getStepSize().intValue() >= ((Number)getMinimum()).intValue()) {
        return ((Number)super.getPreviousValue()).intValue();
      } else {
        return getNumber().intValue() - getStepSize().intValue() - ((Number)getMinimum()).intValue() + ((Number)getMaximum()).intValue();
      }
    }
  }
}
