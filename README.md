# AppDynamics Tuxedo Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case
Tuxedo is an application server for non-Java languages. Specifically what this means is that it provides a bunch of facilities that help customers build and deploy enterprise applications written in C, C++, COBOL, and with the SALT add-on applications written in Python and Ruby
The Tuxedo Monitoring extension collects metrics from an Tuxedo server and uploads them to the AppDynamics Controller. 

##Prerequisite
The Tuxedo server should be installed and configured. Please find refer to this [link] (http://docs.oracle.com/cd/E18050_01/tuxedo/docs11gr1/install/inspin.html#wp1121497) for more details

##Installation

1. Run "mvn clean install"
2. Download and unzip the file 'target/TuxedoMonitor.zip' to \<machineagent install dir\>/monitors
3. Open config.yml and configure the Tuxedo arguments.
```
- tmadminCommand: tmadmin -r
  domainName: domain1
  envVariables:
    - name: TUXCONFIG
      value: /path/to/domain1/TUXCONFIG/file

- tmadminCommand: tmadmin -r
  domainName: domain2
  envVariables:
    - name: TUXCONFIG
      value: /path/to/domain2/TUXCONFIG/file
```

##Metrics
The following metrics are reported. The Metric Path is relative to the "metricPrefix" defined in the monitor.xml
```
Bulletin Board|Servers
Bulletin Board|Services
Bulletin Board|Queues
Bulletin Board|Groups
Bulletin Board|Interfaces
Groups|$GROUPNAME|Queues|$QUEUENAME|Requests Done
Groups|$GROUPNAME|Queues|$QUEUENAME |Load Done
Groups|$GROUPNAME|Queues|$QUEUENAME |Up Time (mins)
Groups|$GROUPNAME|Services|$SERVICENAME|Requests Done 
Groups|$GROUPNAME|Services|$SERVICENAME|Availability
Transactions|TMGACTIVE
Transactions|TMGABORTONLY
Transactions|TMGABORTED
Transactions|TMGREADY
Transactions|TMGDECIDED
```


#Custom Dashboard


##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere](http://community.appdynamics.com/t5/eXchange-Community-AppDynamics/Tuxedo-Monitoring-Extension/idi-p/9877) community.

##Support

For any questions or feature request, please contact [AppDynamics Help](mailto:help@appdynamics.com).
