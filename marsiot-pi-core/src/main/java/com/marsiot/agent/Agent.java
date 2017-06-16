/*
 * Copyright (c) MARSIOT. All rights reserved. http://www.marsiot.com
 */
package com.marsiot.agent;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import com.google.protobuf.AbstractMessageLite;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.Model;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.SiteWhere;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.SiteWhere.Acknowledge;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.SiteWhere.Command;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.SiteWhere.RegisterDevice;

import com.marsiot.MarsiotConfig;
import com.pi4j.system.SystemInfo;
import com.marsiot.ApiClient;
import com.marsiot.Update;
import com.marsiot.Update.AppVersionInfo;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.io.File;
import java.io.FileInputStream;

public class Agent {

    /** Static logger instance */
    private static final java.util.logging.Logger LOGGER = Logger.getLogger(Agent.class.getName());

    /** Default outbound SiteWhere MQTT topic */
    //private static final String DEFAULT_MQTT_OUTBOUND_SITEWHERE = "SiteWhere/input/protobuf";

    /** Default MQTT hostname */
    //private static final String DEFAULT_MQTT_HOSTNAME = "www.marsiot.com";

    /** Default MQTT port */
    private static final int MQTT_PORT = 1883;
    private static final int MQTTS_PORT = 8883;

    /** limx debug, Default Specification Token */
    private static final String DEFAULT_SPECIFICATION_TOKEN = "7dfd6d63-5e8d-4380-be04-fc5c73801dfb";

    /** Command processor Java classname */
    private String commandProcessorClassname;

    /** Hardware id */
    private String hardwareId;

    /** Specification token */
    private String specificationToken;

    /** Site token */
    private String siteToken;

    /** model name */
    private String modelName;

    /** model description */
    private String modelDescription;

    /** MQTT server hostname */
    private String mqttHostname;

    /** MQTT server port */
    private int mqttPort;

    /** Outbound SiteWhere MQTT topic */
    private String outboundSiteWhereTopic;

    /** Inbound SiteWhere MQTT topic */
    private String inboundSiteWhereTopic;

    /** Inbound specification command MQTT topic */
    private String inboundCommandTopic;

    /** MQTT client */
    private MQTT mqtt;

    /** MQTT connection */
    private BlockingConnection connection;

    /** Outbound message processing */
    private MQTTOutbound outbound;

    /** Inbound message processing */
    private MQTTInbound inbound;

    /** Used to execute MQTT inbound in separate thread */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Start the agent using the command processor specified by classname.
     * 
     * @throws MarsiotAgentException
     */
    public void start() throws MarsiotAgentException {
        start(null);
    }

    /**
     * Start the agent.
     */
    public void start(IAgentCommandProcessor processor) throws MarsiotAgentException {
        System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.NoOpLog");
        if (MarsiotConfig.DEBUG) {
            LOGGER.setLevel(Level.ALL);        
        } else {
            LOGGER.setLevel(Level.OFF);
        }

        /*Update update = ApiClient.checkVersionOnMarsiot();
        if (update != null) {
            AppVersionInfo newVersion = update.getDownloadAppVersionInfo("marsiotpi");
            if (newVersion != null && newVersion.code > MarsiotConfig.VERSION) {
                System.out.print("\nCurrent version <" + MarsiotConfig.VERSION +">\n");
                System.out.print("Current version <" + newVersion.code +">\n");
                System.out.print("Update Log:\n" + newVersion.info +"\n\n");
                System.out.print("Please download " + newVersion.code + " version from:\nhttp://www.marsiot.com/download/mariot.jar\n\n");
            }
        }*/

        LOGGER.info("SiteWhere agent starting...");
        this.mqtt = new MQTT();

        if (MarsiotConfig.TLS) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                KeyStore ks = KeyStore.getInstance("JKS");
                    
                File trustFile = new File("./server.jks");
                ks.load(new FileInputStream(trustFile), "20020603".toCharArray());
                tmf.init(ks);
                sslContext.init(null, tmf.getTrustManagers(), null);
                mqtt.setSslContext(sslContext);
            } catch (Exception e) {
                throw new MarsiotAgentException("Failed to load certificate!", e);
            }

            /*
            // Set username if provided.
            if (!StringUtils.isEmpty(component.getUsername())) {
                mqtt.setUserName(component.getUsername());
            }
            // Set password if provided.
            if (!StringUtils.isEmpty(component.getPassword())) {
               mqtt.setPassword(component.getPassword());
            }
            */
        }

        try {
            if (!MarsiotConfig.TLS) {
                mqtt.setHost(getMqttHostname(), getMqttPort());
            } else {
                mqtt.setHost("tls://"+getMqttHostname()+":"+getMqttPort());
            }
        } catch (URISyntaxException e) {
            throw new MarsiotAgentException("Invalid hostname for MQTT server.", e);
        }

        connection = mqtt.blockingConnection();
        try {
            connection.connect();
        } catch (Exception e) {
            throw new MarsiotAgentException("Unable to establish MQTT connection.", e);
        }

        LOGGER.info("Connected to MQTT broker.");

        // Create outbound message processor.
        outbound = new MQTTOutbound(connection, getOutboundSiteWhereTopic());

        // Create an instance of the command processor.
        if (processor == null) {
            processor = createProcessor();
        }
        processor.setHardwareId(hardwareId);
        processor.setSpecificationToken(specificationToken);
        processor.setSiteToken(siteToken);
        processor.setModelName(modelName);
        processor.setModelDescription(modelDescription);
        processor.setEventDispatcher(outbound);

        // Create inbound message processing thread.
        inbound = new MQTTInbound(connection, getInboundSiteWhereTopic(), getInboundCommandTopic(), processor,
                        outbound);

        // Handle shutdown gracefully.
        Runtime.getRuntime().addShutdownHook(new ShutdownHandler());

        // Starts inbound processing loop in a separate thread.
        executor.execute(inbound);

        // Executes any custom startup logic.
        processor.executeStartupLogic(getHardwareId(), getSpecificationToken(), outbound);

        LOGGER.info("SiteWhere agent started.");
    }

    /**
     * Create an instance of the command processor. FOs * @return
     * 
     * @throws MarsiotAgentException
     */
    protected IAgentCommandProcessor createProcessor() throws MarsiotAgentException {
        try {
            Class<?> clazz = Class.forName(getCommandProcessorClassname());
            IAgentCommandProcessor processor = (IAgentCommandProcessor) clazz.newInstance();
            return processor;
        } catch (ClassNotFoundException e) {
            throw new MarsiotAgentException(e);
        } catch (InstantiationException e) {
            throw new MarsiotAgentException(e);
        } catch (IllegalAccessException e) {
            throw new MarsiotAgentException(e);
        }
    }

    /**
     * Internal class for sending MQTT outbound messages.
     * 
     * @author Derek
     */
    public static class MQTTOutbound implements ISiteWhereEventDispatcher {
        /** MQTT outbound topic */
        private String topic;

        /** MQTT connection */
        private BlockingConnection connection;

        public MQTTOutbound(BlockingConnection connection, String topic) {
            this.connection = connection;
            this.topic = topic;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sitewhere.agent.ISiteWhereEventDispatcher#registerDevice(com.sitewhere.
         * device.communication.protobuf.proto.Sitewhere.SiteWhere.RegisterDevice,
         * java.lang.String)
         */
        @Override
        public void registerDevice(RegisterDevice register, String originator) throws MarsiotAgentException {
            sendMessage(Command.SEND_REGISTRATION, register, originator, "registration");
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sitewhere.agent.ISiteWhereEventDispatcher#acknowledge(com.sitewhere.device
         * .communication.protobuf.proto.Sitewhere.SiteWhere.Acknowledge,
         * java.lang.String)
         */
        @Override
        public void acknowledge(Acknowledge ack, String originator) throws MarsiotAgentException {
            sendMessage(Command.SEND_ACKNOWLEDGEMENT, ack, originator, "ack");
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sitewhere.agent.ISiteWhereEventDispatcher#sendMeasurement(com.sitewhere
         * .device.communication.protobuf.proto.Sitewhere.Model.DeviceMeasurements,
         * java.lang.String)
         */
        @Override
        public void sendMeasurement(Model.DeviceMeasurements measurement, String originator)
                throws MarsiotAgentException {
            sendMessage(Command.SEND_DEVICE_MEASUREMENTS, measurement, originator, "measurement");
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sitewhere.agent.ISiteWhereEventDispatcher#sendLocation(com.sitewhere.device
         * .communication.protobuf.proto.Sitewhere.Model.DeviceLocation, java.lang.String)
         */
        @Override
        public void sendLocation(Model.DeviceLocation location, String originator)
                throws MarsiotAgentException {
            sendMessage(Command.SEND_DEVICE_LOCATION, location, originator, "location");
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * com.sitewhere.agent.ISiteWhereEventDispatcher#sendAlert(com.sitewhere.device
         * .communication.protobuf.proto.Sitewhere.Model.DeviceAlert, java.lang.String)
         */
        @Override
        public void sendAlert(Model.DeviceAlert alert, String originator) throws MarsiotAgentException {
            sendMessage(Command.SEND_DEVICE_ALERT, alert, originator, "alert");
        }

        /**
         * Common logic for sending messages via protocol buffers.
         * 
         * @param command
         * @param message
         * @param originator
         * @param label
         * @throws MarsiotAgentException
         */
        protected void sendMessage(SiteWhere.Command command, AbstractMessageLite message, String originator,
                String label) throws MarsiotAgentException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                SiteWhere.Header.Builder builder = SiteWhere.Header.newBuilder();
                builder.setCommand(command);
                if (originator != null) {
                    builder.setOriginator(originator);
                }
                builder.build().writeDelimitedTo(out);
                message.writeDelimitedTo(out);
                connection.publish(getTopic(), out.toByteArray(), QoS.AT_LEAST_ONCE, false);
            } catch (IOException e) {
                throw new MarsiotAgentException("Problem encoding " + label + " message.", e);
            } catch (Exception e) {
                throw new MarsiotAgentException(e);
            }
        }

        public BlockingConnection getConnection() {
            return connection;
        }

        public void setConnection(BlockingConnection connection) {
            this.connection = connection;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    /**
     * Handles inbound commands. Monitors two topics for messages. One contains SiteWhere
     * system messages and the other contains messages defined in the device
     * specification.
     * 
     * @author Derek
     */
    public static class MQTTInbound implements Runnable {

        /** MQTT connection */
        private BlockingConnection connection;

        /** SiteWhere inbound MQTT topic */
        private String sitewhereTopic;

        /** Command inbound MQTT topic */
        private String commandTopic;

        /** Command processor */
        private IAgentCommandProcessor processor;

        /** Event dispatcher */
        private ISiteWhereEventDispatcher dispatcher;

        public MQTTInbound(BlockingConnection connection, String sitewhereTopic, String commandTopic,
                IAgentCommandProcessor processor, ISiteWhereEventDispatcher dispatcher) {
            this.connection = connection;
            this.sitewhereTopic = sitewhereTopic;
            this.commandTopic = commandTopic;
            this.processor = processor;
            this.dispatcher = dispatcher;
        }

        @Override
        public void run() {
            // Subscribe to chosen topic.
            Topic[] topics = {
                new Topic(getSitewhereTopic(), QoS.AT_LEAST_ONCE),
                new Topic(getCommandTopic(), QoS.AT_LEAST_ONCE) 
            };
            try {
                connection.subscribe(topics);
                LOGGER.info("Started MQTT inbound processing thread.");
                while (true) {
                    try {
                        Message message = connection.receive();
                        message.ack();
                        if (getSitewhereTopic().equals(message.getTopic())) {
                            getProcessor().processSiteWhereCommand(message.getPayload(), getDispatcher());
                        } else if (getCommandTopic().equals(message.getTopic())) {
                            getProcessor().processSpecificationCommand(message.getPayload(), getDispatcher());
                        } else {
                            LOGGER.warning("Message for unknown topic received: " + message.getTopic());
                        }
                    } catch (InterruptedException e) {
                        LOGGER.warning("Device event processor interrupted.");
                        return;
                    } catch (Throwable e) {
                        LOGGER.log(Level.SEVERE, "Exception processing inbound message", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception while attempting to subscribe to inbound topics.", e);
            }
        }

        public BlockingConnection getConnection() {
            return connection;
        }

        public void setConnection(BlockingConnection connection) {
            this.connection = connection;
        }

        public String getSitewhereTopic() {
            return sitewhereTopic;
        }

        public void setSitewhereTopic(String sitewhereTopic) {
            this.sitewhereTopic = sitewhereTopic;
        }

        public String getCommandTopic() {
            return commandTopic;
        }

        public void setCommandTopic(String commandTopic) {
            this.commandTopic = commandTopic;
        }

        public IAgentCommandProcessor getProcessor() {
            return processor;
        }

        public void setProcessor(IAgentCommandProcessor processor) {
            this.processor = processor;
        }

        public ISiteWhereEventDispatcher getDispatcher() {
            return dispatcher;
        }

        public void setDispatcher(ISiteWhereEventDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }
    }

    /**
     * Handles graceful shutdown of agent.
     * 
     * @author Derek
     */
    public class ShutdownHandler extends Thread {
        @Override
        public void run() {
            if (connection != null) {
                try {
                    connection.disconnect();
                    LOGGER.info("Disconnected from MQTT broker.");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception disconnecting from MQTT broker.", e);
                }
            }
        }
    }

    /**
     * Validates the agent configuration.
     * 
     * @return
     */
    public boolean load(Properties properties) {
        if (MarsiotConfig.DEBUG) {
            LOGGER.setLevel(Level.ALL);        
        } else {
            LOGGER.setLevel(Level.OFF);
        }

        LOGGER.info("Validating configuration...");
        // Load command processor class name.
        if (properties != null) {
            setCommandProcessorClassname(properties.getProperty(IAgentConfiguration.COMMAND_PROCESSOR_CLASSNAME));
        }
        if (getCommandProcessorClassname() == null) {
            LOGGER.severe("Command processor class name not specified.");
            return false;
        }
        LOGGER.info("Using configured command processor class: " + getCommandProcessorClassname());

        // Validate hardware id.
        if (properties != null) {
            setHardwareId(properties.getProperty(IAgentConfiguration.DEVICE_HARDWARE_ID));
        }
        if (getHardwareId() == null) {
            LOGGER.severe("Device hardware id not specified in configuration.");
            try {
                setHardwareId(SystemInfo.getSerial());
            } catch (UnsupportedOperationException ex) {
            } catch (Exception ex) {
            }
        }
        LOGGER.info("Using configured device hardware id: " + getHardwareId());

        // Validate site token.
        if (properties != null) {
            setSiteToken(properties.getProperty(IAgentConfiguration.SITE_TOKEN));
        }
        if (getSiteToken() == null) {
            LOGGER.severe("Site token not specified.");
            return false;
        }
        LOGGER.info("Using configured site token: " + getSiteToken());

        if (properties != null) {
            String readModelName = "";
            try {
                readModelName = new String(properties.getProperty(IAgentConfiguration.MODEL_NAME).getBytes("ISO8859-1"),"UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }
            setModelName(readModelName);
        }

        if (properties != null) {
            String readModelDescription = "";
            try {
                readModelDescription = new String(properties.getProperty(IAgentConfiguration.MODEL_DESCRIPTION).getBytes("ISO8859-1"),"UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }
            setModelDescription(readModelDescription);
        }

        // Validate specification token.
        if (properties != null) {
            //limx debug setSpecificationToken(properties.getProperty(IAgentConfiguration.DEVICE_SPECIFICATION_TOKEN));
        }
        if (getSpecificationToken() == null) {
            LOGGER.severe("Device specification token not specified in configuration.");
            setSpecificationToken(DEFAULT_SPECIFICATION_TOKEN);
        }
        LOGGER.info("Using configured device specification token: " + getSpecificationToken());

        // Validate MQTT hostname.
        if (properties != null) {
            //limx debug setMqttHostname(properties.getProperty(IAgentConfiguration.MQTT_HOSTNAME));
        }
        if (getMqttHostname() == null) {
            LOGGER.severe("Mqtt hostname not specified in configuration.");
            setMqttHostname(MarsiotConfig.SERVER);
        }
        LOGGER.info("Using configured hostname: " + getMqttHostname());

        // Validate MQTT port.
        String strPort = null;
        if (properties != null) {
            //limx debug strPort = properties.getProperty(IAgentConfiguration.MQTT_PORT);
        }
        if (strPort != null) {
            try {
                setMqttPort(Integer.parseInt(strPort));
            } catch (NumberFormatException e) {
                if (!MarsiotConfig.TLS) {
                    LOGGER.warning("Non-numeric MQTT port specified, using: " + MQTT_PORT);
                    setMqttPort(MQTT_PORT);
                } else {
                    LOGGER.warning("Non-numeric MQTT port specified, using: " + MQTTS_PORT);
                    setMqttPort(MQTTS_PORT);
                }
            }
        } else {
            if (!MarsiotConfig.TLS) {
                LOGGER.warning("No MQTT port specified, using: " + MQTT_PORT);
                setMqttPort(MQTT_PORT);
            } else {
                LOGGER.warning("No MQTT port specified, using: " + MQTTS_PORT);
                setMqttPort(MQTTS_PORT);
            }
        }

        // Validate outbound SiteWhere topic.
        if (properties != null) {
            //limx debug setOutboundSiteWhereTopic(properties.getProperty(IAgentConfiguration.MQTT_OUTBOUND_SITEWHERE_TOPIC));
        }
        if (getOutboundSiteWhereTopic() == null) {
            LOGGER.warning("Using default outbound SiteWhere MQTT topic: " + MarsiotConfig.MQTTPATH);
            setOutboundSiteWhereTopic(MarsiotConfig.MQTTPATH);
        }
        LOGGER.info("Using outbound sitewhere topic: " + getOutboundSiteWhereTopic());

        // Validate inbound SiteWhere topic.
        if (properties != null) {
            setInboundSiteWhereTopic(properties.getProperty(IAgentConfiguration.MQTT_INBOUND_SITEWHERE_TOPIC));
        }
        if (getInboundSiteWhereTopic() == null) {
            String in = calculateInboundSiteWhereTopic();
            LOGGER.warning("Using default inbound SiteWhere MQTT topic: " + in);
            setInboundSiteWhereTopic(in);
        }
        LOGGER.info("Using inbound sitewhere topic: " + getInboundSiteWhereTopic());

        // Validate inbound command topic.
        if (properties != null) {
            setInboundCommandTopic(properties.getProperty(IAgentConfiguration.MQTT_INBOUND_COMMAND_TOPIC));
        }
        if (getInboundCommandTopic() == null) {
            String in = calculateInboundCommandTopic();
            LOGGER.warning("Using default inbound command MQTT topic: " + in);
            setInboundCommandTopic(in);
        }
        LOGGER.info("Using inbound command topic: " + getInboundCommandTopic());

        return true;
    }

    protected String calculateInboundSiteWhereTopic() {
        return "SiteWhere/system/" + getHardwareId();
    }

    protected String calculateInboundCommandTopic() {
        return "SiteWhere/commands/" + getHardwareId();
    }

    public String getCommandProcessorClassname() {
        return commandProcessorClassname;
    }

    public void setCommandProcessorClassname(String commandProcessorClassname) {
        this.commandProcessorClassname = commandProcessorClassname;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getSpecificationToken() {
        return specificationToken;
    }

    public void setSpecificationToken(String specificationToken) {
        this.specificationToken = specificationToken;
    }

    public String getSiteToken() {
        return siteToken;
    }

    public void setSiteToken(String siteToken) {
        this.siteToken = siteToken;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    public String getMqttHostname() {
        return mqttHostname;
    }

    public void setMqttHostname(String mqttHostname) {
        this.mqttHostname = mqttHostname;
    }

    public int getMqttPort() {
        return mqttPort;
    }

    public void setMqttPort(int mqttPort) {
        this.mqttPort = mqttPort;
    }

    public String getOutboundSiteWhereTopic() {
        return outboundSiteWhereTopic;
    }

    public void setOutboundSiteWhereTopic(String outboundSiteWhereTopic) {
        this.outboundSiteWhereTopic = outboundSiteWhereTopic;
    }

    public String getInboundSiteWhereTopic() {
        return inboundSiteWhereTopic;
    }

    public void setInboundSiteWhereTopic(String inboundSiteWhereTopic) {
        this.inboundSiteWhereTopic = inboundSiteWhereTopic;
    }

    public String getInboundCommandTopic() {
        return inboundCommandTopic;
    }

    public void setInboundCommandTopic(String inboundCommandTopic) {
        this.inboundCommandTopic = inboundCommandTopic;
    }
}
