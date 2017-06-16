/*
 * Copyright (c) MARSIOT. All rights reserved. http://www.marsiot.com
 */
package com.example;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import com.sitewhere.spi.device.event.IDeviceEventOriginator;
import com.marsiot.agent.BaseCommandProcessor;
import com.marsiot.agent.ISiteWhereEventDispatcher;
import com.marsiot.agent.MarsiotAgentException;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.Device.Header;
import com.marsiot.device.communication.protobuf.proto.Sitewhere.Device.RegistrationAck;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class MyCommandProcessor extends BaseCommandProcessor {

    private String mHardwareId;

    @Override
    public void executeStartupLogic(String hardwareId, String specificationToken,
            ISiteWhereEventDispatcher dispatcher) throws MarsiotAgentException {
        mHardwareId = hardwareId;
        sendRegistration(hardwareId, specificationToken);
    }

    @Override
    public void handleRegistrationAck(Header header, RegistrationAck ack) {
        switch (ack.getState()) {
            case NEW_REGISTRATION: 
            case ALREADY_REGISTERED: {
                System.out.print("Devide("+mHardwareId+") model("+getModelName()+") registered ok!\n\n");
                onRegistrationConfirmed(ack);
                break;
            }
            case REGISTRATION_ERROR: {
                System.out.print("Registered failed\n\n");
                break;
            }
        }
    }

    public void onRegistrationConfirmed(RegistrationAck ack) {
        try {
            sendAlert(getHardwareId(), "MESSAGE", "register ok", null); 
        } catch (MarsiotAgentException e) {
        }
    }

    public void helloWorld(String greeting, Boolean loud, IDeviceEventOriginator originator)
            throws MarsiotAgentException {
        String response = greeting + " World!";
        if (loud) {
            response = response.toUpperCase();
        }

        System.out.print("hello world! greeting:" + greeting + ", load:" + loud + "\n");
        sendAck(getHardwareId(), response, originator);
    }

    public void readTemp(IDeviceEventOriginator originator) throws MarsiotAgentException {
        System.out.print("read temp! \n");

        if (!getModelName().equalsIgnoreCase("YRZC-WEATHER")) {
            try {
                sendAlert(getHardwareId(), "MESSAGE", "method readTemp unsupported!", null); 
            } catch (MarsiotAgentException e) {
            }

            sendAck(getHardwareId(), "Acknowledged.", originator);
            return;
        }

        String cmd = "sudo ./readtemp";
        String loadStringBuffer = "";
        try {
            Process ps = Runtime.getRuntime().exec(cmd);
            loadStringBuffer = loadStreamToString(ps.getInputStream()).trim();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        String humidity = "";
        String temprature = "";
        if (loadStringBuffer.length() > 0) {
            String[] values = loadStringBuffer.split(",");
            humidity = values[0].substring(3);
            temprature = values[1].substring(5);
        }

        cmd = "sudo ./readwind";
        String wind = "";
        try {
            Process ps = Runtime.getRuntime().exec(cmd);
            wind = loadStreamToString(ps.getInputStream()).trim();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("temprature", temprature);
            jsonObject.put("humidity", humidity);
            jsonObject.put("wind", wind);

            sendAlert(getHardwareId(), "MESSAGE", jsonObject.toString(), null); 
        } catch (MarsiotAgentException e) {
        }

        sendAck(getHardwareId(), "Acknowledged.", originator);
    }

    // read an input-stream into a String
    private String loadStreamToString(InputStream in) throws IOException {
        int ptr = 0;
        in = new BufferedInputStream(in);
        StringBuffer buffer = new StringBuffer();
        while( (ptr = in.read()) != -1 ) {
            buffer.append((char)ptr);
        }
        return buffer.toString();
    }


}


