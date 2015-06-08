package zzp.android.getweb.OwnClass;

/**
 * Created by yingqiang on 2015/5/27.
 */
import java.io.Serializable;
public class UploadFile implements Serializable {
    private String title ;
    private byte [] contentData ;
    private String mimeType ;
    private long contentLength ;
    private String ext ;
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public byte[] getContentData() {
        return contentData;
    }
    public void setContentData(byte[] contentData) {
        this.contentData = contentData;
    }
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    public long getContentLength() {
        return contentLength;
    }
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
    public String getExt() {
        return ext;
    }
    public void setExt(String ext) {
        this.ext = ext;
    }
}
