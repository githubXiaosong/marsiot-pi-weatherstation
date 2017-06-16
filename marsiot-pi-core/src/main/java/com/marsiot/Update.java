package com.marsiot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class Update implements Serializable {

	private List<AppVersionInfo> mVersionList;

	public static class AppVersionInfo {
		public String name;
		public int code;
		public String url;
		public String info;
	};

	public AppVersionInfo getDownloadAppVersionInfo(String appName) {
		AppVersionInfo versionInfo = null;

		for (int i = 0; i < mVersionList.size(); i++) {
			versionInfo = (AppVersionInfo) mVersionList.get(i);
			if (versionInfo != null
					&& versionInfo.name.equalsIgnoreCase(appName)) {
				return versionInfo;
			}
		}

		return versionInfo;
	}

	public static Update parseMarsiot(InputStream inputStream) throws IOException {
		Update update = null;

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		int ch;
		while ((ch = inputStream.read()) != -1) {
			outStream.write(ch);
		}
		byte inData[] = outStream.toByteArray();
		outStream.close();
		String jsonData = new String(inData);

		update = new Update();
		List<AppVersionInfo> versionList = new ArrayList<AppVersionInfo>();

		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			// for (int i = 0; i < jsonArray.length(); i++) {
			// JSONObject jsonObj = (JSONObject) jsonArray.get(i);

			String name = "marsiot";
			JSONObject jsData = jsonObj.getJSONObject("data");
			int code = jsData.getInt("number");
			String url = jsData.getString("link");
			String info = jsData.getString("info");

			AppVersionInfo versionInfo = new AppVersionInfo();
			versionInfo.code = code;
			versionInfo.name = name;
			versionInfo.url = url;
			versionInfo.info = info;
			versionList.add(versionInfo);
			// }
		} catch (JSONException e) {
			System.out.println("Json parse error");
			//e.printStackTrace();
		}

		update.mVersionList = versionList;

		return update;
	}

}




