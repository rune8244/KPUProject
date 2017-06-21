
package com.eteks.homeview3d.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.Stack;

public class XMLWriter extends FilterWriter {
  private Stack<String> elements = new Stack<String>();
  private boolean emptyElement;
  private boolean elementWithText;
  

  public XMLWriter(OutputStream out) throws IOException {
    super(new OutputStreamWriter(out, "UTF-8"));
    this.out.write("<?xml version='1.0'?>\n");
  }
  

  public void writeStartElement(String element) throws IOException {
    if (this.elements.size() > 0) {
      if (this.emptyElement) {
        this.out.write(">");
      }
      writeIndentation();
    }
    this.out.write("<" + element);
    this.elements.push(element);
    this.emptyElement = true;
    this.elementWithText = false;
  }
  

  public void writeEndElement() throws IOException {
    String element = this.elements.pop();
    if (this.emptyElement) {
      this.out.write("/>");
    } else {
      if (!this.elementWithText) {
        writeIndentation();
      }
      this.out.write("</" + element + ">");
    }
    this.emptyElement = false;
    this.elementWithText = false;
  }


  private void writeIndentation() throws IOException {
    this.out.write("\n");
    for (int i = 0; i < this.elements.size(); i++) {
      this.out.write("  ");
    }
  }
  

  public void writeAttribute(String name, String value) throws IOException {
    this.out.write(" " + name + "='" + replaceByEntities(value) + "'");
  }
  

  public void writeAttribute(String name, String value, String defaultValue) throws IOException {
    if ((value != null || value != defaultValue)
        && !value.equals(defaultValue)) {
      writeAttribute(name, value);
    }
  }
  

  public void writeIntegerAttribute(String name, int value) throws IOException {
    writeAttribute(name, String.valueOf(value));
  }
  

  public void writeIntegerAttribute(String name, int value, int defaultValue) throws IOException {
    if (value != defaultValue) {
      writeAttribute(name, String.valueOf(value));
    }
  }
  

  public void writeLongAttribute(String name, long value) throws IOException {
    writeAttribute(name, String.valueOf(value));
  }
  

  public void writeFloatAttribute(String name, float value) throws IOException {
    writeAttribute(name, String.valueOf(value));
  }
  

  public void writeFloatAttribute(String name, float value, float defaultValue) throws IOException {
    if (value != defaultValue) {
      writeFloatAttribute(name, value);
    }
  }

  public void writeFloatAttribute(String name, Float value) throws IOException {
    if (value != null) {
      writeAttribute(name, value.toString());
    }
  }
  

  public void writeBigDecimalAttribute(String name, BigDecimal value) throws IOException {
    if (value != null) {
      writeAttribute(name, String.valueOf(value));
    }
  }

  public void writeBooleanAttribute(String name, boolean value, boolean defaultValue) throws IOException {
    if (value != defaultValue) {
      writeAttribute(name, String.valueOf(value));
    }
  }

  public void writeColorAttribute(String name, Integer color) throws IOException {
    if (color != null) {
      writeAttribute(name, String.format("%08X", color));
    }
  }
  

  public void writeText(String text) throws IOException {
    if (this.emptyElement) {
      this.out.write(">");
      this.emptyElement = false;
      this.elementWithText = true;
    }
    super.out.write(replaceByEntities(text));
  }
  
 
  private static String replaceByEntities(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace("'", "&apos;").replace("\"", "&quot;");
  }
  

  public void write(int c) throws IOException {
    writeText(String.valueOf((char)c));
  }

  public void write(char buffer[], int offset, int length) throws IOException {
    writeText(new String(buffer, offset, length));
  }


  public void write(String str, int offset, int length) throws IOException {
    writeText(str.substring(offset, offset + length));
  }
}