# jbp-launcher

java application launcher provides a easy way to start and stop a java application.

## Quick Start

### build
`git clone https://github.com/dinstone/jbp-launcher.git`
`mvn install`

### unzip
`unzip jbp-launcher-2.3.0.zip`
`cd jbp-launcher-2.3.0`

### develop
now you can develop your application activator like this:

```java

public class FooActivator {

private static final Logger LOG = Logger.getLogger(FooActivator.class.getName());

public void start() {
    showSystemEnvironment();

    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}

public void stop() {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
    
```

### deploy
edit launcher.properties file, modify the application.activator:
`application.activator=demo.FooActivator`

release your application package to 'lib' dir.

### start & stop
execute the shell:
`sh start.sh`
`sh stop.sh`
