package org.example.demo1.common.timing;

@FunctionalInterface
/**
 * 为什么不用 Java 自带的 Supplier？
 * Java 8 自带的 java.util.function.Supplier 的 get() 方法不允许抛出受检异常（Checked Exception）。
 * 但是在我们的链路中，调用 AI 接口、解析 JSON 都会抛出 IOException 等受检异常。如果用自带的 Supplier，你在写 Lambda 表达式（() -> { ... }）时，IDE 会报错，
 * 逼着你在 Lambda 里面写丑陋的 try-catch。
 *
 *一个函数式接口，代表一段“会返回结果，但也可能抛出异常”的代码块
 */
public interface ThrowingSupplier<T> {

    T get() throws Exception;
}
