/*
 * Copyright (c) MARSIOT. All rights reserved. http://www.marsiot.com
 */
package com.marsiot.agent;

import com.marsiot.device.communication.protobuf.proto.Sitewhere.Model;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.SiteWhere;

/**
 * Interface for events that can be dispatched to SiteWhere server.
 */
public interface ISiteWhereEventDispatcher {

	/**
	 * Register a device.
	 * 
	 * @param register
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void registerDevice(SiteWhere.RegisterDevice register, String originator)
			throws MarsiotAgentException;

	/**
	 * Send an acknowledgement message.
	 * 
	 * @param ack
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void acknowledge(SiteWhere.Acknowledge ack, String originator) throws MarsiotAgentException;

	/**
	 * Send a measurement event.
	 * 
	 * @param measurement
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void sendMeasurement(Model.DeviceMeasurements measurement, String originator)
			throws MarsiotAgentException;

	/**
	 * Send a location event.
	 * 
	 * @param location
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void sendLocation(Model.DeviceLocation location, String originator) throws MarsiotAgentException;

	/**
	 * Send an alert event.
	 * 
	 * @param alert
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void sendAlert(Model.DeviceAlert alert, String originator) throws MarsiotAgentException;
}
