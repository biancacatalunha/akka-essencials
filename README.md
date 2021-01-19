# Akka Essentials

## Actors
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
  
## Course Contents
- [x] 1h11min Scala and Parallel Programming recap
- [ ] 3h57min Akka Actors 
- [ ] 1h58min Testing Akka Actors 
- [ ] 1h51min Fault Tolerance
- [ ] 1h37min Akka Infrastructure
- [ ] 1h58min Akka Patterns