# Avro Parser Java Callout 

This directory contains the Java source code and pom.xml file required to
compile a custom policy (implemented in Java) for Apigee Edge. It 
parses an inbound message sent in Avro format. 


## Using The API Proxy


1. create a cache in the Apigee Edge environment called 'testcache'.  This is used by the 
demonstration apiproxy.  You can use the Admin UI to do so. 

2. Now deploy the API Proxy bundle with, Eg,    
   ```./pushapi -v -d -o ORGNAME -e prod -n csv-shredder bundle```

3. Turn on Tracing for the Apigee Edge proxy 

4. Use a client to load a CSV into the cache, via the proxy. Eg,   
   ```curl -i -X POST 
        https://ORGNAME-ENVNAME.apigee.net/avro-parser/shred?name=sample 
        --data-binary @example/users.avro```

5. View the trace screen and see that the Avro data has been parsed


## Dependencies

- Apigee Edge expressions v1.0
- Apigee Edge message-flow v1.0
- Apache commons lang 2.6
- FasterXML Jackson 2.3.0
- Apache Avro 1.7.7

These jars must be available on the classpath for the compile to
succeed. Using maven to build via the pom.xml file will download all of these files for
you, automatically. 



## Notes

From the payload, the shredder produces a Java object of type List<Map<String,Object>>
and caches it. It uses the queryparam "name" as the key for the cached item.
For this demonstration, you can have as many different cached lists as you like, each accessible by name. 



## Building:

You do not need to build the source code to use this callout.  But you can build it if you wish. 

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache.   
  ```bash ./buildsetup.sh```

3. Build with maven.  
  ```cd callout; mvn clean package```  
  
  The above will build the JAR and  copy the generated JAR and its dependencies to the bundle directory.  



## Bugs

There are no unit tests for this project.
