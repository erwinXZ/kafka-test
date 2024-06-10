db = db.getSiblingDB('testdb'); // Create or switch to 'testdb'
db.createCollection('data_table'); // Create 'data_table'
//db.data_table.insertMany([
//    { name: "Document 1", value: 100 },
//    { name: "Document 2", value: 200 },
//    { name: "Document 3", value: 300 }
//]);