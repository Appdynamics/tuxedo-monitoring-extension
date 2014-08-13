package com.appdynamics.extensions.tuxedo;

import com.appdynamics.extensions.tuxedo.conf.Domain;
import com.appdynamics.extensions.yml.YmlReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/15/14
 * Time: 10:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class TuxedoMonitorTest {

    @Test
    public void testProcessOutput() throws IOException {
        InputStream in = getClass().getResourceAsStream("/tuxdump.log");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        List<String> list = new ArrayList<String>();
        String temp;
        while ((temp = br.readLine()) != null) {
            list.add(temp);
        }
        br.close();
        TuxedoMonitor monitor = new TuxedoMonitor();
        monitor.processOutput(list, "Tuxedo");
    }

    @Test
    public void testReadValidConfiguration() throws FileNotFoundException {
        Domain[] domains = YmlReader.readFromClasspath("/conf/config.yml", Domain[].class);
        Assert.assertEquals(2, domains.length);
    }
}
