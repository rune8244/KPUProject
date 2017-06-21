package com.eteks.homeview3d.viewcontroller;

public interface ThreadedTaskView extends View {
  public abstract void invokeLater(Runnable runnable);
  public abstract void setTaskRunning(boolean taskRunning,
                                      View executingView);
}