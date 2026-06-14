package org.example.demo1.service.support;

import org.example.demo1.logging.RecognitionTimingContext;
import org.springframework.stereotype.Component;

@Component
public class RecognitionStepTimer {

    public <T> T measure(RecognitionTimingContext context, String stageName, ThrowingSupplier<T> supplier) throws Exception {
        context.setCurrentStage(stageName);
        long startNano = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            context.recordStage(stageName + "_ms", elapsedMillis(startNano));
        }
    }

    public void measureVoid(RecognitionTimingContext context, String stageName, ThrowingSupplier<Void> supplier) throws Exception {
        measure(context, stageName, supplier);
    }

    public void recordTotal(RecognitionTimingContext context, String stageName, long startNano) {
        context.recordStage(stageName + "_ms", elapsedMillis(startNano));
    }

    private long elapsedMillis(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }
}
