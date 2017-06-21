package com.eteks.homeview3d.model;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.ResourceBundle;

public enum LengthUnit {

  MILLIMETER {
    private Locale        formatLocale;  
    private String        name;
    private DecimalFormat lengthFormatWithUnit;
    private DecimalFormat lengthFormat;
    private DecimalFormat areaFormatWithUnit;

    public Format getFormatWithUnit() {
      checkLocaleChange();
      return this.lengthFormatWithUnit;
    }

    public Format getAreaFormatWithUnit() {
      checkLocaleChange();
      return this.areaFormatWithUnit;
    }

    public Format getFormat() {
      checkLocaleChange();
      return this.lengthFormat;
    }

    public String getName() {
      checkLocaleChange();
      return this.name;
    }
    
    private void checkLocaleChange() {
      if (!Locale.getDefault().equals(this.formatLocale)) {
        this.formatLocale = Locale.getDefault();  
        ResourceBundle resource = ResourceBundle.getBundle(LengthUnit.class.getName());
        this.name = resource.getString("millimeterUnit");
        this.lengthFormatWithUnit = new MeterFamilyFormat("#,##0 " + this.name, 10);          
        this.lengthFormat = new MeterFamilyFormat("#,##0", 10);
        String squareMeterUnit = resource.getString("squareMeterUnit");
        this.areaFormatWithUnit = new SquareMeterAreaFormatWithUnit(squareMeterUnit);
      }
    }

    public float getMagnetizedLength(float length, float maxDelta) {
      return getMagnetizedMeterLength(length, maxDelta);
    }

    public float getMinimumLength() {
      return 0.1f;
    }

    public float getMaximumLength() {
      return 100000f;
    }

    public float centimeterToUnit(float length) {
      return length * 10;
    }

    public float unitToCentimeter(float length) {
      return length / 10;
    }
  },
  
  CENTIMETER {
    private Locale        formatLocale;  
    private String        name;
    private DecimalFormat lengthFormatWithUnit;
    private DecimalFormat lengthFormat;
    private DecimalFormat areaFormatWithUnit;

    public Format getFormatWithUnit() {
      checkLocaleChange();
      return this.lengthFormatWithUnit;
    }

    public Format getAreaFormatWithUnit() {
      checkLocaleChange();
      return this.areaFormatWithUnit;
    }

    public Format getFormat() {
      checkLocaleChange();
      return this.lengthFormat;
    }

    public String getName() {
      checkLocaleChange();
      return this.name;
    }
    
    private void checkLocaleChange() {
      if (!Locale.getDefault().equals(this.formatLocale)) {
        this.formatLocale = Locale.getDefault();  
        ResourceBundle resource = ResourceBundle.getBundle(LengthUnit.class.getName());
        this.name = resource.getString("centimeterUnit");
        this.lengthFormatWithUnit = new MeterFamilyFormat("#,##0.# " + this.name, 1);          
        this.lengthFormat = new MeterFamilyFormat("#,##0.#", 1);
        String squareMeterUnit = resource.getString("squareMeterUnit");
        this.areaFormatWithUnit = new SquareMeterAreaFormatWithUnit(squareMeterUnit);
      }
    }

    public float getMagnetizedLength(float length, float maxDelta) {
      return getMagnetizedMeterLength(length, maxDelta);
    }

    public float getMinimumLength() {
      return 0.1f;
    }

    public float getMaximumLength() {
      return 100000f;
    }

    public float centimeterToUnit(float length) {
      return length;
    }

    public float unitToCentimeter(float length) {
      return length;
    }
  }, 

  METER {
    private Locale        formatLocale;  
    private String        name;
    private DecimalFormat lengthFormatWithUnit;
    private DecimalFormat lengthFormat;
    private DecimalFormat areaFormatWithUnit;

    public Format getFormatWithUnit() {
      checkLocaleChange();
      return this.lengthFormatWithUnit;
    }

    public Format getAreaFormatWithUnit() {
      checkLocaleChange();
      return this.areaFormatWithUnit;
    }

    public Format getFormat() {
      checkLocaleChange();
      return this.lengthFormat;
    }

    public String getName() {
      checkLocaleChange();
      return this.name;
    }
    
    private void checkLocaleChange() {
      if (!Locale.getDefault().equals(this.formatLocale)) {
        this.formatLocale = Locale.getDefault();  
        ResourceBundle resource = ResourceBundle.getBundle(LengthUnit.class.getName());
        this.name = resource.getString("meterUnit");
        this.lengthFormatWithUnit = new MeterFamilyFormat("#,##0.00# " + this.name, 0.01f);          
        this.lengthFormat = new MeterFamilyFormat("#,##0.00#", 0.01f);
        String squareMeterUnit = resource.getString("squareMeterUnit");
        this.areaFormatWithUnit = new SquareMeterAreaFormatWithUnit(squareMeterUnit);
      }
    }

    public float getMagnetizedLength(float length, float maxDelta) {
      return getMagnetizedMeterLength(length, maxDelta);
    }

    public float getMinimumLength() {
      return 0.1f;
    }

    public float getMaximumLength() {
      return 100000f;
    }

    public float centimeterToUnit(float length) {
      return length / 100;
    }

    public float unitToCentimeter(float length) {
      return length * 100;
    }
  }, 

  INCH {
    private Locale        formatLocale;
    private String        name;
    private DecimalFormat lengthFormat;
    private DecimalFormat areaFormatWithUnit;

    public Format getFormatWithUnit() {
      checkLocaleChange();
      return this.lengthFormat;
    }

    public Format getFormat() {
      return getFormatWithUnit();
    }

    public Format getAreaFormatWithUnit() {
      checkLocaleChange();
      return this.areaFormatWithUnit;
    }

    public String getName() {
      checkLocaleChange();
      return this.name;
    }
    
    private void checkLocaleChange() {
      if (!Locale.getDefault().equals(this.formatLocale)) {
        this.formatLocale = Locale.getDefault();  
        ResourceBundle resource = ResourceBundle.getBundle(LengthUnit.class.getName());
        this.name = resource.getString("inchUnit");
        
        final MessageFormat positiveFootFormat = new MessageFormat(resource.getString("footFormat"));
        final MessageFormat positiveFootInchFormat = new MessageFormat(resource.getString("footInchFormat"));
        final MessageFormat positiveFootInchEighthFormat = new MessageFormat(resource.getString("footInchEighthFormat"));
        final MessageFormat negativeFootFormat = new MessageFormat("-" + resource.getString("footFormat"));
        final MessageFormat negativeFootInchFormat = new MessageFormat("-" + resource.getString("footInchFormat"));
        final MessageFormat negativeFootInchEighthFormat = new MessageFormat("-" + resource.getString("footInchEighthFormat"));
        final String        footInchSeparator = resource.getString("footInchSeparator");
        final NumberFormat  footNumberFormat = NumberFormat.getIntegerInstance();
        final NumberFormat  inchNumberFormat = NumberFormat.getNumberInstance();
        final char [] inchFractionCharacters = {'\u215b',   // 1/8
                                                '\u00bc',   // 1/4  
                                                '\u215c',   // 3/8
                                                '\u00bd',   // 1/2
                                                '\u215d',   // 5/8
                                                '\u00be',   // 3/4
                                                '\u215e'};  // 7/8        
        final String [] inchFractionStrings  = {"1/8",
                                                "1/4",  
                                                "3/8",
                                                "1/2",
                                                "5/8",
                                                "3/4",
                                                "7/8"};         
        this.lengthFormat = new DecimalFormat("0.000\"") {

            public StringBuffer format(double number, StringBuffer result,
                                       FieldPosition fieldPosition) {
              float absoluteValue = Math.abs((float)number);
              double feet = Math.floor(centimeterToFoot(absoluteValue));              
              float remainingInches = centimeterToInch((float)absoluteValue - footToCentimeter((float)feet));
              if (remainingInches >= 11.9375f) {
                feet++;
                remainingInches -= 12;
              }
              fieldPosition.setEndIndex(fieldPosition.getEndIndex() + 1);
              if (remainingInches >= 0.0005f) {
                int integerPart = (int)Math.floor(remainingInches);
                float fractionPart = remainingInches - integerPart;
                int eighth = Math.round(fractionPart * 8); 
                if (eighth == 0 || eighth == 8) {
                  (number >= 0 ? positiveFootInchFormat : negativeFootInchFormat).format(
                      new Object [] {feet, Math.round(remainingInches * 8) / 8f}, result, fieldPosition);
                } else { 
                  (number >= 0 ? positiveFootInchEighthFormat : negativeFootInchEighthFormat).format(
                      new Object [] {feet, integerPart, inchFractionCharacters [eighth - 1]}, result, fieldPosition);
                }
              } else {
                (number >= 0 ? positiveFootFormat : negativeFootFormat).format(
                    new Object [] {feet}, result, fieldPosition);
              }
              return result;
            }

            public Number parse(String text, ParsePosition parsePosition) {
              double value = 0;
              ParsePosition numberPosition = new ParsePosition(parsePosition.getIndex());
              skipWhiteSpaces(text, numberPosition);
              int quoteIndex = text.indexOf('\'', parsePosition.getIndex());
              boolean negative = numberPosition.getIndex() < text.length()  
                  && text.charAt(numberPosition.getIndex()) == this.getDecimalFormatSymbols().getMinusSign();
              if (quoteIndex != -1) {
                Number feet = footNumberFormat.parse(text, numberPosition);
                if (feet == null) {
                  parsePosition.setErrorIndex(numberPosition.getErrorIndex());
                  return null;
                }
                skipWhiteSpaces(text, numberPosition);
                if (numberPosition.getIndex() != quoteIndex) {
                  parsePosition.setErrorIndex(numberPosition.getIndex());
                  return null;
                }
                value = footToCentimeter(feet.intValue());                
                numberPosition = new ParsePosition(quoteIndex + 1);
                skipWhiteSpaces(text, numberPosition);
                if (numberPosition.getIndex() < text.length()
                    && footInchSeparator.indexOf(text.charAt(numberPosition.getIndex())) >= 0) {
                  numberPosition.setIndex(numberPosition.getIndex() + 1);
                  skipWhiteSpaces(text, numberPosition);
                }
                if (numberPosition.getIndex() == text.length()) {
                  parsePosition.setIndex(text.length());
                  return value;
                }
              } 
              Number inches = inchNumberFormat.parse(text, numberPosition);
              if (inches == null) {
                parsePosition.setErrorIndex(numberPosition.getErrorIndex());
                return null;
              }
              if (negative) {
                if (quoteIndex == -1) {
                  value = inchToCentimeter(inches.floatValue());
                } else {
                  value -= inchToCentimeter(inches.floatValue());
                }
              } else {
                value += inchToCentimeter(inches.floatValue());
              }
              skipWhiteSpaces(text, numberPosition);
              if (numberPosition.getIndex() == text.length()) {
                parsePosition.setIndex(text.length());
                return value;
              }
              if (text.charAt(numberPosition.getIndex()) == '\"') {
                parsePosition.setIndex(numberPosition.getIndex() + 1);
                return value;
              }

              char fractionChar = text.charAt(numberPosition.getIndex());    
              String fractionString = text.length() - numberPosition.getIndex() >= 3 
                  ? text.substring(numberPosition.getIndex(), numberPosition.getIndex() + 3)
                  : null;
              for (int i = 0; i < inchFractionCharacters.length; i++) {
                if (inchFractionCharacters [i] == fractionChar
                    || inchFractionStrings [i].equals(fractionString)) {
                  int lastDecimalSeparatorIndex = text.lastIndexOf(getDecimalFormatSymbols().getDecimalSeparator(), 
                      numberPosition.getIndex() - 1);
                  if (lastDecimalSeparatorIndex > quoteIndex) {
                    return null;
                  } else {
                    if (negative) {
                      value -= inchToCentimeter((i + 1) / 8f);
                    } else {
                      value += inchToCentimeter((i + 1) / 8f);
                    }
                    parsePosition.setIndex(numberPosition.getIndex() 
                        + (inchFractionCharacters [i] == fractionChar ? 1 : 3));
                    skipWhiteSpaces(text, parsePosition);
                    if (parsePosition.getIndex() < text.length() 
                        && text.charAt(parsePosition.getIndex()) == '\"') {
                      parsePosition.setIndex(parsePosition.getIndex() + 1);
                    }
                    return value;
                  }
                }
              }
              
              parsePosition.setIndex(numberPosition.getIndex());
              return value;
            }

            private void skipWhiteSpaces(String text, ParsePosition fieldPosition) {
              while (fieldPosition.getIndex() < text.length()
                  && Character.isWhitespace(text.charAt(fieldPosition.getIndex()))) {
                fieldPosition.setIndex(fieldPosition.getIndex() + 1);
              }
            }
          };
        
        String squareFootUnit = resource.getString("squareFootUnit");
        this.areaFormatWithUnit = new SquareFootAreaFormatWithUnit("#,##0 " + squareFootUnit);
      }
    }

    public float getMagnetizedLength(float length, float maxDelta) {
      return getMagnetizedInchLength(length, maxDelta);
    }

    public float getMinimumLength() {        
      return LengthUnit.inchToCentimeter(0.125f);
    }

    public float getMaximumLength() {
      return LengthUnit.inchToCentimeter(99974.4f); 
    }

    public float centimeterToUnit(float length) {
      return centimeterToInch(length);
    }

    public float unitToCentimeter(float length) {
      return inchToCentimeter(length);
    }
  },

  INCH_DECIMALS {
    private Locale        formatLocale;
    private String        name;
    private DecimalFormat lengthFormat;
    private DecimalFormat lengthFormatWithUnit;
    private DecimalFormat areaFormatWithUnit;

    public Format getFormatWithUnit() {
      checkLocaleChange();
      return this.lengthFormatWithUnit;
    }

    public Format getFormat() {
      checkLocaleChange();
      return this.lengthFormat;
    }

    public Format getAreaFormatWithUnit() {
      checkLocaleChange();
      return this.areaFormatWithUnit;
    }

    public String getName() {
      checkLocaleChange();
      return this.name;
    }
    
    private void checkLocaleChange() {
      if (!Locale.getDefault().equals(this.formatLocale)) {
        this.formatLocale = Locale.getDefault();  
        ResourceBundle resource = ResourceBundle.getBundle(LengthUnit.class.getName());
        this.name = resource.getString("inchUnit");
        
        class InchDecimalsFormat extends DecimalFormat {
          private final MessageFormat inchDecimalsFormat;
          private final NumberFormat  inchNumberFormat = NumberFormat.getNumberInstance();

          private InchDecimalsFormat(MessageFormat inchDecimalsFormat) {
            super("0.###");
            this.inchDecimalsFormat = inchDecimalsFormat;
          }

          public StringBuffer format(double number, StringBuffer result,
                                     FieldPosition fieldPosition) {
            float inches = centimeterToInch((float)number);
            fieldPosition.setEndIndex(fieldPosition.getEndIndex() + 1);
            this.inchDecimalsFormat.format(new Object [] {inches}, result, fieldPosition);
            return result;
          }

          public Number parse(String text, ParsePosition parsePosition) {
            ParsePosition numberPosition = new ParsePosition(parsePosition.getIndex());
            skipWhiteSpaces(text, numberPosition);
            Number inches = this.inchNumberFormat.parse(text, numberPosition);
            if (inches == null) {
              parsePosition.setErrorIndex(numberPosition.getErrorIndex());
              return null;
            }
            double value = inchToCentimeter(inches.floatValue());
            skipWhiteSpaces(text, numberPosition);
            if (numberPosition.getIndex() < text.length() 
                && text.charAt(numberPosition.getIndex()) == '\"') {
              parsePosition.setIndex(numberPosition.getIndex() + 1);
            } else {
              parsePosition.setIndex(numberPosition.getIndex());
            }
            return value;
          }

          private void skipWhiteSpaces(String text, ParsePosition fieldPosition) {
            while (fieldPosition.getIndex() < text.length()
                && Character.isWhitespace(text.charAt(fieldPosition.getIndex()))) {
              fieldPosition.setIndex(fieldPosition.getIndex() + 1);
            }
          }
        }
        this.lengthFormat = new InchDecimalsFormat(new MessageFormat(resource.getString("inchDecimalsFormat")));
        this.lengthFormatWithUnit = new InchDecimalsFormat(new MessageFormat(resource.getString("inchDecimalsFormatWithUnit")));
        
        String squareFootUnit = resource.getString("squareFootUnit");
        this.areaFormatWithUnit = new SquareFootAreaFormatWithUnit("#,##0.## " + squareFootUnit);
      }
    }

    public float getMagnetizedLength(float length, float maxDelta) {
      return getMagnetizedInchLength(length, maxDelta);
    }

    public float getMinimumLength() {        
      return LengthUnit.inchToCentimeter(0.125f);
    }

    public float getMaximumLength() {
      return LengthUnit.inchToCentimeter(99974.4f); // 3280 ft
    }

    public float centimeterToUnit(float length) {
      return centimeterToInch(length);
    }

    public float unitToCentimeter(float length) {
      return inchToCentimeter(length);
    }
  };

  public static float centimeterToInch(float length) {
    return length / 2.54f;
  }

  public static float centimeterToFoot(float length) {
    return length / 2.54f / 12;
  }

  public static float inchToCentimeter(float length) {
    return length * 2.54f;
  }

  public static float footToCentimeter(float length) {
    return length * 2.54f * 12;
  }

  public abstract Format getFormatWithUnit(); 

  public abstract Format getFormat(); 

  private static class MeterFamilyFormat extends DecimalFormat {
    private final float unitMultiplier;

    public MeterFamilyFormat(String pattern, float unitMultiplier) {
      super(pattern);
      this.unitMultiplier = unitMultiplier;
      
    }

    public StringBuffer format(double number, StringBuffer result,
                               FieldPosition fieldPosition) {
      return super.format(number * this.unitMultiplier, result, fieldPosition);                
    }

    public StringBuffer format(long number, StringBuffer result,
                               FieldPosition fieldPosition) {
      return format((double)number, result, fieldPosition);
    }

    public Number parse(String text, ParsePosition pos) {
      Number number = super.parse(text, pos);
      if (number == null) {
        return null;
      } else {
        return number.floatValue() / this.unitMultiplier;
      }
    }
  }

  public abstract Format getAreaFormatWithUnit();

  private static class SquareMeterAreaFormatWithUnit extends DecimalFormat {
    public SquareMeterAreaFormatWithUnit(String squareMeterUnit) {
      super("#,##0.## " + squareMeterUnit);
    }

    public StringBuffer format(double number, StringBuffer result,
                               FieldPosition fieldPosition) {
      return super.format(number / 10000, result, fieldPosition);                
    }
  }

  private static class SquareFootAreaFormatWithUnit extends DecimalFormat {
    public SquareFootAreaFormatWithUnit(String pattern) {
      super(pattern);
    }

    public StringBuffer format(double number, StringBuffer result,
                               FieldPosition fieldPosition) {
      return super.format(number / 929.0304, result, fieldPosition);                
    }
  }

  public abstract String getName();
  
  public abstract float getMagnetizedLength(float length, float maxDelta);

  private static float getMagnetizedMeterLength(float length, float maxDelta) {
    maxDelta *= 2;
    float precision = 1 / 10f;
    if (maxDelta > 100) {
      precision = 100;
    } else if (maxDelta > 10) {
      precision = 10;
    } else if (maxDelta > 5) {
      precision = 5;
    } else if (maxDelta > 1) {
      precision = 1;
    } else if  (maxDelta > 0.5f) {
      precision = 0.5f;
    } 
    float magnetizedLength = Math.round(length / precision) * precision;
    if (magnetizedLength == 0 && length > 0) {
      return length;
    } else {
      return magnetizedLength;
    }
  }

  private static float getMagnetizedInchLength(float length, float maxDelta) {
    maxDelta = centimeterToInch(maxDelta) * 2;
    float precision = 1 / 8f;
    if (maxDelta > 6) {
      precision = 6;
    } else if (maxDelta > 3) {
      precision = 3;
    } else if (maxDelta > 1) {
      precision = 1;
    } else if  (maxDelta > 0.5f) {
      precision = 0.5f;
    } else if  (maxDelta > 0.25f) {
      precision = 0.25f;
    }
    float magnetizedLength = inchToCentimeter(Math.round(centimeterToInch(length) / precision) * precision);
    if (magnetizedLength == 0 && length > 0) {
      return length;
    } else {
      return magnetizedLength;
    }
  }

  public abstract float getMinimumLength();

  public abstract float getMaximumLength();

  public float getMaximumElevation() {
    return getMaximumLength() / 10;
  }

  public abstract float centimeterToUnit(float length);

  public abstract float unitToCentimeter(float length);
}