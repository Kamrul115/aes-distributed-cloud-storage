
package com.mycompany.cloudsystem;

import java.io.Serializable;

public class FileInfo implements Serializable {
    private String filename;
    private byte[] encryptedData;
    private String uploadedBy;

    public FileInfo(String filename, byte[] encryptedData, String uploadedBy) {
        this.filename = filename;
        this.encryptedData = encryptedData;
        this.uploadedBy = uploadedBy;
    }

    public String getFilename() { return filename; }
    public byte[] getEncryptedData() { return encryptedData; }
    public String getUploadedBy() { return uploadedBy; }
}