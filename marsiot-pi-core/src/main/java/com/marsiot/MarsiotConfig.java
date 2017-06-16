package com.marsiot;

import java.util.Set;

public class MarsiotConfig  {
    public static final int VERSION = 101;

    public static Boolean DEBUG = false;
    public static String SERVER = "www.marsiot.com";
    public static Boolean TLS = false;
    public static String MQTTPATH = "SiteWhere/input/smartiot";

    public static Set mExcludeSet;
    public static Set mIncludeSet;
}
