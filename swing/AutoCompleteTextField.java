package com.eteks.homeview3d.swing;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class AutoCompleteTextField extends JTextField {
  private List<String> autoCompletionStrings;
  private boolean directChange; 
  
  public AutoCompleteTextField(String text, int preferredLength, List<String> autoCompletionStrings) {
    super(preferredLength);
    this.autoCompletionStrings = autoCompletionStrings;
    setDocument(new AutoCompleteDocument(text));    
    this.directChange = true;
  }
  
  @Override
  public void setText(String t) {
    this.directChange = false;
    super.setText(t);
    this.directChange = true;
  }
  
  // 문서 자동완성
  private class AutoCompleteDocument extends PlainDocument {
    
    public AutoCompleteDocument(String text) {
      try {
        replace(0, 0, text, null);
      } catch (BadLocationException ex) {
        throw new RuntimeException(ex);
      }
    }
    
    @Override
    public void insertString(int offset, String string, AttributeSet attr) throws BadLocationException {
      if (directChange && (string != null && string.length() > 0)) {
        int length = getLength();
        if (offset == length || (offset == getSelectionStart() && length - 1 == getSelectionEnd())) {
          String textAtOffset = getText(0, offset); 
          String completion = autoComplete(textAtOffset + string);
          if (completion != null) {
            int completionIndex = offset + string.length();
            super.remove(offset, length - offset);
            super.insertString(offset, string, attr);
            super.insertString(completionIndex, completion.substring(completionIndex), attr);
            select(completionIndex, getLength());
            return;
          }
        }
      }
      super.insertString(offset, string, attr);
    }

    private String autoComplete(String stringStart) {
      stringStart = stringStart.toLowerCase();
      // 알파벳 자동완성 제시
      final Collator comparator = Collator.getInstance();
      comparator.setStrength(Collator.TERTIARY);
      TreeSet<String> matchingStrings = new TreeSet<String>(new Comparator<String>() {
          public int compare(String s1, String s2) {
            return comparator.compare(s1, s2);
          }
        });
      // 맞는 문자열 찾기
      for (String s : autoCompletionStrings) {
        if (s.toLowerCase().startsWith(stringStart)) {
          matchingStrings.add(s);
        }
      }
      if (matchingStrings.size() > 0) {
        // Return the first found one
        return matchingStrings.first();
      } else {
        return null;
      }
    }
  }
}
