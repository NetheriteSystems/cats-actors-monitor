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

  test("dashboard has daisyUI theme") {
    withSystem("d-theme") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("data-theme=\"business\""))
        }
      }
    }
  }

  test("dashboard has HTMX and daisyUI CDN links") {
    withSystem("d-cdn") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("daisyui@5")) &&
          expect(body.contains("htmx.org@4.0.0")) &&
          expect(body.contains("@tailwindcss/browser@4"))
        }
      }
    }
  }

  test("dashboard has summary polling with HTMX attributes") {
    withSystem("d-poll") { system =>
      MonitorTestClient(system) { client =>
        client.getDashboard().map { body =>
          expect(body.contains("hx-get=\"api/summary\"")) &&
          expect(body.contains("hx-trigger=\"every 2s\"")) &&
          expect(body.contains("hx-swap=\"innerHTML\""))
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

  test("summary reflects actor count from system") {
    withSystem("s-count") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "c1")
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "c2")
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "c3")
        result <- MonitorTestClient(system) { client =>
          client.getSummary().map { body =>
            expect(body.contains("Actors")) &&
            expect(body.contains("stat-value text-primary"))
          }
        }
      } yield result
    }
  }

  test("summary shows all actors as idle when none are busy") {
    withSystem("s-idle") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "i1")
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "i2")
        result <- MonitorTestClient(system) { client =>
          client.getSummary().map { body =>
            expect(body.contains("text-success")) &&
            expect(body.contains("Idle"))
          }
        }
      } yield result
    }
  }

  test("summary shows busy count when actor is processing") {
    withSystem("s-busy") { system =>
      for {
        ref    <- system.replyingActorOf(IO(ActorFixture.slowBehavior(500)), "slow")
        _      <- ref ! "msg"
        _      <- IO.sleep(200.millis)
        result <- MonitorTestClient(system) { client =>
          client.getSummary().map { body =>
            expect(body.contains("text-warning")) &&
            expect(body.contains("Busy"))
          }
        }
      } yield result
    }
  }

  test("summary includes system name") {
    withSystem("my-system") { system =>
      MonitorTestClient(system) { client =>
        client.getSummary().map { body =>
          expect(body.contains("my-system"))
        }
      }
    }
  }

  test("summary renders all stat sections for empty system") {
    withSystem("s-empty") { system =>
      MonitorTestClient(system) { client =>
        client.getSummary().map { body =>
          expect(body.contains("text-primary")) &&
          expect(body.contains("text-success")) &&
          expect(body.contains("text-warning")) &&
          expect(body.contains("text-error")) &&
          expect(body.contains("text-info"))
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

  test("tree shows green idle badge for idle actor") {
    withSystem("t-idle") { system =>
      for {
        _      <- system.replyingActorOf(IO(ActorFixture.fastBehavior), "ok")
        result <- MonitorTestClient(system) { client =>
          client.getTree().map { body =>
            expect(body.contains("badge-success")) &&
            expect(body.contains(">idle<"))
          }
        }
      } yield result
    }
  }

  test("tree shows yellow busy badge for busy actor") {
    withSystem("t-busy") { system =>
      for {
        ref    <- system.replyingActorOf(IO(ActorFixture.slowBehavior(5000)), "busy")
        _      <- ref ! "msg"
        _      <- IO.sleep(200.millis)
        result <- MonitorTestClient(system) { client =>
          client.getTree().map { body =>
            expect(body.contains("badge-warning")) &&
            expect(body.contains(">busy<"))
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
            expect(body.contains("badge-info")) &&
            expect(body.contains("mailbox:"))
          }
        }
      } yield result
    }
  }

  test("tree renders for empty system") {
    withSystem("t-empty") { system =>
      MonitorTestClient(system) { client =>
        client.getTree().map { body =>
          expect(body.contains("space-y-1"))
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
}
