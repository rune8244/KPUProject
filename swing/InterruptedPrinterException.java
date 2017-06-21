package com.eteks.homeview3d.swing;

import java.awt.print.PrinterException;

public class InterruptedPrinterException extends PrinterException {
  //인터럽트
  public InterruptedPrinterException() {
    super();
  }

  // 인터럽트 메시지
  public InterruptedPrinterException(String message) {
    super(message);
  }
}
