/*
 * Copyright (c) MARSIOT. All rights reserved. http://www.marsiot.com
 */
package com.marsiot.agent;

/**
 * Constants for agent configuration properties.
 */
public interface IAgentConfiguration {

    /** Property for command processor classname */
    public static final String COMMAND_PROCESSOR_CLASSNAME = "command.processor.classname";

    /** Property for device unique hardware id */
    public static final String DEVICE_HARDWARE_ID = "device.hardware.id";

    /** Property for device specification token */
    public static final String DEVICE_SPECIFICATION_TOKEN = "device.specification.token";

    /** Property for MQTT hostname */
    public static final String MQTT_HOSTNAME = "mqtt.hostname";

    /** Property for MQTT port */
    public static final String MQTT_PORT = "mqtt.port";

    /** Property for outbound SiteWhere MQTT topic */
    public static final String MQTT_OUTBOUND_SITEWHERE_TOPIC = "mqtt.outbound.sitewhere.topic";

    /** Property for inbound SiteWhere MQTT topic */
    public static final String MQTT_INBOUND_SITEWHERE_TOPIC = "mqtt.inbound.sitewhere.topic";

    /** Property for inbound command MQTT topic */
    public static final String MQTT_INBOUND_COMMAND_TOPIC = "mqtt.inbound.command.topic";

    /** Property for site token */
    public static final String SITE_TOKEN = "site.token";

    /** model name */
    public static final String MODEL_NAME = "model.name";

    /** model description */
    public static final String MODEL_DESCRIPTION = "model.description";
}
