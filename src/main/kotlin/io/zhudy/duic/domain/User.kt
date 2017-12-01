package io.zhudy.duic.domain

import org.joda.time.DateTime
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
@Document(collection = "user")
class User(
        @Id
        var id: String = "",
        @Field
        var username: String = "",
        @Field
        var password: String = "",
        @Field("created_at")
        var createdAt: DateTime? = null,
        @Field("updated_at")
        var updatedAt: DateTime? = null
)