Davis Instruments 6357 Vantage Vue Wireless Integrated Sensor Suite (Part #: 06357)
WeatherLinkIP™ Data Logger for Vantage Stations (Part #: 06555)
https://www.davisinstruments.com/support/weather/download/VantageSerialProtocolDocs_v261.pdf

Apache Maven 3.8.5
OpenJDK 18 https://jdk.java.net/18/

mvn versions:display-plugin-updates
mvn versions:display-dependency-updates
mvn clean package

clean and graceful shutdown
pkill -TERM -f wxapp
pkill -INT -f wxapp

immediate / unclean shutdown
pkill -KILL -f wxapp
