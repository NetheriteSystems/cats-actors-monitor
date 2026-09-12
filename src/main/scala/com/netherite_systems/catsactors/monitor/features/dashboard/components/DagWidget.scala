package com.netherite_systems.catsactors.monitor.features.dashboard.components

import cats.effect.IO
import com.netherite_systems.catsactors.monitor.shared.model.{ActorSnapshot, ActorTreeNode, ActorTreeSnapshot}
import com.netherite_systems.htmfx.*
import scalatags.Text.all.*

object DagWidget {

  private val svgTag          = tag("svg")
  private val mailboxWarnAt   = 50
  private val mailboxAccentAt = 10
  private val nodeW           = 170
  private val nodeH           = 90
  private val padX            = 40
  private val padY            = 30

  def build: HtmFx[IO, ActorTreeSnapshot] =
    HtmFx
      .apply[IO, ActorTreeSnapshot] { snapshot =>
        IO.pure(render(snapshot))
      }
      .withPath("api/dag")

  private def render(snapshot: ActorTreeSnapshot): Tag =
    div(cls := "flex flex-col w-full gap-space-md select-none text-on-surface")(
      vitalsBar(snapshot),
      canvasContainer(snapshot)
    )

  private def vitalsBar(snapshot: ActorTreeSnapshot): Tag = {
    val statusDot =
      if snapshot.healthy then "bg-secondary animate-pulse shadow-[0_0_8px_#4edea3]"
      else "bg-error animate-pulse shadow-[0_0_8px_#ffb4ab]"
    val statusText  = if snapshot.healthy then "Active Directed Graph" else "Degraded"
    val statusColor = if snapshot.healthy then "text-secondary" else "text-error"
    div(
      cls := "w-full bg-surface-container-lowest rounded-xl p-space-md shadow-md flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-space-md"
    )(
      div(cls := "flex flex-wrap items-center gap-space-lg")(
        div(cls := "flex items-center gap-space-sm bg-surface-container-low px-space-md py-space-xs rounded-lg")(
          span(cls := s"w-2.5 h-2.5 rounded-full $statusDot"),
          div(cls := "flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Stream Topology"),
            span(cls := s"font-code-sm text-code-sm $statusColor font-semibold")(statusText)
          )
        ),
        div(cls := "h-6 w-px bg-surface-variant hidden md:block"),
        div(cls := "flex items-center gap-space-lg")(
          div(cls := "flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Active Mesh"),
            div(cls := "flex items-baseline gap-space-xs")(
              span(cls := "font-code-lg text-code-lg text-on-surface font-semibold")(s"${snapshot.totalActors}"),
              span(cls := "font-label-sm text-label-sm text-on-surface-variant")("Actors")
            )
          ),
          div(cls := "flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Mailbox Integrity"),
            div(cls := "flex items-center gap-space-xs")(
              span(cls := "font-code-sm text-code-sm text-secondary")(s"${snapshot.totalMailbox} queued"),
              if snapshot.terminatedCount > 0 then
                span(cls := "font-label-sm text-label-sm bg-error-container/20 text-error px-space-xs py-0.5 rounded")(
                  s"${snapshot.terminatedCount} terminated"
                )
              else span(cls := "font-label-sm text-label-sm bg-secondary-container/20 text-secondary px-space-xs py-0.5 rounded")("HEALTHY")
            )
          )
        )
      ),
      div(cls := "flex flex-wrap items-center gap-space-sm")(
        edgeFilterButtons,
        playbackBadge(snapshot)
      )
    )
  }

  private def edgeFilterButtons: Tag =
    div(cls := "bg-surface-container-low p-space-xs rounded-lg flex items-center gap-1")(
      button(
        cls := "px-space-sm py-1 font-label-sm text-label-sm rounded bg-primary text-on-primary font-semibold transition-all",
        id  := "btn-filter-all"
      )("ALL EDGES"),
      button(
        cls := "px-space-sm py-1 font-label-sm text-label-sm rounded text-on-surface-variant hover:text-on-surface hover:bg-surface-container transition-all flex items-center gap-1",
        id := "btn-filter-tell"
      )(
        span(cls := "w-1.5 h-1.5 rounded-full bg-primary"),
        "TELL (!)"
      ),
      button(
        cls := "px-space-sm py-1 font-label-sm text-label-sm rounded text-on-surface-variant hover:text-on-surface hover:bg-surface-container transition-all flex items-center gap-1",
        id := "btn-filter-ask"
      )(
        span(cls := "w-1.5 h-1.5 rounded-full bg-tertiary"),
        "ASK (?)"
      )
    )

  private def playbackBadge(snapshot: ActorTreeSnapshot): Tag = {
    val dotColor = if snapshot.healthy then "bg-secondary" else "bg-error"
    div(cls := "bg-surface-container-low px-space-sm py-1 rounded-lg flex items-center gap-space-xs")(
      span(cls := s"w-2 h-2 rounded-full $dotColor animate-ping"),
      span(cls := "font-label-sm text-label-sm text-on-surface font-semibold")("LIVE REALTIME")
    )
  }

  private def canvasContainer(snapshot: ActorTreeSnapshot): Tag = {
    val layout  = computeLayout(snapshot.actors)
    val depth   = if layout.isEmpty then 1 else layout.map(_.depth).max + 1
    val canvasH = depth * 160 + 80
    div(
      cls   := "relative w-full rounded-xl overflow-hidden bg-surface-container-lowest shadow-xl",
      style := "height: calc(100vh - 240px); min-height: 480px;"
    )(
      div(cls := "relative w-full h-full overflow-hidden", id := "dag-viewport")(
        dotGridSvg,
        edgeSvg(layout),
        div(style := s"position:relative; width:100%; height:${canvasH}px; min-width:100%;")(
          renderNodes(layout).toSeq*
        )
      ),
      dagInteractionScript(snapshot)
    )
  }

  private def dotGridSvg: Tag =
    svgTag(
      attr("xmlns") := "http://www.w3.org/2000/svg",
      cls           := "absolute inset-0 w-full h-full pointer-events-none opacity-20"
    )(
      raw(
        """<defs><pattern id="dot-matrix" width="24" height="24" patternUnits="userSpaceOnUse">""" +
          """<circle cx="2" cy="2" r="1" fill="#4cd7f6"/></pattern></defs>""" +
          """<rect width="100%" height="100%" fill="url(#dot-matrix)"/>"""
      )
    )

  private def edgeSvg(layout: List[LayoutNode]): Tag = {
    val byPath    = layout.map(n => n.actor.path -> n).toMap
    val edgeItems = layout.flatMap { node =>
      node.actor.parentPath.flatMap(byPath.get).map { parent =>
        val x1          = parent.x + nodeW / 2
        val y1          = parent.y + nodeH
        val x2          = node.x + nodeW / 2
        val y2          = node.y
        val cp          = (y1 + y2) / 2
        val pathD       = s"M $x1 $y1 C $x1 $cp, $x2 $cp, $x2 $y2"
        val color       = edgeColor(node.actor)
        val key         = statusKey(node.actor)
        val glowId      = s"glow-${node.actor.path.hashCode.abs}"
        val particleDur = if node.actor.mailboxSize > mailboxAccentAt then "0.6s" else "2.2s"
        val particleR   = if node.actor.mailboxSize > mailboxAccentAt then "4" else "3"
        val dashArray   = if node.actor.mailboxSize > mailboxAccentAt then "none" else "4,4"
        val strokeW     = if node.actor.mailboxSize > mailboxAccentAt then "2.5" else "1.8"
        val dashAttr    = if dashArray == "none" then "" else s""" stroke-dasharray="$dashArray""""
        val pathTag     =
          s"""<path d="$pathD" fill="none" stroke="$color" stroke-width="$strokeW"$dashAttr opacity="0.7" marker-end="url(#arrow-$key)"/>"""
        val particle =
          s"""<circle r="$particleR" fill="$color" filter="url(#$glowId)"><animateMotion dur="$particleDur" repeatCount="indefinite" path="$pathD"/></circle>"""
        pathTag + particle
      }
    }
    svgTag(
      attr("xmlns") := "http://www.w3.org/2000/svg",
      cls           := "absolute inset-0 w-full h-full pointer-events-none"
    )(raw(edgeSvgDefs + edgeItems.mkString + """</defs>"""))
  }

  private def edgeSvgDefs: String =
    """<defs>""" +
      """<filter id="glow-cyan" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="0" flood-color="#4cd7f6" flood-opacity="0.9" stdDeviation="3"/></filter>""" +
      """<filter id="glow-magenta" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="0" flood-color="#d0bcff" flood-opacity="0.8" stdDeviation="3"/></filter>""" +
      """<filter id="glow-error" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="0" flood-color="#ffb4ab" flood-opacity="0.8" stdDeviation="3"/></filter>""" +
      """<marker id="arrow-idle" markerWidth="6" markerHeight="6" refX="8" refY="5" orient="auto-start-reverse" viewBox="0 0 10 10"><path d="M 0 1 L 10 5 L 0 9 z" fill="#4cd7f6"/></marker>""" +
      """<marker id="arrow-busy" markerWidth="6" markerHeight="6" refX="8" refY="5" orient="auto-start-reverse" viewBox="0 0 10 10"><path d="M 0 1 L 10 5 L 0 9 z" fill="#d0bcff"/></marker>""" +
      """<marker id="arrow-terminated" markerWidth="6" markerHeight="6" refX="8" refY="5" orient="auto-start-reverse" viewBox="0 0 10 10"><path d="M 0 1 L 10 5 L 0 9 z" fill="#ffb4ab"/></marker>"""

  private def edgeColor(actor: ActorSnapshot): String =
    if actor.isTerminated then "#ffb4ab"
    else if !actor.isIdle then "#d0bcff"
    else "#4cd7f6"

  private def statusKey(actor: ActorSnapshot): String =
    if actor.isTerminated then "terminated"
    else if !actor.isIdle then "busy"
    else "idle"

  private def renderNodes(layout: List[LayoutNode]): List[Tag] =
    layout.map { ln =>
      val actor = ln.actor
      div(
        cls := s"dag-node absolute w-[170px] p-space-sm rounded-lg ${nodeBg(actor)} ${nodeShadow(actor)} hover:shadow-primary/20 transition-all cursor-pointer group",
        style             := s"left:${ln.x}px; top:${ln.y}px;",
        attr("data-node") := actor.name,
        onclick           := s"dagSelectNode('${actor.name}')"
      )(
        div(cls := "flex items-center justify-between mb-1")(
          div(cls := "flex items-center gap-1.5 min-w-0")(
            span(cls := s"w-2 h-2 rounded-full ${statusDotClass(actor)}"),
            span(cls := s"font-code-sm text-code-sm ${statusTextClass(actor)} font-bold truncate")(actor.name)
          ),
          statusChip(actor)
        ),
        div(cls := "font-code-sm text-code-sm text-on-surface-variant truncate")(actor.path),
        div(cls := "mt-2 flex items-center justify-between text-on-surface-variant font-label-sm text-label-sm")(
          if actor.childCount > 0 then span(s"${actor.childCount} children") else span("Leaf actor"),
          span(cls := s"font-mono ${statusTextClass(actor)}")(actor.statusLabel)
        ),
        div(cls := "mt-1.5 w-full bg-surface-container-lowest h-1 rounded-full overflow-hidden")(
          div(cls := s"${barColor(actor)} h-full", style := s"width: ${mailboxPercent(actor)}%")
        )
      )
    }

  private def nodeBg(actor: ActorSnapshot): String =
    if actor.mailboxSize > mailboxWarnAt then "bg-surface-container-high ring-2 ring-amber-400/80"
    else "bg-surface-container-high"

  private def nodeShadow(actor: ActorSnapshot): String =
    if actor.mailboxSize > mailboxWarnAt then "shadow-xl" else "shadow-md"

  private def statusDotClass(actor: ActorSnapshot): String =
    if actor.isTerminated then "bg-error"
    else if !actor.isIdle then "bg-tertiary animate-pulse"
    else "bg-secondary"

  private def statusTextClass(actor: ActorSnapshot): String =
    actor.statusColor match {
      case "error"     => "text-error"
      case "primary"   => "text-tertiary"
      case "secondary" => "text-secondary"
      case _           => "text-secondary"
    }

  private def statusChip(actor: ActorSnapshot): Tag =
    if actor.mailboxSize > mailboxWarnAt then
      span(cls := "font-label-sm text-label-sm bg-amber-400/20 text-amber-300 px-1 rounded uppercase")("ALERT")
    else if actor.isTerminated then
      span(cls := "font-label-sm text-label-sm bg-error-container text-on-error-container px-1 rounded")("TERM")
    else if actor.childCount > 0 then span(cls := "font-label-sm text-label-sm bg-tertiary/20 text-tertiary px-1 rounded")("PARENT")
    else span(cls := "font-label-sm text-label-sm bg-surface-container text-on-surface-variant px-1 rounded")("LEAF")

  private def barColor(actor: ActorSnapshot): String =
    if actor.mailboxSize > mailboxWarnAt then "bg-amber-400 animate-pulse"
    else if !actor.isIdle then "bg-primary"
    else "bg-secondary"

  private def mailboxPercent(actor: ActorSnapshot): Int =
    math.min(100, math.max(5, actor.mailboxSize * 2))

  private def dagInteractionScript(snapshot: ActorTreeSnapshot): Tag = {
    val nodeDataEntries = snapshot.actors
      .map { a =>
        val isBusy = !a.isIdle && !a.isTerminated
        s""""${a.name}":{"name":"${a.name}","path":"${a.path}","mailbox":${a.mailboxSize},"childCount":${a.childCount},"status":"${a.statusLabel}","terminated":${a.isTerminated},"busy":$isBusy,"idle":${a.isIdle}}"""
      }
      .mkString(",")
    script(
      raw(
        s"""
        |(function() {
        |  const nodeData = {$nodeDataEntries};
        |  window.dagSelectNode = function(name) {
        |    window.dagSelectedActor = name;
        |    document.querySelectorAll('.dag-node').forEach(n => {
        |      n.classList.remove('ring-2', 'ring-primary');
        |    });
        |    const sel = document.querySelector('[data-node="' + name + '"]');
        |    if (sel) sel.classList.add('ring-2', 'ring-primary');
        |    updateInspector(name);
        |  };
        |  function updateInspector(name) {
        |    const d = nodeData[name];
        |    if (!d) return;
        |    const el = (id) => document.getElementById(id);
        |    if (el('insp-title')) el('insp-title').textContent = d.name;
        |    if (el('insp-path')) el('insp-path').textContent = d.path;
        |    if (el('insp-badge')) {
        |      el('insp-badge').textContent = d.status;
        |      el('insp-badge').className = 'font-label-sm text-label-sm px-space-xs py-0.5 rounded font-mono ' +
        |        (d.terminated ? 'bg-error-container text-on-error-container' :
        |         d.busy ? 'bg-tertiary/20 text-tertiary' : 'bg-surface-container text-secondary');
        |    }
        |    if (el('insp-mailbox')) el('insp-mailbox').textContent = d.mailbox;
        |    if (el('insp-children')) el('insp-children').textContent = d.childCount;
        |    if (el('insp-status-detail')) el('insp-status-detail').textContent = d.status;
        |    if (el('insp-bar')) {
        |      const pct = Math.min(100, Math.max(5, d.mailbox * 2));
        |      el('insp-bar').style.width = pct + '%';
        |      el('insp-bar').className = 'h-full transition-all duration-300 ' +
        |        (d.mailbox > $mailboxWarnAt ? 'bg-amber-400 animate-pulse' : d.busy ? 'bg-primary' : 'bg-secondary');
        |    }
        |    if (el('insp-full-path')) el('insp-full-path').textContent = d.path;
        |    if (el('insp-status-text')) el('insp-status-text').textContent = d.status;
        |    if (el('insp-is-idle')) el('insp-is-idle').textContent = d.idle ? 'true' : 'false';
        |  }
        |  document.body.addEventListener('htmx:afterSwap', function(e) {
        |    if (e.detail.target && e.detail.target.id === 'dag-content' && window.dagSelectedActor) {
        |      const sel = document.querySelector('[data-node="' + window.dagSelectedActor + '"]');
        |      if (sel) sel.classList.add('ring-2', 'ring-primary');
        |      updateInspector(window.dagSelectedActor);
        |    }
        |  });
        |  const btnAll = document.getElementById('btn-filter-all');
        |  const btnTell = document.getElementById('btn-filter-tell');
        |  const btnAsk = document.getElementById('btn-filter-ask');
        |  [btnAll, btnTell, btnAsk].forEach(btn => {
        |    if (!btn) return;
        |    btn.addEventListener('click', () => {
        |      [btnAll, btnTell, btnAsk].forEach(b => {
        |        if (!b) return;
        |        b.classList.remove('bg-primary', 'text-on-primary');
        |        b.classList.add('text-on-surface-variant');
        |      });
        |      btn.classList.add('bg-primary', 'text-on-primary');
        |      btn.classList.remove('text-on-surface-variant');
        |    });
        |  });
        |})();
      """.stripMargin
      )
    )
  }

  private def computeLayout(actors: List[ActorSnapshot]): List[LayoutNode] = {
    val roots   = ActorTreeSnapshot("", 0, 0, 0, 0, 0, actors).buildTree
    val canvasW = 1000
    val hGap    = 30
    val vGap    = 70
    assignPositions(roots, padX, padY, canvasW - padX * 2, hGap, nodeH + vGap)
  }

  private def assignPositions(
    nodes: List[ActorTreeNode],
    x: Int,
    y: Int,
    totalW: Int,
    hGap: Int,
    vStep: Int
  ): List[LayoutNode] =
    if nodes.isEmpty then Nil
    else {
      val nodeCount = nodes.size
      val slotW     = totalW / math.max(1, nodeCount)
      nodes.zipWithIndex.flatMap { case (node, idx) =>
        val nx           = x + idx * slotW + (slotW - nodeW) / 2
        val ny           = y
        val childResults =
          if node.children.nonEmpty then assignPositions(node.children, x + idx * slotW, ny + vStep, slotW, hGap, vStep)
          else Nil
        LayoutNode(node.actor, nx, ny, depthOf(node.actor)) :: childResults
      }
    }

  private def depthOf(actor: ActorSnapshot): Int =
    actor.path.count(_ == '/') - 1

  private case class LayoutNode(
    actor: ActorSnapshot,
    x: Int,
    y: Int,
    depth: Int
  )
}
