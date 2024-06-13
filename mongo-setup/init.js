var username = "mongo"; // Replace <username> with your desired username
var password = "mongo123"; // Replace <password> with your desired password

db = db.getSiblingDB('testdb'); // Create or switch to 'testdb'
db.createUser({
    user: username,
    pwd: password,
    roles: [{ role: "readWrite", db: "testdb" }] // Assign the user readWrite role for the testdb database
});
db.createCollection('data_table'); // Create 'data_table'
/* db.createCollection('new_collection'); */

//db.data_table.insertMany([
//    { name: "Document 1", value: 100 },
//    { name: "Document 2", value: 200 },
//    { name: "Document 3", value: 300 }
//]);