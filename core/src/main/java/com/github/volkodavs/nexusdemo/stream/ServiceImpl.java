package com.github.volkodavs.nexusdemo.stream;

import com.github.volkodavs.nexusdemo.api.Service;

public class ServiceImpl implements Service {

    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
