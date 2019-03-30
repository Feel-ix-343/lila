package views.html.user

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object mini {

  def apply(
    u: User,
    playing: Option[lila.game.Pov],
    blocked: Boolean,
    followable: Boolean,
    rel: Option[lila.relation.Relation],
    ping: Option[Int],
    crosstable: Option[lila.game.Crosstable]
  )(implicit ctx: Context) = frag(
    div(cls := "upt__info")(
      div(cls := "upt__info__top")(
        userLink(u, withPowerTip = false),
        u.profileOrDefault.countryInfo map { c =>
          val hasRoomForNameText = u.username.size + c.shortName.size < 20
          span(
            cls := "upt__info__top__country",
            title := (!hasRoomForNameText).option(c.name)
          )(
              img(cls := "flag", src := staticUrl(s"images/flags/${c.code}.png")),
              hasRoomForNameText option c.shortName
            )
        },
        ping.orElse(200.some) map bits.signalBars
      ),
      if (u.engine && !ctx.me.has(u) && !isGranted(_.UserSpy))
        div(cls := "upt__info__warning", dataIcon := "j")(trans.thisPlayerUsesChessComputerAssistance())
      else
        div(cls := "upt__info__ratings")(u.best8Perfs map { showPerfRating(u, _) })
    ),
    ctx.userId map { myId =>
      frag(
        (myId != u.id && u.enabled) option div(cls := "upt__actions btn-rack")(
          a(dataIcon := "1", title := trans.watchGames.txt(), href := routes.User.tv(u.username)),
          !blocked option frag(
            a(dataIcon := "c", title := trans.chat.txt(), href := s"${routes.Message.form()}?user=${u.username}"),
            a(dataIcon := "U", title := trans.challengeToPlay.txt(), href := s"${routes.Lobby.home()}?user=${u.username}#friend")
          ),
          views.html.relation.mini(u.id, blocked, followable, rel)
        ),
        crosstable.flatMap(_.nonEmpty) map { cross =>
          a(
            cls := "upt__score",
            href := s"${routes.User.games(u.username, "me")}#games",
            title := trans.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
          )(trans.yourScore(Html(s"""<strong>${cross.showScore(myId)}</strong> - <strong>${~cross.showOpponentScore(myId)}</strong>""")))
        }
      )
    },
    isGranted(_.UserSpy) option div(cls := "upt__mod")(
      span(
        trans.nbGames.plural(u.count.game, u.count.game.localize),
        " ", momentFromNowOnce(u.createdAt)
      ),
      (u.lameOrTroll || u.disabled) option span(cls := "upt__mod__marks")(mod.userMarks(u, None))
    ),
    (!ctx.pref.isBlindfold) ?? playing map { pov =>
      frag(
        gameFen(pov),
        div(cls := "game_legend")(
          playerText(pov.opponent, withRating = true),
          pov.game.clock map { c =>
            frag(" • ", c.config.show)
          }
        )
      )
    }
  )
}
