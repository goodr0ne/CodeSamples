package com.jobsurv;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.gson.*;
import com.jobsurv.servlets.MainServlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.*;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.crypto.AesCipherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for all interactions with hdfs and other class serializations. This
 * class is a group of similar data serialization functions. Unused methods may
 * be in use in serialization and third_party software. All methods may be
 * transformed to static soon.
 *
 * This class may be separated to different classes due to large code volume.
 *
 * @author Yegor Scherbatkin
 */
public class Reader {
  private Gson gson = new Gson();
  private AesCipherService cipher;
  private Synchronizer sync;
  public static Logger log;
  private FileSystem fs;
  private final ConcurrentHashMap<String, byte[]> keys;

  public Reader() {
    keys = new ConcurrentHashMap<String, byte[]>();
    try {
      fs = SettingsInitializer.getInstance().setFS();
      cipher = new AesCipherService();
      sync = Synchronizer.getInstance();
      log = LoggerFactory.getLogger(MainServlet.class);
      log.error("Logger initialized");
      LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
      StatusPrinter.print(lc);
    } catch (Exception ignored) {}
  }

  public static final Reader instance = new Reader();

  public static Reader getInstance() {
    return instance;
  }

  /**
   * Writes to hadoop with encrypting, using read key function, hadoop api,
   * shiro encrypting.
   *
   * @param id      some user login
   * @param jsonObj stored object of that user in json string form
   * @param type    type of stored object (file name after dot)
   * @param secured flag for enabling shiro encryption
   */
  protected boolean writeHadoop(String id, String jsonObj, String type,
                                boolean secured)
          throws java.io.IOException {
    String operation = "build-in";
    if (operation.equals("thread")) {
      byte[] localKey = new byte[1];
      if (secured) {
        localKey = readKey(id);
      }
      new Thread(new WriteHDFSThread(id, jsonObj, type, secured, fs,
              cipher, localKey)).start();
    } else {
      if (StringUtils.isBlank(id) || StringUtils.isBlank(type)) {
        return false;
      }
      if (type.equals("key")) {
        return false;
      }
      try {
        sync.getFileLock(id, type).lock();
        Path path = new Path("/user/root/" + id + "/" + type);
        byte[] byteJsonObj = CodecSupport.toBytes(jsonObj);
        if (fs.exists(path)) {
          fs.delete(path, true);
        }
        if (secured) {
          byte[] key = readKey(id);
          byte[] encodedJsonObj = cipher.encrypt(byteJsonObj, key).getBytes();
          FSDataOutputStream in = fs.create(path);
          try {
            in.write(encodedJsonObj);
            in.close();
          } catch (Exception e) {
            in.close();
          }
        } else {
          FSDataOutputStream in = fs.create(path);
          in.write(byteJsonObj);
          in.close();
        }
        return true;
      } catch (Exception e) {
        return false;
      } finally {
        sync.getFileLock(id, type).unlock();
      }
    }
    return true;
  }

  /**
   * Reads from hadoop with decrypting (if secured = true), using read key
   * function, hadoop api, shiro decrypting.  Output is string, hadoop data is
   * byte[].
   *
   * @param id      some user login
   * @param type    type of read object (file name after dot)
   * @param secured flag for enabling shiro decryption
   * @return string - read object in json string form
   */
  protected String readHadoop(String id, String type, boolean secured)
          throws java.io.IOException {
    if (StringUtils.isBlank(id) || StringUtils.isBlank(type)) {
      return "";
    }
    if (type.equals("key")) {
      return "";
    }
    try {
      sync.getFileLock(id, type).lock();
      Path path = new Path("/user/root/" + id + "/" + type);
      if (fs.exists(path)) {
        FileStatus stat = fs.getFileStatus(path);
        int infoLength = (int) stat.getLen();
        byte[] codedJsonObj = new byte[infoLength];
        FSDataInputStream out = fs.open(path);
        try {
          int read = out.read(codedJsonObj);
          if (read > -1) {
            out.close();
          }
        } catch (Exception e) {
          out.close();
        }
        if (secured) {
          byte[] key = readKey(id);
          byte[] decodedObj = cipher.decrypt(codedJsonObj, key).getBytes();
          return CodecSupport.toString(decodedObj);
        } else {
          return CodecSupport.toString(codedJsonObj);
        }
      } else {
        return "";
      }
    } catch (Exception e) {
      return "";
    } finally {
      sync.getFileLock(id, type).unlock();
    }
  }

  /**
   *
   */
  public boolean createKey(String id, byte[] key) throws IOException {
    if (StringUtils.isBlank(id)) {
      return false;
    }
    try {
      sync.getFileLock(id, "key").lock();
      Path path = new Path("/user/root/" + id + "/key");
      return !fs.exists(path) && writeKey(id, key);
    } catch (Exception e) {
      return false;
    } finally {
      sync.getFileLock(id, "key").unlock();
    }
  }

  /**
   * Writes cipher key, using hadoop API write byte array to file function.
   *
   * @param id  string - some user login
   * @param key byte[] - key represented in byte array form, length is 16
   */
  protected boolean writeKey(String id, byte[] key) throws java.io.IOException {
    try {
      sync.getFileLock(id, "key").lock();
      Path path = new Path("/user/root/" + id + "/key");
      FSDataOutputStream in = fs.create(path);
      in.write(key);
      in.close();
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      sync.getFileLock(id, "key").unlock();
    }
  }

  /**
   * Reads cipher key, for decrypting and encrypting strings in
   * {@link #readHadoop} function.
   *
   * @param id a string that represents some user login
   * @return key represented in byte array form, length is 16
   */
  protected byte[] readKey(String id) throws java.io.IOException {
    if (keys.containsKey(id)) {
      return keys.get(id);
    }
    try {
      sync.getFileLock(id, "key").lock();
      Path path = new Path("/user/root/" + id + "/key");
      byte[] key = new byte[16];
      if (fs.exists(path)) {
        FSDataInputStream out = fs.open(path);
        try {
          int read = out.read(key);
          if (read > -1) {
            out.close();
          }
          keys.put(id, key);
        } catch (Exception e) {
          out.close();
          return key;
        }
      }
      return key;
    } catch (Exception e) {
      return new byte[1];
    } finally {
      sync.getFileLock(id, "key").unlock();
    }
  }
}
