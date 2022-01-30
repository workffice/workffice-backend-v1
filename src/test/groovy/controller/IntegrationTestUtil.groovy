package controller

import com.fasterxml.jackson.databind.ObjectMapper

class IntegrationTestUtil {
    static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj)
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
    }
}
