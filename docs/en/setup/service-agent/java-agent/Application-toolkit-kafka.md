# Kafka Poll And Invoke
* Dependency the toolkit, such as using maven or gradle

```xml
   <dependency>
      <groupId>org.apache.skywalking</groupId>
      <artifactId>apm-toolkit-kafka</artifactId>
      <version>${skywalking.version}</version>
   </dependency>
```

* usage 1.
```java
   public class ConsumerThread2 extends Thread {
    @Override
    public void run() {
        Properties consumerProperties = new Properties();
        //...consumerProperties.put()
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(topicPattern, new NoOpConsumerRebalanceListener());
        while (true) {
            if (pollAndInvoke(consumer)) break;
        }
        consumer.close();
    }

    @KafkaPollAndInvoke
    private boolean pollAndInvoke(KafkaConsumer<String, String> consumer) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        ConsumerRecords<String, String> records = consumer.poll(100);

        if (!records.isEmpty()) {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url("http://localhost:8080/kafka-scenario/case/kafka-thread2-ping").build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
            }
            response.body().close();
            return true;
        }
        return false;
    }
}
```

_Sample codes only_



