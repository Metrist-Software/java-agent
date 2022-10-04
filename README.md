# java-agent
-----------
Metrist agent for Java. It intercepts HTTP calls and sends data to the local Metrist Orchestrator. To run the agent simply download the latest `jar` file of the latest [release](https://github.com/Metrist-Software/java-agent/releases) then attach the agent by running

```bash
java -javaagent:java-agent-<version>.jar \
  -jar your_app.jar
```

The agent uses the following environment variables

* `METRIST_MONITORING_AGENT_HOST` - defaults to `localhost`
* `METRIST_MONITORING_AGENT_PORT` - defaults to `51712`


# Contributing

Requirements: 
* Java 11
* Maven


To run the example app with the agent, run the following 

```bash
# Replace <version> with the project version. Seem pom.xml
(cd example && mvn clean package) &&\
    mvn clean package &&                       
    java -javaagent:target/java-agent-<version>.jar \
        -jar example/target/example_agent_usage-<version>.jar
```