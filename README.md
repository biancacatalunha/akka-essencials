# Akka Essentials

## Actors
### Basics
With traditional objects:
- We store their state as data
- we call their methods

With actors:
- We store their state as data
- We send messages to them, asynchronously 

Actors are objects we can't access directly, but only send messages to.

Actors interaction is similar to people having conversations. 
You'd say something to someone, and the person will reply or take an action.

Some natural principles:
- Every interaction happens via sending and receiving messages
- Messages are asynchronous by nature
  - it takes time for a message to travel
  - sending and receiving may not happen at the same time
  - or even in the same context

![Akka](images/how_akka_works_1.png)
![Akka](images/how_akka_works_2.png)

### Communication
Sending a message
- Message is enqueued in the actor's mailbox
- (thread-safe)

Processing a message
- A thread is scheduled to run this actor
- Messages are extracted from the mailbox, in order
- The thread invokes the handler on each message
- At this point the actor is unscheduled

### Guarantees
Only one thread operated on an actor at any time
- Actors are effectively single-threaded
- No locks needed
- Message process is atomic

Message delivery guarantees
- At most once delivery
- For any sender-receiver pair, the message order is maintained
  
## Course Contents
- [x] 1h11min Scala and Parallel Programming recap
- [ ] 3h57min Akka Actors 
- [ ] 1h58min Testing Akka Actors 
- [ ] 1h51min Fault Tolerance
- [ ] 1h37min Akka Infrastructure
- [ ] 1h58min Akka Patterns