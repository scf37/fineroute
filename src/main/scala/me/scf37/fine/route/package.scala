package me.scf37.fine

package object route {
  type Service[F[_], Req, Resp] = Req => F[Resp]

  type Filter[F[_], Req, Resp] = Service[F, Req, Resp] => Service[F, Req, Resp]




}
