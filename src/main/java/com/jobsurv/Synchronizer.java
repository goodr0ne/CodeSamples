package com.jobsurv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Synchronizer {

  private final ConcurrentHashMap<String, Lock> fileLocks;
  private final ConcurrentHashMap<String, Lock> operationLocks;

  /**
   * Default constructor, used only once in creating singleton instance phase
   */
  public Synchronizer() {
    fileLocks = new ConcurrentHashMap<String, Lock>();
    operationLocks = new ConcurrentHashMap<String, Lock>();
  }

  /**
   * Creates singleton Synchronizer instance when project is loaded to jvm
   */
  public static final Synchronizer instance = new Synchronizer();

  /**
   * Get instance for singleton Synchronizer class
   *
   * @return returns current instance of singleton Synchronizer
   */
  public static Synchronizer getInstance() {
    return instance;
  }

  public Lock getFileLock(String name, String file) {
    String totalName = name + "." + file;
    fileLocks.putIfAbsent(totalName, new ReentrantLock(true));
    return fileLocks.get(totalName);
  }

  public Lock getOperationLock(String name, String type) {
    String totalName = name + "." + type;
    operationLocks.putIfAbsent(totalName, new ReentrantLock(true));
    return operationLocks.get(totalName);
  }
}