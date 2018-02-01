# Cryptocurrency Realtime Trend
Twitter and Reddit content are analyzed in real time to calculate cryptocurrency
trend list with its popularity index.

NLP sentimental analysis applied to tweets and posts to calculate the content scores.  

![](./diagram.png)

## Prerequisites

You'll need to have API Credentials from Twitter and Reddit to make this demo work.

To obtain them, visit the following websites:
- [Twitter Application Management](http://apps.twitter.com/)
- [Reddit Application Preferences](https://www.reddit.com/prefs/apps)


Please fill in the Twitter and Reddit credentials into these files below.

`src/main/resources/reddit-security.properties`

`src/main/resources/twitter-security.properties`


## Building the Application

To build and package the application, run:

>Please note that maven may take some time to download all dependencies on the first run since the NLP libraries are hefty in file size.

```bash
mvn clean package
```

## Running the Application

After building the application, navigate to `target` folder by:
```bash
cd target
```

Then run the application with: 
```bash
mvn exec:java -Dexec.mainClass="JetCoinTrend"
```