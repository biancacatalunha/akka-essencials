package part1recap

import scala.concurrent.Future

object ThreadModelLimitations extends App {

  /*
  1. OOP encapsulation is only valid in the single threaded model
   */

  class BankAccount(var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int): Unit = this.amount -= money
    def deposit(money: Int): Unit = this.amount += money
    def getAmount: Int = amount
  }

  val account = new BankAccount(2000)
  for(_ <- 1 to 1000) {
    new Thread(() => account.withdraw(1)).start()
  }

  for(_ <- 1 to 1000) {
    new Thread(() => account.deposit(1)).start()
  }

  println(account.getAmount)
  //OOP encapsulation is broken in a multithreaded environment
  //Synchronization (this.synchronized) fixes that with locks
  //which introduces more problems like deadlocks, livelocks

  /*
  2. Delegating something to a thread is a pain.
   */

  //you have a running thread and you want to pass a runnable to that thread
  var task: Runnable = null
  val runningThread: Thread = new Thread(() => {
    while(true) {
      while(task == null) {
        runningThread.synchronized {
          println("[background] waiting for a task...")
          runningThread.wait()
        }
      }

      task.synchronized {
        println("[background] I have a task")
        task.run()
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable) = {
    if(task == null) task = r
    runningThread.synchronized {
      runningThread.notify()
    }
  }

  runningThread.start()
  Thread.sleep(500)
  delegateToBackgroundThread(() => println(42))
  Thread.sleep(1000)
  delegateToBackgroundThread(() => println("This should run in the background"))

  /*
  Need a data structure which
  - can safely receive messages
  - can identify the sender
  - is easily identifiable
  - can guard against errors
   */

  /*
  3. Tracing and dealing with errors in a multithreaded env is a pain.
   */

  // 1M numbers in between 10 threads
  import scala.concurrent.ExecutionContext.Implicits.global
  val futures = (0 to 9)
    .map(i => 100000 * i until 100000 * (i + 1)) // 0 to 99999, 100000 - 199999, 200000 - 299999 etc
    .map(range => Future {
      if(range.contains(546735)) throw new RuntimeException("Invalid number")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)(_ + _) //Future with the sum of all the numbers
  sumFuture.onComplete(println)

  /*
  Threads Limitations Summary:

  OOP is not encapsulated
  - race conditions

  Locks to the rescue?
  - dealocks, livelocks, headaches
  - a massive pain in distributed environments

  Delegating tasks
  - hard, error-prone
  - never feels "first-class" although often needed
  - should never be done in a blocking fashion

  Dealing with error
  - a monumental task in even small systems

   */
}
