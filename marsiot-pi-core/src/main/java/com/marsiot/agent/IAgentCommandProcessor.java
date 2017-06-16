/*
 * Copyright (c) MARSIOT. All rights reserved. http://www.marsiot.com
 */
package com.marsiot.agent;

/**
 * Interface for classes that process commands for an agent.
 */
public interface IAgentCommandProcessor {

	/**
	 * Executes logic that happens before the standard processing loop.
	 * 
	 * @param hardwareId
	 * @param specificationToken
	 * @param dispatcher
	 * @throws MarsiotAgentException
	 */
	public void executeStartupLogic(String hardwareId, String specificationToken,
			ISiteWhereEventDispatcher dispatcher) throws MarsiotAgentException;

	/**
	 * Process a SiteWhere system command.
	 * 
	 * @param message
	 * @param dispatcher
	 * @throws MarsiotAgentException
	 */
	public void processSiteWhereCommand(byte[] message, ISiteWhereEventDispatcher dispatcher)
			throws MarsiotAgentException;

	/**
	 * Process a specification command.
	 * 
	 * @param message
	 * @param dispatcher
	 * @throws MarsiotAgentException
	 */
	public void processSpecificationCommand(byte[] message, ISiteWhereEventDispatcher dispatcher)
			throws MarsiotAgentException;

	/**
	 * Set based on hardware id configured in agent.
	 * 
	 * @param hardwareId
	 * @throws MarsiotAgentException
	 */
	public void setHardwareId(String hardwareId) throws MarsiotAgentException;

	/**
	 * Set based on specification token configured in agent.
	 * 
	 * @param specificationToken
	 * @throws MarsiotAgentException
	 */
	public void setSpecificationToken(String specificationToken) throws MarsiotAgentException;

	/**
	 * Set based on site token configured in agent.
	 * 
	 * @param siteToken
	 * @throws MarsiotAgentException
	 */
	public void setSiteToken(String siteToken) throws MarsiotAgentException;

	/**
	 * Set model name.
	 * 
	 * @param model name
	 * @throws MarsiotAgentException
	 */
	public void setModelName(String modelName) throws MarsiotAgentException;

	/**
	 * Set model description.
	 * 
	 * @param model description
	 * @throws MarsiotAgentException
	 */
	public void setModelDescription(String modelDescription) throws MarsiotAgentException;

	/**
	 * Set the event dispatcher that allows data to be sent back to SiteWhere.
	 * 
	 * @param dispatcher
	 */

	public void setEventDispatcher(ISiteWhereEventDispatcher dispatcher);
}
