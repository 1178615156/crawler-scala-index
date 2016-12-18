package scalaindex


import akka.persistence.{PersistentActor, RecoveryCompleted}
import org.slf4j.Logger

import scala.reflect.ClassTag


/**
  * Created by yujieshui on 2016/11/13.
  */
trait TaskPersistent[Task, TaskResult] {
  self: PersistentActor =>

  private var taskMap   = Map[Task, Option[TaskResult]]()
  private var doingTask = Set[Task]()

  def log: Logger

  def runTask(task: Task): Unit

  def runResult(result: TaskResult): Unit

  def result2task(result: TaskResult): Task

  def markResult(result: TaskResult): Unit = {
    if(!isFinish(result2task(result))) {
      log.info(s"finish task : $result")
      taskMap += result2task(result) -> Some(result)
    }
  }
  def maps: Map[Task, Option[TaskResult]] = taskMap.toMap

  def markTask(task: Task): Unit = {
    if(!taskMap.contains(task)) {
      log.info("mark task : " + task)
      taskMap += task -> None
    }
  }

  def markDoing(task: Task): Unit = {
    if(!doingTask.contains(task)) {
      log.info("doing task : " + task)
      doingTask += task
    }
  }

  def isFinish(task: Task): Boolean = taskMap.get(task).exists(_.nonEmpty)

  def isDoing(task: Task): Boolean = doingTask.contains(task)

  def redoTask(task: Task): Unit = {
    log.info(s"redo task $task")
    runTask(task)
    markDoing(task)
  }


  def taskCommand(implicit TaskClass: ClassTag[Task], TaskResultClass: ClassTag[TaskResult]): Receive = {
    case TaskClass(task)         => persist(task) { task =>
      log.debug(s"receive task  :$task")
      markTask(task)
      runTask(task)
      markDoing(task)
    }
    case TaskResultClass(result) => persist(result) { result =>
      log.debug(s"receive result:$result")
      runResult(result)
      markResult(result)
    }
  }

  def taskRecover(implicit TaskClass: ClassTag[Task], TaskResultClass: ClassTag[TaskResult]): Receive = {
    case TaskClass(task)         =>
      log.debug(s"recover task   :$task")
      markTask(task)
    case TaskResultClass(result) =>
      markResult(result)
      log.debug(s"recover result :$result")
    case RecoveryCompleted       =>
      taskMap.collect { case (key, None) => key }.foreach(redoTask)
  }
}
