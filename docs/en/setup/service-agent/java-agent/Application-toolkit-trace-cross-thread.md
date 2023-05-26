# Trace Cross Thread
These APIs provide ways to continuous tracing in the cross thread scenario with minimal code changes.
All following are sample codes only to demonstrate how to adopt cross thread cases easier.

* Case 1.
```java
    @TraceCrossThread
    public static class MyCallable<String> implements Callable<String> {
        @Override
        public String call() throws Exception {
            return null;
        }
    }
...
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(new MyCallable());
```
* Case 2.
```java
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(CallableWrapper.of(new Callable<String>() {
        @Override public String call() throws Exception {
            return null;
        }
    }));
```
or 
```java
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.execute(RunnableWrapper.of(new Runnable() {
        @Override public void run() {
            //your code
        }
    }));
```
* Case 3.
```java
    @TraceCrossThread
    public class MySupplier<String> implements Supplier<String> {
        @Override
        public String get() {
            return null;
        }
    }
...
    CompletableFuture.supplyAsync(new MySupplier<String>());
```
or 
```java
    CompletableFuture.supplyAsync(SupplierWrapper.of(()->{
            return "SupplierWrapper";
    })).thenAccept(System.out::println);
```
* Case 4.
```java
    CompletableFuture.supplyAsync(SupplierWrapper.of(() -> {
        return "SupplierWrapper";
    })).thenAcceptAsync(ConsumerWrapper.of(c -> {
        // your code visit(url)
        System.out.println("ConsumerWrapper");
    }));
```
or 
```java
    CompletableFuture.supplyAsync(SupplierWrapper.of(() -> {
        return "SupplierWrapper";
    })).thenApplyAsync(FunctionWrapper.of(f -> {
        // your code visit(url)
        return "FunctionWrapper";
    }));
```



