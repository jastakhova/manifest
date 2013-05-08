import main.scala._
import scala.collection.immutable._

object Petclinic {
  implicit val application = new Application()

  val appFork = Input("Source control fork", "dieu")
  val appBranch = Input("Source control branch", "HEAD")

  val db = Cluster("db")
  val app = Cluster("app", 2)
  val lb = Cluster("lb")

  Command("launch") { implicit context =>

    val node2provision = Seq(db, app, lb).foldLeft(Map[Cluster, (PhaseWithResult[ProvisionVmsOutput], Promise[List[String]])]())
      { case (m, node) => {
        val provision = node.provisionVms(EnvParam("identity").value, EnvParam("credential").value)
        m.updated(node, (provision, provision.result.map(_.ips)))
      }
    }

    Phase("db-tune") { implicit context: Context =>
      val installDB = db.recipe("mysql::server", "http://cookbooks") :<
          Map("mysql" ->
            Map(
               "server_root_password" -> "…",
               "server_repl_password" -> "…",
               "server_debian_password" -> "..."))
      (db.recipe("war::mysql", "http://cookbooks") :<
          Map("database" ->
            Map(
              "name" -> "petclinic",
              "schema" -> "…",
              "data" -> "..."))).waits(installDB)
    }.waits(node2provision(db)._1)

    val installLB = lb.recipe("haproxy", "http://cookbooks").waits(node2provision(lb)._1)

    Phase("app-tune") { implicit context: Context =>
      val installApp = app.recipe("tomcat", "http://cookbooks")
      val deployApp = Phase("deploy-app") { implicit context: Context =>
        val deployApp = app.recipe("war", "http://") :<
          Map("war" -> Map("deploy" -> Map("git" -> Map(
            "url" -> "git",
            "revision" -> appBranch.value
          ))))
        (app.recipe("war::configure", "http://") :<
          Map("configure" -> Map(
            "source" -> "https://",
            "to" -> "https://",
            "variables" ->
              Map("hosts" -> node2provision(db)._2.value.mkString(","),
                  "database" -> "petclinic")
          ))
        ).waits(deployApp)
      }.waits(installApp)

      (lb.recipe("war::lb", "http://cookbooks")
        :< Map("haproxy.rebalance" -> Map("nodes" -> node2provision(app)._2.value.mkString(",")))
        ).waits(deployApp)
    }.waits(installLB)

    Output("db-hosts", "Database IP address", node2provision(db)._2.value.mkString(","))
    Output("app-hosts", "Application IP address", node2provision(app)._2.value.mkString(","))
    Output("lb-hosts", "Load Balancer IP address", node2provision(lb)._2.value.mkString(","))
    Output("app-url", "Url to the application", "http://" + node2provision(lb)._2.value.head)
    Output("haproxy-url", "Url to haproxy stats", "http://" + node2provision(lb)._2.value.head + ":22002/")
  }

  application.build
}

