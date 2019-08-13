# Train Track

[Screenshot1]: src/site/markdown/images/Screenshot1.png "Image screenshot1.png"
[Screenshot2]: src/site/markdown/images/Screenshot2.png "Image screenshot2.png"

In June 2019, we <a href="https://hazelcast.com/blog/hazelcast-jet-and-apache-beam/">announced</a> the inclusion of Hazelcast Jet as a runner for <a href="https://beam.apache.org/">Apache Beam</a>.

Now it's time for an example showing how it's done. As a bonus, it's not "<em>Word Count."</em>

![Image of points plotted on a map of Milan, Italy][Screenshot1] 

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

![Image of points plotted on a map west of Milan, Italy][Screenshot2] 
