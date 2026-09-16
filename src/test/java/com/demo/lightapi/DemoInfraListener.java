package com.demo.lightapi;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * demo 被测服务：双 WireMock 实例模拟两套系统（多客户端演示）。
 */
public class DemoInfraListener implements org.testng.ISuiteListener {

    private static final Logger log = LoggerFactory.getLogger(DemoInfraListener.class);

    private static WireMockServer mainApp;
    private static WireMockServer otherApp;

    @Override
    public void onStart(org.testng.ISuite suite) {
        mainApp = new WireMockServer(18090);
        otherApp = new WireMockServer(18091);
        mainApp.start();
        otherApp.start();
        stubMain();
        stubOther();
        log.info("[demo] WireMock 启动完成: 18090(主系统) / 18091(第二套系统)");
    }

    @Override
    public void onFinish(org.testng.ISuite suite) {
        if (mainApp != null) {
            mainApp.stop();
        }
        if (otherApp != null) {
            otherApp.stop();
        }
    }

    private void stubMain() {
        mainApp.stubFor(post(urlEqualTo("/auth/login"))
                .willReturn(okJson("{\"code\":0,\"data\":{\"token\":\"qa-token-001\"}}")));

        mainApp.stubFor(get(urlEqualTo("/products/1"))
                .willReturn(okJson("{\"code\":0,\"data\":{\"id\":1,\"name\":\"机械键盘\",\"price\":199.0}}")));

        mainApp.stubFor(get(urlEqualTo("/products?page=1"))
                .willReturn(okJson("{\"code\":0,\"data\":[{\"id\":1,\"name\":\"机械键盘\"}],\"total\":36}")));

        mainApp.stubFor(post(urlEqualTo("/products"))
                .willReturn(okJson("{\"code\":0,\"data\":{\"id\":100,\"name\":\"created\",\"price\":9.9}}")));

        mainApp.stubFor(get(urlEqualTo("/secure/info"))
                .withHeader("X-Token", equalTo("qa-token-001"))
                .willReturn(okJson("{\"data\":{\"owner\":\"qa\"}}")));
        mainApp.stubFor(get(urlEqualTo("/secure/info"))
                .atPriority(10)
                .willReturn(aResponse().withStatus(401).withBody("{\"error\":\"unauthorized\"}")));

        mainApp.stubFor(get(urlEqualTo("/echo"))
                .withHeader("X-Channel", equalTo("autotest"))
                .willReturn(okJson("{\"data\":\"channel-ok\"}")));
        mainApp.stubFor(get(urlEqualTo("/echo"))
                .atPriority(10)
                .willReturn(aResponse().withStatus(400).withBody("{\"error\":\"need channel\"}")));

        mainApp.stubFor(get(urlEqualTo("/flaky"))
                .inScenario("flaky")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("FIXED")
                .willReturn(aResponse().withStatus(500).withBody("{\"error\":\"boom\"}")));
        mainApp.stubFor(get(urlEqualTo("/flaky"))
                .inScenario("flaky")
                .whenScenarioStateIs("FIXED")
                .willReturn(okJson("{\"data\":\"recovered\"}")));
    }

    private void stubOther() {
        otherApp.stubFor(get(urlEqualTo("/ping"))
                .willReturn(okJson("{\"data\":\"pong\"}")));
    }
}
