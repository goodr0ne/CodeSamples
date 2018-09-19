package com.jobsurv;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.crypto.AesCipherService;

/**
 *
 * @author Yegor Scherbatkin
 */
public class WriteHDFSThread implements Runnable {
  private String id, type, jsonObj;
  private boolean secured;
  private Synchronizer sync;
  private FileSystem fs;
  private AesCipherService cipher;
  private byte[] key;

  /**
   * Default constructor, used only once in creating singleton instance phase
   */
  public WriteHDFSThread(String id, String jsonObj, String type,
                         boolean secured, FileSystem fs,
                         AesCipherService cipher, byte[] key) {
    this.secured = secured;
    this.id = id;
    this.jsonObj = jsonObj;
    this.type = type;
    sync = Synchronizer.getInstance();
    this.fs = fs;
    this.cipher = cipher;
    this.key = key;
  }

  public void run()  {
    if (!StringUtils.isBlank(id) && !StringUtils.isBlank(type)
            && !type.equals("key")) {
      sync.getFileLock(id, type).lock();
      try {
        Path path = new Path("/user/root/" + id + "/" + type);
        byte[] byteJsonObj = CodecSupport.toBytes(jsonObj);
        if (fs.exists(path)) {
          fs.delete(path, true);
        }
        if (secured) {
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
      } catch (Exception ignored) {
      } finally {
        sync.getFileLock(id, type).unlock();
      }
    }
  }
}
