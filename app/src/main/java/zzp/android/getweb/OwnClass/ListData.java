package zzp.android.getweb.OwnClass;

import java.io.Serializable;

/**
 * Created by yingqiang on 2015/5/20.
 */
//自定义数据形式
public class ListData implements Serializable {
    private java.util.Date date;
    private String name, path, link, id;
    private Boolean cb;

    public java.util.Date getDate() {
        return date;
    }

    public String getID() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Boolean getCheck() {
        return cb;
    }

    public void setDate(java.util.Date date) {
        this.date = date;
    }

    public void setID(String id) {
        this.id = id;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setCheck(Boolean cb) {
        this.cb = cb;
    }

    public ListData(java.util.Date date, String name, String path, String link, String id, Boolean cb) {
        this.date = date;
        this.name = name;
        this.path = path;
        this.link = link;
        this.id = id;
        this.cb = cb;
    }
}
