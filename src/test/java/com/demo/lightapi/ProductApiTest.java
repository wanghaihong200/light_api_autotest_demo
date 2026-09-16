package com.demo.lightapi;

import com.demo.lightapi.model.Product;
import com.demo.lightapi.service.ProductApiService;
import com.wang.light.api.client.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * 功能验证：starter 自动配置、Service 分层、POJO 映射（含中文）、断言助手。
 */
@SpringBootTest(classes = DemoApp.class)
public class ProductApiTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private ProductApiService productApiService;

    @Test
    public void getProduct_mapsPojoWithChinese() {
        Product p = productApiService.getProduct(1);
        assertThat(p.getId()).isEqualTo(1L);
        assertThat(p.getName()).isEqualTo("机械键盘");
        assertThat(p.getPrice()).isEqualTo(199.0);
    }

    @Test
    public void createProduct_roundTrip() {
        Product created = productApiService.createProduct("鼠标", 99.5);
        assertThat(created.getId()).isEqualTo(100L);
    }

    @Test
    public void listProducts_fluentAssertions() {
        ApiResponse resp = productApiService.listProducts();
        resp.assertThat()
                .statusCode(200)
                .jsonPath("$.data[0].name", is("机械键盘"))
                .jsonPath("$.total").isEqualTo(36);
    }
}
