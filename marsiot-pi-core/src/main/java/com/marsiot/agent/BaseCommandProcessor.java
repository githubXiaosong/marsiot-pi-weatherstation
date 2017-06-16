/*
 * Copyright (c) MARSIOT. All rights reserved. http://www.marsiot.com
 */
package com.marsiot.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sitewhere.spi.device.event.IDeviceEventOriginator;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.Device;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.Device.Header;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.Device.RegistrationAck;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.SiteWhere.RegisterDevice;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.Model;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.SiteWhere;
import com.marsiot.ApiClient;

/**
 * Base class for command processing. Handles processing of inbound SiteWhere system
 * messages. Processing of specification commands is left up to subclasses.
 */
public abstract class BaseCommandProcessor implements IAgentCommandProcessor {

	/** Static logger instance */
	private static final Logger LOGGER = Logger.getLogger(BaseCommandProcessor.class.getName());

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

	/** SiteWhere event dispatcher */
	private ISiteWhereEventDispatcher eventDispatcher;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sitewhere.agent.IAgentCommandProcessor#executeStartupLogic(java.lang.String,
	 * java.lang.String, com.sitewhere.agent.ISiteWhereEventDispatcher)
	 */
	@Override
	public void executeStartupLogic(String hardwareId, String specificationToken,
			ISiteWhereEventDispatcher dispatcher) throws MarsiotAgentException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.agent.IAgentCommandProcessor#processSiteWhereCommand(byte[],
	 * com.sitewhere.agent.ISiteWhereEventDispatcher)
	 */
	@Override
	public void processSiteWhereCommand(byte[] message, ISiteWhereEventDispatcher dispatcher)
			throws MarsiotAgentException {
		ByteArrayInputStream stream = new ByteArrayInputStream(message);
		try {
			Header header = Device.Header.parseDelimitedFrom(stream);
			switch (header.getCommand()) {
			case ACK_REGISTRATION: {
				RegistrationAck ack = RegistrationAck.parseDelimitedFrom(stream);

                switch (ack.getState()) {
                    case NEW_REGISTRATION: 
                    case ALREADY_REGISTERED: {
                        ApiClient.updateDeviceModel(hardwareId, getModelName(), getModelDescription()); 
                        break;
                    }
                    case REGISTRATION_ERROR: {
                        System.out.print("Registered failed\n\n");
                        break;
                    }
                }

				handleRegistrationAck(header, ack);
				break;
			}
			case ACK_DEVICE_STREAM: {
				// TODO: Add device stream support.
				break;
			}
			case RECEIVE_DEVICE_STREAM_DATA: {
				// TODO: Add device stream support.
				break;
			}
			}
		} catch (IOException e) {
			throw new MarsiotAgentException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.agent.IAgentCommandProcessor#processSpecificationCommand(byte[],
	 * com.sitewhere.agent.ISiteWhereEventDispatcher)
	 */
	@Override
	public void processSpecificationCommand(byte[] message, ISiteWhereEventDispatcher dispatcher)
			throws MarsiotAgentException {
		try {
			ByteArrayInputStream encoded = new ByteArrayInputStream(message);
			ObjectInputStream in = new ObjectInputStream(encoded);

			String commandName = (String) in.readObject();
			Object[] parameters = (Object[]) in.readObject();
			Object[] parametersWithOriginator = new Object[parameters.length + 1];
			Class<?>[] types = new Class[parameters.length];
			Class<?>[] typesWithOriginator = new Class[parameters.length + 1];
			int i = 0;
			for (Object parameter : parameters) {
				types[i] = parameter.getClass();
				typesWithOriginator[i] = types[i];
				parametersWithOriginator[i] = parameters[i];
				i++;
			}
			IDeviceEventOriginator originator = (IDeviceEventOriginator) in.readObject();
			typesWithOriginator[i] = IDeviceEventOriginator.class;
			parametersWithOriginator[i] = originator;

			Method method = null;
			try {
				method = getClass().getMethod(commandName, typesWithOriginator);
				method.invoke(this, parametersWithOriginator);
			} catch (NoSuchMethodException e) {
				method = getClass().getMethod(commandName, types);
				method.invoke(this, parameters);
			}
		} catch (StreamCorruptedException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		} catch (NoSuchMethodException e) {
            System.out.print("Method not found! " + e.getMessage() + "\n");
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		} catch (InvocationTargetException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sitewhere.agent.IAgentCommandProcessor#setHardwareId(java.lang.String)
	 */
	public void setHardwareId(String hardwareId) {
		this.hardwareId = hardwareId;
	}

	public String getHardwareId() {
		return hardwareId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sitewhere.agent.IAgentCommandProcessor#setSpecificationToken(java.lang.String)
	 */
	public void setSpecificationToken(String specificationToken) {
		this.specificationToken = specificationToken;
	}

	public String getSpecificationToken() {
		return specificationToken;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sitewhere.agent.IAgentCommandProcessor#getSiteToken(java.lang.String)
	 */
	public void setSiteToken(String siteToken) {
		this.siteToken = siteToken;
	}

	public String getSiteToken() {
		return siteToken;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
	}

	public String getModelDescription() {
		return modelDescription;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sitewhere.agent.IAgentCommandProcessor#setEventDispatcher(com.sitewhere.agent
	 * .ISiteWhereEventDispatcher)
	 */
	public void setEventDispatcher(ISiteWhereEventDispatcher eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	public ISiteWhereEventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	/**
	 * Handle the registration acknowledgement message.
	 * 
	 * @param ack
	 * @param originator
	 */
	public void handleRegistrationAck(Header header, RegistrationAck ack) {
	}

	/**
	 * Convenience method for sending device registration information to SiteWhere.
	 * 
	 * @param hardwareId
	 * @param specificationToken
	 * @throws MarsiotAgentException
	 */
	public void sendRegistration(String hardwareId, String specificationToken) throws MarsiotAgentException {
		RegisterDevice.Builder builder = RegisterDevice.newBuilder();

		Model.Metadata.Builder lb = Model.Metadata.newBuilder();
		lb.setName("test");
		lb.setValue("aaaa");

		//RegisterDevice register = builder.setHardwareId(hardwareId).setSpecificationToken(specificationToken).setSiteToken(siteToken).addMetadata(lb.build()).build();
		RegisterDevice register = builder.setHardwareId(hardwareId).setSpecificationToken(specificationToken).setSiteToken(siteToken).build();

		getEventDispatcher().registerDevice(register, null);
	}

	/**
	 * Convenience method for sending an acknowledgement event to SiteWhere.
	 * 
	 * @param hardwareId
	 * @param message
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void sendAck(String hardwareId, String message, IDeviceEventOriginator originator)
			throws MarsiotAgentException {
		SiteWhere.Acknowledge.Builder builder = SiteWhere.Acknowledge.newBuilder();
		SiteWhere.Acknowledge ack = builder.setHardwareId(hardwareId).setMessage(message).build();
		getEventDispatcher().acknowledge(ack, getOriginatorEventId(originator));
	}

	/**
	 * Convenience method for sending a measurement event to SiteWhere.
	 * 
	 * @param hardwareId
	 * @param name
	 * @param value
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void sendMeasurement(String hardwareId, String name, double value,
			IDeviceEventOriginator originator) throws MarsiotAgentException {
		Model.DeviceMeasurements.Builder mb = Model.DeviceMeasurements.newBuilder();
		mb.setHardwareId(hardwareId).addMeasurement(
				Model.Measurement.newBuilder().setMeasurementId(name).setMeasurementValue(value).build());
		getEventDispatcher().sendMeasurement(mb.build(), getOriginatorEventId(originator));
	}

	/**
	 * Convenience method for sending a location event to SiteWhere.
	 * 
	 * @param hardwareId
	 * @param originator
	 * @param latitude
	 * @param longitude
	 * @param elevation
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void sendLocation(String hardwareId, double latitude, double longitude, double elevation,
			IDeviceEventOriginator originator) throws MarsiotAgentException {
		Model.DeviceLocation.Builder lb = Model.DeviceLocation.newBuilder();
		lb.setHardwareId(hardwareId).setLatitude(latitude).setLongitude(longitude).setElevation(elevation);
		getEventDispatcher().sendLocation(lb.build(), getOriginatorEventId(originator));
	}

	/**
	 * Convenience method for sending an alert event to SiteWhere.
	 * 
	 * @param hardwareId
	 * @param alertType
	 * @param message
	 * @param originator
	 * @throws MarsiotAgentException
	 */
	public void sendAlert(String hardwareId, String alertType, String message,
			IDeviceEventOriginator originator) throws MarsiotAgentException {
		Model.DeviceAlert.Builder ab = Model.DeviceAlert.newBuilder();
		ab.setHardwareId(hardwareId).setAlertType(alertType).setAlertMessage(message);
		getEventDispatcher().sendAlert(ab.build(), getOriginatorEventId(originator));
	}

	/**
	 * Gets event id of the originating command if available.
	 * 
	 * @param originator
	 * @return
	 */
	protected String getOriginatorEventId(IDeviceEventOriginator originator) {
		if (originator == null) {
			return null;
		}
		return originator.getEventId();
	}
}
