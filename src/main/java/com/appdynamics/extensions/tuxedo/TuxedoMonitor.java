package com.appdynamics.extensions.tuxedo;

import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.tuxedo.conf.Domain;
import com.appdynamics.extensions.tuxedo.conf.EnvVariable;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/14/14
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class TuxedoMonitor extends AManagedMonitor {
    private static final Logger logger = LoggerFactory.getLogger(TuxedoMonitor.class);
    private static final byte[] LINE_SEPARATOR = System.getProperty("line.separator").getBytes();
    private static Pattern TXN_STATUS_PTN =
            Pattern.compile("Transaction status: (TMGACTIVE|TMGABORTONLY|TMGABORTED|TMGCOMCALLED|TMGREADY|TMGDECIDED)");
    private static final Map<String, String> defaultArgs = new HashMap<String, String>() {{
        put("metric-prefix", "Custom Metrics|Tuxedo");
    }};
    public static final String[] TUX_COMMANDS = new String[]{"psc", "psr", "bbs", "pt"};

    //This is to compare the current and previous value to calculate per min metrics.
    private Map<String, Long> previousValueMap;

    public TuxedoMonitor() {
        String version = TuxedoMonitor.class.getPackage().getImplementationTitle();
        String msg = String.format("Using Monitor Version [%s]", version);
        logger.info(msg);
        System.out.println(msg);
        previousValueMap = new HashMap<String, Long>();
    }

    public TaskOutput execute(Map<String, String> argsMap, TaskExecutionContext executionContext)
            throws TaskExecutionException {
        ArgumentsValidator.validateArguments(argsMap, defaultArgs);
        String configFile = argsMap.get("config-file");
        File file = PathResolver.getFile(configFile, AManagedMonitor.class);
        if (file != null) {
            Domain[] domains = YmlReader.readFromFile(file, Domain[].class);
            execute(domains);
        } else {
            logger.error("Cannot locate the config file given by the path {}", configFile);
        }
        return new TaskOutput("Tuxedo Process Completed");
    }

    private void execute(Domain[] domains) {
        if (domains != null && domains.length > 0) {
            for (int i = 0; i < domains.length; i++) {
                Domain domain = domains[i];
                String metricPrefix = getMetricPrefix(domain, i);
                logger.info("Collecting the Tuxedo stats for the domain {}", metricPrefix);
                execute(domain, metricPrefix, TUX_COMMANDS);
            }
        } else {
            logger.error("Tuxedo domains are note added in the config.yml file.");
        }
    }

    private void execute(Domain domain, String metricPrefix, String[] tuxCommands) {
        DataReader reader = null;
        ProcessBuilder pb = createProcessBuilder(domain);
        EnvVariable[] envVariables = domain.getEnvVariables();
        if (envVariables != null && envVariables.length > 0) {
            Map<String, String> environment = pb.environment();
            for (EnvVariable variable : envVariables) {
                environment.put(variable.getName(), variable.getValue());
                logger.debug("Adding the environment variable {}:{}", variable.getName(), variable.getValue());
            }
        }
        try {
            logger.debug("Initializing the process {}");
            Process process = pb.start();
            InputStream errorStream = process.getErrorStream();
            InputStream inputStream = process.getInputStream();
            OutputStream out = process.getOutputStream();
            Thread errReaderThread = new Thread(new ErrorLogger(errorStream), "Tuxedo Err Reader");
            errReaderThread.setDaemon(true);
            errReaderThread.start();
            reader = new DataReader(inputStream);
            Thread dataReaderThread = new Thread(reader, "Tuxedo Out Reader");
            dataReaderThread.setDaemon(true);
            dataReaderThread.start();
            writeCommand("echo on", out);
            writeCommand("verbose on", out);
            if (tuxCommands != null && tuxCommands.length > 0) {
                for (String command : tuxCommands) {
                    writeCommand(command, out);
                }
            }
            writeCommand("q", out);
            out.flush();
            out.close();
            //Wait only for a max of 10 seconds
            dataReaderThread.join(10000);
            processOutput(metricPrefix, reader);
        } catch (IOException e) {
            logger.error("Error while interacting with Tuxedo", e);
        } catch (InterruptedException e) {
            logger.error("", e);
        }
    }

    private ProcessBuilder createProcessBuilder(Domain domain) {
        if (!Strings.isNullOrEmpty(domain.getTmadminCommand())) {
            String command = domain.getTmadminCommand();
            String[] split = command.split(" ");
            if (logger.isDebugEnabled()) {
                logger.debug("The original command array is {}", Arrays.toString(split));
            }
            return new ProcessBuilder(split);
        } else if (!Strings.isNullOrEmpty(domain.getTmadminPath())) {
            String tmadminPath = domain.getTmadminPath();
            if (new File(tmadminPath).exists()) {
                logger.debug("Creating the process builder with {} and -r", tmadminPath);
                return new ProcessBuilder(tmadminPath, "-r");
            } else {
                throw new RuntimeException("tmadminPath is not valid " + tmadminPath);
            }
        } else {
            logger.error("The tmadmin command = {}, path = {}", domain.getTmadminCommand(), domain.getTmadminPath());
            throw new RuntimeException("The tmadminCommand and tmadminPath are empty.");
        }
    }

    private void processOutput(String metricPrefix, DataReader reader) {
        if (reader != null) {
            List<String> output = reader.getOutput();
            if (output != null) {
                if (logger.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder("The tmadmin output is ");
                    sb.append("\n");
                    for (String line : output) {
                        sb.append(line).append("\n");
                    }
                    logger.debug(sb.toString());
                }
                processOutput(output, metricPrefix);
            } else {
                logger.warn("The output read from the tuxedo output stream is null");
            }
        }
    }

    protected void processOutput(List<String> output, String metricPrefix) {
        Map<String, List<String>> outMap = extractOutput(output);
        printBulletinBoard(outMap, metricPrefix);
        printQueueDetails(outMap, metricPrefix);
        printServiceDetails(outMap, metricPrefix);
        printTransactions(outMap, metricPrefix);
    }

    private void printTransactions(Map<String, List<String>> outMap, String metricPathPattern) {
        metricPathPattern = metricPathPattern + "|Transactions|%s";
        GroupCounter<String> statusCounter = new GroupCounter<String>();
        List<String> list = outMap.get("pt");
        if (list != null) {
            for (String out : list) {
                Matcher matcher = TXN_STATUS_PTN.matcher(out);
                if (matcher.find()) {
                    statusCounter.increment(matcher.group(1));
                }
            }
        }
        String[] stats = {"TMGACTIVE", "TMGABORTONLY", "TMGABORTED", "TMGCOMCALLED", "TMGREADY", "TMGDECIDED"};
        for (String status : stats) {
            Long count = statusCounter.get(status);
            if (count == null) {
                count = 0L;
            }
            String countStr = String.valueOf(count);
            printCollectiveObservedCurrent(String.format(metricPathPattern, status), countStr);
        }
    }

    private void printServiceDetails(Map<String, List<String>> outMap, String metricPathPattern) {
        metricPathPattern = metricPathPattern + "|Groups|%s|Services|%s|%s";
        List<String> list = outMap.get("psc");
        if (list != null) {
            List<Map<String, String>> listMap = extractKeyValueMap(list);
            for (Map<String, String> map : listMap) {
                String groupId = map.get("Group ID");
                if (groupId != null) {
                    if (groupId.contains(",")) {
                        groupId = groupId.substring(0, groupId.indexOf(","));
                    }
                }
                String serviceName = map.get("Service Name");
                logger.debug("Service Details: The service name is {} and group id is {}", serviceName, groupId);
                if (groupId != null && serviceName != null) {
                    String reqDone = map.get("Requests Done");
                    String status = map.get("Current status");
                    logger.debug("Service Details: The Req Done is {} , Current status is {}", reqDone, status);
                    if (reqDone != null) {
                        String metricPath = String.format(metricPathPattern, groupId, serviceName, "Requests Done");
                        printCollectiveObservedCurrent(metricPath, reqDone);
                        Long perMinValue = getPerMinValue(metricPath, reqDone);
                        if (perMinValue != null) {
                            metricPath = String.format(metricPathPattern, groupId, serviceName, "Requests per Minute");
                            printCollectiveObservedCurrent(metricPath, perMinValue.toString());
                        }

                    }
                    if (status != null) {
                        String metricPath = String.format(metricPathPattern, groupId, serviceName, "Availability");
                        int code = getAvailabilityCode(status);
                        printCollectiveObservedCurrent(metricPath, String.valueOf(code));
                    }
                }
            }
        }
    }

    private int getAvailabilityCode(String status) {
        int code;
        if (status.equals("AVAILABLE")) {
            code = 1;
        } else {
            code = 0;
        }
        return code;
    }

    private void printQueueDetails(Map<String, List<String>> outMap, String metricPathPattern) {
        metricPathPattern = metricPathPattern + "|Groups|%s|Queues|%s|%s";
        List<String> list = outMap.get("psr");
        if (list != null) {
            List<Map<String, String>> listMap = extractKeyValueMap(list);
            for (Map<String, String> map : listMap) {
                String groupId = map.get("Group ID");
                if (groupId != null) {
                    if (groupId.contains(",")) {
                        groupId = groupId.substring(0, groupId.indexOf(","));
                    }
                }
                String qName = map.get("Queue Name");
                logger.debug("Queue Details: The Group is {} and Queue is {}", groupId, qName);
                if (groupId != null && qName != null) {
                    String reqDone = map.get("Requests done");
                    String loadDone = map.get("Load done");
                    String upTime = map.get("Up time");
                    logger.debug("Queue Details: The Req Done is {} , Load Done is {} and Up Time is {}", reqDone, loadDone, upTime);
                    if (reqDone != null) {
                        String metricPath = String.format(metricPathPattern, groupId, qName, "Requests Done");
                        printCollectiveObservedCurrent(metricPath, reqDone);
                        Long perMinValue = getPerMinValue(metricPath, reqDone);
                        if (perMinValue != null) {
                            metricPath = String.format(metricPathPattern, groupId, qName, "Requests per Minute");
                            printCollectiveObservedCurrent(metricPath, perMinValue.toString());
                        }

                    }

                    if (loadDone != null) {
                        String metricPath = String.format(metricPathPattern, groupId, qName, "Load Done");
                        printCollectiveObservedCurrent(metricPath, loadDone);
                        Long perMinValue = getPerMinValue(metricPath, loadDone);
                        if (perMinValue != null) {
                            metricPath = String.format(metricPathPattern, groupId, qName, "Load per Minute");
                            printCollectiveObservedCurrent(metricPath, perMinValue.toString());
                        }
                    }
                    if (upTime != null) {
                        long mins = convertToMinutes(upTime);
                        String metricPath = String.format(metricPathPattern, groupId, qName, "Up Time (mins)");
                        printCollectiveObservedCurrent(metricPath, String.valueOf(mins));
                    }
                }
            }
        }
    }

    private long convertToMinutes(String upTime) {
        String[] split = upTime.split(":");
        long seconds = 0;
        if (split.length == 3) {
            seconds += (Long.parseLong(split[0]) * 60 * 60);
            seconds += (Long.parseLong(split[1]) * 60);
            seconds += Long.parseLong(split[2]);
        } else if (split.length == 2) {
            seconds += Long.parseLong(split[0]) * 60;
            seconds += Long.parseLong(split[1]);
        } else if (split.length == 1) {
            seconds += Long.parseLong(split[0]);
        }
        return (seconds / 60);
    }

    /**
     * There will be 1 block of data for each Service or Queue, separated by a blank line
     * The outer List contains each block and inside the List contains a Map of key-value pairs.
     *
     * @param list
     * @return
     */
    private List<Map<String, String>> extractKeyValueMap(List<String> list) {
        List<Map<String, String>> listMap = Lists.newArrayList();
        Map<String, String> map = Maps.newHashMap();
        listMap.add(map);
        for (String out : list) {
            if (out.trim().isEmpty()) {
                map = Maps.newHashMap();
                listMap.add(map);
            } else {
                int start = out.indexOf(":");
                if (start > 0) {
                    String key = out.substring(0, start).trim();
                    String value = out.substring(start + 1).trim();
                    map.put(key, value);
                } else {
                    logger.warn("Cannot extract as key-value pair {}", out);
                }
            }
        }
        return listMap;
    }

    private void printBulletinBoard(Map<String, List<String>> outMap, String metricPrefix) {
        metricPrefix = metricPrefix + "|Bulletin Board|";
        List<String> list = outMap.get("bbs");
        if (list != null && !list.isEmpty()) {
            for (String out : list) {
                if (out.contains("Current number of servers")) {
                    String value = getValue(out);
                    printCollectiveObservedCurrent(metricPrefix + "Servers", value);
                } else if (out.contains("Current number of services")) {
                    String value = getValue(out);
                    printCollectiveObservedCurrent(metricPrefix + "Services", value);
                } else if (out.contains("Current number of request queues")) {
                    String value = getValue(out);
                    printCollectiveObservedCurrent(metricPrefix + "Queues", value);
                } else if (out.contains("Current number of server groups")) {
                    String value = getValue(out);
                    printCollectiveObservedCurrent(metricPrefix + "Groups", value);
                } else if (out.contains("Current number of interfaces")) {
                    String value = getValue(out);
                    printCollectiveObservedCurrent(metricPrefix + "Interfaces", value);
                }
            }
        }
    }

    private String getValue(String out) {
        String[] split = out.split(": ");
        if (split.length == 2) {
            return split[1];
        } else {
            logger.warn("Cannot extract the value of the output using the delimiter ':' from {}", out);
            return null;
        }
    }

    protected Map<String, List<String>> extractOutput(List<String> output) {
        Map<String, List<String>> outputMap = Maps.newHashMap();
        String command = null;
        List<String> list = null;
        for (String s : output) {
            if (s.startsWith("> ")) {
                command = s.substring(2);
                list = Lists.newArrayList();
                outputMap.put(command, list);
                logger.debug("Extracted the command {}", command);
            } else {
                if (list != null) {
                    list.add(s);
                }
            }
        }
        return outputMap;
    }

    private void writeCommand(String cmd, OutputStream out) throws IOException {
        logger.debug("Writing the tuxedo command {}", cmd);
        out.write(cmd.getBytes());
        out.write(LINE_SEPARATOR);
    }


    //Assuming that the extension runs every minute
    private Long getPerMinValue(String metricPath, String currentValStr) {
        if (!Strings.isNullOrEmpty(currentValStr)) {
            try {
                long currentValue = Long.parseLong(currentValStr);
                Long previousValue = previousValueMap.get(metricPath);
                previousValueMap.put(metricPath, currentValue);
                if (previousValue != null) {
                    return currentValue - previousValue;
                }
            } catch (NumberFormatException e) {
                logger.error("The value {} is not parsable", currentValStr);
            }
        }
        return null;
    }

    private void printCollectiveObservedCurrent(String metricPath, String metricValue) {
        printMetric(metricPath, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    public void printMetric(String metricPath, String metricValue, String aggregation, String timeRollup,
                            String cluster) {
        MetricWriter metricWriter = getMetricWriter(metricPath,
                aggregation,
                timeRollup,
                cluster
        );


        if (metricValue != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sending [" + aggregation + "/" + timeRollup + "/" + cluster
                        + "] metric = " + metricPath + " = " + metricValue);
            }
            metricWriter.printMetric(metricValue);
        } else {
            logger.warn("Null metric value received for {}", metricPath);
        }
    }

    private String getMetricPrefix(Domain domain, int index) {
        String metricPrefix = domain.getMetricPrefix();
        logger.debug("The metric prefix from the config file is {}", metricPrefix);
        if (Strings.isNullOrEmpty(metricPrefix)) {
            String domainName = domain.getDomainName();
            logger.debug("The domain name from the config file is {}", domainName);
            if (!Strings.isNullOrEmpty(domainName)) {
                metricPrefix = "Custom Metrics|Tuxedo|" + domainName;
            }
        }
        if (Strings.isNullOrEmpty(metricPrefix)) {
            metricPrefix = "Custom Metrics|Tuxedo|Domain" + index;
        }
        logger.debug("The final metric prefix is {}", metricPrefix);
        return metricPrefix;
    }

//    public static void main(String[] args) {
//        String path = "/home/abey/oracle/tuxedo11gR1/bin/tmadmin";
//        HashMap<String, String> argsMap = Maps.newHashMap();
//        argsMap.put("metric-prefix", "Custom Metrics|Tuxedo");
//        argsMap.put("tmadmin-path", path);
//        new TuxedoMonitor().execute(new String[]{"psc", "psr", "bbs", "pt"}, argsMap);
//    }
}
