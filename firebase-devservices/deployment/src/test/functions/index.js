// The Cloud Functions for Firebase SDK to create Cloud Functions and triggers.
const {logger} = require("firebase-functions");
const {onRequest} = require("firebase-functions/v2/https");

exports.helloworld = onRequest(async (req, res) => {
    logger.log("Received hello world request");
    res.send("Hello world");
});
