package com.eteks.homeview3d.model;

import java.math.BigDecimal;

public class CatalogLight extends CatalogPieceOfFurniture implements Light {
  private final LightSource [] lightSources;

  public CatalogLight(String id, String name, String description, Content icon, Content model, 
                                 float width, float depth, float height, float elevation, boolean movable, 
                                 LightSource [] lightSources,
                                 float [][] modelRotation, String creator,
                                 boolean resizable, BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, null, model, width, depth, height, elevation, movable,   
        lightSources, modelRotation, creator, resizable, price, valueAddedTaxPercentage);
  }
         
  public CatalogLight(String id, String name, String description, 
                      Content icon, Content planIcon, Content model, 
                      float width, float depth, float height, float elevation, boolean movable, 
                      LightSource [] lightSources,
                      float [][] modelRotation, String creator,
                      boolean resizable, BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, planIcon, model, width, depth, height, elevation, movable,   
        lightSources, modelRotation, creator, resizable, true, true, price, valueAddedTaxPercentage);
  }
         
  public CatalogLight(String id, String name, String description, 
                      Content icon, Content planIcon, Content model, 
                      float width, float depth, float height, float elevation, boolean movable, 
                      LightSource [] lightSources,
                      float [][] modelRotation, String creator,
                      boolean resizable, boolean deformable, boolean texturable,
                      BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, planIcon, model, width, depth, height, elevation, movable,   
        lightSources, null, modelRotation, creator, resizable, true, true, price, valueAddedTaxPercentage, null);
  }
         
  public CatalogLight(String id, String name, String description, 
                      Content icon, Content planIcon, Content model, 
                      float width, float depth, float height, float elevation, boolean movable, 
                      LightSource [] lightSources, String staircaseCutOutShape,
                      float [][] modelRotation, String creator,
                      boolean resizable, boolean deformable, boolean texturable,
                      BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, null, null, null, null, icon, planIcon, model, width, depth, height, elevation, movable, 
        lightSources, staircaseCutOutShape, modelRotation, creator, resizable, deformable, texturable, 
        price, valueAddedTaxPercentage, currency);
  }
         
  public CatalogLight(String id, String name, String description, 
                      String information, String [] tags, Long creationDate, Float grade, 
                      Content icon, Content planIcon, Content model, 
                      float width, float depth, float height, float elevation, boolean movable,
                      LightSource [] lightSources, String staircaseCutOutShape, 
                      float [][] modelRotation, String creator, 
                      boolean resizable, boolean deformable, boolean texturable, 
                      BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, 
        icon, planIcon, model, width, depth, height, elevation, 1f, movable, lightSources, 
        staircaseCutOutShape, modelRotation, creator, resizable, deformable, texturable, 
        price, valueAddedTaxPercentage, currency);
  }
         
  public CatalogLight(String id, String name, String description, 
                      String information, String [] tags, Long creationDate, Float grade, 
                      Content icon, Content planIcon, Content model, 
                      float width, float depth, float height, float elevation, float dropOnTopElevation, 
                      boolean movable, LightSource [] lightSources, String staircaseCutOutShape, 
                      float [][] modelRotation, String creator, 
                      boolean resizable, boolean deformable, boolean texturable, 
                      BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, 
        icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable, lightSources, 
        staircaseCutOutShape, modelRotation, false, creator, resizable, deformable, texturable, 
        price, valueAddedTaxPercentage, currency);
  }
         
  public CatalogLight(String id, String name, String description, 
                      String information, String [] tags, Long creationDate, Float grade, 
                      Content icon, Content planIcon, Content model, 
                      float width, float depth, float height, float elevation, float dropOnTopElevation, 
                      boolean movable, LightSource [] lightSources, String staircaseCutOutShape, 
                      float [][] modelRotation, boolean backFaceShown, String creator, 
                      boolean resizable, boolean deformable, boolean texturable, 
                      BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    super(id, name, description, information, tags, creationDate, grade, 
        icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable, 
        staircaseCutOutShape, modelRotation, backFaceShown, creator, resizable, deformable, texturable, 
        price, valueAddedTaxPercentage, currency);
    this.lightSources = lightSources;
  }
         
  public LightSource [] getLightSources() {
    if (this.lightSources.length == 0) {
      return this.lightSources;
    } else {
      return this.lightSources.clone();
    }
  }
}
