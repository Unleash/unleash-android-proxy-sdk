package io.getunleash.polling

import io.getunleash.data.ProxyResponse
import io.getunleash.data.Toggle
import kotlinx.serialization.json.Json

object TestReponses {
    val threeToggles = """
        {
            	"toggles": [
                    {
                        "name": "variantToggle",
                        "enabled": true,
                        "variant": {
                            "name": "green",
                            "payload": {
                                "type": "string",
                                "value": "some-text"
                            }
                        }
                    }, {
                        "name": "featureToggle",
                        "enabled": true,
                        "variant": {
                            "name": "disabled"
                        }
                    }, {
                        "name": "simpleToggle",
                        "enabled": true
                    }
                ]
            }
    """.trimIndent()

    val complicatedVariants = """{
            	"toggles": [
                    {
                        "name": "variantToggle",
                        "enabled": true,
                        "variant": {
                            "name": "green",
                            "payload": {
                                "type": "number",
                                "value": 54
                            }
                        }
                    }, {
                        "name": "featureToggle",
                        "enabled": true,
                        "variant": {
                            "name": "disabled"
                        }
                    }, {
                        "name": "simpleToggle",
                        "enabled": true
                        "variant": {
                            "name": "red",
                            "payload": {
                                "type": "json",
                                "value": { "key": "value" }
                            }
                        }
                    }
                ]
            }""".trimIndent()

    fun String.toToggleMap(): Map<String, Toggle> = Json.decodeFromString(ProxyResponse.serializer(), this).toggles.groupBy { it.name }.mapValues { (_, v) -> v.first() }
}