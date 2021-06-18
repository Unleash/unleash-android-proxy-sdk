package io.getunleash.errors

class ServerException(statusCode: Int) : Exception("Unleash responsded with $statusCode") {
}