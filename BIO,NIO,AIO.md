![](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\io\javaio.jpg)

![](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\io\javaio2.jpg)

**同步与异步**

- **同步：** 同步就是发起一个调用后，被调用者未处理完请求之前，调用不返回。
- **异步：** 异步就是发起一个调用后，立刻得到被调用者的回应表示已接收到请求，但是被调用者并没有返回结果，此时我们可以处理其他的请求，被调用者通常依靠事件，回调等机制来通知调用者其返回结果。

同步和异步的区别最大在于异步的话调用者不需要等待处理结果，被调用者会通过回调等机制来通知调用者其返回结果。

**阻塞和非阻塞**

- **阻塞：** 阻塞就是发起一个请求，调用者一直等待请求结果返回，也就是当前线程会被挂起，无法从事其他任务，只有当条件就绪才能继续。
- **非阻塞：** 非阻塞就是发起一个请求，调用者不用一直等着结果返回，可以先去干其他事情。







# 1. BIO (Blocking I/O)

![](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\io\bio通信模型图.jpg)

采用 **BIO 通信模型** 的服务端，通常由一个独立的 Acceptor 线程负责监听客户端的连接,

- 我们一般通过在`while(true)` 循环中服务端会调用 `accept()` 方法等待接收客户端的连接的方式监听请求

- 请求一旦接收到一个连接请求，就可以建立通信套接字在这个通信套接字上进行读写操作，此时不能再接收其他客户端连接请求，只能等待同当前连接的客户端的操作执行完成， 不过可以通过多线程来支持多个客户端的连接

- 如果要让 **BIO 通信模型** 能够同时处理多个客户端请求，就必须使用多线程（主要原因是`socket.accept()`、`socket.read()`、`socket.write()` 涉及的三个主要函数都是**同步阻塞**的）

- 接收到客户端连接请求->创建一个新的线程进行链路处理->输出流返回应答给客户端->线程销毁 ,典型的 **一请求一应答通信模型** 
- 可以通过 **线程池机制** 改善
- 使用`FixedThreadPool` 可以有效的控制了线程的最大数量，保证了系统有限的资源的控制，

## 1.1问题

- 线程的创建和销毁成本很高
- 线程的切换成本也是很高
-  Linux操作系统中,线程本质上就是一个进程，创建和销毁线程都是重量级的系统函数
- 如果并发访问量增加会导致线程数急剧膨胀可能会导致线程堆栈溢出、创建新线程失败等问题，最终导致进程宕机或者僵死，不能对外提供服务

## 1.2伪异步 IO

![](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\io\伪异步IO模型图.jpg)

- 当有新的客户端接入时，将客户端的 Socket 封装成一个Task（该任务实现java.lang.Runnable接口）投递到后端的线程池中进行处理

- JDK 的线程池维护一个消息队列和 N 个活跃线程，对消息队列中的任务进行处理

- 由于线程池可以设置消息队列的大小和最大线程数，因此，它的资源占用是可控的，无论多少个客户端并发访问，**都不会导致资源的耗尽和宕机**

## 1.3代码演示

```java
public class IOClient {

  public static void main(String[] args) {
    // TODO 创建多个线程，模拟多个客户端连接服务端
    new Thread(() -> {
      try {
        Socket socket = new Socket("127.0.0.1", 3333);
        while (true) {
          try {
            socket.getOutputStream().write((new Date() + ": hello world").getBytes());
            Thread.sleep(2000);
          } catch (Exception e) {
          }
        }
      } catch (IOException e) {
      }
    }).start();

  }

}
```

```java
public class IOServer {

  public static void main(String[] args) throws IOException {
    // TODO 服务端处理客户端连接请求
    ServerSocket serverSocket = new ServerSocket(3333);

    // 接收到客户端连接请求之后为每个客户端创建一个新的线程进行链路处理
    new Thread(() -> {
      while (true) {
        try {
          // 阻塞方法获取新的连接
          Socket socket = serverSocket.accept();

          // 每一个新的连接都创建一个线程，负责读取数据
          new Thread(() -> {
            try {
              int len;
              byte[] data = new byte[1024];
              InputStream inputStream = socket.getInputStream();
              // 按字节流方式读取数据
              while ((len = inputStream.read(data)) != -1) {
                System.out.println(new String(data, 0, len));
              }
            } catch (IOException e) {
            }
          }).start();

        } catch (IOException e) {
        }

      }
    }).start();

  }

}
```

## 1.4总结

在活动连接数不是特别高（**小于单机1000**）的情况下，这种模型是比较不错的，可以让每一个连接专注于自己的 I/O 并且编程模型简单，也不用过多考虑系统的过载、限流等问题。线程池本身就是一个天然的漏斗，可以缓冲一些系统处理不了的连接或请求。但是，当面对十万甚至百万级连接的时候，传统的 BIO 模型是无能为力的。因此，我们需要一种更高效的 I/O 处理模型来应对更高的并发量。

# 2. NIO (New I/O)

## 2.1 NIO 简介

- 在Java 1.4 中引入了NIO框架(同步非阻塞的I/O模型)

- NIO提供了与传统BIO模型中的 `Socket` 和 `ServerSocket` 相对应的 `SocketChannel` 和 `ServerSocketChannel` 两种不同的套接字通道实现,两种通道都支持阻塞和非阻塞两种模式

## 2.2NIO的特性/NIO与IO区别

- **Non-blocking IO（非阻塞IO) IO流是阻塞的，NIO流是不阻塞的**
  - 单线程在从通道读取buffer,还可以做别的事情,当数据读取到buffer时候,线程再处理数据,写同样如此

- **Buffer(缓冲区)IO 面向流(Stream oriented)，而 NIO 面向缓冲区(Buffer oriented)**
  - I/O中将数据读写入stream对象,虽然stream中有buffer开头的拓展类,但是只是流的包装类
  - NIO中,所有数据使用缓冲区做处理,

- **channel**NIO 通过Channel（通道） 进行读写。
  - 通道是双向的，可读也可写
  - 而流的读写是单向的
  - 无论读写，通道只能和Buffer交互。因为 Buffer，通道可以异步地读写

- **selectors(选择器)**NIO有选择器，而IO没有。
  - 选择器用于使用单个线程处理多个通道
  - 线程之间的切换对于操作系统来说是昂贵的。 因此，为了提高系统效率选择器是有用的。

![](C:\Users\27660\Desktop\2019年03月20日迎战魔都\JavaGuide\io\Selector.jpg)

## 2.3 NIO 读数据和写数据方式

- 从通道进行数据读取 ：创建一个缓冲区，然后请求通道读取数据。
- 从通道进行数据写入 ：创建一个缓冲区，填充数据，并要求通道写入数据。

## 2.4NIO核心组件

- Channel(通道)
- Buffer(缓冲区)
- Selector(选择器)

### channel

(涵盖了UDP 和 TCP 网络IO，以及文件IO)(https://mp.weixin.qq.com/s?__biz=MzU4NDQ4MzU5OA==&mid=2247483966&idx=1&sn=d5cf18c69f5f9ec2aff149270422731f&chksm=fd98545fcaefdd49296e2c78000ce5da277435b90ba3c03b92b7cf54c6ccc71d61d13efbce63#rd)

- DatagramChannel
- SocketChannel  ServerSocketChannel
- FileChannel

### buffer	

(https://mp.weixin.qq.com/s?__biz=MzU4NDQ4MzU5OA==&mid=2247483961&idx=1&sn=f67bef4c279e78043ff649b6b03fdcbc&chksm=fd985458caefdd4e3317ccbdb2d0a5a70a5024d3255eebf38183919ed9c25ade536017c0a6ba#rd)

- ByteBuffer
- ByteBuffer
- ShortBuffer
- IntBuffer
- FloatBuffer
- DoubleBuffer
- LongBuffer

1. **把数据写入buffer；**
2. **调用flip；**
3. **从Buffer中读取数据；**
4. **调用buffer.clear()或者buffer.compact()。**

**clear()** 或 **compact()** 方法。**clear会清空整个buffer，compact则只清空已读取的数据**



**capacity容量  position位置 limit限制**

- buffer就是一个固定的内存区,buffer的大小叫做capacity
- 写入数据需要从一个确定的位置,默认为0,写入数据后,为写入数据所到达的位置 ,**当buffer从写模式变换为读模式position会归零,position向后移动**
- **写模式下,limit等同于buffer的capacity ,再读模式下,等同于写模式下的position的大小**

### selector (多路复用器)

https://mp.weixin.qq.com/s?__biz=MzU4NDQ4MzU5OA==&mid=2247483970&idx=1&sn=d5e2b133313b1d0f32872d54fbdf0aa7&chksm=fd985423caefdd354b587e57ce6cf5f5a7bec48b9ab7554f39a8d13af47660cae793956e0f46#rd

**要使用Selector的话，我们必须把Channel注册到Selector上，然后就可以调用Selector的select()方法。这个方法会进入阻塞，直到有一个channel的状态符合条件。当方法返回后，线程可以处理这些事件。**





## 2.5 代码示例

```java
/**
 * 
 * @author 闪电侠
 * @date 2019年2月21日
 * @Description: NIO 改造后的服务端
 */
public class NIOServer {
  public static void main(String[] args) throws IOException {
    // 1. serverSelector负责轮询是否有新的连接，服务端监测到新的连接之后，不再创建一个新的线程，
    // 而是直接将新连接绑定到clientSelector上，这样就不用 IO 模型中 1w 个 while 循环在死等
    Selector serverSelector = Selector.open();
    // 2. clientSelector负责轮询连接是否有数据可读
    Selector clientSelector = Selector.open();

    new Thread(() -> {
      try {
        // 对应IO编程中服务端启动
        ServerSocketChannel listenerChannel = ServerSocketChannel.open();
        listenerChannel.socket().bind(new InetSocketAddress(3333));
        listenerChannel.configureBlocking(false);
        listenerChannel.register(serverSelector, SelectionKey.OP_ACCEPT);

        while (true) {
          // 监测是否有新的连接，这里的1指的是阻塞的时间为 1ms
          if (serverSelector.select(1) > 0) {
            Set<SelectionKey> set = serverSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = set.iterator();

            while (keyIterator.hasNext()) {
              SelectionKey key = keyIterator.next();

              if (key.isAcceptable()) {
                try {
                  // (1)
                  // 每来一个新连接，不需要创建一个线程，而是直接注册到clientSelector
                  SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
                  clientChannel.configureBlocking(false);
                  clientChannel.register(clientSelector, SelectionKey.OP_READ);
                } finally {
                  keyIterator.remove();
                }
              }

            }
          }
        }
      } catch (IOException ignored) {
      }
    }).start();
    new Thread(() -> {
      try {
        while (true) {
          // (2) 批量轮询是否有哪些连接有数据可读，这里的1指的是阻塞的时间为 1ms
          if (clientSelector.select(1) > 0) {
            Set<SelectionKey> set = clientSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = set.iterator();

            while (keyIterator.hasNext()) {
              SelectionKey key = keyIterator.next();

              if (key.isReadable()) {
                try {
                  SocketChannel clientChannel = (SocketChannel) key.channel();
                  ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                  // (3) 面向 Buffer
                  clientChannel.read(byteBuffer);
                  byteBuffer.flip();
                  System.out.println(
                      Charset.defaultCharset().newDecoder().decode(byteBuffer).toString());
                } finally {
                  keyIterator.remove();
                  key.interestOps(SelectionKey.OP_READ);
                }
              }

            }
          }
        }
      } catch (IOException ignored) {
      }
    }).start();

  }
}
```

## 2.6问题

- JDK 的 NIO 底层由 **epoll** 实现，该实现饱受诟病的空轮询 bug 会导致 cpu 飙升 100%
- 项目庞大之后，自行实现的 NIO 很容易出现各类 bug，维护成本较高，上面这一坨代码我都不能保证没有 bug

# 3.AIO

https://mp.weixin.qq.com/s?__biz=Mzg3MjA4MTExMw==&mid=2247484746&idx=1&sn=c0a7f9129d780786cabfcac0a8aa6bb7&source=41#wechat_redirect

- **异步非阻塞的IO模型**

- AIO 也就是 NIO 2 **异步 IO 是基于事件和回调机制实现**

# 4.Path Files

java7中io发生了变化,引入了path  files代替file 

https://mp.weixin.qq.com/s?__biz=MzU4NDQ4MzU5OA==&mid=2247483976&idx=1&sn=2296c05fc1b840a64679e2ad7794c96d&chksm=fd985429caefdd3f48e2ee6fdd7b0f6fc419df90b3de46832b484d6d1ca4e74e7837689c8146&token=537240785&lang=zh_CN#rd

```java
package com.filesAndPath;

import com.sun.javaws.exceptions.ExitException;
import org.junit.jupiter.api.Test;
import sun.security.validator.SimpleValidator;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * files 和 path 是jdk.7 引入的新的用来代替file的类
 */
public class FilesDemo {

    Path path = Paths.get("C:\\Users\\27660\\Desktop\\testfile.txt");

    //仅仅创建文件
    @Test
    public void test1() throws  Exception{
        Path file = Files.createFile(path);
        boolean exists = Files.exists(file);
        System.out.println(exists);
    }

    @Test
    public void test2() throws  Exception{
        Files.delete(path);
    }

    @Test
    public void test3() throws  Exception{
        Path targetPath = Paths.get("C:\\Users\\27660\\Desktop\\2019年03月20日迎战魔都\\1.txt");
        Path file = Files.copy(path, targetPath);
        boolean exists = Files.exists(file);
        System.out.println(exists);
        Files.delete(file);
    }


    @Test
    public void test4() throws  Exception{
        System.out.println(Files.isDirectory(path));
        System.out.println(Files.size(path));
        System.out.println(Files.readAttributes(path,"*"));
    }
    Path dir = Paths.get("C:\\Users\\27660\\Desktop\\2019年03月20日迎战魔都\\JavaGuide");

    @Test
    public void test5() throws  Exception{
//        URI uri = path.toUri();
//        System.out.println(uri);
        DirectoryStream<Path> paths = Files.newDirectoryStream(dir);
        paths.forEach(System.out::println);
    }

    @Test
    public void test6() throws  Exception{
        LinkedList<Path> result = new LinkedList<>();
        Files.walkFileTree(dir,new FindJavaVis(result));
        result.forEach(System.out::println);
    }

    static  class FindJavaVis extends  SimpleFileVisitor<Path>{
        private List<Path> result ;

        public FindJavaVis(List<Path> result) {
            this.result = result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
           if(file.toString().endsWith(".md")){
                result.add(file.getFileName());
           }
           return  FileVisitResult.CONTINUE;
        }
    }


}

```

# 5.内存映射

这个功能主要是为了提高大文件的读写速度而设计的。内存映射文件(memory-mappedfile)能让你创建和修改那些大到无法读入内存的文件。有了内存映射文件，你就可以认为文件已经全部读进了内存，然后把它当成一个非常大的数组来访问了。将文件的一段区域映射到内存中，比传统的文件处理速度要快很多。内存映射文件它虽然最终也是要从磁盘读取数据，但是它并不需要将数据读取到OS内核缓冲区，而是直接将进程的用户私有地址空间中的一部分区域与文件对象建立起映射关系，就好像直接从内存中读、写文件一样，速度当然快了。

# 6.AsynchronousFileChannel异步读取文件

java7新增

http://wiki.jikexueyuan.com/project/java-nio-zh/java-nio-asynchronousfilechannel.html



# 7.高并发与aio nio

http://www.importnew.com/21341.html

























































