akka-percolator
=================

Sample project, inspired by the article of Andrew Easter[1]. Mostly a playground of Akka and Elasticsearch technologies.
This project written in Java 8 and excluded UI part along with Play framework.
To start - import the project in your Eclipse, run the Main.java class. The 100 of simple search queries are inserted in Elasticsearch, then percolation starts against a dummy generated log entries.
You should see output stream of a matched log entries like below:

```
907===>{"timestamp":"2014-12-21 17:56:16.362","response time":"907","method":"DELETE","path":"/c","status":"500","device":"TV","user agent":"Internet Explorer"}
908===>{"timestamp":"2014-12-21 17:56:16.672","response time":"908","method":"DELETE","path":"/d","status":"500","device":"TV","user agent":"Safari"}
901===>{"timestamp":"2014-12-21 17:56:17.132","response time":"901","method":"GET","path":"/b","status":"200","device":"Desktop","user agent":"Firefox"}
```

To stop, simple kill the execution, the Elasticsearch indexes will re-populate on the next start.

Resources:
[1] http://www.dreweaster.com/blog/2013/07/08/reactive-real-time-log-search-with-play-akka-angularjs-and-elasticsearch/