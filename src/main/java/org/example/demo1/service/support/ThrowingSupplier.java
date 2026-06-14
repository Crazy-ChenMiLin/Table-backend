package org.example.demo1.service.support;

@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Exception;
}
