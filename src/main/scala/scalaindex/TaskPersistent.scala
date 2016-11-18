package scalaindex


import akka.persistence.{PersistentActor, RecoveryCompleted}
import org.slf4j.Logger

import scala.reflect.ClassTag


/**
  * Created by yujieshui on 2016/11/13.
  */
trait TaskPersistent[Task, TaskResult] {
  self: PersistentActor =>

  def log: Logger

  var taskMap: Map[Task, Option[TaskResult]] = Map()

  def runTask(task: Task): Unit

  def runResult(result: TaskResult): Unit

  def result2task(result: TaskResult): Task

  def markResult(result: TaskResult): Unit = taskMap += result2task(result) -> Some(result)

  def markTask(task: Task): Unit = taskMap += task -> None

  def isFinish(task: Task) = taskMap.get(task).exists(_.nonEmpty)
  def isDoing (task: Task) = taskMap.get(task).exists(_.isEmpty)
  def redoTask(task: Task) = {
    log.info(s"redo task $task")
    runTask(task)
  }


  def taskCommand(implicit TaskClass: ClassTag[Task], TaskResultClass: ClassTag[TaskResult]): Receive = {
    case TaskClass(task)         => persist(task) { task =>
      log.debug(s"receive task :$task")
      markTask(task)
      runTask(task)
    }
    case TaskResultClass(result) => persist(result) { result =>
      log.debug(s"receive result:$result")
      runResult(result)
      markResult(result)
    }
  }

  def taskRecover(implicit TaskClass: ClassTag[Task], TaskResultClass: ClassTag[TaskResult]): Receive = {
    case TaskClass(task)         =>
      log.debug(s"recover task :$task")
      markTask(task)
    case TaskResultClass(result) =>
      markResult(result)
      log.debug(s"recover result :$result")
    case RecoveryCompleted       =>
      taskMap.collect { case (key, None) => key }.foreach(redoTask)
  }
}
