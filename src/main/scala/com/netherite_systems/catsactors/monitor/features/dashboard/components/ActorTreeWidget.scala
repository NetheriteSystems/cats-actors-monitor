package com.netherite_systems.catsactors.monitor.features.dashboard.components

import cats.effect.IO
import com.netherite_systems.catsactors.monitor.shared.model.{ActorSnapshot, ActorTreeSnapshot}
import com.netherite_systems.htmfx.*
import scalatags.Text.all.*

object ActorTreeWidget {

  private val details = tag("details")
  private val summary = tag("summary")

  def build: HtmFx[IO, ActorTreeSnapshot] =
    HtmFx
      .apply[IO, ActorTreeSnapshot] { snapshot =>
        IO.pure(render(snapshot))
      }
      .withPath("api/tree")

  def renderStatusBadges(snapshot: ActorTreeSnapshot): Tag =
    div(style := "display:none")(
      snapshot.actors.map(renderSingleStatus).toSeq*
    )

  private def renderSingleStatus(actor: ActorSnapshot): Tag =
    div(id := actorId(actor), attr("hx-swap-oob") := "true", cls := "flex items-center gap-2")(
      renderStatusDot(actor),
      renderBadge(actor),
      renderMailboxBadge(actor)
    )

  private def render(snapshot: ActorTreeSnapshot): Tag = {
    val roots = buildTree(snapshot.actors)
    div(cls := "space-y-1")(roots.map(renderNode).toSeq*)
  }

  private def buildTree(actors: List[ActorSnapshot]): List[TreeNode] = {
    val byPath = actors.map(a => a.path -> TreeNode(a, scala.collection.mutable.ListBuffer.empty)).toMap
    val roots  = scala.collection.mutable.ListBuffer.empty[TreeNode]
    actors.foreach { actor =>
      actor.parentPath match {
        case Some(parent) if byPath.contains(parent) =>
          byPath(parent).children += byPath(actor.path)
        case _ =>
          roots += byPath(actor.path)
      }
    }
    roots.toList
  }

  private def renderNode(node: TreeNode): Tag =
    if node.children.nonEmpty then
      details(cls := "collapse collapse-arrow bg-base-100 border border-base-300 mb-1")(
        summary(cls := "collapse-title text-sm font-medium flex items-center gap-2")(
          span(cls := "font-mono")(node.actor.name),
          span(id := actorId(node.actor), cls := "flex items-center gap-2")(
            renderStatusDot(node.actor),
            renderBadge(node.actor),
            renderMailboxBadge(node.actor)
          )
        ),
        div(cls := "collapse-content")(
          div(cls := "ml-4 space-y-1")(node.children.toList.map(renderNode).toSeq*)
        )
      )
    else
      div(cls := "flex items-center gap-2 py-1 px-3 text-sm rounded bg-base-100 border border-base-300 mb-1 ml-4")(
        span(cls := "font-mono")(node.actor.name),
        span(id := actorId(node.actor), cls := "flex items-center gap-2")(
          renderStatusDot(node.actor),
          renderBadge(node.actor),
          renderMailboxBadge(node.actor)
        )
      )

  private def renderStatusDot(actor: ActorSnapshot): Tag =
    if actor.isTerminated then span(cls := "status status-error status-sm")
    else if !actor.isIdle then span(cls := "status status-warning status-sm")
    else span(cls                       := "status status-success status-sm")

  private def renderBadge(actor: ActorSnapshot): Tag =
    if actor.isTerminated then span(cls := "badge badge-error badge-xs")("terminated")
    else if !actor.isIdle then span(cls := "badge badge-warning badge-xs")("busy")
    else span(cls := "badge badge-success badge-xs")("idle")

  private def renderMailboxBadge(actor: ActorSnapshot): Tag =
    if actor.mailboxSize > 0 then span(cls := "badge badge-info badge-xs")(s"mailbox: ${actor.mailboxSize}")
    else span(cls := "badge badge-ghost badge-xs")("mailbox: 0")

  private def actorId(actor: ActorSnapshot): String =
    "status-" + actor.path.replaceAll("[^a-zA-Z0-9-]", "-")

  private case class TreeNode(
    actor: ActorSnapshot,
    children: scala.collection.mutable.ListBuffer[TreeNode]
  )
}
