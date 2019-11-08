package com.rpiaggio.crystal.effects

import cats.{Defer, MonadError}
import cats.effect.{Bracket, ExitCase, Sync}
import japgolly.scalajs.react.{CallbackTo, CatsReact}

trait CallbackToEffects {
  private val callbackToMonadError: MonadError[CallbackTo, Throwable] =
    CatsReact.reactCallbackCatsInstance
  //      implicitly[MonadError[CallbackTo, Throwable]]

  trait CallbackToDefer extends Defer[CallbackTo] {
    override def defer[A](fa: => CallbackTo[A]): CallbackTo[A] =
      CallbackTo.byName(fa)
  }

  implicit val callbackToDefer: Defer[CallbackTo] = new CallbackToDefer {}

  trait CallbackToBracket extends Bracket[CallbackTo, Throwable] {
    override def bracketCase[A, B](acquire: CallbackTo[A])(use: A => CallbackTo[B])(release: (A, ExitCase[Throwable]) => CallbackTo[Unit]): CallbackTo[B] =
      acquire.flatMap { a =>
        handleErrorWith(use(a))(t => release(a, ExitCase.Error(t)).flatMap(_ => raiseError(t)))
          .flatMap(b => release(a, ExitCase.Completed).map(_ => b))
      }
    override def pure[A](x: A): CallbackTo[A] = callbackToMonadError.pure(x)
    override def flatMap[A, B](fa: CallbackTo[A])(f: A => CallbackTo[B]): CallbackTo[B] = callbackToMonadError.flatMap(fa)(f)
    override def tailRecM[A, B](a: A)(f: A => CallbackTo[Either[A, B]]): CallbackTo[B] = callbackToMonadError.tailRecM(a)(f)
    override def raiseError[A](e: Throwable): CallbackTo[A] = callbackToMonadError.raiseError(e)
    override def handleErrorWith[A](fa: CallbackTo[A])(f: Throwable => CallbackTo[A]): CallbackTo[A] = callbackToMonadError.handleErrorWith(fa)(f)
  }

  implicit val callbackToBracket: Bracket[CallbackTo, Throwable] = new CallbackToBracket {}

  trait CallbackToSync extends CallbackToBracket with CallbackToDefer with Sync[CallbackTo] {
    override def suspend[A](thunk: => CallbackTo[A]): CallbackTo[A]
    = CallbackTo.byName(thunk)
  }

  implicit val callbackToSync: Sync[CallbackTo] = new CallbackToSync {}
}

object CallbackToEffects extends CallbackToEffects
