# TradingTablePro 📊

A lightweight, privacy-focused desktop trading journal and analytics dashboard designed for financial market traders. Built completely in Java Swing, this application allows traders to track their setups, log entry signals, and view instant performance metrics locally.

## Features ✨

* **Comprehensive Trade Logging:** Custom drop-down inputs to track currency pairs/indices, chart patterns, wave structures, divergences, support/resistance levels, trade directions, and entry signals.
* **Instant Performance Analytics:** Built-in calculation engine that tracks total trades, win/loss ratios, win rates, and your bias towards bullish or bearish trades.
* **Robust Local Storage:** Automatically manages records using an embedded SQLite database.
* **Transaction Safety:** Implements database transaction rollbacks to protect your data from corruption if an issue occurs mid-save.
* **Modern UI:** Clean, modern Swing interface utilizing an adaptive system look-and-feel, row hover effects, and distinct, color-coded table headers for easy readability.

## Tech Stack 🛠

* **Language:** Java 17+
* **Framework:** Java Swing (AWT UI Utilities)
* **Database:** SQLite (via JDBC Driver)
* **Project Management:** Maven

## Getting Started 🚀

### Prerequisites
* Java Development Kit (JDK) 17 or higher
* Maven installed (or managed through an IDE like IntelliJ IDEA)

### Installation & Run
1. Clone the repository:
   ```bash
   git clone [https://github.com/Solo-Saad/Trading-Table-Pro.git](https://github.com/Solo-Saad/Trading-Table-Pro.git)