package com.appdynamics.extensions.tuxedo.conf;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 8/7/14
 * Time: 11:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class Domain {
    private String domainName;
    private String tmadminPath;
    private String metricPrefix;
    private String tmadminCommand;
    private EnvVariable[] envVariables;


    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getTmadminPath() {
        return tmadminPath;
    }

    public void setTmadminPath(String tmadminPath) {
        this.tmadminPath = tmadminPath;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public EnvVariable[] getEnvVariables() {
        return envVariables;
    }

    public void setEnvVariables(EnvVariable[] envVariables) {
        this.envVariables = envVariables;
    }

    public String getTmadminCommand() {
        return tmadminCommand;
    }

    public void setTmadminCommand(String tmadminCommand) {
        this.tmadminCommand = tmadminCommand;
    }
}
