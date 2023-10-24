Changes by Version
==================
Release Notes.

9.0.0
------------------

### Kernel Updates

* Support re-transform/hot-swap classes with other java agents, and remove the obsolete cache enhanced class feature.
* Implement new naming policies for names of auxiliary type, interceptor delegate field, renamed origin method, method
  access name, method cache value field. All names are under `sw$` name trait. They are predictable and unchanged after
  re-transform.

```
* SWAuxiliaryTypeNamingStrategy
  Auxiliary type name pattern: <origin_class_name>$<name_trait>$auxiliary$<auxiliary_type_instance_hash>

* DelegateNamingResolver
  Interceptor delegate field name pattern: <name_trait>$delegate$<class_name_hash>$<plugin_define_hash>$<intercept_point_hash>

* SWMethodNameTransformer
  Renamed origin method pattern: <name_trait>$original$<method_name>$<method_description_hash>

* SWImplementationContextFactory
  Method cache value field pattern: cachedValue$<name_trait>$<origin_class_name_hash>$<field_value_hash>
  Accessor method name pattern:  <renamed_origin_method>$accessor$<name_trait>$<origin_class_name_hash>
```

Here is an example of manipulated enhanced class with new naming policies of auxiliary classes, fields, and methods

```java
 import sample.mybatis.controller.HotelController$sw$auxiliary$19cja42;
 import sample.mybatis.controller.HotelController$sw$auxiliary$p257su0;
 import sample.mybatis.domain.Hotel;
 import sample.mybatis.service.HotelService;

 @RequestMapping(value={"/hotel"})
 @RestController
 public class HotelController
 implements EnhancedInstance {
     @Autowired
     @lazy
     private HotelService hotelService;
     private volatile Object _$EnhancedClassField_ws;

     // Interceptor delegate fields
     public static volatile /* synthetic */ InstMethodsInter sw$delegate$td03673$ain2do0$8im5jm1;
     public static volatile /* synthetic */ InstMethodsInter sw$delegate$td03673$ain2do0$edkmf61;
     public static volatile /* synthetic */ ConstructorInter sw$delegate$td03673$ain2do0$qs9unv1;
     public static volatile /* synthetic */ InstMethodsInter sw$delegate$td03673$fl4lnk1$m3ia3a2;
     public static volatile /* synthetic */ InstMethodsInter sw$delegate$td03673$fl4lnk1$sufrvp1;
     public static volatile /* synthetic */ ConstructorInter sw$delegate$td03673$fl4lnk1$cteu7s1;

     // Origin method cache value field
     private static final /* synthetic */ Method cachedValue$sw$td03673$g5sobj1;

     public HotelController() {
         this(null);
         sw$delegate$td03673$ain2do0$qs9unv1.intercept(this, new Object[0]);
     }

     private /* synthetic */ HotelController(sw.auxiliary.p257su0 p257su02) {
     }

     @GetMapping(value={"city/{cityId}"})
     public Hotel selectByCityId(@PathVariable(value="cityId") int n) {
         // call interceptor with auxiliary type and parameters and origin method object
         return (Hotel)sw$delegate$td03673$ain2do0$8im5jm1.intercept(this, new Object[]{n}, new HotelController$sw$auxiliary$19cja42(this, n), cachedValue$sw$td03673$g5sobj1);
     }

     // Renamed origin method
     private /* synthetic */ Hotel sw$origin$selectByCityId$a8458p3(int cityId) {
/*22*/         return this.hotelService.selectByCityId(cityId);
     }

     // Accessor of renamed origin method, calling from auxiliary type
     final /* synthetic */ Hotel sw$origin$selectByCityId$a8458p3$accessor$sw$td03673(int n) {
         // Calling renamed origin method
         return this.sw$origin$selectByCityId$a8458p3(n);
     }

     @OverRide
     public Object getSkyWalkingDynamicField() {
         return this._$EnhancedClassField_ws;
     }

     @OverRide
     public void setSkyWalkingDynamicField(Object object) {
         this._$EnhancedClassField_ws = object;
     }

     static {
         ClassLoader.getSystemClassLoader().loadClass("org.apache.skywalking.apm.dependencies.net.bytebuddy.dynamic.Nexus").getMethod("initialize", Class.class, Integer.TYPE).invoke(null, HotelController.class, -1072476370);
         // Method object
         cachedValue$sw$td03673$g5sobj1 = HotelController.class.getMethod("selectByCityId", Integer.TYPE);
     }
 }
```

Auxiliary type of Constructor :
```java
class HotelController$sw$auxiliary$p257su0 {
}
```

Auxiliary type of  `selectByCityId` method:
```java
class HotelController$sw$auxiliary$19cja42
implements Runnable,
Callable {
    private HotelController argument0;
    private int argument1;

    public Object call() throws Exception {
        return this.argument0.sw$origin$selectByCityId$a8458p3$accessor$sw$td03673(this.argument1);
    }

    @OverRide
    public void run() {
        this.argument0.sw$origin$selectByCityId$a8458p3$accessor$sw$td03673(this.argument1);
    }

    HotelController$sw$auxiliary$19cja42(HotelController hotelController, int n) {
        this.argument0 = hotelController;
        this.argument1 = n;
    }
}
```

#### Features and Bug Fixes

* Support Jdk17 ZGC metric collect
* Support Jetty 11.x plugin
* Support access to the sky-walking tracer context in spring gateway filter
* Fix the scenario of using the HBase plugin with spring-data-hadoop.
* Add RocketMQ 5.x plugin
* Fix the conflict between the logging kernel and the JDK threadpool plugin.
* Fix the thread safety bug of finishing operation for the span named "SpringCloudGateway/sendRequest"
* Fix NPE in guava-eventbus-plugin.
* Add WebSphere Liberty 23.x plugin
* Add Plugin to support aerospike Java client
* Add ClickHouse parsing to the jdbc-common plugin.
* Support to trace redisson lock
* Upgrade netty-codec-http2 to 4.1.94.Final
* Upgrade guava to 32.0.1
* Fix issue with duplicate enhancement by ThreadPoolExecutor
* Add plugin to support for RESTeasy 6.x.
* Fix the conditions for resetting UUID, avoid the same uuid causing the configuration not to be updated.
* Fix witness class in springmvc-annotation-5.x-plugin to avoid falling into v3 use cases.
* Fix Jedis-2.x plugin bug and add test for Redis cluster scene
* Merge two instrumentation classes to avoid duplicate enhancements in MySQL plugins.
* Support asynchronous invocation in jetty client 9.0 and 9.x plugin
* Add nacos-client 2.x plugin
* Staticize the tags for preventing synchronization in JDK 8
* Add RocketMQ-Client-Java 5.x plugin
* Fix NullPointerException in lettuce-5.x-plugin.

#### Documentation

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/178?closed=1)
