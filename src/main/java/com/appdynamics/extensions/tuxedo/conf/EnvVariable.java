package com.appdynamics.extensions.tuxedo.conf;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 8/7/14
 * Time: 11:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class EnvVariable {
    private String value;
    private String name;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
