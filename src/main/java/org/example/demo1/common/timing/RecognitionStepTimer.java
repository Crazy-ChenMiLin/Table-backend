package org.example.demo1.common.timing;

import org.example.demo1.common.context.RecognitionTimingContext;
import org.springframework.stereotype.Component;

/**
 *非侵入性：业务代码完全不需要关心计时的逻辑，只需要把自己的代码用 () -> { ... } 包起来扔给 measure 方法即可。
 */
@Component
public class RecognitionStepTimer {

    public <T> T measure(RecognitionTimingContext context, String stageName, ThrowingSupplier<T> supplier) throws Exception {
        context.setCurrentStage(stageName);
         // 1. 掐表开始
        long startNano = System.nanoTime();
        try {
        // 2. 接口执行
            return supplier.get();
        } finally {
        // 3. 无论成功失败，记录耗时，存入ctx里面
            context.recordStage(stageName + "_ms", elapsedMillis(startNano));
        }
    }

    public void measureVoid(RecognitionTimingContext context, String stageName, ThrowingSupplier<Void> supplier) throws Exception {
        measure(context, stageName, supplier);
    }

    public void recordTotal(RecognitionTimingContext context, String stageName, long startNano) {
        context.recordStage(stageName + "_ms", elapsedMillis(startNano));
    }
    //计算时间差，记录耗时
    private long elapsedMillis(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }
}
