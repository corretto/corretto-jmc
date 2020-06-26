## JMC for Corretto

*JMC for Corretto* is a build of [JDK Mission Control (JMC)](https://openjdk.java.net/projects/jmc/) by the Corretto team. It tracks the latest stable JMC release with extra patches to support the usage of the JDK Flight Recorder feature in Corretto releases including:

* Corretto 8.262.y.z or later
* Corretto 11.0.x.y.z or later

Check out the [JMC README.md](src/README.md) for more project details.

To contribute to the JMC project in the OpenJDK community, please refer to the [OpenJDK JMC contribution wiki](https://wiki.openjdk.java.net/display/jmc/Contributing).

### Build JMC for Corretto

#### Prerequisites

* [Corretto-8](https://github.com/corretto/corretto-8/releases) or any OpenJDK8 distributions
* [Maven](https://maven.apache.org/install.html)
* (Optional, used for docker build option) [Docker](https://www.docker.com/products/docker-desktop)

#### Build wrapper

The `build.sh` wrapper script allows you to build JMC for Corretto locally or inside a docker on Linux or macOS. Try `./build.sh help` to explore build options.

When build is completed and successful, the artifacts are placed under the `build` directory.

### Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

### License

This library is licensed under the Universal Permissive License, Version 1.0.

