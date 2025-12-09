package com.nugget.collector.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue

@ConfigurationProperties(prefix = "collector.mongo")
class CollectorMongoProperties(
    var logs: MongoDataSourceProperties = MongoDataSourceProperties(),
    var metadata: MongoDataSourceProperties = MongoDataSourceProperties()
) {
    class MongoDataSourceProperties(
        @DefaultValue("mongodb://localhost:27017/logs")
        var uri: String = "mongodb://localhost:27017/logs"
    )
}
