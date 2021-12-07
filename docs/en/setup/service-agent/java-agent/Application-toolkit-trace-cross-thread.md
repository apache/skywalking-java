# trace cross thread
* Dependency the toolkit, such as using maven or gradle
```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-trace</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```

* usage 1.
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
* usage 2.
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
* usage 3.
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
* usage 4.
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
_Sample codes only_



