package com.eteks.homeview3d.viewcontroller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.eteks.homeview3d.model.UserPreferences;

public class ThreadedTaskController implements Controller {
  private static ExecutorService    tasksExecutor;
  private final UserPreferences     preferences;
  private final ViewFactory         viewFactory;
  private final Callable<Void>      threadedTask;
  private final String              taskMessage;
  private final ExceptionHandler    exceptionHandler;
  private ThreadedTaskView          view;
  private Future<?>                 task;

  public ThreadedTaskController(Callable<Void> threadedTask,
                                String taskMessage,
                                ExceptionHandler exceptionHandler,
                                UserPreferences preferences, 
                                ViewFactory viewFactory) {
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.threadedTask = threadedTask;
    this.taskMessage = taskMessage;
    this.exceptionHandler = exceptionHandler;
  }
  
  public ThreadedTaskView getView() {
    if (this.view == null) {
      this.view = this.viewFactory.createThreadedTaskView(this.taskMessage, this.preferences, this);
    }
    return this.view;
  }

  public void executeTask(final View executingView) {
    if (tasksExecutor == null) {
      tasksExecutor = Executors.newSingleThreadExecutor();
    }

    this.task = tasksExecutor.submit(new FutureTask<Void>(this.threadedTask) {
        @Override
        public void run() {
          getView().invokeLater(new Runnable() {
              public void run() {
                getView().setTaskRunning(true, executingView);
              }
            });
          super.run();
        }
      
        @Override
        protected void done() {
          getView().invokeLater(new Runnable() {
              public void run() {
                getView().setTaskRunning(false, executingView);
                task = null;
              }
            });
          
          try {
            get();
          } catch (ExecutionException ex) {
            final Throwable throwable = ex.getCause();
            if (throwable instanceof Exception) {
              getView().invokeLater(new Runnable() {
                  public void run() {
                    exceptionHandler.handleException((Exception)throwable);
                  }
                });
            } else {
              throwable.printStackTrace();
            }
          } catch (final InterruptedException ex) {
            getView().invokeLater(new Runnable() {
                public void run() {
                  exceptionHandler.handleException(ex);
                }
              });
          }
        }
      });
  }
  
  public void cancelTask() {
    if (this.task != null) {
      this.task.cancel(true);
    }
  }
  
  public boolean isTaskRunning() {
    return this.task != null && !this.task.isDone();
  }

  public static interface ExceptionHandler {
    public void handleException(Exception ex);
  }
}
