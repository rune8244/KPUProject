package com.eteks.homeview3d.swing;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

public class HomePDFPrinter {
  private final Home            home;
  private final UserPreferences preferences;
  private final HomeController  controller;
  private final Font            defaultFont;

public HomePDFPrinter(Home home, 
                        UserPreferences preferences, 
                        HomeController controller, 
                        Font defaultFont) {
    this.home = home;
    this.preferences = preferences;
    this.controller = controller;
    this.defaultFont = defaultFont;
  }

 public void write(OutputStream outputStream) throws IOException {
    PageFormat pageFormat = HomePrintableComponent.getPageFormat(this.home.getPrint());
    Document pdfDocument = new Document(new Rectangle((float)pageFormat.getWidth(), (float)pageFormat.getHeight()));
    try {
      PdfWriter pdfWriter = PdfWriter.getInstance(pdfDocument, outputStream);
      pdfDocument.open();
      
      pdfDocument.addAuthor(System.getProperty("user.name", ""));
      String pdfDocumentCreator = this.preferences.getLocalizedString(
          HomePDFPrinter.class, "pdfDocument.creator");    
      pdfDocument.addCreator(pdfDocumentCreator);
      pdfDocument.addCreationDate();
      String homeName = this.home.getName();
      if (homeName != null) {
        pdfDocument.addTitle(this.controller.getContentManager().getPresentationName(
            homeName, ContentManager.ContentType.PDF));
      }
      
      PdfContentByte pdfContent = pdfWriter.getDirectContent();
      HomePrintableComponent printableComponent = 
          new HomePrintableComponent(this.home, this.controller, this.defaultFont);
      for (int page = 0, pageCount = printableComponent.getPageCount(); page < pageCount; page++) {
        if (Thread.interrupted()) {
          throw new InterruptedIOException();
        }
        PdfTemplate pdfTemplate = pdfContent.createTemplate((float)pageFormat.getWidth(), 
            (float)pageFormat.getHeight());
        Graphics g = pdfTemplate.createGraphicsShapes((float)pageFormat.getWidth(), 
            (float)pageFormat.getHeight());        
        
        printableComponent.print(g, pageFormat, page);
        
        pdfContent.addTemplate(pdfTemplate, 0, 0);
        g.dispose();
        
        if (page != pageCount - 1) {
          pdfDocument.newPage();
        }
      }
      pdfDocument.close();
    } catch (DocumentException ex) {
      IOException exception = new IOException("Couldn't print to PDF");
      exception.initCause(ex);
      throw exception;
    } catch (InterruptedPrinterException ex) {
      throw new InterruptedIOException("Print to PDF interrupted");
    } catch (PrinterException ex) {
      IOException exception = new IOException("Couldn't print to PDF");
      exception.initCause(ex);
      throw exception;
    }
  }
}
