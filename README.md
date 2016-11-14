# crawler-scala-index

爬取 [scala-index](https://index.scala-lang.org) 上的lib,
然后向 [repox](https://github.com/Centaur/repox) 请求

### feature 
 1. 当自己是第一个吃螃蟹的人,也可以不用漫长的等待...
 2. 因为已经向repox请求过了,so 在本地可以安全的使用 [coursier](https://github.com/alexarchambault/coursier)
在也不用担心sha1不一致了
当然在爬取请求的时候应该使用使用原生 sbt
 
 
 
### 使用

```
git clone https://github.com/1178615156/crawler-scala-index
cd crawler-scala-index
sh start.sh 
```

### config 
修改改文件进行配置 

[1](src/main/scala/crawler/scalaindex/CrawlerScalaIndexConfig.scala)

默认值
```
 scalaVersion = Seq("2.11.8", "2.12.0")
 q            = "targets:scala_2.11"
 sort         = "starts"
 pageStart    = 1
 pageEnd      = 20
```