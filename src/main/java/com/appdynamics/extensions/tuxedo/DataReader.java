package com.appdynamics.extensions.tuxedo;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


public class DataReader implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(DataReader.class);

    private InputStream in;
    private List<String> output;

    public DataReader(InputStream in) {
        this.in = in;
        this.output = Lists.newArrayList();
    }

    public void run() {
        logger.debug("Started the Data reader");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String temp;
        try {
            while ((temp = reader.readLine()) != null) {
                output.add(temp);
            }
        } catch (IOException e) {
            logger.error("Exception while reading the output stream", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        logger.debug("Tuxedo Reader Process Completed");
    }

    public List<String> getOutput() {
        return output;
    }
}
