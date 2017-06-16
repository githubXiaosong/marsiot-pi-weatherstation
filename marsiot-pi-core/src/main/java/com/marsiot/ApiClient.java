package com.marsiot;

import com.marsiot.Update;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiClient extends ApiBase {

    public static Update checkVersionOnMarsiot() {
        String newUrl = "";
        try {
            newUrl = "http://www.marsiot.com/app/versionmanager/api/require_firmware";

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("model", "marsiotpi");

            return Update.parseMarsiot(_post(newUrl, params, null));
        } catch (Exception e) {
        }

        return null;
    }

    public static void updateDeviceModel(String hardwareId, String name, String description) {
        String newUrl = "";
        try {
            newUrl = "http://www.marsiot.com/marsiot/index.php?route=api/smartiot/updatedevicemodel&hardwareId="+hardwareId;

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("name", name);
            params.put("description", description);

            _post(newUrl, params, null);
        } catch (Exception e) {
        }
    }

}
