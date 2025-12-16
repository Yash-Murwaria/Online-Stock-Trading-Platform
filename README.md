Online Stock Trading Platform (Single-File Java Web App)
🌟 Overview

This project is a single-file Java Web Application that demonstrates Review-1 + Review-2 concepts using Servlets, JDBC, DAO–Service architecture, and an embedded Jetty server.
It simulates a simple online stock trading platform with authentication, database integration, and layered architecture — all implemented cleanly in one Java file for easy review and execution.
The application is intentionally compact while still showcasing real-world best practices such as connection pooling, password hashing, transactions, and testable service logic.


🚀 Features
The system allows users to:
🔐 Log in using credentials stored in an embedded database
📊 View a list of available stocks
🔄 Perform a transactional stock price update (demo admin action)
🧠 Understand complete request flow:
Servlet → Service → DAO → Database


🎯 Key Learning Objectives Demonstrated
✔ Problem Understanding & Solution Design
Complete, working web application
Clear separation of concerns:
Model → DAO → Service → Servlet
Secure session-based authentication
Authentication filter protecting restricted pages


✔ Core Java Concepts
Object-Oriented Programming (OOP)
Encapsulation using POJO models
Interfaces & abstractions (DataSource, Filter)
Modular design despite single-file constraint


✔ Database Integration (H2 + JDBC)
Embedded H2 database (auto-created on first run)
Schema creation and seed data
PreparedStatement usage (SQL injection prevention)
Proper resource handling using try-with-resources
Transaction management (commit / rollback)
Connection pooling using HikariCP


✔ Web Technologies
Embedded Jetty Server
Servlet-based web flow
Login, logout, and protected stock pages
Minimal HTML rendering directly inside servlets
Authentication filter (AuthFilter)


✔ Security Best Practices
Password hashing using BCrypt
No plaintext password storage
Session invalidation on logout


✔ Review-2 Enhancements (New)
✅ UserService made unit-testable
Constructor-based DAO injection
✅ Authentication logic isolated for testing
✅ Clear comments explaining testing intent
✅ DataSource exposed for test reuse
✅ Cleaner, refactored code with no behavior change


📁 File Structure
File	                               Purpose
JavaWebAppSingleFile.java	           Entire project source code (server, models, DAO, services, servlets, filters)
./data/javawebdb.mv.db	               Auto-generated H2 database file (created on first run)


⚙️ Requirements
Before running the application, ensure you have:
☕ JDK 11 or higher
📦 Maven (for dependency management and execution)


📦 Required Maven Dependencies
Add the following dependencies to your pom.xml:
org.eclipse.jetty:jetty-server
org.eclipse.jetty:jetty-servlet
com.h2database:h2
com.zaxxer:HikariCP
org.mindrot:jbcrypt


🧩 Minimal pom.xml Snippet
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

    <dependency>
      <groupId>com.zaxxer</groupId>
      <artifactId>HikariCP</artifactId>
      <version>5.1.0</version>
    </dependency>

    <dependency>
      <groupId>org.mindrot</groupId>
      <artifactId>jbcrypt</artifactId>
      <version>0.4</version>
    </dependency>
  </dependencies>
</project>

▶️ How to Run
1️⃣ Compile and Run using Maven
mvn compile
mvn exec:java -Dexec.mainClass=JavaWebAppSingleFile

2️⃣ Open in Browser
http://localhost:8080


🔑 Demo Credentials
Username	            Password
student	                student123
reviewer	            reviewer123


🔐 Authentication Details
Session-based authentication
Protected URLs enforced using AuthFilter
Logout properly invalidates session
Passwords stored as BCrypt hashes


🧪 Testing Note (Review-2)
Although this is a single-file application, the service layer is intentionally designed to be unit-testable:
UserService supports DAO injection
Authentication logic is isolated
Can be tested independently without Jetty or database

