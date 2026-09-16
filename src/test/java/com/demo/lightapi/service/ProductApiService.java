package com.demo.lightapi.service;

import com.demo.lightapi.model.Product;
import com.wang.light.api.client.ApiResponse;
import com.wang.light.api.service.BaseApiService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口定义代码化：一个接口一个方法，业务工程推荐分层。
 */
@Component
public class ProductApiService extends BaseApiService {

    public ProductApiService(com.wang.light.api.client.ApiClient client) {
        super(client);
    }

    public Product getProduct(long id) {
        return get("/products/" + id).execute().as("$.data", Product.class);
    }

    public Product createProduct(String name, double price) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        body.put("price", price);
        return post("/products").body(body).execute().as("$.data", Product.class);
    }

    public ApiResponse listProducts() {
        return get("/products").query("page", 1).execute();
    }
}
