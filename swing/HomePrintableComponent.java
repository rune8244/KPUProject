package com.eteks.homeview3d.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.HomePrint;
import com.eteks.homeview3d.model.LengthUnit;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.viewcontroller.ContentManager;
import com.eteks.homeview3d.viewcontroller.HomeController;
import com.eteks.homeview3d.viewcontroller.PlanView;
import com.eteks.homeview3d.viewcontroller.View;

// ��ġ�� 3D�� ��ȯ �� �̸����� �� ������
public class HomePrintableComponent extends JComponent implements Printable {
  public enum Variable {
    PAGE_NUMBER("$pageNumber", "{0, number, integer}"),
    PAGE_COUNT("$pageCount", "{1, number, integer}"),
    PLAN_SCALE("$planScale", "{2}"),
    DATE("$date", "{3, date}"),
    TIME("$time", "{3, time}"),
    HOME_PRESENTATION_NAME("$name", "{4}"),
    HOME_NAME("$file", "{5}"),
    LEVEL_NAME("$level", "{6}");    
    
    private final String userCode;
    private final String formatCode;

    private Variable(String userCode, String formatCode) {
      this.userCode = userCode;
      this.formatCode = formatCode;      
    }    
    
    public String getUserCode() {
      return this.userCode;
    }
    
    public String getFormatCode()  {
      return this.formatCode;
    }
    
    public static MessageFormat getMessageFormat(String format) {
      final String temp = "|#&%<>/!";
      format = format.replace("$$", temp);
      format = format.replace("'", "''");
      format = format.replace("{", "'{'");
      for (Variable variable : Variable.values()) {
        format = format.replace(variable.getUserCode(), variable.getFormatCode());
      }
      format = format.replace(temp, "$");
      return new MessageFormat(format);
    }
  };

  private static final float   HEADER_FOOTER_MARGIN = LengthUnit.centimeterToInch(0.2f) * 72;
  
  private final Home           home;
  private final HomeController controller;
  private final Font           defaultFont;
  private final Font           headerFooterFont;
  private int                  page;
  private int                  pageCount = -1;
  private Set<Integer>         printablePages = new HashSet<Integer>();
  private int                  furniturePageCount;
  private int                  planPageCount;
  private Date                 printDate;
  private JLabel               fixedHeaderLabel;
  private JLabel               fixedFooterLabel;
  private JLabel               fixedFirstPageHeaderLabel;
  private JLabel               fixedFirstPageFooterLabel;
  
  // ����Ʈ���� ��� ����
  public HomePrintableComponent(Home home, HomeController controller, Font defaultFont) {
    this.home = home;
    this.controller = controller;
    this.defaultFont = defaultFont;
    this.headerFooterFont = defaultFont.deriveFont(11f);
    
    try {
      ResourceBundle resource = ResourceBundle.getBundle(HomePrintableComponent.class.getName());
      this.fixedHeaderLabel = getFixedHeaderOrFooterLabel(resource, "fixedHeader");
      this.fixedFooterLabel = getFixedHeaderOrFooterLabel(resource, "fixedFooter");
      this.fixedFirstPageHeaderLabel = getFixedHeaderOrFooterLabel(resource, "fixedFirstPageHeader");
      if (this.fixedFirstPageHeaderLabel == null) {
        this.fixedFirstPageHeaderLabel = this.fixedHeaderLabel;
      }
      this.fixedFirstPageFooterLabel = getFixedHeaderOrFooterLabel(resource, "fixedFirstPageFooter");
      if (this.fixedFirstPageFooterLabel == null) {
        this.fixedFirstPageFooterLabel = this.fixedFooterLabel;
      }
    } catch (MissingResourceException ex) {
    }
  }

  private JLabel getFixedHeaderOrFooterLabel(ResourceBundle resource, String resourceKey) {
    try {     
      String classFile = "/" + HomePrintableComponent.class.getName().replace('.', '/') + ".properties";
      String urlBase = HomePrintableComponent.class.getResource(classFile).toString();
      urlBase = urlBase.substring(0, urlBase.length() - classFile.length());      
      
      String fixedHeaderOrFooter = String.format(resource.getString(resourceKey), urlBase);      
      JLabel fixedHeaderOrFooterLabel = new JLabel(fixedHeaderOrFooter, JLabel.CENTER);
      fixedHeaderOrFooterLabel.setFont(this.headerFooterFont);
      fixedHeaderOrFooterLabel.setSize(fixedHeaderOrFooterLabel.getPreferredSize());
      return fixedHeaderOrFooterLabel;
    } catch (MissingResourceException ex) {
      return null;
    }
  }
  
  public int print(Graphics g, PageFormat pageFormat, int page) throws PrinterException {
    if (Thread.interrupted()) {
      throw new InterruptedPrinterException();
    }
    
    Graphics2D g2D = (Graphics2D)g;
    g2D.setFont(this.defaultFont);
    g2D.setColor(Color.WHITE);
    g2D.fill(new Rectangle2D.Double(0, 0, pageFormat.getWidth(), pageFormat.getHeight()));

    Paper oldPaper = pageFormat.getPaper();
    try {
      String fixedPrintMargin = System.getProperty("com.eteks.homeview3d.swing.fixedPrintMargin", null);
      if (fixedPrintMargin != null) {
        float margin = LengthUnit.centimeterToInch(Float.parseFloat(fixedPrintMargin)) * 72;
        Paper noMarginPaper = pageFormat.getPaper();
        noMarginPaper.setImageableArea(margin, margin, oldPaper.getWidth() - 2 * margin, oldPaper.getHeight() - 2 * margin);
        pageFormat.setPaper(noMarginPaper);
      }
    } catch (NumberFormatException ex) {
      ex.printStackTrace();
    } catch (AccessControlException ex) {
    }
    int pageExists = NO_SUCH_PAGE;
    HomePrint homePrint = this.home.getPrint();
    
    // ��� ����
    float imageableY = (float)pageFormat.getImageableY();
    float imageableHeight = (float)pageFormat.getImageableHeight();
    String header = null;
    float  xHeader = 0;
    float  yHeader = 0;
    float  xFixedHeader = 0;
    float  yFixedHeader = 0;
    String footer = null;
    float  xFooter = 0;
    float  yFooter = 0;
    float  xFixedFooter = 0;
    float  yFixedFooter = 0;
    
    JLabel fixedHeaderPageLabel = page == 0
        ? this.fixedFirstPageHeaderLabel : this.fixedHeaderLabel;
    JLabel fixedFooterPageLabel = page == 0
        ? this.fixedFirstPageFooterLabel : this.fixedFooterLabel;
    if (fixedHeaderPageLabel != null) {
      fixedHeaderPageLabel.setSize((int)pageFormat.getImageableWidth(), fixedHeaderPageLabel.getPreferredSize().height);
      imageableHeight -= fixedHeaderPageLabel.getHeight() + HEADER_FOOTER_MARGIN;
      imageableY += fixedHeaderPageLabel.getHeight() + HEADER_FOOTER_MARGIN;
      xFixedHeader = (float)pageFormat.getImageableX();
      yFixedHeader = (float)pageFormat.getImageableY();
    }
    
    if (fixedFooterPageLabel != null) {
      fixedFooterPageLabel.setSize((int)pageFormat.getImageableWidth(), fixedFooterPageLabel.getPreferredSize().height);
      imageableHeight -= fixedFooterPageLabel.getHeight() + HEADER_FOOTER_MARGIN;
      xFixedFooter = (float)pageFormat.getImageableX();
      yFixedFooter = (float)(pageFormat.getImageableY() + pageFormat.getImageableHeight()) - fixedFooterPageLabel.getHeight();
    }
    
    Rectangle clipBounds = g2D.getClipBounds();
    AffineTransform oldTransform = g2D.getTransform();
    final PlanView planView = this.controller.getPlanController().getView();
    if (homePrint != null
        || fixedHeaderPageLabel != null
        || fixedFooterPageLabel != null) {
      if (homePrint != null) {
        FontMetrics fontMetrics = g2D.getFontMetrics(this.headerFooterFont);
        float headerFooterHeight = fontMetrics.getAscent() + fontMetrics.getDescent() + HEADER_FOOTER_MARGIN;
        
        int pageNumber = page + 1; 
        int pageCount = getPageCount(); 
        String planScale = "?";
        if (homePrint.getPlanScale() != null) {
          planScale = "1/" + Math.round(1 / homePrint.getPlanScale());
        } else {
          Float preferredScale = null;
          if (planView instanceof PlanComponent) {
            preferredScale = ((PlanComponent)planView).getPrintPreferredScale(g, pageFormat);
          } else if (planView instanceof MultipleLevelsPlanPanel) {
            preferredScale = ((MultipleLevelsPlanPanel)planView).getPrintPreferredScale(g, pageFormat);
          }     
          if (preferredScale != null) {
            planScale = "1/" + Math.round(1 / preferredScale);
          }
        }          
        if (page == 0) {
          this.printDate = new Date();
        }
        String homeName = this.home.getName();
        if (homeName == null) {
          homeName = "";
        }
        String levelName = "";
        if (this.home.getSelectedLevel() != null) {
          levelName = this.home.getSelectedLevel().getName();
        }
        String homePresentationName = this.controller.getContentManager().getPresentationName(
             homeName, ContentManager.ContentType.SWEET_HOME_3D);
        Object [] variableValues = new Object [] {
            pageNumber, pageCount, planScale, this.printDate, homePresentationName, homeName, levelName};
        
        // ��� �ؽ�Ʈ ����
        String headerFormat = homePrint.getHeaderFormat();      
        if (headerFormat != null) {
          header = Variable.getMessageFormat(headerFormat).format(variableValues).trim();
          if (header.length() > 0) {
            xHeader = ((float)pageFormat.getWidth() - fontMetrics.stringWidth(header)) / 2;
            yHeader = imageableY + fontMetrics.getAscent();
            imageableY += headerFooterHeight;
            imageableHeight -= headerFooterHeight;
          } else {
            header = null;
          }
        }
        
        String footerFormat = homePrint.getFooterFormat();
        if (footerFormat != null) {
          footer = Variable.getMessageFormat(footerFormat).format(variableValues).trim();
          if (footer.length() > 0) {
            xFooter = ((float)pageFormat.getWidth() - fontMetrics.stringWidth(footer)) / 2;
            yFooter = imageableY + imageableHeight - fontMetrics.getDescent();
            imageableHeight -= headerFooterHeight;
          } else {
            footer = null;
          }
        }
      }
      
      // ������Ʈ
      Paper paper = pageFormat.getPaper();
      switch (pageFormat.getOrientation()) {
        case PageFormat.PORTRAIT:
          paper.setImageableArea(paper.getImageableX(), imageableY, 
              paper.getImageableWidth(), imageableHeight);
          break;
        case PageFormat.LANDSCAPE :
          paper.setImageableArea(paper.getWidth() - (imageableHeight + imageableY), 
              paper.getImageableY(), 
              imageableHeight, paper.getImageableHeight());
        case PageFormat.REVERSE_LANDSCAPE:
          paper.setImageableArea(imageableY, paper.getImageableY(), 
              imageableHeight, paper.getImageableHeight());
          break;
      }
      pageFormat.setPaper(paper);
      
      if (clipBounds == null) {
        g2D.clipRect((int)pageFormat.getImageableX(), (int)pageFormat.getImageableY(), 
            (int)pageFormat.getImageableWidth(), (int)pageFormat.getImageableHeight());
      } else {  
        g2D.clipRect(clipBounds.x, (int)pageFormat.getImageableY(), 
            clipBounds.width, (int)pageFormat.getImageableHeight());
      }
    }
    
    View furnitureView = this.controller.getFurnitureController().getView();
    if (furnitureView != null 
        && (homePrint == null || homePrint.isFurniturePrinted())) {
      FurnitureTable furnitureTable = null;
      final FurnitureTable.FurnitureFilter furnitureFilter;
      if (furnitureView instanceof FurnitureTable
          && (homePrint == null
              || homePrint.isPlanPrinted()
              || homePrint.isView3DPrinted())) {
        final Level selectedLevel = home.getSelectedLevel();
        furnitureTable = (FurnitureTable)furnitureView;
        furnitureFilter = furnitureTable.getFurnitureFilter();
        furnitureTable.setFurnitureFilter(new FurnitureTable.FurnitureFilter() {
            public boolean include(Home home, HomePieceOfFurniture piece) {
              return (furnitureFilter == null || furnitureFilter.include(home, piece))
                  && piece.isAtLevel(selectedLevel)
                  && (piece.getLevel() == null || piece.getLevel().isViewable());
            }
          });
      } else {
        furnitureFilter = null;
      }
      pageExists = ((Printable)furnitureView).print(g2D, pageFormat, page);
      if (furnitureTable != null) {
        ((FurnitureTable)furnitureView).setFurnitureFilter(furnitureFilter);
      }
      if (pageExists == PAGE_EXISTS
          && !this.printablePages.contains(page)) {
        this.printablePages.add(page);
        this.furniturePageCount++;
      }
    }
    if (pageExists == NO_SUCH_PAGE 
        && planView != null 
        && (homePrint == null || homePrint.isPlanPrinted())) {
      pageExists = ((Printable)planView).print(g2D, pageFormat, page - this.furniturePageCount);
      if (pageExists == PAGE_EXISTS
          && !this.printablePages.contains(page)) {
        this.printablePages.add(page);
        this.planPageCount++;
      }
    }
    View view3D = this.controller.getHomeController3D().getView();
    if (pageExists == NO_SUCH_PAGE
        && view3D != null
        && (homePrint == null || homePrint.isView3DPrinted())) {
      pageExists = ((Printable)view3D).print(g2D, pageFormat, page - this.planPageCount - this.furniturePageCount);
      if (pageExists == PAGE_EXISTS
          && !this.printablePages.contains(page)) {
        this.printablePages.add(page);
      }
    }
    
    if (pageExists == PAGE_EXISTS) {
      g2D.setTransform(oldTransform);
      g2D.setClip(clipBounds);
      g2D.setFont(this.headerFooterFont);
      g2D.setColor(Color.BLACK);
      if (fixedHeaderPageLabel != null) {
        g2D.translate(xFixedHeader, yFixedHeader);
        fixedHeaderPageLabel.print(g2D);
        g2D.translate(-xFixedHeader, -yFixedHeader);
      }
      if (header != null) {
        g2D.drawString(header, xHeader, yHeader);
      }
      if (footer != null) {
        g2D.drawString(footer, xFooter, yFooter);
      }
      if (fixedFooterPageLabel != null) {
        g2D.translate(xFixedFooter, yFixedFooter);
        fixedFooterPageLabel.print(g2D);
        g2D.translate(-xFixedFooter, -yFixedFooter);
      }
    }  
    pageFormat.setPaper(oldPaper);    
    return pageExists;
  }

  @Override
  public Dimension getPreferredSize() {
    PageFormat pageFormat = getPageFormat(this.home.getPrint());
    double maxSize = Math.max(pageFormat.getWidth(), pageFormat.getHeight());
    Insets insets = getInsets();
    int maxPreferredSize = Math.round(400 * SwingTools.getResolutionScale());
    return new Dimension((int)(pageFormat.getWidth() / maxSize * maxPreferredSize) + insets.left + insets.right, 
        (int)(pageFormat.getHeight() / maxSize * maxPreferredSize) + insets.top + insets.bottom);
  }
    
  @Override
  protected void paintComponent(Graphics g) {
    try {
      Graphics2D g2D = (Graphics2D)g.create();
      PageFormat pageFormat = getPageFormat(this.home.getPrint());
      Insets insets = getInsets();
      double scale = (getWidth() - insets.left - insets.right) / pageFormat.getWidth();
      g2D.scale(scale, scale);
      print(g2D, pageFormat, this.page);
      g2D.dispose();
    } catch (PrinterException ex) {
      throw new RuntimeException(ex);
    }
  }  
  
  public void setPage(int page) {
    if (this.page != page) {
      this.page = page;
      repaint();
    }
  }
   
  public int getPage() {
    return this.page;
  }
  
  public int getPageCount() {
    if (this.pageCount == -1) {
      PageFormat pageFormat = getPageFormat(this.home.getPrint());
      BufferedImage dummyImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
      Graphics dummyGraphics = dummyImage.getGraphics();
      this.pageCount = 0; 
      try {
        while (print(dummyGraphics, pageFormat, this.pageCount) == Printable.PAGE_EXISTS) {
          this.pageCount++;
        }
      } catch (PrinterException ex) {
        throw new RuntimeException(ex);
      }
      dummyGraphics.dispose();
    }
    return this.pageCount;
  }
 
  public static PageFormat getPageFormat(HomePrint homePrint) {
    final PrinterJob printerJob = PrinterJob.getPrinterJob();
    if (homePrint == null) {
      return printerJob.defaultPage();
    } else {
      PageFormat pageFormat = new PageFormat();
      switch (homePrint.getPaperOrientation()) {
        case PORTRAIT :
          pageFormat.setOrientation(PageFormat.PORTRAIT);
          break;
        case LANDSCAPE :
          pageFormat.setOrientation(PageFormat.LANDSCAPE);
          break;
        case REVERSE_LANDSCAPE :
          pageFormat.setOrientation(PageFormat.REVERSE_LANDSCAPE);
          break;
      }
      Paper paper = new Paper();
      paper.setSize(homePrint.getPaperWidth(), homePrint.getPaperHeight());
      paper.setImageableArea(homePrint.getPaperLeftMargin(), homePrint.getPaperTopMargin(), 
          homePrint.getPaperWidth() - homePrint.getPaperLeftMargin() - homePrint.getPaperRightMargin(), 
          homePrint.getPaperHeight() - homePrint.getPaperTopMargin() - homePrint.getPaperBottomMargin());
      pageFormat.setPaper(paper);
      pageFormat = printerJob.validatePage(pageFormat);
      return pageFormat;
    }
  }
}