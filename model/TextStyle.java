package com.eteks.homeview3d.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TextStyle implements Serializable {
  private static final long serialVersionUID = 1L;
    
  private final String  fontName;
  private final float   fontSize;
  private final boolean bold;
  private final boolean italic;
  
  private static final List<WeakReference<TextStyle>> textStylesCache = new ArrayList<WeakReference<TextStyle>>(); 
  
  public TextStyle(float fontSize) {
    this(fontSize, false, false);    
  }

  public TextStyle(float fontSize, boolean bold, boolean italic) {
    this(null, fontSize, bold, italic);
  }

  public TextStyle(String fontName, float fontSize, boolean bold, boolean italic) {
    this(fontName, fontSize, bold, italic, true);
  }
  
  private TextStyle(String fontName, float fontSize, boolean bold, boolean italic, boolean cached) {
    this.fontName = fontName;
    this.fontSize = fontSize;
    this.bold = bold;
    this.italic = italic;
    
    if (cached) {
      textStylesCache.add(new WeakReference<TextStyle>(this));
    }
  }
  
  /**
   * 스타일 읽고 캐시 업데이트.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    
    textStylesCache.add(new WeakReference<TextStyle>(this));
  }

  private TextStyle getInstance(String fontName, float fontSize, boolean bold, boolean italic) {
    TextStyle textStyle = new TextStyle(fontName, fontSize, bold, italic, false);
    for (int i = textStylesCache.size() - 1; i >= 0; i--) {
      TextStyle cachedTextStyle = textStylesCache.get(i).get();
      if (cachedTextStyle == null) {
        textStylesCache.remove(i);
      } else if (cachedTextStyle.equals(textStyle)) {
        return textStyle;
      }
    }
    textStylesCache.add(new WeakReference<TextStyle>(textStyle));
    return textStyle;
  }

  public String getFontName() {
    return this.fontName;
  }

  public float getFontSize() {
    return this.fontSize;
  }

  public boolean isBold() {
    return this.bold;
  }

  public boolean isItalic() {
    return this.italic;
  }

  public TextStyle deriveStyle(String fontName) {
    if (getFontName() == fontName
        || (fontName != null && fontName.equals(getFontName()))) {
      return this;
    } else {
      return getInstance(fontName, getFontSize(), isBold(), isItalic());
    }
  }

  public TextStyle deriveStyle(float fontSize) {
    if (getFontSize() == fontSize) {
      return this;
    } else {
      return getInstance(getFontName(), fontSize, isBold(), isItalic());
    }
  }

  public TextStyle deriveBoldStyle(boolean bold) {
    if (isBold() == bold) {
      return this;
    } else {
      return getInstance(getFontName(), getFontSize(), bold, isItalic());
    }
  }

  public TextStyle deriveItalicStyle(boolean italic) {
    if (isItalic() == italic) {
      return this;
    } else {
      return getInstance(getFontName(), getFontSize(), isBold(), italic);
    }
  }

  public boolean equals(Object object) {
    if (object instanceof TextStyle) {
      TextStyle textStyle = (TextStyle)object;
      return (textStyle.fontName == this.fontName
              || (textStyle.fontName != null && textStyle.fontName.equals(this.fontName)))
          && textStyle.fontSize == this.fontSize
          && textStyle.bold == this.bold
          && textStyle.italic == this.italic;
    }
    return false;
  }

  public int hashCode() {
    int hashCode = Float.floatToIntBits(this.fontSize);
    if (this.fontName != null) {
      hashCode += this.fontName.hashCode();
    }
    if (this.bold) {
      hashCode++;
    }
    if (this.italic) {
      hashCode++;
    }
    return hashCode;
  }
}
