package com.demo.lightapi;

import com.wang.light.api.client.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * 功能验证：配置驱动 token 鉴权（自动登录 + header 注入）。
 */
@SpringBootTest(classes = DemoApp.class)
public class AuthFlowTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private ApiClient apiClient;

    @Test
    public void secureEndpoint_autoLogin() {
        apiClient.get("/secure/info").execute().assertThat()
                .statusCode(200)
                .jsonPath("$.data.owner", is("qa"));
    }
}
