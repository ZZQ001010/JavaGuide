## 一.配置

动态路由,监控,安全,服务监控

### 路由

```java
zuul.routes.<route>.path=<path>
zuul.routes.<route>.serviceId=<serviceName>
```

提供一种简化配置的方式

```java
zuul.routes.<serviceId>=<path>
```

### 单实例映射

```yaml
zuul:
    routes:
      client-a: 
        path: /client/**
      url: http://localhost:8888
```

### forward本地跳转

- 在zuul项目中添加controller层,提供restful Api接口

- yml/properties 配置

```yaml
zuul:
    routes: 
      client-a: 
        path: /client/**
      url: forward: /client
```

### 相同路径加载规则

如果出现相同的路径, spring会在加载配置文件的时候进行覆盖

### 路由通配符

- /**            匹配任意级目录

- /*                 匹配目录下任意字符

- /?                匹配一个字符

### 路由器前缀

```yaml
zuul:    
    prefix: 
  routes: 
      client-a: /client/**
                path: /client-a/**
        serviceId: client-a
#        stripPrefix: flase   关闭前缀
```

### 服务屏蔽

```yaml
    zuul: 
      ignored-services: #禁用的服务,这里可以写* 表示,所有的服务都不能够访问, 可以写服务名,多个服务名,隔开
    ignored-patterns: #忽略的接口,屏蔽的接口
```

### 敏感头信息

```yaml
zuul:
    routes:
      client-a:
        sensitiveHeaders: Cookie,set-Cookie #这里写的敏感头,会切断他和下层服务的交互
```

zuul默认会清除用户请求的信息,**这个地方一定要配置**

### 重定向问题

```yaml
zuul: 
    add-host-header:true   #避免在认证过程之后暴露认证服务地址
```

### 重试机制

```yaml
zuul:
    retryable:true #开启重试
ribbon:
    MaxAutoRetries: 1    #同一服务重试次数
    MaxAutoRetriesNextServer:1 #切换相同服务数量
```

### 二.原理

zuul 的核心就是一些紧密配合的Filter来实现,没有Filter 的责任链,就没有zuul

- zuul中的每个filter之间不能够直接通讯,在请求线程中通过requestContext来共享状态,内部都是使用threadLocal实现,

- zuul核心逻辑就是在zuulservlet 中

#### 生命周期

pre: 路由前执行,可以进行鉴权,限流

route:路由执行,

post: 源服务返回结果或者异常之后执行

error: 整个声明周期如果发生异常就会进入error

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\zuul中默认的filter](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\zuul中默认的filter.jpg)

如果想禁用其中的filter 可以配置

```yaml
zuul.SendErrorFilter.error.disable=true
zuul.<禁用filter类名>.<filterType>.disable=true|false
```

### 三.业务处理

继承`ZuulFilter`即可,实现其中的方法

### 四.限流(限流算法)

#### 漏桶

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\漏桶算法](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\漏桶算法.jpg)

- 设计一个桶,桶的下面有个孔,孔会匀速的漏掉水

- 数据流会被收集起来,多余的移除

- 在b/s 架构中,当然不会把溢出的流量浪费,可以使用列队进行收集起来,尽量合理利用资源

#### 令牌桶

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\令牌桶算法](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\令牌桶算法.jpg)

- 桶中存放令牌,令牌以一个恒定的速率被加入桶中,可以积压,可以溢出

- 当数据涌出来的时候,量化请求获取令牌,如果取到令牌直接放行,同时桶内丢掉这个令牌,如果不能得到令牌,这个请求就丢弃

- 由于令牌桶内可以存入一定量的令牌,因此会存在一定量的流量冲突,这也就是两个算法应用不同场景

#### 使用spring-cloud-zuul-ratelimit ,

针对zuul写的限流库

1. 引入依赖

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-zuul-ratelimit</artifactId>
        </dependency>
```

2.yml配置

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\spring-cloud-ratelimit](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\spring-cloud-ratelimit.jpg)

### 四.动态路由

- 结合spring cloud config +bus   (springcloud 推崇的方式)

- **重写zuul的配置读取方式, 读取数据库配置,采用事件定时刷新机制,轻松实现管理界面,灵活读高**

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\DiscoveryClientRouteLocator](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\DiscoveryClientRouteLocator.jpg)

- locateRoutes()方法就是将配置文件中的规则信息,包装程linkedHashMap<String,ZuulRoute>, string就是path

- refresh()  刷新

#### ZuulServierAutoConfigurtion

注册监听器,过滤器,

#### ZuulHandlerMapping

- 将本地配置的映射关系,映射到远程的过程控制

```java
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (this.routeLocator instanceof RefreshableRouteLocator) {
            ((RefreshableRouteLocator) this.routeLocator).refresh();
        }
    }
```

这个dirty很重要,是用来控制当前是否需要重新加入映射配置文件信息的标记,如果为true,就会触发配置信息的重新加载,然后在设置为false

#### 五.基于DB 实现动态路由

吧映射信息存入数据库，链接数据库，取出信息

继承simpleroutelocator 覆盖他的locateRoutes()

注入ioc 就ok

### springcloud Zuul 灰色发布

- 是指在 系统迭代新功能的时候一种平滑过渡的上线方式,
  - 在原有基础上,额外增加一个新增版本,这个版本包含我们要测试验证的一些功能,然后用负载均衡引入一小部分流量到新的版本应用中,如果版本没有什么问题,就把此系统,或者服务一步一步的进行替换,    

#### 灰色发布1

- 配置eureka matadata 

```yaml
eureka.instance.metadata.<key>=<value>
```

如果要实现这样的功能需要pom引入依赖

```xml
<dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>ribbon-discovery-filter-spring-cloud-starter</artifactId>
</dependency>
```

yml配置

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\灰色发布yml配置](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\灰色发布yml配置.jpg)

springboot 启动类main方法参数传递

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\灰色发布springboot main方法](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\灰色发布springboot main方法.jpg)

切换node1 node2 node3 就会启动不同的实例

- 最重要的就是filter 编写

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\灰色发布filter编写](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\灰色发布filter编写.jpg)

### 六.文件上传

在bootstarp.yml中需要对上传文件的闸值做限制,对服务熔断,负载均衡超时时间做改变

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\springcloud 文件上传](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\springcloud 文件上传.jpg)

### 七. 常用技巧

#### 饥饿加载

第一次请求，到来的时候，zuul会使用ribbon去调用远程服务，由于ribbon的原因，会首先从eureka 中获取元数据（注册表）这个过程非常耗时，因此我们在zuul启动的时候就去加载注册表，配置一下参数

```yaml
zuul:
    ribbon:
      eager-load:
        enable:true
```

#### 请求体修改

从requestContext.getCurrentContext() 获取请求参数

通过context.setRequest(new HttpservletRequestWrapper(context.getrequest({

})))

实现接口中的方法就ok

#### okhttp替换为clienthttp

clientHttp 难以拓展,okhttp有以下优点

- 支持spdy,可以用于合并多个对于同一个host的请求

- 使用连接复用机制减少资源消耗

- 使用GZIP减少数据传输

- 对相应缓存,避免重复的网络请求

1. 加入okhttp依赖

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp</artifactId>
</dependency>
```

#### retry机制

SpringCloud zuul 重试机制是配合spring retry and ribbon 使用的

```xml
<dependency>
	<groupId>org.springframework.retry</groupId>
  <artifactId>spring-retry</artifactId>
</dependency>
```

**yml配置**

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\spring-rery yml配置](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\spring-rery yml配置.jpg)

上述配置是对所有的服务来说,可以单独的配置

```yaml
zuul.routes.<rooute>.retryable=true
```

#### Header传递

通过requestContext 的addZuulRequestHeader() 方法可以动态的添加header到下游

#### SpringCloud zuul 多层负载

实际业务中,会在zuul上加一层nginx,通过nginx请求到zuul负载层

?    但是此时有一个问题, 如果zuul挂掉一个,此时就会有 1/n 的请求失败,这个时候怎么办? 

**解决方案**

OpenRestry整合nginx和lua,通过lua脚本从注册中心获取upd的服务,动态加载到nginx中,实现多层负载

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\nginx+lua实现多层负载](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\nginx+lua实现多层负载.jpg)

[https://github.com/zzq1314zll/nginx-zuul-dynamic-lb](https://github.com/zzq1314zll/nginx-zuul-dynamic-lb)

实现的原理就是使用lua 脚本通过eureka 提供restfull api ,获取up的服务

#### springcloud zuul应用优化

##### 容器优化

undertow提供阻塞或者基于xnio的非阻塞机制,在springboot中移除tomcat,添加undertow 

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\undertow配置参数](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\undertow配置参数.jpg)

这里的worker-thread参数为什么是*8 ,这是jboss团队反复调试得出,

**zuul下游是静态资源的情况下设置稍小,与数据库交互大的情况下设置大**

##### 组件优化

###### hystrix

       在zuul启动之后需要初始化很多数据,因此第一次请求比较慢,这个时候hystrix 很可能会启动熔断机制这个时候hystrix 不会买单,解决问题配置如下

```yaml
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=5000
#设置等待时间
hystrix.command.default.execution.timeout.enabled=false
#直接关掉timeout
```

**线程隔离模式的选择**

在网关中对资源的使用应该严格控制,如果不加限制,会导致资源的滥用,在恶略的线上环境会造成雪崩,

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\hystrix线程隔离方式对比](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\hystrix线程隔离方式对比.jpg)

###### ribbon

zuul整合ribbon 可以设置调用服务超时重试机制,下面是yml配置

```yaml
ribbon:
	ConnectTimeout:3000
  ReadTimeout:60000
  MaxAutoRetries: 1 #对第一次请求的重试次数
  MaxAutoRetrieNextServer: 1 #要重试的下一个服务的最大数量(不包含第一个服务)
  OkToRetryOnAllOperations: true
```

##### jvm优化

网关需要的是`吞吐量`

对于jdk1.8之前,我们选择使用parallel scavenge 收集器, 这个收集器和cms 不同,cms 是专注的gc停顿时间和缩短垃圾回收时用户线程的停顿时间,Parallel scavenge 关注的是达到一个可控的吞吐量

参数:

```java
-XX:MaxGCPauseMillis    控制最大垃圾收集停顿时间
-XX:GCTimeRatio					直接设置吞吐量大小
-Xmx  									设置最大堆
-XX:+UseAdaptiveSizePolicy  这是一个开关,当打开此开关的时候就不需要手动指定,eden 和 survivor 的比例,虚拟机会根据当前系统运行的状况收集性能监控信息,动态调整这些 参数提供合适的停顿和吞吐量
```

**自适应调节是parallel Scavenge 和ParNew 的最大区别**

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\jvm垃圾回收搭配](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\jvm垃圾回收搭配.jpg)

**建议:**

    使用参数`-XX:+TargetSurvivorRatio` 使用此参数可以调整survivor的对象利用率,默认为50%,建议加大,加大之后,ygc会发生比较明显的效果,让对象尽量在新生代回收掉,避免进入老年代发生FGC
    
    使用参数`-XX:SavengeBeforeFullGC`让fullgc执行前先执行一次ygc 推荐使用这个参数

##### 内部优化

![C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\zuul 内部优化](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\Spring Cloud\image\zuul 内部优化.jpg)
























