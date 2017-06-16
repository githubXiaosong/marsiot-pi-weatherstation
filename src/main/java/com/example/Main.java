package com.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.marsiot.MarsiotConfig;
import java.util.HashSet;

import com.marsiot.agent.Agent;

/**
 * @author xiangstudio
 */
public class Main {

    /** Default filename for configuration properties */
    private static final String DEFAULT_CONFIG_FILENAME = "config.properties";

    /** Agent controlled by this loader */
    private static Agent agent = new Agent();

    /**
     * Start the agent loader.
     * 
     * @param args
     */
    public static void main(String[] args) {
        String propsFile = DEFAULT_CONFIG_FILENAME;
        FileInputStream in = null;
        try {
            in = new FileInputStream(propsFile);
            Properties props = new Properties();
            props.load(in);
            agent.load(props);
        } catch (IOException e) {
            System.out.print("Unable to load "+DEFAULT_CONFIG_FILENAME+"\n");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }

        if (agent.getCommandProcessorClassname() == null) {
            System.out.print("Can't load 'command.processor.classname' form "+DEFAULT_CONFIG_FILENAME+"\n");
            System.exit(0);
        }

        if (agent.getSiteToken() == null || agent.getSiteToken().length() == 0) {
            System.out.print("Can't load 'site.token' from "+DEFAULT_CONFIG_FILENAME+"\n");
            System.exit(0);
        }

        try {
            agent.start();
        } catch (Exception e) {
            System.out.print("Unable to start: " + e.getMessage() + "\n");
            System.exit(0);
        }

        System.out.print("Wellcome to " + agent.getMqttHostname() + "\n");
    }
}


