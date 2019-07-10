package cn.micro.lemon;

import cn.micro.lemon.dubbo.ServiceDefinition;

import java.util.concurrent.CompletableFuture;

public interface InvokeProxy {

    void initialize(MicroConfig microConfig);

    Object invoke(ServiceDefinition serviceDefinition);

    CompletableFuture<Object> invokeAsync(ServiceDefinition serviceDefinition);

}
