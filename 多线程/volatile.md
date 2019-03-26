## 1.简介

volatile关键字用来阻止（伪）编译器认为的无法“被代码本身”改变的代码（变量/对象）进行优化(**指令重排**)

如在C语言中，volatile关键字可以用来提醒编译器它后面所定义的变量随时有可能改变，因此编译后的程序每次需要存储或读取这个变量的时候，都会直接从变量地址中读取数。如果没有volatile关键字，则编译器可能优化读取和存储，可能暂时使用寄存器中的值，如果这个变量由别的程序更新了的话，将出现不一致的现象。据



在 JDK1.2 之前，Java的内存模型实现总是从主存（即共享内存）读取变量，是不需要进行特别的注意的。而在当前的 Java 内存模型下，线程可以把变量保存本地内存（比如机器的**寄存器**）中，而不是直接在主存中进行读写。这就可能造成一个线程在主存中修改了一个变量的值，而另外一个线程还继续使用它在寄存器中的变量值的拷贝，造成数据的不一致。 



![](https://ws1.sinaimg.cn/large/006rNwoDgy1fpo4w1256pj307l04m3yf.jpg)

要解决这个问题，就需要把变量声明为 **volatile**，这就指示 JVM，这个变量是不稳定的，每次使用它都到主存中进行读取

![](https://ws1.sinaimg.cn/large/006rNwoDgy1fpo4w2fiv9j30d606mmx5.jpg)

## 2.volatile可见性

volatile 修饰的成员变量,

- 每次线程在读数据的时候,强迫线程从**主存**(共享内存),中重读该成员变量的值
- 每次线程在修改数据的时候,强迫线程将变化的值写入主存中,

不同的线程都能够看到相同的此变量的值

eg.

```java
private volatile boolean isRunning = true;
 int m;
    public boolean isRunning() {
        return isRunning;
    }
    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }
    @Override
    public void run() {
        System.out.println("进入run了");
        while (isRunning == true) {
            int a=2;
            int b=3;
            int c=a+b;
            m=c;
        }
        System.out.println(m);
        System.out.println("线程被停止了！");
    }
}
public class Run {
    public static void main(String[] args) throws InterruptedException {
        RunThread thread = new RunThread();

        thread.start();
        Thread.sleep(1000);
        thread.setRunning(false);

        System.out.println("已经赋值为false");
    }
}

```

如果不加volatile 会死循环,加上之后得出结果

但是如果在m=c之后加上system.out.println(m),或者加上sleep() 这个时候也能得出正确结果

**jvm会尽力保证内存的可见性,即使这个变量没有加同步关键字,只要有时间,jvm就会保证内存的可见性**

## 3.禁止指令重排

volatile关键字用来阻止（伪）编译器认为的无法“被代码本身”改变的代码（变量/对象）进行优化(**指令重排**)

## 4.原子性??

volatile 无法保证对变量的原子性

## 5.synchronized 和volatile比较

- volatile是线程同步的轻量级实现,volatile的性能比synchronized好
- volatile只能修饰变量,synchronized 可以代码块,方法
- 多线程访问volatile关键字不会发生阻塞,synchronized会
- volatile能保证变量的可见性,不能保证原子性,但是synchronized 都可以保证
- volatile用于解决多线程之间的变量的可见性问题,synchronized保证的是多线程竞争访问资源的同步性









































