package com.marsiot;


import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Random;
import java.io.File;

public class ApiBase {
    public static final String UTF_8 = "UTF-8";
    public static final String DESC = "descend";
    public static final String ASC = "ascend";

    private final static int TIMEOUT_CONNECTION = 20000;
    private final static int TIMEOUT_SOCKET = 20000;
    private final static int RETRY_TIME = 3;
    private final static String HOST = "www.marsiot.com";

    private static String appCookie;
    private static String appUserAgent;

    public static void cleanCookie() {
        appCookie = "";
    }

    private static String getCookie() {
        return appCookie;
    }

    private static String getUserAgent() {
        if (appUserAgent == null || appUserAgent == "") {
            StringBuilder ua = new StringBuilder(HOST);
            appUserAgent = ua.toString();
        }
        return appUserAgent;
    }


    //printf "apiuser:Dazhangwei1234"|base64
    public static String getAdminAuthorization() {
        return "Basic YWRtaW46UGlhb2ppbmh1aTEyMzQ=";
    }

    private static String getSitewhereTenant() {
        return "sitewhere1234567890";
    }

    private static HttpClient getHttpClient() {
        HttpClient httpClient = new HttpClient();

        httpClient.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);

        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler());

        httpClient.getHttpConnectionManager().getParams()
                .setConnectionTimeout(TIMEOUT_CONNECTION);

        httpClient.getHttpConnectionManager().getParams()
                .setSoTimeout(TIMEOUT_SOCKET);

        httpClient.getParams().setContentCharset(UTF_8);
        return httpClient;
    }

    private static GetMethod getHttpGet(String url, String cookie,
                                        String userAgent) {
        GetMethod httpGet = new GetMethod(url);

        httpGet.getParams().setSoTimeout(TIMEOUT_SOCKET);
        httpGet.setRequestHeader("Host", HOST);
        httpGet.setRequestHeader("Connection", "Keep-Alive");
        httpGet.setRequestHeader("Cookie", cookie);
        httpGet.setRequestHeader("User-Agent", userAgent);
        return httpGet;
    }

    private static GetMethod getHttpGetNew(String url, String cookie,
                                           String userAgent, String authorication, String tenant) {
        GetMethod httpGet = new GetMethod(url);

        httpGet.getParams().setSoTimeout(TIMEOUT_SOCKET);
        httpGet.setRequestHeader("Host", "HOST");
        httpGet.setRequestHeader("Connection", "Keep-Alive");
        httpGet.setRequestHeader("Cookie", cookie);
        httpGet.setRequestHeader("User-Agent", userAgent);
        httpGet.setRequestHeader("Authorization", authorication);
        httpGet.setRequestHeader("X-SiteWhere-Tenant", tenant);
        return httpGet;
    }

    private static PostMethod getHttpPost(String url, String cookie,
                                          String userAgent) {
        PostMethod httpPost = new PostMethod(url);

        httpPost.getParams().setSoTimeout(TIMEOUT_SOCKET);
        httpPost.setRequestHeader("Host", HOST);
        httpPost.setRequestHeader("Connection", "Keep-Alive");
        httpPost.setRequestHeader("Cookie", cookie);
        httpPost.setRequestHeader("User-Agent", userAgent);
        return httpPost;
    }

    private static PostMethod getHttpPostNew(String url, String cookie,
                                             String userAgent, String authorication, String tenant) {
        PostMethod httpPost = new PostMethod(url);

        httpPost.getParams().setSoTimeout(TIMEOUT_SOCKET);
        httpPost.setRequestHeader("Host", HOST);
        httpPost.setRequestHeader("Connection", "Keep-Alive");
        httpPost.setRequestHeader("Cookie", cookie);
        httpPost.setRequestHeader("User-Agent", userAgent);
        httpPost.setRequestHeader("Authorization", authorication);
        httpPost.setRequestHeader("X-SiteWhere-Tenant", tenant);
        return httpPost;
    }

    public static String _MakeURL(String p_url, Map<String, Object> params) {
        StringBuilder url = new StringBuilder(p_url);
        if (url.indexOf("?") < 0)
            url.append('?');

        for (String name : params.keySet()) {
            url.append('&');
            url.append(name);
            url.append('=');
            url.append(String.valueOf(params.get(name)));

            // not do URLEncoder here
            // url.append(URLEncoder.encode(String.valueOf(params.get(name)),
            // UTF_8));
        }

        return url.toString().replace("?&", "?");
    }

    public static InputStream http_get(String url) {
        String cookie = getCookie();
        String userAgent = getUserAgent();

        HttpClient httpClient = null;
        GetMethod httpGet = null;

        String responseBody = "";
        int time = 0;
        do {
            try {
                httpClient = getHttpClient();
                httpGet = getHttpGet(url, cookie, userAgent);
                int statusCode = httpClient.executeMethod(httpGet);
                if (statusCode != HttpStatus.SC_OK) {
                    //throw AppException.http(statusCode);
                }
                // limx debug responseBody = httpGet.getResponseBodyAsString();
                InputStream inputStream = httpGet.getResponseBodyAsStream();
                if (inputStream == null) {
                    break;
                }
                responseBody = getData(inputStream);

                break;
            } catch (HttpException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }

                e.printStackTrace();
                //throw AppException.http(e);
            } catch (IOException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }

                e.printStackTrace();
                //throw AppException.network(e);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                httpGet.releaseConnection();
                httpClient = null;
            }
        } while (time < RETRY_TIME);

        responseBody = responseBody.replaceAll("\\p{Cntrl}", "");
        return new ByteArrayInputStream(responseBody.getBytes());
    }

    public static InputStream http_getNew(String url, String authorization) {
        String cookie = getCookie();
        String userAgent = getUserAgent();
        String tenant = getSitewhereTenant();

        HttpClient httpClient = null;
        GetMethod httpGet = null;

        String responseBody = "";
        int time = 0;
        do {
            try {
                httpClient = getHttpClient();
                httpGet = getHttpGetNew(url, cookie, userAgent, authorization, tenant);
                int statusCode = httpClient.executeMethod(httpGet);
                if (statusCode != HttpStatus.SC_OK) {
                    //throw AppException.http(statusCode);
                }
                // limx debug responseBody = httpGet.getResponseBodyAsString();
                InputStream inputStream = httpGet.getResponseBodyAsStream();
                if (inputStream == null) {
                    break;
                }
                responseBody = getData(inputStream);

                break;
            } catch (HttpException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }

                e.printStackTrace();
                //throw AppException.http(e);
            } catch (IOException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }
                e.printStackTrace();
                //throw AppException.network(e);
            } catch (java.lang.Exception e) {
                e.printStackTrace();
                //throw AppException.network(e);
            } finally {
                httpGet.releaseConnection();
                httpClient = null;
            }
        } while (time < RETRY_TIME);

        responseBody = responseBody.replaceAll("\\p{Cntrl}", "");
        return new ByteArrayInputStream(responseBody.getBytes());
    }

    private static String getData(InputStream inputStream) throws Exception {
        String data = "";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int len = -1;
        byte[] buff = new byte[1024];
        try {
            while ((len = inputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, len);
            }
            byte[] bytes = outputStream.toByteArray();
            data = new String(bytes);
        } catch (IOException e) {
            throw new Exception(e.getMessage(), e);
        } finally {
            outputStream.close();
        }
        return data;
    }


    protected static InputStream _postNew(String url, String jsonData) {
        String cookie = getCookie();
        String userAgent = getUserAgent();
        String authorication = getAdminAuthorization();
        String tenant = getSitewhereTenant();

        HttpClient httpClient = null;
        PostMethod httpPost = null;

		/*
        int length = (params == null ? 0 : params.size())
				+ (files == null ? 0 : files.size());
		Part[] parts = new Part[length];
		int i = 0;
		if (params != null)
			for (String name : params.keySet()) {
				parts[i++] = new StringPart(name, String.valueOf(params
						.get(name)), UTF_8);
			}




		if (files != null)
			for (String file : files.keySet()) {
				try {
					parts[i++] = new FilePart(file, files.get(file));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

		*/

        StringRequestEntity jsonDataEntity = null;
        try {
            jsonDataEntity = new StringRequestEntity(jsonData, "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        String responseBody = "";
        int time = 0;
        do {
            try {
                httpClient = getHttpClient();
                httpPost = getHttpPostNew(url, cookie, userAgent, authorication, tenant);

                httpPost.setRequestEntity(jsonDataEntity);
                int statusCode = httpClient.executeMethod(httpPost);
                // if (statusCode != HttpStatus.SC_OK) {
                // throw AppException.http(statusCode);
                // } else
                if (statusCode == HttpStatus.SC_OK || statusCode == 500) {

                    Cookie[] cookies = httpClient.getState().getCookies();
                    String tmpcookies = "";
                    for (Cookie ck : cookies) {
                        tmpcookies += ck.toString() + ";";
                    }
                    if (tmpcookies != "") {
                        appCookie = tmpcookies;
                    }
                }
                // limx debug responseBody = httpPost.getResponseBodyAsString();
                InputStream inputStream = httpPost.getResponseBodyAsStream();
                responseBody = getData(inputStream);
                break;
            } catch (HttpException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }

                // e.printStackTrace();
                //throw AppException.http(e);
            } catch (IOException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }

                // e.printStackTrace();
                //throw AppException.network(e);
            } catch (Exception e) {
                // e.printStackTrace();
            } finally {
                httpPost.releaseConnection();
                httpClient = null;
            }
        } while (time < RETRY_TIME);

        responseBody = responseBody.replaceAll("\\p{Cntrl}", "");
        return new ByteArrayInputStream(responseBody.getBytes());
    }


    protected static InputStream _post(String url,Map<String, Object> params, Map<String, File> files) {
        String cookie = getCookie();
        String userAgent = getUserAgent();

        HttpClient httpClient = null;
        PostMethod httpPost = null;

        int length = (params == null ? 0 : params.size())
                + (files == null ? 0 : files.size());
        Part[] parts = new Part[length];
        int i = 0;
        if (params != null)
            for (String name : params.keySet()) {
                parts[i++] = new StringPart(name, String.valueOf(params
                        .get(name)), UTF_8);
            }
        if (files != null)
            for (String file : files.keySet()) {
                try {
                    parts[i++] = new FilePart(file, files.get(file));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        String responseBody = "";
        int time = 0;
        do {
            try {
                httpClient = getHttpClient();
                httpPost = getHttpPost(url, cookie, userAgent);
                httpPost.setRequestEntity(new MultipartRequestEntity(parts,
                        httpPost.getParams()));
                int statusCode = httpClient.executeMethod(httpPost);
                // if (statusCode != HttpStatus.SC_OK) {
                // throw AppException.http(statusCode);
                // } else
                if (statusCode == HttpStatus.SC_OK || statusCode == 500) {

                    Cookie[] cookies = httpClient.getState().getCookies();
                    String tmpcookies = "";
                    for (Cookie ck : cookies) {
                        tmpcookies += ck.toString() + ";";
                    }
                    if (tmpcookies != "") {
                        appCookie = tmpcookies;
                    }
                }
                // limx debug responseBody = httpPost.getResponseBodyAsString();
                InputStream inputStream = httpPost.getResponseBodyAsStream();
                responseBody = getData(inputStream);
                break;
            } catch (HttpException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }

                // e.printStackTrace();
                //throw AppException.http(e);
            } catch (IOException e) {
                time++;
                if (time < RETRY_TIME) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                }

                // e.printStackTrace();
                //throw AppException.network(e);
            } catch (Exception e) {
                // e.printStackTrace();
            } finally {
                httpPost.releaseConnection();
                httpClient = null;
            }
        } while (time < RETRY_TIME);

        responseBody = responseBody.replaceAll("\\p{Cntrl}", "");
        return new ByteArrayInputStream(responseBody.getBytes());
    }


}



