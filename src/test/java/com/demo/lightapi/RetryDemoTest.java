package com.demo.lightapi;

import com.wang.light.api.client.ApiClient;
import com.wang.light.api.retry.ApiRetryAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * 功能验证：重试器（5xx 自动重试，断言失败不重试）。
 */
@SpringBootTest(classes = DemoApp.class)
public class RetryDemoTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private ApiClient apiClient;

    @Test(retryAnalyzer = ApiRetryAnalyzer.class)
    public void flakyEndpoint_retriesOn500() {
        apiClient.get("/flaky").execute().assertThat()
                .statusCode(200)
                .jsonPath("$.data", is("recovered"));
    }
}
