# ADB-S Flight Telemetry Stream Processing Demo

Reads a ADB-S telemetry stream from [ADB-S Exchange](https://www.adsbexchange.com/) on all commercial aircraft flying anywhere in the world. 
There is typically 5,000 - 6,000 aircraft at any point in time. 
This is then filtered, aggregated and certain features are enriched and displayed in Grafana
 service provides real-time information about flights. 
             

The demo will calculate following metrics and publish them in Grafana
- Filter out planes outside of defined airports
- Sliding over last 1 minute to detect, whether the plane is ascending, descending or staying in the same level 
- Based on the plane type and phase of the flight provides information about maximum noise levels nearby to the airport and estimated C02 emissions for a region


