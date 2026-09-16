package com.demo.lightapi;

import com.wang.light.api.bootstrap.Api;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 功能验证：真实公网 API（Open-Meteo 天气，无鉴权）。
 * <p>
 * 校验 current_weather.time 包含当天日期。接口未传 timezone 参数时按 GMT 返回，
 * 因此"当天"以 UTC 口径计算——若用本机时区（UTC+8），北京时间 0~8 点间 UTC 仍是昨天，
 * 用例会在每天清晨出现约 8 小时的误报窗口。
 * 注意：本用例依赖外网连通性。
 */
@SpringBootTest(classes = DemoApp.class)
public class OpenMeteoRealApiTest extends AbstractTestNGSpringContextTests {

    @Test
    public void currentWeather_timeContainsToday() {
        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);

        Api.client("openmeteo").get("/v1/forecast")
                .query("latitude", 39.9)
                .query("longitude", 117.4)
                .query("current_weather", true)
                .execute().assertThat()
                .statusCode(200)
                .jsonPath("$.current_weather.time").contains(today);
    }
}
