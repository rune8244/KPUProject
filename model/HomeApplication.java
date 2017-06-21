package com.eteks.homeview3d.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class HomeApplication {
  private List<Home> homes = new ArrayList<Home>();
  private final CollectionChangeSupport<Home> homesChangeSupport = 
                             new CollectionChangeSupport<Home>(this);

  public void addHomesListener(CollectionListener<Home> listener) {
    this.homesChangeSupport.addCollectionListener(listener);
  }

  public void removeHomesListener(CollectionListener<Home> listener) {
    this.homesChangeSupport.removeCollectionListener(listener);
  } 

  public Home createHome() {
    return new Home(getUserPreferences().getNewWallHeight());
  }

  public List<Home> getHomes() {
    return Collections.unmodifiableList(this.homes);
  }

  public void addHome(Home home) {
    this.homes = new ArrayList<Home>(this.homes);
    this.homes.add(home);
    this.homesChangeSupport.fireCollectionChanged(home, this.homes.size() - 1, CollectionEvent.Type.ADD);
  }

  public void deleteHome(Home home) {
    int index = this.homes.indexOf(home);
    if (index != -1) {
      this.homes = new ArrayList<Home>(this.homes);
      this.homes.remove(index);
      this.homesChangeSupport.fireCollectionChanged(home, index, CollectionEvent.Type.DELETE);
    }
  }

  public abstract HomeRecorder getHomeRecorder();

  public HomeRecorder getHomeRecorder(HomeRecorder.Type type) {
    return getHomeRecorder();
  }

  public abstract UserPreferences getUserPreferences();

  public String getName() {
    return "Sweet Home 3D";
  }

  public String getVersion() {
    return "";
  }

  public String getId() {
    return null;
  }
}
