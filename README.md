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
```
然后 `sh start.sh --url=${repox-url}` 例如 `sh start.sh --url=http://127.0.0.1:8078` 

---

###

默认一个lib cache之后是不会再次重复cache的无论成功与否
当然可以先 `sh stop.sh` , `rm -rf journal/` , `sh start.sh` 重新cache

### config 

`vi src/main/resources/application.conf` 

```
scalaVersion = ["2.10","2.11", "2.12"]      //需要请求缓存的scala version
q            = ""                           //查询条件
sort         = "starts"                     //排序方式
pageStart    = 1                            //启示页
pageEnd      = 1                            //终止页
withSources  = true                         //是否下载 sources
withJavadoc  = true                         //是否下载 javadoc
reTryNum     = 3                            //重试次数
```

