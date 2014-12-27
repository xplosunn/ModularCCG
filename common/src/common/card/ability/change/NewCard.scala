package common.card.ability.change

import common.game.RemoteCard

/**
 * Created by HugoSousa on 10-12-2014.
 */
class NewCard(val remoteCard: RemoteCard, val zone: Int) extends GameChange{
  override def toString: String =
    "NewCard : " + remoteCard + ", zone: " + zone + "."
}
