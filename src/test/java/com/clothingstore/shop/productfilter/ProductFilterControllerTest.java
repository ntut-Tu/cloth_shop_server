package com.clothingstore.shop.productfilter;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductFilterControllerTest {
    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404})
    void attributeAuthorizationErrorsKeepTheApiResponseEnvelope(int statusCode) {
        ProductFilterService service = mock(ProductFilterService.class);
        ProductFilterController controller = new ProductFilterController(service);
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = statusCode == 401 ? null : "vendor-token";
        if (token != null) {
            request.setCookies(new Cookie("token", token));
        }
        Map<String, String> attributes = Map.of("material", "Cotton");
        HttpStatus status = HttpStatus.valueOf(statusCode);
        String reason = "Access denied " + statusCode;
        doThrow(new ResponseStatusException(status, reason))
                .when(service).updateAttributes(token, 7, attributes);

        var response = controller.updateAttributes(request, 7, attributes);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isStatus()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo(reason);
        assertThat(response.getBody().getData()).isNull();
        verify(service).updateAttributes(token, 7, attributes);
    }
}
