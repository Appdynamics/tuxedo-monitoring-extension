package com.appdynamics.extensions.tuxedo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/14/14
 * Time: 4:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ErrorLogger implements Runnable {
    public static final Logger logger = LoggerFactory.getLogger(ErrorLogger.class);

    private InputStream errorStream;

    public ErrorLogger(InputStream errorStream) {
        this.errorStream = errorStream;
    }

    public void run() {
        logger.debug("Started the Error reader");
        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
        String temp = null;
        try {
            while ((temp = reader.readLine()) != null) {
                logger.error(temp);
            }
        } catch (IOException e) {
            logger.error("Exception while reading the error stream", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        logger.debug("Tuxedo Error Stream closed");
    }
}
