package com.netherite_systems.catsactors.monitor

import scala.concurrent.duration.*

import cats.effect.IO
import com.suprnation.actor.ActorSystem
import weaver.*

object DashboardRoutesSuite extends SimpleIOSuite {

  private def withSystem(name: String)(f: ActorSystem[IO] => IO[Expectations]): IO[Expectations] =
    ActorSystem[IO](name).use(f)

  test("dashboard has valid HTML structure") {
    withSystem("d-html") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("<html")) &&
          expect(body.contains("</html>")) &&
          expect(body.contains("<head")) &&
          expect(body.contains("<body"))
        }
      }
    }
  }

  test("dashboard uses dark mode class") {
    withSystem("d-theme") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("class=\"dark\""))
        }
      }
    }
  }

  test("dashboard has HTMX and Tailwind CDN links") {
    withSystem("d-cdn") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("htmx.org@4.0.0")) &&
          expect(body.contains("tailwindcss.com"))
        }
      }
    }
  }

  test("dashboard has Material Symbols and Geist fonts") {
    withSystem("d-fonts") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("Material+Symbols+Outlined")) &&
          expect(body.contains("Geist")) &&
          expect(body.contains("JetBrains+Mono"))
        }
      }
    }
  }

  test("dashboard has tree load-once trigger") {
    withSystem("d-tree1") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("hx-trigger=\"load\""))
        }
      }
    }
  }

  test("dashboard has status poller with none swap") {
    withSystem("d-status") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("hx-get=\"api/status\"")) &&
          expect(body.contains("hx-swap=\"none\""))
        }
      }
    }
  }

  test("tree renders actor names") {
    withSystem("t-names") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "alpha")
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "beta")
        result <- MonitorTestClient(system) { client =>
          client.getTree().map { body =>
            expect(body.contains("alpha")) &&
            expect(body.contains("beta"))
          }
        }
      } yield result
    }
  }

  test("tree shows IDLE status for idle actor") {
    withSystem("t-idle") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "ok")
        result <- MonitorTestClient(system) { client =>
          client.getTree().map { body =>
            expect(body.contains(">IDLE<"))
          }
        }
      } yield result
    }
  }

  test("tree shows BUSY status for busy actor") {
    withSystem("t-busy") { system =>
      for {
        ref    <- system.replyingActorOf(IO(ActorFixture.slowBehavior(5000)), "busy")
        _      <- ref ! "msg"
        _      <- IO.sleep(200.millis)
        result <- MonitorTestClient(system) { client =>
          client.getTree().map { body =>
            expect(body.contains(">BUSY<"))
          }
        }
      } yield result
    }
  }

  test("tree shows mailbox count badge") {
    withSystem("t-mail") { system =>
      for {
        ref    <- system.replyingActorOf(IO(ActorFixture.slowBehavior(2000)), "queued")
        _      <- ref ! "m1"
        _      <- ref ! "m2"
        _      <- ref ! "m3"
        _      <- IO.sleep(200.millis)
        result <- MonitorTestClient(system) { client =>
          client.getTree().map { body =>
            expect(body.contains("Q:"))
          }
        }
      } yield result
    }
  }

  test("tree renders for empty system") {
    withSystem("t-empty") { system =>
      MonitorTestClient(system) { client =>
        client.getTree().map { body =>
          expect(body.contains("Actor Hierarchy Graph"))
        }
      }
    }
  }

  test("tree has search bar and filter pills") {
    withSystem("t-search") { system =>
      MonitorTestClient(system) { client =>
        client.getTree().map { body =>
          expect(body.contains("actorSearchInput")) &&
          expect(body.contains("filter-pill"))
        }
      }
    }
  }

  test("status has OOB swap attribute for each actor") {
    withSystem("oob-oob") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "oob")
        result <- MonitorTestClient(system) { client =>
          client.getStatus().map { body =>
            expect(body.contains("hx-swap-oob=\"true\""))
          }
        }
      } yield result
    }
  }

  test("status has stable actor IDs") {
    withSystem("oob-ids") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "named")
        result <- MonitorTestClient(system) { client =>
          client.getStatus().map { body =>
            expect(body.contains("id=\"status-"))
          }
        }
      } yield result
    }
  }

  test("status renders hidden div for empty system") {
    withSystem("oob-empty") { system =>
      MonitorTestClient(system) { client =>
        client.getStatus().map { body =>
          expect(body.contains("display:none"))
        }
      }
    }
  }

  test("status includes header OOB badges") {
    withSystem("oob-header") { system =>
      MonitorTestClient(system) { client =>
        client.getStatus().map { body =>
          expect(body.contains("header-system-name")) &&
          expect(body.contains("header-uptime")) &&
          expect(body.contains("header-actors"))
        }
      }
    }
  }

  test("dag page has valid HTML structure") {
    withSystem("dag-html") { system =>
      MonitorTestClient(system) { client =>
        client.getDagPage().map { body =>
          expect(body.contains("<html")) &&
          expect(body.contains("Topology DAG"))
        }
      }
    }
  }

  test("dag page has HTMX polling for dag content") {
    withSystem("dag-poll") { system =>
      MonitorTestClient(system) { client =>
        client.getDagPage().map { body =>
          expect(body.contains("hx-get=\"api/dag\"")) &&
          expect(body.contains("hx-swap=\"innerHTML\""))
        }
      }
    }
  }

  test("dag page has navigation back to hierarchy") {
    withSystem("dag-nav") { system =>
      MonitorTestClient(system) { client =>
        client.getDagPage().map { body =>
          expect(body.contains("Back to Hierarchy"))
        }
      }
    }
  }

  test("dag renders actor nodes") {
    withSystem("dag-nodes") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "worker-1")
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "worker-2")
        result <- MonitorTestClient(system) { client =>
          client.getDag().map { body =>
            expect(body.contains("worker-1")) &&
            expect(body.contains("worker-2")) &&
            expect(body.contains("dag-node"))
          }
        }
      } yield result
    }
  }

  test("dag renders SVG edges with particles") {
    withSystem("dag-edges") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "parent")
        result <- MonitorTestClient(system) { client =>
          client.getDag().map { body =>
            expect(body.contains("<svg")) &&
            expect(body.contains("animateMotion")) &&
            expect(body.contains("dot-matrix"))
          }
        }
      } yield result
    }
  }

  test("dag shows Stream Topology vitals bar") {
    withSystem("dag-vitals") { system =>
      MonitorTestClient(system) { client =>
        client.getDag().map { body =>
          expect(body.contains("Stream Topology")) &&
          expect(body.contains("Active Mesh")) &&
          expect(body.contains("Mailbox Integrity"))
        }
      }
    }
  }

  test("dag has inspector panel") {
    withSystem("dag-insp") { system =>
      MonitorTestClient(system) { client =>
        client.getDagPage().map { body =>
          expect(body.contains("dag-inspector")) &&
          expect(body.contains("ACTOR INSPECTION")) &&
          expect(body.contains("Mailbox Queue"))
        }
      }
    }
  }

  test("dag shows LIVE realtime badge") {
    withSystem("dag-live") { system =>
      MonitorTestClient(system) { client =>
        client.getDag().map { body =>
          expect(body.contains("LIVE REALTIME"))
        }
      }
    }
  }

  test("dag includes header OOB badges") {
    withSystem("dag-oob") { system =>
      MonitorTestClient(system) { client =>
        client.getDag().map { body =>
          expect(body.contains("header-system-name")) &&
          expect(body.contains("header-uptime")) &&
          expect(body.contains("header-actors"))
        }
      }
    }
  }
}
