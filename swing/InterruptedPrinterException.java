package com.eteks.homeview3d.swing;

import java.awt.print.PrinterException;

public class InterruptedPrinterException extends PrinterException {
  //���ͷ�Ʈ
  public InterruptedPrinterException() {
    super();
  }

  // ���ͷ�Ʈ �޽���
  public InterruptedPrinterException(String message) {
    super(message);
  }
}
