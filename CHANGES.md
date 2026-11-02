Changes by Version
==================
Release Notes.

9.9.0
------------------

* Fix the `NullPointerException` thrown by the `postgresql-8.x` plugin when executing statements prepared with
  `Statement.RETURN_GENERATED_KEYS` or column names (apache/skywalking#14138).
* Fix the `NullPointerException` thrown by the Lettuce plugin and the lost trace context of reactive commands when
  Lettuce tracing is enabled, and when a reactive command is cancelled before it is written (apache/skywalking#14178).
* Trace the commands that Lettuce writes as a batch, such as `StatefulConnection#dispatch(Collection)`, with one
  `Lettuce/BATCH_WRITE` span. Stop the Lettuce plugin from starting a trace of its own for every reactive command
  created without an active trace, and from creating a span that is never finished for a command that is already
  cancelled or timed out when it is written.
* Share the `Lettuce/BATCH_WRITE` span among all commands of a batch: it ends when the last of them completes, and is an
  error when any of them fails. A batch of a cluster connection gets one span per slot, as Lettuce writes the commands
  of each slot as a batch of their own. Mark the span of a reactive Lettuce command as an error when Redis replies with
  an error, such as `WRONGTYPE`.
* Fix the `NullPointerException`s thrown by the `rocketMQ-5.x` plugin, and the callback span stopped by the wrong
  callback, when an asynchronous send with a callback succeeds on rocketmq-client 5.1.4 or later, which wraps the
  callback in a `SendCallback` of its own (apache/skywalking#14189).
* Fix the missing `peer` and `mq.broker` of the consumer spans of the `rocketMQ-client-java-5.x` plugin for push
  consumers built by rocketmq-client-java 5.0.8 or later, which construct `PushConsumerImpl` with another constructor.
* Stop the `rocketMQ-client-java-5.x` plugin from dropping the lite topic (rocketmq-client-java 5.1.0 or later) and the
  priority (5.2.0 or later) of the messages it traces, which turned them into normal messages. The plugin rebuilt every
  message to add its headers, and now adds them to the copy of the message that the client sends.
* Test the `rocketMQ-5.x` plugin with rocketmq-client 5.5, and the `rocketMQ-client-java-5.x` plugin with
  rocketmq-client-java 5.0, 5.1 and 5.2.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/267?closed=1)

------------------
Find change logs of all versions [here](changes).
