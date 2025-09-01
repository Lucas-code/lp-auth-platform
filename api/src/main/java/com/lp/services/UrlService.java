package com.lp.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UrlService {
    @Value("${client.address}")
    private String clientAddress;

    public String getClientUrl() {
        return clientAddress;
    }
}
