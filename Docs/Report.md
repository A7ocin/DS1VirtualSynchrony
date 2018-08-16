# Project report - Distributed Systems 1

Authors: *Garau Nicola* and *Manfredi Salvatore* - A.Y. 2017 - 2018    

## 1 - Introduction

The project is an implementation of the **Virtual Synchrony** protocol used for reliable group communication.

A multicast (group communication) is reliable when each sent message is delivered to every member of the group. Unlike other techniques, in which the groups are fixed, virtual synchrony exploits the presence of dynamic groups where the processes can join or leave.

The project has been implemented in Java using <u>Akka</u>, a toolkit designed for the creation of concurrent and distributed message-driven applications.

##2 - Protocol

Virtual synchrony is a distributed algorithm whose functioning revolves around the concept of **view**. 

### 2.1 - View

A view is nothing but a "list" of the processes part of the current group.

Each time a process joins or leaves (either voluntarily or due to a crash) the group, all the participants **install** a new view and take care of the <u>unstable</u> messages.

**Note**: message cannot cross epochs. This means that a message sent before a new view install must be delivered before the receiver installs the new view.

![](images/viewChange.png)

//stable messages

//flush messages

##3 - Classes

###3.1 - Actors
#####GenericActor
#####GroupManager
#####Participant

###3.2 - Enums
#####ActorStatusType
#####ActorType
#####SendingStatusType

###3.3 - Messages
#####AssignId
#####ChangeView
#####GenericMessage
#####JoinRequest
#####Message

###3.4 - Views
#####View

[^roba]: robaccia

