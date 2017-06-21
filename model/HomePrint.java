package com.eteks.homeview3d.model;

import java.io.Serializable;

public class HomePrint implements Serializable {

  public enum PaperOrientation {PORTRAIT, LANDSCAPE, REVERSE_LANDSCAPE};
    
  private static final long serialVersionUID = -2868070768300325498L;
  
  private final PaperOrientation paperOrientation;
  private final float            paperWidth;
  private final float            paperHeight;
  private final float            paperTopMargin;
  private final float            paperLeftMargin;
  private final float            paperBottomMargin;
  private final float            paperRightMargin;
  private final boolean          furniturePrinted;
  private final boolean          planPrinted;
  private final boolean          view3DPrinted;
  private final Float            planScale;
  private final String           headerFormat;
  private final String           footerFormat;

  public HomePrint(PaperOrientation paperOrientation,
                   float paperWidth,
                   float paperHeight,
                   float paperTopMargin,
                   float paperLeftMargin,
                   float paperBottomMargin,
                   float paperRightMargin,
                   boolean furniturePrinted,
                   boolean planPrinted,
                   boolean view3DPrinted,
                   Float planScale,
                   String headerFormat,
                   String footerFormat) {
    this.paperOrientation = paperOrientation;
    this.paperWidth = paperWidth;
    this.paperHeight = paperHeight;
    this.paperTopMargin = paperTopMargin;
    this.paperLeftMargin = paperLeftMargin;
    this.paperBottomMargin = paperBottomMargin;
    this.paperRightMargin = paperRightMargin;
    this.furniturePrinted = furniturePrinted;
    this.planPrinted = planPrinted;
    this.view3DPrinted = view3DPrinted;
    this.planScale = planScale;
    this.headerFormat = headerFormat;
    this.footerFormat = footerFormat;
  }

  public PaperOrientation getPaperOrientation() {
    return this.paperOrientation;
  }

  public float getPaperBottomMargin() {
    return this.paperBottomMargin;
  }

  public float getPaperHeight() {
    return this.paperHeight;
  }

  public float getPaperLeftMargin() {
    return this.paperLeftMargin;
  }

  public float getPaperRightMargin() {
    return this.paperRightMargin;
  }

  public float getPaperTopMargin() {
    return this.paperTopMargin;
  }

  public float getPaperWidth() {
    return this.paperWidth;
  }

  public boolean isFurniturePrinted() {
    return this.furniturePrinted;
  }

  public boolean isPlanPrinted() {
    return this.planPrinted;
  }

  public boolean isView3DPrinted() {
    return this.view3DPrinted;
  } 

  public Float getPlanScale() {
    return this.planScale;
  }

  public String getHeaderFormat() {
    return this.headerFormat;
  }

  public String getFooterFormat() {
    return this.footerFormat;
  }
}
