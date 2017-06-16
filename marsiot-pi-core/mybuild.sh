#!/bin/sh

#export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
#download gradle 3 from https://gradle.org/install
#export PATH=$PATH:/opt/gradle-3.3/bin

/opt/gradle-3.3/bin/gradle clean shadowJar --debug
cp ./build/libs/marsiot-pi-core.jar ../libs/
