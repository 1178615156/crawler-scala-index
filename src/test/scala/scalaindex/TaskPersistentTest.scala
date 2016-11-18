package scalaindex

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.persistence.PersistentActor
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{FunSuite, WordSpecLike}

import scala.concurrent.duration._
import scalaindex.TestTaskPersistent.{GetResult, Task, TaskResult}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by yujieshui on 2016/11/16.
  */
class TestTaskPersistent(override val persistenceId: String)
  extends PersistentActor
    with TaskPersistent[Task, TaskResult] {

  override def runTask(task: Task): Unit = {
    context.system.scheduler.scheduleOnce(1.second) {
      self ! TaskResult(task, task.i.toString)
    }
  }

  override def runResult(result: TaskResult): Unit = {
    println(result)
  }

  override def result2task(result: TaskResult): Task = result.task

  override def receiveRecover: Receive = taskRecover

  override def receiveCommand: Receive = taskCommand orElse {
    case GetResult() => sender() ! taskMap.values.toList.collect { case Some(x) => x }
  }

}

object TestTaskPersistent {

  case class Task(i: Int)

  case class TaskResult(task: Task, string: String)

  case class GetResult()

}

class TaskPersistentTest extends WordSpecLike {
  val id           = System.nanoTime().toString
  val finishTask   = TestTaskPersistent.Task(1)
  val unfinishTask = TestTaskPersistent.Task(2)
  implicit val timeout = Timeout(1.second)
  "persistent" must {
    lazy val testKit = new TestKit(ActorSystem("taskPersistent"))
    import testKit._
    lazy val actor = system.actorOf(Props(new TestTaskPersistent(id)))

    "run" in {
      actor ! finishTask
      Thread.sleep(2222)
      pipe(actor ask GetResult()) pipeTo testActor
      expectMsg(List(TaskResult(finishTask, "1")))
    }
    "lost msg" in {
      actor ! unfinishTask
      actor ! PoisonPill
    }
    "system terminate" in {
      system.terminate()
    }
  }

  "recover" must {
    lazy val testKit = new TestKit(ActorSystem("taskPersistent"))
    import testKit._
    lazy val actor = system.actorOf(Props(new TestTaskPersistent(id)))
    "start only have finish task" in {
      actor ask GetResult() pipeTo testActor
      expectMsg(List(TaskResult(finishTask, "1")))
    }
    "wait infinish task to finish" in {
      Thread.sleep(2222)
      actor ask GetResult() pipeTo testActor
      expectMsg(List(TaskResult(finishTask, "1"), TaskResult(unfinishTask, "2")))

    }
  }
}
