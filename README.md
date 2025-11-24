📈 Online Stock Trading Platform

A Java-based stock trading system where Traders can buy/sell stocks, track their portfolio, and receive market updates, while Admins manage users, system settings, and financial data security.

This project is part of the Review-1 Submission and includes MVC architecture, OOP concepts, JDBC, GUI (Swing), multithreading, collections, and UML diagrams.

📂 Project Structure
OnlineStockTradingPlatform/
│
├── src/
│    └── Main.java
│
├── assets/
│    ├── er_diagram.png
│    ├── class_diagram.png
│    ├── sequence_diagram.png
│    ├── screenshots/
│    │      ├── login.png
│    │      ├── admin_dashboard.png
│    │      └── trader_dashboard.png
│
├── database/
│    └── tables.sql
│
└── README.md

⭐ 1. Project Description

The Online Stock Trading Platform allows users to perform stock trades, track real-time portfolio value, view trade history, and receive simulated market updates.
The system supports:
	•	Trader: Buys/sells stocks, views portfolio, receives updates.
	•	Admin: Manages users, updates security settings, monitors system activity.

The project demonstrates end-to-end Java development with GUI, JDBC integration, MVC structure, multithreading, and proper software engineering practices.

⚙️ 2. Features

👨‍💼 Admin
	•	Add / Edit / Delete Users
	•	Secure financial data settings
	•	Update system configurations
	•	Monitor trade activities
	•	Generate reports

💹 Trader
	•	Buy and Sell Stocks
	•	Portfolio value tracking
	•	Real-time market price updates (threaded simulation)
	•	View Trade History
	•	Alerts & Notifications

🧰 System Features
	•	MVC structure (Controller, Service, DAO, Model, UI)
	•	JDBC integration + fallback to in-memory storage
	•	Stock price simulation using a background thread
	•	Swing-based GUI (Login, Admin, Trader Dashboards)
	•	Error handling through custom exceptions
	•	Persistent storage (CSV files + SQL support)

  🔧 3. How to Run the Project

Prerequisites
	•	Java 8+
	•	MySQL (optional)
	•	JDBC connector (mysql-connector-j)
	•	VS Code / IntelliJ / Eclipse

  A. Run Without Database

This project has an auto-fallback mode—if DB connection fails, it uses in-memory Maps + CSV.

Compile:
cd src
javac Main.java
Run: java Main

B. Run With MySQL JDBC
	1.	Create the database: CREATE DATABASE stock_trading;
  2.	Run the provided file: database/tables.sql
	3.	Update DB credentials inside: DBConnection.URL
DBConnection.USER
DBConnection.PASS
	4.	Compile with MySQL connector: javac -cp .:mysql-connector-j.jar Main.java
java -cp .:mysql-connector-j.jar Main

🧱 4. OOP Concepts Used
OOP Concept               Where Used
Abstraction               DAO interfaces, service interfaces
Encapsulation             Private fields in all models + getters/setters
Inheritance               Trader and Admin extend User
Polymorphism              Overridden methods in Dashboard classes
Interfaces                Tradable, Persistable
Custom Exceptions         InvalidTradeException, UserNotFoundException

🗄 5. JDBC Usage

The project uses JDBC to:
	•	Store and retrieve users
	•	Insert and fetch trades
	•	Update stock info
	•	Validate login credentials

If JDBC fails → automatically switches to in-memory storage using HashMaps.

The DAO architecture:
UserDAO ← JDBCUserDAO
StockDAO ← JDBCStockDAO
TradeDAO ← JDBCTradeDAO

🧩 6. ER Diagram
Entities: Users (UserId, Name, Email, Role)
          Stocks (StockId, Name, Price)
          Trades (TradeId, UserId, StockId, Quantity, Type)
          Portfolio (UserId, StockId, Quantity)

📜 7. SQL File
Includes:
	•	User table
	•	Stock table
	•	Trade table
	•	Portfolio table

  🏁 8. Conclusion

This project demonstrates:

✔ Complete Java application development
✔ OOP + MVC + DAO + JDBC
✔ Functional GUI
✔ Multithreading
✔ Realistic trading workflow
✔ Clean GitHub structure


  
