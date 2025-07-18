# SQL Prompter

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.0-green)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-6.0%2B-47A248)](https://www.mongodb.com/)
[![Ollama](https://img.shields.io/badge/Ollama-LLaMA3-FF6B6B)](https://ollama.ai/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📝 Overview

SQL Prompter is an intelligent query assistant that allows users to interact with MongoDB databases using natural language. It leverages OLLAMA with LLaMA3 to convert natural language queries into MongoDB queries and returns results in a user-friendly format.

## ✨ Features

- **Natural Language to Query**: Convert plain English questions into MongoDB queries using LLaMA3
- **Interactive Web UI**: Clean, responsive interface for querying databases
- **Real-time Feedback**: Chat-like interface with typing indicators
- **Tabular Results**: Formatted display of query results
- **Multiple Collections**: Support for querying different MongoDB collections
- **OLLAMA Integration**: Local LLM processing with LLaMA3 model

## 🚀 Quick Start

### Prerequisites

1. **Java Development Kit (JDK 17 or higher)**
   - Download and install from [Oracle JDK](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or use OpenJDK
   - Verify installation: `java -version`

2. **Maven (3.6.3 or higher)**
   - Download from [Maven](https://maven.apache.org/download.cgi)
   - Verify installation: `mvn -v`

3. **MongoDB (6.0 or higher)**
   - Download and install from [MongoDB](https://www.mongodb.com/try/download/community)
   - Start MongoDB service: `mongod --dbpath=/path/to/data/db`

4. **OLLAMA with LLaMA3**
   - Install OLLAMA from [ollama.ai](https://ollama.ai/)
   - Pull the LLaMA3 model: 
     ```bash
     ollama pull llama3
     ```
   - **Run OLLAMA in interactive mode**:
     ```bash
     ollama run llama3
     ```
     This will start an interactive chat with the model. Test it by typing a message and pressing Enter.
   
   - **Run OLLAMA as a server** (required for the application):
     ```bash
     # Start the OLLAMA server (runs on port 11434 by default)
     ollama serve
     
     # In a new terminal, verify it's running:
     curl http://localhost:11434/api/tags
     ```
   
   - **Common OLLAMA commands**:
     ```bash
     # List available models
     ollama list
     
     # Remove a model
     ollama rm llama3
     
     # Get information about a model
     ollama show llama3
     ```

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/sql-prompter.git
   cd sql-prompter/sql-prompter
   ```

2. **Build the application**
   ```bash
   mvn clean install
   ```

3. **Configure the application**
   ```bash
   # Copy the example configuration
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   
   Edit `application.properties` if you need to change default ports or database names.

4. **Start the components**
   In separate terminal windows:
   
   - **Terminal 1**: Start MongoDB
     ```bash
     mongod --dbpath=/path/to/your/mongodb/data
     ```
   
   - **Terminal 2**: Start OLLAMA
     ```bash
     ollama serve
     ```
   
   - **Terminal 3**: Start the application
     ```bash
     mvn spring-boot:run
     ```

5. **Access the application**
   Open your browser and navigate to: `http://localhost:8080`

## 🖥️ User Interface

The application features a clean, interactive web interface with the following components:

1. **Collection Selector**: Dropdown to choose which MongoDB collection to query
2. **Chat Interface**:
   - Message input field for natural language queries
   - Send button to submit queries
   - Message history showing both user queries and system responses
   - Typing indicators during query processing
3. **Results Display**:
   - Tabular format for query results
   - Pagination for large result sets
   - Error messages for invalid queries

## 🗄️ Database Information

The application uses MongoDB with the following default configuration:
- **Database Name**: `employee_db`
- **Port**: `27017`
- **Collections**:
  - `employees`: Stores employee records
  - `departments`: Department information
  - `projects`: Project details

### Sample Data

Sample data is automatically loaded on application startup. You can also manually seed the database using:

```bash
# Using the provided seed script
mvn spring-boot:run -Dspring-boot.run.arguments=--app.seed-data=true
```

## 🤖 OLLAMA Integration

The application uses OLLAMA with LLaMA3 for natural language processing:

- **Base URL**: `http://localhost:11434`
- **Model**: `llama3`
- **API Endpoint**: `POST /api/generate`

### How it works:
1. User submits a natural language query
2. The application sends the query to OLLAMA's LLaMA3 model
3. LLaMA3 converts the query into a MongoDB query
4. The application executes the query against the database
5. Results are formatted and displayed to the user

### Testing OLLAMA Integration

To verify OLLAMA is working correctly:

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "llama3",
  "prompt": "Translate to MongoDB query: Find all employees in the IT department",
  "stream": false
}'
```

## 🛠️ Configuration

Configure your MongoDB connection in `application.properties`:

```properties
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=your_database
```

## 🧩 API Endpoints

### Process Natural Language Query
- **URL**: `/api/query/nlq`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "query": "Show me all users from New York",
    "collection": "users"
  }
  ```
- **Success Response** (200 OK):
  ```json
  {
    "status": "success",
    "data": [...],
    "metadata": {
      "executionTime": "150ms",
      "resultCount": 42
    }
  }
  ```

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/responsive/ai/sql_prompter/
│   │   ├── config/         # Configuration classes
│   │   ├── controller/     # REST controllers
│   │   ├── model/          # Data models
│   │   ├── repository/     # Data access layer
│   │   ├── service/        # Business logic
│   │   └── exception/      # Custom exceptions
│   └── resources/
│       ├── static/         # Frontend assets
│       └── application.properties
└── test/                   # Test files
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- Frontend powered by [Bootstrap 5](https://getbootstrap.com/)
- Icons by [Font Awesome](https://fontawesome.com/)
