package com.alirace.util;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AhoCorasickAutomationTest {

    private static AhoCorasickAutomation aca = new AhoCorasickAutomation();

    @Test
    public void find() {

        // String text = "&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=5&peer.port=9001&http.method=GET";
        // String text = "&component=java-spring-rest-template&span.kind=client&http.url=http://localhost:9001/getItem?id=5&peer.port=9001&http.method=GET";
        String text = "http.status_code=200&component=java-web-servlet&span.kind=server&sampler.type=const&sampler.param=1&http.url=http://localhost:8081/buyItem&http.method=GET&error=1";

        HashMap<String, List<Integer>> result = aca.find(text);

        System.out.println(text);

        for (Map.Entry<String, List<Integer>> entry : result.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
}