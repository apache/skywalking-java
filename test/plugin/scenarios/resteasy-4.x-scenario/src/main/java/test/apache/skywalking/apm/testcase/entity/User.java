package test.apache.skywalking.apm.testcase.entity;


import java.io.Serializable;

public class User implements Serializable {

    private int id;
    private String userName;

    public User(int id) {
        this.id = id;
    }

    public User(int id, String userName) {
        this.id = id;
        this.userName = userName;
    }

    public User() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}