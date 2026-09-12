package com.netherite_systems.catsactors.monitor.features.dashboard.page

import com.netherite_systems.htmfx4.HtmxAttributes.*
import scalatags.Text.all.*

object DashboardPage {

  private val pageTitle    = tag("title")
  private val materialLink = tag("link")
  private val asideTag     = tag("aside")
  private val navTag       = tag("nav")
  private val mainTag      = tag("main")
  private val styleTag     = tag("style")
  private val headerTag    = tag("header")

  def build(summaryEndpointPath: String, treeEndpointPath: String, statusEndpointPath: String, pollingSeconds: Int): Tag =
    html(cls := "dark", lang := "en")(
      head(
        meta(charset := "utf-8"),
        meta(name    := "viewport", content := "width=device-width, initial-scale=1.0"),
        pageTitle("cats-actors Monitor"),
        materialLink(
          href := "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200",
          rel  := "stylesheet"
        ),
        link(href := "https://fonts.googleapis.com", rel := "preconnect"),
        link(href := "https://fonts.gstatic.com", rel    := "preconnect", attr("crossorigin") := ""),
        link(
          href := "https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap",
          rel  := "stylesheet"
        ),
        materialLink(
          href := "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap",
          rel  := "stylesheet"
        ),
        script(src := "https://cdn.tailwindcss.com"),
        tag("script")(id := "tailwind-config")(raw(tailwindConfig)),
        tag("script")(src := "https://cdn.jsdelivr.net/npm/htmx.org@4.0.0"),
        baseStyleTag
      ),
      body(cls := "bg-background font-body-md text-on-surface antialiased")(
        header(pollingSeconds),
        sidebar,
        mainContent(summaryEndpointPath, treeEndpointPath, statusEndpointPath, pollingSeconds),
        interactionScript
      )
    )

  private def header(pollingSeconds: Int): Tag =
    headerTag(
      cls := "fixed top-0 left-0 right-0 z-50 h-16 bg-surface-container-lowest/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.4)]"
    )(
      div(cls := "w-full h-16 px-gutter-lg flex items-center justify-between gap-space-lg")(
        div(cls := "flex items-center gap-space-md shrink-0")(
          div(cls := "flex flex-col")(
            span(cls := "font-headline-sm text-headline-sm tracking-tight text-on-surface uppercase")("cats-actors Monitor"),
            div(cls := "flex items-center gap-space-sm")(
              span(cls := "font-code-sm text-code-sm text-primary", id := "header-system-name")("loading..."),
              span(cls := "font-label-sm text-label-sm px-space-xs bg-surface-container text-on-surface-variant rounded")("cats-actors")
            )
          )
        ),
        div(cls := "hidden lg:flex items-center gap-space-md bg-surface-container-low px-space-md py-space-xs rounded-lg")(
          headerStatusBadge,
          headerDivider,
          headerMetric("Uptime", "header-uptime", "loading..."),
          headerDivider,
          headerMetric("Active Actors", "header-actors", "loading..."),
          headerDivider,
          headerMetric("Mailbox", "header-mailbox", "loading...")
        ),
        div(cls := "flex items-center gap-space-md shrink-0")(
          div(cls := "flex flex-col items-end")(
            span(cls := "font-code-sm text-code-sm text-on-surface")("cats-actors-monitor"),
            span(cls := "font-label-sm text-label-sm text-primary-container uppercase", id := "header-interval")(
              s"${pollingSeconds}s interval"
            )
          ),
          div(cls := "w-8 h-8 rounded-full bg-primary flex items-center justify-center")(
            span(cls := "material-symbols-outlined text-on-primary text-[18px]")("monitor_heart")
          )
        )
      )
    )

  private def headerStatusBadge: Tag =
    div(cls := "flex items-center gap-space-xs", id := "header-health")(
      span(cls := "w-2 h-2 rounded-full bg-secondary animate-pulse"),
      span(cls := "font-label-sm text-label-sm text-secondary uppercase")("HEALTHY")
    )

  private def headerDivider: Tag =
    div(cls := "h-4 w-px bg-surface-variant")

  private def headerMetric(label: String, valueId: String, initialValue: String): Tag =
    div(cls := "flex flex-col")(
      span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")(label),
      span(cls := "font-code-sm text-code-sm text-on-surface", id := valueId)(initialValue)
    )

  private def sidebar: Tag =
    asideTag(
      cls := "fixed left-0 top-16 bottom-0 w-64 bg-surface-container-lowest z-40 flex flex-col p-space-md"
    )(
      div(cls := "flex items-center justify-between px-space-sm mb-space-md")(
        span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Runtime Operations"),
        span(cls := "font-label-sm text-label-sm text-secondary bg-surface-container px-space-xs rounded")("cats-actors")
      ),
      navTag(cls := "flex flex-col gap-space-xs flex-1")(
        sidebarLinkComponent("Actor Hierarchy", "/user", isActive = true, linkHref = "/"),
        sidebarLinkComponent("Topology DAG", "DAG", isActive = false, linkHref = "/dag"),
        sidebarLinkComponent("Dead Letter Stream", "0", isActive = false, disabled = true),
        sidebarLinkComponent("Cluster Nodes", "N/A", isActive = false, disabled = true)
      ),
      div(cls := "bg-surface-container-low p-space-sm rounded-lg flex flex-col gap-space-xs mt-auto", id := "sidebar-mailbox")(
        span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Mailbox Saturation"),
        div(cls := "w-full bg-surface-container-highest h-1 rounded-full overflow-hidden")(
          div(cls := "bg-primary h-full", id := "sidebar-mailbox-bar", style := "width: 0%")
        ),
        div(cls := "flex justify-between font-label-sm text-label-sm text-on-surface-variant")(
          span("Avg mailbox"),
          span(cls := "text-secondary", id := "sidebar-avg-mailbox")("0 msgs")
        )
      )
    )

  private[page] def sidebarLinkComponent(
    label: String,
    badge: String,
    isActive: Boolean,
    linkHref: String = "#",
    disabled: Boolean = false
  ): Tag =
    if disabled then
      div(
        cls := "flex items-center justify-between px-space-md py-space-xs text-on-surface-variant/50 font-code-md text-code-md rounded-lg cursor-not-allowed"
      )(
        span(label),
        span(cls := "font-label-sm text-label-sm text-on-surface-variant/30")(badge)
      )
    else
      a(
        cls := s"flex items-center justify-between px-space-md py-space-xs ${
            if isActive then "bg-surface-container text-primary"
            else "text-on-surface-variant hover:bg-surface-container hover:text-on-surface"
          } font-code-md text-code-md rounded-lg transition-colors",
        href := linkHref
      )(
        span(label),
        span(cls := s"font-label-sm text-label-sm ${if isActive then "text-primary" else "text-on-surface-variant"}")(badge)
      )

  private def mainContent(
    summaryEndpointPath: String,
    treeEndpointPath: String,
    statusEndpointPath: String,
    pollingSeconds: Int
  ): Tag =
    div(cls := "pl-64")(
      mainTag(cls := "relative pt-16 min-h-screen bg-background w-full px-gutter-lg")(
        div(cls := "flex flex-col w-full")(
          div(
            id        := "summary",
            hxGet     := summaryEndpointPath,
            hxTrigger := s"every ${pollingSeconds}s",
            hxSwap    := "innerHTML"
          )(loadingPlaceholder("Connecting to actor system...")),
          div(cls := "grid grid-cols-1 xl:grid-cols-12 gap-space-md w-full items-start mt-space-md")(
            div(cls := "xl:col-span-7")(
              div(
                id        := "actor-tree",
                hxGet     := treeEndpointPath,
                hxTrigger := "load",
                hxSwap    := "innerHTML"
              )(loadingPlaceholder("Loading actors...")),
              div(
                id        := "status-poller",
                hxGet     := statusEndpointPath,
                hxTrigger := s"every ${pollingSeconds}s",
                hxSwap    := "none"
              )
            ),
            div(cls := "xl:col-span-5", id := "detail-panel")(ActorDetailPlaceholder.render)
          )
        )
      )
    )

  private def loadingPlaceholder(message: String): Tag =
    div(cls := "flex items-center gap-2 p-4 bg-surface-container-low rounded-lg")(
      span(cls := "material-symbols-outlined text-primary animate-spin text-[18px]")("sync"),
      span(cls := "font-code-sm text-code-sm text-on-surface-variant")(message)
    )

  private object ActorDetailPlaceholder {
    def render: Tag =
      div(cls := "bg-surface-container-lowest rounded-xl shadow-md p-space-md flex flex-col gap-space-md")(
        div(cls := "flex flex-col items-center justify-center py-12 text-center")(
          span(cls := "material-symbols-outlined text-outline text-[48px] mb-space-md")("touch_app"),
          span(cls := "font-headline-sm text-headline-sm text-on-surface-variant")("Select an Actor"),
          span(cls := "font-body-sm text-body-sm text-outline mt-space-xs")(
            "Click on an actor in the hierarchy tree to inspect its details"
          )
        )
      )
  }

  private[page] val tailwindConfig: String =
    """
    |tailwind.config = {
    |    darkMode: "class",
    |    theme: {
    |      extend: {
    |        colors: {
    |          "background": "#0f131c",
    |          "on-surface": "#dfe2ee",
    |          "on-surface-variant": "#bcc9cd",
    |          "surface": "#0f131c",
    |          "surface-dim": "#0f131c",
    |          "surface-bright": "#353942",
    |          "surface-container-lowest": "#0a0e16",
    |          "surface-container-low": "#181c24",
    |          "surface-container": "#1c2028",
    |          "surface-container-high": "#262a33",
    |          "surface-container-highest": "#31353e",
    |          "surface-variant": "#31353e",
    |          "surface-tint": "#4cd7f6",
    |          "on-primary": "#003640",
    |          "primary": "#4cd7f6",
    |          "primary-container": "#06b6d4",
    |          "on-primary-container": "#00424f",
    |          "on-secondary": "#003824",
    |          "secondary": "#4edea3",
    |          "secondary-container": "#00a572",
    |          "on-secondary-container": "#00311f",
    |          "tertiary": "#d0bcff",
    |          "tertiary-container": "#b395ff",
    |          "on-tertiary": "#3c0091",
    |          "on-tertiary-container": "#4900ae",
    |          "error": "#ffb4ab",
    |          "on-error": "#690005",
    |          "error-container": "#93000a",
    |          "on-error-container": "#ffdad6",
    |          "outline": "#869397",
    |          "outline-variant": "#3d494c",
    |          "inverse-surface": "#dfe2ee",
    |          "inverse-on-surface": "#2c3039",
    |          "inverse-primary": "#00687a",
    |          "tertiary-fixed": "#e9ddff",
    |          "tertiary-fixed-dim": "#d0bcff",
    |          "on-tertiary-fixed": "#23005c",
    |          "on-tertiary-fixed-variant": "#5516be",
    |          "primary-fixed": "#acedff",
    |          "primary-fixed-dim": "#4cd7f6",
    |          "on-primary-fixed": "#001f26",
    |          "on-primary-fixed-variant": "#004e5c",
    |          "secondary-fixed": "#6ffbbe",
    |          "secondary-fixed-dim": "#4edea3",
    |          "on-secondary-fixed": "#002113",
    |          "on-secondary-fixed-variant": "#005236",
    |          "on-background": "#dfe2ee"
    |        },
    |        borderRadius: {
    |          DEFAULT: "0.125rem",
    |          lg: "0.25rem",
    |          xl: "0.5rem",
    |          full: "0.75rem"
    |        },
    |        spacing: {
    |          "margin": "0.75rem",
    |          "margin-lg": "1rem",
    |          "space-xl": "1rem",
    |          "gutter": "0.5rem",
    |          "space-xs": "0.125rem",
    |          "space-md": "0.5rem",
    |          "gutter-lg": "0.75rem",
    |          "space-sm": "0.25rem",
    |          "space-lg": "0.75rem"
    |        },
    |        fontFamily: {
    |          "headline-md": ["Geist"],
    |          "body-md": ["Geist"],
    |          "code-lg": ["JetBrains Mono"],
    |          "label-sm": ["JetBrains Mono"],
    |          "label-md": ["JetBrains Mono"],
    |          "code-sm": ["JetBrains Mono"],
    |          "headline-sm": ["Geist"],
    |          "body-sm": ["Geist"],
    |          "body-lg": ["Geist"],
    |          "headline-lg": ["Geist"],
    |          "code-md": ["JetBrains Mono"]
    |        },
    |        fontSize: {
    |          "headline-md": ["1.25rem", { lineHeight: "1.75rem", letterSpacing: "-0.02em", fontWeight: "600" }],
    |          "body-md": ["0.8125rem", { lineHeight: "1.25rem", fontWeight: "400" }],
    |          "code-lg": ["0.875rem", { lineHeight: "1.25rem", letterSpacing: "-0.01em", fontWeight: "500" }],
    |          "label-sm": ["0.625rem", { lineHeight: "0.75rem", letterSpacing: "0.075em", fontWeight: "600" }],
    |          "label-md": ["0.6875rem", { lineHeight: "0.875rem", letterSpacing: "0.05em", fontWeight: "600" }],
    |          "code-sm": ["0.6875rem", { lineHeight: "1rem", fontWeight: "400" }],
    |          "headline-sm": ["1rem", { lineHeight: "1.5rem", letterSpacing: "-0.015em", fontWeight: "600" }],
    |          "body-sm": ["0.75rem", { lineHeight: "1.125rem", fontWeight: "400" }],
    |          "body-lg": ["0.875rem", { lineHeight: "1.375rem", fontWeight: "400" }],
    |          "headline-lg": ["1.75rem", { lineHeight: "2.25rem", letterSpacing: "-0.025em", fontWeight: "600" }],
    |          "code-md": ["0.75rem", { lineHeight: "1.125rem", letterSpacing: "-0.005em", fontWeight: "400" }]
    |        }
    |      }
    |    }
    |  }
    """.stripMargin

  private[page] val baseStyleTag: Tag =
    styleTag(
      raw(
        """@layer base{html,body{margin:0;padding:0;}body{overscroll-behavior:none;}}""" +
          """::-webkit-scrollbar{display:none;}"""
      )
    )

  private val interactionScript: Tag =
    script(
      raw(
        """
        |document.addEventListener('DOMContentLoaded', () => {
        |  // Expand/Collapse for tree nodes
        |  document.addEventListener('click', (e) => {
        |    const toggle = e.target.closest('.node-toggle');
        |    if (toggle) {
        |      e.stopPropagation();
        |      const treeNode = toggle.closest('.tree-node');
        |      const siblingContainer = treeNode.nextElementSibling;
        |      if (siblingContainer && siblingContainer.classList.contains('tree-children')) {
        |        const isHidden = siblingContainer.style.display === 'none';
        |        siblingContainer.style.display = isHidden ? 'flex' : 'none';
        |        const icon = toggle.querySelector('.material-symbols-outlined');
        |        if (icon) icon.textContent = isHidden ? 'expand_more' : 'chevron_right';
        |      }
        |    }
        |  });
        |
        |  // Expand All / Collapse All
        |  document.addEventListener('click', (e) => {
        |    if (e.target.closest('#btnExpandAll')) {
        |      document.querySelectorAll('.tree-children').forEach(el => el.style.display = 'flex');
        |      document.querySelectorAll('.node-toggle .material-symbols-outlined').forEach(i => i.textContent = 'expand_more');
        |    }
        |    if (e.target.closest('#btnCollapseAll')) {
        |      document.querySelectorAll('.tree-children').forEach(el => el.style.display = 'none');
        |      document.querySelectorAll('.node-toggle .material-symbols-outlined').forEach(i => i.textContent = 'chevron_right');
        |    }
        |  });
        |
        |  // Search
        |  document.addEventListener('input', (e) => {
        |    if (e.target.id === 'actorSearchInput') {
        |      const query = e.target.value.toLowerCase().trim();
        |      document.querySelectorAll('.tree-node').forEach(node => {
        |        const text = node.textContent.toLowerCase();
        |        const path = (node.getAttribute('data-actor-path') || '').toLowerCase();
        |        node.style.display = (!query || text.includes(query) || path.includes(query)) ? 'flex' : 'none';
        |      });
        |    }
        |  });
        |
        |  // ESC to clear search
        |  document.addEventListener('keydown', (e) => {
        |    if (e.key === 'Escape') {
        |      const input = document.getElementById('actorSearchInput');
        |      if (input && document.activeElement === input) {
        |        input.value = '';
        |        input.dispatchEvent(new Event('input'));
        |        input.blur();
        |      }
        |    }
        |  });
        |
        |  // Filter pills
        |  document.addEventListener('click', (e) => {
        |    const pill = e.target.closest('.filter-pill');
        |    if (pill) {
        |      document.querySelectorAll('.filter-pill').forEach(p => {
        |        p.classList.remove('bg-primary-container', 'text-on-primary-container');
        |        p.classList.add('bg-surface-container', 'text-on-surface-variant');
        |      });
        |      pill.classList.remove('bg-surface-container', 'text-on-surface-variant');
        |      pill.classList.add('bg-primary-container', 'text-on-primary-container');
        |      const filter = pill.getAttribute('data-filter');
        |      document.querySelectorAll('.tree-node').forEach(node => {
        |        const text = node.textContent.toLowerCase();
        |        if (filter === 'all') {
        |          node.style.display = 'flex';
        |        } else if (filter === 'warning' && (text.includes('high') || text.includes('q: 1') || text.includes('q: 2') || text.includes('q: 3') || text.includes('q: 4') || text.includes('q: 5') || text.includes('q: 6') || text.includes('q: 7') || text.includes('q: 8') || text.includes('q: 9'))) {
        |          node.style.display = 'flex';
        |        } else if (filter === 'failed' && (text.includes('terminated') || text.includes('restarting'))) {
        |          node.style.display = 'flex';
        |        } else if (filter === 'idle' && text.includes('idle')) {
        |          node.style.display = 'flex';
        |        } else if (filter !== 'all') {
        |          node.style.display = 'none';
        |        }
        |      });
        |    }
        |  });
        |});
      """.stripMargin
      )
    )
}
