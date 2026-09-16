package com.demo.lightapi;

import com.wang.light.api.client.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * 功能验证：业务自定义 Filter（lambda 即实现），为请求统一打标。
 */
@SpringBootTest(classes = DemoApp.class)
public class FilterDemoTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private ApiClient apiClient;

    @Test
    public void businessFilter_tagsEveryRequest() {
        apiClient.addFilter(new com.wang.light.api.spi.Filter() {
            @Override
            public com.wang.light.api.client.ApiRequest beforeRequest(com.wang.light.api.client.ApiRequest request) {
                return request.header("X-Channel", "autotest");
            }
        });
        apiClient.get("/echo").execute().assertThat()
                .statusCode(200)
                .jsonPath("$.data", is("channel-ok"));
    }
}
