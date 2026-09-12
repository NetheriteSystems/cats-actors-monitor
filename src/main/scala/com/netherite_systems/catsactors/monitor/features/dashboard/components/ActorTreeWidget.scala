package com.netherite_systems.catsactors.monitor.features.dashboard.components

import cats.effect.IO
import com.netherite_systems.catsactors.monitor.shared.model.{ActorSnapshot, ActorTreeNode, ActorTreeSnapshot}
import com.netherite_systems.htmfx.*
import scalatags.Text.all.*

object ActorTreeWidget {

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
    div(id := actorId(actor), attr("hx-swap-oob") := "true", cls := "flex items-center gap-space-md shrink-0")(
      renderMailboxBadge(actor),
      renderStatusBadge(actor)
    )

  private def render(snapshot: ActorTreeSnapshot): Tag = {
    val roots = snapshot.buildTree
    div(cls := "flex flex-col w-full")(
      treeHeader(snapshot, roots),
      div(cls := "bg-surface-container-lowest rounded-xl shadow-md p-space-md flex flex-col gap-space-sm")(
        treeToolbar,
        div(cls := "flex flex-col gap-0.5 select-none font-code-md text-code-md text-on-surface", id := "actorTreeRoot")(
          roots.map(renderNode).toSeq*
        ),
        treeStatusBar(snapshot)
      )
    )
  }

  private def treeHeader(snapshot: ActorTreeSnapshot, roots: List[ActorTreeNode]): Tag =
    div(cls := "flex items-center justify-between pb-space-xs bg-surface-container-low px-space-sm py-space-xs rounded-lg mb-space-sm")(
      div(cls := "flex items-center gap-space-sm")(
        span(cls := "material-symbols-outlined text-primary text-[18px]")("account_tree"),
        span(cls := "font-headline-sm text-headline-sm text-on-surface")("Actor Hierarchy Graph"),
        span(cls := "font-label-sm text-label-sm text-outline uppercase")(s"/${snapshot.systemName}")
      ),
      div(cls := "flex items-center gap-space-md")(
        span(cls := "font-code-sm text-code-sm text-on-surface-variant")(s"${roots.size} Subtrees Loaded"),
        span(cls := "font-label-sm text-label-sm bg-surface-container text-secondary px-space-xs rounded")("REALTIME")
      )
    )

  private def treeToolbar: Tag =
    div(
      cls := "flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-space-sm bg-surface-container-low p-space-xs rounded-lg mb-space-xs"
    )(
      div(cls := "flex-1 flex items-center gap-space-sm bg-surface-container-lowest px-space-md py-space-xs rounded")(
        span(cls := "material-symbols-outlined text-outline text-[16px]")("search"),
        input(
          cls := "bg-transparent border-none outline-none text-on-surface font-code-sm text-code-sm placeholder:text-outline w-full",
          id  := "actorSearchInput",
          attr("placeholder") := "Search by ActorPath (e.g. /user/*), status, or mailbox size...",
          attr("type")        := "text"
        ),
        span(cls := "font-label-sm text-label-sm text-outline bg-surface-container px-space-xs py-0.5 rounded")("ESC TO CLEAR")
      ),
      div(cls := "flex items-center gap-space-xs overflow-x-auto shrink-0 pb-1 lg:pb-0")(
        filterPill("ALL ACTORS", "all", isActive = true),
        filterPill("MAILBOX > 0", "warning", dotColor = Some("bg-primary")),
        filterPill("TERMINATED", "failed", dotColor = Some("bg-error")),
        filterPill("IDLE", "idle", dotColor = None),
        div(cls := "h-4 w-px bg-surface-variant mx-space-xs"),
        div(cls := "flex items-center gap-space-xs")(
          button(
            cls := "bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface p-space-xs rounded flex items-center",
            id            := "btnExpandAll",
            attr("title") := "Expand entire tree"
          )(
            span(cls := "material-symbols-outlined text-[16px]")("unfold_more")
          ),
          button(
            cls := "bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface p-space-xs rounded flex items-center",
            id            := "btnCollapseAll",
            attr("title") := "Collapse all levels"
          )(
            span(cls := "material-symbols-outlined text-[16px]")("unfold_less")
          )
        )
      )
    )

  private def filterPill(label: String, filter: String, isActive: Boolean = false, dotColor: Option[String] = None): Tag =
    button(
      cls := s"filter-pill ${
          if isActive then "bg-primary-container text-on-primary-container"
          else "bg-surface-container text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high"
        } px-space-sm py-space-xs rounded font-label-sm text-label-sm flex items-center gap-space-xs transition-all",
      attr("data-filter") := filter
    )(
      dotColor.map(c => span(cls := s"w-1.5 h-1.5 rounded-full $c")).toSeq :+
        raw(label)
    )

  private def treeStatusBar(snapshot: ActorTreeSnapshot): Tag =
    div(
      cls := "mt-space-sm pt-space-xs bg-surface-container-low px-space-sm py-space-xs rounded-lg flex items-center justify-between text-on-surface-variant font-code-sm text-code-sm"
    )(
      div(cls := "flex items-center gap-space-sm")(
        span(cls := s"w-2 h-2 rounded-full ${if snapshot.healthy then "bg-secondary" else "bg-error"}"),
        span(s"${snapshot.totalActors} actors in ${snapshot.systemName}")
      ),
      div(cls := "flex items-center gap-space-xs")(
        span(cls := "material-symbols-outlined text-[14px]")("sync"),
        span(cls := "font-label-sm text-label-sm")("AUTO-REFRESH")
      )
    )

  private def renderNode(node: ActorTreeNode): Tag = {
    val hasChildren = node.children.nonEmpty
    val depth       = node.actor.path.count(_ == '/') - 1
    val paddingLeft = s"pl-${math.min(depth * 4, 16)}"

    div(cls := s"$paddingLeft flex flex-col gap-0.5")(
      div(
        cls := s"tree-node group flex items-center justify-between py-1 px-space-sm rounded ${nodeBackground(node.actor)} transition-colors cursor-pointer",
        attr("data-actor-path") := node.actor.path,
        attr("data-mailbox")    := node.actor.mailboxSize,
        attr("data-status")     := node.actor.statusLabel
      )(
        div(cls := "flex items-center gap-space-xs min-w-0")(
          if hasChildren then
            button(cls := "node-toggle text-outline hover:text-on-surface w-4 h-4 flex items-center justify-center")(
              span(cls := "material-symbols-outlined text-[14px]")("expand_more")
            )
          else span(cls := "w-4"),
          span(cls := s"material-symbols-outlined ${nodeIconColor(node.actor)} text-[16px]")(nodeIcon(node.actor)),
          span(cls := s"${nodeNameClass(node.actor)} truncate")(node.actor.name),
          if node.actor.childCount > 0 && !hasChildren then
            span(cls := "font-label-sm text-label-sm text-outline ml-space-xs")(s"[${node.actor.childCount} children]")
          else span()
        ),
        div(cls := "flex items-center gap-space-md shrink-0", id := actorId(node.actor))(
          renderMailboxBadge(node.actor),
          renderStatusBadge(node.actor)
        )
      ),
      if hasChildren then
        div(cls := "tree-children pl-4 flex flex-col gap-0.5")(
          node.children.map(renderNode).toSeq*
        )
      else span()
    )
  }

  private def nodeBackground(actor: ActorSnapshot): String =
    if actor.isTerminated then "bg-error-container/20 hover:bg-error-container/30"
    else if !actor.isIdle && actor.mailboxSize > 50 then "bg-surface-container-low"
    else "hover:bg-surface-container-low"

  private def nodeIcon(actor: ActorSnapshot): String =
    if actor.isTerminated then "warning"
    else if actor.path.contains("system") then "shield"
    else if actor.childCount > 0 then "folder_special"
    else "circle"

  private def nodeIconColor(actor: ActorSnapshot): String =
    if actor.isTerminated then "text-error"
    else if !actor.isIdle then "text-primary"
    else if actor.path.contains("system") then "text-tertiary"
    else "text-outline"

  private def nodeNameClass(actor: ActorSnapshot): String =
    if actor.isTerminated then "text-error font-semibold"
    else if !actor.isIdle then "text-on-surface font-semibold"
    else "text-on-surface-variant"

  private def renderMailboxBadge(actor: ActorSnapshot): Tag =
    if actor.mailboxSize > 50 then
      span(cls := "font-label-sm text-label-sm bg-surface-container-high text-primary px-space-xs py-0.5 rounded font-bold")(
        s"Q: ${actor.mailboxSize}"
      )
    else if actor.mailboxSize > 0 then span(cls := "font-label-sm text-label-sm text-outline")(s"Q: ${actor.mailboxSize}")
    else span(cls := "font-label-sm text-label-sm text-outline")("Q: 0")

  private def renderStatusBadge(actor: ActorSnapshot): Tag =
    if actor.isTerminated then
      span(cls := "font-label-sm text-label-sm bg-error-container text-on-error-container font-bold px-space-xs rounded")(actor.statusLabel)
    else if !actor.isIdle then
      span(cls := "font-label-sm text-label-sm bg-primary text-on-primary font-semibold px-space-xs rounded")(actor.statusLabel)
    else span(cls := "font-label-sm text-label-sm bg-surface-container text-secondary px-space-xs rounded")(actor.statusLabel)

  private def actorId(actor: ActorSnapshot): String =
    "status-" + actor.path.replaceAll("[^a-zA-Z0-9-]", "-")
}
