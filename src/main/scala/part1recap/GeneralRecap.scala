package part1recap

import scala.annotation.tailrec
import scala.reflect.runtime.universe.Try

object GeneralRecap extends App {

  val aCondition: Boolean = false
  var aVariable = 42
  aVariable += 1

  //expressions
  val aConditionVal = if(aCondition) 42 else 65

  //code block
  val aCodeBlock = {
    if(aCondition) 74
    56 //the result is the last expression
  }

  //types
  //Unit (side effect)
  val theUnit = println("Hello, Scala")

  //functions
  def aFunction(x: Int) = x + 1

  //recursion
  @tailrec
  def factorial(n: Int, acc: Int): Int =
    if(n <= 0) acc
    else factorial(n - 1, acc * n)

  //OOP
  class Animal
  class Dog extends Animal
  val aDog: Animal = new Dog

  trait Carnivore {
    def eat(a: Animal): Unit
  }

  class Crocodile extends Animal with Carnivore {
    override def eat(a: Animal): Unit = println("crunch")
  }

  //method notations
  val aCrocodile = new Crocodile
  aCrocodile.eat(aDog)
  aCrocodile eat aDog

  //anonymous classes
  val aCarnivore = new Carnivore {
    override def eat(a: Animal): Unit = print("roar")
  }

  aCarnivore eat aDog

  //generics
  //covariant - extension
  abstract class MyList[+A]
  //companion object
  object MyList

  //case classes
  case class Person(name: String, age: Int)

  //exceptions
  val aPotentialFailure = try {
    throw new RuntimeException("I'm innocent, I swear") //returns nothing
  } catch {
    case e: Exception => "I caught an exception"
  } finally {
    //side effects
    println("some logs")
  }

  //functional programming
  val incrementer = new Function[Int, Int] {
    override def apply(v1: Int): Int = v1 + 1
  }

  val incremented = incrementer(42)
  //incrementer.apply(42)

  val anonymousIncrementer = (x: Int) => x + 1
  //Int => Int === Function1[Int, Int]

  //FP is all about working with functions as first class
  List(1,2,3).map(incrementer)
  //map = higher order function (takes a function as a parameter or returns another function as a result)

  //for comprehensions
  val pairs = for {
    num <- List(1,2,3,4)
    char <- List('a','b','c','d')
  } yield num + "-" + char
  //same as:
  //List(1,2,3,4)/flatMap(num => List('a','b','c','d').map(char => num + "-" + char))

  //Seq, Array, List, Vector, Map, Tuples, Sets

  //"collections"
  //Options and Try
  val anOption = Some(2)
  val aTry = Try {
    throw new RuntimeException
  }

  //pattern matching
  val unknown = 2

  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  val bob = Person("Bob", 22)
  val greeting = bob match {
    case Person(name, _) => s"Hi, my name is $name"
    case _ => "I don't know my name"
  }

}
