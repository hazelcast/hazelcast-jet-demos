# Train Track

## TODO

Beam job name is visible on Management Center on Beam 2.15 onwards. Upgrade `pom.xml` once 2.15 released.

Check browser compatibility. Works on Firefox. Fails on Safari. Works on Chrome. Internet Explorer unknown.


## Instructions

```
mvn clean install
```

Then from *same* directory

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
