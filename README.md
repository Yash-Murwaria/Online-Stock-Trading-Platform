📈 Online Stock Trading Platform

🌟 Overview

This project is a single-file Java Web Application that demonstrates all required Review-1 concepts. It uses Servlets, JDBC, DAO–Service architecture, and an embedded Jetty server to simulate a simple online stock trading system.

The system allows users to:
	•	Log in using credentials stored in an embedded database
	•	View a list of available stocks
	•	Understand complete flow of DAO → Service → Servlet

This project is intentionally compact, easy to run, and designed to showcase clear understanding of web application fundamentals.

⸻

🎯 Key Learning Objectives Demonstrated

✔ Problem Understanding & Solution Design
	•	Small but complete working web application
	•	Clean separation of layers (Model → DAO → Service → Servlets)
	•	Secure session handling for authenticated pages

✔ Core Java Concepts
	•	OOP principles
	•	Interfaces & abstractions
	•	Encapsulation with POJOs
	•	Modular design despite being single-file

✔ Database Integration (H2 + JDBC)
	•	Embedded H2 database (auto-created)
	•	Schema creation + seed data
	•	Prepared statements to avoid SQL injection
	•	Proper resource & connection handling

✔ Web Technologies
	•	Servlet-based web flow
	•	Login, logout, and protected stock pages
	•	Minimal HTML rendering inside servlets

  📁 File Structure
  File                    Purpose
JavaWebApp.java          Entire project source code — server, models, DAOs, services, servlets
~/javawebdb.mv.db        Auto-generated H2 database file (created on first run)

Requirements
Before running the application, ensure you have:
	•	JDK 11+
	•	Maven (for dependency management and running)

📦 Required Maven Dependencies

Add these to your pom.xml:
	•	org.eclipse.jetty:jetty-server
	•	org.eclipse.jetty:jetty-servlet
	•	com.h2database:h2

  Minimal Example pom.xml Snippet
  <project>
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
</project>

▶️ How to Run
	1.	Compile & run with Maven:
  mvn compile
mvn exec:java -Dexec.mainClass=JavaWebApp
2.	Open your browser:
http://localhost:8080
	3.	Demo Credentials
	•	Username: student
	•	Password: student123

  

	•	Session-based authentication

