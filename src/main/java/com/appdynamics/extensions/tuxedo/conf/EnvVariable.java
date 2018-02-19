/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

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
