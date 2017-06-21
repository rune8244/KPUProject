package com.eteks.homeview3d.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public interface Content extends Serializable {
  InputStream openStream() throws IOException;
}
