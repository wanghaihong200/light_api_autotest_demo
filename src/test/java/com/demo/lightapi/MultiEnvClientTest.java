package com.demo.lightapi;

import com.wang.light.api.bootstrap.Api;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * 功能验证：多环境多客户端（第二套系统独立 base-url，互不干扰）。
 */
@SpringBootTest(classes = DemoApp.class)
public class MultiEnvClientTest extends AbstractTestNGSpringContextTests {

    @Test
    public void secondSystem_viaNamedClient() {
        com.wang.light.api.client.ApiClient other = Api.client("other");
        other.get("/ping").execute().assertThat()
                .statusCode(200)
                .jsonPath("$.data", is("pong"));
    }
}
