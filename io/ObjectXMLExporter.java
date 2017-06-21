package com.eteks.homeview3d.io;

import java.io.IOException;

import com.eteks.homeview3d.io.XMLWriter;

public abstract class ObjectXMLExporter<T> {

  public void writeElement(XMLWriter writer, T object) throws IOException {
    writer.writeStartElement(getTag(object));
    writeAttributes(writer, object);
    writeChildren(writer, object);
    writer.writeEndElement();
  }


  protected String getTag(T object) {
    String tagName = object.getClass().getSimpleName();
    if (tagName.startsWith("Home") && !tagName.equals("Home")) {
      tagName = tagName.substring(4);
    }
    return Character.toLowerCase(tagName.charAt(0)) + tagName.substring(1);
  }


  protected void writeAttributes(XMLWriter writer, T object) throws IOException {
  }

  protected void writeChildren(XMLWriter writer, T object) throws IOException {
  }
}