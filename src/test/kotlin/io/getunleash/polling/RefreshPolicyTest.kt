package io.getunleash.polling

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class RefreshPolicyTest {

    @Test
    fun `can sha256 a string`() {
        val s = RefreshPolicy.sha256("expected")
        assertThat(s).isEqualTo("cea23dd4b87e8b00d19fb9ccaaef93e97353c7353e2070f3baf05aeb3995dff4")
    }
}