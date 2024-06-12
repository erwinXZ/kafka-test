package com.example.transforms;

import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkTaskContext;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.common.config.ConfigDef;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This transformation adds a new field to the value of Kafka Connect records.
 */
public class AddFieldTransform<R extends ConnectRecord<R>> implements Transformation<R> {

    private static final Logger log = LoggerFactory.getLogger(AddFieldTransform.class);
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private MongoCollection<Document> mongoCollection;

    /**
     * This method is called for each record and applies the transformation.
     *
     * @param record the input record
     * @return the transformed record
     */
    @Override
    public R apply(R record) {
        // Check if the record value is a Struct
        if (record.value() instanceof Struct) {
            Struct value = (Struct) record.value();
            log.debug("Original value: {}", value);

            // Extract the operation type
            String op = value.getString("op");
            log.debug("Operation: " + op);
            
            if ("c".equals(op) || "u".equals(op) || "r".equals(op)) { // Handle create or update
                // Extract the 'after' field from the envelope
                Struct after = value.getStruct("after");
                if (after != null) {
                    log.debug("After value: {}", after);

                    // Create a new schema with an additional field
                    Schema updatedSchema = createUpdatedSchema(after.schema());

                    log.debug("updatedSchema: {}", updatedSchema);

                    // Create a new Struct with the updated schema and copy existing fields
                    Struct updatedValue = createUpdatedValue(after, updatedSchema);

                    log.debug("Transformed value updatedValue: {}", updatedValue);
                    log.debug("record.topic(): {}", record.topic());
                    log.debug("record.kafkaPartition(): {}", record.kafkaPartition());
                    log.debug("record.keySchema(): {}", record.keySchema());
                    log.debug("record.key(): {}", record.key());
                    log.debug("record.timestamp(): {}", record.timestamp());

                    if ("u".equals(op)) {
                        // Update MongoDB
                        updateMongoDB(updatedValue);
                        return null;
                    }

                    // Return a new record with the updated value
                    return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(),
                            updatedSchema, updatedValue, record.timestamp());
                } else {
                    log.warn("No 'after' field found in the record: {}", record);
                }

            } else if ("d".equals(op)) { // Handle delete
                Struct before = value.getStruct("before");
                if (before != null) {
                    log.debug("Before value: {}", before);

                    // Delete from MongoDB
                    deleteFromMongoDB(before);

                    return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), record.valueSchema(), null, record.timestamp());
                } else {
                    log.warn("No 'before' field found in the record: {}", record);
                }
            }

        }
        return record;
    }

    /**
     * Creates a new schema based on the existing schema with an additional field.
     *
     * @param schema the existing schema
     * @return the updated schema
     */
    private Schema createUpdatedSchema(Schema afterSchema) {
        SchemaBuilder builder = SchemaBuilder.struct();

        // Copy existing fields to the new schema
        for (Field field : afterSchema.fields()) {
            builder.field(field.name(), field.schema());
        }

        // Add a new field to the schema
        builder.field("newField", Schema.STRING_SCHEMA);
        return builder.build();
    }

    /**
     * Creates a new Struct based on the updated schema and copies the existing
     * fields.
     *
     * @param value         the existing Struct
     * @param updatedSchema the updated schema
     * @return the updated Struct
     */
    private Struct createUpdatedValue(Struct after, Schema updatedSchema) {
        Struct updatedValue = new Struct(updatedSchema);

        // Copy existing fields to the new Struct
        for (Field field : after.schema().fields()) {
            updatedValue.put(field.name(), after.get(field));
        }

        // Add a new field value to the new Struct
        updatedValue.put("newField", "newValue");
        return updatedValue;
    }

    private void updateMongoDB(Struct updatedValue) {
        // Extract the value of 'id' from the updatedValue Struct
        Object id = updatedValue.get("id");

        // Convert the updatedValue Struct to a Document
        Document document = structToDocument(updatedValue);

        // Ensure 'id' exists before attempting to update
        if (id != null) {
            // Construct the filter to find the document by 'id'
            Bson filter = Filters.eq("id", id);

            // Perform the update operation with ReplaceOptions for upsert
            mongoCollection.replaceOne(filter, document, new ReplaceOptions().upsert(true));
        } else {
            log.error("Document does not contain 'id' field: {}", updatedValue);
        }
    }

    // Helper method to convert Struct to Document
    private Document structToDocument(Struct struct) {
        Document document = new Document();
        for (Field field : struct.schema().fields()) {
            document.append(field.name(), struct.get(field));
        }
        return document;
    }

    private void deleteFromMongoDB(Struct before) {
        // Extract the value of 'id' from the before Struct
        Object id = before.get("id");

        // Ensure 'id' exists before attempting to delete
        if (id != null) {
            // Construct the filter to find the document by 'id'
            Bson filter = Filters.eq("id", id);

            // Perform the delete operation
            mongoCollection.deleteOne(filter);
        } else {
            log.error("Document does not contain 'id' field: {}", before);
        }
    }

    /**
     * Returns the configuration definition for this transformation.
     *
     * @return the configuration definition
     */
    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    /**
     * Closes the transformation and performs any necessary cleanup.
     */
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Configures the transformation with the given configuration.
     *
     * @param configs the configuration settings
     */
    @Override
    public void configure(Map<String, ?> configs) {
        String connectionString = "mongodb://mongo:mongo123@mongodb:27017/testdb"; // MongoDB connection string
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        mongoClient = MongoClients.create(settings);
        mongoDatabase = mongoClient.getDatabase("testdb"); // Replace with your database name
        mongoCollection = mongoDatabase.getCollection("data_table"); // Replace with your collection name

    }
}