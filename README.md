# marsiot-pi-weatherstation
树莓派监测温湿度/风速数据并传送至MARSIOT平台

http://www.marsiot.com/marsiot/index.php?route=blog/blog&blog_id=17

本文介绍了在树莓派上连接传感器感知温度/湿度/风速，并通过MARSIOT提供的SDK接入MARSIOT物联网平台。

文章中涉及到的传感器以及源码等资源会提供详细说明供爱好者研究学习。


准备传感器和24V直流电源

    1. 温湿度测量传感器 DHT11

    DHT11是一款有已校准数字信号输出的温湿度传感器.

    淘宝上购买不到5元

    2. 风速传感器

    测量风速用，购买时选择输出类型为0～5V电压型输出

    我是在中华传感器天猫店上购买的（非广告），200元左右

    3. PCF8591模数转换模块

    因为树莓派没有ADC，风速仪的0～5V信号需要通过PCF8591转换为数字信号给树莓派处理

    淘宝上购买6元左右

    4. 24V直流电源

    我用的是一台维修用直流可调电源（可调范围0～30V），你也可以选用其他直流电源，只要保证24V直流输出即可



电路连接

    1. DHT11采用单总线协议和树莓派通信

        DATA信号线连接至树莓派的GPIO 07（PIN 07）
        VCC连接树莓派的5V
        GND连接树莓派的GND


    2. PCF8591采用I2C总线协议和树莓派通信

        SDA连接至树莓派SDA1（PIN 03）
        SCL连接至树莓派SCL1（PIN 05）
        VCC连接树莓派的5V
        GND连接树莓派的GND


    3. 风速仪输出信号为0～5V电压模拟信号

        SIGNAL信号线连接至PCF8591的AIN0
        红色正极连接至24V直流电源的输出电压正极
        黑色负极连接至24V直流电源的输出电压负极
        另外注意：黑色负极也同时连接至树莓派的GND


注册MARSIOT平台新用户

    PC浏览器上访问www.marsiot.com/marsiot/admin，用手机号注册新用户


在树莓派上下载编译运行程序，读取数据并接入MARSIOT平台

    1. 下载我放在GITHUB上的代码: https://github.com/marsiot/marsiot-pi-weatherstation.git

    2. cd marsiot-pi-weatherstation

    3. 编译 ./mybuild.sh

    4. 打开config.properties，编辑site.token（登录MARSIOT平台后，查看开发包下载中的site token）

    5. 运行 ./myrun.sh

登录MARSIOT平台查看设备和数据

    1. 查看树莓派设备

    如果设备注册成功，设备列表中会出现相应的设备

    2. 添加读取温度的指令

    点击添加命令按钮

    名称填写readTemp,参数不添加

    3. 向树莓派发送查询指令

    点击设备列表的纸飞机按钮向设备发送readTemp命令

    4. 查看设备收到的消息







