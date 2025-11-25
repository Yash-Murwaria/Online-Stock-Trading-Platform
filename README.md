📈 Online Stock Trading Platform

## Purpose
A compact single-file Java web application implementing the Review-1 requirements:
- Problem understanding & solution design
- Core Java concepts (OOP, interfaces, service layer)
- JDBC integration (H2 embedded DB, PreparedStatement, resource handling)
- Servlets & Web integration (Login + Stocks pages)

## Files
- `JavaWebApp.java` — the single source file. Contains all models, DAOs, services, and servlets.
- H2 DB file will be created in your home directory as `javawebdb.mv.db`.

## Prerequisites
- JDK 11 or newer
- Maven (to fetch dependencies and compile)

## Maven dependencies
You need the following dependencies (declare them in your `pom.xml` if using Maven):
- org.eclipse.jetty:jetty-server
- org.eclipse.jetty:jetty-servlet
- com.h2database:h2

Example `pom.xml` snippet (minimal):

```xml
<project ...>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>javawebapp-single</artifactId>
  <version>1.0.0</version>
  <properties>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>jetty-server</artifactId>
      <version>11.0.15</version>
    </dependency>
    <dependency>
      <groupId>org.eclipse.jetty</groupId>
      <artifactId>jetty-servlet</artifactId>
      <version>11.0.15</version>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <version>2.1.214</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin> <!-- compiler plugin --></plugin>
    </plugins>
  </build>
</project>
