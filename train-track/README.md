# Train Track

XXX TODO - Retest all on 3.1, IMDG 3.12.1

XXX TODO - Add test of Beam Pipeline

XXX TODO - Change Beam from 2.14.0-SNAPSHOT to 2.14.0 

XXX TODO - Check beam job name in MC

XXX TODO - Add speed to GPS stream -- with BEAM!

XXX TODO - Beam continuous write/read

XXX Instructions

```
mvn clean install
```

Then from same directory

```
java -jar train-track-grid/target/train-track-grid.jar
java -jar train-track-web-ui/target/train-track-web-ui.jar
java -jar train-track-data-feed/target/train-track-data-feed.jar
java -jar train-track-beam-runner/target/train-track-beam-runner.jar
```

Then

```
http://localhost:8085/
```
