package com.eteks.homeview3d.swing;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.eteks.homeview3d.model.UserPreferences;

// 컨트롤러 액션
public class ControllerAction extends ResourceAction {
  private final Object    controller;
  private final Method    controllerMethod;
  private final Object [] parameters;

public ControllerAction(UserPreferences preferences, 
                          Class<?> resourceClass, 
                          String actionPrefix, 
                          Object controller, 
                          String method, 
                          Object ... parameters) throws NoSuchMethodException {
    this(preferences, resourceClass, actionPrefix, false, controller, method, parameters);
  }

 public ControllerAction(UserPreferences preferences, 
                          Class<?> resourceClass, 
                          String actionPrefix,
                          boolean enabled,
                          Object controller, 
                          String method, 
                          Object ... parameters) throws NoSuchMethodException {
    super(preferences, resourceClass, actionPrefix, enabled);
    this.controller = controller;
    this.parameters = parameters;
    Class<?> [] parametersClass = new Class [parameters.length];
    for(int i = 0; i < parameters.length; i++)
      parametersClass [i] = parameters [i].getClass();
    
    this.controllerMethod = controller.getClass().getMethod(method, parametersClass);
  }

  @Override
  public void actionPerformed(ActionEvent ev) {
    try {
      this.controllerMethod.invoke(controller, parameters);
    } catch (IllegalAccessException ex) {
      throw new RuntimeException (ex);
    } catch (InvocationTargetException ex) {
      throw new RuntimeException (ex);
    }
  }
}
