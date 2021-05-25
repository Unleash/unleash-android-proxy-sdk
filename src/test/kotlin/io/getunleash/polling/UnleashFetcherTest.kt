package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnleashFetcherTest {
    private val complicatedVariants = """{
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
                        "enabled": true,
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

    private val actualProxyResponse =
        """{"toggles":[
                {"name":"asdasd","enabled":true,"variant":{"name":"123","payload":{"type":"string","value":"11"},"enabled":true}},
                {"name":"atrxrmnqwe","enabled":true,"variant":{"name":"disabled","enabled":false}},
                {"name":"clint.testToggle","enabled":true,"variant":{"name":"disabled","enabled":false}},
                {"name":"demounleash.cheque","enabled":true,"variant":{"name":"disabled","enabled":false}},
                {"name":"demounleash.inversiones","enabled":true,"variant":{"name":"disabled","enabled":false}},
                {"name":"demounleash.movimientosenriquecidos","enabled":true,"variant":{"name":"disabled","enabled":false}},
                {"name":"EnableAt","enabled":true,"variant":{"name":"disabled","enabled":false}},
                {"name":"Test_release","enabled":true,"variant":{"name":"disabled","enabled":false}},
                {"name":"unleash_android_sdk_demo","enabled":true, "variant":{"name":"HelloNeptune","enabled":true }}
            ]
            }""".trimIndent()

    @Test
    fun `Fetcher uses ETags if present`() {
        val server = MockWebServer()
        val fetcher =
            UnleashFetcher(UnleashConfig(proxyUrl = server.url("/proxy").toString(), clientSecret = "my-secret"))
        val fakeEtagHeader = "etag-1"
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(complicatedVariants)
                .addHeader("ETag", fakeEtagHeader)
                .addHeader("Cache-Control", "private, must-revalidate")
        )
        server.enqueue(MockResponse().setResponseCode(304))

        val response = fetcher.getResponseAsync(UnleashContext.newBuilder().userId("hello").build()).get()
        assertThat(response.isFetched()).isTrue
        assertThat(response.isFailed()).isFalse
        assertThat(response.isNotModified()).isFalse
        val firstRequest = server.takeRequest()
        assertThat(firstRequest.getHeader("If-None-Match")).isNull()


        val cachedResponse = fetcher.getResponseAsync(UnleashContext.newBuilder().userId("hello").build()).get()
        val secondRequest = server.takeRequest()
        assertThat(secondRequest.getHeader("If-None-Match")).isEqualTo(fakeEtagHeader)
        assertThat(cachedResponse.isNotModified()).isTrue
        assertThat(cachedResponse.isFetched()).isFalse
        assertThat(cachedResponse.isFailed()).isFalse

    }

    @Test
    fun `Can handle actual proxy response`() {
        val server = MockWebServer()
        val fetcher =
            UnleashFetcher(UnleashConfig(proxyUrl = server.url("/proxy").toString(), clientSecret = "my-secret"))
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(actualProxyResponse)
        )
        val response = fetcher.getResponseAsync(UnleashContext.newBuilder().userId("hello").build()).get()
        assertThat(response.isFetched()).isTrue
        assertThat(response.isFailed()).isFalse
        assertThat(response.isNotModified()).isFalse
        assertThat(response.config).isNotNull
    }
}