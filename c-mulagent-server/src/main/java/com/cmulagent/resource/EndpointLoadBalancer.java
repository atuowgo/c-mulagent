package com.cmulagent.resource;

import java.util.List;

public class EndpointLoadBalancer {

    private final List<String> endpoints;
    private int currentIndex = 0;

    public EndpointLoadBalancer(List<String> endpoints) {
        this.endpoints = endpoints;
    }

    public String next() {
        String endpoint = endpoints.get(currentIndex % endpoints.size());
        currentIndex++;
        return endpoint;
    }
}