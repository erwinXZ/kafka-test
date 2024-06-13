# PostgreSQL to MongoDB Replication with Kafka, Kafka Connect, Debezium, and Zookeeper

This setup demonstrates how to replicate data from a PostgreSQL database to a MongoDB database using Kafka, Debezium, and Zookeeper.

## Prerequisites

Ensure you have Docker and Docker Compose installed on your machine.

## Setup

### Step 1: Clone the Repository

Clone this repository to your local machine.

```bash
git clone <repository_url>
cd <repository_directory>
```

### Step 3: Start the Services

Start all the services using Docker Compose:

```bash
docker-compose up -d
```
### Step 4: Configure PostgreSQL Source Connector
Deploy the PostgreSQL Source Connector:

```bash
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
    http://localhost:8083/connectors/ -d @postgres-connector-config.json
```

Deploy the MongoDB Sink Connector:

```bash
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
    http://localhost:8083/connectors/ -d @mongodb-sink-config.json
```

## Testing
### Verify PostgreSQL Connection
Connect to the PostgreSQL database using the following command:

```bash
docker exec -it <postgres_container_id> psql -U postgres -d testdb
```
Check the data in the data_table:

```sql
SELECT * FROM data_table;
```

### Verify MongoDB Connection
Connect to the MongoDB database using the following command:

```bash
docker exec -it <mongo_container_id> mongo -u mongo -p mongo123 --authenticationDatabase admin
Switch to the testdb database:
```

```javascript
use testdb;
```
Check the data in the data_table collection:

```javascript
db.data_table.find().pretty();
```

### Verify Kafka Topics
List the Kafka topics:

```bash
docker exec -it <kafka_container_id> kafka-topics --list --zookeeper zookeeper:2181
```
Check PostgreSQL Source Connector Status
Check the status of the PostgreSQL source connector:

```bash
curl -s http://localhost:8083/connectors/postgres-source/status | jq
```
Check MongoDB Sink Connector Status
Check the status of the MongoDB sink connector:

```bash
curl -s http://localhost:8083/connectors/mongo-sink/status | jq
```

### Additional Commands
To Test Databases Availability
PostgreSQL:

```bash
docker exec -it <postgres_container_id> psql -U postgres -c '\l'
```

MongoDB:

```bash
docker exec -it <mongo_container_id> mongo -u mongo -p mongo123 --authenticationDatabase admin --eval "db.adminCommand('ping')"
```

To Test Kafka Topics
```bash
docker exec -it <kafka_container_id> kafka-topics --describe --topic dbserver1.public.data_table --zookeeper zookeeper:2181
```

To See Connectors Availability
```bash
curl -s http://localhost:8083/connectors | jq
```

## Clean Up
To clean up the environment, stop and remove all containers:

```bash
docker-compose down -v
```

## Custom SMT (Single Message Transform) for Kafka Connect

This project provides a custom Single Message Transform (SMT) for Kafka Connect. The custom SMT `AddFieldTransform` transforms Kafka Connect records by adding, updating, and deleting documents in a MongoDB collection based on changes in a PostgreSQL database.

### Features

- Adds new records to MongoDB when new records are inserted in PostgreSQL.
- Updates existing records in MongoDB when records are updated in PostgreSQL.
- Deletes records from MongoDB when records are deleted in PostgreSQL.
- Handles only the `after` part of the change event from PostgreSQL to MongoDB.

### Prerequisites

- Java 8 or later
- Maven
- Docker and Docker Compose

### 1. Build the project
```sh
mvn clean package
```

### 2. Copy the jar file
Copy the result jar in the target file into the connect-plugins folder