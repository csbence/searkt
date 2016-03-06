package edu.unh.cs.ai.realtimesearch.environment

interface Action<ReceiverState : State<ReceiverState>>
class NoOperationAction<ReceiverState : State<ReceiverState>> : Action<ReceiverState>