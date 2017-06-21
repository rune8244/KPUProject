package com.eteks.homeview3d.swing;

import java.text.Format;
import java.text.ParseException;
import java.util.Date;

import javax.swing.JFormattedTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.NumberFormatter;

import com.eteks.homeview3d.model.LengthUnit;
import com.eteks.homeview3d.model.UserPreferences;

public class NullableSpinner extends AutoCommitSpinner {
  public NullableSpinner() {
    this(new NullableSpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
  }

  public NullableSpinner(NullableSpinnerNumberModel model) {
    super(model, 
          model instanceof NullableSpinnerLengthModel 
             ? ((NullableSpinnerLengthModel)model).getLengthUnit().getFormat()  
             : null);
    final JFormattedTextField textField = ((DefaultEditor)getEditor()).getTextField();
    final JFormattedTextField.AbstractFormatter defaultFormatter = textField.getFormatter();
    textField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
        @Override
        public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField textfield) {
          return new NumberFormatter () {
              @Override
              public Format getFormat() {
                if (defaultFormatter instanceof NumberFormatter) {
                  return ((NumberFormatter)defaultFormatter).getFormat();
                } else {
                  return super.getFormat();
                }
              }
              
              @Override
              public boolean getCommitsOnValidEdit() {
                if (defaultFormatter instanceof NumberFormatter) {
                  return ((NumberFormatter)defaultFormatter).getCommitsOnValidEdit();
                } else {
                  return super.getCommitsOnValidEdit();
                }
              }
              
              @SuppressWarnings({"rawtypes"})
              @Override
              public Comparable getMaximum() {
                if (defaultFormatter instanceof NumberFormatter) {
                  return ((NumberFormatter)defaultFormatter).getMaximum();
                } else {
                  return super.getMaximum();
                }
              }
              
              @SuppressWarnings({"rawtypes"})
              @Override
              public Comparable getMinimum() {
                if (defaultFormatter instanceof NumberFormatter) {
                  return ((NumberFormatter)defaultFormatter).getMinimum();
                } else {
                  return super.getMinimum();
                }
              }
              
              @SuppressWarnings({"rawtypes"})
              @Override
              public void setMaximum(Comparable maximum) {
                if (defaultFormatter instanceof NumberFormatter) {
                  ((NumberFormatter)defaultFormatter).setMaximum(maximum);
                } else {
                  super.setMaximum(maximum);
                }
              }
              
              @SuppressWarnings({"rawtypes"})
              @Override
              public void setMinimum(Comparable minimum) {
                if (defaultFormatter instanceof NumberFormatter) {
                  ((NumberFormatter)defaultFormatter).setMinimum(minimum);
                } else {
                  super.setMinimum(minimum);
                }
              }
              
              @Override
              public Object stringToValue(String text) throws ParseException {
                if (text.length() == 0 && ((NullableSpinnerNumberModel)getModel()).isNullable()) {
                  return null;
                } else {
                  return defaultFormatter.stringToValue(text);
                }
              }

              @Override
              public String valueToString(Object value) throws ParseException {
                if (value == null && ((NullableSpinnerNumberModel)getModel()).isNullable()) {
                  return "";
                } else {
                  return defaultFormatter.valueToString(value);
                }
              }
            };
        }
      });
  }
  
  public static class NullableSpinnerDateModel extends SpinnerDateModel {
    private boolean isNull;
    private boolean nullable;

    @Override
    public Object getNextValue() {
      if (this.isNull) {
        return super.getValue();
      } 
      return super.getNextValue();
    }

    @Override
    public Object getPreviousValue() {
      if (this.isNull) {
        return super.getValue();
      } 
      return super.getPreviousValue();
    }

    @Override
    public Object getValue() {
      if (this.isNull) {
        return null;
      } else {
        return super.getValue();
      }
    }

    @Override
    public void setValue(Object value) {
      if (value == null && isNullable()) {
        if (!this.isNull) {
          this.isNull = true;
          fireStateChanged();
        }
      } else {
        if (this.isNull 
            && value != null 
            && value.equals(super.getValue())) {
          this.isNull = false;
          fireStateChanged();
        } else {
          this.isNull = false;
          super.setValue(value);
        }
      }
    }

    public boolean isNullable() {
      return this.nullable;
    }

    public void setNullable(boolean nullable) {
      this.nullable = nullable;
      if (!nullable && getValue() == null) {
        setValue(new Date());
      }
    }
  }
  
  public static class NullableSpinnerNumberModel extends SpinnerNumberModel {
    private boolean isNull;
    private boolean nullable;

    public NullableSpinnerNumberModel(int value, int minimum, int maximum, int stepSize) {
      super(value, minimum, maximum, stepSize);
    }

    public NullableSpinnerNumberModel(float value, float minimum, float maximum, float stepSize) {
      super(new Float(value), new Float(minimum), new Float(maximum), new Float(stepSize));
    }

    @Override
    public Object getNextValue() {
      if (this.isNull) {
        return super.getValue();
      } 
      Object nextValue = super.getNextValue();
      if (nextValue == null) {
        return getMaximum();
      } else {
        return nextValue;
      }
    }

    @Override
    public Object getPreviousValue() {
      if (this.isNull) {
        return super.getValue();
      } 
      Object previousValue = super.getPreviousValue();
      if (previousValue == null) {
        return getMinimum();
      } else {
        return previousValue;
      }
    }

    @Override
    public Object getValue() {
      if (this.isNull) {
        return null;
      } else {
        return super.getValue();
      }
    }

    @Override
    public void setValue(Object value) {
      if (value == null && isNullable()) {
        if (!this.isNull) {
          this.isNull = true;
          fireStateChanged();
        }
      } else {
        if (this.isNull 
            && value != null 
            && value.equals(super.getValue())) {
          this.isNull = false;
          fireStateChanged();
        } else {
          this.isNull = false;
          super.setValue(value);
        }
      }
    }

    @Override
    public Number getNumber() {
      return (Number)getValue();
    }

    public boolean isNullable() {
      return this.nullable;
    }

    public void setNullable(boolean nullable) {
      this.nullable = nullable;
      if (!nullable && getValue() == null) {
        setValue(getMinimum());
      }
    }
  }
  
  public static class NullableSpinnerModuloNumberModel extends NullableSpinnerNumberModel {
    public NullableSpinnerModuloNumberModel(int value, int minimum, int maximum, int stepSize) {
      super(value, minimum, maximum, stepSize);
    }
    
    @Override
    public Object getNextValue() {
      if (getValue() == null
          || getNumber().intValue() + getStepSize().intValue() < ((Number)getMaximum()).intValue()) {
        return ((Number)super.getNextValue()).intValue();
      } else {
        return getNumber().intValue() + getStepSize().intValue() - ((Number)getMaximum()).intValue() + ((Number)getMinimum()).intValue();
      }
    }
    
    @Override
    public Object getPreviousValue() {
      if (getValue() == null
          || getNumber().intValue() - getStepSize().intValue() >= ((Number)getMinimum()).intValue()) {
        return ((Number)super.getPreviousValue()).intValue();
      } else {
        return getNumber().intValue() - getStepSize().intValue() - ((Number)getMinimum()).intValue() + ((Number)getMaximum()).intValue();
      }
    }
  }
 
  public static class NullableSpinnerLengthModel extends NullableSpinnerNumberModel {
    private final UserPreferences preferences;

    
    public NullableSpinnerLengthModel(UserPreferences preferences, float minimum, float maximum) {
      this(preferences, minimum, minimum, maximum);
    }

   
    public NullableSpinnerLengthModel(UserPreferences preferences, float value, float minimum, float maximum) {
      super(value, minimum, maximum, 
            preferences.getLengthUnit() == LengthUnit.INCH
            || preferences.getLengthUnit() == LengthUnit.INCH_DECIMALS
              ? LengthUnit.inchToCentimeter(0.125f) : 0.5f);
      this.preferences = preferences;
    }

    public Float getLength() {
      if (getValue() == null) {
        return null;
      } else {
        return Float.valueOf(((Number)getValue()).floatValue());
      }
    }

    public void setLength(Float length) {
      setValue(length);
    }

    public void setMinimumLength(float minimum) {
      setMinimum(Float.valueOf(minimum));
    }
       
    private LengthUnit getLengthUnit() {
      return this.preferences.getLengthUnit();
    }
  }
}