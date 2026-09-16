package com.demo.lightapi;

import com.wang.light.api.bootstrap.Api;
import com.wang.light.api.client.ApiResponse;
import com.wang.light.api.db.Db;
import org.assertj.core.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

/**
 * 功能验证：DB 数据驱动的真实登录接口测试。
 * <p>
 * 数据源：huxuebao.tester_datasource_demo（category=登录场景），
 * 每行驱动一次 POST /api/auth/login，断言登录成功且返回的 user 字段
 * 与 test_platform.users 表中同名账号一致。
 * <p>
 * 已知数据漂移：smokeviewer 行的密码(qaz12340)与实际账号不符，登录返回 401，
 * 该行将失败——经确认保持现状，作为数据漂移的可见提醒。
 * 注意：本用例依赖 127.0.0.1:8000 服务与 MySQL(3307) 可用。
 */
@SpringBootTest(classes = DemoApp.class)
public class LoginDataDrivenTest extends AbstractTestNGSpringContextTests {

    @DataProvider(name = "loginScenarios")
    public Object[][] loginScenarios() {
        List<Map<String, Object>> rows = Db.query(
                "SELECT username, password FROM huxuebao.tester_datasource_demo"
                        + " WHERE category = '登录场景'");
        Object[][] data = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            data[i] = new Object[]{r.get("username"), r.get("password")};
        }
        return data;
    }

    @Test(dataProvider = "loginScenarios")
    public void login_userFieldsMatchUsersTable(String username, String password) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("username", username);
        body.put("password", password);

        ApiResponse resp = Api.client("usercenter").post("/api/auth/login")
                .header("Content-Type", "application/json")
                .body(body)
                .execute();

        resp.assertThat()
                .statusCode(200)
                .jsonPath("$.user.username", is(username));

        String token = String.valueOf(resp.assertThat().jsonPath("$.token").value());
        Assertions.assertThat(token).as("token 应非空").isNotBlank();

        Map<String, Object> dbUser = Db.queryOne(
                "SELECT id, username, display_name, is_admin, is_active"
                        + " FROM test_platform.users WHERE username = ?", username);
        Assertions.assertThat(dbUser).as("users 表应存在账号 " + username).isNotNull();

        resp.assertThat()
                .jsonPath("$.user.id", is(((Number) dbUser.get("id")).intValue()))
                .jsonPath("$.user.username", is(String.valueOf(dbUser.get("username"))))
                .jsonPath("$.user.display_name", is(String.valueOf(dbUser.get("display_name"))))
                .jsonPath("$.user.is_admin", is(asBoolean(dbUser.get("is_admin"))))
                .jsonPath("$.user.is_active", is(asBoolean(dbUser.get("is_active"))));
    }

    /** JDBC 对 tinyint(1) 可能返回 Boolean 或 Integer，统一转 Boolean 再比对 */
    private static Boolean asBoolean(Object v) {
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }
}
