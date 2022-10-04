# java-agent
-----------
Metrist agent for Java. It intercepts HTTP calls and sends data to the local Metrist Orchestrator.

#### Bash shell download and verification instructions

The following steps will download and verify the debian package

    sudo apt install wget gnupg
    cd /tmp
    wget -nc http://dist.metrist.io/orchestrator-plugins/java/latest.txt
    wget -nc http://dist.metrist.io/orchestrator-plugins/java/$(cat latest.txt).tar.gz
    wget -nc http://dist.metrist.io/orchestrator-plugins/java/$(cat latest.txt).tar.gz.asc
    wget -nc https://github.com/Metrist-Software/orchestrator/blob/main/dist/trustedkeys.gpg
    gpg --keyring ./trustedkeys.gpg --verify $(cat latest.txt).tar.gz.asc
    # Extract the agent jar file
    tar xzvf $(cat latest.txt).tar.gz

To run the agent

    java -javaagent:java-agent-<version>.jar \
      -jar your_app.jar

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
