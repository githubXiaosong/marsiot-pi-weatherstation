/*
 * Copyright (c) MARSIOT. All rights reserved. http://www.marsiot.com
 */
package com.marsiot.agent;

public class MarsiotAgentException extends Exception {

	private static final long serialVersionUID = 3351303154000958250L;

	public MarsiotAgentException() {
	}

	public MarsiotAgentException(String message) {
		super(message);
	}

	public MarsiotAgentException(Throwable error) {
		super(error);
	}

	public MarsiotAgentException(String message, Throwable error) {
		super(message, error);
	}
}
